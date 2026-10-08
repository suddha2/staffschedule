-- V027: fix V024's 'Carer' baseline for Senior Carers.
-- V024 used position('CARER' in skills), so 'Senior Carer' (-> 'SENIORCARER') looked like it
-- already had the baseline and was skipped. Those staff then fail the HARD "Missing required
-- skill" on every care shift (125 hits on Barnet rota 453). Match the exact token instead.
BEGIN;

CREATE TABLE IF NOT EXISTS v027_employee_skills_backup AS
  SELECT id, skills FROM employee
   WHERE active
     AND NOT ('carer' = ANY (string_to_array(lower(replace(coalesce(skills,''),' ','')), ',')));

UPDATE employee
   SET skills = CASE WHEN coalesce(skills,'') = '' THEN 'Carer' ELSE skills || ',Carer' END
 WHERE active
   AND NOT ('carer' = ANY (string_to_array(lower(replace(coalesce(skills,''),' ','')), ',')));

COMMIT;
