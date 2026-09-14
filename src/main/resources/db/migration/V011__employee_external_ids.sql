-- V011: source-system employee ids on the employee record.
--
-- People Planner and PeopleHR each use their own employee id space (neither is
-- the scheduler's employee.id, and payroll number is empty), so leave sync needs
-- a stable per-source key rather than matching on email every time. By domain:
--   zero-hours (bank) carers      -> unavailability lives in People Planner
--   contracted (permanent) staff  -> leave applied in PeopleHR
-- so most employees carry exactly one of these, but both are supported.
--
-- Populate once from the affinity email/name bridge (or the admin form); after
-- that leave sync matches by the stored id, with email as a fallback.

ALTER TABLE employee ADD COLUMN IF NOT EXISTS pp_employee_id       VARCHAR(50);
ALTER TABLE employee ADD COLUMN IF NOT EXISTS peoplehr_employee_id VARCHAR(50);

CREATE INDEX IF NOT EXISTS ix_employee_pp_id
    ON employee (pp_employee_id) WHERE pp_employee_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_employee_peoplehr_id
    ON employee (peoplehr_employee_id) WHERE peoplehr_employee_id IS NOT NULL;

COMMENT ON COLUMN employee.pp_employee_id IS
    'People Planner EmployeeID (leave sync match key for PP_API; typically zero-hours carers).';
COMMENT ON COLUMN employee.peoplehr_employee_id IS
    'PeopleHR employee id (leave sync match key for HR_API; typically contracted staff).';
