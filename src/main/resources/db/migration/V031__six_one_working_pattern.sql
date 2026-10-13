-- V031: compliance 6:1 - no PERMANENT carer works more than 6 consecutive days (permanent
-- live-ins included; zero-hours staff are not covered). HARD: when no compliant carer is available the slot is left empty (and shows
-- as unassigned) rather than giving someone a 7th day in a row. Penalty = one per 7-day run.
-- Counted across week boundaries; any shift start date counts as a working day (sleep-ins
-- included). Tune here, not in code: the code registers the rule with a ZERO weight.
BEGIN;

INSERT INTO constraint_setting (constraint_name, score_weight, constraint_type, enabled, description)
VALUES ('Max 6 consecutive working days', 1000, 'HARD', true,
        'Compliance 6:1 (permanent carers): no 7 days in a row, one match per 7-day run (V031)')
ON CONFLICT (constraint_name) DO UPDATE
   SET score_weight = EXCLUDED.score_weight,
       constraint_type = EXCLUDED.constraint_type,
       enabled = EXCLUDED.enabled,
       description = EXCLUDED.description
 WHERE constraint_setting.description LIKE 'Seeded%' OR constraint_setting.description LIKE '%(V031)%';

COMMIT;
