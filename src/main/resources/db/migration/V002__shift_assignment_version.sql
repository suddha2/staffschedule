-- =====================================================================================
-- Optimistic-lock version column on rota_shift_assignment.
-- Prevents two admins approving different ShiftRequests for the same assignment
-- from both winning the race.
-- =====================================================================================

ALTER TABLE rota_shift_assignment
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
