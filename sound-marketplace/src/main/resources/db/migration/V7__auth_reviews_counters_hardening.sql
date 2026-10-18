-- V7__auth_reviews_counters_hardening.sql (Flyway copy: sections 1-5 only).
-- Existing databases: run the full reviewed script by hand once (it has pre/post checks),
-- then Flyway baselines at 7 and skips this file. Fresh databases: Flyway runs it after V6.

-- -----------------------------------------------------------------------------
-- 1. user_account — email verification
-- -----------------------------------------------------------------------------
-- Your request calls the flag `is_verified BOOLEAN DEFAULT FALSE`. The column already
-- exists as `is_email_verified` (BIT(1), which is how MySQL stores a Java Boolean).
-- We keep the existing name so the entity and all queries keep working, and make it
-- NOT NULL DEFAULT 0 so "not verified" is never ambiguous.

-- 1a. NULL means "never verified"
UPDATE user_account SET is_email_verified = b'0' WHERE is_email_verified IS NULL;

ALTER TABLE user_account
  MODIFY COLUMN is_email_verified BIT(1) NOT NULL DEFAULT b'0',
  -- 1b. When the account was activated (audit + support questions)
  ADD COLUMN verified_at TIMESTAMP NULL AFTER is_email_verified,
  -- 1c. Throttle "resend verification email" (e.g. max once per 60 s per account)
  ADD COLUMN verification_sent_at TIMESTAMP NULL AFTER verification_expires_at;

-- 1d. Backfill verified_at for already-verified users (best available date)
UPDATE user_account SET verified_at = created_at
WHERE is_email_verified = b'1' AND verified_at IS NULL;

-- 1e. OPTIONAL — the old plaintext `verification_token` column. The current code stores
--     only the hash. Clearing old plaintext tokens invalidates verification links sent
--     BEFORE the hashing change; those users can press "Resend". Uncomment to run:
-- UPDATE user_account SET verification_token = NULL
-- WHERE verification_token IS NOT NULL AND is_email_verified = b'1';


-- -----------------------------------------------------------------------------
-- 2. tracks — play / like counters
-- -----------------------------------------------------------------------------
-- Columns already exist. Re-sync them to the real rows once, and stop them going negative.

UPDATE tracks t
SET t.play_count = (SELECT COUNT(*) FROM playback_history p WHERE p.track_id = t.id),
    t.like_count = (SELECT COUNT(*) FROM track_likes     l WHERE l.track_id = t.id);

ALTER TABLE tracks
  ADD CONSTRAINT chk_tracks_play_count_nonneg CHECK (play_count >= 0),
  ADD CONSTRAINT chk_tracks_like_count_nonneg CHECK (like_count >= 0);

-- Speeds up "plays for this track in the last N minutes" (replay de-duplication window)
ALTER TABLE playback_history
  ADD INDEX idx_history_track_played (track_id, played_at);


-- -----------------------------------------------------------------------------
-- 3. user_reviews — 1-5 star ratings and comments
-- -----------------------------------------------------------------------------
-- Rating CHECK (1-5) and comment TEXT already exist.

-- 3a. Track edits
ALTER TABLE user_reviews
  ADD COLUMN updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

-- 3b. One review per reviewer per artist (they edit it instead of posting again).
--     Keeps the NEWEST review when duplicates exist. Run 0's duplicate query first.
DELETE r_old FROM user_reviews r_old
JOIN user_reviews r_new
  ON r_old.reviewer_id = r_new.reviewer_id
 AND r_old.target_user_id = r_new.target_user_id
 AND r_old.id < r_new.id;

ALTER TABLE user_reviews
  ADD UNIQUE KEY uk_user_reviews_reviewer_target (reviewer_id, target_user_id),
  -- 3c. Artist profile shows newest reviews first
  ADD INDEX idx_user_reviews_target_created (target_user_id, created_at);


-- -----------------------------------------------------------------------------
-- 4. password_reset_tokens — hashed tokens with expiry tracking
-- -----------------------------------------------------------------------------
-- token_hash + expiry_date + used already exist. Problems found in the current data/code:
--   * the raw token is ALSO saved in `token` (plaintext), which defeats the hash
--   * no record of when a token was created, used, or from which IP
--   * deleting a user fails because the FK has no ON DELETE action

-- 4a. Allow `token` to be empty, then wipe dead plaintext tokens now.
--     (Live unused tokens are wiped by the code change, which stops writing `token`.)
ALTER TABLE password_reset_tokens MODIFY COLUMN token VARCHAR(100) NULL;

UPDATE password_reset_tokens SET token = NULL
WHERE used = b'1' OR expiry_date < NOW();

-- 4b. Audit columns
ALTER TABLE password_reset_tokens
  ADD COLUMN created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN used_at      TIMESTAMP   NULL,
  ADD COLUMN requested_ip VARCHAR(45) NULL,
  ADD INDEX idx_reset_user_active (user_account_id, used, expiry_date);

-- 4c. Remove a user's reset tokens when the user is deleted
ALTER TABLE password_reset_tokens DROP FOREIGN KEY FKjm73e0r5hx3qkhmp2a1f9b4ut;
ALTER TABLE password_reset_tokens
  ADD CONSTRAINT fk_reset_tokens_user
  FOREIGN KEY (user_account_id) REFERENCES user_account (id) ON DELETE CASCADE;

-- 4d. Any token without a hash is from the old scheme: burn it.
UPDATE password_reset_tokens SET used = b'1' WHERE token_hash IS NULL AND used = b'0';


-- -----------------------------------------------------------------------------
-- 5. email_logs — find delivery failures fast (verification / reset emails)
-- -----------------------------------------------------------------------------
ALTER TABLE email_logs
  ADD INDEX idx_email_logs_status_sent (status, sent_at);


