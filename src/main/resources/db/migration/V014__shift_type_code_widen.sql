-- V014: shift_templates.shift_type is now a free-text shift-type CODE (data-driven),
-- not the old 20-char enum name. Widen it so longer custom codes fit. Value-preserving.
ALTER TABLE shift_templates ALTER COLUMN shift_type TYPE VARCHAR(40);
