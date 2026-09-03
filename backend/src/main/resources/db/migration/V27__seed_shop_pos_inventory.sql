-- =============================================================================
-- V27: POS-ready stock at shop warehouses + missing main-warehouse balances
-- Sales deduct from each shop's linked warehouse location (LOC-WH-*).
-- =============================================================================

SET @mdl_business_id = (SELECT id FROM businesses WHERE code = 'MDL');
SET @loc_main   = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-MAIN');
SET @loc_wh_a   = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-WH-A');
SET @loc_wh_b   = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-WH-B');
SET @loc_wh_c   = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-WH-C');

-- Main warehouse — cable SKU used in POS demos
INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_main, p.id, 'OPENING_BALANCE', 3000.0000, 3000.0000, 'SEED', 'V27 — main warehouse 4mm cable'
FROM products p
WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-CBL-002'
  AND NOT EXISTS (
      SELECT 1 FROM inventory_balances b
      WHERE b.business_id = @mdl_business_id AND b.location_id = @loc_main AND b.product_id = p.id
  );
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_main, p.id, 3000.0000, LAST_INSERT_ID()
FROM products p
WHERE p.business_id = @mdl_business_id AND p.sku = 'MDL-CBL-002'
  AND NOT EXISTS (
      SELECT 1 FROM inventory_balances b
      WHERE b.business_id = @mdl_business_id AND b.location_id = @loc_main AND b.product_id = p.id
  );

-- Shop A warehouse — products missing for counter sales
INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_wh_a, p.id, 'OPENING_BALANCE', v.qty, v.qty, 'SEED', 'V27 — Shop A POS stock'
FROM products p
JOIN (
    SELECT 'MDL-LED-001' AS sku, 25.0000 AS qty UNION ALL
    SELECT 'MDL-CBL-002', 150.0000 UNION ALL
    SELECT 'MDL-CBL-001', 200.0000 UNION ALL
    SELECT 'MDL-SWT-002', 30.0000
) v ON v.sku = p.sku
WHERE p.business_id = @mdl_business_id
  AND NOT EXISTS (
      SELECT 1 FROM inventory_balances b
      WHERE b.business_id = @mdl_business_id AND b.location_id = @loc_wh_a AND b.product_id = p.id
  );
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_wh_a, p.id, v.qty, t.id
FROM products p
JOIN (
    SELECT 'MDL-LED-001' AS sku, 25.0000 AS qty UNION ALL
    SELECT 'MDL-CBL-002', 150.0000 UNION ALL
    SELECT 'MDL-CBL-001', 200.0000 UNION ALL
    SELECT 'MDL-SWT-002', 30.0000
) v ON v.sku = p.sku
JOIN inventory_transactions t
  ON t.business_id = @mdl_business_id
 AND t.location_id = @loc_wh_a
 AND t.product_id = p.id
 AND t.reference_type = 'SEED'
 AND t.notes = 'V27 — Shop A POS stock'
WHERE p.business_id = @mdl_business_id
  AND NOT EXISTS (
      SELECT 1 FROM inventory_balances b
      WHERE b.business_id = @mdl_business_id AND b.location_id = @loc_wh_a AND b.product_id = p.id
  );

-- Shop B warehouse — retail floor stock for common SKUs
INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_wh_b, p.id, 'OPENING_BALANCE', v.qty, v.qty, 'SEED', 'V27 — Shop B POS stock'
FROM products p
JOIN (
    SELECT 'MDL-SWT-001' AS sku, 35.0000 AS qty UNION ALL
    SELECT 'MDL-CBL-002', 120.0000 UNION ALL
    SELECT 'MDL-CBL-001', 80.0000 UNION ALL
    SELECT 'MDL-SWT-002', 25.0000 UNION ALL
    SELECT 'MDL-LED-003', 40.0000 UNION ALL
    SELECT 'MDL-ACC-001', 15.0000 UNION ALL
    SELECT 'MDL-ACC-002', 20.0000
) v ON v.sku = p.sku
WHERE p.business_id = @mdl_business_id
  AND NOT EXISTS (
      SELECT 1 FROM inventory_balances b
      WHERE b.business_id = @mdl_business_id AND b.location_id = @loc_wh_b AND b.product_id = p.id
  );
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_wh_b, p.id, v.qty, t.id
FROM products p
JOIN (
    SELECT 'MDL-SWT-001' AS sku, 35.0000 AS qty UNION ALL
    SELECT 'MDL-CBL-002', 120.0000 UNION ALL
    SELECT 'MDL-CBL-001', 80.0000 UNION ALL
    SELECT 'MDL-SWT-002', 25.0000 UNION ALL
    SELECT 'MDL-LED-003', 40.0000 UNION ALL
    SELECT 'MDL-ACC-001', 15.0000 UNION ALL
    SELECT 'MDL-ACC-002', 20.0000
) v ON v.sku = p.sku
JOIN inventory_transactions t
  ON t.business_id = @mdl_business_id
 AND t.location_id = @loc_wh_b
 AND t.product_id = p.id
 AND t.reference_type = 'SEED'
 AND t.notes = 'V27 — Shop B POS stock'
WHERE p.business_id = @mdl_business_id
  AND NOT EXISTS (
      SELECT 1 FROM inventory_balances b
      WHERE b.business_id = @mdl_business_id AND b.location_id = @loc_wh_b AND b.product_id = p.id
  );

-- Shop C warehouse — initial retail stock (was empty)
INSERT INTO inventory_transactions (business_id, location_id, product_id, transaction_type, quantity_change, quantity_after, reference_type, notes)
SELECT @mdl_business_id, @loc_wh_c, p.id, 'OPENING_BALANCE', v.qty, v.qty, 'SEED', 'V27 — Shop C POS stock'
FROM products p
JOIN (
    SELECT 'MDL-LED-002' AS sku, 75.0000 AS qty UNION ALL
    SELECT 'MDL-SWT-001', 30.0000 UNION ALL
    SELECT 'MDL-SWT-003', 18.0000 UNION ALL
    SELECT 'MDL-CBL-002', 100.0000 UNION ALL
    SELECT 'MDL-CBL-001', 60.0000 UNION ALL
    SELECT 'MDL-ACC-001', 12.0000 UNION ALL
    SELECT 'MDL-LED-004', 22.0000
) v ON v.sku = p.sku
WHERE p.business_id = @mdl_business_id
  AND NOT EXISTS (
      SELECT 1 FROM inventory_balances b
      WHERE b.business_id = @mdl_business_id AND b.location_id = @loc_wh_c AND b.product_id = p.id
  );
INSERT INTO inventory_balances (business_id, location_id, product_id, quantity_on_hand, last_transaction_id)
SELECT @mdl_business_id, @loc_wh_c, p.id, v.qty, t.id
FROM products p
JOIN (
    SELECT 'MDL-LED-002' AS sku, 75.0000 AS qty UNION ALL
    SELECT 'MDL-SWT-001', 30.0000 UNION ALL
    SELECT 'MDL-SWT-003', 18.0000 UNION ALL
    SELECT 'MDL-CBL-002', 100.0000 UNION ALL
    SELECT 'MDL-CBL-001', 60.0000 UNION ALL
    SELECT 'MDL-ACC-001', 12.0000 UNION ALL
    SELECT 'MDL-LED-004', 22.0000
) v ON v.sku = p.sku
JOIN inventory_transactions t
  ON t.business_id = @mdl_business_id
 AND t.location_id = @loc_wh_c
 AND t.product_id = p.id
 AND t.reference_type = 'SEED'
 AND t.notes = 'V27 — Shop C POS stock'
WHERE p.business_id = @mdl_business_id
  AND NOT EXISTS (
      SELECT 1 FROM inventory_balances b
      WHERE b.business_id = @mdl_business_id AND b.location_id = @loc_wh_c AND b.product_id = p.id
  );
