-- V015: a shift type carries its own flat rate override (the "type tagged with rate"
-- model). NULL = use the region/level rate card (the 6 built-ins). Special types set it
-- (e.g. SHIFT_LEAD = 0 salaried, COMPLEX = 20). rate_basis on the type says how it applies.
ALTER TABLE shift_type ADD COLUMN IF NOT EXISTS rate NUMERIC(8,2);

-- The demo shift-lead is salaried (£0 per shift).
UPDATE shift_type SET rate = 0 WHERE code = 'SHIFT_LEAD';
