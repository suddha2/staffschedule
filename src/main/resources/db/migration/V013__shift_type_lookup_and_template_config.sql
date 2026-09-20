-- V013: make shift types data-driven, and move per-shift behaviour onto the template.
--
-- Part of retiring the hard-coded ShiftType enum. Shift types become rows (labels +
-- behaviour DEFAULTS a template inherits); the actual behaviour for a given service
-- is set on the shift_template when it's built (pairing, rate, overrides).
--
-- This migration is ADDITIVE and behaviour-preserving: the six existing codes are
-- seeded with flags that exactly match today's hard-coded logic, and the new
-- shift_templates columns default to "inherit from the type", so nothing changes
-- until later installments start reading them.

-- 1. Shift-type lookup (labels + inheritable defaults). New types = new rows, no code.
CREATE TABLE IF NOT EXISTS shift_type (
    code                        VARCHAR(40)  PRIMARY KEY,
    display_name                VARCHAR(80)  NOT NULL,
    active                      BOOLEAN      NOT NULL DEFAULT TRUE,
    -- behaviour defaults (a template inherits these; may override per location)
    counts_toward_weekly_cap    BOOLEAN      NOT NULL DEFAULT TRUE,   -- false: LONG_DAY, FLOATING, SLEEP_IN
    counts_as_work              BOOLEAN      NOT NULL DEFAULT TRUE,   -- false: SLEEP_IN (excluded from work constraints)
    counts_as_location_coverage BOOLEAN      NOT NULL DEFAULT TRUE,   -- true: DAY, WAKING_NIGHT, LONG_DAY only
    paid_hours                  BOOLEAN      NOT NULL DEFAULT TRUE,   -- false: SLEEP_IN (not hourly)
    mineable                    BOOLEAN      NOT NULL DEFAULT TRUE,   -- false: SLEEP_IN (not a preference signal)
    default_rate_basis          VARCHAR(20)  NOT NULL DEFAULT 'HOURLY', -- HOURLY | DAILY | FLAT
    default_is_follower         BOOLEAN      NOT NULL DEFAULT FALSE,  -- true: SLEEP_IN (shadow of a leader)
    default_pairs_with          VARCHAR(40)                            -- SLEEP_IN pairs with LONG_DAY
);

-- 2. Seed the six current codes to MATCH today's hard-coded behaviour exactly.
INSERT INTO shift_type
    (code, display_name, counts_toward_weekly_cap, counts_as_work, counts_as_location_coverage,
     paid_hours, mineable, default_rate_basis, default_is_follower, default_pairs_with)
VALUES
    ('DAY',          'Day',          TRUE,  TRUE,  TRUE,  TRUE,  TRUE,  'HOURLY', FALSE, NULL),
    ('WAKING_NIGHT', 'Waking Night', TRUE,  TRUE,  TRUE,  TRUE,  TRUE,  'HOURLY', FALSE, NULL),
    ('LONG_DAY',     'Long Day',     FALSE, TRUE,  TRUE,  TRUE,  TRUE,  'DAILY',  FALSE, NULL),
    ('FLOATING',     'Floating',     FALSE, TRUE,  FALSE, TRUE,  TRUE,  'HOURLY', FALSE, NULL),
    ('CARE_CALL',    'Care Call',    TRUE,  TRUE,  FALSE, TRUE,  TRUE,  'HOURLY', FALSE, NULL),
    ('SLEEP_IN',     'Sleep In',     FALSE, FALSE, FALSE, FALSE, FALSE, 'FLAT',   TRUE,  'LONG_DAY')
ON CONFLICT (code) DO NOTHING;

-- 3. Template-level config the builder sets per service (all optional → inherit the type default).
ALTER TABLE shift_templates ADD COLUMN IF NOT EXISTS rate                     NUMERIC(8,2);   -- overrides rate card when set (e.g. complex 20, sleep-in 0)
ALTER TABLE shift_templates ADD COLUMN IF NOT EXISTS rate_basis               VARCHAR(20);    -- HOURLY | DAILY | FLAT; null = type default
ALTER TABLE shift_templates ADD COLUMN IF NOT EXISTS is_follower              BOOLEAN NOT NULL DEFAULT FALSE; -- this template mirrors its leader's carer
ALTER TABLE shift_templates ADD COLUMN IF NOT EXISTS paired_with_template_id  INTEGER;        -- the leader template this one follows
