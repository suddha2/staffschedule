-- =====================================================================================
-- Role hierarchy overhaul. Standardises role names to uppercase, adds the three
-- new admin-tier roles (OPS_MANAGER, ROTA_EDITOR, READ_ONLY), and seeds
-- OPS_MANAGER onto every existing non-admin web user so they keep their working
-- access once the new method security kicks in.
--
-- Capability hierarchy: ADMIN ⊇ OPS_MANAGER ⊇ ROTA_EDITOR ⊇ READ_ONLY
--   ADMIN        — user management, publishing, everything else
--   OPS_MANAGER  — employee + shift-template CRUD, schedule edits, requests
--   ROTA_EDITOR  — schedule edits, resolve shift requests
--   READ_ONLY    — view-only across the app
--
-- Idempotent — safe to re-run.
-- =====================================================================================

-- 1. Normalise any case-variant of 'admin' to the canonical 'ADMIN'.
UPDATE roles SET name = 'ADMIN' WHERE LOWER(name) = 'admin';

-- 2. Ensure the four canonical role rows exist. id is application-managed
--    (no sequence on the table), so allocate sequentially after the current MAX.
INSERT INTO roles (id, name)
SELECT COALESCE((SELECT MAX(id) FROM roles), 0) + 1, 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');

INSERT INTO roles (id, name)
SELECT COALESCE((SELECT MAX(id) FROM roles), 0) + 1, 'OPS_MANAGER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'OPS_MANAGER');

INSERT INTO roles (id, name)
SELECT COALESCE((SELECT MAX(id) FROM roles), 0) + 1, 'ROTA_EDITOR'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROTA_EDITOR');

INSERT INTO roles (id, name)
SELECT COALESCE((SELECT MAX(id) FROM roles), 0) + 1, 'READ_ONLY'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'READ_ONLY');

-- 3. Every existing web user who does NOT already have ADMIN gets OPS_MANAGER,
--    so they retain editing rights they had before the security tightening.
--    Idempotent: skips users who already have OPS_MANAGER assigned.
INSERT INTO user_roles (userid, roleid)
SELECT u.id, (SELECT id FROM roles WHERE name = 'OPS_MANAGER')
FROM users u
WHERE u.id NOT IN (
    SELECT ur.userid FROM user_roles ur
    JOIN roles ra ON ra.id = ur.roleid
    WHERE ra.name = 'ADMIN'
)
AND u.id NOT IN (
    SELECT ur2.userid FROM user_roles ur2
    JOIN roles rb ON rb.id = ur2.roleid
    WHERE rb.name = 'OPS_MANAGER'
);
