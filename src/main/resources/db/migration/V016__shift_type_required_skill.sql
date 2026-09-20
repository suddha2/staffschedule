-- V016: a shift type can declare the skill/role required to work it (eligibility tag).
-- When a template of this type is created via the builder, that skill is applied
-- automatically, so e.g. shift-lead shifts become lead-only without hand-tagging.
-- NULL = no eligibility requirement.
ALTER TABLE shift_type ADD COLUMN IF NOT EXISTS required_skill VARCHAR(40);

UPDATE shift_type SET required_skill = 'SHIFT_LEAD' WHERE code = 'SHIFT_LEAD';
