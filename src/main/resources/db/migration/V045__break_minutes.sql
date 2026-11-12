-- V045: a template's break is a length in minutes, not a start/end time.
--
-- Worked hours = (end - start) - break_minutes. That is what the solver, pay and stats
-- use, and what total_hours stores on every save from now on.
--
-- Existing break windows convert to their length (a break over midnight wraps; a
-- zero-length window such as 00:00-00:00 becomes 0). break_start / break_end are left
-- in place, unread, so the old values can still be checked; drop them in a later release.
--
-- total_hours is NOT recalculated here: some regions set it by hand and it disagrees
-- with the times. Each template's total is recalculated when it is next saved, and the
-- template screens list every active template that fails the save checks.
--
-- Save checks (statutory break and the shift type's daily limit) read these settings.

BEGIN;

ALTER TABLE shift_templates ADD COLUMN IF NOT EXISTS break_minutes integer NOT NULL DEFAULT 0;

UPDATE shift_templates
   SET break_minutes = CASE
         WHEN break_start IS NULL OR break_end IS NULL THEN 0
         ELSE ((EXTRACT(EPOCH FROM (break_end - break_start)) / 60)::int + 1440) % 1440
       END;

COMMENT ON COLUMN shift_templates.break_start IS 'Unused since V045 (break_minutes). Kept for reference.';
COMMENT ON COLUMN shift_templates.break_end IS 'Unused since V045 (break_minutes). Kept for reference.';

INSERT INTO solver_tuning (setting_key, int_value, description)
VALUES ('statutoryBreakAfterMinutes', 360,
        'A working shift longer than this many minutes (after its break) needs a statutory break; checked when a template is saved (V045)'),
       ('statutoryBreakMinutes', 20,
        'Shortest break allowed on a shift over statutoryBreakAfterMinutes; checked when a template is saved (V045)')
ON CONFLICT (setting_key) DO NOTHING;

COMMIT;
