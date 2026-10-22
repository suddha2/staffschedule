-- V036: no shift straight after a waking night (agreed 2026-10-22). After a waking night the
-- carer's only worked shift the next date may be another waking night - never a day, long day,
-- floating or lead shift straight off the night. A sleep-in (not counted as work) is fine.
--
-- Why: 'No incompatible back-to-back shifts' already listed waking night -> day / long day as
-- forbidden, but only SOFT 80,000 against 1,000,000 for an empty shift, so the solver paid it.
-- After V035 the Barnet re-solve (12 Oct-8 Nov) still had 51 nights running into a morning
-- shift (e.g. 20:00-08:00 then 08:00-20:00: 24h on duty).
--
-- HARD, same tier as the same-day rules (1000): when nobody else is free the shift is left open.
-- Real-life exceptions are added by hand on the rota, not planned. Template pins that would
-- break it are skipped (the night stands, the next-day pin is listed in rota_skipped_pin), and
-- carry-overs / seed locks are released before the solve.
-- Tune here, not in code: the code registers the rule with a ZERO weight.
BEGIN;

INSERT INTO constraint_setting (constraint_name, score_weight, constraint_type, enabled, description)
VALUES ('Shift straight after a waking night', 1000, 'HARD', true,
        'After a waking night only another waking night may be worked the next date (V036)')
ON CONFLICT (constraint_name) DO UPDATE
   SET score_weight = EXCLUDED.score_weight,
       constraint_type = EXCLUDED.constraint_type,
       enabled = EXCLUDED.enabled,
       description = EXCLUDED.description
 WHERE constraint_setting.description LIKE 'Seeded%' OR constraint_setting.description LIKE '%(V036)%';

COMMIT;
