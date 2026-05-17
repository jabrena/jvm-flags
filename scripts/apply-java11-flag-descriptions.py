#!/usr/bin/env python3
"""Apply proper descriptions to java-11.json flags from OpenJDK 11u globals and curated text."""

from __future__ import annotations

import json
import re
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "src/main/resources/java-11.json"

GLOBALS_URLS = (
    "https://raw.githubusercontent.com/openjdk/jdk11u/master/src/hotspot/share/runtime/globals.hpp",
    "https://raw.githubusercontent.com/openjdk/jdk11u/master/src/hotspot/share/c1/c1_globals.hpp",
    "https://raw.githubusercontent.com/openjdk/jdk11u/master/src/hotspot/share/opto/c2_globals.hpp",
    "https://raw.githubusercontent.com/openjdk/jdk11u/master/src/hotspot/share/gc/g1/g1_globals.hpp",
    "https://raw.githubusercontent.com/openjdk/jdk11u/master/src/hotspot/share/gc/shenandoah/shenandoah_globals.hpp",
    "https://raw.githubusercontent.com/openjdk/jdk11u/master/src/hotspot/share/gc/cms/cms_globals.hpp",
)

MACRO_START = re.compile(
    r"\b(?:product|develop|diagnostic|experimental|manageable|notproduct|"
    r"pd_product|product_pd|develop_pd|lp64_product|product_rw)\s*"
    r"\(\s*(\w+)\s*,\s*(\w+)\s*,",
    re.MULTILINE,
)

# Overrides when globals.hpp text is missing, abbreviated, or has known typos.
MANUAL_DESCRIPTIONS: dict[str, str] = {
    "AllocateHeapAt": (
        "Path to a directory where a temporary file is created as backing store for the Java heap."
    ),
    "AllowVectorizeOnDemand": (
        "Globally suppress on-demand vectorization set in VectorizeMethod."
    ),
    "CMSInitiatingOccupancyFraction": (
        "Percentage occupancy of the old generation at which a CMS collection cycle starts."
    ),
    "PrintGC": "Enables basic garbage collection logging to stdout.",
    "PrintGCDetails": "Prints detailed information about each garbage collection event.",
    "UseConcMarkSweepGC": (
        "Enables the Concurrent Mark Sweep (CMS) collector for the old generation."
    ),
    "ShenandoahGCMode": "Shenandoah GC mode (for example SATB); defines which barriers are installed.",
    "ShenandoahSoftMaxHeapSize": (
        "Soft maximum heap size for Shenandoah; the collector may shrink below this when idle."
    ),
    "G1UseAdaptiveIHOP": (
        "Adaptively adjusts initiating heap occupancy from the initial IHOP value."
    ),
    "PrintFlagsRanges": "Print valid ranges for all JVM flags when used with -XX:+PrintFlagsFinal.",
    "SegmentedCodeCache": "Divides the code cache into distinct segments for different code types.",
    "ExecutingUnitTests": "Indicates the JVM is running unit tests; adjusts internal behavior for test harnesses.",
    "PreTouchParallelChunkSize": (
        "Size in bytes of each chunk when pretouching heap memory in parallel."
    ),
    "UseSIMDForArrayEquals": (
        "Use SIMD instructions in generated code for Arrays.equals when supported by the CPU."
    ),
    "UseSimpleArrayEquals": (
        "Use a simple scalar implementation for Arrays.equals instead of SIMD intrinsics."
    ),
    "CalculateClassFingerprint": (
        "Compute a fingerprint hash for each loaded class for class loading and CDS."
    ),
    "InlineSynchronizedMethods": (
        "Allow the C1 compiler to inline synchronized methods."
    ),
}

# Curated descriptions keyed by catalog node id (from scripts/flag-descriptions.py).
DESCRIPTIONS_BY_NODE_ID: dict[str, str] = {
    "flag-cmsinitiatingoccupancyfraction": (
        "Occupancy percentage of the old generation at which CMS starts a collection cycle."
    ),
    "flag-useconcmarksweepgc": (
        "Enables the mostly concurrent CMS collector for the old generation (deprecated in later JDKs)."
    ),
    "flag-pending-printgc": "Enables basic garbage collection logging to stdout.",
    "flag-printgcdetails": "Prints detailed information about each garbage collection event.",
    "flag-verbosegc": "Enables basic garbage collection logging to stdout.",
}


def polish_description(text: str) -> str:
    text = re.sub(r"\s+", " ", text).strip()
    if not text:
        return text
    text = text[0].upper() + text[1:]
    if text.endswith("."):
        return text
    return text + "."


def internal_name(catalog_flag: str) -> str | None:
    if not catalog_flag.startswith("-XX:"):
        return None
    rest = catalog_flag[4:]
    if rest.startswith("+") or rest.startswith("-"):
        return rest[1:]
    equals = rest.find("=")
    return rest[:equals] if equals >= 0 else rest


def is_fallback_description(desc: str, internal: str) -> bool:
    words = re.sub(r"([a-z])([A-Z])", r"\1 \2", internal).lower().strip()
    return desc.lower().rstrip(".") == words


def fetch_globals() -> dict[str, str]:
    flags: dict[str, str] = {}
    for url in GLOBALS_URLS:
        with urllib.request.urlopen(url, timeout=60) as response:
            text = response.read().decode("utf-8")
        for match in MACRO_START.finditer(text):
            name = match.group(2)
            chunk = text[match.end() : match.end() + 280]
            strings = re.findall(r'"([^"]*)"', chunk)
            if strings:
                flags[name] = " ".join(part.strip() for part in strings).strip()
    flags.update(MANUAL_DESCRIPTIONS)
    return flags


def description_for(node: dict, globals_desc: dict[str, str]) -> str | None:
    node_id = node["id"]
    if node_id in DESCRIPTIONS_BY_NODE_ID:
        return DESCRIPTIONS_BY_NODE_ID[node_id]

    internal = internal_name(node["flag"])
    if internal and internal in globals_desc:
        return polish_description(globals_desc[internal])
    return None


def main() -> None:
    print("Fetching OpenJDK 11u globals.hpp descriptions...")
    globals_desc = fetch_globals()

    data = json.loads(CATALOG.read_text(encoding="utf-8"))
    updated = 0
    still_fallback: list[str] = []

    for node in data["nodes"]:
        if node.get("type") != "flag":
            continue
        internal = internal_name(node["flag"]) or ""
        current = node.get("description", "")
        if not is_fallback_description(current, internal):
            continue

        new_desc = description_for(node, globals_desc)
        if new_desc is None:
            still_fallback.append(node["flag"])
            continue

        node["description"] = new_desc
        updated += 1

    CATALOG.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(f"Updated {updated} flag descriptions in {CATALOG.name}")
    if still_fallback:
        print(f"Still using fallback descriptions ({len(still_fallback)}):")
        for flag in still_fallback:
            print(f"  {flag}")


if __name__ == "__main__":
    main()
