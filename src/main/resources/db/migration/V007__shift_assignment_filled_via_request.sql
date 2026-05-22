-- =====================================================================================
-- "Filled via request" flag on rota_shift_assignment.
-- Set when an assignment's employee is filled by approving a mobile shift request
-- (vs solver-generated or manually assigned). Cleared on any manual edit via /api/save.
-- Drives a distinct highlight on the ViewSchedule grid.
-- =====================================================================================

ALTER TABLE rota_shift_assignment
  ADD COLUMN filled_via_request BOOLEAN NOT NULL DEFAULT FALSE;
