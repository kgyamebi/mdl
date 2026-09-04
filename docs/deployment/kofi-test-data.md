# Kofi stock data (Modern Dream A)

Test catalog and on-hand stock imported from `STOCK1KOFI.TXT` — **Modern Dream A** shop/warehouse only.

## What gets loaded

| Item | Count |
|------|-------|
| Products | 2,436 |
| With on-hand stock | 543 |
| Categories | 8 improved categories (see below) |
| Location | Modern Dream A warehouse (`LOC-WH-A`) |
| Main warehouses | Empty (no stock from this file) |

**Categories (V30):** Lighting & Lamps · Switches & Sockets · Cables & Wiring · Conduit & Trunking · Circuit Protection · Distribution Boards · Fans & Ventilation · Tools & Accessories

## Shops & staff

| Shop | Workers | Stock |
|------|---------|-------|
| **Modern Dream A** | Marvin (`marvin@mdl.local`) | Full Kofi data |
| **Modern Dream B** | Stephen, Fausty | Empty (for later) |
| Shop C | — | Deactivated |

Staff password (same as demo worker): **`Worker@123!`**

## Apply data

### 1. Restart backend (runs Flyway V29)

```powershell
cd backend
mvn spring-boot:run
```

Or restart Docker stack if you use `docker-compose.stack.yml`.

### 2. Apply staff accounts (once)

After the app has started at least once (demo users exist):

```powershell
# If using docker MariaDB from docker-compose.yml:
Get-Content scripts/data/seed_kofi_staff.sql | docker exec -i mdl-mariadb mariadb -umdl_user -pchange_me_in_production mdl_platform
```

Adjust credentials/host if your `.env` differs.

### 3. Log in and test

- **Owner:** `owner@mdl.local` / your owner password
- **Marvin (Shop A POS):** `marvin@mdl.local` / `Worker@123!`
- **Stephen / Fausty (Shop B):** `stephen@mdl.local` / `fausty@mdl.local` / `Worker@123!`

Use **Sales → Modern Dream A** for POS. Stock deducts from the shop warehouse.

## Regenerate from source file

If you update `backend/src/main/resources/db/seed/STOCK1KOFI.txt`:

```powershell
py -3 scripts/data/generate_kofi_seed.py
```

Then bump to a new Flyway version (e.g. V30) or reset the dev database before re-running V29.

## Notes

- Negative quantities in the export are stored as **0** on hand.
- Items with no selling price use cost price; if both are 0, a placeholder price of **0.0001 GHS** is set so the product can exist in the catalog.
- Cable/trunk items are marked **METRE** unit where detected; everything else is **PIECE**.
