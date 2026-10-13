-- V028: a shift template with no required_gender means "any carer".
-- The deployed gender rule only skipped Gender.ANY, so a NULL scored as a HARD mismatch
-- for every carer and the solver left those slots empty (Barnet rota 464: 148 of 176
-- unfilled - 13 EDINBURG, 3 HIGHFIELDS DAY, Alexandra Flat A/E). The template screens
-- save "no preference" as NULL, which is how these appeared. Set them to ANY; the app
-- now also normalises NULL to ANY on every save.
BEGIN;

CREATE TABLE IF NOT EXISTS v028_shift_template_gender_backup AS
  SELECT id, required_gender FROM shift_templates
   WHERE required_gender IS NULL OR trim(required_gender) = '';

UPDATE shift_templates
   SET required_gender = 'ANY'
 WHERE required_gender IS NULL OR trim(required_gender) = '';

COMMIT;
