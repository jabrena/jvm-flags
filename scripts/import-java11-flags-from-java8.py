#!/usr/bin/env python3
"""Import PrintFlagsFinal flags missing from java-11.json using java-8.json metadata."""

from __future__ import annotations

import json
import re
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA11_CATALOG = ROOT / "src/main/resources/java-11.json"
JAVA8_CATALOG = ROOT / "src/main/resources/java-8.json"
SNAPSHOT = ROOT / "src/test/resources/printflagsfinal/java11.md"
NAMESPACE = uuid.UUID("6ba7b810-9dad-11d1-80b4-00c04fd430c8")

FLAG_LINE = re.compile(
    r"\s+(?P<type>bool|intx|uintx|ccstr|ccstrlist|double|size_t)\s+"
    r"(?P<name>\S+)\s+(?:=|:=)\s+(?P<value>\S*)\s+\{"
)

# Subcategories present in java-8 but not yet in java-11 (needed for imported edges).
EXTRA_SUBCATEGORIES = [
    {
        "id": "gc-cms",
        "label": "CMS",
        "type": "subcategory",
        "parent": "garbage-collection",
    },
    {
        "id": "gc-logging-legacy",
        "label": "Legacy Logging",
        "type": "subcategory",
        "parent": "garbage-collection",
    },
]

# Remap java-8 edge targets that do not exist on the java-11 graph.
CATEGORY_REMAP = {
    "logging-legacy": "logging-unified",
}

COPY_FIELDS = (
    "id",
    "label",
    "type",
    "flag",
    "description",
    "flagId",
    "testValue",
    "testArgs",
    "requires",
    "testable",
    "acceptNonZeroExit",
)


def polish_description(text: str) -> str:
    text = re.sub(r"\s+", " ", text).strip()
    if not text:
        return text
    text = text[0].upper() + text[1:]
    if text.endswith("."):
        return text
    return text + "."


def categorize(internal: str) -> str:
    """Return a category or subcategory id (aligned with categorize-pending-java8-flags.py)."""
    n = internal
    if n.startswith("AdaptiveSize") or n.startswith("UseAdaptiveSize"):
        return "garbage-collection"
    if "BiasedLock" in n:
        return "sync-monitors"
    if n == "AggressiveHeap":
        return "heap"
    if n.startswith("CMS") or n.startswith("CMS_"):
        return "gc-cms"
    if n.startswith("G1"):
        return "gc-g1-tuning"
    if n.startswith(("ParGC", "ParallelGC", "ParallelOld", "PSChunk")) or n in {
        "UseParNewGC",
        "ParallelRefProcEnabled",
        "ParallelRefProcBalancingEnabled",
    }:
        return "gc-parallel"
    if n.startswith("UseSerial") or n in {
        "MarkSweepAlwaysCompactCount",
        "MarkSweepDeadRatio",
    }:
        return "gc-serial"
    gc_log_prefixes = (
        "PrintGCTask", "PrintGCCause", "PrintGCDate", "PrintGCID", "PrintGCApplication",
        "PrintHeapAt", "PrintReferenceGC", "PrintPromotion", "PrintOldPLAB", "PrintPLAB",
        "PrintTLAB", "PrintTenuring", "PrintStringDedup", "PrintStringTable",
        "PrintParallelOldGC", "PrintSafepointStatistics", "PrintJNIGC", "PrintAdaptiveSize",
        "PrintCMS", "PrintFLS", "PrintFLSCensus", "GCLogFile", "NumberOfGCLogFiles",
        "UseGCLogFileRotation", "GCTaskTimeStamp", "GCPauseInterval",
    )
    if any(n.startswith(prefix) for prefix in gc_log_prefixes):
        return "gc-logging-legacy"
    if n in {"verbosegc", "PrintGCDetails"}:
        return "gc-logging-legacy"
    gc_general = (
        "GC", "Gc", "Tenur", "Survivor", "Eden", "YoungGen", "OldGen", "OldSize", "NewSize",
        "NewRatio", "Promotion", "Scavenge", "MarkStack", "PLAB", "TLAB", "Allocation",
        "Prefetch", "SoftRef", "ExplicitGC", "DisableExplicit", "CollectGen", "Compact",
        "FreeList", "FLS", "Sweep", "CardMark", "RefProc", "RefDiscovery",
    )
    jit_exclusions = (
        "Compile", "Tier", "Inline", "Loop", "Opto", "C1", "C2", "BCEA", "Alias", "Escape",
        "Eliminate", "Profile", "Trap", "Node", "BlockLayout", "RangeCheck", "SuperWord",
        "Vector", "SSE", "Intrinsic", "MathExact", "Montgomery", "Multiply", "Square",
        "GHASH", "PopCount", "AES", "SHA",
    )
    if any(token in n for token in gc_general) and not any(token in n for token in jit_exclusions):
        if any(token in n for token in ("TLAB", "PLAB", "Allocate", "Prefetch", "Allocation")):
            return "gc-allocation"
        if any(token in n for token in ("Tenur", "Survivor", "Young", "OldGen", "NewSize", "NewRatio")):
            return "heap"
        if n.startswith("Use") and n.endswith("GC"):
            return "gc-default"
        return "garbage-collection"
    if n.startswith("PrintCompilation") or n.startswith("PrintCodeCache") or n == "PrintWarnings":
        return "jit-compilation"
    if n.startswith("PrintCommandLine") or n in {"PrintVMOptions", "PrintFlagsInitial"}:
        return "runtime"
    if n.startswith("Print") and "Class" in n:
        return "class-loading"
    if n.startswith("Print") and ("Shared" in n or "Archive" in n):
        return "class-cds"
    if n.startswith("Print"):
        return "diagnostics"
    if n.startswith("Trace"):
        if "Class" in n:
            return "class-loading"
        if "GC" in n or "Gen" in n:
            return "gc-logging-legacy"
        return "diagnostics"
    if n.startswith(("FlightRecorder", "Perf")) or n in {
        "LogJFR", "UnlockCommercialFeatures",
        "ProfilerPrintByteCodeStatistics",
        "ProfilerRecordPC",
    }:
        return "diagnostics"
    if n.startswith(("HeapDump", "ErrorFile", "OnError", "OnOutOfMemory", "CrashOn", "CreateMinidump")):
        return "diag-error-handling"
    if n.startswith("NativeMemory") or n == "PrintNMTStatistics":
        return "diag-native-memory"
    if n.startswith("C1"):
        return "jit-compilation"
    if n.startswith(("Tier", "Compile", "OnStackReplace", "BackgroundCompilation", "CICompiler")):
        return "jit-tiered" if "Tier" in n else "jit-compilation"
    if any(token in n for token in ("Inline", "Inlin", "ClipInlining", "Bimorphic", "Peel")):
        return "jit-inlining"
    if any(token in n for token in ("Escape", "Eliminate", "DoEscape", "RangeCheck", "Reassociate")):
        return "jit-escape-analysis"
    if any(
        token in n
        for token in (
            "Intrinsic", "AES", "SHA", "CRC", "SIMD", "SSE", "Neon", "LSE", "GHASH",
            "Montgomery", "MulAdd", "Multiply", "Square", "PopCount", "MathExact", "UseDivMod",
        )
    ):
        return "jit-intrinsics"
    if any(
        token in n
        for token in (
            "Opto", "Loop", "Alias", "Block", "BCEA", "Node", "Vector", "AlignVector",
            "ConditionalMove", "SuperWord", "Unroll", "Scheduling", "Bundling", "LIR",
            "TypeProfile", "TrapBased", "MaxNode", "LiveNode", "ValueMap", "InteriorEntry",
            "JumpTable", "PartialPeel", "TimeLinearScan",
        )
    ):
        return "jit-compilation"
    if n.startswith(("BiasedLock", "Monitor", "Lock", "Inflate", "Contended", "Sync")):
        return "sync-monitors"
    if any(token in n for token in ("Safepoint", "Suspend", "DeferThr", "VMThread", "PausePadding")):
        return "sync-safepoints"
    if any(token in n for token in ("Thread", "Yield", "Concurrency", "Processor", "Priority", "BindGC")):
        return "threads-synchronization"
    if any(token in n for token in ("Class", "Verify", "Bytecode", "Redefine", "Loader", "Module")):
        if any(token in n for token in ("Verify", "Bytecode")):
            return "class-verification"
        if any(token in n for token in ("Shared", "Archive", "CDS", "DumpShared")):
            return "class-cds"
        return "class-loading"
    if n.startswith(("Shared", "Archive", "RequireShared", "VerifyShared")):
        return "class-cds"
    if any(token in n for token in ("Metaspace", "Perm", "BootClassLoader")):
        return "metaspace"
    if any(token in n for token in ("Stack", "FPU", "YellowPages", "RedPages", "ShadowPages")):
        return "stack"
    if "CodeCache" in n or n.startswith("NmethodSweep"):
        return "code-cache"
    if any(token in n for token in ("CompressedOop", "ObjectAlignment", "FieldsAllocation", "CompactFields")):
        return "object-layout"
    if n.startswith("Use") and "GC" in n:
        return "garbage-collection"
    if any(token in n for token in ("NUMA", "LargePage", "RAM", "Ergo", "FootPrint", "AggressiveHeap")):
        return "heap"
    if n.startswith("MaxDirectMemory") or n == "ObjectAlignmentInBytes":
        return "heap"
    if any(token in n for token in ("DTrace", "FlightRecorder", "ManagementServer", "Attach", "DisableAttach")):
        return "diagnostics"
    if any(token in n for token in ("JNI", "JNIEnv", "JavaMonitor", "CriticalJNI")):
        return "runtime"
    if any(token in n for token in ("Security", "Endorsed", "ExtDirs")):
        return "security"
    if n.startswith("java.") or n in {"hashCode"}:
        return "system-properties"
    if n in {"Xint", "Xcomp", "Xmixed", "Xbatch"} or n.startswith("AlwaysActAs"):
        return "jit-execution-modes"
    if any(token in n for token in ("AutoBox", "AutoGC", "AdjustConcurrency")):
        return "runtime"
    return "runtime"


def flag_id_for_node(node_id: str) -> str:
    return str(uuid.uuid5(NAMESPACE, f"jvm-flags-graph:{node_id}"))


def to_internal_name(catalog_flag: str) -> str | None:
    if not catalog_flag.startswith("-XX:"):
        return None
    rest = catalog_flag[4:]
    if rest.startswith("+") or rest.startswith("-"):
        return rest[1:]
    equals = rest.find("=")
    return rest[:equals] if equals >= 0 else rest


def catalog_flag_string(flag_type: str, internal_name: str) -> str:
    if flag_type == "bool":
        return f"-XX:+{internal_name}"
    if flag_type in ("intx", "uintx", "double"):
        return f"-XX:{internal_name}=<n>"
    if flag_type == "size_t":
        return f"-XX:{internal_name}=<size>"
    return f"-XX:{internal_name}=<value>"


def node_id_for(internal_name: str) -> str:
    return f"flag-pending-{internal_name.lower()}"


def parse_snapshot(path: Path) -> dict[str, str]:
    flags: dict[str, str] = {}
    for match in FLAG_LINE.finditer(path.read_text(encoding="utf-8")):
        flags[match.group("name")] = match.group("type")
    return flags


def copy_flag_node(source: dict, internal_name: str, flag_type: str) -> dict:
    node = {key: source[key] for key in COPY_FIELDS if key in source}
    if "flagId" not in node:
        node["flagId"] = flag_id_for_node(node["id"])
    if "description" not in node or not node["description"]:
        node["description"] = polish_description(
            re.sub(r"([a-z])([A-Z])", r"\1 \2", internal_name).lower()
        )
    return node


def new_flag_node(internal_name: str, flag_type: str, categorize) -> dict:
    node_id = node_id_for(internal_name)
    flag = catalog_flag_string(flag_type, internal_name)
    return {
        "id": node_id,
        "label": flag,
        "type": "flag",
        "flag": flag,
        "description": polish_description(
            re.sub(r"([a-z])([A-Z])", r"\1 \2", internal_name).lower()
        ),
        "flagId": flag_id_for_node(node_id),
    }


def categorize_java11(internal_name: str, categorize) -> str:
    if internal_name.startswith("Shenandoah"):
        return "gc-shenandoah"
    if internal_name in {"PrintGC"}:
        return "gc-logging"
    parent = categorize(internal_name)
    return CATEGORY_REMAP.get(parent, parent)


def main() -> None:
    data = json.loads(JAVA11_CATALOG.read_text(encoding="utf-8"))
    java8 = json.loads(JAVA8_CATALOG.read_text(encoding="utf-8"))

    snapshot_flags = parse_snapshot(SNAPSHOT)
    existing_ids = {node["id"] for node in data["nodes"]}
    graph_ids = existing_ids.copy()
    mapped_internal: set[str] = set()
    for node in data["nodes"]:
        if node.get("type") != "flag":
            continue
        internal = to_internal_name(node["flag"])
        if internal:
            mapped_internal.add(internal)

    java8_by_internal: dict[str, dict] = {}
    for node in java8["nodes"]:
        if node.get("type") != "flag":
            continue
        internal = to_internal_name(node["flag"])
        if internal:
            java8_by_internal[internal] = node

    java8_edge_by_target = {edge["target"]: edge["source"] for edge in java8["edges"]}

    pending = sorted(name for name in snapshot_flags if name not in mapped_internal)
    if not pending:
        print("No pending flags to import.")
        return

    for sub in EXTRA_SUBCATEGORIES:
        if sub["id"] not in graph_ids:
            data["nodes"].append(sub)
            graph_ids.add(sub["id"])
            data["edges"].append({"source": sub["parent"], "target": sub["id"]})

    new_nodes: list[dict] = []
    new_edges: list[dict] = []
    from_java8 = 0
    new_only = 0

    for internal_name in pending:
        flag_type = snapshot_flags[internal_name]
        if internal_name in java8_by_internal:
            node = copy_flag_node(java8_by_internal[internal_name], internal_name, flag_type)
            parent = java8_edge_by_target.get(node["id"])
            if parent is None:
                parent = categorize_java11(internal_name, categorize)
            else:
                parent = CATEGORY_REMAP.get(parent, parent)
            from_java8 += 1
        else:
            node = new_flag_node(internal_name, flag_type, categorize)
            parent = categorize_java11(internal_name, categorize)
            new_only += 1

        if node["id"] in existing_ids:
            raise SystemExit(f"Duplicate node id {node['id']!r} for {internal_name!r}")

        if parent not in graph_ids:
            raise SystemExit(
                f"Unknown parent {parent!r} for {internal_name!r} (flag {node['flag']!r})"
            )

        new_nodes.append(node)
        new_edges.append({"source": parent, "target": node["id"]})
        existing_ids.add(node["id"])

    data["nodes"].extend(new_nodes)
    data["edges"].extend(new_edges)
    JAVA11_CATALOG.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(
        f"Imported {len(new_nodes)} flags into {JAVA11_CATALOG.name} "
        f"({from_java8} from java-8, {new_only} new)"
    )


if __name__ == "__main__":
    main()
