#!/usr/bin/env python3
"""Generate per-JDK JVM flags graph JSON from java-25.json template."""

import copy
import json
import uuid
from pathlib import Path

NAMESPACE = uuid.UUID("6ba7b810-9dad-11d1-80b4-00c04fd430c8")


def flag_id_for_node(node_id: str) -> str:
    return str(uuid.uuid5(NAMESPACE, f"jvm-flags-graph:{node_id}"))

ROOT = Path(__file__).resolve().parents[1]
TEMPLATE = ROOT / "docs/json/java-25.json"
OUT_DIR = ROOT / "docs/json"

# Node IDs to drop per version (categories, subcategories, flags).
REMOVE_BY_VERSION: dict[int, set[str]] = {
    8: {
        "containers", "gc-logging",
        "gc-zgc", "gc-shenandoah",
        "flag-softmaxheapsize", "flag-usecompactobjectheaders",
        "flag-usezgc", "flag-zgenerational", "flag-zuncommitdelay",
        "flag-useshenandoahgc", "flag-shenandoahgcheuristics", "flag-shenandoahuncommitdelay",
        "flag-useepsilongc",
        "flag-initialrampercentage", "flag-minrampercentage", "flag-maxrampercentage",
        "flag-initialrampercentage-containers", "flag-minrampercentage-containers",
        "flag-maxrampercentage-containers",
        "flag-usecontainersupport", "flag-activeprocessorcount",
        "flag-xlog-gc", "flag-xlog-gc-star", "flag-xlog-gc-file",
        "flag-xlog-class-load", "flag-xlog-safepoint", "flag-xlog-os",
        "flag-xlog-os-container", "flag-xlog-jit-compilation", "flag-xlog-cds",
        "flag-archiveclassesatexit", "flag-dumploadedclasslist",
        "flag-module-path", "flag-add-modules", "flag-add-exports",
        "flag-add-opens", "flag-limit-modules", "flag-patch-module",
        "flag-java-security-manager", "flag-usevectorizedmismatchintrinsic",
        "flag-useheavymonitors", "flag-monitordeflationmax", "flag-monitoruseddeflationthreshold",
        "flag-startflightrecording", "flag-flightrecorderoptions", "flag-oldobjectsamplesize",
        "diag-flight-recorder",
        "flag-nonprofiledcodeheapsize", "flag-profiledcodeheapsize", "flag-nonnmethodcodeheapsize",
    },
    11: {
        "flag-usecompactobjectheaders", "flag-zgenerational",
        "flag-archiveclassesatexit",
        "flag-usevectorizedmismatchintrinsic",
        "flag-useheavymonitors", "flag-monitordeflationmax", "flag-monitoruseddeflationthreshold",
        "flag-oldobjectsamplesize",
    },
    17: {
        "flag-usecompactobjectheaders", "flag-zgenerational",
    },
    21: {
        "flag-usecompactobjectheaders",
    },
}

# Extra nodes/edges per version.
EXTRA: dict[int, dict] = {
    8: {
        "nodes": [
            {"id": "gc-cms", "label": "CMS", "type": "subcategory", "parent": "garbage-collection"},
            {"id": "gc-logging-legacy", "label": "Legacy Logging", "type": "subcategory", "parent": "garbage-collection"},
            {"id": "logging-legacy", "label": "Legacy Logging", "type": "subcategory", "parent": "logging"},
            {"id": "flag-useconcmarksweepgc", "label": "-XX:+UseConcMarkSweepGC", "type": "flag", "flag": "-XX:+UseConcMarkSweepGC"},
            {"id": "flag-cmsinitiatingoccupancyfraction", "label": "-XX:CMSInitiatingOccupancyFraction=<n>", "type": "flag", "flag": "-XX:CMSInitiatingOccupancyFraction=<n>"},
            {"id": "flag-usecmsparallelremark", "label": "-XX:+UseCMSParallelRemark", "type": "flag", "flag": "-XX:+UseCMSParallelRemark"},
            {"id": "flag-verbosegc", "label": "-verbose:gc", "type": "flag", "flag": "-verbose:gc"},
            {"id": "flag-printgcdetails", "label": "-XX:+PrintGCDetails", "type": "flag", "flag": "-XX:+PrintGCDetails"},
            {"id": "flag-printgctimestamps", "label": "-XX:+PrintGCTimeStamps", "type": "flag", "flag": "-XX:+PrintGCTimeStamps"},
            {"id": "flag-xloggc", "label": "-Xloggc:<file>", "type": "flag", "flag": "-Xloggc:<file>"},
            {"id": "flag-permsize", "label": "-XX:PermSize=<size>", "type": "flag", "flag": "-XX:PermSize=<size>"},
            {"id": "flag-maxpermsize", "label": "-XX:MaxPermSize=<size>", "type": "flag", "flag": "-XX:MaxPermSize=<size>"},
            {"id": "flag-security-manager", "label": "-Djava.security.manager", "type": "flag", "flag": "-Djava.security.manager"},
        ],
        "edges": [
            {"source": "garbage-collection", "target": "gc-cms"},
            {"source": "garbage-collection", "target": "gc-logging-legacy"},
            {"source": "logging", "target": "logging-legacy"},
            {"source": "gc-cms", "target": "flag-useconcmarksweepgc"},
            {"source": "gc-cms", "target": "flag-cmsinitiatingoccupancyfraction"},
            {"source": "gc-cms", "target": "flag-usecmsparallelremark"},
            {"source": "gc-logging-legacy", "target": "flag-verbosegc"},
            {"source": "gc-logging-legacy", "target": "flag-printgcdetails"},
            {"source": "gc-logging-legacy", "target": "flag-printgctimestamps"},
            {"source": "gc-logging-legacy", "target": "flag-xloggc"},
            {"source": "metaspace", "target": "flag-permsize"},
            {"source": "metaspace", "target": "flag-maxpermsize"},
            {"source": "security", "target": "flag-security-manager"},
        ],
        "relabel": {
            "gc-default": "Parallel (default)",
            "flag-useg1gc": "-XX:+UseG1GC (available, not default)",
        },
    },
    11: {
        "nodes": [],
        "edges": [],
        "relabel": {
            "gc-zgc": "ZGC (experimental)",
        },
    },
    17: {
        "nodes": [],
        "edges": [],
        "relabel": {
            "flag-java-security-manager": "-Djava.security.manager (deprecated)",
        },
    },
    21: {
        "nodes": [],
        "edges": [],
        "relabel": {
            "gc-zgc": "ZGC",
        },
    },
}


def filter_graph(data: dict, version: int) -> dict:
    out = copy.deepcopy(data)
    remove = set(REMOVE_BY_VERSION.get(version, set()))
    extra = EXTRA.get(version, {})
    remove |= set(extra.get("remove", set()))

    out["nodes"] = [n for n in out["nodes"] if n["id"] not in remove]
    out["edges"] = [e for e in out["edges"] if e["source"] not in remove and e["target"] not in remove]

    relabel = extra.get("relabel", {})
    for node in out["nodes"]:
        if node["id"] in relabel:
            node["label"] = relabel[node["id"]]

    for node in extra.get("nodes", []):
        if node.get("type") == "flag" and "flagId" not in node:
            node["flagId"] = flag_id_for_node(node["id"])
        out["nodes"].append(node)
    out["edges"].extend(extra.get("edges", []))

    # Version-specific root / metadata
    out["metadata"] = {
        "title": f"Java {version} JVM Flags",
        "version": str(version),
        "description": "Graph representation of JVM flags organized by category",
    }
    for node in out["nodes"]:
        if node["id"] == "root":
            node["label"] = f"Java {version} JVM Flags"

    return out


def main() -> None:
    template = json.loads(TEMPLATE.read_text(encoding="utf-8"))
    for version in (8, 11, 17, 21):
        path = OUT_DIR / f"java-{version}.json"
        graph = filter_graph(template, version)
        path.write_text(json.dumps(graph, indent=2) + "\n", encoding="utf-8")
        nodes = len(graph["nodes"])
        edges = len(graph["edges"])
        print(f"Wrote {path.name}: {nodes} nodes, {edges} edges")


if __name__ == "__main__":
    main()
