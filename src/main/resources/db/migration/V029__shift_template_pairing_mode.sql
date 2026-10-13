-- V029: per-template sleep-in staffing.
-- Pairing used to be decided only by shift type: every SLEEP_IN mirrored a LONG_DAY's
-- carer. Some LA packages need the sleep-in covered by a different carer, so a template
-- can now say so. NULL = the shift type's default (unchanged behaviour for every row).
--   PAIRED     = same carer as the long day
--   STANDALONE = own carer, chosen by the solver
-- Separately, the app now gives any sleep-in with no long day left to mirror its own
-- carer automatically (e.g. 61 VICTORIA: 2 sleep-ins, 1 long day).
BEGIN;

ALTER TABLE shift_templates ADD COLUMN IF NOT EXISTS pairing_mode varchar(12);

ALTER TABLE shift_templates DROP CONSTRAINT IF EXISTS shift_templates_pairing_mode_check;
ALTER TABLE shift_templates ADD CONSTRAINT shift_templates_pairing_mode_check
  CHECK (pairing_mode IS NULL OR pairing_mode IN ('PAIRED', 'STANDALONE'));

COMMIT;
