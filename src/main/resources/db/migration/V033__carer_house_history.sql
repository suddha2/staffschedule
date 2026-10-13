-- V033: carers' real house history (People Planner), so the solver puts a house's own
-- regulars first and continuity only carries forward placements People Planner backs.
--
-- carer_house_history holds, per carer and live service location, the People Planner shifts
-- they worked there over the last 12 months and what share of all their shifts that is
-- (share_pct). It is filled by the apply_house_history.sql script generated from the People
-- Planner import (re-run to refresh); this migration only creates it. While it is empty both
-- uses are off: no house is "tracked", so nobody counts as new and carry-overs work as before.
--
-- Familiarity with a house = share_pct scaled so houseHistoryFullSharePct (solver_tuning, 20)
-- or more is 100: it is one of the carer's own houses. Below houseHistoryMinShifts shifts it
-- is 0. A house is TRACKED when somebody has history there; houses People Planner can't see
-- are never judged.
--
-- 'Carer new to this house' (SOFT): per shift, weight x (100 - familiarity). A stranger costs
-- 100 x 5000 = 500,000 - above the hours-fairness, house-mix and weekly-pattern rules (an
-- extra 15h long day over a permanent's usual hours costs ~180,000), half an unfilled slot
-- (1,000,000) - and a carer for whom the house is a small sideline costs nearly as much, so
-- the house's own regulars get its shifts first. Tested on Barnet 12 Oct-8 Nov: 1500 left
-- strangers on Riverdale; 5000 gave 96.6% of shifts to regulars with no loss of cover,
-- 6:1 or hours fairness. Continuity only carries a carer forward
-- at a house with familiarity >= 50. Tune here, not in code.
BEGIN;

CREATE TABLE IF NOT EXISTS carer_house_history (
    employee_id  integer      NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    location     varchar(255) NOT NULL,  -- live service location (shift_templates.location, trimmed)
    shifts       integer      NOT NULL,  -- People Planner shifts worked at the house in the window
    share_pct    numeric(5,1) NOT NULL,  -- those shifts as % of all the carer's shifts in the window
    last_worked  date,
    window_start date,
    window_end   date,
    refreshed_at timestamp    NOT NULL DEFAULT now(),
    PRIMARY KEY (employee_id, location)
);

INSERT INTO constraint_setting (constraint_name, score_weight, constraint_type, enabled, description)
VALUES ('Carer new to this house', 5000, 'SOFT', true,
        'Per shift, x (100 - familiarity): carer whose People Planner history at the house is thin or none (carer_house_history) (V033)')
ON CONFLICT (constraint_name) DO UPDATE
   SET score_weight = EXCLUDED.score_weight,
       constraint_type = EXCLUDED.constraint_type,
       enabled = EXCLUDED.enabled,
       description = EXCLUDED.description
 WHERE constraint_setting.description LIKE 'Seeded%' OR constraint_setting.description LIKE '%(V033)%';

INSERT INTO solver_tuning (setting_key, int_value, description) VALUES
 ('houseHistoryMinShifts', 4,
  'People Planner shifts at a house (last 12 months) below which a carer counts as new there'),
 ('houseHistoryFullSharePct', 20,
  'Share of a carer''s People Planner shifts at a house that makes it fully one of their own houses')
ON CONFLICT (setting_key) DO UPDATE
   SET int_value = EXCLUDED.int_value
 WHERE solver_tuning.int_value = 0;  -- only replace an auto-seeded placeholder

COMMIT;
