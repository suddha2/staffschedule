-- V024: 'Carer' baseline skill model + enable lead-eligibility.
-- Rationale: the "Missing required skill" constraint was off because templates required
-- specialist skills (PRADER-WILLI, etc.) that no employee holds -> infeasible. Set every
-- NON-lead shift to require the baseline skill 'Carer' (which all carers have), keep lead
-- shifts requiring SHIFT_LEAD, and ensure every active employee has 'Carer'. Then enabling
-- the constraint only bites on lead shifts (SHIFT_LEAD), so lead shifts go to leads while
-- care shifts remain fillable by anyone. Specialist skills can be re-added per shift once
-- employee skill data is populated. Backups below make it reversible.
BEGIN;

CREATE TABLE IF NOT EXISTS v024_template_reqskills_backup AS
  SELECT id, required_skills FROM shift_templates WHERE active;
CREATE TABLE IF NOT EXISTS v024_employee_skills_backup AS
  SELECT id, skills FROM employee WHERE active;

-- 1. non-lead templates require the baseline 'Carer' (replaces unsatisfiable specialist reqs)
UPDATE shift_templates SET required_skills = 'Carer'
 WHERE active AND coalesce(shift_type,'') <> 'SHIFT_LEAD';

-- 2. every active employee carries the baseline 'Carer' (leads keep SHIFT_LEAD too)
UPDATE employee
   SET skills = CASE WHEN coalesce(skills,'') = '' THEN 'Carer' ELSE skills || ',Carer' END
 WHERE active AND position('CARER' in upper(replace(coalesce(skills,''),' ',''))) = 0;

-- 3. enable the skills constraint (stays SOFT at its configured weight for now)
UPDATE constraint_setting SET enabled = true WHERE constraint_name = 'Missing required skill';

COMMIT;
