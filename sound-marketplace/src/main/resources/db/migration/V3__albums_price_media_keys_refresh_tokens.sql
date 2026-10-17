-- Target: MySQL 8.0.x. Run through Flyway after V2.

-- 1. Album pricing
ALTER TABLE albums
  ADD COLUMN price DECIMAL(10,2) NOT NULL DEFAULT 9.99;

-- 2. Tracks media keys, status, and soft delete
ALTER TABLE tracks
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'READY',
  ADD COLUMN stream_key VARCHAR(500) NULL,
  ADD COLUMN preview_key VARCHAR(500) NULL,
  ADD COLUMN master_key VARCHAR(500) NULL,
  ADD COLUMN waveform_key VARCHAR(500) NULL,
  ADD COLUMN deleted_at TIMESTAMP NULL;

-- 3. Albums and merchandise soft delete
ALTER TABLE albums
  ADD COLUMN deleted_at TIMESTAMP NULL;

ALTER TABLE merchandise
  ADD COLUMN deleted_at TIMESTAMP NULL;

-- 4. Rotating refresh tokens table
CREATE TABLE refresh_tokens (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     INT         NOT NULL,
  token_hash  CHAR(64)    NOT NULL,
  expires_at  TIMESTAMP   NOT NULL,
  revoked_at  TIMESTAMP   NULL,
  created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  replaced_by BIGINT      NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_refresh_token_hash (token_hash),
  KEY idx_refresh_user (user_id),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. Verification token hash on user_account
ALTER TABLE user_account
  ADD COLUMN verification_token_hash CHAR(64) NULL,
  ADD COLUMN verification_expires_at TIMESTAMP NULL,
  ADD INDEX idx_user_verification_hash (verification_token_hash);
