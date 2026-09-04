-- Run once after the app has started at least once (demo users exist).
-- Password stays Worker@123! (copied from the demo shop worker account).
SET @mdl_business_id = (SELECT id FROM businesses WHERE code = 'MDL');

-- Marvin — Modern Dream A (was demo John)
UPDATE users
SET email = 'marvin@mdl.local',
    username = 'marvin',
    first_name = 'Marvin',
    last_name = 'Kofi'
WHERE email = 'john@mdl.local';

DELETE ula FROM user_location_assignments ula
JOIN users u ON u.id = ula.user_id
WHERE u.email = 'marvin@mdl.local';

INSERT INTO user_location_assignments (user_id, business_id, location_id, access_level)
SELECT u.id, @mdl_business_id, l.id, 'FULL'
FROM users u
JOIN locations l ON l.business_id = @mdl_business_id AND l.code IN ('LOC-SHOP-A', 'LOC-WH-A')
WHERE u.email = 'marvin@mdl.local';

-- Stephen — Modern Dream B
INSERT INTO users (email, username, password_hash, first_name, last_name, status)
SELECT 'stephen@mdl.local', 'stephen', u.password_hash, 'Stephen', 'Mensah', 'ACTIVE'
FROM users u
WHERE u.email = 'marvin@mdl.local'
  AND NOT EXISTS (SELECT 1 FROM users x WHERE x.email = 'stephen@mdl.local');

INSERT INTO user_business_memberships (user_id, business_id, is_default, status)
SELECT u.id, @mdl_business_id, TRUE, 'ACTIVE'
FROM users u
WHERE u.email = 'stephen@mdl.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_business_memberships m
      WHERE m.user_id = u.id AND m.business_id = @mdl_business_id
  );

INSERT INTO user_roles (user_id, role_id, business_id)
SELECT u.id, r.id, @mdl_business_id
FROM users u
JOIN roles r ON r.code = 'SHOP_WORKER' AND r.business_id IS NULL
WHERE u.email = 'stephen@mdl.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.business_id = @mdl_business_id
  );

INSERT INTO user_location_assignments (user_id, business_id, location_id, access_level)
SELECT u.id, @mdl_business_id, l.id, 'FULL'
FROM users u
JOIN locations l ON l.business_id = @mdl_business_id AND l.code IN ('LOC-SHOP-B', 'LOC-WH-B')
WHERE u.email = 'stephen@mdl.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_location_assignments a
      WHERE a.user_id = u.id AND a.location_id = l.id
  );

-- Fausty — Modern Dream B
INSERT INTO users (email, username, password_hash, first_name, last_name, status)
SELECT 'fausty@mdl.local', 'fausty', u.password_hash, 'Fausty', 'Asante', 'ACTIVE'
FROM users u
WHERE u.email = 'marvin@mdl.local'
  AND NOT EXISTS (SELECT 1 FROM users x WHERE x.email = 'fausty@mdl.local');

INSERT INTO user_business_memberships (user_id, business_id, is_default, status)
SELECT u.id, @mdl_business_id, TRUE, 'ACTIVE'
FROM users u
WHERE u.email = 'fausty@mdl.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_business_memberships m
      WHERE m.user_id = u.id AND m.business_id = @mdl_business_id
  );

INSERT INTO user_roles (user_id, role_id, business_id)
SELECT u.id, r.id, @mdl_business_id
FROM users u
JOIN roles r ON r.code = 'SHOP_WORKER' AND r.business_id IS NULL
WHERE u.email = 'fausty@mdl.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.business_id = @mdl_business_id
  );

INSERT INTO user_location_assignments (user_id, business_id, location_id, access_level)
SELECT u.id, @mdl_business_id, l.id, 'FULL'
FROM users u
JOIN locations l ON l.business_id = @mdl_business_id AND l.code IN ('LOC-SHOP-B', 'LOC-WH-B')
WHERE u.email = 'fausty@mdl.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_location_assignments a
      WHERE a.user_id = u.id AND a.location_id = l.id
  );

