-- V038: where a carer works comes from their services, not People Planner history (2026-10-23).
-- People Planner history (carer_house_history) is for tuning weights and checking solutions; it
-- is no longer an input to solving. New staff get their houses by having them listed in
-- employee.preferred_service ("HOUSE:weight,..."), and the solver obeys the list.
--
-- 1. 'Not one of the carer''s services' HARD 1000: a carer only works at houses listed with a
--    weight > 0. A carer with no services listed may work anywhere in their region. Lead shifts
--    are exempt. Pins and carry-overs outside the list are skipped / not carried.
-- 2. 'Lower-weight service' SOFT 5000 per point: per shift, 100 - the carer's weight at the
--    house, so a slot goes to the carer who lists it highest (same scale as V033 had). The
--    share split across a carer's houses stays with 'Service mix target'.
-- 3. OFF: 'Carer new to this house' (V033) and 'Carer never worked at this house' (V034), the two
--    rules fed by People Planner history.
-- Tune here, not in code: the code registers the new rules with a ZERO weight.
BEGIN;

INSERT INTO constraint_setting (constraint_name, score_weight, constraint_type, enabled, description)
VALUES ('Not one of the carer''s services', 1000, 'HARD', true,
        'Carer only works at houses in their preferred_service (weight > 0); no list = anywhere in region; lead shifts exempt (V038)'),
       ('Lower-weight service', 5000, 'SOFT', true,
        'Per shift, x (100 - the carer''s preferred_service weight at the house); no list = 0 (V038)')
ON CONFLICT (constraint_name) DO UPDATE
   SET score_weight = EXCLUDED.score_weight,
       constraint_type = EXCLUDED.constraint_type,
       enabled = EXCLUDED.enabled,
       description = EXCLUDED.description
 WHERE constraint_setting.description LIKE 'Seeded%'
    OR constraint_setting.description LIKE 'Never registered%'
    OR constraint_setting.description LIKE '%(V038)%';

UPDATE constraint_setting
   SET enabled = false,
       description = 'Off: People Planner history is not a solving input; replaced by Lower-weight service (V038)'
 WHERE constraint_name = 'Carer new to this house';

UPDATE constraint_setting
   SET enabled = false,
       description = 'Off: People Planner history is not a solving input; replaced by Not one of the carer''s services (V038)'
 WHERE constraint_name = 'Carer never worked at this house';

COMMIT;
