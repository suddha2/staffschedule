-- V037: rule changes from Barnet's rota 477 review (2026-10-23). Applies to every region:
-- constraint_setting and solver_tuning are global.
--
-- 1. 'Minimum rest between shifts' OFF. It was already inert (solver_tuning minRestHours = 0);
--    the hard rest rules from V035 (only a sleep-in after a day / long day) and V036 (nothing
--    straight after a waking night) cover the real cases.
-- 2. 'Max days per location per week' OFF. It fought the 20 Glebe / 1st Avenue patterns and the
--    hard rules. 6:1 still stops a 7th day in a row for permanent carers (not zero-hours).
-- 3. 'Max locations per period' ON, HARD, max 4 houses per carer per solve (a 4-week rota).
--    Each flat is its own house: continuity is about the people supported, not the building.
--    Lead shifts don't count. Pins and carry-overs that would break it are skipped / released
--    before the solve, so it never starts infeasible; a slot nobody can take stays open.
-- 4. 'Penalize daily location switches' ON, SOFT, per carer per day the house changes from
--    the day before (lead shifts excluded). Soft first, then review the before/after figures.
--    100,000 = a tenth of an empty shift (1,000,000): it moves who works where, it never leaves
--    a shift open. Barnet 477 had 386 switches (33% of carer-days) vs 14% in People Planner.
-- Tune here, not in code.
BEGIN;

UPDATE constraint_setting
   SET enabled = false,
       description = 'Off: superseded by the hard rest rules (V035, V036); minRestHours was 0 (V037)'
 WHERE constraint_name = 'Minimum rest between shifts';

UPDATE constraint_setting
   SET enabled = false,
       description = 'Off: conflicted with the hard rules and the 20 Glebe / 1st Avenue patterns (V037)'
 WHERE constraint_name = 'Max days per location per week';

INSERT INTO constraint_setting (constraint_name, score_weight, constraint_type, enabled, description)
VALUES ('Max locations per period', 1000, 'HARD', true,
        'At most solver_tuning maxLocationsPerPeriod houses per carer per solve; each flat counts; lead shifts exempt (V037)'),
       ('Penalize daily location switches', 100000, 'SOFT', true,
        'Per carer per day working a different house from the day before; lead shifts exempt (V037)')
ON CONFLICT (constraint_name) DO UPDATE
   SET score_weight = EXCLUDED.score_weight,
       constraint_type = EXCLUDED.constraint_type,
       enabled = EXCLUDED.enabled,
       description = EXCLUDED.description
 WHERE constraint_setting.description LIKE 'Seeded%'
    OR constraint_setting.description LIKE 'Never registered%'
    OR constraint_setting.description LIKE '%(V037)%';

UPDATE solver_tuning
   SET int_value = 4,
       description = 'Ceiling on distinct houses (each flat counts) per carer per solve; HARD rule Max locations per period (V037)'
 WHERE setting_key = 'maxLocationsPerPeriod';

COMMIT;
