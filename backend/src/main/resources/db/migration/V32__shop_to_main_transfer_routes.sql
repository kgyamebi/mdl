-- V32: Shop stock → main warehouse return routes (complement existing main → shop distribution).

INSERT INTO warehouse_transfer_routes (business_id, from_warehouse_id, to_warehouse_id, enabled, notes)
SELECT sw.business_id, sw.id, mw.id, TRUE,
       CONCAT('Return: ', sw.name, ' → ', mw.name)
FROM warehouses sw
JOIN warehouses mw
  ON mw.business_id = sw.business_id
 AND mw.warehouse_type = 'MAIN'
 AND mw.status = 'ACTIVE'
WHERE sw.warehouse_type = 'SHOP'
  AND sw.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM warehouse_transfer_routes r
      WHERE r.business_id = sw.business_id
        AND r.from_warehouse_id = sw.id
        AND r.to_warehouse_id = mw.id
  );
