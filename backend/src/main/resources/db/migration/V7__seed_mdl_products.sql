-- =============================================================================
-- V7: MDL demo product catalog (electrical products)
-- Prices are in business currency (GHS) — not hard-coded in application logic.
-- =============================================================================

INSERT INTO product_categories (business_id, parent_id, name, code, description, sort_order)
SELECT b.id, NULL, 'Lighting', 'LIGHTING', 'LED panels, bulbs, and tubes', 10
FROM businesses b WHERE b.code = 'MDL';

INSERT INTO product_categories (business_id, parent_id, name, code, description, sort_order)
SELECT b.id, NULL, 'Switches & Sockets', 'SWITCHES', 'Switches, sockets, and outlets', 20
FROM businesses b WHERE b.code = 'MDL';

INSERT INTO product_categories (business_id, parent_id, name, code, description, sort_order)
SELECT b.id, NULL, 'Cables & Wiring', 'CABLES', 'Electrical cables sold by the metre', 30
FROM businesses b WHERE b.code = 'MDL';

INSERT INTO product_categories (business_id, parent_id, name, code, description, sort_order)
SELECT b.id, NULL, 'Circuit Protection', 'PROTECTION', 'MCBs, RCBOs, and fuses', 40
FROM businesses b WHERE b.code = 'MDL';

INSERT INTO product_categories (business_id, parent_id, name, code, description, sort_order)
SELECT b.id, NULL, 'Tools & Accessories', 'ACCESSORIES', 'Tape, junction boxes, and consumables', 50
FROM businesses b WHERE b.code = 'MDL';

-- Lighting
INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-LED-001', 'LED Panel 60x60 48W', '600x600mm recessed LED panel, 6500K cool white', 'MDL', 'PIECE', 145.0000, 185.0000, 10
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'LIGHTING' WHERE b.code = 'MDL';

INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-LED-002', 'LED Bulb 12W E27 Warm White', 'A60 shape, 2700K warm white, energy saving', 'MDL', 'PIECE', 12.0000, 18.0000, 50
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'LIGHTING' WHERE b.code = 'MDL';

INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-LED-003', 'LED Tube 18W 4ft', 'T8 replacement tube, 6500K, includes starter', 'MDL', 'PIECE', 25.0000, 35.0000, 30
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'LIGHTING' WHERE b.code = 'MDL';

INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-LED-004', 'LED Downlight 7W Round', 'Recessed downlight, 3000K, cutout 75mm', 'MDL', 'PIECE', 30.0000, 42.0000, 25
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'LIGHTING' WHERE b.code = 'MDL';

-- Switches & Sockets
INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-SWT-001', '1-Gang Switch White', '13A rocker switch, white plastic', 'MDL', 'PIECE', 8.0000, 12.0000, 40
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'SWITCHES' WHERE b.code = 'MDL';

INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-SWT-002', '2-Gang Switch White', '13A double rocker switch, white plastic', 'MDL', 'PIECE', 12.0000, 18.0000, 30
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'SWITCHES' WHERE b.code = 'MDL';

INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-SWT-003', 'Double Socket 13A White', 'Twin switched socket outlet, white', 'MDL', 'PIECE', 15.0000, 22.0000, 35
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'SWITCHES' WHERE b.code = 'MDL';

INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-SWT-004', 'USB Double Socket', 'Twin socket with 2x USB-A charging ports', 'MDL', 'PIECE', 65.0000, 85.0000, 15
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'SWITCHES' WHERE b.code = 'MDL';

-- Cables
INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-CBL-001', '2.5mm Twin & Earth Cable', 'Sold per metre — PVC insulated', 'MDL', 'METRE', 6.0000, 8.5000, 100
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'CABLES' WHERE b.code = 'MDL';

INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-CBL-002', '4mm Twin & Earth Cable', 'Sold per metre — PVC insulated', 'MDL', 'METRE', 10.0000, 14.0000, 80
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'CABLES' WHERE b.code = 'MDL';

INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-CBL-003', '1.5mm Single Core Cable', 'Sold per metre — red or black', 'MDL', 'METRE', 2.5000, 4.0000, 150
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'CABLES' WHERE b.code = 'MDL';

-- Circuit Protection
INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-PRT-001', 'MCB 32A Single Pole', 'Type B, DIN rail mount', 'MDL', 'PIECE', 32.0000, 45.0000, 20
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'PROTECTION' WHERE b.code = 'MDL';

INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-PRT-002', 'MCB 63A Double Pole', 'Type C, DIN rail mount', 'MDL', 'PIECE', 90.0000, 120.0000, 10
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'PROTECTION' WHERE b.code = 'MDL';

INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-PRT-003', 'RCBO 40A 30mA', 'Combined MCB + RCD, DIN rail mount', 'MDL', 'PIECE', 125.0000, 165.0000, 8
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'PROTECTION' WHERE b.code = 'MDL';

-- Accessories
INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-ACC-001', 'Electrical Tape Black', '19mm x 20m PVC insulation tape', 'MDL', 'PIECE', 3.0000, 5.0000, 60
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'ACCESSORIES' WHERE b.code = 'MDL';

INSERT INTO products (business_id, category_id, sku, name, description, brand, unit_of_measure, cost_price, selling_price, reorder_level)
SELECT b.id, c.id, 'MDL-ACC-002', 'Junction Box 3x3', 'PVC junction box with lid', 'MDL', 'PIECE', 5.0000, 8.0000, 40
FROM businesses b JOIN product_categories c ON c.business_id = b.id AND c.code = 'ACCESSORIES' WHERE b.code = 'MDL';

-- Barcodes (628 prefix = Ghana GS1 country code)
INSERT INTO barcodes (business_id, product_id, barcode, barcode_type, is_primary)
SELECT b.id, p.id, '6281234567001', 'EAN13', TRUE
FROM businesses b JOIN products p ON p.business_id = b.id AND p.sku = 'MDL-LED-001' WHERE b.code = 'MDL';

INSERT INTO barcodes (business_id, product_id, barcode, barcode_type, is_primary)
SELECT b.id, p.id, '6281234567002', 'EAN13', TRUE
FROM businesses b JOIN products p ON p.business_id = b.id AND p.sku = 'MDL-LED-002' WHERE b.code = 'MDL';

INSERT INTO barcodes (business_id, product_id, barcode, barcode_type, is_primary)
SELECT b.id, p.id, '6281234567003', 'EAN13', TRUE
FROM businesses b JOIN products p ON p.business_id = b.id AND p.sku = 'MDL-LED-003' WHERE b.code = 'MDL';

INSERT INTO barcodes (business_id, product_id, barcode, barcode_type, is_primary)
SELECT b.id, p.id, '6281234567004', 'EAN13', TRUE
FROM businesses b JOIN products p ON p.business_id = b.id AND p.sku = 'MDL-LED-004' WHERE b.code = 'MDL';

INSERT INTO barcodes (business_id, product_id, barcode, barcode_type, is_primary)
SELECT b.id, p.id, '6281234567011', 'EAN13', TRUE
FROM businesses b JOIN products p ON p.business_id = b.id AND p.sku = 'MDL-SWT-001' WHERE b.code = 'MDL';

INSERT INTO barcodes (business_id, product_id, barcode, barcode_type, is_primary)
SELECT b.id, p.id, '6281234567012', 'EAN13', TRUE
FROM businesses b JOIN products p ON p.business_id = b.id AND p.sku = 'MDL-SWT-002' WHERE b.code = 'MDL';

INSERT INTO barcodes (business_id, product_id, barcode, barcode_type, is_primary)
SELECT b.id, p.id, '6281234567013', 'EAN13', TRUE
FROM businesses b JOIN products p ON p.business_id = b.id AND p.sku = 'MDL-SWT-003' WHERE b.code = 'MDL';

INSERT INTO barcodes (business_id, product_id, barcode, barcode_type, is_primary)
SELECT b.id, p.id, '6281234567014', 'EAN13', TRUE
FROM businesses b JOIN products p ON p.business_id = b.id AND p.sku = 'MDL-SWT-004' WHERE b.code = 'MDL';

INSERT INTO barcodes (business_id, product_id, barcode, barcode_type, is_primary)
SELECT b.id, p.id, '6281234567021', 'EAN13', TRUE
FROM businesses b JOIN products p ON p.business_id = b.id AND p.sku = 'MDL-CBL-001' WHERE b.code = 'MDL';

INSERT INTO barcodes (business_id, product_id, barcode, barcode_type, is_primary)
SELECT b.id, p.id, '6281234567022', 'EAN13', TRUE
FROM businesses b JOIN products p ON p.business_id = b.id AND p.sku = 'MDL-CBL-002' WHERE b.code = 'MDL';

INSERT INTO barcodes (business_id, product_id, barcode, barcode_type, is_primary)
SELECT b.id, p.id, '6281234567031', 'EAN13', TRUE
FROM businesses b JOIN products p ON p.business_id = b.id AND p.sku = 'MDL-PRT-001' WHERE b.code = 'MDL';

INSERT INTO barcodes (business_id, product_id, barcode, barcode_type, is_primary)
SELECT b.id, p.id, '6281234567032', 'EAN13', TRUE
FROM businesses b JOIN products p ON p.business_id = b.id AND p.sku = 'MDL-PRT-002' WHERE b.code = 'MDL';

INSERT INTO barcodes (business_id, product_id, barcode, barcode_type, is_primary)
SELECT b.id, p.id, '6281234567041', 'EAN13', TRUE
FROM businesses b JOIN products p ON p.business_id = b.id AND p.sku = 'MDL-ACC-001' WHERE b.code = 'MDL';
