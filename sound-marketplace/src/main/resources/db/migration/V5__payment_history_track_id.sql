-- Target: MySQL 8.0.x / H2. Run through Flyway after V4.

-- 1. Track association on payment_history
ALTER TABLE payment_history
  ADD COLUMN track_id INT NULL;

ALTER TABLE payment_history
  ADD CONSTRAINT fk_payment_history_track FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE SET NULL;
