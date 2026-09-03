-- =============================================================================
-- V9: MDL demo inventory — opening balances with matching ledger entries
-- Main warehouse holds bulk stock; shop warehouses hold retail quantities.
-- =============================================================================

SET @mdl_business_id = (SELECT id FROM businesses WHERE code = 'MDL');
SET @loc_main   = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-MAIN');
SET @loc_wh_a   = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-WH-A');
SET @loc_wh_b   = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-WH-B');

-- Helper: seed one balance row via opening transaction
-- Main warehouse — bulk import stock
INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_main, p.id, 'OPENING_BALANCE', 500.0000, 500.0000, 'SEED', 'Initial main warehouse stock'
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-LED-001';
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_main, p.id, 500.0000, LAST_INSERT_ID()
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-LED-001';

INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_main, p.id, 'OPENING_BALANCE', 2000.0000, 2000.0000, 'SEED', 'Initial main warehouse stock'
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-LED-002';
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_main, p.id, 2000.0000, LAST_INSERT_ID()
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-LED-002';

INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_main, p.id, 'OPENING_BALANCE', 800.0000, 800.0000, 'SEED', 'Initial main warehouse stock'
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-LED-003';
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_main, p.id, 800.0000, LAST_INSERT_ID()
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-LED-003';

INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_main, p.id, 'OPENING_BALANCE', 600.0000, 600.0000, 'SEED', 'Initial main warehouse stock'
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-SWT-001';
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_main, p.id, 600.0000, LAST_INSERT_ID()
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-SWT-001';

INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_main, p.id, 'OPENING_BALANCE', 5000.0000, 5000.0000, 'SEED', 'Initial main warehouse stock (metres)'
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-CBL-001';
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_main, p.id, 5000.0000, LAST_INSERT_ID()
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-CBL-001';

INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_main, p.id, 'OPENING_BALANCE', 200.0000, 200.0000, 'SEED', 'Initial main warehouse stock'
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-PRT-001';
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_main, p.id, 200.0000, LAST_INSERT_ID()
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-PRT-001';

-- Shop A warehouse — retail floor stock
INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_wh_a, p.id, 'OPENING_BALANCE', 120.0000, 120.0000, 'SEED', 'Initial Shop A warehouse stock'
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-LED-002';
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_wh_a, p.id, 120.0000, LAST_INSERT_ID()
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-LED-002';

INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_wh_a, p.id, 'OPENING_BALANCE', 45.0000, 45.0000, 'SEED', 'Initial Shop A warehouse stock'
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-SWT-001';
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_wh_a, p.id, 45.0000, LAST_INSERT_ID()
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-SWT-001';

INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_wh_a, p.id, 'OPENING_BALANCE', 30.0000, 30.0000, 'SEED', 'Initial Shop A warehouse stock'
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-SWT-003';
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_wh_a, p.id, 30.0000, LAST_INSERT_ID()
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-SWT-003';

INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_wh_a, p.id, 'OPENING_BALANCE', 8.0000, 8.0000, 'SEED', 'Low stock demo — below reorder level'
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-ACC-001';
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_wh_a, p.id, 8.0000, LAST_INSERT_ID()
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-ACC-001';

-- Shop B warehouse
INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_wh_b, p.id, 'OPENING_BALANCE', 90.0000, 90.0000, 'SEED', 'Initial Shop B warehouse stock'
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-LED-002';
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_wh_b, p.id, 90.0000, LAST_INSERT_ID()
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-LED-002';

INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_wh_b, p.id, 'OPENING_BALANCE', 20.0000, 20.0000, 'SEED', 'Initial Shop B warehouse stock'
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-SWT-003';
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_wh_b, p.id, 20.0000, LAST_INSERT_ID()
FROM products p WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-SWT-003';
