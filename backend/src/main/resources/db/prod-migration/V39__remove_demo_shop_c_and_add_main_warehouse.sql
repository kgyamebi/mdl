-- =============================================================================
-- V39: Retire the leftover demo "Shop C" structure and establish the real
-- central receiving warehouse.
--
-- V38 repurposed both demo MAIN warehouses (WH-MAIN, WH-MAIN-B) into shop
-- warehouses B and D, which left the business with zero MAIN warehouses.
-- ImportService requires a MAIN warehouse destination, so container imports
-- would be rejected without the warehouse created here.
--
-- Written to be re-runnable: MariaDB does not roll back DDL, so a migration
-- that stops partway must be safe to apply again.
-- =============================================================================

SET @mdl_business_id = (SELECT id FROM businesses WHERE code = 'MDL');

-- ---------------------------------------------------------------------------
-- Remove the demo Shop C structure (never used for real stock)
-- ---------------------------------------------------------------------------

SET @loc_shop_c = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-SHOP-C');
SET @loc_wh_c = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-WH-C');
SET @wh_shop_c = (SELECT id FROM warehouses WHERE business_id = @mdl_business_id AND code = 'WH-SHOP-C');

DELETE FROM warehouse_transfer_routes
WHERE business_id = @mdl_business_id
  AND (from_warehouse_id = @wh_shop_c OR to_warehouse_id = @wh_shop_c);

UPDATE inventory_balances SET last_transaction_id = NULL
WHERE business_id = @mdl_business_id AND location_id IN (@loc_shop_c, @loc_wh_c);

DELETE FROM inventory_balances
WHERE business_id = @mdl_business_id AND location_id IN (@loc_shop_c, @loc_wh_c);

DELETE FROM inventory_transactions
WHERE business_id = @mdl_business_id AND location_id IN (@loc_shop_c, @loc_wh_c);

DELETE FROM user_location_assignments
WHERE business_id = @mdl_business_id AND location_id IN (@loc_shop_c, @loc_wh_c);

UPDATE shops SET warehouse_id = NULL
WHERE business_id = @mdl_business_id AND code = 'SHOP-C';

DELETE FROM shops WHERE business_id = @mdl_business_id AND code = 'SHOP-C';
DELETE FROM warehouses WHERE business_id = @mdl_business_id AND code = 'WH-SHOP-C';
DELETE FROM locations WHERE business_id = @mdl_business_id AND code IN ('LOC-SHOP-C', 'LOC-WH-C');

-- ---------------------------------------------------------------------------
-- Central receiving warehouse — restricted to owner/manager oversight
-- ---------------------------------------------------------------------------

INSERT INTO locations (business_id, name, code, location_type, city, country, status)
SELECT @mdl_business_id, 'MODERN DREAM MAIN WAREHOUSE', 'LOC-WH-MAIN', 'WAREHOUSE', 'Accra', 'Ghana', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM locations
    WHERE business_id = @mdl_business_id AND code = 'LOC-WH-MAIN'
);

SET @loc_wh_main = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-WH-MAIN');

INSERT INTO warehouses (business_id, location_id, name, code, warehouse_type, is_restricted, description, status)
SELECT @mdl_business_id, @loc_wh_main, 'MODERN DREAM MAIN WAREHOUSE', 'WH-MAIN', 'MAIN', TRUE,
       'Central import receiving warehouse — distributes to both shops', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM warehouses
    WHERE business_id = @mdl_business_id AND code = 'WH-MAIN'
);

-- ---------------------------------------------------------------------------
-- Transfer routes: main warehouse distributes to every shop warehouse, and
-- shop warehouses can return stock to the main warehouse.
-- ---------------------------------------------------------------------------

INSERT INTO warehouse_transfer_routes (business_id, from_warehouse_id, to_warehouse_id, enabled, notes)
SELECT @mdl_business_id, mw.id, sw.id, TRUE, CONCAT('Distribution: ', mw.name, ' -> ', sw.name)
FROM warehouses mw
JOIN warehouses sw
  ON sw.business_id = mw.business_id
 AND sw.warehouse_type = 'SHOP'
 AND sw.status = 'ACTIVE'
WHERE mw.business_id = @mdl_business_id
  AND mw.code = 'WH-MAIN'
  AND NOT EXISTS (
      SELECT 1 FROM warehouse_transfer_routes r
      WHERE r.business_id = @mdl_business_id
        AND r.from_warehouse_id = mw.id
        AND r.to_warehouse_id = sw.id
  );

INSERT INTO warehouse_transfer_routes (business_id, from_warehouse_id, to_warehouse_id, enabled, notes)
SELECT @mdl_business_id, sw.id, mw.id, TRUE, CONCAT('Return: ', sw.name, ' -> ', mw.name)
FROM warehouses mw
JOIN warehouses sw
  ON sw.business_id = mw.business_id
 AND sw.warehouse_type = 'SHOP'
 AND sw.status = 'ACTIVE'
WHERE mw.business_id = @mdl_business_id
  AND mw.code = 'WH-MAIN'
  AND NOT EXISTS (
      SELECT 1 FROM warehouse_transfer_routes r
      WHERE r.business_id = @mdl_business_id
        AND r.from_warehouse_id = sw.id
        AND r.to_warehouse_id = mw.id
  );
