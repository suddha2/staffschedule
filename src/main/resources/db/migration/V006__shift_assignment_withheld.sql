-- =====================================================================================
-- "Withheld" flag on rota_shift_assignment.
-- Set when a published slot is removed from the mobile Available list by the
-- response-bound rules (>= 5 requests since publish, or >= 24h since publish with
-- >= 1 request). Cleared when the slot is re-published, which restarts its window.
-- =====================================================================================

ALTER TABLE rota_shift_assignment
  ADD COLUMN withheld BOOLEAN NOT NULL DEFAULT FALSE;
