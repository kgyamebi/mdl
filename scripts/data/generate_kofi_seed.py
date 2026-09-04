#!/usr/bin/env py -3
"""Generate Flyway migration from STOCK1KOFI.TXT for Modern Dream A shop stock."""

import csv
import re
import sys
from collections import OrderedDict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_INPUT = Path(r"n:\STOCK1KOFI.TXT")
FALLBACK_INPUT = ROOT / "backend/src/main/resources/db/seed/STOCK1KOFI.txt"
OUTPUT = ROOT / "backend/src/main/resources/db/migration/V29__seed_modern_dream_kofi_stock.sql"
STAFF_OUTPUT = ROOT / "scripts/data/seed_kofi_staff.sql"

METRE_PATTERN = re.compile(
    r"\b(CABLE|METRE|METER|MM2|TWIN|EARTH|WIRE|COIL|TRUNK|75/95|95MM)\b",
    re.IGNORECASE,
)


def slug_code(value: str, max_len: int = 48) -> str:
    code = re.sub(r"[^A-Z0-9]+", "-", value.upper()).strip("-")
    return code[:max_len] or "MISC"


def sql_str(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def parse_decimal(raw: str) -> float:
    raw = (raw or "").strip()
    if not raw:
        return 0.0
    try:
        return float(raw)
    except ValueError:
        return 0.0


def infer_uom(name: str, category: str) -> str:
    text = f"{category} {name}"
    if METRE_PATTERN.search(text):
        return "METRE"
    return "PIECE"


def load_rows(path: Path) -> list[dict]:
    rows: list[dict] = []
    with path.open(newline="", encoding="utf-8", errors="replace") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            item = (row.get("itemname") or "").strip()
            if not item:
                continue
            rows.append(
                {
                    "location": (row.get("location") or "").strip(),
                    "category": (row.get("category") or "UNCATEGORIZED").strip(),
                    "itemname": item,
                    "qty": parse_decimal(row.get("qty_in_stock") or "0"),
                    "cost": parse_decimal(row.get("cost_price") or "0"),
                    "sell": parse_decimal(row.get("selling_price") or "0"),
                }
            )
    return rows


def dedupe_rows(rows: list[dict]) -> list[dict]:
    seen: OrderedDict[str, dict] = OrderedDict()
    for row in rows:
        key = row["itemname"].upper()
        if key not in seen:
            seen[key] = row
            continue
        existing = seen[key]
        if row["qty"] > existing["qty"]:
            existing["qty"] = row["qty"]
        if row["sell"] > 0 and existing["sell"] <= 0:
            existing["sell"] = row["sell"]
        if row["cost"] > 0 and existing["cost"] <= 0:
            existing["cost"] = row["cost"]
    return list(seen.values())


def clear_catalog_sql() -> list[str]:
    return [
        "-- Clear transactional demo data tied to old products",
        "DELETE sri FROM sale_return_items sri",
        "JOIN sale_returns sr ON sr.id = sri.sale_return_id",
        "WHERE sr.business_id = @mdl_business_id;",
        "DELETE srr FROM sale_return_refunds srr",
        "JOIN sale_returns sr ON sr.id = srr.sale_return_id",
        "WHERE sr.business_id = @mdl_business_id;",
        "DELETE FROM sale_returns WHERE business_id = @mdl_business_id;",
        "DELETE sp FROM sale_payments sp",
        "JOIN sales s ON s.id = sp.sale_id",
        "WHERE s.business_id = @mdl_business_id;",
        "DELETE si FROM sale_items si",
        "JOIN sales s ON s.id = si.sale_id",
        "WHERE s.business_id = @mdl_business_id;",
        "DELETE FROM sales WHERE business_id = @mdl_business_id;",
        "DELETE sti FROM stock_transfer_items sti",
        "JOIN stock_transfers st ON st.id = sti.transfer_id",
        "WHERE st.business_id = @mdl_business_id;",
        "DELETE FROM stock_transfers WHERE business_id = @mdl_business_id;",
        "DELETE ii FROM import_items ii",
        "JOIN imports i ON i.id = ii.import_id",
        "WHERE i.business_id = @mdl_business_id;",
        "DELETE FROM imports WHERE business_id = @mdl_business_id;",
        "DELETE stl FROM stocktake_lines stl",
        "JOIN stocktakes st ON st.id = stl.stocktake_id",
        "WHERE st.business_id = @mdl_business_id;",
        "DELETE FROM stocktakes WHERE business_id = @mdl_business_id;",
        "DELETE FROM inventory_reservations WHERE business_id = @mdl_business_id;",
        "DELETE FROM inventory_adjustment_requests WHERE business_id = @mdl_business_id;",
        "DELETE FROM inventory_balances WHERE business_id = @mdl_business_id;",
        "DELETE FROM inventory_transactions WHERE business_id = @mdl_business_id;",
        "DELETE FROM barcodes WHERE business_id = @mdl_business_id;",
        "DELETE FROM products WHERE business_id = @mdl_business_id;",
        "DELETE FROM product_categories WHERE business_id = @mdl_business_id;",
        "",
    ]


def staff_sql() -> str:
    return """-- Run once after the app has started at least once (demo users exist).
-- Password stays Worker@123! (copied from the demo shop worker account).
SET @mdl_business_id = (SELECT id FROM businesses WHERE code = 'MDL');

-- Marvin — Modern Dream A (was demo John)
UPDATE users
SET email = 'marvin@mdl.local',
    username = 'marvin',
    first_name = 'Marvin',
    last_name = 'Kofi'
WHERE email = 'john@mdl.local';

DELETE ula FROM user_location_assignments ula
JOIN users u ON u.id = ula.user_id
WHERE u.email = 'marvin@mdl.local';

INSERT INTO user_location_assignments (user_id, business_id, location_id, access_level)
SELECT u.id, @mdl_business_id, l.id, 'FULL'
FROM users u
JOIN locations l ON l.business_id = @mdl_business_id AND l.code IN ('LOC-SHOP-A', 'LOC-WH-A')
WHERE u.email = 'marvin@mdl.local';

-- Stephen — Modern Dream B
INSERT INTO users (email, username, password_hash, first_name, last_name, status)
SELECT 'stephen@mdl.local', 'stephen', u.password_hash, 'Stephen', 'Mensah', 'ACTIVE'
FROM users u
WHERE u.email = 'marvin@mdl.local'
  AND NOT EXISTS (SELECT 1 FROM users x WHERE x.email = 'stephen@mdl.local');

INSERT INTO user_business_memberships (user_id, business_id, is_default, status)
SELECT u.id, @mdl_business_id, TRUE, 'ACTIVE'
FROM users u
WHERE u.email = 'stephen@mdl.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_business_memberships m
      WHERE m.user_id = u.id AND m.business_id = @mdl_business_id
  );

INSERT INTO user_roles (user_id, role_id, business_id)
SELECT u.id, r.id, @mdl_business_id
FROM users u
JOIN roles r ON r.code = 'SHOP_WORKER' AND r.business_id IS NULL
WHERE u.email = 'stephen@mdl.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.business_id = @mdl_business_id
  );

INSERT INTO user_location_assignments (user_id, business_id, location_id, access_level)
SELECT u.id, @mdl_business_id, l.id, 'FULL'
FROM users u
JOIN locations l ON l.business_id = @mdl_business_id AND l.code IN ('LOC-SHOP-B', 'LOC-WH-B')
WHERE u.email = 'stephen@mdl.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_location_assignments a
      WHERE a.user_id = u.id AND a.location_id = l.id
  );

-- Fausty — Modern Dream B
INSERT INTO users (email, username, password_hash, first_name, last_name, status)
SELECT 'fausty@mdl.local', 'fausty', u.password_hash, 'Fausty', 'Asante', 'ACTIVE'
FROM users u
WHERE u.email = 'marvin@mdl.local'
  AND NOT EXISTS (SELECT 1 FROM users x WHERE x.email = 'fausty@mdl.local');

INSERT INTO user_business_memberships (user_id, business_id, is_default, status)
SELECT u.id, @mdl_business_id, TRUE, 'ACTIVE'
FROM users u
WHERE u.email = 'fausty@mdl.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_business_memberships m
      WHERE m.user_id = u.id AND m.business_id = @mdl_business_id
  );

INSERT INTO user_roles (user_id, role_id, business_id)
SELECT u.id, r.id, @mdl_business_id
FROM users u
JOIN roles r ON r.code = 'SHOP_WORKER' AND r.business_id IS NULL
WHERE u.email = 'fausty@mdl.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.business_id = @mdl_business_id
  );

INSERT INTO user_location_assignments (user_id, business_id, location_id, access_level)
SELECT u.id, @mdl_business_id, l.id, 'FULL'
FROM users u
JOIN locations l ON l.business_id = @mdl_business_id AND l.code IN ('LOC-SHOP-B', 'LOC-WH-B')
WHERE u.email = 'fausty@mdl.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_location_assignments a
      WHERE a.user_id = u.id AND a.location_id = l.id
  );
"""


def main() -> int:
    input_path = DEFAULT_INPUT if DEFAULT_INPUT.exists() else FALLBACK_INPUT
    if not input_path.exists():
        print(f"Input not found: {input_path}", file=sys.stderr)
        return 1

    rows = dedupe_rows(load_rows(input_path))
    categories = sorted({r["category"] for r in rows})

    lines: list[str] = [
        "-- =============================================================================",
        "-- V29: Modern Dream Light — Kofi shop stock (STOCK1KOFI.TXT)",
        "-- Modern Dream A shop + warehouse only. Main warehouses left empty.",
        "-- =============================================================================",
        "",
        "SET @mdl_business_id = (SELECT id FROM businesses WHERE code = 'MDL');",
        "",
        "-- Accra shop names (2 active shops, 3 warehouses incl. 2 main)",
        "UPDATE locations SET name = 'Modern Dream A', city = 'Accra' WHERE business_id = @mdl_business_id AND code = 'LOC-SHOP-A';",
        "UPDATE locations SET name = 'Modern Dream A Warehouse', city = 'Accra' WHERE business_id = @mdl_business_id AND code = 'LOC-WH-A';",
        "UPDATE locations SET name = 'Modern Dream B', city = 'Accra' WHERE business_id = @mdl_business_id AND code = 'LOC-SHOP-B';",
        "UPDATE locations SET name = 'Modern Dream B Warehouse', city = 'Accra' WHERE business_id = @mdl_business_id AND code = 'LOC-WH-B';",
        "UPDATE locations SET name = 'Main Import Warehouse', city = 'Accra' WHERE business_id = @mdl_business_id AND code = 'LOC-MAIN';",
        "UPDATE locations SET name = 'Regional Distribution Center', city = 'Accra' WHERE business_id = @mdl_business_id AND code = 'LOC-MAIN-B';",
        "UPDATE shops SET name = 'Modern Dream A' WHERE business_id = @mdl_business_id AND code = 'SHOP-A';",
        "UPDATE shops SET name = 'Modern Dream B' WHERE business_id = @mdl_business_id AND code = 'SHOP-B';",
        "UPDATE shops SET status = 'INACTIVE' WHERE business_id = @mdl_business_id AND code = 'SHOP-C';",
        "UPDATE locations SET status = 'INACTIVE' WHERE business_id = @mdl_business_id AND code IN ('LOC-SHOP-C', 'LOC-WH-C');",
        "UPDATE warehouses SET name = 'Modern Dream A Warehouse', description = 'Modern Dream A — shop stock (Kofi data)' WHERE business_id = @mdl_business_id AND code = 'WH-SHOP-A';",
        "UPDATE warehouses SET name = 'Modern Dream B Warehouse', description = 'Modern Dream B — shop stock' WHERE business_id = @mdl_business_id AND code = 'WH-SHOP-B';",
        "",
        "SET @loc_wh_a = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-WH-A');",
        "",
        *clear_catalog_sql(),
        "-- Categories from stock export",
    ]

    for idx, category in enumerate(categories, start=1):
        code = slug_code(category)
        lines.append(
            "INSERT INTO product_categories (business_id, parent_id, name, code, description, sort_order) "
            f"SELECT @mdl_business_id, NULL, {sql_str(category.title())}, {sql_str(code)}, "
            f"{sql_str(f'Modern Dream A — {category.title()}')}, {idx * 10};"
        )

    lines.append("")
    lines.append("-- Products (all items; stock only where qty_in_stock > 0)")

    stock_rows: list[tuple[str, float]] = []
    for i, row in enumerate(rows, start=1):
        sku = f"MDF-A-{i:05d}"
        cat_code = slug_code(row["category"])
        sell = row["sell"] if row["sell"] > 0 else (row["cost"] if row["cost"] > 0 else 0.0001)
        cost = row["cost"] if row["cost"] > 0 else None
        uom = infer_uom(row["itemname"], row["category"])
        cost_sql = f"{cost:.4f}" if cost is not None else "NULL"
        lines.append(
            "INSERT INTO products (business_id, category_id, sku, name, description, brand, "
            f"unit_of_measure, cost_price, selling_price, reorder_level) "
            f"SELECT @mdl_business_id, c.id, {sql_str(sku)}, {sql_str(row['itemname'])}, "
            f"{sql_str('Modern Dream A catalog')}, 'MDL', {sql_str(uom)}, {cost_sql}, {sell:.4f}, 5 "
            f"FROM product_categories c WHERE c.business_id = @mdl_business_id AND c.code = {sql_str(cat_code)};"
        )
        qty = max(0.0, row["qty"])
        if qty > 0:
            stock_rows.append((sku, qty))

    lines.extend(["", "-- Opening stock — Modern Dream A warehouse only"])
    for sku, qty in stock_rows:
        lines.extend(
            [
                "INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)",
                f"SELECT @mdl_business_id, @loc_wh_a, p.id, 'OPENING_BALANCE', {qty:.4f}, {qty:.4f}, 'SEED', 'Kofi stock import — Modern Dream A'",
                f"FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = {sql_str(sku)};",
                "INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)",
                f"SELECT @mdl_business_id, @loc_wh_a, p.id, {qty:.4f}, LAST_INSERT_ID()",
                f"FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = {sql_str(sku)};",
                "",
            ]
        )

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    STAFF_OUTPUT.write_text(staff_sql() + "\n", encoding="utf-8")

    print(f"Wrote {OUTPUT} ({OUTPUT.stat().st_size // 1024} KB)")
    print(f"Wrote {STAFF_OUTPUT}")
    print(f"Products: {len(rows)}, with stock: {len(stock_rows)}, categories: {len(categories)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
