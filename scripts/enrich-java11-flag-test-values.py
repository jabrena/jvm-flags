#!/usr/bin/env python3
"""Add or fix testValue/requires on java-11.json flags using the Java 11 PrintFlagsFinal snapshot."""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "src/main/resources/java-11.json"
SNAPSHOT = ROOT / "src/test/resources/printflagsfinal/java11.md"
TMP = "/tmp/jvm-flags-graph.txt"

FLAG_LINE = re.compile(
    r"\s+(?P<type>bool|intx|uintx|ccstr|ccstrlist|double|size_t)\s+"
    r"(?P<name>\S+)\s+(?:=|:=)\s*(?P<value>\S*)\s+\{(?P<tags>[^}]*)\}"
)

EXCLUDED = {
    "-version",
    "-showversion",
    "-help",
    "-X",
    "-Xint",
    "-Xcomp",
    "-Xmixed",
    "-Xbatch",
    "-Xshare:on",
    "-Xshare:off",
    "-Xshare:auto",
    "-Xverify:all",
    "-Xverify:none",
    "-Xloggc:<file>",
    "-Djava.security.policy=<file>",
    "-Dfile.encoding=UTF-8",
    "-Duser.timezone=UTC",
    "-Djava.io.tmpdir=<path>",
    "-Djava.security.manager",
    "-XX:+UnlockDiagnosticVMOptions",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:G1NewSizePercent=<n>",
    "-XX:G1MaxNewSizePercent=<n>",
    "-XX:SharedArchiveFile=<file>",
    "-XX:+PrintNMTStatistics",
    "-XX:PermSize=<size>",
    "-XX:MaxPermSize=<size>",
}

ALIASES = {
    "-Xms<size>": "InitialHeapSize",
    "-Xmx<size>": "MaxHeapSize",
    "-Xss<size>": "ThreadStackSize",
    "-verbose:gc": "PrintGC",
    "-XX:+UseAESIntrinsics": "UseAES",
    "-XX:+UseCRC32Intrinsics": "UseCRC32",
}

KEEP_TESTABLE_FALSE = {
    "-XX:+PrintFlagsFinal",
    "-Xshare:on",
    "-XX:AllocateHeapAt=<value>",
}

COLLECTOR_REQUIRES: dict[str, list[str]] = {
    "CMSIncrementalMode": ["-XX:+UseConcMarkSweepGC"],
}

# Snapshot defaults or paired flags that differ from java-8 catalog copies.
JAVA11_TEST_OVERRIDES: dict[str, tuple[str, list[str] | None]] = {
    "CompilationPolicyChoice": ("-XX:CompilationPolicyChoice=2", None),
    "G1ConcRefinementThresholdStep": ("-XX:G1ConcRefinementThresholdStep=2", None),
    "G1RSetRegionEntries": ("-XX:G1RSetRegionEntries=256", None),
    "G1RSetSparseRegionEntries": ("-XX:G1RSetSparseRegionEntries=4", None),
    "GCPauseIntervalMillis": (
        "-XX:GCPauseIntervalMillis=201",
        ["-XX:MaxGCPauseMillis=200"],
    ),
}

ACCEPT_NON_ZERO_EXIT = {
    "PrintSharedArchiveAndExit",
    "RequireSharedSpaces",
    "DumpSharedSpaces",
}

CCSTR_TEST_VALUES: dict[str, str] = {
    "AllocateHeapAt": "",
    "CompileCommand": "quiet,java/lang/Object.hashCode",
    "CompileCommandFile": TMP,
    "CompileOnly": "java.lang.Object",
    "DumpLoadedClassList": TMP,
    "ErrorFile": TMP,
    "ErrorReportServer": "localhost",
    "ExtraSharedClassListFile": TMP,
    "FlightRecorderOptions": "stackdepth=64",
    "HeapDumpPath": "/tmp",
    "InlineDataFile": TMP,
    "OnError": "true",
    "OnOutOfMemoryError": "true",
    "PerfDataSaveFile": TMP,
    "ReplayDataFile": TMP,
    "SharedClassListFile": TMP,
    "SharedArchiveConfigFile": TMP,
    "StartFlightRecording": "dumponexit=true",
    "SyncKnobs": "noop",
    "TraceJVMTI": "all",
}

PLACEHOLDER = re.compile(r"<(?:n|size|value|options|ms|file|path|mode)>")


def parse_snapshot(path: Path) -> dict[str, dict[str, str]]:
    flags: dict[str, dict[str, str]] = {}
    for match in FLAG_LINE.finditer(path.read_text(encoding="utf-8")):
        flags[match.group("name")] = {
            "type": match.group("type"),
            "value": match.group("value"),
            "tags": match.group("tags").strip(),
        }
    return flags


def to_print_flags_final_name(catalog_flag: str) -> str | None:
    if catalog_flag in EXCLUDED:
        return None
    if catalog_flag in ALIASES:
        return ALIASES[catalog_flag]
    if not catalog_flag.startswith("-XX:"):
        return None
    rest = catalog_flag[4:]
    if rest.startswith("+") or rest.startswith("-"):
        return rest[1:]
    equals = rest.find("=")
    return rest[:equals] if equals >= 0 else rest


def requires_for(tags: str) -> list[str]:
    requires: list[str] = []
    if "diagnostic" in tags:
        requires.append("-XX:+UnlockDiagnosticVMOptions")
    if "experimental" in tags:
        requires.append("-XX:+UnlockExperimentalVMOptions")
    return requires


def normalize_numeric_value(internal_name: str, flag_type: str, value: str) -> str:
    if value in ("", "0") and internal_name.endswith("Percentage"):
        return "5"
    if value in ("", "-1"):
        return "5"
    if flag_type == "double" and value:
        return value
    if flag_type in ("intx", "uintx", "size_t") and value:
        return value
    return "5"


def test_value_for(internal_name: str, snapshot_flag: dict[str, str]) -> str:
    flag_type = snapshot_flag["type"]
    value = snapshot_flag["value"]

    if flag_type == "bool":
        return f"-XX:+{internal_name}"

    if flag_type in ("intx", "uintx", "double"):
        normalized = normalize_numeric_value(internal_name, flag_type, value)
        return f"-XX:{internal_name}={normalized}"

    if flag_type == "size_t":
        normalized = value if value else "65536"
        return f"-XX:{internal_name}={normalized}"

    if flag_type in ("ccstr", "ccstrlist"):
        literal = CCSTR_TEST_VALUES.get(internal_name, TMP)
        return f"-XX:{internal_name}={literal}"

    return f"-XX:{internal_name}={value or '5'}"


def needs_test_value(node: dict, internal: str, snapshot_flag: dict[str, str]) -> bool:
    test_value = node.get("testValue")
    if test_value is None:
        return True
    if PLACEHOLDER.search(test_value):
        return True
    # Refresh values copied from java-8 when they used invalid sentinels (e.g. =0).
    if re.search(r"=0$", test_value):
        return True
    # Undo mistaken =5 when snapshot default is 0.
    if (
        test_value.endswith("=5")
        and snapshot_flag.get("value") == "0"
        and internal not in JAVA11_TEST_OVERRIDES
    ):
        return True
    return False


def main() -> None:
    snapshot = parse_snapshot(SNAPSHOT)
    data = json.loads(CATALOG.read_text(encoding="utf-8"))

    enriched = 0
    skipped = 0
    removed_commercial = 0
    missing_snapshot: list[str] = []

    for node in data["nodes"]:
        if node.get("type") != "flag":
            continue

        catalog_flag = node["flag"]
        if catalog_flag in KEEP_TESTABLE_FALSE:
            node["testable"] = False
            node.pop("testValue", None)
            node.pop("requires", None)
            skipped += 1
            continue

        internal = to_print_flags_final_name(catalog_flag)
        if internal is None:
            continue

        if internal not in snapshot:
            missing_snapshot.append(catalog_flag)
            node["testable"] = False
            continue

        snapshot_flag = snapshot[internal]
        unlock_requires = requires_for(snapshot_flag["tags"])

        override = JAVA11_TEST_OVERRIDES.get(internal)
        if override:
            node["testValue"] = override[0]
            if override[1]:
                node["requires"] = override[1]
            enriched += 1
        elif needs_test_value(node, internal, snapshot_flag):
            node["testValue"] = test_value_for(internal, snapshot_flag)
            enriched += 1

        collector_requires = COLLECTOR_REQUIRES.get(internal)
        if collector_requires:
            node["requires"] = collector_requires
        elif unlock_requires:
            node["requires"] = unlock_requires
        elif "requires" in node and "-XX:+UnlockCommercialFeatures" in node["requires"]:
            commercial_only = [
                req for req in node["requires"] if req != "-XX:+UnlockCommercialFeatures"
            ]
            if commercial_only:
                node["requires"] = commercial_only
            else:
                node.pop("requires", None)
            removed_commercial += 1

        if internal in ACCEPT_NON_ZERO_EXIT:
            node["acceptNonZeroExit"] = True

        node.pop("testable", None)

    CATALOG.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(f"Enriched {enriched} flags with testValue in {CATALOG.name}")
    print(f"Removed UnlockCommercialFeatures from {removed_commercial} flags")
    print(f"Left testable:false for {skipped} flags")
    if missing_snapshot:
        print(f"Missing from snapshot ({len(missing_snapshot)}): {missing_snapshot[:10]}")


if __name__ == "__main__":
    main()
