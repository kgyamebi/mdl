import { execFileSync } from 'child_process';
import { readFileSync } from 'fs';

function parseCsvLine(line) {
  const values = [];
  let value = '';
  let quoted = false;
  for (let index = 0; index < line.length; index += 1) {
    const char = line[index];
    if (char === '"') {
      if (quoted && line[index + 1] === '"') {
        value += '"';
        index += 1;
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

function parse(path) {
  const lines = readFileSync(path, 'utf8').trim().split(/\r?\n/).slice(1);
  return lines.filter((line) => line.trim()).map((line) => {
    const [loc, sku, name, , , , qty, cost, sell] = parseCsvLine(line);
    return {
      loc,
      sku,
      name,
      qty: Number(qty),
      cost: Number(cost),
      sell: Number(sell),
    };
  });
}

const dump = execFileSync(
  'mariadb',
  [
    '-h',
    'localhost',
    '-u',
    'mdl_user',
    '-pchange_me_in_production',
    'mdl_platform',
    '--batch',
    '--skip-column-names',
    '-e',
    `SELECT l.name, COALESCE(p.sku, ''), b.quantity_on_hand, p.cost_price, p.selling_price
     FROM inventory_balances b
     JOIN products p ON p.id = b.product_id
     JOIN locations l ON l.id = b.location_id`,
  ],
  { encoding: 'utf8' },
);

const db = new Map();
for (const line of dump.trim().split('\n')) {
  const [loc, sku, qty, cost, sell] = line.split('\t');
  db.set(`${loc}|${sku}`, { qty: Number(qty), cost: Number(cost), sell: Number(sell) });
}

function check(rows, locName) {
  let missing = 0;
  let qtyMismatch = 0;
  let priceMismatch = 0;
  for (const row of rows) {
    const hit =
      db.get(`${locName}|${row.sku}`)
      ?? db.get(`${locName}|${row.sku}-A`)
      ?? db.get(`${locName}|${row.sku}-B`)
      ?? [...db.entries()].find(([key]) => key.startsWith(`${locName}|${row.sku}`))?.[1];
    if (!hit) {
      missing += 1;
      continue;
    }
    if (Math.abs(hit.qty - row.qty) > 0.0001) qtyMismatch += 1;
    if (Math.abs(hit.cost - row.cost) > 0.011 || Math.abs(hit.sell - row.sell) > 0.011) priceMismatch += 1;
  }
  return { file: rows.length, missing, qtyMismatch, priceMismatch };
}

const a = parse('data/stock-import/MODERN DREAM A STOCK.csv');
const b = parse('data/stock-import/MODERN DREAM B STOCK.csv');
console.log(JSON.stringify({
  A: check(a, 'MODERN DREAM A SHOP'),
  B: check(b, 'MODERN DREAM B SHOP'),
  dbRows: db.size,
}, null, 2));
