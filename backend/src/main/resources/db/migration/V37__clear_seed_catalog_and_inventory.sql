-- =============================================================================
-- V37: Clear test/demo product catalog and inventory for production readiness.
-- Keeps business structure (shops, warehouses, locations, roles, routes).
-- Leaves products/categories/stock empty so real data can be loaded later.
-- =============================================================================

SET @mdl_business_id = (SELECT id FROM businesses WHERE code = 'MDL');

-- Approvals tied to stock workflows
DELETE aia FROM approval_instance_actions aia
JOIN approval_instances ai ON ai.id = aia.approval_instance_id
WHERE ai.business_id = @mdl_business_id;

DELETE FROM approval_instances WHERE business_id = @mdl_business_id;

-- Sales / returns
DELETE sri FROM sale_return_items sri
JOIN sale_returns sr ON sr.id = sri.sale_return_id
WHERE sr.business_id = @mdl_business_id;

DELETE srr FROM sale_return_refunds srr
JOIN sale_returns sr ON sr.id = srr.sale_return_id
WHERE sr.business_id = @mdl_business_id;

DELETE FROM sale_returns WHERE business_id = @mdl_business_id;

DELETE sp FROM sale_payments sp
JOIN sales s ON s.id = sp.sale_id
WHERE s.business_id = @mdl_business_id;

DELETE si FROM sale_items si
JOIN sales s ON s.id = si.sale_id
WHERE s.business_id = @mdl_business_id;

DELETE FROM sales WHERE business_id = @mdl_business_id;

-- Transfers
DELETE sti FROM stock_transfer_items sti
JOIN stock_transfers st ON st.id = sti.transfer_id
WHERE st.business_id = @mdl_business_id;

DELETE FROM stock_transfers WHERE business_id = @mdl_business_id;

-- Imports
DELETE FROM import_evidence WHERE business_id = @mdl_business_id;

DELETE ii FROM import_items ii
JOIN imports i ON i.id = ii.import_id
WHERE i.business_id = @mdl_business_id;

DELETE FROM imports WHERE business_id = @mdl_business_id;

-- Stocktakes
DELETE stl FROM stocktake_lines stl
JOIN stocktakes st ON st.id = stl.stocktake_id
WHERE st.business_id = @mdl_business_id;

DELETE FROM stocktakes WHERE business_id = @mdl_business_id;

-- Inventory workflows + ledger
DELETE FROM inventory_reservations WHERE business_id = @mdl_business_id;
DELETE FROM inventory_adjustment_requests WHERE business_id = @mdl_business_id;
UPDATE inventory_balances SET last_transaction_id = NULL WHERE business_id = @mdl_business_id;
DELETE FROM inventory_balances WHERE business_id = @mdl_business_id;
DELETE FROM inventory_transactions WHERE business_id = @mdl_business_id;

-- Catalog
DELETE FROM barcodes WHERE business_id = @mdl_business_id;
DELETE FROM products WHERE business_id = @mdl_business_id;
DELETE FROM product_categories WHERE business_id = @mdl_business_id;

-- Operational noise from test stock
DELETE FROM business_alerts WHERE business_id = @mdl_business_id;
DELETE FROM report_exports WHERE business_id = @mdl_business_id;
DELETE FROM notifications WHERE business_id = @mdl_business_id;
