-- V043: minimum 11 hours' rest between a carer's shifts (Barnet, 2026-11-11). Applies to every
-- region: constraint_setting and solver_tuning are global.
--
-- Why: V037 switched 'Minimum rest between shifts' off as covered by V035/V036, but those only
-- stop same-date stacks and shifts straight off a waking night. Barnet's latest rota still had:
--   - 9 Claydon days (08:00-22:00) back to back: 10 hours' rest (12 cases)
--   - a Claydon day into a 07:00 Riverdale long day: 9 hours (3)
--   - a sleep-in ending 07:00 then an 08:00 day at another house: 1 hour (about 16)
--
-- How it is measured (code: RotaConstraintProvider.minRestBetweenShifts):
--   - between one shift and the carer's NEXT shift only;
--   - a long day and its paired sleep-in are one block that ends when the sleep-in ends;
--   - exception: after a sleep-in, the next shift at the SAME house needs no rest (live-in
--     pattern: long day, sleep-in, long day), and a sleep-in after a shift at the same house.
--
-- HARD, same tier as the other rest rules (1000 per break): when nobody else can take the
-- shift it stays open. Template pins that would break it are skipped (listed in
-- rota_skipped_pin), carry-overs and seed locks are released before the solve.
-- To make it a preference instead, set constraint_type = 'SOFT' with a soft weight; the
-- pre-solve checks then stand down. Tune here, not in code. minRestHours needs a restart.
BEGIN;

UPDATE solver_tuning
   SET int_value = 11,
       description = 'Minimum hours'' rest between a carer''s consecutive shifts (sleep-in then same house exempt); 0 = rule off (V043)'
 WHERE setting_key = 'minRestHours';

INSERT INTO constraint_setting (constraint_name, score_weight, constraint_type, enabled, description)
VALUES ('Minimum rest between shifts', 1000, 'HARD', true,
        'At least solver_tuning minRestHours between a shift and the carer''s next; long day + sleep-in is one block; after a sleep-in the same house is exempt (V043)')
ON CONFLICT (constraint_name) DO UPDATE
   SET score_weight = EXCLUDED.score_weight,
       constraint_type = EXCLUDED.constraint_type,
       enabled = EXCLUDED.enabled,
       description = EXCLUDED.description;

COMMIT;
