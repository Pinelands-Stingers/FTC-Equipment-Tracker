#!/usr/bin/env python3
"""Scrape the sellable catalog from AndyMark, goBILDA and REV Robotics.

Outputs (tools/catalog/data/):
  parts_andymark.json / parts_gobilda.json / parts_rev.json
  parts_all.json / parts_all.csv
  meta.json

One row per variant/SKU. Each row: sku, name, vendor, url, nonRobot,
categories (vendor breadcrumb/type, verbatim), tags (normalized vocabulary).

Only the Python standard library is required.

Usage:
  python scrape_catalog.py                 # all vendors
  python scrape_catalog.py --vendor rev    # one vendor
  python scrape_catalog.py --force         # ignore cached state
  python scrape_catalog.py --delay 1.0     # seconds between page fetches
"""

from __future__ import annotations

import argparse
import csv
import gzip
import html as html_mod
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone

HERE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(HERE, "data")
STATE_DIR = os.path.join(OUT_DIR, ".state")

USER_AGENT = (
    "FTC-Equipment-Tracker-catalog/1.0 "
    "(FTC team 25310; parts catalog seed; contact: team@pinelands-stingers.example)"
)

VENDORS = {
    "andymark": "https://andymark.com",
    "gobilda": "https://www.gobilda.com",
    "rev": "https://www.revrobotics.com",
}

SITE_LABEL = {
    "andymark": "AndyMark",
    "gobilda": "goBILDA",
    "rev": "REV",
}

# ---------------------------------------------------------------------------
# HTTP
# ---------------------------------------------------------------------------


class NotFound(Exception):
    pass


def http_get(url: str, delay: float = 0.0, retries: int = 4, timeout: int = 45) -> tuple[str, str]:
    """GET a URL, returning (final_url, decoded_text). Raises NotFound on 404/410."""
    last_exc: Exception | None = None
    for attempt in range(retries):
        if delay and attempt == 0:
            time.sleep(delay)
        req = urllib.request.Request(
            url,
            headers={
                "User-Agent": USER_AGENT,
                "Accept": "text/html,application/json;q=0.9,*/*;q=0.8",
                "Accept-Encoding": "gzip",
                "Accept-Language": "en-US,en;q=0.9",
            },
        )
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                raw = resp.read()
                if resp.headers.get("Content-Encoding", "").lower() == "gzip":
                    raw = gzip.decompress(raw)
                return resp.geturl(), raw.decode("utf-8", "replace")
        except urllib.error.HTTPError as exc:
            if exc.code in (404, 410):
                raise NotFound(url) from exc
            last_exc = exc
            if exc.code in (429, 500, 502, 503, 504) and attempt < retries - 1:
                time.sleep(min(30.0, (2 ** attempt) * 2.0))
                continue
            raise
        except (urllib.error.URLError, TimeoutError) as exc:
            last_exc = exc
            if attempt < retries - 1:
                time.sleep(min(30.0, (2 ** attempt) * 2.0))
                continue
            raise
    raise last_exc if last_exc else RuntimeError(url)


# ---------------------------------------------------------------------------
# Text helpers
# ---------------------------------------------------------------------------

_TITLE_RE = re.compile(r"<title[^>]*>(.*?)</title>", re.S | re.I)
_REFRESH_RE = re.compile(
    r"<meta[^>]*http-equiv=[\"']?refresh[\"']?[^>]*content=[\"'](.*?)[\"']", re.S | re.I
)
_LD_RE = re.compile(
    r"<script[^>]*application/ld\+json[^>]*>(.*?)</script>", re.S | re.I
)
_H1_RE = re.compile(
    r"<h1[^>]*class=[\"'][^\"']*productView-title[^\"']*[\"'][^>]*>(.*?)</h1>", re.S | re.I
)
_H1_GENERIC_RE = re.compile(r"<h1[^>]*>(.*?)</h1>", re.S | re.I)
_CARD_TITLE_RE = re.compile(
    r"<h4[^>]*class=[\"'][^\"']*card-title[^\"']*[\"'][^>]*>(.*?)</h4>", re.S | re.I
)
_JSCONTEXT_RE = re.compile(r"window\.jsContext\s*=\s*JSON\.parse\(\"(.*?)\"\);", re.S)
_BCDRe = re.compile(r"var BCData\s*=\s*(\{.*?\});", re.S)


def clean_text(value) -> str:
    if value is None:
        return ""
    text = str(value)
    text = html_mod.unescape(text)
    text = text.replace("\ufffd", " ")
    for ch in ("\u2122", "\u00ae", "\u00a9", "\u2019"):
        text = text.replace(ch, "" if ch in ("\u2122", "\u00ae", "\u00a9") else "'")
    text = text.replace("\\/", "/").replace("\\\"", '"')
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def strip_site_suffix(name: str, vendor: str) -> str:
    label = SITE_LABEL.get(vendor, "")
    if not label:
        return name
    name = re.sub(r"\s*[-|]\s*" + re.escape(label) + r"\s*$", "", name, flags=re.I)
    return name.strip()


def find_refresh_target(page: str) -> str | None:
    m = _REFRESH_RE.search(page)
    if not m:
        return None
    content = m.group(1)
    um = re.search(r"url\s*=\s*(.+)$", content, re.I | re.S)
    if not um:
        return None
    return html_mod.unescape(um.group(1).strip().strip("'\""))


def page_title(page: str) -> str:
    m = _TITLE_RE.search(page)
    return clean_text(m.group(1)) if m else ""


# ---------------------------------------------------------------------------
# JSON-LD parsing
# ---------------------------------------------------------------------------


def iter_ld_objects(page: str):
    for m in _LD_RE.finditer(page):
        raw = m.group(1).strip()
        try:
            data = json.loads(raw)
        except Exception:
            continue
        yield from _walk(data)


def _walk(node):
    if isinstance(node, dict):
        yield node
        for value in node.values():
            yield from _walk(value)
    elif isinstance(node, list):
        for item in node:
            yield from _walk(item)


def ld_product(page: str) -> dict | None:
    for obj in iter_ld_objects(page):
        if obj.get("@type") == "Product":
            return obj
    return None


def ld_breadcrumb(page: str) -> list[str]:
    for obj in iter_ld_objects(page):
        if obj.get("@type") == "BreadcrumbList":
            names = []
            items = obj.get("itemListElement")
            if isinstance(items, list):
                for element in items:
                    if not isinstance(element, dict):
                        continue
                    item = element.get("item")
                    if isinstance(item, dict) and item.get("name"):
                        names.append(clean_text(item["name"]))
                    elif element.get("name"):
                        names.append(clean_text(element["name"]))
            return names
    return []


def parse_jscontext(page: str) -> dict | None:
    m = _JSCONTEXT_RE.search(page)
    if not m:
        return None
    try:
        return json.loads(json.loads('"' + m.group(1) + '"'))
    except Exception:
        return None


def parse_bcdata(page: str) -> dict | None:
    m = _BCDRe.search(page)
    if not m:
        return None
    try:
        return json.loads(m.group(1))
    except Exception:
        return None


# ---------------------------------------------------------------------------
# Tagging
# ---------------------------------------------------------------------------

TAG_RULES: list[tuple[str, re.Pattern]] = [
    ("motor", re.compile(r"\b(gear ?motor|motor|brushless|brushed)\b", re.I)),
    ("gearbox", re.compile(r"(gearbox|gear box|planetary|super ?sport|ultra ?planetary|max ?planetary|versaplanetary|shark|stingray)", re.I)),
    ("servo", re.compile(r"\bservo", re.I)),
    ("bearing", re.compile(r"\bbearings?\b|pillow block|linear bushing", re.I)),
    ("gear", re.compile(r"\bgears?\b|pinion|miter|worm|bevel|rack|20 ?dp|32 ?dp|mod ?[01]", re.I)),
    ("sprocket", re.compile(r"\bsprockets?\b|sprocket", re.I)),
    ("chain", re.compile(r"\bchain\b|\bchains\b", re.I)),
    ("belt", re.compile(r"\bbelts?\b|timing belt|htd|gt2|gt3", re.I)),
    ("pulley", re.compile(r"\bpulleys?\b", re.I)),
    ("wheel", re.compile(r"\bwheels?\b|\bomni\b|mecanum|traction|compliant|grip wheel|flap wheel|conveyor|caster|\brollers?\b", re.I)),
    ("hub", re.compile(r"\bhubs?\b", re.I)),
    ("shaft", re.compile(r"\bshafts?\b|shafting|d-?shaft|rex shaft|hex shaft|lead screw|acme", re.I)),
    ("extrusion", re.compile(r"extrusion|\bchannel\b|u-?channel|c-?channel|\bbeams?\b|gorail|go ?tube|maxtube|\btubes?\b|maxspline", re.I)),
    ("bracket", re.compile(r"\bbrackets?\b|\bgusset\b", re.I)),
    ("plate", re.compile(r"\bplates?\b|pattern plate|grid plate|\bpanel\b|\bsheets?\b|\bhdpe\b|\bacm\b|polycarbonate sheet|carbon fiber|composite", re.I)),
    ("screw", re.compile(r"\bscrews?\b|socket head|button head|cap screw|thread-?forming|rivet", re.I)),
    ("nut", re.compile(r"\bnuts?\b|nut strip|\bnut\b", re.I)),
    ("washer", re.compile(r"\bwashers?\b|\bshims?\b", re.I)),
    ("spacer", re.compile(r"\bspacers?\b", re.I)),
    ("standoff", re.compile(r"\bstandoffs?\b", re.I)),
    ("collar", re.compile(r"\bcollars?\b|clamping collar", re.I)),
    ("coupler", re.compile(r"coupler|universal joint|u-?joint|cv joint", re.I)),
    ("sensor", re.compile(r"\bsensors?\b|encoder|gyro|imu|odometry|limit switch|ultrasonic|distance sensor|color sensor|beam break|potentiometer", re.I)),
    ("camera", re.compile(r"\bcameras?\b|limelight|webcam|usb camera|vision", re.I)),
    ("controller", re.compile(r"\bcontrollers?\b|motor commander|motor controller|spark|roboclaw|talon|victor|speed controller", re.I)),
    ("power", re.compile(r"power distribution|voltage regulator|\bbec\b|circuit breaker|\bfuses?\b|battery|batteries|charger|ni-?mh|li-?po", re.I)),
    ("cable", re.compile(r"\bcables?\b|\bwires?\b|harness|jumper wire|extension cable|adaptor|adapter|connector|power ?pole|anderson|xt30|xt60|xt90|tjc8", re.I)),
    ("pneumatics", re.compile(r"pneumatic|solenoid|air cylinder|regulator", re.I)),
    ("spring", re.compile(r"\bsprings?\b|\bshocks?\b|damper|gas strut", re.I)),
    ("pin", re.compile(r"\bpins?\b|dowel|\bclevis\b|quick release", re.I)),
    ("clip", re.compile(r"\bclips?\b|\bclamps?\b|retaining ring|circlip|e-?clip|\bhose clamp\b", re.I)),
    ("bushing", re.compile(r"\bbushings?\b|\bsleeve\b|flanged bushing", re.I)),
    ("tubing", re.compile(r"\btubing\b|\bhose\b|\bcord\b|surgical|ptfe|round belt", re.I)),
    ("key", re.compile(r"machine key|woodruff|\bkeys?\b(?!\s*(?:hex|allen|wrench))", re.I)),
    ("magnet", re.compile(r"\bmagnets?\b", re.I)),
    ("adhesive", re.compile(r"\badhesive\b|\btape\b|velcro|\bglue\b|thread ?locker|grease|lubricant", re.I)),
    ("foam", re.compile(r"\bfoam\b|\brubber\b|grommet|\bfeet\b", re.I)),
    ("light", re.compile(r"\bleds?\b|\blights?\b|headlight|light strip|addressable", re.I)),
    ("hinge", re.compile(r"\bhinges?\b", re.I)),
    ("plug", re.compile(r"\bplugs?\b|tube cap|end ?cap|\bcaps?\b(?!\s*(?:screw|head))", re.I)),
    ("fastener", re.compile(r"\bscrews?\b|\bnuts?\b|\bbolts?\b|\bwashers?\b|\brivets?\b", re.I)),
    ("tool", re.compile(r"\btools?\b|\bwrench\b|screwdriver|hex key|wera|grease|lubricant|thread ?locker|drill bit|plier|caliper|tape", re.I)),
    ("kit", re.compile(r"\bkits?\b|\bbundles?\b", re.I)),
    ("merch", re.compile(r"\bt-?shirt\b|hoodie|lanyard|plush|plushie|sticker|\bmerch\b|\bhats?\b|apparel|keychain|baseball cap|snapback", re.I)),
    ("misc", re.compile(r".*")),
]

NON_ROBOT_RE = re.compile(
    r"\bt-?shirt\b|hoodie|lanyard|plush|plushie|sticker|\bmerch\b|\bmerchandise\b|"
    r"\bhats?\b|apparel|keychain|\bmug\b|water bottle|baseball cap|snapback",
    re.I,
)


def derive_tags(text: str) -> list[str]:
    tags: list[str] = []
    for tag, pattern in TAG_RULES:
        if tag == "misc":
            continue
        if pattern.search(text):
            tags.append(tag)
    if not tags:
        tags.append("misc")
    return sorted(set(tags))


def is_non_robot(name: str, categories: list[str], tags: list[str]) -> bool:
    joined = " ".join([name] + categories)
    if "merch" in tags:
        return True
    return bool(NON_ROBOT_RE.search(joined))


def finish_record(
    vendor: str, sku: str, name: str, url: str, categories: list[str], tag_basis: str = ""
) -> dict:
    sku = clean_text(sku)
    name = strip_site_suffix(clean_text(name), vendor)
    cats = [c for c in (clean_text(c) for c in categories) if c]
    basis = clean_text(tag_basis) or name
    tags = derive_tags(basis)
    if tags == ["misc"] and cats:
        tags = derive_tags(basis + " " + " ".join(cats))
    return {
        "sku": sku,
        "name": name,
        "vendor": vendor,
        "url": url,
        "nonRobot": is_non_robot(name, cats, tags),
        "categories": cats,
        "tags": tags,
    }


# ---------------------------------------------------------------------------
# AndyMark (Shopify products.json)
# ---------------------------------------------------------------------------


def scrape_andymark(delay: float, force: bool, errors: list) -> list[dict]:
    vendor = "andymark"
    records: list[dict] = []
    page = 1
    seen: set[str] = set()
    while True:
        url = f"{VENDORS[vendor]}/products.json?limit=250&page={page}"
        try:
            _, text = http_get(url, delay=delay if page > 1 else 0.0)
        except Exception as exc:  # noqa: BLE001
            errors.append(f"{vendor}: {url}: {exc}")
            break
        try:
            products = json.loads(text).get("products", [])
        except Exception as exc:  # noqa: BLE001
            errors.append(f"{vendor}: {url}: bad JSON ({exc})")
            break
        if not products:
            break
        for product in products:
            handle = product.get("handle") or ""
            title = product.get("title") or ""
            product_type = clean_text(product.get("product_type") or "")
            tags = [clean_text(t) for t in (product.get("tags") or []) if clean_text(t)]
            categories = ([product_type] if product_type else []) + tags
            link = f"{VENDORS[vendor]}/products/{handle}"
            variants = product.get("variants") or []
            if not variants:
                variants = [{"sku": None, "title": "Default Title"}]
            multi = len(variants) > 1
            for variant in variants:
                sku = variant.get("sku") or handle
                vtitle = clean_text(variant.get("title") or "")
                name = title
                if multi and vtitle and vtitle.lower() != "default title":
                    name = f"{title} - {vtitle}"
                key = f"{vendor}:{sku.lower()}"
                if key in seen:
                    continue
                seen.add(key)
                records.append(finish_record(vendor, sku, name, link, categories))
        page += 1
    return records


# ---------------------------------------------------------------------------
# BigCommerce (goBILDA + REV): sitemap -> product pages
# ---------------------------------------------------------------------------


def product_sitemap_urls(vendor: str, delay: float, errors: list) -> list[str]:
    url = f"{VENDORS[vendor]}/xmlsitemap.php?type=products&page=1"
    try:
        _, text = http_get(url, delay=0.0)
    except Exception as exc:  # noqa: BLE001
        errors.append(f"{vendor}: {url}: {exc}")
        return []
    locs = re.findall(r"<loc>(.*?)</loc>", text)
    locs = [clean_text(l) for l in locs]
    if vendor == "gobilda":
        locs = [l for l in locs if "/rd-" not in l]
    return locs


def extract_bigcommerce_product(vendor: str, page: str, url: str) -> dict | None:
    """Return a record, or None if the page is not a purchasable product."""
    # 1. meta-refresh redirect stub (goBILDA variant links / category links)
    target = find_refresh_target(page)
    if target is not None:
        absolute = urllib.parse.urljoin(url, target)
        query = urllib.parse.parse_qs(urllib.parse.urlparse(absolute).query)
        sku_values = query.get("sku")
        if not sku_values or not sku_values[0].strip():
            return None  # redirect to a category/anchor, not a product
        sku = sku_values[0].strip()
        return {
            "kind": "product",
            "sku": sku,
            "name": page_title(page) or sku,
            "categories": [],
            "resolve_categories_from": urllib.parse.urlunparse(
                urllib.parse.urlparse(absolute)._replace(query="", fragment="")
            ),
        }

    # 2. JSON-LD Product with a real string SKU
    product = ld_product(page)
    if product:
        sku = product.get("sku")
        if isinstance(sku, str) and sku.strip() and not sku.strip().isdigit():
            name = clean_text(product.get("name")) or page_title(page)
            return {
                "kind": "product",
                "sku": sku.strip(),
                "name": name,
                "categories": _clean_breadcrumb(ld_breadcrumb(page), name),
            }

    # 3. goBILDA jsContext product
    if vendor == "gobilda":
        ctx = parse_jscontext(page)
        if ctx:
            p = ctx.get("product") or {}
            sku = p.get("sku")
            if isinstance(sku, str) and sku.strip():
                name = clean_text(p.get("title")) or page_title(page)
                categories: list[str] = []
                for c in p.get("category") or []:
                    categories.extend(part.strip() for part in str(c).split("/") if part.strip())
                return {"kind": "product", "sku": sku.strip(), "name": name, "categories": categories}

        # goBILDA family pages carry no JSON-LD/jsContext: per-variant SKUs live in
        # <h4 class="card-title">Name</h4> ... data-product-sku="SKU" cards.
        cards = gobilda_variant_cards(page)
        if cards:
            base = gobilda_h1(page) or page_title(page)
            if len(cards) == 1:
                return {"kind": "product", "sku": cards[0][0], "name": base, "categories": []}
            variants = [
                {"sku": sku, "name": base if not vname else f"{base} - {vname}"}
                for sku, vname in cards
            ]
            return {
                "kind": "variants",
                "variants": variants,
                "categories": [],
                "tag_basis": base,
            }

    # 4. REV: BCData product, or option/family page that must be expanded
    if vendor == "rev":
        bc = parse_bcdata(page)
        attrs = (bc or {}).get("product_attributes") or {}
        sku = attrs.get("sku")
        if isinstance(sku, str) and sku.strip():
            name = page_title(page)
            m = _H1_RE.search(page)
            if m:
                name = clean_text(m.group(1)) or name
            return {
                "kind": "product",
                "sku": sku.strip(),
                "name": name,
                "categories": _clean_breadcrumb(ld_breadcrumb(page), name),
            }
        variants = rev_variants(page, attrs)
        if variants:
            parent = clean_text((ld_product(page) or {}).get("name")) or page_title(page)
            return {
                "kind": "variants",
                "variants": variants,
                "categories": _clean_breadcrumb(ld_breadcrumb(page), parent),
                "tag_basis": parent,
            }
    return None


_REV_LABEL_RE = re.compile(
    r"<label[^>]*data-product-attribute-value=\"(\d+)\"[^>]*>(.*?)</label>", re.S | re.I
)
_REV_SKU_RE = re.compile(r"\(\s*([A-Za-z0-9][A-Za-z0-9-]{2,})\s*\)\s*$")


def rev_variants(page: str, attrs: dict) -> list[dict]:
    """Extract option variants from a REV family page, e.g. 'Name (REV-41-3334)'."""
    labels: dict[str, str] = {}
    for m in _REV_LABEL_RE.finditer(page):
        labels[m.group(1)] = clean_text(m.group(2))
    if not labels:
        return []

    chosen: list[str] = []
    variant_ids = attrs.get("available_variant_values") or []
    if variant_ids:
        for vid in variant_ids:
            text = labels.get(str(vid))
            if text:
                chosen.append(text)
    else:
        # Fall back: take labels outside add-on "product-list" attribute blocks.
        blocks = re.split(r'data-product-attribute="([^"]+)"', page)
        for i in range(1, len(blocks), 2):
            if blocks[i] == "product-list":
                continue
            content = blocks[i + 1] if i + 1 < len(blocks) else ""
            for m in _REV_LABEL_RE.finditer(content):
                text = labels.get(m.group(1))
                if text:
                    chosen.append(text)

    out: list[dict] = []
    seen: set[str] = set()
    for text in chosen:
        m = _REV_SKU_RE.search(text)
        if not m:
            continue
        sku = m.group(1).strip()
        name = _REV_SKU_RE.sub("", text).strip(" -\u2013\u2014")
        if sku and name and sku.lower() not in seen:
            seen.add(sku.lower())
            out.append({"sku": sku, "name": name})
    return out


def _clean_breadcrumb(breadcrumb: list[str], product_name: str) -> list[str]:
    if not breadcrumb:
        return []
    parts = list(breadcrumb)
    if parts and parts[0].lower() == "home":
        parts = parts[1:]
    if parts and clean_text(parts[-1]).lower() == clean_text(product_name).lower():
        parts = parts[:-1]
    return parts


def gobilda_h1(page: str) -> str:
    m = _H1_RE.search(page) or _H1_GENERIC_RE.search(page)
    return clean_text(m.group(1)) if m else ""


def gobilda_variant_cards(page: str) -> list[tuple[str, str]]:
    """Pair each per-variant SKU with the nearest preceding card title (goBILDA family pages)."""
    cards: list[tuple[str, str]] = []
    seen: set[str] = set()
    for m in re.finditer(r'data-product-sku="([^"]+)"', page):
        sku = clean_text(m.group(1))
        if not sku or sku.lower() in seen:
            continue
        seen.add(sku.lower())
        titles = _CARD_TITLE_RE.findall(page[: m.start()])
        vname = clean_text(titles[-1]) if titles else ""
        cards.append((sku, vname))
    return cards


def scrape_bigcommerce(vendor: str, delay: float, force: bool, errors: list, limit: int = 0) -> list[dict]:
    urls = product_sitemap_urls(vendor, delay, errors)
    if limit:
        urls = urls[:limit]
    state_path = os.path.join(STATE_DIR, f"{vendor}_pages.json")
    state: dict[str, dict | None] = {}
    if not force and os.path.exists(state_path):
        try:
            with open(state_path, "r", encoding="utf-8") as fh:
                state = json.load(fh)
        except Exception:
            state = {}

    page_cache: dict[str, list[str]] = {}
    pending = [u for u in urls if u not in state]
    print(f"[{vendor}] sitemap urls={len(urls)} cached={len(urls) - len(pending)} to_fetch={len(pending)}")

    for index, url in enumerate(pending, 1):
        try:
            _, page = http_get(url, delay=delay)
        except NotFound:
            state[url] = None
            continue
        except Exception as exc:  # noqa: BLE001
            errors.append(f"{vendor}: {url}: {exc}")
            continue

        try:
            descriptor = extract_bigcommerce_product(vendor, page, url)
        except Exception as exc:  # noqa: BLE001
            errors.append(f"{vendor}: {url}: parse error ({exc})")
            descriptor = None

        if descriptor is None:
            state[url] = None
        elif descriptor["kind"] == "variants":
            state[url] = [
                {
                    "sku": v["sku"],
                    "name": v["name"],
                    "categories": descriptor.get("categories", []),
                    "tag_basis": descriptor.get("tag_basis", ""),
                    "url": url,
                }
                for v in descriptor["variants"]
            ]
        else:
            categories = descriptor.get("categories", [])
            resolve_from = descriptor.get("resolve_categories_from")
            if resolve_from:
                if resolve_from not in page_cache:
                    try:
                        _, base_page = http_get(resolve_from, delay=delay)
                        base_name = page_title(base_page)
                        page_cache[resolve_from] = _clean_breadcrumb(
                            ld_breadcrumb(base_page), base_name
                        )
                    except Exception:  # noqa: BLE001
                        page_cache[resolve_from] = []
                categories = page_cache[resolve_from]
            state[url] = [
                {
                    "sku": descriptor["sku"],
                    "name": descriptor["name"],
                    "categories": categories,
                    "url": url,
                }
            ]

        if index % 25 == 0:
            _save_json(state_path, state)
            print(f"[{vendor}] {index}/{len(pending)} fetched")

    _save_json(state_path, state)

    records: list[dict] = []
    skipped = 0
    for url in urls:
        entries = state.get(url)
        if not entries:
            skipped += 1
            continue
        for entry in entries:
            records.append(
                finish_record(
                    vendor,
                    entry["sku"],
                    entry["name"],
                    url,
                    entry.get("categories", []),
                    entry.get("tag_basis", ""),
                )
            )
    print(f"[{vendor}] products={len(records)} skipped={skipped}")
    return records


# ---------------------------------------------------------------------------
# Output
# ---------------------------------------------------------------------------


def _save_json(path: str, data) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as fh:
        json.dump(data, fh, ensure_ascii=False, indent=2, sort_keys=True)
    os.replace(tmp, path)


def dedupe(records: list[dict]) -> tuple[list[dict], int]:
    seen: set[str] = set()
    result: list[dict] = []
    dupes = 0
    for record in records:
        key = f"{record['vendor']}:{(record['sku'] or '').lower()}"
        if not record["sku"]:
            key = f"{record['vendor']}:url:{record['url'].lower()}"
        if key in seen:
            dupes += 1
            continue
        seen.add(key)
        result.append(record)
    return result, dupes


def write_csv(path: str, records: list[dict]) -> None:
    columns = ["sku", "name", "vendor", "url", "nonRobot", "tags", "categories"]
    with open(path, "w", encoding="utf-8", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=columns)
        writer.writeheader()
        for record in records:
            writer.writerow(
                {
                    "sku": record["sku"],
                    "name": record["name"],
                    "vendor": record["vendor"],
                    "url": record["url"],
                    "nonRobot": "true" if record["nonRobot"] else "false",
                    "tags": "|".join(record["tags"]),
                    "categories": "|".join(record["categories"]),
                }
            )


def build_meta(records: list[dict], per_vendor_raw: dict, dupes: int, errors: list) -> dict:
    from collections import Counter

    tag_counts: Counter = Counter()
    for record in records:
        tag_counts.update(record["tags"])
    by_vendor: dict[str, dict] = {}
    for vendor in VENDORS:
        subset = [r for r in records if r["vendor"] == vendor]
        by_vendor[vendor] = {
            "products": len(subset),
            "nonRobot": sum(1 for r in subset if r["nonRobot"]),
            "missingSku": sum(1 for r in subset if not r["sku"]),
            "missingName": sum(1 for r in subset if not r["name"]),
            "sitemapUrls": per_vendor_raw.get(vendor),
        }
    return {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "totalProducts": len(records),
        "duplicatesRemoved": dupes,
        "nonRobot": sum(1 for r in records if r["nonRobot"]),
        "byVendor": by_vendor,
        "tagCounts": dict(sorted(tag_counts.items(), key=lambda kv: (-kv[1], kv[0]))),
        "errors": errors,
    }


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def main() -> int:
    parser = argparse.ArgumentParser(description="Scrape AndyMark/goBILDA/REV catalog")
    parser.add_argument("--vendor", default="all", choices=["all", *VENDORS.keys()])
    parser.add_argument("--delay", type=float, default=1.0, help="seconds between page fetches")
    parser.add_argument("--force", action="store_true", help="ignore cached page state")
    parser.add_argument("--limit", type=int, default=0, help="max sitemap URLs per BigCommerce vendor (0 = all)")
    args = parser.parse_args()

    os.makedirs(OUT_DIR, exist_ok=True)
    os.makedirs(STATE_DIR, exist_ok=True)

    targets = list(VENDORS) if args.vendor == "all" else [args.vendor]
    errors: list[str] = []
    all_records: list[dict] = []
    per_vendor_raw: dict[str, int] = {}

    if "andymark" in targets:
        print("[andymark] fetching products.json ...")
        recs = scrape_andymark(args.delay, args.force, errors)
        print(f"[andymark] products={len(recs)}")
        _save_json(os.path.join(OUT_DIR, "parts_andymark.json"), recs)
        all_records.extend(recs)
        per_vendor_raw["andymark"] = len(recs)

    for vendor in ("gobilda", "rev"):
        if vendor not in targets:
            continue
        recs = scrape_bigcommerce(vendor, args.delay, args.force, errors, args.limit)
        _save_json(os.path.join(OUT_DIR, f"parts_{vendor}.json"), recs)
        all_records.extend(recs)

    # sitemap raw counts for meta
    for vendor in ("gobilda", "rev"):
        if vendor in targets:
            state_path = os.path.join(STATE_DIR, f"{vendor}_pages.json")
            try:
                with open(state_path, "r", encoding="utf-8") as fh:
                    per_vendor_raw[vendor] = len(json.load(fh))
            except Exception:
                per_vendor_raw[vendor] = None

    all_records.sort(key=lambda r: (r["vendor"], r["name"].lower(), r["sku"].lower()))
    all_records, dupes = dedupe(all_records)

    _save_json(os.path.join(OUT_DIR, "parts_all.json"), all_records)
    write_csv(os.path.join(OUT_DIR, "parts_all.csv"), all_records)
    meta = build_meta(all_records, per_vendor_raw, dupes, errors)
    _save_json(os.path.join(OUT_DIR, "meta.json"), meta)

    print("\n=== Summary ===")
    for vendor, info in meta["byVendor"].items():
        if info["products"] or info.get("sitemapUrls"):
            print(
                f"  {vendor:9s} products={info['products']:5d} "
                f"nonRobot={info['nonRobot']:4d} missingSku={info['missingSku']} "
                f"missingName={info['missingName']}"
            )
    print(f"  total={meta['totalProducts']} duplicatesRemoved={dupes} errors={len(errors)}")
    print(f"  outputs -> {OUT_DIR}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
