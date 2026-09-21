-- V019: a shift type can be marked "last resort" — least favourable to assign a carer to.
-- The solver applies a soft "Last-resort assignment penalty" per assigned shift of such a
-- type, so it fills everything else first and only uses this type when nothing better is
-- available. FLOATING is the canonical case (the mobile publish-and-grab pool).
-- Tune/disable the effect via constraint_setting; flag other types in Manage Shift Types.
ALTER TABLE shift_type ADD COLUMN IF NOT EXISTS last_resort BOOLEAN NOT NULL DEFAULT false;

UPDATE shift_type SET last_resort = true WHERE code = 'FLOATING';
