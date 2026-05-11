-- =====================================================================================
-- Allow employees to re-request a shift after a previous rejection.
--
-- The original V001 constraint forbade ANY duplicate (shift_assignment_id, employee_id)
-- pair across history — so a rejection was permanently final. We now want only one
-- *active* (PENDING) request per (assignment, employee) at a time. Historical rows in
-- REJECTED/APPROVED/FILLED state are allowed to coexist with a new PENDING row.
-- =====================================================================================

ALTER TABLE shift_request
  DROP CONSTRAINT uq_shiftreq_employee_assignment;

-- Partial unique index: only enforced for currently-pending requests.
CREATE UNIQUE INDEX uq_shiftreq_emp_assignment_pending
  ON shift_request (shift_assignment_id, employee_id)
  WHERE status = 'PENDING';
