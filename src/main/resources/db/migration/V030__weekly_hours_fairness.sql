-- V030: weekly hours fairness. All weights and thresholds live here (constraint_setting /
-- solver_tuning), not in code: the code registers each rule with a ZERO weight, so without
-- these rows the rules are off.
--
-- Intent (agreed 2026-10-13):
--   1. Every permanent carer gets at least their contracted weekly minimum (e.g. 37.5h).
--   2. A permanent carer's hours don't drop sharply below their usual level (People Planner
--      baseline, employee.baseline_weekly_hours): floor = 90% of usual.
--   3. Above their usual level, extra work goes to zero-hours staff, not one permanent on 80-90h.
--   4. Zero-hours staff share the remaining work evenly.
--
-- Units: the permanent rules are per MINUTE (short of the minimum / below the floor / above
-- usual); the zero-hours rule is per HOUR SQUARED of a carer's week.
--   Unassigned shift = 1,000,000 per empty slot, for scale.
--   Permanent idle week + Permanent below weekly minimum: 1000/min (37.5h short = 2.25M).
--   Permanent below usual earnings: 300/min (12h under the floor = 216k).
--   Permanent above usual hours:   200/min (a 12h shift over usual = 144k).
--   Zero-hours fair share:         100 per hour^2 (a 12h shift to someone on 40h costs 110k,
--                                  on 60h 158k: work spreads to whoever has fewest hours).
-- Rows are inserted, or replace only an auto-seeded placeholder / an earlier V030 draft -
-- never a row someone has tuned.
BEGIN;

-- Usual weekly hours from People Planner history (filled by pp_import/pipeline/p6_baseline_hours.sql).
ALTER TABLE employee ADD COLUMN IF NOT EXISTS baseline_weekly_hours numeric(5,1);

INSERT INTO constraint_setting (constraint_name, score_weight, constraint_type, enabled, description) VALUES
 ('Permanent idle week',             1000, 'SOFT', true, 'Permanent carer with no worked shifts in a week, per minute of weekly minimum (V030)'),
 ('Permanent below weekly minimum',  1000, 'SOFT', true, 'Permanent carer working under their contracted weekly minimum, per minute short (V030)'),
 ('Permanent below usual earnings',   300, 'SOFT', true, 'Permanent carer under permanentEarningsFloorPct of usual weekly hours, per minute (V030)'),
 ('Permanent above usual hours',      200, 'SOFT', true, 'Permanent carer above usual weekly hours (or minimum if no history), per minute over (V030)'),
 ('Zero-hours fair share',            100, 'SOFT', true, 'Zero-hours carer weekly hours squared: spreads remaining work evenly (V030)')
ON CONFLICT (constraint_name) DO UPDATE
   SET score_weight = EXCLUDED.score_weight,
       constraint_type = EXCLUDED.constraint_type,
       enabled = EXCLUDED.enabled,
       description = EXCLUDED.description
 WHERE constraint_setting.description LIKE 'Seeded%' OR constraint_setting.description LIKE '%(V030)%';

-- Superseded / broken balance rules:
--   Min weekly hours not met: skipped weeks with no shifts; replaced by the two rules above.
--   Penalize overloading individual employees: compared 4-week totals with 1.3 x the WEEKLY
--     minimum, so every zero-hours hour was "overload".
--   Encourage balanced hours around midpoint: compared 4-week totals with a weekly target.
UPDATE constraint_setting
   SET enabled = false,
       description = 'Disabled by V030 (hours fairness): ' || coalesce(description, '')
 WHERE constraint_name IN ('Min weekly hours not met',
                           'Penalize overloading individual employees',
                           'Encourage balanced hours around midpoint')
   AND enabled;

INSERT INTO solver_tuning (setting_key, int_value, description)
VALUES ('permanentEarningsFloorPct', 90,
        'Permanent carer''s week may not drop below this % of their usual weekly hours; 0 = rule off')
ON CONFLICT (setting_key) DO UPDATE
   SET int_value = EXCLUDED.int_value
 WHERE solver_tuning.int_value = 0;  -- only replace the auto-seeded "off" placeholder

COMMIT;
