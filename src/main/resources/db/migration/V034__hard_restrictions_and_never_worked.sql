-- V034: rota feedback 2026-10 (Barnet rota 469).
--
-- 1. Restricted day of week / shift type / service become HARD. They were SOFT 100,000, so a
--    carer recorded as unable to work (13 Barnet carers are restricted all 7 days) was still
--    rostered whenever that scored better (Castro Munakandafa at 29 Edgeworth, Camelia Bojan at
--    20 Glebe). Same tier as gender (HARD 100). Template pins, continuity carry-overs and lead
--    locks that break a restriction are now skipped before the solve (pins are listed in
--    rota_skipped_pin with the reason), so the hard rule can't be broken by a fixed slot.
--
-- 2. New HARD rule 'Carer never worked at this house': a carer with no People Planner shift at
--    all at a tracked house (carer_house_history, V033) is never placed there; the slot stays
--    open for the team to cover with a shadow shift first. Lead shifts and houses People Planner
--    doesn't cover (13 Edinburg, 57 Sturgess) are exempt. 'Carer new to this house' (SOFT,
--    graded) still ranks regulars above carers for whom the house is a sideline. Pins of a
--    carer onto a house they've never worked are skipped with the reason.
--
-- 3. 'Max hours per week' (10,000 per whole hour over max) duplicates 'Max weekly hours
--    exceeded' (20,000 per minute over max), which stays. Switched off.
--
-- Tune here, not in code: the code registers the new rule with a ZERO weight.
BEGIN;

UPDATE constraint_setting
   SET score_weight = 100, constraint_type = 'HARD', enabled = true,
       description = 'Carer recorded as unable to work this day / shift type / service; pins and carry-overs that break it are skipped (V034)'
 WHERE constraint_name IN ('Restricted day of week', 'Restricted Shift Type', 'Restricted Service');

INSERT INTO constraint_setting (constraint_name, score_weight, constraint_type, enabled, description)
VALUES ('Carer never worked at this house', 100, 'HARD', true,
        'No People Planner shift at all at this tracked house: leave the slot open for a shadow shift first; lead shifts exempt (V034)')
ON CONFLICT (constraint_name) DO UPDATE
   SET score_weight = EXCLUDED.score_weight,
       constraint_type = EXCLUDED.constraint_type,
       enabled = EXCLUDED.enabled,
       description = EXCLUDED.description
 WHERE constraint_setting.description LIKE 'Seeded%' OR constraint_setting.description LIKE '%(V034)%';

UPDATE constraint_setting
   SET enabled = false,
       description = 'Disabled by V034: duplicates Max weekly hours exceeded'
 WHERE constraint_name = 'Max hours per week';

COMMIT;
