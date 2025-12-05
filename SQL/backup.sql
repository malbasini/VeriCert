-- MySQL dump 10.13  Distrib 8.0.40, for macos14 (arm64)
--
-- Host: localhost    Database: VeriCert
-- ------------------------------------------------------
-- Server version	8.0.40

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
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL,
  `actor` varchar(255) NOT NULL,
  `action` varchar(255) NOT NULL,
  `entity` varchar(255) NOT NULL,
  `entity_id` varchar(255) NOT NULL,
  `ts` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `payload` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `tenant_id` (`tenant_id`),
  CONSTRAINT `fk_audit_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_log`
--

LOCK TABLES `audit_log` WRITE;
/*!40000 ALTER TABLE `audit_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `certificate`
--

DROP TABLE IF EXISTS `certificate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `certificate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `serial` varchar(64) NOT NULL,
  `owner_name` varchar(255) NOT NULL,
  `owner_email` varchar(255) NOT NULL,
  `pdf_url` varchar(255) NOT NULL,
  `sha256` varchar(64) NOT NULL,
  `status` enum('ISSUED','REVOKED') NOT NULL DEFAULT 'ISSUED',
  `issued_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `revoked_reason` varchar(255) DEFAULT NULL,
  `revoked_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `serial` (`serial`),
  KEY `status` (`status`),
  KEY `fk_certficate_tenant_idx` (`tenant_id`),
  CONSTRAINT `fk_certficate_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=87 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `certificate`
--

LOCK TABLES `certificate` WRITE;
/*!40000 ALTER TABLE `certificate` DISABLE KEYS */;
INSERT INTO `certificate` VALUES (86,14,'C8ADBA1B210246C8BF3F','Marco Albasini','malbasini@gmail.com','/files/14/C8ADBA1B210246C8BF3F.pdf','Xn9L6Q4gwRCrAtTW_VlVcGsN02Uh-Of_xJT4xsgpJ_8','ISSUED','2025-11-13 02:39:46',NULL,NULL);
/*!40000 ALTER TABLE `certificate` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `membership`
--

DROP TABLE IF EXISTS `membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `membership` (
  `user_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `role` enum('ADMIN','MANAGER','ISSUER','VIEWER') NOT NULL DEFAULT 'VIEWER',
  `status` enum('ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`user_id`,`tenant_id`),
  UNIQUE KEY `uq_member` (`tenant_id`,`user_id`),
  KEY `fk_membership_tenant` (`tenant_id`),
  CONSTRAINT `fk_membership_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`),
  CONSTRAINT `fk_membership_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_role` CHECK ((`role` in (_utf8mb4'ADMIN',_utf8mb4'MANAGER',_utf8mb4'ISSUER',_utf8mb4'VIEWER'))),
  CONSTRAINT `chk_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'SUSPENDED',_utf8mb4'REVOKED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `membership`
--

LOCK TABLES `membership` WRITE;
/*!40000 ALTER TABLE `membership` DISABLE KEYS */;
INSERT INTO `membership` VALUES (14,14,'ADMIN','ACTIVE'),(15,15,'ADMIN','ACTIVE'),(24,14,'VIEWER','ACTIVE');
/*!40000 ALTER TABLE `membership` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `certificate_id` bigint DEFAULT NULL,
  `provider` varchar(16) NOT NULL,
  `checkout_session_id` varchar(255) DEFAULT NULL,
  `provider_intent_id` varchar(64) DEFAULT NULL,
  `status` varchar(16) NOT NULL,
  `amount_minor` bigint NOT NULL,
  `currency` varchar(8) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `idempotency_key` varchar(64) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `checkout_session_id` (`checkout_session_id`),
  UNIQUE KEY `idempotency_key` (`idempotency_key`),
  KEY `idx_payments_session` (`checkout_session_id`),
  KEY `idx_payments_intent` (`provider_intent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=95 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
INSERT INTO `payments` VALUES (1,14,NULL,'STRIPE','cs_test_a1c1QDX9fUpmSuTnmgrSLlUQhcKMnltncEc114xNGSoTTEsKehb35WJJXG',NULL,'PENDING',9990,'EUR','Piano ENTERPRISE (Mensile)','48bbcfa4-068d-468d-a766-0cf05d36a8f4','2025-11-03 03:04:46','2025-11-03 03:04:46'),(2,14,NULL,'STRIPE','cs_test_a1HrP3K18lMfWoss46pHpeH43W8ZxKgK8BHbVIXDq0zIAJkZGUyfWvbwWx','pi_3SPF1uIX50JfMIoY18SMzFtk','SUCCEEDED',9990,'EUR','Piano ENTERPRISE (Mensile)','feb85c13-58c3-46d1-9b01-0e44bdd49b95','2025-11-03 03:05:58','2025-11-03 03:06:11'),(3,14,NULL,'PAYPAL',NULL,'45150102BA130651G','SUCCEEDED',9990,'EUR','Piano ENTERPRISE (Mensile)',NULL,'2025-11-03 03:17:55','2025-11-03 03:18:20'),(4,14,NULL,'STRIPE','cs_test_a1R3BS1XZiUpiw4hoTZx53T8vjjpaGXxNDiPTeHezDWTyOUmGRPqxl6WCO',NULL,'SUCCEEDED',0,'EUR','Piano FREE (Mensile)','5776212a-e7a1-4b62-a7ee-9e76bb1e3073','2025-11-13 22:42:36','2025-11-13 22:43:37'),(5,14,NULL,'STRIPE','cs_test_a1cvHiww6MCSHgOvwEuRgXzOHRmv0WExvnA6ZvlkTQwNP99LPjWjT8nwEX',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','c4dc32b4-89f1-447f-84cf-8688d4f0af64','2025-11-18 18:00:29','2025-11-18 18:00:29'),(6,14,NULL,'STRIPE','cs_test_a12rV8emHKEzsIvK6lisHjDZ6sNoZaHNLXybBQoToOBVgSC6fkaFommTNu',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','efe66e90-8e6f-4f72-8067-e9fbf43b0c80','2025-11-18 18:07:54','2025-11-18 18:07:54'),(7,14,NULL,'STRIPE','cs_test_a1GSHfT2FlwkRgHo0Zf7BwcCgH7u9xomZmdgoixJwRYZB3Wrb1ZayiFat3',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','4a9904ea-e19b-4d79-85b8-5424510ef6ff','2025-11-18 18:16:29','2025-11-18 18:16:29'),(8,14,NULL,'STRIPE','cs_test_a11ougwjbe6vLZR7QzvysMpA73HsqJUoCPu2pbDKPzfOUjMM5sMzcyhDBj',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','7147db0f-fa27-418d-98f9-544fc4164c18','2025-11-18 21:41:59','2025-11-18 21:41:59'),(9,14,NULL,'STRIPE','cs_test_a1IZz0FvhVw3SXMMY1fnKUgnEClzfbLJ3aUM0SZVykIV0dYLe79YA3MeNx',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','0a4f8722-e347-4671-9f95-0dec566274f8','2025-11-18 21:45:59','2025-11-18 21:45:59'),(10,14,NULL,'STRIPE','cs_test_a1esr8GALREC2xJ9f5gz1Uso3MyOz6BYhddx6pk16z4xvwLqQiRYXkkOu1',NULL,'PENDING',990,'EUR','Piano PRO (Mensile)','569cabac-40b8-44f9-bc09-6cfb7067bc4e','2025-11-18 21:47:44','2025-11-18 21:47:44'),(11,14,NULL,'STRIPE','cs_test_a1NEM9yjwEU8EnwKm1uZRNmSkiIbEShLXK5W5nulRx9CArEOES7VvdvlhW',NULL,'PENDING',990,'EUR','Piano PRO (Mensile)','53b15b62-90f9-484f-abc0-2165d0b2fd0f','2025-11-18 21:50:49','2025-11-18 21:50:49'),(12,14,NULL,'STRIPE','cs_test_a1AZ15d30HplNJO73z2Y0CGeocm84CNimjBGCdyihZBH69Xa8jphd4wdhU',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','f020e460-9b72-4bf9-b12b-80c6b67cc9af','2025-11-18 21:51:52','2025-11-18 21:51:52'),(13,14,NULL,'STRIPE','cs_test_a11BQm8y2S45MzvIMrp5Cs1cERcCkfwLRD9wvskicZKWOXUjhfAJWnm2Lf',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','667b193a-349c-4838-8ed2-04cd0fbbed0c','2025-11-18 21:57:08','2025-11-18 21:57:08'),(14,14,NULL,'STRIPE','cs_test_a19y2hb90hRYAXlhqJIMKQpMqbIl1dttQDUempZQMMGRHIY02g4FLkezDB','pi_3SUyJ4IX50JfMIoY05NMKTyS','SUCCEEDED',9504,'EUR','Piano PRO (Annuale -20%)','b3f2920b-69ef-461c-94b2-3073c68ec328','2025-11-18 22:25:31','2025-11-18 22:27:41'),(15,14,NULL,'STRIPE','cs_test_a1YEN4OsVI5FVdCtP1V7Ed4CInzlCTvYs7WEAzEilsnY93y0aPtuU1fRcM','pi_3SUyO5IX50JfMIoY0LarbYd4','SUCCEEDED',9504,'EUR','Piano PRO (Annuale -20%)','4a5ad68f-94ff-48af-8718-1685b0dd8ec2','2025-11-18 22:32:02','2025-11-18 22:34:24'),(16,14,NULL,'STRIPE','cs_test_a1E7EUGmFSDI6QcQeOMHHEdDA7PWhdl6PrzTMrShzEnR9tJHQZ1f73IPj2','pi_3SUyVzIX50JfMIoY2tgN7fpy','SUCCEEDED',9504,'EUR','Piano PRO (Annuale -20%)','5e0269dc-f295-4c21-90e1-7f8fdab2a0d0','2025-11-18 22:40:35','2025-11-18 22:41:09'),(17,14,NULL,'STRIPE','cs_test_a1shjJsi83HfOAWVvGpw5IWeEQLcewl7glgZCnV8PlisYPcRZAMXiL6dym','pi_3SUyj0IX50JfMIoY0lXI8ag8','SUCCEEDED',990,'EUR','Piano PRO (Mensile)','ba6c44bf-8f81-456e-834a-ccc694052f50','2025-11-18 22:53:49','2025-11-18 22:54:29'),(18,14,NULL,'STRIPE','cs_test_a1ahnuv3HOBQgpzfi5KeHTKIUEcTuUEefVv8mBDN5IsYHfzHuB8Jdc9ew8',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','41781b1e-59a7-4c00-b439-991d353519c1','2025-11-19 05:30:16','2025-11-19 05:30:16'),(19,14,NULL,'STRIPE','cs_test_a1ptVlbiZOzgPv40TBMjhnbR9Mek2nwz88Nm8nVpubjo230szQif84JfZ7',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','e92dc360-8470-4943-b50b-0bebbaab4354','2025-11-19 05:32:57','2025-11-19 05:32:57'),(20,14,NULL,'STRIPE','cs_test_a1bDKrsTqJ3XefzleCBGEh5LjHLYGIr0uybXwGABw0zWi5XLsbnzwdqzA5',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','5778e0a2-f7f5-4034-b82b-ae66106bd1fe','2025-11-19 05:38:07','2025-11-19 05:38:07'),(21,14,NULL,'STRIPE','cs_test_a1iL3Q3dB1eW7c0DKPZDeeNTkuFDZrC22WeCabtVouoAnAjbsQq4CPri0c',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','a57af682-ebf9-4165-9648-b9c20de93fbb','2025-11-19 05:42:50','2025-11-19 05:42:50'),(22,14,NULL,'STRIPE','cs_test_a1VI0k1Tto8y7HGmfYU5L59z2Q1eezdCtqAPbzkTcLuguNXNgT1xSz2EDx',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','6d2def49-34de-4462-a250-837eb3ce951e','2025-11-19 05:45:52','2025-11-19 05:45:52'),(23,14,NULL,'STRIPE','cs_test_a1at3abhg1tn1IPDFHemPkluyCcaaL4WWMgVLrGSCPNGKEjmNGJdBduAF9',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','2718af19-105a-4507-81e3-b3737473c13e','2025-11-19 05:48:27','2025-11-19 05:48:27'),(24,14,NULL,'STRIPE','cs_test_a1E6tdLkd4uEcUGCFDPy1pqhhhvzVDc1N7jbVfsUKqK0BNbbcKBT91SudI',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','6c8186a9-00cf-45ac-a19f-9b777e3116cf','2025-11-19 07:11:16','2025-11-19 07:11:16'),(25,14,NULL,'STRIPE','cs_test_a1zQKl2CDDIbUJWkH9bECatyOlISuq6JXW6yRuUJZolIwl0254MLs5VNSw',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','48a87817-80f7-4412-9e11-0b8e7dab965c','2025-11-19 07:14:30','2025-11-19 07:14:30'),(26,14,NULL,'STRIPE','cs_test_a1UN4mgSAXFeYOuk3pbauWolUzMlaUhiQH3nCiiArx1PqGPCTdsNgSMWSW',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','32d95df4-28cd-4001-9ae5-5ca07c979bff','2025-11-19 07:19:39','2025-11-19 07:19:39'),(27,14,NULL,'STRIPE','cs_test_a12TUhCKdVSQxk64vJeED0jJRguvtlWKIBiYDmRAbyxLEXYRQpHW1b5euI',NULL,'PENDING',990,'EUR','Piano PRO (Mensile)','554c77ab-ad74-4443-83a7-eaf95856d1d6','2025-11-19 07:23:12','2025-11-19 07:23:12'),(28,14,NULL,'STRIPE','cs_test_a1QFVJMV6VCo6X014KilhSPyrHwIlrikk0PeVMnTn27HvLSL7HroL5lQ9d',NULL,'PENDING',990,'EUR','Piano PRO (Mensile)','c01a12aa-51c7-4320-bb8c-079e71669284','2025-11-19 07:24:21','2025-11-19 07:24:21'),(29,14,NULL,'STRIPE','cs_test_a1Q3tw0b3Xo8uu9iwEVbK7jbN4NyI18JPcd8jj3okEvUc9bQeze9CoKyTK','pi_3SV9BDIX50JfMIoY0ZboKp8i','SUCCEEDED',966,'EUR','Piano PRO (Mensile)','f9b5c368-bc2c-4eeb-b014-2a1ad5949906','2025-11-19 10:03:14','2025-11-19 10:04:12'),(30,14,NULL,'STRIPE','cs_test_a1cmgn5qnXw0o0uhLgrUEHrCUWzwOp6CrqUPcg3PB0lZ3AhlGNQKh5ZBbg',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','bd26999a-bdd9-44bd-a33a-7f973b52003b','2025-11-19 10:33:27','2025-11-19 10:33:27'),(31,14,NULL,'STRIPE','cs_test_a1i7XRL7ewsTLEc7zlVdqN1xy0CYwlsHZUP7HgqX0pnpWHhj9HkwXBEbaA',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','e152dbd7-9015-4eca-8bf7-2fe4a5e2fd0a','2025-11-19 20:04:21','2025-11-19 20:04:21'),(32,14,NULL,'STRIPE','cs_test_a1fS6DswZ8MENUUPn4Xgu25v1CLFbNPBqpmXKwdsJU8L6IEh7h28LWc1Zd','pi_3SVIhtIX50JfMIoY0F7VXe36','SUCCEEDED',9504,'EUR','Piano PRO (Annuale -20%)','670d2207-bd10-473d-bb85-5e7607f578de','2025-11-19 20:13:58','2025-11-19 20:14:35'),(33,14,NULL,'STRIPE','cs_test_a1o1ddtWBwM1U2e8iw58BmqOO5fiG4lW7MnG27Mapw3ecz6qwJ9rle7KOR','pi_3SVIu3IX50JfMIoY1twg9MfG','SUCCEEDED',9504,'EUR','Piano PRO (Annuale -20%)','c9ebc80a-6eb9-4779-848a-a93525fe8ff0','2025-11-19 20:26:43','2025-11-19 20:27:08'),(34,14,NULL,'STRIPE','cs_test_a13vROFBj1F3jQ2ZNjL7MPWHHPi2YQIOfBb06ZZhdPRzj8s3oxRJhSCoRv',NULL,'PENDING',2990,'EUR','Piano BUSINESS (Mensile)','d9e358fe-85c7-478d-ab1a-a6defa0ba031','2025-11-20 18:00:31','2025-11-20 18:00:31'),(35,14,NULL,'STRIPE','cs_test_a1RFz0r64R2hFB7F86o1hFIhJlb6IQEz82sBLR0zsfFzscBoe0WXREjJeW','pi_3SVd74IX50JfMIoY1M7xYW15','SUCCEEDED',28704,'EUR','Piano BUSINESS (Annuale -20%)','278eeec1-12af-471b-b069-137e3de53d9b','2025-11-20 18:01:23','2025-11-20 18:01:56'),(36,14,NULL,'STRIPE','cs_test_a15HRZBNU5ZwDiRd052nVSUKNZ5MqJ3KOQZeuIwwOJgudD4bE7VkPhCKkk','pi_3SVd8vIX50JfMIoY2KXvOta5','SUCCEEDED',28704,'EUR','Piano BUSINESS (Annuale -20%)','1f8f1df9-914f-45f4-a44c-97864b264958','2025-11-20 18:03:34','2025-11-20 18:03:50'),(37,14,NULL,'STRIPE','cs_test_a1ObCugtxPbaH9zueiPwXSVVjrgNggtxI6uFHIiXRWNey8JWgJi8wbHfUk',NULL,'PENDING',28704,'EUR','Piano BUSINESS (Annuale -20%)','6c4f1487-9351-4798-8155-e27cd796d625','2025-11-20 18:13:25','2025-11-20 18:13:25'),(38,14,NULL,'STRIPE','cs_test_a1dMze4Rmakf6uvg3JIUWKNygZytXLZTl2dzOufHrSCc6ALaDECwszlyv2','pi_3SVdLUIX50JfMIoY26bEW6e4','SUCCEEDED',9504,'EUR','Piano PRO (Annuale -20%)','364eb6de-4bbf-4225-8082-85ef2b31ce1a','2025-11-20 18:16:37','2025-11-20 18:16:50'),(39,14,NULL,'STRIPE','cs_test_a1IAXcamMmFpDFLIcGwQUOawzELKB5FzbuJy6Noq7oa1atktiQbR6FjFkA','pi_3SVdRaIX50JfMIoY1oQ00sRj','SUCCEEDED',9504,'EUR','Piano PRO (Annuale -20%)','e282884e-32da-43ce-8052-8bd136d8d3cd','2025-11-20 18:22:44','2025-11-20 18:23:07'),(40,14,NULL,'STRIPE','cs_test_a1T2zwEk8Pb5Aj760oCXZTgpIHjucZunSVnTduA6LCEjzw7bAC9ZI6JQGd',NULL,'PENDING',28704,'EUR','Piano BUSINESS (Annuale -20%)','1c3bb11a-cdf3-4f64-850f-d9f14b3ff1af','2025-11-20 18:27:42','2025-11-20 18:27:42'),(41,14,NULL,'STRIPE','cs_test_a1cOEVtI0SSD5B0sJ5fOfJbiK2yQmktbwdZ95wn2sEJR3UBocHB1zCnMiQ',NULL,'PENDING',28704,'EUR','Piano BUSINESS (Annuale -20%)','cd3fe080-9c89-4c56-93f7-da7dd3f18cbe','2025-11-20 18:31:20','2025-11-20 18:31:20'),(42,14,NULL,'STRIPE','cs_test_a1aqdQcIZG67GM5aAvxgjks7EeYW3q0OsA9fk2tHr0sBP38iuwNvL8W6qm','pi_3SVdcbIX50JfMIoY1tFNwn29','SUCCEEDED',28704,'EUR','Piano BUSINESS (Annuale -20%)','837cdbbc-d02a-45cf-a236-16d174456d99','2025-11-20 18:34:15','2025-11-20 18:34:30'),(43,14,NULL,'STRIPE','cs_test_a1XDKTloZNcrfMuqp9OFbsqaQI7WJ6Gc6RECNuxNlLtHqp1aHDIvUCo48K',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','b4770ba2-7b97-4cb0-82b9-5228aaea7059','2025-11-20 19:49:18','2025-11-20 19:49:18'),(44,14,NULL,'STRIPE','cs_test_a1SCw5BAijuv9ZD8yFr031cRicNMjO4yahKg32cEAOLYi17X8ylQBmT5Xo',NULL,'PENDING',9504,'EUR','Piano PRO (Annuale -20%)','858014bf-a8b5-455a-aadd-552ac36eef21','2025-11-20 19:51:43','2025-11-20 19:51:43'),(45,14,NULL,'STRIPE','cs_test_a1TrBmttKvfgwaT4ls1nCq5ByYcw5n5GN2Ui8Rq9NKEl3p9erZgWv7Y6qT','pi_3SVf3pIX50JfMIoY1sUVYfZP','SUCCEEDED',9504,'EUR','Piano PRO (Annuale -20%)','0a8cff26-0a66-4e52-a433-fc3c272e1812','2025-11-20 20:06:29','2025-11-20 20:06:42'),(46,14,NULL,'STRIPE','cs_test_a1idkDWmIxOh3eLc6JVQE1zBvfATRZSFQVlNWWDQCMgHnSJqRlPqj7wAFR','pi_3SVfOWIX50JfMIoY1zACkGKc','SUCCEEDED',95904,'EUR','Piano ENTERPRISE (Annuale -20%)','f34a862c-585d-440d-a861-5083bd13702b','2025-11-20 20:27:48','2025-11-20 20:28:05'),(47,14,NULL,'PAYPAL',NULL,'8A52075921545545P','PENDING',9750,'EUR','Piano ENTERPRISE (Annuale -20%)',NULL,'2025-11-20 21:17:47','2025-11-20 21:17:48'),(48,14,NULL,'PAYPAL',NULL,'5DJ0641717810514J','PENDING',1208,'EUR','Piano PRO (Mensile)',NULL,'2025-11-20 23:12:15','2025-11-20 23:12:15'),(49,14,NULL,'PAYPAL',NULL,'7MW869042S7233135','PENDING',1208,'EUR','Piano PRO (Mensile)',NULL,'2025-11-20 23:14:21','2025-11-20 23:14:21'),(50,14,NULL,'PAYPAL',NULL,'4YN69399PG7313131','PENDING',966,'EUR','Piano PRO (Annuale -20%)',NULL,'2025-11-20 23:19:37','2025-11-20 23:19:37'),(51,14,NULL,'PAYPAL',NULL,'04V04940DP058894K','PENDING',1208,'EUR','Piano PRO (Mensile)',NULL,'2025-11-20 23:24:49','2025-11-20 23:24:49'),(52,14,NULL,'PAYPAL',NULL,'1SB15279MC299704F','PENDING',966,'EUR','Piano PRO (Annuale -20%)',NULL,'2025-11-20 23:51:03','2025-11-20 23:51:03'),(53,14,NULL,'PAYPAL',NULL,'3FY95289HK052333W','PENDING',1208,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 00:03:42','2025-11-21 00:03:42'),(54,14,NULL,'PAYPAL',NULL,'7YG764473L220721J','PENDING',1208,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 00:06:32','2025-11-21 00:06:32'),(55,14,NULL,'PAYPAL',NULL,'3MD16049NC4389042','PENDING',1208,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 00:23:26','2025-11-21 00:23:26'),(56,14,NULL,'PAYPAL',NULL,'7RF36280EG6455457','SUCCEEDED',1208,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 00:40:24','2025-11-21 00:42:10'),(57,14,NULL,'PAYPAL',NULL,'8KE991707S5060433','PENDING',966,'EUR','Piano PRO (Annuale -20%)',NULL,'2025-11-21 21:07:05','2025-11-21 21:07:05'),(58,14,NULL,'PAYPAL',NULL,'2J065215U2727504C','SUCCEEDED',1473,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 21:45:08','2025-11-21 21:46:14'),(59,14,NULL,'PAYPAL',NULL,'6D191769SF219570F','SUCCEEDED',1473,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 21:54:49','2025-11-21 21:55:10'),(60,14,NULL,'PAYPAL',NULL,'3P0573621G9419144','PENDING',1473,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 22:06:22','2025-11-21 22:06:22'),(62,14,NULL,'PAYPAL',NULL,'6A795569486557013','SUCCEEDED',1207,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 22:52:33','2025-11-21 22:53:04'),(63,14,NULL,'PAYPAL',NULL,'0XP51779XA605010E','SUCCEEDED',1200,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 23:07:44','2025-11-21 23:08:02'),(64,14,NULL,'PAYPAL',NULL,'2VR15993RG553073R','PENDING',1200,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 23:12:50','2025-11-21 23:12:50'),(65,14,NULL,'PAYPAL',NULL,'12B47145P7671215M','PENDING',1200,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 23:14:23','2025-11-21 23:14:23'),(66,14,NULL,'PAYPAL',NULL,'15998831609348509','SUCCEEDED',1207,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 23:15:08','2025-11-21 23:15:17'),(67,14,NULL,'PAYPAL',NULL,'5M122983UF617802M','SUCCEEDED',1207,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 23:30:42','2025-11-21 23:31:05'),(68,14,NULL,'PAYPAL',NULL,'51678035JB315624G','PENDING',990,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 23:31:18','2025-11-21 23:31:18'),(69,14,NULL,'PAYPAL',NULL,'07462307H4160771Y','PENDING',990,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 23:31:59','2025-11-21 23:31:59'),(70,14,NULL,'PAYPAL',NULL,'7S937130YX205230Y','PENDING',990,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 23:34:14','2025-11-21 23:34:14'),(71,14,NULL,'PAYPAL',NULL,'918010588C880111G','SUCCEEDED',1207,'EUR','Piano PRO (Mensile)',NULL,'2025-11-21 23:35:47','2025-11-21 23:36:05'),(72,14,NULL,'PAYPAL',NULL,'4HY800278K234120R','SUCCEEDED',3647,'EUR','Piano BUSINESS (Mensile)',NULL,'2025-11-21 23:46:37','2025-11-21 23:46:50'),(73,14,NULL,'PAYPAL',NULL,'25W37752XR268752W','PENDING',2335,'EUR','Piano BUSINESS (Annuale -20%)',NULL,'2025-11-21 23:47:18','2025-11-21 23:47:18'),(74,14,NULL,'PAYPAL',NULL,'13A09035AK161625R','PENDING',2848,'EUR','Piano BUSINESS (Annuale -20%)',NULL,'2025-11-21 23:49:28','2025-11-21 23:49:28'),(75,14,NULL,'PAYPAL',NULL,'89W72436G14457226','PENDING',2335,'EUR','Piano BUSINESS (Annuale -20%)',NULL,'2025-11-21 23:52:45','2025-11-21 23:52:45'),(76,14,NULL,'PAYPAL',NULL,'3VM967315E201604Y','PENDING',2335,'EUR','Piano BUSINESS (Annuale -20%)',NULL,'2025-11-21 23:57:43','2025-11-21 23:57:43'),(77,14,NULL,'PAYPAL',NULL,'1KE10312MJ637535P','SUCCEEDED',2918,'EUR','Piano BUSINESS (Annuale -20%)',NULL,'2025-11-22 00:00:12','2025-11-22 00:00:23'),(78,14,NULL,'PAYPAL',NULL,'6HA90986RC085291P','SUCCEEDED',9750,'EUR','Piano ENTERPRISE (Annuale -20%)',NULL,'2025-11-22 00:00:55','2025-11-22 00:01:03'),(79,14,NULL,'STRIPE','cs_test_a1hSwJmKZlWG0BtkW4ZBmQsRQUQ4mIlafuOAInIreP0teU89TiOYGKEC2R','pi_3SW5sWIX50JfMIoY038AcwJZ','SUCCEEDED',1207,'EUR','Piano PRO (Mensile)','a37456ff-33b3-4949-9066-b619a67b6c61','2025-11-22 00:44:33','2025-11-22 00:44:50'),(80,14,NULL,'STRIPE','cs_test_a1lka6Mz5t7rIYfxTzOJrW8lyUtJR04wGGAtDRDfGPBfPuCcNLTMptYVv5','pi_3SW5tCIX50JfMIoY2zcHbWUr','SUCCEEDED',11592,'EUR','Piano PRO (Annuale -20%)','f9be2204-5cb5-4673-8038-c648c50d5208','2025-11-22 00:45:11','2025-11-22 00:45:31'),(81,14,NULL,'STRIPE','cs_test_a1EVPYrUnoxFPhPuiEJn3qxnHOa7chvct4jynX20KOm44rimvK21CPsiQr','pi_3SW5tlIX50JfMIoY0uGFuc42','SUCCEEDED',3647,'EUR','Piano BUSINESS (Mensile)','7f382fa2-0137-4580-9402-13f5188da600','2025-11-22 00:45:50','2025-11-22 00:46:06'),(82,14,NULL,'STRIPE','cs_test_a1epZdjydN3w7BEtxh7W8u7pZMpGQVhUscsvvmtcENMB5U9sbHWxieVuNZ','pi_3SW5uFIX50JfMIoY1fsb01yV','SUCCEEDED',35016,'EUR','Piano BUSINESS (Annuale -20%)','7731cdf2-7cdb-43ef-9383-f47c6fe1b527','2025-11-22 00:46:21','2025-11-22 00:46:36'),(83,14,NULL,'STRIPE','cs_test_a1Jrrpsr6iXsrjObPOdbEH437oyPPgaZ2BgWrjdsh34Lj1LmJ1ULRG44pd','pi_3SW5vKIX50JfMIoY25dCUOuo','SUCCEEDED',12187,'EUR','Piano ENTERPRISE (Mensile)','704f64e7-1f86-4025-85f7-6bc5178aef46','2025-11-22 00:46:54','2025-11-22 00:47:43'),(84,14,NULL,'PAYPAL',NULL,'8M170803MA200163N','SUCCEEDED',1207,'EUR','Piano PRO (Mensile)',NULL,'2025-11-22 00:48:04','2025-11-22 00:48:23'),(85,14,NULL,'PAYPAL',NULL,'8AW943449T212480J','SUCCEEDED',3647,'EUR','Piano BUSINESS (Mensile)',NULL,'2025-11-22 00:48:43','2025-11-22 00:48:52'),(86,14,NULL,'PAYPAL',NULL,'2NH26248GA265363G','SUCCEEDED',12187,'EUR','Piano ENTERPRISE (Mensile)',NULL,'2025-11-22 00:49:03','2025-11-22 00:49:14'),(87,14,NULL,'PAYPAL',NULL,'4RC41405U5876783T','SUCCEEDED',11592,'EUR','Piano PRO (Annuale -20%)',NULL,'2025-11-22 00:49:24','2025-11-22 00:49:34'),(88,14,NULL,'PAYPAL',NULL,'99921763GE399372D','SUCCEEDED',35016,'EUR','Piano BUSINESS (Annuale -20%)',NULL,'2025-11-22 00:49:53','2025-11-22 00:50:03'),(89,14,NULL,'STRIPE','cs_test_a1D1WJ4FOxBQv8dIiIQmkrnWb0qZGEkGX4JEHb7NInfTT8FygsgYTApwoV','pi_3SW6GyIX50JfMIoY1wAt7jpX','SUCCEEDED',1208,'EUR','Piano PRO (Mensile)','89d1a1f3-96d7-4bdd-8a14-a41629254148','2025-11-22 01:09:53','2025-11-22 01:10:05'),(90,14,NULL,'STRIPE','cs_test_a13bvNCHeADQ49OGmYtslW9iFApnaRjquPaFXj7LuvoxfSc0Wj6fPQLgZ3','pi_3SW6HKIX50JfMIoY2qeOVzMZ','SUCCEEDED',3648,'EUR','Piano BUSINESS (Mensile)','2799a39a-eb72-4f64-b1e8-8fe2d1205581','2025-11-22 01:10:15','2025-11-22 01:10:27'),(91,14,NULL,'STRIPE','cs_test_a1rao9ZHshmHJu1CCPYx3KNReqR0T6FVdrjxE7vZwUz3DuzBScUh4cAFSU','pi_3SW6HcIX50JfMIoY03gjgGNL','SUCCEEDED',12188,'EUR','Piano ENTERPRISE (Mensile)','2c042a18-49f7-4ad5-bc3d-aa2715e407fc','2025-11-22 01:10:35','2025-11-22 01:10:45'),(92,14,NULL,'STRIPE','cs_test_a1uU0xEf6cCQe6WXdHaU3rM9wrFP85xJvUcH23ryAmq22Bn1ZmpZxnIayR','pi_3SW6InIX50JfMIoY1ZCrcKAW','SUCCEEDED',11593,'EUR','Piano PRO (Annuale -20%)','1e80c9fa-1c7f-4e8e-a317-45094f868594','2025-11-22 01:11:34','2025-11-22 01:11:58'),(93,14,NULL,'STRIPE','cs_test_a1qlDyz1GsfndECsraGHh6vqF7DV4AzJwPOIFzVVAD9PuQQuOFbYtoWbW2','pi_3SW6JZIX50JfMIoY1J4XvUr0','SUCCEEDED',35017,'EUR','Piano BUSINESS (Annuale -20%)','7fb9d183-f14f-4b65-8d5b-ad21618f2154','2025-11-22 01:12:30','2025-11-22 01:12:47'),(94,14,NULL,'STRIPE','cs_test_a1LPTS4P0BsvioFsk2E6pYlNv1yX44xxuO6yQ4tQ8TPJ4gBfFayGohwLwN','pi_3SW6MJIX50JfMIoY2ojZp9li','SUCCEEDED',117001,'EUR','Piano ENTERPRISE (Annuale -20%)','f7e950e2-307c-4f85-b989-5ee38cd3390c','2025-11-22 01:15:16','2025-11-22 01:15:37');
/*!40000 ALTER TABLE `payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `persistent_logins`
--

DROP TABLE IF EXISTS `persistent_logins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `persistent_logins` (
  `username` varchar(64) NOT NULL,
  `series` varchar(64) NOT NULL,
  `token` varchar(64) NOT NULL,
  `last_used` timestamp NOT NULL,
  PRIMARY KEY (`series`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `persistent_logins`
--

LOCK TABLES `persistent_logins` WRITE;
/*!40000 ALTER TABLE `persistent_logins` DISABLE KEYS */;
INSERT INTO `persistent_logins` VALUES ('malbasini','MtITv29C4XYDnYLofyCIgw==','5MNGp5o9k1ACBEd/NWzQhw==','2025-11-22 02:15:06');
/*!40000 ALTER TABLE `persistent_logins` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `plan_definitions`
--

DROP TABLE IF EXISTS `plan_definitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plan_definitions` (
  `id` bigint NOT NULL,
  `code` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `certs_per_month` int NOT NULL,
  `api_calls_per_month` int DEFAULT NULL,
  `storage_per_month` bigint DEFAULT NULL,
  `support_priority` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0',
  `price_monthly_cents` bigint DEFAULT NULL,
  `price_annual_cents` bigint DEFAULT NULL,
  `stripe_price_monthly_id` varchar(255) DEFAULT NULL,
  `stripe_price_annual_id` varchar(255) DEFAULT NULL,
  `paypal_plan_monthly_id` varchar(255) DEFAULT NULL,
  `paypal_plan_annual_id` varchar(255) DEFAULT NULL,
  `vat_code` varchar(255) DEFAULT NULL,
  `annual_discount` varchar(255) DEFAULT NULL,
  `update_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `plan_definitions_pk` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `plan_definitions`
--

LOCK TABLES `plan_definitions` WRITE;
/*!40000 ALTER TABLE `plan_definitions` DISABLE KEYS */;
INSERT INTO `plan_definitions` VALUES (1,'FREE','FREE',5,100,100,'Supporto community',0,0,'0','0','0','0','0','0','2025-11-15 01:31:46'),(2,'PRO','PRO',100,50000,5000,'Supporto email',990,792,'9,90','7,92','12,08','9,66','22%','20%','2025-11-15 02:10:08'),(3,'BUSINESS','BUSINESS',500,200000,25000,'Supporto prioritario',2990,2392,'29,90','23,92','36,48','26,18','22%','20%','2025-11-15 02:15:25'),(4,'ENTERPRISE','ENTERPRISE',5000,1000000,200000,'Supporto Enterprise & SLA',9990,7992,'99,90','79,92','121,88','97,50','22%','20%','2025-11-15 02:20:30');
/*!40000 ALTER TABLE `plan_definitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `signing_key`
--

DROP TABLE IF EXISTS `signing_key`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `signing_key` (
  `kid` varchar(255) NOT NULL,
  `public_key_pem` text NOT NULL,
  `status` varchar(255) NOT NULL,
  `not_before_ts` timestamp NULL DEFAULT NULL,
  `not_after_ts` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`kid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `signing_key`
--

LOCK TABLES `signing_key` WRITE;
/*!40000 ALTER TABLE `signing_key` DISABLE KEYS */;
INSERT INTO `signing_key` VALUES ('key-2025-10-30','-----BEGIN PUBLIC KEY-----\n      MCowBQYDK2VwAyEA698vPNPQmHeEggeDIBzxqAK2gJNHXBOVGrdcrWn8ZHM=\n         -----END PUBLIC KEY-----','ACTIVE','2025-10-30 15:49:11',NULL);
/*!40000 ALTER TABLE `signing_key` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `template`
--

DROP TABLE IF EXISTS `template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `name` varchar(120) NOT NULL,
  `version` varchar(20) NOT NULL,
  `html` text NOT NULL,
  `user_vars_json` json DEFAULT NULL,
  `user_vars_schema` json DEFAULT NULL,
  `sys_vars_schema` json DEFAULT NULL,
  `active` bit(1) NOT NULL DEFAULT b'0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `tenant_id` (`tenant_id`),
  KEY `idx_template_tenant_updated` (`tenant_id`,`updated_at` DESC),
  CONSTRAINT `fk_template_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `template`
--

LOCK TABLES `template` WRITE;
/*!40000 ALTER TABLE `template` DISABLE KEYS */;
INSERT INTO `template` VALUES (42,14,'Template Demo','1.1','<!DOCTYPE html>\n<html lang=\"it\" xmlns:th=\"http://www.thymeleaf.org\">\n<head>\n    <meta charset=\"UTF-8\"></meta>\n    <title th:text=\"\'Certificato \' + ${serial}\">Certificato</title>\n    <style>\n        /* --- Pagina / palette PRO --- */\n        @page { size: A4; margin: 14mm; }\n        * { box-sizing: border-box; }\n        html, body { margin:0; padding:0; color:#0f172a!important; font-family: Arial, Helvetica, sans-serif; }\n\n        /* Tavolozza PRO (blu profondo, accenti dorati) */\n        .pro {\n            --ink:#0f172a!important; --muted:#6b7280!important; --bg:#ffffff!important; --paper:#f8fafc!important;\n            --primary:#0a376e!important;            /* blu profondo */\n            --primary-900:#082a53!important;\n            --accent:#b58b00!important;             /* oro sobrio */\n            --border:#d1d5db!important;\n            --verify-bg:#f3f4f6!important;\n            --badge-bg:#0a376e!important; --badge-fg:#fff!important;\n            --chip-bg:#ffffff!important; --chip-fg:#000000!important;\n        }\n\n        /* --- Cornice principale --- */\n        .page { background: var(--paper); border: 1px solid var(--border); border-radius: 10px; }\n        .frame { margin: 8mm; padding: 10mm; background: var(--bg); border: 2px solid var(--primary); border-radius: 8px; }\n\n        /* --- Banda superiore di sicurezza --- */\n        .security-band {\n            background: linear-gradient(90deg, var(--primary) 0%, var(--primary-900) 100%);\n            color: #fff; padding: 3mm 6mm; border-radius: 6px; margin-bottom: 8mm;\n        }\n        .security-band .left { display:inline-block; width: 60%; vertical-align: middle; }\n        .security-band .right { display:inline-block; width: 39%; text-align: right; vertical-align: middle; }\n        .security-kicker { font-size: 9pt; opacity: .9; }\n        .security-title { font-size: 12pt; font-weight: 700; letter-spacing: .5px; }\n\n        /* --- Header --- */\n        .header { border-bottom: 2px solid var(--border); padding-bottom: 6mm; margin-bottom: 8mm; }\n        .brand { display: inline-block; vertical-align: top; }\n        .crest { display:inline-block; width:42px; height:42px; border-radius:8px; background: var(--primary);\n            border:2px solid #fff; outline:2px solid var(--primary); margin-right:10px; vertical-align: middle; }\n        .brand-title { display:inline-block; vertical-align: middle; color: var(--primary);\n            font-weight: 800; font-size: 18pt; letter-spacing:.3px; }\n        /* Titoli serif (fallback generico) */\n        .serif { font-family: \"Georgia\", \"Times New Roman\", serif; }\n\n        .meta { float: right; text-align: right; color: var(--muted); font-size: 10pt; }\n        .chip { display:inline-block; padding:2px 8px; border-radius:999px; background: var(--chip-bg); color: var(--chip-fg); font-weight:700; font-size:9pt; }\n        .serial { display:inline-block; padding:2px 6px; border:1px solid var(--border); background:#f3f4f6; border-radius:6px;\n            font-family: \"Courier New\", Courier, monospace; font-size:10pt; color: var(--ink); }\n\n        /* --- Titolo documento --- */\n        .title { text-align:center; margin: 10mm 0 8mm 0; }\n        .title h1 { margin:0; font-size: 24pt; color: var(--ink); }\n        .subtitle { margin-top: 2mm; color: var(--muted); font-size: 11pt; }\n        .badge { display:inline-block; margin-top: 4mm; padding: 3px 10px; border-radius: 999px;\n            background: var(--badge-bg); color: var(--badge-fg); font-weight: 700; font-size: 10pt; border:1px solid #163c6a; }\n\n        /* --- Griglie semplici --- */\n        .row { width:100%; }\n        .col { display:inline-block; vertical-align: top; width: 48.5%; }\n        .spacer { height: 2mm; }\n\n        /* --- Card dati --- */\n        .card { border:1px solid var(--border); border-radius:10px; background:#fff; padding:8mm; margin-bottom:8mm; }\n        .kv { margin: 3mm 0; }\n        .kv label { display:block; font-size:9pt; color: var(--muted); margin-bottom: 1mm; letter-spacing: .2px; }\n        .kv .v { font-size:12.5pt; font-weight:700; color: var(--ink); }\n\n        /* --- Sezione verifica --- */\n        .verify { border:1px dashed #9aa3b2; background: var(--verify-bg); border-radius:10px; padding:6mm; }\n        .qr { display:inline-block; width: 120px; vertical-align: top; }\n        .qr img { width:120px; height:120px; border:1px solid var(--border); border-radius:8px; }\n        .verify-text { display:inline-block; width: calc(100% - 130px); padding-left: 6mm; vertical-align: top; }\n        .verify-label { font-weight: 800; margin-bottom: 2mm; letter-spacing: .2px; }\n        .verify-url { word-break: break-all; color: var(--primary-900); font-weight: 800; font-size: 10.5pt; margin: 2mm 0 3mm; }\n        .note { color: var(--muted); font-size: 9.5pt; }\n\n        /* --- Firme --- */\n        .sign { margin-top: 10mm; }\n        .sigbox { display:inline-block; width: 48.5%; vertical-align: top; text-align: center; }\n        .sigline { border-top:1px solid var(--ink); margin-top:18mm; padding-top:2mm; font-size:10pt; color:#374151; }\n        .siglabel { font-size:9pt; color: var(--muted); }\n\n        /* --- Footer --- */\n        .footer { margin-top: 12mm; border-top:1px solid var(--border); padding-top:4mm; color: var(--muted); font-size: 9pt;\n            display: table; width:100%; }\n        .foot-left, .foot-right { display: table-cell; width:50%; vertical-align: middle; }\n        .foot-right { text-align: right; }\n\n        /* --- Divider sottile con accento --- */\n        .divider { height:1px; background: linear-gradient(90deg, rgba(181,139,0,.0), rgba(181,139,0,.8), rgba(181,139,0,.0)); margin: 6mm 0; }\n\n        /* Evita orfani/righe spezzate brutte in PDF */\n        .kv .v, .verify, .sigbox, .title { page-break-inside: avoid; }\n    </style>\n</head>\n<body>\n<div class=\"page pro\">\n    <div class=\"frame\">\n        <!-- Banda superiore -->\n        <div class=\"security-band\">\n            <div class=\"left\">\n                <div class=\"security-kicker\">Documento digitale firmato e verificabile</div>\n                <div class=\"security-title serif\">Attestato di conseguimento</div>\n            </div>\n            <div class=\"right\">\n                <span class=\"chip\">Seriale</span>\n                <span class=\"serial\" th:text=\"${serial}\">ABCDEF123456</span>\n            </div>\n        </div>\n\n        <!-- Header -->\n        <div class=\"header\">\n            <div class=\"brand\">\n                <div class=\"img\">\n                    <img th:src=\"${logoUrl}\" alt=\"${logoUrl}\"></img>\n                </div>\n                <span class=\"crest\"></span>\n                <span class=\"brand-title serif\" th:text=\"${tenantName != null ? tenantName : \'Azienda Demo\'}\">Azienda Demo</span>\n            </div>\n            <div class=\"meta\">\n                <div>\n                    <span th:text=\"${issuerName != null ? issuerName : \'Dott. Mario Rossi\'}\">Dott. Mario Rossi</span><br></br>\n                    <span th:text=\"${issuerTitle != null ? issuerTitle : \'Direttore Formazione\'}\">Direttore Formazione</span>\n                </div>\n                <div class=\"spacer\"></div>\n                 <span>Emesso il:</span>\n                 <span th:text=\"${#temporals.format(issuedAt, \'dd/MM/yyyy\')}\">01/01/2025</span>\n            </div>\n            <div style=\"clear:both;\"></div>\n        </div>\n\n        <!-- Titolo -->\n        <div class=\"title\">\n            <h1 class=\"serif\">Certificato di Completamento</h1>\n            <div class=\"subtitle\">Si attesta che il/la candidato/a ha completato con profitto il seguente percorso.</div>\n            <div class=\"badge\">Attestato digitale verificabile</div>\n        </div>\n\n        <!-- Dati principali -->\n        <div class=\"card\">\n            <div class=\"row\">\n                <div class=\"col\">\n                    <div class=\"kv\">\n                        <label>Intestatario</label>\n                        <div class=\"v\" th:text=\"${ownerName}\">Nome Cognome</div>\n                    </div>\n                    <div class=\"kv\">\n                        <label>Email</label>\n                        <div class=\"v\" th:text=\"${ownerEmail}\">nome.cognome@example.com</div>\n                    </div>\n                    <div class=\"kv\">\n                        <label>Corso / Oggetto</label>\n                        <div class=\"v\" th:text=\"${courseName}\">SPRING-BOOT</div>\n                    </div>\n                </div>\n                <div class=\"col\">\n                    <div class=\"kv\">\n                        <label>Codice interno</label>\n                        <div class=\"v\"><span class=\"chip\" th:text=\"${courseCode}\">SPRING-K987</span></div>\n                    </div>\n                    <div class=\"kv\">\n                        <label>Ore / Esito</label>\n                        <div class=\"v\">\n                            <span th:text=\"${hours}\">45</span><span> ore — </span><span th:text=\"${grade}\">A</span>\n                        </div>\n                    </div>\n                </div>\n            </div>\n\n            <div class=\"divider\"></div>\n\n            <!-- Verifica pubblica -->\n            <div class=\"verify\">\n                <div class=\"qr\">\n                    <img th:src=\"\'data:image/png;base64,\' + ${qrBase64}\" alt=\"QR Code\"></img>\n                </div>\n                <div class=\"verify-text\">\n                    <div class=\"verify-label serif\">Verifica pubblica</div>\n                    <div class=\"note\">Inquadra il QR oppure visita:</div>\n                    <div class=\"verify-url\" th:text=\"${verifyUrl}\">https://example.org/v/ABCDE12345</div>\n                    <div class=\"note\">\n                        Seriale: <span class=\"serial\" th:text=\"${serial}\">ABCDEF123456</span>\n                    </div>\n                </div>\n            </div>\n        </div>\n\n        <!-- Firme -->\n        <div class=\"sign\">\n            <div class=\"sigbox\">\n                <div class=\"sigline serif\" th:text=\"${issuerName}\">Dott. Mario Rossi</div>\n                <div class=\"siglabel\" th:text=\"${issuerTitle}\">Direttore Formazione</div>\n                <div class=\"img\">\n                    <img th:src=\"${signatureImageUrl}\" width=\"100px\" height=\"70px\" alt=\"Signature\"></img>\n                </div>\n            </div>\n            <div class=\"sigbox\">\n                <div class=\"sigline serif\" th:text=\"${tenantName}\">Azienda Demo</div>\n                <div class=\"siglabel\">Autorità Emettente</div>\n            </div>\n        </div>\n\n        <!-- Footer -->\n        <div class=\"footer\">\n            <div class=\"foot-left\">\n                © <span th:text=\"${#temporals.format(#temporals.createNow(),\'yyyy\')}\">2025</span>\n                · <span th:text=\"${tenantName != null ? tenantName : \'Azienda Demo\'}\">Azienda Demo</span>\n            </div>\n            <div class=\"foot-right\">\n                Documento firmato digitalmente · Verifica: <span th:text=\"${verifyUrl}\">https://example.org/v/ABCDE12345</span>\n            </div>\n        </div>\n    </div>\n</div>\n</body>\n</html>\n','{\"grade\": \"A\", \"hours\": 34, \"ownerName\": \"Marco Albasini\", \"courseCode\": \"SPRING-K098\", \"courseName\": \"SPRING-BOOT\", \"ownerEmail\": \"malbasini@gmail.com\", \"tenantName\": \"Azienda Demo\"}','{\"grade\": {\"type\": \"string\", \"label\": \"Esito\", \"required\": false}, \"hours\": {\"type\": \"number\", \"label\": \"Ore\", \"required\": true}, \"ownerName\": {\"type\": \"string\", \"label\": \"Intestatario\", \"required\": true}, \"courseCode\": {\"type\": \"string\", \"label\": \"Codice corso\", \"required\": true}, \"courseName\": {\"type\": \"string\", \"label\": \"Nome corso\", \"required\": true}, \"ownerEmail\": {\"type\": \"string\", \"label\": \"Email\", \"required\": true}}',NULL,_binary '','2025-11-10 17:55:13','2025-11-13 03:09:43'),(43,14,'GUIDE INFORMATICHE','14.0','<DOCTYPE HTML>\n  <html>\n  <head>\n    </head>\n  <body>\n  </body>\n  </html>',NULL,'{\"profile\": {\"website\": \"https://www.acme.it\", \"displayName\": \"ACME Training S.r.l.\", \"contactEmail\": \"info@acme.it\"}, \"branding\": {\"logoUrl\": \"/files/tenant-14/logo.png\", \"issuerName\": \"Dott. Mario Rossi\", \"issuerRole\": \"Direttore Formazione\", \"primaryColor\": \"#0d6efd\", \"defaultTemplateId\": 4, \"signatureImageUrl\": \"/files/tenant-14/signature.png\"}}',NULL,_binary '\0','2025-11-19 04:34:28','2025-11-19 04:34:28');
/*!40000 ALTER TABLE `template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant`
--

DROP TABLE IF EXISTS `tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `plan` varchar(30) NOT NULL,
  `status` varchar(255) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant`
--

LOCK TABLES `tenant` WRITE;
/*!40000 ALTER TABLE `tenant` DISABLE KEYS */;
INSERT INTO `tenant` VALUES (14,'Azienda Demo','ENTERPRISE','ACTIVE','2025-10-06 03:56:01'),(15,'ROSSI S.P.A.','FREE','ACTIVE','2025-10-12 01:21:00');
/*!40000 ALTER TABLE `tenant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant_settings`
--

DROP TABLE IF EXISTS `tenant_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_settings` (
  `tenant_id` bigint NOT NULL,
  `json_settings` json DEFAULT NULL,
  `plan_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `billing_cycle` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `current_period_start` timestamp NOT NULL,
  `current_period_end` timestamp NOT NULL,
  `certs_per_month` int DEFAULT NULL,
  `api_call_per_month` int DEFAULT NULL,
  `storage_mb` bigint DEFAULT NULL,
  `support` varchar(145) DEFAULT NULL,
  `provider` varchar(45) DEFAULT NULL,
  `checkout_session_id` varchar(450) DEFAULT NULL,
  `subscription_id` varchar(450) DEFAULT NULL,
  `last_invoice_id` varchar(450) DEFAULT NULL,
  `status` varchar(45) NOT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`tenant_id`),
  CONSTRAINT `fk_tenant_settings_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant_settings`
--

LOCK TABLES `tenant_settings` WRITE;
/*!40000 ALTER TABLE `tenant_settings` DISABLE KEYS */;
INSERT INTO `tenant_settings` VALUES (14,'{\"profile\": {\"website\": \"https://www.acme.it\", \"displayName\": \"ACME Training S.r.l.\", \"contactEmail\": \"info@acme.it\"}, \"branding\": {\"logoUrl\": \"/files/tenant-14/logo.png\", \"issuerName\": \"Dott. Mario Rossi\", \"issuerRole\": \"Direttore Formazione\", \"primaryColor\": \"#0d6efd\", \"defaultTemplateId\": 4, \"signatureImageUrl\": \"/files/tenant-14/signature.png\"}}','ENTERPRISE','ANNUAL','2025-11-22 01:15:37','2026-11-22 01:15:37',5000,1000000,200000,'Supporto Enterprise & SLA','STRIPE','cs_test_a1LPTS4P0BsvioFsk2E6pYlNv1yX44xxuO6yQ4tQ8TPJ4gBfFayGohwLwN',NULL,NULL,'ACTIVE','2025-11-22 02:15:36');
/*!40000 ALTER TABLE `tenant_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usage_meter`
--

DROP TABLE IF EXISTS `usage_meter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usage_meter` (
  `tenant_id` bigint NOT NULL,
  `usage_day` date NOT NULL,
  `certs_generated` int NOT NULL DEFAULT '0',
  `pdf_storage_mb` decimal(10,2) NOT NULL DEFAULT '0.00',
  `api_calls` int NOT NULL DEFAULT '0',
  `last_update_ts` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `verifications_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`tenant_id`,`usage_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usage_meter`
--

LOCK TABLES `usage_meter` WRITE;
/*!40000 ALTER TABLE `usage_meter` DISABLE KEYS */;
INSERT INTO `usage_meter` VALUES (14,'2025-10-29',1,0.00,0,'2025-10-29 18:58:18',0),(14,'2025-10-30',4,0.00,2,'2025-10-30 09:15:00',0),(14,'2025-10-31',8,0.00,0,'2025-10-31 20:09:00',2),(14,'2025-11-01',9,0.01,0,'2025-11-01 21:46:47',15),(14,'2025-11-02',0,0.01,0,'2025-11-02 16:45:00',0),(14,'2025-11-03',0,0.01,0,'2025-11-03 17:30:00',0),(14,'2025-11-04',0,0.01,0,'2025-11-04 04:00:00',0),(14,'2025-11-05',1,0.01,0,'2025-11-05 17:45:00',1),(14,'2025-11-06',0,0.01,0,'2025-11-06 05:45:00',0),(14,'2025-11-07',0,0.01,0,'2025-11-07 09:30:00',0),(14,'2025-11-08',11,0.04,0,'2025-11-08 19:00:00',2),(14,'2025-11-09',3,0.07,0,'2025-11-09 08:30:00',0),(14,'2025-11-10',2,0.09,0,'2025-11-10 18:48:07',1),(14,'2025-11-11',1,0.09,0,'2025-11-11 08:15:00',1),(14,'2025-11-12',3,0.18,0,'2025-11-12 15:30:00',4),(14,'2025-11-13',1,0.08,0,'2025-11-13 07:15:00',1),(14,'2025-11-14',0,0.08,0,'2025-11-14 02:30:00',0),(14,'2025-11-15',0,0.08,0,'2025-11-15 16:00:00',0),(14,'2025-11-16',0,0.08,0,'2025-11-16 03:15:00',0),(14,'2025-11-18',0,0.08,0,'2025-11-18 21:45:00',0),(14,'2025-11-19',0,0.08,0,'2025-11-19 20:15:00',0),(14,'2025-11-20',0,0.08,0,'2025-11-20 21:45:00',0),(14,'2025-11-21',0,0.08,0,'2025-11-21 21:45:06',0),(14,'2025-11-22',0,0.08,0,'2025-11-22 01:15:00',0),(15,'2025-10-30',0,0.00,0,'2025-10-30 09:15:00',0),(15,'2025-10-31',0,0.00,0,'2025-10-31 19:16:27',0),(15,'2025-11-01',0,0.00,0,'2025-11-01 21:45:00',0),(15,'2025-11-02',0,0.00,0,'2025-11-02 16:45:00',0),(15,'2025-11-03',0,0.00,0,'2025-11-03 17:30:00',0),(15,'2025-11-04',0,0.00,0,'2025-11-04 04:00:00',0),(15,'2025-11-05',0,0.00,0,'2025-11-05 17:45:00',0),(15,'2025-11-06',0,0.00,0,'2025-11-06 05:45:00',0),(15,'2025-11-07',0,0.00,0,'2025-11-07 09:30:00',0),(15,'2025-11-08',0,0.00,0,'2025-11-08 19:00:00',0),(15,'2025-11-09',0,0.00,0,'2025-11-09 08:30:00',0),(15,'2025-11-10',0,0.00,0,'2025-11-10 18:45:00',0),(15,'2025-11-11',0,0.00,0,'2025-11-11 08:15:00',0),(15,'2025-11-12',0,0.00,0,'2025-11-12 15:30:00',0),(15,'2025-11-13',0,0.00,0,'2025-11-13 07:15:00',0),(15,'2025-11-14',0,0.00,0,'2025-11-14 02:30:00',0),(15,'2025-11-15',0,0.00,0,'2025-11-15 16:00:00',0),(15,'2025-11-16',0,0.00,0,'2025-11-16 03:15:00',0),(15,'2025-11-18',0,0.00,0,'2025-11-18 21:45:00',0),(15,'2025-11-19',0,0.00,0,'2025-11-19 20:15:00',0),(15,'2025-11-20',0,0.00,0,'2025-11-20 21:45:00',0),(15,'2025-11-21',0,0.00,0,'2025-11-21 21:45:06',0),(15,'2025-11-22',0,0.00,0,'2025-11-22 01:15:00',0);
/*!40000 ALTER TABLE `usage_meter` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `username_UNIQUE` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (14,'malbasini','$2a$10$NrshmRWtVntY4YpU4jlpAem0nVda6RouBeTNX73U4V3h3iL1l9Ply','malbasini@gmail.com','2025-10-06 03:56:01'),(15,'admin','$2a$10$aI.xyHrBJHAxfF6uaN4qf.VYJCQe4My5m/oFYYfEMoy7Ar8MmDJxu','admin@example.com','2025-10-12 01:21:00'),(24,'Luca Verdi','$2a$10$d2uW0QHARGA6aIxUBZLzXOeZBCL8xmPjK8HkT8rgnn1lFxVjbu9wa','luca.verdi@example.com','2025-10-23 02:24:48');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `verification_token`
--

DROP TABLE IF EXISTS `verification_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `verification_token` (
  `code` varchar(24) NOT NULL,
  `certificate_id` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` timestamp NULL DEFAULT NULL,
  `kid` varchar(255) DEFAULT NULL,
  `jti` varchar(255) DEFAULT NULL,
  `sha256_cached` varchar(255) DEFAULT NULL,
  `compact_jws` text,
  PRIMARY KEY (`code`),
  KEY `fk_token_certificate` (`certificate_id`),
  KEY `idx_verif_token_jti` (`jti`),
  CONSTRAINT `fk_token_certificate` FOREIGN KEY (`certificate_id`) REFERENCES `certificate` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `verification_token`
--

LOCK TABLES `verification_token` WRITE;
/*!40000 ALTER TABLE `verification_token` DISABLE KEYS */;
INSERT INTO `verification_token` VALUES ('QRWP82XJLLPFDNUVY84HU9NC',86,'2025-11-13 02:39:45','2026-11-13 02:39:45','key-2025-10-30','d6a99fe9-81e2-4275-84d5-3f9ae1d865ce','Xn9L6Q4gwRCrAtTW_VlVcGsN02Uh-Of_xJT4xsgpJ_8','eyJraWQiOiJrZXktMjAyNS0xMC0zMCIsInR5cCI6IkpPU0UiLCJhbGciOiJFZERTQSJ9.eyJleHAiOjE3OTQ1NDExODUsImNlcnRJZCI6ODYsImlhdCI6MTc2MzAwNTE4NSwidGVuYW50SWQiOjE0LCJqdGkiOiJkNmE5OWZlOS04MWUyLTQyNzUtODRkNS0zZjlhZTFkODY1Y2UiLCJzaGEyNTYiOiJYbjlMNlE0Z3dSQ3JBdFRXX1ZsVmNHc04wMlVoLU9mX3hKVDR4c2dwSl84In0.3PEnWPErrUNEQShaAIjhjtOzu_NsxCESfK_kTFIWQaws65mFOoxp2Ca_gkw5v0lQx3_t56P0VPHXB_gasbnuAg');
/*!40000 ALTER TABLE `verification_token` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-11-22  3:29:15
