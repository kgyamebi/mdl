import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

const root = join(import.meta.dirname, '..', '..');

function parseCsv(path) {
  const text = readFileSync(path, 'utf8').replace(/^\uFEFF/, '');
  const lines = text.split(/\r?\n/).filter((line) => line.trim());
  const header = splitCsv(lines[0]);
  return lines.slice(1).map((line) => {
    const values = splitCsv(line);
    const row = {};
    header.forEach((key, index) => {
      row[key] = values[index] ?? '';
    });
    return row;
  });
}

function splitCsv(line) {
  const values = [];
  let value = '';
  let quoted = false;
  for (let i = 0; i < line.length; i += 1) {
    const char = line[i];
    if (char === '"') {
      if (quoted && line[i + 1] === '"') {
        value += '"';
        i += 1;
      } else {
        quoted = !quoted;
      }
    } else if (char === ',' && !quoted) {
      values.push(value);
      value = '';
    } else {
      value += char;
    }
  }
  values.push(value);
  return values;
}

function query(sql) {
  return execFileSync(
    'mariadb',
    [
      '-h',
      'localhost',
      '-u',
      'mdl_user',
      '-pchange_me_in_production',
      '--batch',
      '--skip-column-names',
      'mdl_platform',
      '-e',
      sql,
    ],
    { encoding: 'utf8' },
  )
    .trim()
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => line.split('\t'));
}

function expectedSku(itemCode, location) {
  return itemCode;
}

const shopA = parseCsv(join(root, 'data/stock-import/MODERN DREAM A STOCK.csv'));
const shopB = parseCsv(join(root, 'data/stock-import/MODERN DREAM B STOCK.csv'));

const aCodes = new Set(shopA.map((row) => row.item_code));
const bCodes = new Set(shopB.map((row) => row.item_code));
const sharedCodes = [...aCodes].filter((code) => bCodes.has(code));

const dbRows = query(`
  SELECT p.sku, p.name, p.cost_price, p.selling_price, l.name, b.quantity_on_hand
  FROM products p
  JOIN inventory_balances b ON b.product_id = p.id
  JOIN locations l ON l.id = b.location_id
  ORDER BY p.sku, l.name
`);

const bySkuLocation = new Map();
for (const [sku, name, cost, sell, location, qty] of dbRows) {
  bySkuLocation.set(`${sku}|${location}`, { sku, name, cost: Number(cost), sell: Number(sell), location, qty: Number(qty) });
}

function checkShop(rows, shopName) {
  let missing = 0;
  let qtyMismatch = 0;
  let priceMismatch = 0;
  let nameMismatch = 0;
  const samples = [];
  for (const row of rows) {
    const sku = expectedSku(row.item_code, shopName);
    const prefixed = `${shopName.includes(' A ') ? 'A' : 'B'}-${row.item_code}`;
    const found = bySkuLocation.get(`${sku}|${shopName}`) ?? bySkuLocation.get(`${prefixed}|${shopName}`);
    if (!found) {
      missing += 1;
      if (samples.length < 8) samples.push(`MISSING ${row.item_code} ${row.item_name}`);
      continue;
    }
    if (Number(row.quantity) !== found.qty) {
      qtyMismatch += 1;
      if (samples.length < 8) {
        samples.push(`QTY ${row.item_code} csv=${row.quantity} db=${found.qty}`);
      }
    }
    if (Number(row.cost_price) !== found.cost || Number(row.selling_price) !== found.sell) {
      priceMismatch += 1;
    }
    if (found.name !== row.item_name) {
      nameMismatch += 1;
    }
  }
  return { rows: rows.length, missing, qtyMismatch, priceMismatch, nameMismatch, samples };
}

const a = checkShop(shopA, 'MODERN DREAM A SHOP');
const b = checkShop(shopB, 'MODERN DREAM B SHOP');

const locCounts = query(`
  SELECT l.name, COUNT(*) FROM inventory_balances b
  JOIN locations l ON l.id = b.location_id
  GROUP BY l.id ORDER BY l.name
`);

const products = query('SELECT COUNT(*) FROM products')[0][0];
const balances = query('SELECT COUNT(*) FROM inventory_balances')[0][0];
const warehousesEmpty = query(`
  SELECT COUNT(*) FROM inventory_balances b
  JOIN locations l ON l.id = b.location_id
  WHERE l.location_type = 'WAREHOUSE'
`)[0][0];

console.log(JSON.stringify({
  csv: { shopA: shopA.length, shopB: shopB.length, sharedItemCodes: sharedCodes.length },
  db: { products: Number(products), balances: Number(balances), warehouseBalanceRows: Number(warehousesEmpty) },
  locations: Object.fromEntries(locCounts),
  shopA: a,
  shopB: b,
}, null, 2));
