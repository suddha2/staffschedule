-- V023: standardize the shift-lead skill token to SHIFT_LEAD (underscore).
-- The shift_type default and the SHIFT_LEAD templates required "SHIFT LEAD" (with a space),
-- which never matched the employee tag "SHIFT_LEAD" (underscore) in the skills constraint
-- (case-insensitive string compare). Align both sides onto SHIFT_LEAD so lead-eligibility
-- works when the "Missing required skill" constraint is enabled. Idempotent (re-run is a no-op).
UPDATE shift_type
   SET required_skill = replace(required_skill, 'SHIFT LEAD', 'SHIFT_LEAD')
 WHERE required_skill LIKE '%SHIFT LEAD%';

UPDATE shift_templates
   SET required_skills = replace(required_skills, 'SHIFT LEAD', 'SHIFT_LEAD')
 WHERE required_skills LIKE '%SHIFT LEAD%';
