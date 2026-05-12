-- =====================================================================================
-- Audit log for "publish unallocated shifts" actions.
-- One row per publish attempt — including no-op publishes (zero unallocated shifts)
-- and failed FCM dispatches — so the count is "how many times the publish button was
-- pressed", not "how many times an FCM broadcast actually went out".
-- =====================================================================================

CREATE TABLE publish_log (
  id                  BIGSERIAL PRIMARY KEY,
  rota_id             BIGINT NOT NULL,
  service             VARCHAR(255) NULL,            -- NULL = "publish all services" (global button)
  published_by        VARCHAR(100) NULL,            -- auth.getName() of the admin who pressed publish
  published_at        TIMESTAMP NOT NULL DEFAULT NOW(),
  unallocated_count   INTEGER NOT NULL,
  broadcast_sent      BOOLEAN NOT NULL,             -- TRUE iff FCM accepted the message
  fcm_message_id      VARCHAR(255) NULL,            -- FCM-returned id like projects/X/messages/123, NULL if not sent
  notification_title  VARCHAR(255) NULL,
  notification_body   VARCHAR(500) NULL
);

CREATE INDEX idx_publish_log_rota_at
  ON publish_log(rota_id, published_at DESC);

CREATE INDEX idx_publish_log_rota_service_at
  ON publish_log(rota_id, service, published_at DESC);
