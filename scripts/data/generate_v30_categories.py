#!/usr/bin/env py -3
"""Generate V30 migration to recategorize Kofi stock products into improved categories."""

import csv
import re
import sys
from collections import OrderedDict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
INPUT = ROOT / "backend/src/main/resources/db/seed/STOCK1KOFI.txt"
OUTPUT = ROOT / "backend/src/main/resources/db/migration/V30__improve_kofi_categories.sql"

CATEGORIES = OrderedDict(
    [
        ("LIGHTING", ("Lighting & Lamps", "LED panels, bulbs, tubes, downlights, and fittings", 10)),
        ("SWITCHES_SOCKETS", ("Switches & Sockets", "Switches, sockets, plug tops, and dimmers", 20)),
        ("CABLES", ("Cables & Wiring", "Electrical cable sold by the metre or roll", 30)),
        ("CONDUIT_TRUNKING", ("Conduit & Trunking", "Conduit, trunking, clips, and saddles", 40)),
        (
            "CIRCUIT_PROTECTION",
            ("Circuit Protection", "MCBs, mainswitches, contactors, and fuses", 50),
        ),
        ("DISTRIBUTION", ("Distribution Boards", "Consumer units and phase distribution boards", 60)),
        ("FANS_VENTILATION", ("Fans & Ventilation", "Ceiling, wall, and industrial fans", 70)),
        ("ACCESSORIES", ("Tools & Accessories", "Holders, connectors, tape, and general consumables", 80)),
    ]
)


def sql_str(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def dedupe_rows(rows: list[dict]) -> list[dict]:
    seen: OrderedDict[str, dict] = OrderedDict()
    for row in rows:
        key = row["itemname"].upper()
        if key not in seen:
            seen[key] = row
    return list(seen.values())


def load_rows(path: Path) -> list[dict]:
    rows: list[dict] = []
    with path.open(newline="", encoding="utf-8", errors="replace") as handle:
        for row in csv.DictReader(handle):
            item = (row.get("itemname") or "").strip()
            if item:
                rows.append({"itemname": item, "old_category": (row.get("category") or "").strip()})
    return dedupe_rows(rows)


def categorize(name: str, old_category: str) -> str:
    n = name.upper()

    if re.search(r"FAN|EXHAUST|VENTIL|AIR CURTAIN", n):
        return "FANS_VENTILATION"

    if re.search(
        r"TRUNK|TRUNKING|\d+MM\s+(CLIP|SADDLE|PLASTIC TUBE|TUBE V-|TUBE RR|PRECISION PIPE)|"
        r"PVC CLIP|CABLE TRAY|\d+X\d+\s+TRUNK",
        n,
    ):
        return "CONDUIT_TRUNKING"

    if re.search(
        r"\bCABLE\b|\bWIRE\b|TWIN|EARTH|\d+MM2|\d+\.\d+MM|\d+/\d+|75/95|TV CABLE|"
        r"FLEX CABLE|COAX|SINGLE CORE|MULTICORE",
        n,
    ) and not re.search(r"LED|LIGHT|BULB|SOCKET|SWITCH", n):
        return "CABLES"

    if re.search(
        r"\bMCB\b|RCBO|RCD\b|CONTACTOR|OVERLOAD|WYLEX|MAINSWITCH|MAIN SWITCH|"
        r"ISOLATOR|\d+POLE\s+\d+A|CHANGEOVER|TRANSFER SWITCH",
        n,
    ):
        return "CIRCUIT_PROTECTION"

    if re.search(
        r"\d+WAY\s+(SPN|TPN|3PHASE)|CONSUMER UNIT|DISTRIBUTION|DB BOX|PANEL BOARD|"
        r"ENCLOSURE.*WAY|HAVELLS\s+\d+WAY",
        n,
    ):
        return "DISTRIBUTION"

    if re.search(
        r"SOCKET|PLUGTOP|PLUG TOP|DIMMER|ADAPTOR|ADAPTER|"
        r"\dG\s+\dW|\dG\s+2WAY|\dG\s+1W|\dG\s+SOCKET|"
        r"DOUBLE SOCKET|SINGLE SOCKET|SWITCH.*WHITE|V-MAX.*SOCKET",
        n,
    ) and not re.search(r"MCB|RCBO|12WAY|24WAY", n):
        return "SWITCHES_SOCKETS"

    if re.search(
        r"LED|BULB|TUBE|T8|T5|DOWNLIGHT|DOWN LIGHT|PANEL|SPOT\s*LIGHT|SPOTLIGHT|"
        r"\bLAMP\b|CHOKE|2D BULB|FITTING|BATTEN|FLURESCENT|FLUORESCENT|RECESS|"
        r"SURFACE|BULB|GLOBE|STRIPLITE|STRIP LIGHT|WALL LIGHT|CEILING LIGHT|"
        r"EXIT PANEL|EMERGENCY|GU10|COB|LUMEN|SENSER LIGHT|SENSOR LIGHT|"
        r"CONDUIT.*LED|COND.*LED|LIGHTING|LUMINAIRE|PHILIPS.*W",
        n,
    ):
        return "LIGHTING"

    if old_category == "CABLE":
        return "CABLES"
    if old_category == "MAINSWITCH":
        return "CIRCUIT_PROTECTION"

    return "ACCESSORIES"


def main() -> int:
    if not INPUT.exists():
        print(f"Missing {INPUT}", file=sys.stderr)
        return 1

    rows = load_rows(INPUT)
    assignments: list[tuple[str, str]] = []
    for i, row in enumerate(rows, start=1):
        sku = f"MDF-A-{i:05d}"
        code = categorize(row["itemname"], row["old_category"])
        assignments.append((sku, code))

    lines = [
        "-- =============================================================================",
        "-- V30: Improve Kofi stock categories — data-only recategorization",
        "-- =============================================================================",
        "",
        "SET @mdl_business_id = (SELECT id FROM businesses WHERE code = 'MDL');",
        "",
        "-- Remove legacy flat categories from V29",
        "UPDATE products p",
        "JOIN product_categories c ON c.id = p.category_id",
        "SET p.category_id = NULL",
        "WHERE p.business_id = @mdl_business_id",
        "  AND c.code IN ('ACCESSORIES', 'PIPE', 'CABLE', 'MAINSWITCH');",
        "",
        "DELETE FROM product_categories",
        "WHERE business_id = @mdl_business_id",
        "  AND code IN ('ACCESSORIES', 'PIPE', 'CABLE', 'MAINSWITCH');",
        "",
        "-- Improved retail categories",
    ]

    for code, (name, description, sort_order) in CATEGORIES.items():
        lines.append(
            "INSERT INTO product_categories (business_id, parent_id, name, code, description, sort_order) "
            f"SELECT @mdl_business_id, NULL, {sql_str(name)}, {sql_str(code)}, "
            f"{sql_str(description)}, {sort_order};"
        )

    lines.append("")
    lines.append("-- Reassign products by SKU")

    by_code: dict[str, list[str]] = {code: [] for code in CATEGORIES}
    for sku, code in assignments:
        by_code[code].append(sku)

    for code, skus in by_code.items():
        if not skus:
            continue
        # Batch SKUs in groups of 50 for manageable UPDATE statements
        for i in range(0, len(skus), 50):
            batch = skus[i : i + 50]
            in_list = ", ".join(sql_str(s) for s in batch)
            lines.append(
                "UPDATE products p "
                "JOIN product_categories c ON c.business_id = p.business_id AND c.code = "
                f"{sql_str(code)} "
                f"SET p.category_id = c.id "
                f"WHERE p.business_id = @mdl_business_id AND p.sku IN ({in_list});"
            )

    lines.append("")
    OUTPUT.write_text("\n".join(lines) + "\n", encoding="utf-8")

    from collections import Counter

    counts = Counter(code for _, code in assignments)
    print(f"Wrote {OUTPUT}")
    for code, count in counts.most_common():
        print(f"  {CATEGORIES[code][0]}: {count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
