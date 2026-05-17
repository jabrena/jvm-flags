#!/usr/bin/env python3
"""
Ensure every java-*.json catalog contains the full canonical taxonomy from
catalog-taxonomy.json (domains, categories, subcategories, and structure edges).

Hierarchy: root -> domain -> category -> subcategory -> flag
Empty containers are allowed in JSON; the site hides those without flags.

Run from project root: python3 scripts/sync-catalog-taxonomy.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
RESOURCES = PROJECT_ROOT / "src" / "main" / "resources"
TAXONOMY_PATH = RESOURCES / "catalog-taxonomy.json"
VERSIONS = (8, 11, 17, 21, 25)


def load_taxonomy() -> tuple[list[dict], list[dict], list[dict]]:
    with TAXONOMY_PATH.open(encoding="utf-8") as f:
        data = json.load(f)
    return data["domains"], data["categories"], data["subcategories"]


def first_flag_index(nodes: list[dict]) -> int:
    for i, node in enumerate(nodes):
        if node.get("type") == "flag":
            return i
    return len(nodes)


def root_index(nodes: list[dict]) -> int:
    for i, node in enumerate(nodes):
        if node.get("type") == "root":
            return i
    return 0


def sync_catalog(
    path: Path,
    domains: list[dict],
    categories: list[dict],
    subcategories: list[dict],
) -> int:
    with path.open(encoding="utf-8") as f:
        catalog = json.load(f)

    nodes = catalog["nodes"]
    edges = catalog["edges"]
    node_ids = {n["id"] for n in nodes}
    edge_pairs = {(e["source"], e["target"]) for e in edges}

    changes = 0
    insert_at = first_flag_index(nodes)
    root_at = root_index(nodes) + 1

    for domain in domains:
        did = domain["id"]
        if did not in node_ids:
            nodes.insert(root_at, {"id": did, "label": domain["label"], "type": "domain"})
            root_at += 1
            insert_at += 1
            node_ids.add(did)
            changes += 1
        else:
            for node in nodes:
                if node["id"] == did and node.get("label") != domain["label"]:
                    node["label"] = domain["label"]
                    changes += 1
                    break

    for cat in categories:
        cid = cat["id"]
        parent = cat["parent"]
        if cid not in node_ids:
            nodes.insert(
                insert_at,
                {"id": cid, "label": cat["label"], "type": "category", "parent": parent},
            )
            insert_at += 1
            node_ids.add(cid)
            changes += 1
        else:
            for node in nodes:
                if node["id"] == cid:
                    if node.get("label") != cat["label"]:
                        node["label"] = cat["label"]
                        changes += 1
                    if node.get("parent") != parent:
                        node["parent"] = parent
                        changes += 1
                    break

    for sub in subcategories:
        sid = sub["id"]
        if sid not in node_ids:
            nodes.insert(
                insert_at,
                {
                    "id": sid,
                    "label": sub["label"],
                    "type": "subcategory",
                    "parent": sub["parent"],
                },
            )
            insert_at += 1
            node_ids.add(sid)
            changes += 1
        else:
            for node in nodes:
                if node["id"] == sid:
                    if node.get("label") != sub["label"]:
                        node["label"] = sub["label"]
                        changes += 1
                    if node.get("parent") != sub["parent"]:
                        node["parent"] = sub["parent"]
                        changes += 1
                    break

    category_ids = {c["id"] for c in categories}
    domain_ids = {d["id"] for d in domains}

    # Remove legacy root -> category edges (replaced by domain -> category).
    new_edges = []
    for edge in edges:
        pair = (edge["source"], edge["target"])
        if edge["source"] == "root" and edge["target"] in category_ids:
            changes += 1
            continue
        new_edges.append(edge)
    edges[:] = new_edges
    edge_pairs = {(e["source"], e["target"]) for e in edges}

    for domain in domains:
        pair = ("root", domain["id"])
        if pair not in edge_pairs:
            edges.append({"source": "root", "target": domain["id"]})
            edge_pairs.add(pair)
            changes += 1

    for cat in categories:
        pair = (cat["parent"], cat["id"])
        if pair not in edge_pairs:
            edges.append({"source": cat["parent"], "target": cat["id"]})
            edge_pairs.add(pair)
            changes += 1

    for sub in subcategories:
        pair = (sub["parent"], sub["id"])
        if pair not in edge_pairs:
            edges.append({"source": sub["parent"], "target": sub["id"]})
            edge_pairs.add(pair)
            changes += 1

    if changes:
        with path.open("w", encoding="utf-8") as f:
            json.dump(catalog, f, indent=2, ensure_ascii=False)
            f.write("\n")

    return changes


def main() -> int:
    if not TAXONOMY_PATH.is_file():
        print(f"Missing taxonomy: {TAXONOMY_PATH}", file=sys.stderr)
        return 1

    domains, categories, subcategories = load_taxonomy()
    total = 0
    for version in VERSIONS:
        path = RESOURCES / f"java-{version}.json"
        if not path.is_file():
            print(f"Skip missing {path.name}")
            continue
        n = sync_catalog(path, domains, categories, subcategories)
        print(f"{path.name}: {n} change(s)")
        total += n

    print(f"Done ({total} total changes).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
