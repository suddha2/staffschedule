-- V012: single-table-inheritance discriminator for ShiftAssignment.
--
-- The SLEEP_IN shadow-variable split made ShiftAssignment a single-table
-- inheritance hierarchy on rota_shift_assignment with discriminator column
-- assignment_type ('WORK' = WorkShiftAssignment, 'SLEEP_IN' = SleepInShiftAssignment).
-- The column was only ever added by hand on dev, so a fresh deploy is missing it
-- and every read of the table fails with "column assignment_type does not exist".
--
-- Backfill: existing rows on a pre-split DB are ordinary work assignments, except
-- those against a SLEEP_IN shift template, which become the SLEEP_IN subtype so
-- Hibernate instantiates them correctly.

ALTER TABLE rota_shift_assignment ADD COLUMN IF NOT EXISTS assignment_type VARCHAR(31);

UPDATE rota_shift_assignment rsa
   SET assignment_type = CASE WHEN st.shift_type = 'SLEEP_IN' THEN 'SLEEP_IN' ELSE 'WORK' END
  FROM shift s
  JOIN shift_templates st ON st.id = s.shift_template_id
 WHERE s.id = rsa.shift_id
   AND rsa.assignment_type IS NULL;

-- Any row without a resolvable shift/template defaults to the work subtype.
UPDATE rota_shift_assignment SET assignment_type = 'WORK' WHERE assignment_type IS NULL;
