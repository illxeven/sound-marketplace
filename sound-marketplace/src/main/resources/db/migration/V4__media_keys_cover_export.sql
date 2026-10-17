-- Target: MySQL 8.0.x / H2. Run through Flyway after V3.

-- 1. Track cover key
ALTER TABLE tracks
  ADD COLUMN cover_key VARCHAR(500) NULL;

-- 2. Album cover key
ALTER TABLE albums
  ADD COLUMN cover_key VARCHAR(500) NULL;

-- 3. User avatar key
ALTER TABLE user_profile
  ADD COLUMN avatar_key VARCHAR(500) NULL;

-- 4. Merchandise image keys
ALTER TABLE merchandise
  ADD COLUMN image_key_1 VARCHAR(500) NULL,
  ADD COLUMN image_key_2 VARCHAR(500) NULL,
  ADD COLUMN image_key_3 VARCHAR(500) NULL,
  ADD COLUMN image_key_4 VARCHAR(500) NULL,
  ADD COLUMN image_key_5 VARCHAR(500) NULL;
