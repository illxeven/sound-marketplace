-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: sound_marketplace_db
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `albums`
--

DROP TABLE IF EXISTS `albums`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `albums` (
  `id` int NOT NULL AUTO_INCREMENT,
  `artist_id` int DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `cover_art_url` varchar(255) DEFAULT NULL,
  `release_date` date DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `artist_id` (`artist_id`),
  CONSTRAINT `albums_ibfk_1` FOREIGN KEY (`artist_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `artist_tools_subscriptions`
--

DROP TABLE IF EXISTS `artist_tools_subscriptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `merchandise`
--

DROP TABLE IF EXISTS `merchandise`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchandise` (
  `id` int NOT NULL AUTO_INCREMENT,
  `merchandise_type_id` int DEFAULT NULL,
  `seller_id` int DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `price` decimal(38,2) NOT NULL,
  `file_path` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `merchandise_type_id` (`merchandise_type_id`),
  KEY `seller_id` (`seller_id`),
  CONSTRAINT `merchandise_ibfk_1` FOREIGN KEY (`merchandise_type_id`) REFERENCES `merchandise_type` (`id`),
  CONSTRAINT `merchandise_ibfk_2` FOREIGN KEY (`seller_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `merchandise_type`
--

DROP TABLE IF EXISTS `merchandise_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchandise_type` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_history`
--

DROP TABLE IF EXISTS `payment_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `playback_history`
--

DROP TABLE IF EXISTS `playback_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `playlist_tracks`
--

DROP TABLE IF EXISTS `playlist_tracks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `playlist_tracks` (
  `playlist_id` int NOT NULL,
  `track_id` int NOT NULL,
  `added_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`playlist_id`,`track_id`),
  KEY `track_id` (`track_id`),
  CONSTRAINT `playlist_tracks_ibfk_1` FOREIGN KEY (`playlist_id`) REFERENCES `playlists` (`id`) ON DELETE CASCADE,
  CONSTRAINT `playlist_tracks_ibfk_2` FOREIGN KEY (`track_id`) REFERENCES `tracks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `playlists`
--

DROP TABLE IF EXISTS `playlists`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `playlists` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `is_public` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `playlists_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `radio_stations`
--

DROP TABLE IF EXISTS `radio_stations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reposts`
--

DROP TABLE IF EXISTS `reposts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_name` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `track_comments`
--

DROP TABLE IF EXISTS `track_comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `track_comments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `track_id` int DEFAULT NULL,
  `comment_text` varchar(1000) NOT NULL,
  `parent_comment_id` int DEFAULT NULL,
  `user_display_name` varchar(255) DEFAULT NULL,
  `timestamp_played_sec` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `track_id` (`track_id`),
  KEY `parent_comment_id` (`parent_comment_id`),
  CONSTRAINT `track_comments_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `track_comments_ibfk_2` FOREIGN KEY (`track_id`) REFERENCES `tracks` (`id`) ON DELETE CASCADE,
  CONSTRAINT `track_comments_ibfk_3` FOREIGN KEY (`parent_comment_id`) REFERENCES `track_comments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `track_likes`
--

DROP TABLE IF EXISTS `track_likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tracks`
--

DROP TABLE IF EXISTS `tracks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tracks` (
  `id` int NOT NULL AUTO_INCREMENT,
  `artist_id` int DEFAULT NULL,
  `album_id` int DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `file_path` varchar(255) NOT NULL,
  `duration_seconds` int DEFAULT NULL,
  `genre` varchar(255) DEFAULT NULL,
  `is_promoted` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `price` decimal(38,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `artist_id` (`artist_id`),
  KEY `album_id` (`album_id`),
  CONSTRAINT `tracks_ibfk_1` FOREIGN KEY (`artist_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `tracks_ibfk_2` FOREIGN KEY (`album_id`) REFERENCES `albums` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_account`
--

DROP TABLE IF EXISTS `user_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_account` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_profile`
--

DROP TABLE IF EXISTS `user_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_profile` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_account_id` int DEFAULT NULL,
  `display_name` varchar(255) NOT NULL,
  `bio` text,
  `avatar_url` varchar(500) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_account_id` (`user_account_id`),
  CONSTRAINT `user_profile_ibfk_1` FOREIGN KEY (`user_account_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_relationships`
--

DROP TABLE IF EXISTS `user_relationships`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_account_id` int NOT NULL,
  `role_id` int NOT NULL,
  PRIMARY KEY (`user_account_id`,`role_id`),
  KEY `role_id` (`role_id`),
  CONSTRAINT `user_roles_ibfk_1` FOREIGN KEY (`user_account_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_roles_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-25 17:34:04
