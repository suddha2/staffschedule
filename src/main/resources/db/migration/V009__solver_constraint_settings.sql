-- V009: make solver constraints database-driven.
--
-- Two tables, because there are two different kinds of tunable:
--
--   constraint_setting  how much a constraint counts (score_weight) and whether
--                       breaking it is forbidden or merely undesirable
--                       (constraint_type). Read per solve, so a change takes
--                       effect on the next solve with no restart.
--
--   solver_tuning       numeric thresholds a constraint compares against (weekly
--                       cap, max locations per period, ...). These are read when
--                       the constraint streams are built, so a change here needs
--                       an application restart.
--
-- Both tables are self-seeding and incremental: on every start,
-- SolverConfigService inserts a row for any constraint or threshold that does
-- not have one yet, carrying the code default. Existing rows are never
-- overwritten. Constraints that have never actually run are seeded with
-- enabled = false, so adding a constraint in code needs no migration.

CREATE TABLE IF NOT EXISTS constraint_setting (
    constraint_name VARCHAR(200) PRIMARY KEY,
    score_weight    BIGINT       NOT NULL,
    constraint_type VARCHAR(10)  NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    description     VARCHAR(500),
    CONSTRAINT constraint_setting_type_check CHECK (constraint_type IN ('HARD', 'SOFT')),
    CONSTRAINT constraint_setting_weight_check CHECK (score_weight >= 0)
);

COMMENT ON TABLE constraint_setting IS
    'Per-constraint score weight and severity. constraint_type HARD means the solver may never break the rule; SOFT means it weighs the cost. enabled = false switches the rule off without losing its type or weight.';

COMMENT ON COLUMN constraint_setting.score_weight IS
    'Magnitude only. Applied as a hard or a soft penalty according to constraint_type.';

CREATE TABLE IF NOT EXISTS solver_tuning (
    setting_key VARCHAR(80) PRIMARY KEY,
    int_value   INTEGER     NOT NULL,
    description VARCHAR(500)
);

COMMENT ON TABLE solver_tuning IS
    'Numeric thresholds used inside constraints. Read at solver build time, so a change requires a restart.';

-- Reference: what the seeding will insert for the rules that changed as a result
-- of the constraint gap analysis against 209,514 historical shifts. Nothing to
-- run -- these are the code defaults the tables will be populated with.
--
--   Permanent weekly shift cap       SOFT, weight 100000  (was HARD, weight 1)
--       ~38% of real employee-weeks exceeded the cap of 6.
--   Permanent weekly shift minimum   SOFT, weight 100000  (was HARD, weight 1)
--       37.1% of real weeks fell below 5. Together with the cap, only 24.9%
--       of observed employee-weeks were legal at all, which is what made
--       periods unsolvable.
--
--   solver_tuning.maxNonFloatingShiftsPerLocationPerWeek = 10  (was 4)
--       4 forbade 34% of observed reality; 10 forbids 6%.
--   solver_tuning.maxLocationsPerPeriod = 6                    (was 3)
--       3 forbade 32% of observed reality; 6 forbids 4.5%.
--   Both belong to constraints that are currently NOT registered in
--   defineConstraints, so widening them has no effect until they are.
--
-- Useful queries once seeded:
--   SELECT constraint_name, score_weight FROM constraint_setting
--    WHERE constraint_type = 'HARD' AND enabled ORDER BY constraint_name;
--   UPDATE constraint_setting SET constraint_type = 'SOFT', score_weight = 100000
--    WHERE constraint_name = 'Gender mismatch';
