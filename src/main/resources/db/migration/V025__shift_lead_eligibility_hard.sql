-- V025: make lead-eligibility a HARD rule. At SOFT 200k the "Missing required skill"
-- penalty was below "Continuity - keep carer in seeded slot" (300k), so SPREAD solves
-- kept the seeded non-lead carers on lead shifts. HARD dominates all soft, so a non-lead
-- is never placed on a SHIFT_LEAD shift (left empty only if no lead is free). Feasible
-- after V024 (care requires 'Carer' which all have; lead requires 'SHIFT_LEAD' which the
-- tagged leads have).
UPDATE constraint_setting SET constraint_type = 'HARD'
 WHERE constraint_name = 'Missing required skill';
