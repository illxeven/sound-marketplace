-- V1 Baseline schema for Sound Marketplace
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `albums` (
  `id` int NOT NULL AUTO_INCREMENT,
  `artist_id` int DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `cover_art_url` varchar(255) DEFAULT NULL,
  `release_date` date DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `cover_image_blob` longblob COMMENT 'Binary storage for album cover art',
  PRIMARY KEY (`id`),
  KEY `artist_id` (`artist_id`),
  CONSTRAINT `albums_ibfk_1` FOREIGN KEY (`artist_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `artist_tools_subscriptions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `artist_id` int DEFAULT NULL,
  `tool_name` varchar(255) NOT NULL,
  `status` varchar(255) NOT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `artist_id` (`artist_id`),
  CONSTRAINT `artist_tools_subscriptions_ibfk_1` FOREIGN KEY (`artist_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `cart` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_user` (`user_id`),
  CONSTRAINT `fk_cart_user_account` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `cart_items` (
  `id` int NOT NULL AUTO_INCREMENT,
  `cart_id` int NOT NULL,
  `merchandise_id` int DEFAULT NULL,
  `track_id` int DEFAULT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `price_snapshot` decimal(38,2) NOT NULL,
  `added_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cart_items_cart` (`cart_id`),
  KEY `idx_cart_items_merch` (`merchandise_id`),
  KEY `idx_cart_items_track` (`track_id`),
  CONSTRAINT `fk_cart_items_cart` FOREIGN KEY (`cart_id`) REFERENCES `cart` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cart_items_merchandise` FOREIGN KEY (`merchandise_id`) REFERENCES `merchandise` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cart_items_tracks` FOREIGN KEY (`track_id`) REFERENCES `tracks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `document_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'e.g., TERMS_OF_SERVICE, PRIVACY_POLICY',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Brief summary of what this document type covers',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `document_type` VALUES (1,'TERMS_OF_SERVICE','Terms of service');

CREATE TABLE `email_logs` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL COMMENT 'Optional link if the recipient is a registered user account',
  `recipient_email` varchar(255) NOT NULL COMMENT 'Email address the message was sent to',
  `sender_email` varchar(255) NOT NULL COMMENT 'Sender email address or system alias',
  `subject` varchar(255) NOT NULL COMMENT 'Subject line of the email',
  `body` longtext NOT NULL COMMENT 'Full HTML or plain text body of the email sent',
  `status` varchar(50) NOT NULL DEFAULT 'SENT' COMMENT 'Delivery status e.g., SENT, FAILED, PENDING',
  `error_message` text COMMENT 'Stores failure reason if delivery failed',
  `sent_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp when email dispatch occurred',
  PRIMARY KEY (`id`),
  KEY `idx_recipient_email` (`recipient_email`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_email_logs_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `genres` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `genres` VALUES (1,'Hip Hop & Rap (General)','2026-10-10 04:13:17'),(2,'Hip Hop & Rap Instrumental','2026-10-10 04:13:17'),(3,'Trap','2026-10-10 04:13:17'),(4,'Trap Instrumental','2026-10-10 04:13:17'),(5,'Drill','2026-10-10 04:13:17'),(6,'Drill Instrumental','2026-10-10 04:13:17'),(7,'Backpack Rap','2026-10-10 04:13:17'),(8,'Backpack Rap Instrumental','2026-10-10 04:13:17'),(9,'Cloud Rap','2026-10-10 04:13:17'),(10,'Cloud Rap Instrumental','2026-10-10 04:13:17'),(11,'Rock Rap','2026-10-10 04:13:17'),(12,'Rock Rap Instrumental','2026-10-10 04:13:17'),(13,'Reggae','2026-10-10 04:13:17'),(14,'Reggae Instrumental','2026-10-10 04:13:17'),(15,'Reggaeton','2026-10-10 04:13:17'),(16,'Reggaeton Instrumental','2026-10-10 04:13:17'),(17,'Dancehall','2026-10-10 04:13:17'),(18,'Dancehall Instrumental','2026-10-10 04:13:17'),(19,'Merengue','2026-10-10 04:13:17'),(20,'Merengue Instrumental','2026-10-10 04:13:17'),(21,'Bachata','2026-10-10 04:13:17'),(22,'Bachata Instrumental','2026-10-10 04:13:17'),(23,'K-Pop','2026-10-10 04:13:17'),(24,'K-Pop Instrumental','2026-10-10 04:13:17'),(25,'Pop','2026-10-10 04:13:17'),(26,'Pop Instrumental','2026-10-10 04:13:17'),(27,'R&B & Soul','2026-10-10 04:13:17'),(28,'R&B & Soul Instrumental','2026-10-10 04:13:17'),(29,'House Music','2026-10-10 04:13:17'),(30,'House Music Instrumental','2026-10-10 04:13:17'),(31,'Electronic (Techno, Trance, Ambient, etc.)','2026-10-10 04:13:17'),(32,'Electronic Instrumental','2026-10-10 04:13:17'),(33,'Rock','2026-10-10 04:13:17'),(34,'Rock Instrumental','2026-10-10 04:13:17'),(35,'Alternative Rock','2026-10-10 04:13:17'),(36,'Alternative Rock Instrumental','2026-10-10 04:13:17'),(37,'Dance & EDM','2026-10-10 04:13:17'),(38,'Dance & EDM Instrumental','2026-10-10 04:13:17'),(39,'Indie','2026-10-10 04:13:17'),(40,'Indie Instrumental','2026-10-10 04:13:17'),(41,'Classical (including Hindustani & Carnatic)','2026-10-10 04:13:17'),(42,'Classical Instrumental','2026-10-10 04:13:17'),(43,'Bollywood / Filmi','2026-10-10 04:13:17'),(44,'Bollywood / Filmi Instrumental','2026-10-10 04:13:17'),(45,'Indian Folk (Bhangra, Garba, etc.)','2026-10-10 04:13:17'),(46,'Indian Folk Instrumental','2026-10-10 04:13:17'),(47,'Country','2026-10-10 04:13:17'),(48,'Country Instrumental','2026-10-10 04:13:17'),(49,'Jazz & Blues','2026-10-10 04:13:17'),(50,'Jazz & Blues Instrumental','2026-10-10 04:13:17'),(51,'Folk & Singer-Songwriter','2026-10-10 04:13:17'),(52,'Folk & Singer-Songwriter Instrumental','2026-10-10 04:13:17'),(53,'Soundtrack','2026-10-10 04:13:17'),(54,'Soundtrack Instrumental','2026-10-10 04:13:17'),(55,'Spoken Word','2026-10-10 04:13:17'),(56,'Spoken Word Instrumental','2026-10-10 04:13:17'),(57,'World','2026-10-10 04:13:17'),(58,'World Instrumental','2026-10-10 04:13:17'),(59,'Afro Beats','2026-10-10 04:13:17'),(60,'Afro Beats Instrumental','2026-10-10 04:13:17'),(61,'Other','2026-10-10 04:13:17'),(62,'Other Instrumental','2026-10-10 04:13:17');

CREATE TABLE `legal_documents` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Version tag e.g., v1.0, v1.2',
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'The actual legal text or HTML markup',
  `is_active` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Flag indicating currently enforced version',
  `effective_date` timestamp NOT NULL COMMENT 'When this version went live',
  `created_by` bigint NOT NULL COMMENT 'Admin user who uploaded/modified the document',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `legal_documents_document_type` (
  `document_id` bigint NOT NULL,
  `document_type_id` bigint NOT NULL,
  PRIMARY KEY (`document_id`,`document_type_id`),
  KEY `fk_lddt_type` (`document_type_id`),
  CONSTRAINT `fk_lddt_document` FOREIGN KEY (`document_id`) REFERENCES `legal_documents` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_lddt_type` FOREIGN KEY (`document_type_id`) REFERENCES `document_type` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `merchandise` (
  `id` int NOT NULL AUTO_INCREMENT,
  `merchandise_type_id` int DEFAULT NULL,
  `seller_id` int DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `price` decimal(38,2) NOT NULL,
  `file_path` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `image_blob` longblob,
  `options` varchar(500) DEFAULT NULL,
  `category_id` bigint NOT NULL,
  `stock_quantity` int NOT NULL DEFAULT '10',
  `quantity` int NOT NULL DEFAULT '0',
  `image_blob_2` longblob,
  `image_blob_3` longblob,
  `image_blob_4` longblob,
  `image_blob_5` longblob,
  PRIMARY KEY (`id`),
  KEY `merchandise_type_id` (`merchandise_type_id`),
  KEY `seller_id` (`seller_id`),
  KEY `FKsvdmis046cfcq27q2qjua6l5p` (`category_id`),
  CONSTRAINT `FKsvdmis046cfcq27q2qjua6l5p` FOREIGN KEY (`category_id`) REFERENCES `merchandise_categories` (`id`),
  CONSTRAINT `merchandise_ibfk_1` FOREIGN KEY (`merchandise_type_id`) REFERENCES `merchandise_type` (`id`),
  CONSTRAINT `merchandise_ibfk_2` FOREIGN KEY (`seller_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `merchandise_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `group_type_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_merch_group` (`group_type_id`),
  CONSTRAINT `fk_merch_group` FOREIGN KEY (`group_type_id`) REFERENCES `merchandise_group_types` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `merchandise_categories` VALUES (1,'Short Sleeve T-Shirt',1,'2026-10-10 13:22:26'),(2,'Long Sleeve T-Shirt',1,'2026-10-10 13:22:26'),(3,'Hoodies & Sweatshirts',1,'2026-10-10 13:22:26'),(4,'Pants & Joggers',1,'2026-10-10 13:22:26'),(5,'Hats & Beanies',1,'2026-10-10 13:22:26'),(6,'Figurines',2,'2026-10-10 13:22:26'),(7,'Vinyl Toys',2,'2026-10-10 13:22:26'),(8,'Statues',2,'2026-10-10 13:22:26'),(9,'Keychains',3,'2026-10-10 13:22:26'),(10,'Stickers & Decals',3,'2026-10-10 13:22:26'),(11,'Pins & Badges',3,'2026-10-10 13:22:26'),(12,'Tote Bags',3,'2026-10-10 13:22:26'),(13,'Comic Books',4,'2026-10-10 13:22:26'),(14,'Art Books',4,'2026-10-10 13:22:26'),(15,'Magazines',4,'2026-10-10 13:22:26'),(16,'Beats & Instrumentals',5,'2026-10-10 13:22:26'),(17,'Full Tracks & Songs',5,'2026-10-10 13:22:26'),(18,'Sound Kits & Samples',5,'2026-10-10 13:22:26'),(19,'Vinyl Records & CDs',5,'2026-10-10 13:22:26');

CREATE TABLE `merchandise_group_types` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `merchandise_group_types` VALUES (1,'Apparel','Clothing and wearable merch','2026-10-10 13:22:26'),(2,'Collectibles','Figurines, toys, and physical memorabilia','2026-10-10 13:22:26'),(3,'Accessories','Everyday carry items and merch extras','2026-10-10 13:22:26'),(4,'Media & Literature','Printed comics, books, and physical media','2026-10-10 13:22:26'),(5,'Music & Audio','Digital or physical sound products, beats, and tracks','2026-10-10 13:22:26');

CREATE TABLE `merchandise_reviews` (
  `id` int NOT NULL AUTO_INCREMENT,
  `merchandise_id` int NOT NULL,
  `user_id` int NOT NULL,
  `rating` int NOT NULL,
  `comment` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_merch_reviews_merchandise` (`merchandise_id`),
  KEY `fk_merch_reviews_user` (`user_id`),
  CONSTRAINT `fk_merch_reviews_merchandise` FOREIGN KEY (`merchandise_id`) REFERENCES `merchandise` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_merch_reviews_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `merchandise_reviews_chk_1` CHECK (((`rating` >= 1) and (`rating` <= 5)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `merchandise_type` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `merchandise_type` VALUES (2,'Sample Pack');

CREATE TABLE `password_reset_tokens` (
  `id` int NOT NULL AUTO_INCREMENT,
  `expiry_date` datetime(6) NOT NULL,
  `token` varchar(100) NOT NULL,
  `used` bit(1) NOT NULL,
  `user_account_id` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK71lqwbwtklmljk3qlsugr1mig` (`token`),
  KEY `FKjm73e0r5hx3qkhmp2a1f9b4ut` (`user_account_id`),
  CONSTRAINT `FKjm73e0r5hx3qkhmp2a1f9b4ut` FOREIGN KEY (`user_account_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `payment_history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `buyer_id` int DEFAULT NULL,
  `merchandise_id` int DEFAULT NULL,
  `stripe_charge_id` varchar(255) NOT NULL,
  `amount` decimal(38,2) NOT NULL,
  `currency` varchar(255) NOT NULL,
  `status` varchar(255) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `buyer_id` (`buyer_id`),
  KEY `merchandise_id` (`merchandise_id`),
  CONSTRAINT `payment_history_ibfk_1` FOREIGN KEY (`buyer_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `payment_history_ibfk_2` FOREIGN KEY (`merchandise_id`) REFERENCES `merchandise` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `playback_history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `track_id` int DEFAULT NULL,
  `played_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `track_id` (`track_id`),
  CONSTRAINT `playback_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `playback_history_ibfk_2` FOREIGN KEY (`track_id`) REFERENCES `tracks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `playlist_tracks` (
  `playlist_id` int NOT NULL,
  `track_id` int NOT NULL,
  `added_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`playlist_id`,`track_id`),
  KEY `track_id` (`track_id`),
  CONSTRAINT `playlist_tracks_ibfk_1` FOREIGN KEY (`playlist_id`) REFERENCES `playlists` (`id`) ON DELETE CASCADE,
  CONSTRAINT `playlist_tracks_ibfk_2` FOREIGN KEY (`track_id`) REFERENCES `tracks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `playlists` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `is_public` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `playlists_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `radio_stations` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `seed_genre` varchar(255) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `radio_stations_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reposts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `track_id` int DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9p5ye4s71lbxkbdus0iqkkbel` (`user_id`,`track_id`),
  KEY `track_id` (`track_id`),
  CONSTRAINT `reposts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `reposts_ibfk_2` FOREIGN KEY (`track_id`) REFERENCES `tracks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `roles` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_name` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `roles` VALUES (6,'admin'),(1,'consumer'),(4,'music_artist'),(2,'producer'),(3,'sound_engineer'),(7,'super_admin'),(5,'supply_chain_inventory_management');

CREATE TABLE `track_comments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `track_id` int DEFAULT NULL,
  `comment_text` varchar(1000) NOT NULL,
  `timestamp_played_sec` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `parent_comment_id` int DEFAULT NULL,
  `user_display_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `track_id` (`track_id`),
  CONSTRAINT `track_comments_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `track_comments_ibfk_2` FOREIGN KEY (`track_id`) REFERENCES `tracks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `track_likes` (
  `user_id` int NOT NULL,
  `track_id` int NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`track_id`),
  UNIQUE KEY `UK8eacsa3eg2ivnhfwucrmsmk6t` (`user_id`,`track_id`),
  KEY `track_id` (`track_id`),
  CONSTRAINT `track_likes_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `track_likes_ibfk_2` FOREIGN KEY (`track_id`) REFERENCES `tracks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tracks` (
  `id` int NOT NULL AUTO_INCREMENT,
  `artist_id` int DEFAULT NULL COMMENT 'Primary artist ID reference',
  `album_id` int DEFAULT NULL COMMENT 'Null if the track is a standalone single',
  `title` varchar(255) NOT NULL,
  `file_path` varchar(255) NOT NULL,
  `duration_seconds` int DEFAULT NULL,
  `genre_id` int DEFAULT NULL,
  `is_promoted` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `price` decimal(38,2) DEFAULT NULL,
  `artist_name` varchar(200) DEFAULT NULL COMMENT 'Display name of the primary artist',
  `features` varchar(1000) DEFAULT NULL COMMENT 'Featured artists or guest credits',
  `cover_image_blob` longblob COMMENT 'Binary storage for track artwork/cover image',
  `video_file_path` varchar(500) DEFAULT NULL,
  `video_blob` longblob,
  `media_type` varchar(50) NOT NULL DEFAULT 'AUDIO',
  PRIMARY KEY (`id`),
  KEY `artist_id` (`artist_id`),
  KEY `album_id` (`album_id`),
  KEY `fk_tracks_genre_id` (`genre_id`),
  CONSTRAINT `fk_tracks_genre_id` FOREIGN KEY (`genre_id`) REFERENCES `genres` (`id`) ON DELETE SET NULL,
  CONSTRAINT `tracks_ibfk_1` FOREIGN KEY (`artist_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `tracks_ibfk_2` FOREIGN KEY (`album_id`) REFERENCES `albums` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=59 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_account` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_email_verified` bit(1) DEFAULT NULL,
  `verification_token` varchar(100) DEFAULT NULL,
  `stripe_customer_id` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=252 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_agreements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT 'Links to user_account table',
  `document_id` bigint NOT NULL COMMENT 'Links to legal_documents version accepted',
  `agreed_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp of consent',
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'User IP address at acceptance for compliance',
  PRIMARY KEY (`id`),
  KEY `fk_user_agreements_doc` (`document_id`),
  KEY `idx_user_agreements_user_id` (`user_id`),
  CONSTRAINT `fk_user_agreements_doc` FOREIGN KEY (`document_id`) REFERENCES `legal_documents` (`id`),
  CONSTRAINT `fk_user_agreements_user_account` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_profile` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_account_id` int DEFAULT NULL,
  `display_name` varchar(255) NOT NULL,
  `bio` text,
  `avatar_url` varchar(500) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `avatar_image_blob` longblob COMMENT 'Binary storage for user or artist profile picture',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_account_id` (`user_account_id`),
  CONSTRAINT `user_profile_ibfk_1` FOREIGN KEY (`user_account_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=157 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_relationships` (
  `id` int NOT NULL AUTO_INCREMENT,
  `follower_id` int DEFAULT NULL,
  `followed_id` int DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `follower_id` (`follower_id`),
  KEY `followed_id` (`followed_id`),
  CONSTRAINT `user_relationships_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_relationships_ibfk_2` FOREIGN KEY (`followed_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_reviews` (
  `id` int NOT NULL AUTO_INCREMENT,
  `reviewer_id` int NOT NULL,
  `target_user_id` int NOT NULL,
  `rating` int NOT NULL,
  `comment` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_user_reviews_reviewer` (`reviewer_id`),
  KEY `idx_user_reviews_target` (`target_user_id`),
  CONSTRAINT `fk_user_reviews_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_reviews_target` FOREIGN KEY (`target_user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_reviews_chk_1` CHECK ((`rating` between 1 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_roles` (
  `user_account_id` int NOT NULL,
  `role_id` int NOT NULL,
  PRIMARY KEY (`user_account_id`,`role_id`),
  KEY `role_id` (`role_id`),
  CONSTRAINT `user_roles_ibfk_1` FOREIGN KEY (`user_account_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_roles_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
