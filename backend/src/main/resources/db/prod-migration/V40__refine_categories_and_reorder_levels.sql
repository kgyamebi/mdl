-- =============================================================================
-- V40: Refine imported product categories and replace placeholder reorder levels.
--
-- Two problems from the stocktake import:
--
-- 1. "Tools & Accessories" was the catch-all bucket in the import script, so it
--    collected sockets, light fittings and changeover switches that never matched
--    an earlier keyword rule (e.g. "600X600 PIERLIGHT" missed the lighting rule
--    because "LIGHT" was not on a word boundary, and "HAVELL 32AMP C/O" missed
--    circuit protection because the rule only knew "CHANGEOVER").
--
-- 2. Every product was imported with reorder_level = 0, which made the low-stock
--    warning fire only at zero or negative stock — never as an early warning.
--
-- The reclassification below only moves products *out of* the catch-all bucket, so
-- categories the stocktaker got right are left untouched. Rule order matters and is
-- called out where two rules could both match.
-- =============================================================================

SET @mdl_business_id = (SELECT id FROM businesses WHERE code = 'MDL');

-- Security, detection and pest-control devices had no home of their own.
INSERT INTO product_categories (business_id, name, code, description, sort_order, status)
SELECT @mdl_business_id, 'Security & Safety', 'SECURITY_AND_SAFETY',
       'CCTV, detectors, sensors, intercoms and safety devices', 90, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM product_categories
    WHERE business_id = @mdl_business_id AND code = 'SECURITY_AND_SAFETY'
);

SET @cat_tools = (SELECT id FROM product_categories
                  WHERE business_id = @mdl_business_id AND code = 'TOOLS_AND_ACCESSORIES');
SET @cat_cables = (SELECT id FROM product_categories
                   WHERE business_id = @mdl_business_id AND code = 'CABLES_AND_WIRING');
SET @cat_protection = (SELECT id FROM product_categories
                       WHERE business_id = @mdl_business_id AND code = 'CIRCUIT_PROTECTION');
SET @cat_boards = (SELECT id FROM product_categories
                   WHERE business_id = @mdl_business_id AND code = 'DISTRIBUTION_BOARDS');
SET @cat_fans = (SELECT id FROM product_categories
                 WHERE business_id = @mdl_business_id AND code = 'FANS_AND_VENTILATION');
SET @cat_security = (SELECT id FROM product_categories
                     WHERE business_id = @mdl_business_id AND code = 'SECURITY_AND_SAFETY');
SET @cat_lighting = (SELECT id FROM product_categories
                     WHERE business_id = @mdl_business_id AND code = 'LIGHTING_AND_LAMPS');
SET @cat_conduit = (SELECT id FROM product_categories
                    WHERE business_id = @mdl_business_id AND code = 'CONDUIT_AND_TRUNKING');
SET @cat_switches = (SELECT id FROM product_categories
                     WHERE business_id = @mdl_business_id AND code = 'SWITCHES_AND_SOCKETS');

-- ---------------------------------------------------------------------------
-- Step 1 — Cables, flex and earthing.
-- Must run before the lighting rule: "4FT EARTH ROD COPPER" contains "4FT",
-- which the lighting rule also matches.
-- ---------------------------------------------------------------------------
UPDATE products SET category_id = @cat_cables
WHERE business_id = @mdl_business_id AND category_id = @cat_tools
  AND (name REGEXP 'EARTH ?ROD'
       OR name LIKE '%COREFLEX%'
       OR name LIKE '%RG59%'
       OR name LIKE '%NET CAB%'
       OR name REGEXP '^[0-9]+(\\.[0-9]+)?X[0-9]+ *MM'
       OR name REGEXP '^(0\\.75|1\\.5|2\\.5)X[0-9]');

-- ---------------------------------------------------------------------------
-- Step 2 — Changeover switches and multi-pole breakers ("C/O", "100AMP 2P").
-- Must run before the fan rule so "ORIENT C/O 100AM" is not read as an Orient fan.
-- ---------------------------------------------------------------------------
UPDATE products SET category_id = @cat_protection
WHERE business_id = @mdl_business_id AND category_id = @cat_tools
  AND (name LIKE '%C/O%'
       OR name REGEXP '[0-9]+(A|AM|AMP|AMPS)[[:space:]]*[0-9]?P([[:space:]]|$)'
       OR name REGEXP '[0-9]P[[:space:]]+[0-9]+A');

-- ---------------------------------------------------------------------------
-- Step 3 — Distribution boards, sized by way count ("6WAY SINGLE FACE").
-- ---------------------------------------------------------------------------
UPDATE products SET category_id = @cat_boards
WHERE business_id = @mdl_business_id AND category_id = @cat_tools
  AND name REGEXP '[0-9]+[[:space:]]*WAY';

-- ---------------------------------------------------------------------------
-- Step 4 — Fans, which the stocktaker recorded by model name rather than "FAN".
-- ---------------------------------------------------------------------------
UPDATE products SET category_id = @cat_fans
WHERE business_id = @mdl_business_id AND category_id = @cat_tools
  AND (name REGEXP '^BAJAJ'
       OR name LIKE '%CROMPTON%'
       OR name LIKE '%CRUZAIR%'
       OR name LIKE '%BRIZ AIR%'
       OR name LIKE '%NOVELLA%'
       OR name LIKE '%ENERGON%'
       OR name LIKE '%STYLO%'
       OR name LIKE '%ANTI DUST%'
       OR name LIKE 'ORIENT GRATIA%'
       OR name REGEXP 'REGAL[[:space:]]+(LONG|MEDIUM|SHORT)');

-- ---------------------------------------------------------------------------
-- Step 5 — Security and detection.
-- Must run before the switches rule, whose "TV" match would otherwise claim CCTV.
-- ---------------------------------------------------------------------------
UPDATE products SET category_id = @cat_security
WHERE business_id = @mdl_business_id AND category_id = @cat_tools
  AND (name LIKE '%CCTV%'
       OR name LIKE '%SMOKE DETECTOR%'
       OR name LIKE '%MOTION SENSOR%'
       OR name LIKE '%VIDEO INTERCOM%'
       OR name LIKE '%DOORBELL%'
       OR name LIKE '%DOOR CHINE%'
       OR name LIKE '%TALKBACK%'
       OR name LIKE '%MOSQUITO%'
       OR name LIKE '%INSECT%'
       OR name LIKE '%FIRE WORDS%');

-- ---------------------------------------------------------------------------
-- Step 6 — Light fittings, panels, chandeliers and control gear. Covers the
-- stocktaker's spellings: LGHT, CHADALIER, CHOCK, FITING, STRIPLITE, HALLOGEN.
-- "W/P" (waterproof) items in this bucket are fluorescent battens, not sockets.
-- ---------------------------------------------------------------------------
UPDATE products SET category_id = @cat_lighting
WHERE business_id = @mdl_business_id AND category_id = @cat_tools
  AND (name REGEXP '[0-9]+[[:space:]]*WATTS?'
       OR name REGEXP '[0-9]+FT'
       OR name REGEXP '600X(300|600|1200)'
       OR name LIKE '%FIT%'
       OR name LIKE '%W/P%'
       OR name LIKE '%RECESS%'
       OR name LIKE '%CHADAL%'
       OR name LIKE '%CHADEL%'
       OR name LIKE '%CHANDEL%'
       OR name LIKE '%CHOCK%'
       OR name LIKE '%CHOKE%'
       OR name LIKE '%GATELIGHT%'
       OR name LIKE '%GATE LGHT%'
       OR name LIKE '%GATE GSE%'
       OR name LIKE '%LGHT%'
       OR name LIKE '%PROFILE%'
       OR name LIKE '%ROPLIGHT%'
       OR name LIKE '%STRIPLITE%'
       OR name LIKE '%HALOGEN%'
       OR name LIKE '%HALLOGEN%'
       OR name LIKE '%DIFFUSER%'
       OR name LIKE '%PORSYLNE%'
       OR name LIKE '%SOLDIUM%'
       OR name LIKE '%MINISON%'
       OR name LIKE '%DROPPING%'
       OR name LIKE '%SWIM POOL%'
       OR name LIKE '%SPARE GLASS%'
       OR name LIKE '%E27%'
       OR name LIKE '%E-14%'
       OR name REGEXP 'GU[[:space:]]*10'
       OR name REGEXP 'MR[[:space:]]*1[56]'
       OR name REGEXP '(^|[^A-Z0-9])T[58]([^A-Z0-9]|$)');

-- ---------------------------------------------------------------------------
-- Step 7 — Conduit, junction and circular boxes, clips and bushes.
-- Must run before the switches rule, which also matches "COVER".
-- ---------------------------------------------------------------------------
UPDATE products SET category_id = @cat_conduit
WHERE business_id = @mdl_business_id AND category_id = @cat_tools
  AND (name LIKE '%JUNCTION%'
       OR name LIKE '%CIRCULAR%'
       OR name LIKE '%CONDUIT%'
       OR name LIKE '%COMDUIT%'
       OR name LIKE '%PVC CLIP%'
       OR name LIKE '%BUSH%');

-- ---------------------------------------------------------------------------
-- Step 8 — Wiring accessories: sockets, switch plates, A/C points, cooker
-- units, patress boxes and cover plates. Broadest rule, so it runs last.
-- Spellings are truncated in the source data ("DOUBL", "SINGLG"), so match stems.
-- ---------------------------------------------------------------------------
UPDATE products SET category_id = @cat_switches
WHERE business_id = @mdl_business_id AND category_id = @cat_tools
  AND (name LIKE '%SOCK%'
       OR name LIKE '%A/C%'
       OR name LIKE '%SWITCH%'
       OR name LIKE '%SINGL%'
       OR name LIKE '%DOUBL%'
       OR name LIKE '%GANG%'
       OR name LIKE '%COVER%'
       OR name LIKE '%PATRESS%'
       OR name LIKE '%COOKER%'
       OR name LIKE '%CARD SLOT%'
       OR name LIKE '%POWER CARD%'
       OR name LIKE '%ADAPTOR%'
       OR name LIKE '%EXTEN%'
       OR name LIKE '%NEON CONNECTOR%'
       OR name LIKE '%ZENITH%'
       OR name LIKE '%ZENTH%'
       OR name LIKE '%TV%'
       OR name REGEXP '[0-9]+(A|AM|AMP|AMPS|AMS)([[:space:]]|$)');

-- ---------------------------------------------------------------------------
-- Reorder levels
--
-- Every product was imported with reorder_level = 0, so "Low stock" only ever
-- fired once an item was already gone. A reorder level should instead be the
-- point at which you reorder while stock is still on the shelf.
--
-- There is no sales history yet, so the best available signal for how fast an
-- item moves is how much of it the business chose to hold at stocktake. The
-- level is set to roughly a third of that holding, then capped by price band so
-- expensive, slow-moving items don't trigger early reorders and tie up cash.
--
-- Deliberately not floored at 1: an item the business only ever holds one or two
-- of should be flagged when it runs out, not while it is still in stock.
--
-- Items counted at zero or negative have no holding to learn from, so they fall
-- back to a typical holding for their category. They already show as low today,
-- and will get a sensible trigger once restocked.
-- ---------------------------------------------------------------------------
UPDATE products p
JOIN product_categories c ON c.id = p.category_id
LEFT JOIN (
    SELECT product_id, MAX(quantity_on_hand) AS counted_quantity
    FROM inventory_balances
    WHERE business_id = @mdl_business_id
    GROUP BY product_id
) stock ON stock.product_id = p.id
SET p.reorder_level = LEAST(
        ROUND(0.30 * CASE
            WHEN COALESCE(stock.counted_quantity, 0) > 0 THEN stock.counted_quantity
            -- Median counted quantity of in-stock items per category.
            WHEN c.code IN ('SWITCHES_AND_SOCKETS') THEN 12
            WHEN c.code IN ('LIGHTING_AND_LAMPS', 'CONDUIT_AND_TRUNKING',
                            'TOOLS_AND_ACCESSORIES') THEN 10
            WHEN c.code = 'FANS_AND_VENTILATION' THEN 4
            ELSE 2
        END),
        CASE
            WHEN GREATEST(p.selling_price, p.cost_price) >= 2000 THEN 1
            WHEN GREATEST(p.selling_price, p.cost_price) >= 500 THEN 2
            WHEN GREATEST(p.selling_price, p.cost_price) >= 100 THEN 4
            WHEN GREATEST(p.selling_price, p.cost_price) >= 20 THEN 12
            WHEN GREATEST(p.selling_price, p.cost_price) > 0 THEN 40
            -- Price still unknown from the stocktake; stay conservative.
            ELSE 8
        END)
WHERE p.business_id = @mdl_business_id;
