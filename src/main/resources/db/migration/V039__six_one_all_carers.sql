-- V039: compliance 6:1 now covers every carer, permanent and zero-hours (2026-11-02). The change
-- itself is in the code (RotaConstraintProvider.sixOneApplies); weight and HARD stay as V031 set
-- them. This keeps the rule's description on the rules screen true.
BEGIN;

UPDATE constraint_setting
   SET description = 'Compliance 6:1 (all carers): no 7 days in a row, one match per 7-day run (V031, V039)'
 WHERE constraint_name = 'Max 6 consecutive working days';

COMMIT;
