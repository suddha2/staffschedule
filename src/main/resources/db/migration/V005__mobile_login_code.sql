-- =====================================================================================
-- Email one-time login codes for the mobile app.
-- The employee enters their email, receives a 6-digit code, and verifies it to obtain
-- a PASETO session token — no password, works for any email provider.
-- The code is stored only as a BCrypt hash. Rows are short-lived (codes expire in
-- minutes) but retained for audit / rate-limiting.
-- =====================================================================================

CREATE TABLE mobile_login_code (
  id          BIGSERIAL PRIMARY KEY,
  email       VARCHAR(255) NOT NULL,
  code_hash   VARCHAR(255) NOT NULL,          -- BCrypt hash of the 6-digit code
  expires_at  TIMESTAMP NOT NULL,
  attempts    INTEGER NOT NULL DEFAULT 0,     -- failed verify attempts against this code
  consumed    BOOLEAN NOT NULL DEFAULT FALSE, -- TRUE once successfully used
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mobile_login_code_email_created
  ON mobile_login_code(email, created_at DESC);
