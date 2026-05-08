-- =====================================================================================
-- Unallocated Shifts Publishing — schema migration
-- Run manually against staffrota_live (no Flyway is configured yet).
-- =====================================================================================

-- 1. Add email to employee (admin sets this to match employee's Google account).
ALTER TABLE employee
  ADD COLUMN email VARCHAR(255) NULL;

ALTER TABLE employee
  ADD CONSTRAINT uq_employee_email UNIQUE (email);

-- 2. FCM device tokens per employee.
CREATE TABLE employee_device (
  id            BIGSERIAL PRIMARY KEY,
  employee_id   INTEGER NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
  fcm_token     VARCHAR(512) NOT NULL,
  platform      VARCHAR(20) NOT NULL DEFAULT 'ANDROID',
  registered_at TIMESTAMP NOT NULL DEFAULT NOW(),
  last_seen_at  TIMESTAMP,
  active        BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uq_empdevice_token UNIQUE (fcm_token)
);

CREATE INDEX idx_empdevice_employee_active
  ON employee_device(employee_id, active);

-- 3. Employee shift requests.
CREATE TABLE shift_request (
  id                  BIGSERIAL PRIMARY KEY,
  shift_assignment_id BIGINT NOT NULL REFERENCES rota_shift_assignment(id) ON DELETE CASCADE,
  employee_id         INTEGER NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
  rota_id             BIGINT NOT NULL,
  status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  requested_at        TIMESTAMP NOT NULL DEFAULT NOW(),
  resolved_at         TIMESTAMP,
  resolved_by         VARCHAR(100),
  CONSTRAINT uq_shiftreq_employee_assignment UNIQUE (shift_assignment_id, employee_id)
);

CREATE INDEX idx_shiftreq_rota_status
  ON shift_request(rota_id, status);

CREATE INDEX idx_shiftreq_assignment_status
  ON shift_request(shift_assignment_id, status);
