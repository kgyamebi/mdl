-- =============================================================================
-- V5: MDL (Modern Dream Light) seed — locations, warehouses, shops, routes
-- Demonstrates multiple MAIN warehouses and shop structure for demo workflows.
-- =============================================================================

SET @mdl_business_id = (SELECT id FROM businesses WHERE code = 'MDL');

-- ---------------------------------------------------------------------------
-- Locations
-- ---------------------------------------------------------------------------

INSERT INTO locations (business_id, name, code, location_type, city, country, status) VALUES
    (@mdl_business_id, 'Main Import Warehouse',       'LOC-MAIN',   'WAREHOUSE', 'Accra', 'Ghana', 'ACTIVE'),
    (@mdl_business_id, 'Regional Distribution Center',  'LOC-MAIN-B', 'WAREHOUSE', 'Accra', 'Ghana', 'ACTIVE'),
    (@mdl_business_id, 'Shop A',                        'LOC-SHOP-A', 'SHOP',      'Accra', 'Ghana', 'ACTIVE'),
    (@mdl_business_id, 'Shop A Warehouse',              'LOC-WH-A',   'WAREHOUSE', 'Accra', 'Ghana', 'ACTIVE'),
    (@mdl_business_id, 'Shop B',                        'LOC-SHOP-B', 'SHOP',      'Accra', 'Ghana', 'ACTIVE'),
    (@mdl_business_id, 'Shop B Warehouse',              'LOC-WH-B',   'WAREHOUSE', 'Accra', 'Ghana', 'ACTIVE'),
    (@mdl_business_id, 'Shop C',                        'LOC-SHOP-C', 'SHOP',      'Accra', 'Ghana', 'ACTIVE'),
    (@mdl_business_id, 'Shop C Warehouse',              'LOC-WH-C',   'WAREHOUSE', 'Accra', 'Ghana', 'ACTIVE');

SET @loc_main   = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-MAIN');
SET @loc_main_b = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-MAIN-B');
SET @loc_shop_a = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-SHOP-A');
SET @loc_wh_a   = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-WH-A');
SET @loc_shop_b = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-SHOP-B');
SET @loc_wh_b   = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-WH-B');
SET @loc_shop_c = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-SHOP-C');
SET @loc_wh_c   = (SELECT id FROM locations WHERE business_id = @mdl_business_id AND code = 'LOC-WH-C');

-- ---------------------------------------------------------------------------
-- Warehouses — two MAIN warehouses (restricted) + three shop warehouses
-- ---------------------------------------------------------------------------

INSERT INTO warehouses (business_id, location_id, name, code, warehouse_type, is_restricted, description, status) VALUES
    (@mdl_business_id, @loc_main,   'Main Import Warehouse',       'WH-MAIN',   'MAIN',  TRUE,  'Central import receiving — owner controlled', 'ACTIVE'),
    (@mdl_business_id, @loc_main_b, 'Regional Distribution Center',  'WH-MAIN-B', 'MAIN',  TRUE,  'Secondary main warehouse', 'ACTIVE'),
    (@mdl_business_id, @loc_wh_a,   'Shop A Warehouse',            'WH-SHOP-A', 'SHOP',  FALSE, 'Shop A stock storage', 'ACTIVE'),
    (@mdl_business_id, @loc_wh_b,   'Shop B Warehouse',            'WH-SHOP-B', 'SHOP',  FALSE, 'Shop B stock storage', 'ACTIVE'),
    (@mdl_business_id, @loc_wh_c,   'Shop C Warehouse',            'WH-SHOP-C', 'SHOP',  FALSE, 'Shop C stock storage', 'ACTIVE');

SET @wh_main   = (SELECT id FROM warehouses WHERE business_id = @mdl_business_id AND code = 'WH-MAIN');
SET @wh_main_b = (SELECT id FROM warehouses WHERE business_id = @mdl_business_id AND code = 'WH-MAIN-B');
SET @wh_shop_a = (SELECT id FROM warehouses WHERE business_id = @mdl_business_id AND code = 'WH-SHOP-A');
SET @wh_shop_b = (SELECT id FROM warehouses WHERE business_id = @mdl_business_id AND code = 'WH-SHOP-B');
SET @wh_shop_c = (SELECT id FROM warehouses WHERE business_id = @mdl_business_id AND code = 'WH-SHOP-C');

-- ---------------------------------------------------------------------------
-- Shops — each linked to its shop warehouse
-- ---------------------------------------------------------------------------

INSERT INTO shops (business_id, location_id, warehouse_id, name, code, status) VALUES
    (@mdl_business_id, @loc_shop_a, @wh_shop_a, 'Shop A', 'SHOP-A', 'ACTIVE'),
    (@mdl_business_id, @loc_shop_b, @wh_shop_b, 'Shop B', 'SHOP-B', 'ACTIVE'),
    (@mdl_business_id, @loc_shop_c, @wh_shop_c, 'Shop C', 'SHOP-C', 'ACTIVE');

-- ---------------------------------------------------------------------------
-- Authorized transfer routes
-- Main warehouses can distribute to shop warehouses.
-- Shop B may receive from Shop A warehouse (nearby warehouse example).
-- ---------------------------------------------------------------------------

INSERT INTO warehouse_transfer_routes (business_id, from_warehouse_id, to_warehouse_id, enabled, notes) VALUES
    (@mdl_business_id, @wh_main,   @wh_shop_a, TRUE, 'Main → Shop A distribution'),
    (@mdl_business_id, @wh_main,   @wh_shop_b, TRUE, 'Main → Shop B distribution'),
    (@mdl_business_id, @wh_main,   @wh_shop_c, TRUE, 'Main → Shop C distribution'),
    (@mdl_business_id, @wh_main_b, @wh_shop_a, TRUE, 'Regional main → Shop A'),
    (@mdl_business_id, @wh_main_b, @wh_shop_b, TRUE, 'Regional main → Shop B'),
    (@mdl_business_id, @wh_main_b, @wh_shop_c, TRUE, 'Regional main → Shop C'),
    (@mdl_business_id, @wh_main,   @wh_main_b, TRUE, 'Main → Regional main transfer'),
    (@mdl_business_id, @wh_shop_a, @wh_shop_b, TRUE, 'Nearby: Shop A WH → Shop B WH');
