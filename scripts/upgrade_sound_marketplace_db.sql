-- ==============================================================================
-- Sound Marketplace - Database Schema Upgrade Script
-- Target Database: MySQL 8.0+ (sound_marketplace_db)
-- Base Schema: schema_only.sql (baseline + play_count/like_count)
-- Upgrades to: Phase 8 (Flyway V1 -> V5 alignment + Hibernate validate integrity)
-- ==============================================================================

USE `sound_marketplace_db`;

-- ------------------------------------------------------------------------------
-- 1. PAYMENT HISTORY (Include track_id & performance index)
-- ------------------------------------------------------------------------------
ALTER TABLE `payment_history`
  ADD COLUMN `track_id` INT DEFAULT NULL AFTER `merchandise_id`,
  ADD KEY `idx_payment_track` (`track_id`),
  ADD CONSTRAINT `fk_payment_history_track`
      FOREIGN KEY (`track_id`) REFERENCES `tracks` (`id`) ON DELETE SET NULL;

ALTER TABLE `payment_history`
  ADD INDEX `idx_payment_buyer_created` (`buyer_id`, `created_at`);

-- ------------------------------------------------------------------------------
-- 2. TRACKS (Status, S3/MinIO media storage keys, soft delete, indexes)
-- Note: `play_count` and `like_count` already exist in schema_only.sql
-- ------------------------------------------------------------------------------
ALTER TABLE `tracks`
  ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'READY',
  ADD COLUMN `stream_key` VARCHAR(500) NULL,
  ADD COLUMN `preview_key` VARCHAR(500) NULL,
  ADD COLUMN `master_key` VARCHAR(500) NULL,
  ADD COLUMN `waveform_key` VARCHAR(500) NULL,
  ADD COLUMN `cover_key` VARCHAR(500) NULL,
  ADD COLUMN `deleted_at` TIMESTAMP NULL;

ALTER TABLE `tracks`
  ADD INDEX `idx_tracks_created` (`created_at`),
  ADD INDEX `idx_tracks_artist_created` (`artist_id`, `created_at`),
  ADD FULLTEXT INDEX `ft_tracks_search` (`title`, `artist_name`, `features`);

-- Sync denormalized counters with existing playback and like rows
UPDATE `tracks` t
SET t.play_count = (SELECT COUNT(*) FROM `playback_history` p WHERE p.track_id = t.id),
    t.like_count = (SELECT COUNT(*) FROM `track_likes`     l WHERE l.track_id = t.id);

-- ------------------------------------------------------------------------------
-- 3. ALBUMS (Price, storage key, soft delete)
-- ------------------------------------------------------------------------------
ALTER TABLE `albums`
  ADD COLUMN `price` DECIMAL(10,2) NOT NULL DEFAULT 9.99,
  ADD COLUMN `cover_key` VARCHAR(500) NULL,
  ADD COLUMN `deleted_at` TIMESTAMP NULL;

-- ------------------------------------------------------------------------------
-- 4. USER PROFILE (Avatar storage key)
-- ------------------------------------------------------------------------------
ALTER TABLE `user_profile`
  ADD COLUMN `avatar_key` VARCHAR(500) NULL;

-- ------------------------------------------------------------------------------
-- 5. MERCHANDISE (Image storage keys 1-5, soft delete)
-- ------------------------------------------------------------------------------
ALTER TABLE `merchandise`
  ADD COLUMN `deleted_at` TIMESTAMP NULL,
  ADD COLUMN `image_key_1` VARCHAR(500) NULL,
  ADD COLUMN `image_key_2` VARCHAR(500) NULL,
  ADD COLUMN `image_key_3` VARCHAR(500) NULL,
  ADD COLUMN `image_key_4` VARCHAR(500) NULL,
  ADD COLUMN `image_key_5` VARCHAR(500) NULL;

-- ------------------------------------------------------------------------------
-- 6. USER ACCOUNT (Verification token hash & expiry)
-- ------------------------------------------------------------------------------
ALTER TABLE `user_account`
  ADD COLUMN `verification_token_hash` CHAR(64) NULL,
  ADD COLUMN `verification_expires_at` TIMESTAMP NULL,
  ADD INDEX `idx_user_verification_token` (`verification_token`),
  ADD INDEX `idx_user_verification_hash` (`verification_token_hash`);

-- ------------------------------------------------------------------------------
-- 7. PASSWORD RESET TOKENS (Hash storage & unique index)
-- ------------------------------------------------------------------------------
ALTER TABLE `password_reset_tokens`
  ADD COLUMN `token_hash` CHAR(64) NULL,
  ADD UNIQUE KEY `uk_reset_token_hash` (`token_hash`);

UPDATE `password_reset_tokens` SET `used` = b'1' WHERE `token_hash` IS NULL;

-- ------------------------------------------------------------------------------
-- 8. PLAYLIST TRACKS (Explicit ordering & index)
-- ------------------------------------------------------------------------------
ALTER TABLE `playlist_tracks`
  ADD COLUMN `position` INT NOT NULL DEFAULT 0,
  ADD INDEX `idx_playlist_position` (`playlist_id`, `position`);

UPDATE `playlist_tracks` pt
JOIN (
  SELECT playlist_id, track_id,
         ROW_NUMBER() OVER (PARTITION BY playlist_id ORDER BY added_at, track_id) - 1 AS pos
  FROM `playlist_tracks`
) ranked
  ON ranked.playlist_id = pt.playlist_id AND ranked.track_id = pt.track_id
SET pt.position = ranked.pos;

-- ------------------------------------------------------------------------------
-- 9. CART ITEMS (Unique constraints per item type)
-- ------------------------------------------------------------------------------
ALTER TABLE `cart_items`
  ADD UNIQUE KEY `uk_cart_merch` (`cart_id`, `merchandise_id`),
  ADD UNIQUE KEY `uk_cart_track` (`cart_id`, `track_id`);

-- ------------------------------------------------------------------------------
-- 10. USER RELATIONSHIPS (Deduplicate & add unique key)
-- ------------------------------------------------------------------------------
DELETE r1 FROM `user_relationships` r1
JOIN `user_relationships` r2
  ON r1.follower_id = r2.follower_id
 AND r1.followed_id = r2.followed_id
 AND r1.id > r2.id;

ALTER TABLE `user_relationships`
  ADD UNIQUE KEY `uk_follow` (`follower_id`, `followed_id`);

-- ------------------------------------------------------------------------------
-- 11. TRACK LIKES (Remove redundant index)
-- ------------------------------------------------------------------------------
ALTER TABLE `track_likes` DROP INDEX `UK8eacsa3eg2ivnhfwucrmsmk6t`;

-- ------------------------------------------------------------------------------
-- 12. TRACK COMMENTS (Dangling pointer cleanup & hierarchical constraint)
-- ------------------------------------------------------------------------------
UPDATE `track_comments` c
LEFT JOIN `track_comments` p ON p.id = c.parent_comment_id
SET c.parent_comment_id = NULL
WHERE c.parent_comment_id IS NOT NULL AND p.id IS NULL;

ALTER TABLE `track_comments`
  ADD INDEX `idx_comments_track_created` (`track_id`, `created_at`),
  ADD INDEX `idx_comments_parent` (`parent_comment_id`),
  ADD CONSTRAINT `fk_comments_parent`
      FOREIGN KEY (`parent_comment_id`) REFERENCES `track_comments` (`id`) ON DELETE CASCADE;

-- ------------------------------------------------------------------------------
-- 13. PLAYBACK HISTORY (Index for user play query pattern)
-- ------------------------------------------------------------------------------
ALTER TABLE `playback_history`
  ADD INDEX `idx_history_user_played` (`user_id`, `played_at`);

-- ------------------------------------------------------------------------------
-- 14. NEW TABLES (Phase 4 queue & player state, Phase 5 checkout, Phase 6 auth)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `user_queue_items` (
  `user_id`   INT         NOT NULL,
  `position`  INT         NOT NULL,
  `track_id`  INT         NOT NULL,
  `source`    VARCHAR(32) NULL COMMENT 'PLAYLIST, ALBUM, ARTIST, RADIO, MANUAL',
  `source_id` INT         NULL,
  `added_at`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `position`),
  KEY `idx_queue_track` (`track_id`),
  CONSTRAINT `fk_queue_user`  FOREIGN KEY (`user_id`)  REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_queue_track` FOREIGN KEY (`track_id`) REFERENCES `tracks` (`id`)       ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_player_state` (
  `user_id`          INT        NOT NULL,
  `current_position` INT        NULL,
  `position_ms`      INT        NOT NULL DEFAULT 0,
  `shuffle`          TINYINT(1) NOT NULL DEFAULT 0,
  `repeat_mode`      VARCHAR(8) NOT NULL DEFAULT 'OFF' COMMENT 'OFF, ALL, ONE',
  `updated_at`       TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_player_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `orders` (
  `id`                       BIGINT       NOT NULL AUTO_INCREMENT,
  `buyer_id`                 INT          NOT NULL,
  `status`                   VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, PAID, FAILED, REFUNDED, CANCELED, NEEDS_REFUND',
  `total_cents`              BIGINT       NOT NULL,
  `currency`                 CHAR(3)      NOT NULL DEFAULT 'usd',
  `stripe_payment_intent_id` VARCHAR(255) NULL,
  `created_at`               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `paid_at`                  TIMESTAMP    NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_orders_payment_intent` (`stripe_payment_intent_id`),
  KEY `idx_orders_buyer_created` (`buyer_id`, `created_at`),
  CONSTRAINT `fk_orders_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `order_items` (
  `id`               BIGINT      NOT NULL AUTO_INCREMENT,
  `order_id`         BIGINT      NOT NULL,
  `item_type`        VARCHAR(20) NOT NULL COMMENT 'TRACK, ALBUM, MERCHANDISE',
  `track_id`         INT         NULL,
  `album_id`         INT         NULL,
  `merchandise_id`   INT         NULL,
  `seller_id`        INT         NULL,
  `unit_price_cents` BIGINT      NOT NULL,
  `quantity`         INT         NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_items_order` (`order_id`),
  KEY `idx_items_seller` (`seller_id`),
  CONSTRAINT `fk_items_order`  FOREIGN KEY (`order_id`)       REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_items_track`  FOREIGN KEY (`track_id`)       REFERENCES `tracks` (`id`),
  CONSTRAINT `fk_items_album`  FOREIGN KEY (`album_id`)       REFERENCES `albums` (`id`),
  CONSTRAINT `fk_items_merch`  FOREIGN KEY (`merchandise_id`) REFERENCES `merchandise` (`id`),
  CONSTRAINT `fk_items_seller` FOREIGN KEY (`seller_id`)      REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `entitlements` (
  `user_id`       INT       NOT NULL,
  `track_id`      INT       NOT NULL,
  `order_item_id` BIGINT    NOT NULL,
  `granted_at`    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `track_id`),
  KEY `idx_entitlements_item` (`order_item_id`),
  CONSTRAINT `fk_ent_user`  FOREIGN KEY (`user_id`)       REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ent_track` FOREIGN KEY (`track_id`)      REFERENCES `tracks` (`id`),
  CONSTRAINT `fk_ent_item`  FOREIGN KEY (`order_item_id`) REFERENCES `order_items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `stripe_events` (
  `id`          VARCHAR(255) NOT NULL,
  `type`        VARCHAR(100) NOT NULL,
  `received_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `refresh_tokens` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`     INT         NOT NULL,
  `token_hash`  CHAR(64)    NOT NULL,
  `expires_at`  TIMESTAMP   NOT NULL,
  `revoked_at`  TIMESTAMP   NULL,
  `created_at`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `replaced_by` BIGINT      NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
  KEY `idx_refresh_user` (`user_id`),
  CONSTRAINT `fk_refresh_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
