-- V017: per-type caps become data (were hard-coded in ShiftTypeLimitConfig).
-- max_hours_per_day  -> "Max hours per shift type per day" constraint
-- max_per_week       -> "Weekly limit per shift type" constraint
-- NULL = no cap for that type.
ALTER TABLE shift_type ADD COLUMN IF NOT EXISTS max_hours_per_day INTEGER;
ALTER TABLE shift_type ADD COLUMN IF NOT EXISTS max_per_week      INTEGER;

-- Seed to match the previous hard-coded values exactly.
UPDATE shift_type SET max_hours_per_day = 15 WHERE code = 'LONG_DAY';
UPDATE shift_type SET max_hours_per_day = 13 WHERE code = 'DAY';
UPDATE shift_type SET max_hours_per_day = 6  WHERE code = 'FLOATING';
UPDATE shift_type SET max_hours_per_day = 12 WHERE code = 'WAKING_NIGHT';

-- DAY/WAKING_NIGHT intentionally have no per-type weekly cap (shared budget elsewhere);
-- SLEEP_IN is uncapped (pairs 1:1 with LONG_DAY).
UPDATE shift_type SET max_per_week = 7 WHERE code = 'LONG_DAY';
UPDATE shift_type SET max_per_week = 4 WHERE code = 'FLOATING';
