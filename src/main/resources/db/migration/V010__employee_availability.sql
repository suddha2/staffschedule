-- V010: employee leave / unavailability.
--
-- Read by the solver as a problem fact; the "Employee unavailable (leave)" hard
-- constraint stops any assignment landing on a covered date. Intended to be
-- filled by a DAILY JOB from the People Planner Data Engine API (source='PP_API',
-- external_ref = the PP unavailability id), with manual entry as a fallback.
--
-- The unique (source, external_ref) lets the daily job upsert idempotently and
-- lets a cancelled PP leave be removed rather than duplicated.

CREATE TABLE IF NOT EXISTS employee_availability (
    id            BIGSERIAL PRIMARY KEY,
    employee_id   INTEGER      NOT NULL,
    start_date    DATE         NOT NULL,
    end_date      DATE         NOT NULL,
    type          VARCHAR(20)  NOT NULL DEFAULT 'PLANNED_LEAVE',
    source        VARCHAR(10)  NOT NULL DEFAULT 'MANUAL',
    external_ref  VARCHAR(100),
    reason        VARCHAR(255),
    synced_at     TIMESTAMP,
    CONSTRAINT employee_availability_type_check
        CHECK (type IN ('PLANNED_LEAVE','SICK','UNAVAILABLE','TRAINING')),
    CONSTRAINT employee_availability_source_check
        CHECK (source IN ('PP_API','HR_API','MANUAL')),
    CONSTRAINT employee_availability_dates_check
        CHECK (end_date >= start_date)
);

-- Idempotent upsert key for the daily sync (only meaningful where external_ref is set).
CREATE UNIQUE INDEX IF NOT EXISTS uq_avail_source_ref
    ON employee_availability (source, external_ref)
    WHERE external_ref IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_avail_emp   ON employee_availability (employee_id);
CREATE INDEX IF NOT EXISTS ix_avail_dates ON employee_availability (start_date, end_date);

COMMENT ON TABLE employee_availability IS
    'Leave / unavailability spans. Whole-day, inclusive. Populated by the daily People Planner sync (PP_API) or manual entry. A shift starting on any covered date cannot be allocated to the employee.';
