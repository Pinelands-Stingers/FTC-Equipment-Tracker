#!/usr/bin/env python3
"""Seed the Firestore `parts` collection from the scraped catalog.

Reads tools/catalog/data/parts_all.json, drops non-robot items, maps each
record to the app's Part document shape, and batch-writes it to the `parts`
collection. Bumps `catalogMeta/current.version` so app clients re-sync.

Requires a Firebase service account key (Firebase console -> Project settings
-> Service accounts -> Generate new private key). Never commit that key.

Setup:
    pip install -r tools/catalog/requirements.txt

Usage:
    python tools/catalog/seed_firestore.py --key path/to/serviceAccount.json
    python tools/catalog/seed_firestore.py --key ... --dry-run
    python tools/catalog/seed_firestore.py --key ... --prune
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from datetime import datetime, timezone

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_INPUT = os.path.join(HERE, "data", "parts_all.json")

VENDOR_MAP = {
    "andymark": "ANDYMARK",
    "gobilda": "GOBILDA",
    "rev": "REV",
}

BATCH_LIMIT = 450


def make_doc_id(vendor: str, sku: str, name: str, used: set) -> str:
    basis = (sku or name).strip().lower()
    slug = re.sub(r"[^a-z0-9]+", "-", basis).strip("-")
    if not slug:
        slug = "part"
    doc_id = f"{vendor.lower()}-{slug}"[:140]
    if doc_id not in used:
        used.add(doc_id)
        return doc_id
    suffix = 2
    while f"{doc_id}-{suffix}" in used:
        suffix += 1
    unique = f"{doc_id}-{suffix}"
    used.add(unique)
    return unique


def build_documents(records: list) -> dict:
    documents: dict = {}
    used: set = set()
    for record in records:
        if record.get("nonRobot"):
            continue
        vendor = VENDOR_MAP.get((record.get("vendor") or "").strip().lower())
        if not vendor:
            continue
        name = (record.get("name") or "").strip()
        if not name:
            continue
        sku = (record.get("sku") or "").strip()
        tags = [t for t in (record.get("tags") or []) if isinstance(t, str) and t]
        doc_id = make_doc_id(vendor, sku, name, used)
        documents[doc_id] = {
            "sku": sku,
            "name": name,
            "nameLower": name.lower(),
            "vendor": vendor,
            "category": tags[0] if tags else "",
            "tags": tags,
            "url": (record.get("url") or "").strip(),
            "ftcLegal": True,
        }
    return documents


def chunked(seq: list, size: int):
    for i in range(0, len(seq), size):
        yield seq[i : i + size]


def main() -> int:
    parser = argparse.ArgumentParser(description="Seed Firestore with the parts catalog")
    parser.add_argument("--key", required=True, help="path to the Firebase service account JSON")
    parser.add_argument("--input", default=DEFAULT_INPUT, help="path to parts_all.json")
    parser.add_argument("--dry-run", action="store_true", help="parse and report, write nothing")
    parser.add_argument("--prune", action="store_true", help="delete Firestore parts not in the input")
    parser.add_argument("--version", type=int, default=0, help="catalog version (0 = current timestamp)")
    args = parser.parse_args()

    with open(args.input, "r", encoding="utf-8") as fh:
        records = json.load(fh)

    documents = build_documents(records)
    version = args.version or int(datetime.now(timezone.utc).timestamp())
    print(f"input records  : {len(records)}")
    print(f"robot parts    : {len(documents)}")
    print(f"catalog version: {version}")

    if args.dry_run:
        print("dry run - nothing written")
        return 0

    import firebase_admin
    from firebase_admin import credentials, firestore

    firebase_admin.initialize_app(credentials.Certificate(args.key))
    db = firestore.client()
    collection = db.collection("parts")

    items = list(documents.items())
    written = 0
    for group in chunked(items, BATCH_LIMIT):
        batch = db.batch()
        for doc_id, data in group:
            batch.set(collection.document(doc_id), data)
        batch.commit()
        written += len(group)
        print(f"  wrote {written}/{len(items)}")

    if args.prune:
        existing = {doc.id for doc in collection.stream()}
        stale = existing - set(documents)
        removed = 0
        for group in chunked(sorted(stale), BATCH_LIMIT):
            batch = db.batch()
            for doc_id in group:
                batch.delete(collection.document(doc_id))
            batch.commit()
            removed += len(group)
        print(f"pruned {removed} stale parts")

    db.document("catalogMeta/current").set(
        {
            "version": version,
            "count": len(documents),
            "updatedAt": datetime.now(timezone.utc).isoformat(),
        }
    )
    print(f"done: seeded {len(documents)} parts, catalogMeta version={version}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
