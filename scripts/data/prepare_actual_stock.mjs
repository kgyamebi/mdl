import { createReadStream, existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createInterface } from 'node:readline';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(scriptDir, '..', '..');
const sourceDir = process.env.STOCK_SOURCE_DIR || 'C:\\Users\\user\\Desktop\\stock files';
const outputDir = join(repoRoot, 'data', 'stock-import');
const migrationPath = join(
  repoRoot,
  'backend',
  'src',
  'main',
  'resources',
  'db',
  'prod-migration',
  'V38__configure_locations_and_import_actual_stock.sql',
);

const SOURCES = [
  {
    label: 'Modern Dream A',
    file: 'MAVINLOCATIONSUMMARYSTOCK.TXT',
    location: 'MODERN DREAM A SHOP',
    locationCode: 'LOC-SHOP-A',
    output: 'MODERN DREAM A STOCK.csv',
  },
  {
    label: 'Modern Dream B',
    file: 'STEPHENSIDE1STOCK1.TXT',
    location: 'MODERN DREAM B SHOP',
    locationCode: 'LOC-SHOP-B',
    output: 'MODERN DREAM B STOCK.csv',
  },
];

const REQUIRED_ALIASES = {
  category: ['category', 'itemcategory', 'group'],
  item_code: ['item_id', 'itemid', 'item_code', 'itemcode', 'sku', 'code'],
  item_name: ['itemname', 'item_name', 'name', 'description'],
  quantity: ['qtyinstock', 'qty_in_stock', 'quantity', 'qty', 'stock'],
  selling_price: ['salesprice', 'sellingprice', 'selling_price', 'saleprice', 'price'],
  cost_price: ['costprice', 'cost_price', 'cost'],
};

const NAME_REPLACEMENTS = [
  [/\bCELING\b/g, 'CEILING'],
  [/\bCHANDELIAR\b/g, 'CHANDELIER'],
  [/\bCHANDALIER\b/g, 'CHANDELIER'],
  [/\bFLOODLIGHT\b/g, 'FLOOD LIGHT'],
  [/\bSPOTLIGHT\b/g, 'SPOT LIGHT'],
  [/\bSTREETLITE\b/g, 'STREET LIGHT'],
  [/\bSTREEW\s+LITE\b/g, 'STREET LIGHT'],
  [/\bSTREET\s+LITE\b/g, 'STREET LIGHT'],
  [/\bPENDING\s+LITE\b/g, 'PENDANT LIGHT'],
  [/\bPENDING\s+LIGHT\b/g, 'PENDANT LIGHT'],
  [/\bGATE\s+LITE\b/g, 'GATE LIGHT'],
  [/\bCORNER\s+LITE\b/g, 'CORNER LIGHT'],
  [/\bWALL\s+LITE\b/g, 'WALL LIGHT'],
  [/\bSURF\b/g, 'SURFACE'],
  [/\bCOND\b/g, 'CONDUIT'],
  [/\bWAL\s+FAN\b/g, 'WALL FAN'],
  [/\bOREINT\b/g, 'ORIENT'],
  [/\bPHILLIPS\b/g, 'PHILIPS'],
  [/\bDOWNLITE\b/g, 'DOWNLIGHT'],
  [/\bFILLAMENT\b/g, 'FILAMENT'],
  [/\bLITE\b/g, 'LIGHT'],
];

const CATEGORY_RULES = [
  ['Fans & Ventilation', /\b(FAN|EXTRACTOR|VENTILAT|WHIRL WIND|ORBIT)/],
  ['Cables & Wiring', /\b(CABLE|WIRE|FLEX\b|COAX|CAT[ -]?[456]|TWIN\s*&?\s*EARTH)/],
  [
    'Switches & Sockets',
    /\b(SWITCH|SOCKET|SKT|PLUG|DIMMER|REGULATOR|BELL|COOKER UNIT|TELEPHONE|DATA OUTLET|TV OUTLET|WATER HEATER)|\b[1-9]G(?:\s+[1-9](?:W|WAY))?\b|\bA\/C(?:\s+\d+G)?\s+\d+A\b|\b13A\s+(?:SINGLE|DOUBLE)\b/,
  ],
  [
    'Circuit Protection',
    /\b(MCB|MCCB|RCBO|RCD|BREAKER|FUSE|CONTACTOR|ISOLATOR|SURGE|CHANGEOVER|CHANGE OVER|CUTOUT|FRIDGE GUARD|VOLTAGE GUARD|INCOMER|\d+POLE)/,
  ],
  [
    'Distribution Boards',
    /\b(DISTRIBUTION|CONSUMER UNIT|SPN|TPN|DB BOX|METAL BOARD|METER BOARD)|\b\d+\s*WAY\s+(?:HAGER|HPL|RR)\b/,
  ],
  [
    'Conduit & Trunking',
    /\b(CONDUIT|TRUNK|PIPE|SADDLE|BEND|ELBOW|COUPLER|COUPLING|JUNCTION BOX|FLEXIBLE TUBE|MALE BUSH|TOWER CLIP)/,
  ],
  [
    'Lighting & Lamps',
    /\b(LED|LIGHT|LAMP|BULB|CHANDELIER|DOWNLIGHT|PANEL|FLOOD|SPOT|STREET|PENDANT|TRACK|ROPE|HALOGEN|BALLAST|LUMINAR|CAPSULE|CHOKE|TUBE|HOLDER|PHOTOCELL|GU10|STARTER|FITTING|GLOBE|CEILING ROSE|COB|REFLECTOR)|\b\d+(?:\.\d+)?W\b/,
  ],
  ['Tools & Accessories', /.*/],
];

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

function parseStockRecord(line, sourceFile, rowNumber) {
  // The export occasionally contains an unescaped inch mark at the end of an
  // item name (for example: "TOWER CLIPS 4""). Anchor the final three numeric
  // fields so those records are recovered without shifting columns.
  const match = line.match(/^"([^"]*)","([^"]*)","(.*)",([^,]*),([^,]*),([^,]*)$/);
  if (!match) {
    const fallback = parseCsvLine(line);
    if (fallback.length !== 6) {
      throw new Error(`Malformed stock record at ${sourceFile}:${rowNumber}`);
    }
    return fallback;
  }
  return match.slice(1);
}

function normalizedHeader(value) {
  return value.trim().toLowerCase().replace(/[^a-z0-9_]/g, '');
}

function resolveColumns(headers) {
  const normalized = headers.map(normalizedHeader);
  return Object.fromEntries(
    Object.entries(REQUIRED_ALIASES).map(([target, aliases]) => {
      const index = normalized.findIndex((header) => aliases.includes(header));
      if (index < 0) {
        throw new Error(`Required field '${target}' not found. Headers: ${headers.join(', ')}`);
      }
      return [target, index];
    }),
  );
}

function cleanName(value) {
  let name = value
    .normalize('NFKC')
    .replace(/[\u0000-\u001f\u007f]/g, ' ')
    .trim()
    .toUpperCase();
  for (const [pattern, replacement] of NAME_REPLACEMENTS) {
    name = name.replace(pattern, replacement);
  }
  return name
    .replace(/\s*\/\s*/g, '/')
    .replace(/\s+/g, ' ')
    .replace(/\(\s+/g, '(')
    .replace(/\s+\)/g, ')')
    .trim();
}

function inferCategory(name) {
  return CATEGORY_RULES.find(([, pattern]) => pattern.test(name))[0];
}

function parseNumber(value, field, rowNumber) {
  const normalized = value.trim().replace(/,/g, '');
  if (normalized === '') return 0;
  const number = Number(normalized);
  if (!Number.isFinite(number)) {
    throw new Error(`Invalid ${field} '${value}' at row ${rowNumber}`);
  }
  return number;
}

function formatQuantity(value) {
  return Number.isInteger(value) ? String(value) : String(value).replace(/0+$/, '').replace(/\.$/, '');
}

function formatMoney(value) {
  return value.toFixed(2);
}

function csvQuote(value) {
  return `"${String(value).replace(/"/g, '""')}"`;
}

function sqlString(value) {
  return `'${String(value).replace(/\\/g, '\\\\').replace(/'/g, "''")}'`;
}

async function readSource(source) {
  const path = join(sourceDir, source.file);
  if (!existsSync(path)) throw new Error(`Required source file not found: ${path}`);

  const input = createInterface({
    input: createReadStream(path, { encoding: 'utf8' }),
    crlfDelay: Infinity,
  });
  let headers;
  let columns;
  let rowNumber = 0;
  const rows = [];
  const seenCodes = new Set();

  for await (const rawLine of input) {
    rowNumber += 1;
    if (!rawLine.trim()) continue;
    const line = rawLine.replace(/^\uFEFF/, '');
    const values = headers ? parseStockRecord(line, source.file, rowNumber) : parseCsvLine(line);
    if (!headers) {
      headers = values;
      columns = resolveColumns(headers);
      continue;
    }

    const itemCode = values[columns.item_code]?.trim();
    const rawName = values[columns.item_name]?.trim() ?? '';
    const blankPlaceholder =
      !itemCode &&
      !rawName &&
      parseNumber(values[columns.quantity] ?? '', 'quantity', rowNumber) === 0 &&
      parseNumber(values[columns.cost_price] ?? '', 'cost price', rowNumber) === 0 &&
      parseNumber(values[columns.selling_price] ?? '', 'selling price', rowNumber) === 0;
    if (blankPlaceholder) continue;
    if (!itemCode) throw new Error(`Blank item code at ${source.file}:${rowNumber}`);
    if (seenCodes.has(itemCode)) {
      throw new Error(`Duplicate item code '${itemCode}' within ${source.file}:${rowNumber}`);
    }
    seenCodes.add(itemCode);

    const itemName = cleanName(rawName);
    if (!itemName) throw new Error(`Blank item name for '${itemCode}' at ${source.file}:${rowNumber}`);
    const quantity = parseNumber(values[columns.quantity] ?? '', 'quantity', rowNumber);
    const costPrice = parseNumber(values[columns.cost_price] ?? '', 'cost price', rowNumber);
    const sellingPrice = parseNumber(values[columns.selling_price] ?? '', 'selling price', rowNumber);
    if (costPrice < 0 || sellingPrice < 0) {
      throw new Error(`Negative price for '${itemCode}' at ${source.file}:${rowNumber}`);
    }

    const flags = [];
    if (quantity < 0) flags.push('NEGATIVE_STOCK');
    if (quantity === 0) flags.push('ZERO_STOCK');
    if (costPrice === 0) flags.push('MISSING_COST_PRICE');
    if (sellingPrice === 0) flags.push('MISSING_SELLING_PRICE');

    rows.push({
      shop: source.label,
      location: source.location,
      locationCode: source.locationCode,
      itemCode,
      itemName,
      sourceCategory: (values[columns.category] || 'ACCESSORIES').trim().toUpperCase(),
      category: inferCategory(itemName),
      quantity,
      costPrice,
      sellingPrice,
      unitOfMeasure: 'PIECE',
      flags: flags.join('|'),
    });
  }
  return rows;
}

function writeCsv(source, rows) {
  const headers = [
    'location',
    'item_code',
    'item_name',
    'source_category',
    'category',
    'unit_of_measure',
    'quantity',
    'cost_price',
    'selling_price',
    'data_flags',
  ];
  const lines = [headers.map(csvQuote).join(',')];
  for (const row of rows) {
    lines.push(
      [
        csvQuote(row.location),
        csvQuote(row.itemCode),
        csvQuote(row.itemName),
        csvQuote(row.sourceCategory),
        csvQuote(row.category),
        csvQuote(row.unitOfMeasure),
        formatQuantity(row.quantity),
        formatMoney(row.costPrice),
        formatMoney(row.sellingPrice),
        csvQuote(row.flags),
      ].join(','),
    );
  }
  writeFileSync(join(outputDir, source.output), `${lines.join('\n')}\n`, 'utf8');
}

function categoryCode(name) {
  return name.toUpperCase().replace(/&/g, 'AND').replace(/[^A-Z0-9]+/g, '_').replace(/^_|_$/g, '');
}

function buildMigration(allRows) {
  const categories = [...new Set(allRows.map((row) => row.category))];
  const duplicateCodes = new Map();
  for (const row of allRows) {
    const entries = duplicateCodes.get(row.itemCode) || [];
    entries.push(row);
    duplicateCodes.set(row.itemCode, entries);
  }

  // The stocktaker's item ID remains intact in source_item_code. Internal SKU adds
  // a shop namespace only when the same item ID means different products across shops.
  for (const entries of duplicateCodes.values()) {
    const productDefinitions = new Set(
      entries.map(
        (row) =>
          `${row.itemName}|${row.category}|${row.costPrice.toFixed(2)}|${row.sellingPrice.toFixed(2)}`,
      ),
    );
    for (const row of entries) {
      row.internalSku =
        entries.length > 1 && productDefinitions.size > 1
          ? `${row.locationCode === 'LOC-SHOP-A' ? 'A' : 'B'}-${row.itemCode}`
          : row.itemCode;
    }
  }

  const lines = [
    '-- Generated from only MAVINLOCATIONSUMMARYSTOCK.TXT and STEPHENSIDE1STOCK1.TXT.',
    '-- Re-run scripts/data/prepare_actual_stock.mjs to reproduce.',
    '',
    "SET @mdl_business_id = (SELECT id FROM businesses WHERE code = 'MDL');",
    '',
    '-- Preserve stocktaker deficits. Reserved quantity must remain non-negative.',
    'ALTER TABLE inventory_balances DROP CONSTRAINT IF EXISTS chk_inventory_balances_non_negative;',
    'ALTER TABLE inventory_balances DROP CONSTRAINT IF EXISTS chk_inventory_balances_reserved_lte_on_hand;',
    'ALTER TABLE inventory_balances DROP CONSTRAINT IF EXISTS chk_inventory_balances_reserved_non_negative;',
    'ALTER TABLE inventory_balances ADD CONSTRAINT chk_inventory_balances_reserved_non_negative CHECK (quantity_reserved >= 0);',
    '',
    '-- Make a retry safe if a non-transactional MariaDB migration previously stopped partway.',
    'UPDATE inventory_balances SET last_transaction_id = NULL WHERE business_id = @mdl_business_id;',
    'DELETE FROM inventory_balances WHERE business_id = @mdl_business_id;',
    'DELETE FROM inventory_transactions WHERE business_id = @mdl_business_id;',
    'DELETE FROM barcodes WHERE business_id = @mdl_business_id;',
    'DELETE FROM products WHERE business_id = @mdl_business_id;',
    'DELETE FROM product_categories WHERE business_id = @mdl_business_id;',
    '',
    '-- Six active locations: two shops and two warehouses per shop.',
    "UPDATE locations SET name = 'MODERN DREAM A SHOP', code = 'LOC-SHOP-A', location_type = 'SHOP', status = 'ACTIVE' WHERE business_id = @mdl_business_id AND code = 'LOC-SHOP-A';",
    "UPDATE locations SET name = 'MODERN DREAM A WAREHOUSE A', code = 'LOC-WH-A', location_type = 'WAREHOUSE', status = 'ACTIVE' WHERE business_id = @mdl_business_id AND code = 'LOC-WH-A';",
    "UPDATE locations SET name = 'MODERN DREAM A WAREHOUSE B', code = 'LOC-WH-A2', location_type = 'WAREHOUSE', status = 'ACTIVE' WHERE business_id = @mdl_business_id AND code = 'LOC-MAIN';",
    "UPDATE locations SET name = 'MODERN DREAM B SHOP', code = 'LOC-SHOP-B', location_type = 'SHOP', status = 'ACTIVE' WHERE business_id = @mdl_business_id AND code = 'LOC-SHOP-B';",
    "UPDATE locations SET name = 'MODERN DREAM B WAREHOUSE C', code = 'LOC-WH-B', location_type = 'WAREHOUSE', status = 'ACTIVE' WHERE business_id = @mdl_business_id AND code = 'LOC-WH-B';",
    "UPDATE locations SET name = 'MODERN DREAM B WAREHOUSE D', code = 'LOC-WH-B2', location_type = 'WAREHOUSE', status = 'ACTIVE' WHERE business_id = @mdl_business_id AND code = 'LOC-MAIN-B';",
    "UPDATE locations SET status = 'INACTIVE' WHERE business_id = @mdl_business_id AND code IN ('LOC-SHOP-C', 'LOC-WH-C');",
    '',
    "UPDATE warehouses SET name = 'MODERN DREAM A WAREHOUSE A', code = 'WH-A', warehouse_type = 'SHOP', is_restricted = FALSE, description = 'Modern Dream A warehouse A', status = 'ACTIVE' WHERE business_id = @mdl_business_id AND code = 'WH-SHOP-A';",
    "UPDATE warehouses SET name = 'MODERN DREAM A WAREHOUSE B', code = 'WH-B', warehouse_type = 'SHOP', is_restricted = FALSE, description = 'Modern Dream A warehouse B', status = 'ACTIVE' WHERE business_id = @mdl_business_id AND code = 'WH-MAIN';",
    "UPDATE warehouses SET name = 'MODERN DREAM B WAREHOUSE C', code = 'WH-C', warehouse_type = 'SHOP', is_restricted = FALSE, description = 'Modern Dream B warehouse C', status = 'ACTIVE' WHERE business_id = @mdl_business_id AND code = 'WH-SHOP-B';",
    "UPDATE warehouses SET name = 'MODERN DREAM B WAREHOUSE D', code = 'WH-D', warehouse_type = 'SHOP', is_restricted = FALSE, description = 'Modern Dream B warehouse D', status = 'ACTIVE' WHERE business_id = @mdl_business_id AND code = 'WH-MAIN-B';",
    "UPDATE warehouses SET status = 'INACTIVE' WHERE business_id = @mdl_business_id AND code = 'WH-SHOP-C';",
    '',
    "UPDATE shops SET name = 'Modern Dream A', status = 'ACTIVE' WHERE business_id = @mdl_business_id AND code = 'SHOP-A';",
    "UPDATE shops SET name = 'Modern Dream B', status = 'ACTIVE' WHERE business_id = @mdl_business_id AND code = 'SHOP-B';",
    "UPDATE shops SET status = 'INACTIVE' WHERE business_id = @mdl_business_id AND code = 'SHOP-C';",
    '',
    '-- V37 runs immediately before this production-only import and clears product/stock data.',
  ];

  for (const [index, category] of categories.entries()) {
    lines.push(
      `INSERT INTO product_categories (business_id, name, code, description, sort_order, status) VALUES (@mdl_business_id, ${sqlString(category)}, ${sqlString(categoryCode(category))}, 'Imported actual stock category', ${(index + 1) * 10}, 'ACTIVE');`,
    );
  }

  lines.push('', '-- Exact stocktaker item IDs are retained in description metadata.');
  const insertedSkus = new Set();
  for (const row of allRows) {
    if (insertedSkus.has(row.internalSku)) continue;
    insertedSkus.add(row.internalSku);
    const description = `Source item code: ${row.itemCode}; Source category: ${row.sourceCategory}${
      row.flags ? `; Flags: ${row.flags}` : ''
    }`;
    lines.push(
      `INSERT INTO products (business_id, category_id, sku, name, description, unit_of_measure, cost_price, selling_price, tax_inclusive, track_inventory, reorder_level, status) SELECT @mdl_business_id, c.id, ${sqlString(row.internalSku)}, ${sqlString(row.itemName)}, ${sqlString(description)}, ${sqlString(row.unitOfMeasure)}, ${row.costPrice.toFixed(2)}, ${row.sellingPrice.toFixed(2)}, TRUE, TRUE, 0, 'ACTIVE' FROM product_categories c WHERE c.business_id = @mdl_business_id AND c.code = ${sqlString(categoryCode(row.category))};`,
    );
  }

  lines.push('', '-- Merged totals are assigned to each SHOP location, as instructed.');
  for (const row of allRows) {
    lines.push(
      `INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes) SELECT @mdl_business_id, l.id, p.id, 'OPENING_BALANCE', ${formatQuantity(row.quantity)}, ${formatQuantity(row.quantity)}, 'ACTUAL_STOCK_IMPORT', ${sqlString(`Merged stock import; source item code ${row.itemCode}${row.flags ? `; ${row.flags}` : ''}`)} FROM locations l JOIN products p ON p.business_id = @mdl_business_id AND p.sku = ${sqlString(row.internalSku)} WHERE l.business_id = @mdl_business_id AND l.code = ${sqlString(row.locationCode)};`,
    );
    lines.push(
      `INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, quantity_reserved, last_transaction_id) SELECT @mdl_business_id, l.id, p.id, ${formatQuantity(row.quantity)}, 0, t.id FROM locations l JOIN products p ON p.business_id = @mdl_business_id AND p.sku = ${sqlString(row.internalSku)} JOIN inventory_transactions t ON t.business_id = @mdl_business_id AND t.location_id = l.id AND t.product_id = p.id AND t.reference_type = 'ACTUAL_STOCK_IMPORT' WHERE l.business_id = @mdl_business_id AND l.code = ${sqlString(row.locationCode)};`,
    );
  }
  return `${lines.join('\n')}\n`;
}

mkdirSync(outputDir, { recursive: true });
mkdirSync(dirname(migrationPath), { recursive: true });
const datasets = [];
for (const source of SOURCES) {
  const rows = await readSource(source);
  datasets.push({ source, rows });
  writeCsv(source, rows);
}

const allRows = datasets.flatMap(({ rows }) => rows);
writeFileSync(migrationPath, buildMigration(allRows), 'utf8');

const globalCodes = new Map();
for (const row of allRows) {
  const entries = globalCodes.get(row.itemCode) || [];
  entries.push(row);
  globalCodes.set(row.itemCode, entries);
}
const crossShopConflicts = [...globalCodes.values()].filter(
  (rows) =>
    rows.length > 1 &&
    new Set(
      rows.map(
        (row) =>
          `${row.itemName}|${row.category}|${row.costPrice.toFixed(2)}|${row.sellingPrice.toFixed(2)}`,
      ),
    ).size > 1,
);

for (const { source, rows } of datasets) {
  console.log(
    `${source.label}: ${rows.length} rows; ${rows.filter((row) => row.quantity < 0).length} negative; ` +
      `${rows.filter((row) => row.quantity === 0).length} zero; ` +
      `${rows.filter((row) => row.costPrice === 0).length} zero cost; ` +
      `${rows.filter((row) => row.sellingPrice === 0).length} zero selling price`,
  );
}
console.log(`Cross-shop item-code/product conflicts namespaced internally: ${crossShopConflicts.length}`);
console.log(`Clean CSVs: ${outputDir}`);
console.log(`Flyway import: ${migrationPath}`);
