-- V032: standing pins the solver did not apply for a rota, and why (carer has left, wrong
-- gender, missing skill, 7th consecutive day for a permanent carer, clash with another pin).
-- Written when a solved rota is saved; shown to planners on the schedule page so stale pins
-- get fixed instead of disappearing into the server log.
BEGIN;

CREATE TABLE IF NOT EXISTS rota_skipped_pin (
    id                bigserial PRIMARY KEY,
    rota_id           bigint      NOT NULL REFERENCES rota(id) ON DELETE CASCADE,
    shift_date        date,
    location          varchar(100),
    shift_type        varchar(40),
    shift_template_id bigint,
    week_of_period    smallint,
    employee_id       bigint,
    employee_name     varchar(200),
    reason            varchar(300),
    created_at        timestamp   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_rota_skipped_pin_rota ON rota_skipped_pin (rota_id);

COMMIT;
