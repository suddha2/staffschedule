-- V040: a service can have flats under it (2026-11-02). Each flat and the service above it stay
-- ordinary locations, referred to by name as before: a shift template's location decides whether
-- it is flat level or whole-service, and a carer only works the ones listed in their services
-- (listing a service does not cover its flats, or the other way round). parent_id only links them,
-- so dropdowns can group a service with its flats.
--
-- Adds Alexandra Road and its six flats, which were missing from the master, using their People
-- Planner ids. Without them Alexandra could not be chosen in the service dropdowns.
BEGIN;

ALTER TABLE service_location ADD COLUMN IF NOT EXISTS parent_id bigint REFERENCES service_location (id);
CREATE INDEX IF NOT EXISTS idx_service_location_parent ON service_location (parent_id);

INSERT INTO service_location (external_id, region_code, region_name, staffrota_region, service_id, name, mobizio_id, active, staffrota_location)
VALUES
  ('01-0214', '01', 'Barnet', 'BARNET', '0214', 'Alexandra Road No. 8', '2-1768', true, 'Alexandra Road'),
  ('01-0215', '01', 'Barnet', 'BARNET', '0215', 'Alexandra Road No. 8 Flat A (KC)', '2-1784', true, 'Alexandra Flat A - KC'),
  ('01-0216', '01', 'Barnet', 'BARNET', '0216', 'Alexandra Road No. 8 Flat B (LC)', '2-1785', true, 'Alexandra Flat B - LC'),
  ('01-0217', '01', 'Barnet', 'BARNET', '0217', 'Alexandra Road No. 8 Flat E (FR)', '2-1786', true, 'Alexandra Flat E - RF'),
  ('01-0218', '01', 'Barnet', 'BARNET', '0218', 'Alexandra Road No. 8 Flat F (GT)', '2-1787', true, 'Alexandra Flat F - GT'),
  ('01-0219', '01', 'Barnet', 'BARNET', '0219', 'Alexandra Road No. 8 Flat G (AA)', '2-1788', true, 'Alexandra Flat G -AA'),
  ('01-0220', '01', 'Barnet', 'BARNET', '0220', 'Alexandra Road No. 8 Flat H (GFH)', '2-1789', true, 'Alexandra Flat H - GFH')
ON CONFLICT (external_id) DO UPDATE
   SET staffrota_location = EXCLUDED.staffrota_location,
       mobizio_id = EXCLUDED.mobizio_id;

UPDATE service_location f
   SET parent_id = p.id
  FROM service_location p
 WHERE p.external_id = '01-0214'
   AND f.external_id IN ('01-0215', '01-0216', '01-0217', '01-0218', '01-0219', '01-0220');

COMMIT;
