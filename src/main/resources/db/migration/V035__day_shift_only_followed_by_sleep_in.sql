-- V035: a day or long day can only be followed by a sleep-in on the same date - no waking
-- night, floating or second day shift for that carer that day (agreed 2026-10-22).
--
-- Why: the same-day rule only checked time overlap, so Day 08:00-18:00 then a waking night
-- 20:00-08:00 passed, and the per-day hour caps count each shift type separately. The re-solve
-- of Barnet 12 Oct-8 Nov after V034 gave Clinton Mariga Tue Day + Tue waking night + his pinned
-- Wed long day (about 36h with one 2h break); 30 Day/Long Day + same-day waking night stacks.
--
-- HARD, same tier as 'No invalid same-day shift combinations' (1000): when no other carer is
-- free the shift is left open. Continuity seeds and seed locks that would break it are released
-- before the solve; template pins already refuse a day shift plus another worked shift that day.
-- Tune here, not in code: the code registers the rule with a ZERO weight.
BEGIN;

INSERT INTO constraint_setting (constraint_name, score_weight, constraint_type, enabled, description)
VALUES ('Day or long day followed by another shift', 1000, 'HARD', true,
        'A day or long day is the carer''s only worked shift that date; only its sleep-in may follow (V035)')
ON CONFLICT (constraint_name) DO UPDATE
   SET score_weight = EXCLUDED.score_weight,
       constraint_type = EXCLUDED.constraint_type,
       enabled = EXCLUDED.enabled,
       description = EXCLUDED.description
 WHERE constraint_setting.description LIKE 'Seeded%' OR constraint_setting.description LIKE '%(V035)%';

COMMIT;
