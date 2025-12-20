-- MySQL dump 10.13  Distrib 8.0.40, for macos14 (arm64)
--
-- Host: localhost    Database: VeriCert2
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
) ENGINE=InnoDB AUTO_INCREMENT=112 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `certificate`
--

LOCK TABLES `certificate` WRITE;
/*!40000 ALTER TABLE `certificate` DISABLE KEYS */;
INSERT INTO `certificate` VALUES (107,16,'F9FE193097904E718486','Marco Albasini','malbasini@gmail.com','/files/16/F9FE193097904E718486.pdf','dUfF8KMPCKK-D5G36_fKoxvDXnjCEyY9en6L6V7Ikxg','ISSUED','2025-12-10 17:17:00',NULL,NULL),(109,16,'AA4E271A7A7B419FB2B0','Mario Bianchi','marco.albasini@pec.it','/files/16/AA4E271A7A7B419FB2B0.pdf','gOfL_AwNEzYMZO6jWKeUKD-PHTTGbGpoOyJJsGk5GXI','ISSUED','2025-12-11 12:30:06',NULL,NULL),(111,16,'FFCC44EF38C94546B8C7','Luca Rossi','malbasini@gmail.com','/files/16/FFCC44EF38C94546B8C7.pdf','tZS6FiHTVS6dR-GN70xntjOAZ5gB7dshCsX4hUdn0m8','ISSUED','2025-12-11 23:13:01',NULL,NULL);
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
INSERT INTO `membership` VALUES (33,16,'ADMIN','ACTIVE'),(34,16,'ISSUER','ACTIVE'),(35,16,'MANAGER','ACTIVE');
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
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
INSERT INTO `payments` VALUES (24,16,NULL,'STRIPE','cs_test_a1YrBW3UIUQQKcKxr68Af03ooAhRck05dmT7jOaE4qBgojAKiZgv9UYxVR',NULL,'PENDING',1208,'EUR','Piano PRO (Mensile)','d13dcff1-7609-474b-a65c-31d71c1e5640','2025-12-09 05:11:38','2025-12-09 05:11:38'),(25,16,NULL,'PAYPAL',NULL,'0ET58576BS198834Y','PENDING',1208,'EUR','Piano PRO (Mensile)',NULL,'2025-12-09 05:11:57','2025-12-09 05:11:57'),(26,16,NULL,'STRIPE','cs_test_a1aPJUMvwkNNWQ5aPkuXd2sidc1as4eryMq62R9SJgb3sHDqW176coC24w','pi_3Scu6aIX50JfMIoY1ozsBzVS','SUCCEEDED',1208,'EUR','Piano PRO (Mensile)','4bf00c70-7d87-4a37-ab24-9f0830f1b630','2025-12-10 19:35:19','2025-12-10 19:35:29'),(27,16,NULL,'STRIPE','cs_test_a1CoH4iLMoZekvc7d4aYXIlu7kMFdBux3SK7v2NcSU4vh2XlF4DD8jselm','pi_3SdJqkIX50JfMIoY0QcQO0jD','SUCCEEDED',1208,'EUR','Piano PRO (Mensile)','29749faa-fde5-485a-9662-155be1814463','2025-12-11 23:04:35','2025-12-11 23:04:52'),(28,16,NULL,'PAYPAL',NULL,'6TL703329L100742N','SUCCEEDED',1208,'EUR','Piano PRO (Mensile)',NULL,'2025-12-11 23:05:07','2025-12-11 23:05:39'),(29,16,NULL,'STRIPE','cs_test_a1UpLX04KIIAQvZm5Uca8RjhGVxpmpAWeMqWBkkoTs6vA4mbNJqDtEJLpj','pi_3SdZDaIX50JfMIoY2iXhB3TC','SUCCEEDED',11592,'EUR','Piano PRO (Annuale -20%)','5dd5ed89-9f65-46d9-833c-246fd970512e','2025-12-12 15:29:14','2025-12-12 15:29:28'),(30,16,NULL,'STRIPE','cs_test_a1M60WN8wRQUKO7twXhUBbyEZF3yENTqHFIieM0ENIYyH5o44oCEXOiXh5','pi_3SdZE6IX50JfMIoY0B8oKfkT','SUCCEEDED',3648,'EUR','Piano BUSINESS (Mensile)','6bf60773-d2e9-4323-995f-a47b32299752','2025-12-12 15:29:48','2025-12-12 15:30:00'),(31,16,NULL,'STRIPE','cs_test_a1RmDCogQmY5yxN1rB8vcpeNdhjp2aLw9QALKmPKSFpnkwsCqJR4uvwDKb',NULL,'PENDING',35016,'EUR','Piano BUSINESS (Annuale -20%)','4ab07ace-4ab3-48c6-a628-1f3b86a5ed8d','2025-12-12 15:30:16','2025-12-12 15:30:16'),(32,16,NULL,'STRIPE','cs_test_a1LagxSEmQgehDiZzmVZnNm5c4bQZ99WZkNC1xPIFN4awEhCEeKM7PfJwG','pi_3SdZH6IX50JfMIoY03xpPuMh','SUCCEEDED',35016,'EUR','Piano BUSINESS (Annuale -20%)','5fc9f781-f186-4b58-9978-0e72dcfb40ab','2025-12-12 15:32:56','2025-12-12 15:33:05'),(33,16,NULL,'STRIPE','cs_test_a1VFglGaKp4iFdV6rI07LrzjvHu9yqd41nMQ9fgoigSjsWtO8rTwF09Hco','pi_3SdZHQIX50JfMIoY0EM6vn6E','SUCCEEDED',12188,'EUR','Piano ENTERPRISE (Mensile)','fa3eb9bf-8703-4243-9b44-5dc94c1503c7','2025-12-12 15:33:15','2025-12-12 15:33:25'),(34,16,NULL,'STRIPE','cs_test_a14eMZefMcziC8MtsyXCaClPinSdelrOlYEq1JwWfAYbfmE4mh86ZHjzfF','pi_3SdZHoIX50JfMIoY2uRDc110','SUCCEEDED',117000,'EUR','Piano ENTERPRISE (Annuale -20%)','69816909-715a-44e9-9b83-4bdfa5baa8ac','2025-12-12 15:33:39','2025-12-12 15:33:49'),(35,16,NULL,'PAYPAL',NULL,'3SA26827G0894623R','SUCCEEDED',1208,'EUR','Piano PRO (Mensile)',NULL,'2025-12-12 15:34:20','2025-12-12 15:34:43'),(36,16,NULL,'PAYPAL',NULL,'7DX48182TU746822P','SUCCEEDED',11592,'EUR','Piano PRO (Annuale -20%)',NULL,'2025-12-12 15:35:01','2025-12-12 15:35:30'),(37,16,NULL,'PAYPAL',NULL,'2EX569980X338501K','SUCCEEDED',3648,'EUR','Piano BUSINESS (Mensile)',NULL,'2025-12-12 15:35:43','2025-12-12 15:35:50'),(38,16,NULL,'PAYPAL',NULL,'8FK89886YB142671R','SUCCEEDED',35016,'EUR','Piano BUSINESS (Annuale -20%)',NULL,'2025-12-12 15:36:01','2025-12-12 15:36:17'),(39,16,NULL,'PAYPAL',NULL,'60H88662CT2090502','SUCCEEDED',12188,'EUR','Piano ENTERPRISE (Mensile)',NULL,'2025-12-12 15:36:27','2025-12-12 15:36:37'),(40,16,NULL,'PAYPAL',NULL,'79J65301H9823813Y','SUCCEEDED',117000,'EUR','Piano ENTERPRISE (Annuale -20%)',NULL,'2025-12-12 15:36:46','2025-12-12 15:36:55'),(41,16,NULL,'STRIPE','cs_test_a1LNXzd5Gh8zUoJh8qijWA5kaxT8SPoksyg1H5ioL3T5hzPxsMT5s9wTsc',NULL,'PENDING',1208,'EUR','Piano PRO (Mensile)','52b9f013-d870-4071-be60-f40412119848','2025-12-13 00:16:12','2025-12-13 00:16:12'),(42,16,NULL,'STRIPE','cs_test_a1K6uyWbR4xN7UiqT21VQtHGzdjlmkipNrxRYChEDWUKVpy6Q59UYwpqLr',NULL,'PENDING',1208,'EUR','Piano PRO (Mensile)','092d74a2-5c04-412c-a708-bbc4ec9d19a2','2025-12-13 00:20:42','2025-12-13 00:20:42'),(43,16,NULL,'STRIPE','cs_test_a1wlk9B0LxMcwkMorLGkgmehdZKWNNspvlzuN5Vh6C8D2sOP5xuasQEicD','pi_3SdhXCIX50JfMIoY28KK8a4E','SUCCEEDED',1208,'EUR','Piano PRO (Mensile)','e4cf31d4-1edd-4812-be82-d8b17bc4c02a','2025-12-13 00:22:06','2025-12-13 00:31:26'),(44,16,NULL,'STRIPE','cs_test_a1xPxRNt4sdsq7NxdXOiDwibiKCjcfajo7Tb5v1MCjbGY4iFMQGzMPQXIN','pi_3SdiS0IX50JfMIoY2w7Q8lKF','SUCCEEDED',1208,'EUR','Piano PRO (Mensile)','df2bf2e2-f6c7-4464-a5a7-12eccd33f438','2025-12-13 01:20:48','2025-12-13 01:20:58'),(45,16,NULL,'STRIPE','cs_test_a1qQeZbVOE4bRjuzJUPz47QuBP7TZexB1foOzRgYwxSIRo6ndIUwnJpDAH',NULL,'PENDING',1208,'EUR','Piano PRO (Mensile)','6ae758af-feaf-4d0e-b976-bb7c23786498','2025-12-13 01:26:12','2025-12-13 01:26:12'),(46,16,NULL,'STRIPE','cs_test_a1rJwGZPVWoQnCWkZCPbjMjOC2tUdgqy2sfzVOzvGBXFmeA4sWiDpIgG0W','pi_3SdiZUIX50JfMIoY22MnPvyn','SUCCEEDED',1208,'EUR','Piano PRO (Mensile)','25a8bbe4-30c2-4d2b-a915-90f3c51b7536','2025-12-13 01:28:33','2025-12-13 01:28:42'),(47,16,NULL,'PAYPAL',NULL,'6EJ005309Y302281G','SUCCEEDED',1208,'EUR','Piano PRO (Mensile)',NULL,'2025-12-13 01:33:45','2025-12-13 01:34:00'),(48,16,NULL,'PAYPAL',NULL,'8P2577482F1853246','SUCCEEDED',1208,'EUR','Piano PRO (Mensile)',NULL,'2025-12-14 08:05:36','2025-12-14 08:05:50'),(49,16,NULL,'PAYPAL',NULL,'4LF61893V53984816','SUCCEEDED',1208,'EUR','Piano PRO (Mensile)',NULL,'2025-12-14 08:43:18','2025-12-14 08:43:36'),(50,16,NULL,'PAYPAL',NULL,'1JP6915597810842E','PENDING',3648,'EUR','Piano BUSINESS (Mensile)',NULL,'2025-12-14 22:30:57','2025-12-14 22:30:57');
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
INSERT INTO `persistent_logins` VALUES ('malbasini','4Vcq9/1pJXj+2xqVRoywYQ==','97wZ4qXb7Id0rQyuAYLy+g==','2025-12-14 23:46:55');
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
INSERT INTO `plan_definitions` VALUES (1,'FREE','FREE',5,100,100,'Supporto community',0,0,'0','0','0','0','0','0','2025-11-15 00:31:46'),(2,'PRO','PRO',100,50000,5000,'Supporto email',990,792,'price_1SXPMXIX50JfMIoY8IequQDT','price_1SXPO6IX50JfMIoY8Tmnbxsv','P-7PG93597W86489544NE7D57Q','P-6W092672K03879817NE7D6RI','22%','20%','2025-11-15 01:10:08'),(3,'BUSINESS','BUSINESS',500,200000,25000,'Supporto prioritario',2990,2392,'price_1SWu7PIX50JfMIoYKsSqKdbU','price_1SWu8kIX50JfMIoYF7KjQD0b','P-9FL32270RM5431112NE7D7KA','P-13R04593RL216125LNE7EAAA','22%','20%','2025-11-15 01:15:25'),(4,'ENTERPRISE','ENTERPRISE',5000,1000000,200000,'Supporto Enterprise & SLA',9990,7992,'price_1SWuGuIX50JfMIoY774Nflhw','price_1SWuJbIX50JfMIoYEqqcxria','P-4DU73391TB6044142NE7EANI','P-3GW4411637574502NNE7EAYA','22%','20%','2025-11-15 01:20:30');
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
INSERT INTO `signing_key` VALUES ('key-2025-10-30','-----BEGIN PUBLIC KEY-----\n      MCowBQYDK2VwAyEA698vPNPQmHeEggeDIBzxqAK2gJNHXBOVGrdcrWn8ZHM=\n         -----END PUBLIC KEY-----','ACTIVE','2025-10-30 14:49:11',NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=59 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `template`
--

LOCK TABLES `template` WRITE;
/*!40000 ALTER TABLE `template` DISABLE KEYS */;
INSERT INTO `template` VALUES (1,16,'Template Demo','1.1','<!DOCTYPE html>\n<html lang=\"it\" xmlns:th=\"http://www.thymeleaf.org\">\n<head>\n    <meta charset=\"UTF-8\"></meta>\n    <title th:text=\"\'Certificato \' + ${serial}\">Certificato</title>\n    <style>\n        /* --- Pagina / palette PRO --- */\n        @page { size: A4; margin: 14mm; }\n        * { box-sizing: border-box; }\n        html, body { margin:0; padding:0; color:#0f172a!important; font-family: Arial, Helvetica, sans-serif; }\n\n        /* Tavolozza PRO (blu profondo, accenti dorati) */\n        .pro {\n            --ink:#0f172a!important; --muted:#6b7280!important; --bg:#ffffff!important; --paper:#f8fafc!important;\n            --primary:#0a376e!important;            /* blu profondo */\n            --primary-900:#082a53!important;\n            --accent:#b58b00!important;             /* oro sobrio */\n            --border:#d1d5db!important;\n            --verify-bg:#f3f4f6!important;\n            --badge-bg:#0a376e!important; --badge-fg:#fff!important;\n            --chip-bg:#ffffff!important; --chip-fg:#000000!important;\n        }\n\n        /* --- Cornice principale --- */\n        .page { background: var(--paper); border: 1px solid var(--border); border-radius: 10px; }\n        .frame { margin: 8mm; padding: 10mm; background: var(--bg); border: 2px solid var(--primary); border-radius: 8px; }\n\n        /* --- Banda superiore di sicurezza --- */\n        .security-band {\n            background: linear-gradient(90deg, var(--primary) 0%, var(--primary-900) 100%);\n            color: #fff; padding: 3mm 6mm; border-radius: 6px; margin-bottom: 8mm;\n        }\n        .security-band .left { display:inline-block; width: 60%; vertical-align: middle; }\n        .security-band .right { display:inline-block; width: 39%; text-align: right; vertical-align: middle; }\n        .security-kicker { font-size: 9pt; opacity: .9; }\n        .security-title { font-size: 12pt; font-weight: 700; letter-spacing: .5px; }\n\n        /* --- Header --- */\n        .header { border-bottom: 2px solid var(--border); padding-bottom: 6mm; margin-bottom: 8mm; }\n        .brand { display: inline-block; vertical-align: top; }\n        .crest { display:inline-block; width:42px; height:42px; border-radius:8px; background: var(--primary);\n            border:2px solid #fff; outline:2px solid var(--primary); margin-right:10px; vertical-align: middle; }\n        .brand-title { display:inline-block; vertical-align: middle; color: var(--primary);\n            font-weight: 800; font-size: 18pt; letter-spacing:.3px; }\n        /* Titoli serif (fallback generico) */\n        .serif { font-family: \"Georgia\", \"Times New Roman\", serif; }\n\n        .meta { float: right; text-align: right; color: var(--muted); font-size: 10pt; }\n        .chip { display:inline-block; padding:2px 8px; border-radius:999px; background: var(--chip-bg); color: var(--chip-fg); font-weight:700; font-size:9pt; }\n        .serial { display:inline-block; padding:2px 6px; border:1px solid var(--border); background:#f3f4f6; border-radius:6px;\n            font-family: \"Courier New\", Courier, monospace; font-size:10pt; color: var(--ink); }\n\n        /* --- Titolo documento --- */\n        .title { text-align:center; margin: 10mm 0 8mm 0; }\n        .title h1 { margin:0; font-size: 24pt; color: var(--ink); }\n        .subtitle { margin-top: 2mm; color: var(--muted); font-size: 11pt; }\n        .badge { display:inline-block; margin-top: 4mm; padding: 3px 10px; border-radius: 999px;\n            background: var(--badge-bg); color: var(--badge-fg); font-weight: 700; font-size: 10pt; border:1px solid #163c6a; }\n\n        /* --- Griglie semplici --- */\n        .row { width:100%; }\n        .col { display:inline-block; vertical-align: top; width: 48.5%; }\n        .spacer { height: 2mm; }\n\n        /* --- Card dati --- */\n        .card { border:1px solid var(--border); border-radius:10px; background:#fff; padding:8mm; margin-bottom:8mm; }\n        .kv { margin: 3mm 0; }\n        .kv label { display:block; font-size:9pt; color: var(--muted); margin-bottom: 1mm; letter-spacing: .2px; }\n        .kv .v { font-size:12.5pt; font-weight:700; color: var(--ink); }\n\n        /* --- Sezione verifica --- */\n        .verify { border:1px dashed #9aa3b2; background: var(--verify-bg); border-radius:10px; padding:6mm; }\n        .qr { display:inline-block; width: 120px; vertical-align: top; }\n        .qr img { width:120px; height:120px; border:1px solid var(--border); border-radius:8px; }\n        .verify-text { display:inline-block; width: calc(100% - 130px); padding-left: 6mm; vertical-align: top; }\n        .verify-label { font-weight: 800; margin-bottom: 2mm; letter-spacing: .2px; }\n        .verify-url { word-break: break-all; color: var(--primary-900); font-weight: 800; font-size: 10.5pt; margin: 2mm 0 3mm; }\n        .note { color: var(--muted); font-size: 9.5pt; }\n\n        /* --- Firme --- */\n        .sign { margin-top: 10mm; }\n        .sigbox { display:inline-block; width: 48.5%; vertical-align: top; text-align: center; }\n        .sigline { border-top:1px solid var(--ink); margin-top:18mm; padding-top:2mm; font-size:10pt; color:#374151; }\n        .siglabel { font-size:9pt; color: var(--muted); }\n\n        /* --- Footer --- */\n        .footer { margin-top: 12mm; border-top:1px solid var(--border); padding-top:4mm; color: var(--muted); font-size: 9pt;\n            display: table; width:100%; }\n        .foot-left, .foot-right { display: table-cell; width:50%; vertical-align: middle; }\n        .foot-right { text-align: right; }\n\n        /* --- Divider sottile con accento --- */\n        .divider { height:1px; background: linear-gradient(90deg, rgba(181,139,0,.0), rgba(181,139,0,.8), rgba(181,139,0,.0)); margin: 6mm 0; }\n\n        /* Evita orfani/righe spezzate brutte in PDF */\n        .kv .v, .verify, .sigbox, .title { page-break-inside: avoid; }\n    </style>\n</head>\n<body>\n<div class=\"page pro\">\n    <div class=\"frame\">\n        <!-- Banda superiore -->\n        <div class=\"security-band\">\n            <div class=\"left\">\n                <div class=\"security-kicker\">Documento digitale firmato e verificabile</div>\n                <div class=\"security-title serif\">Attestato di conseguimento</div>\n            </div>\n            <div class=\"right\">\n                <span class=\"chip\">Seriale</span>\n                <span class=\"serial\" th:text=\"${serial}\">ABCDEF123456</span>\n            </div>\n        </div>\n\n        <!-- Header -->\n        <div class=\"header\">\n            <div class=\"brand\">\n                <div class=\"img\">\n                    <img th:src=\"${logoUrl}\" alt=\"${logoUrl}\"></img>\n                </div>\n                <span class=\"crest\"></span>\n                <span class=\"brand-title serif\" th:text=\"${tenantName != null ? tenantName : \'Azienda Demo\'}\">Azienda Demo</span>\n            </div>\n            <div class=\"meta\">\n                <div>\n                    <span th:text=\"${issuerName != null ? issuerName : \'Dott. Mario Rossi\'}\">Dott. Mario Rossi</span><br></br>\n                    <span th:text=\"${issuerTitle != null ? issuerTitle : \'Direttore Formazione\'}\">Direttore Formazione</span>\n                </div>\n                <div class=\"spacer\"></div>\n                 <span>Emesso il:</span>\n                 <span th:text=\"${#temporals.format(issuedAt, \'dd/MM/yyyy\')}\">01/01/2025</span>\n            </div>\n            <div style=\"clear:both;\"></div>\n        </div>\n\n        <!-- Titolo -->\n        <div class=\"title\">\n            <h1 class=\"serif\">Certificato di Completamento</h1>\n            <div class=\"subtitle\">Si attesta che il/la candidato/a ha completato con profitto il seguente percorso.</div>\n            <div class=\"badge\">Attestato digitale verificabile</div>\n        </div>\n\n        <!-- Dati principali -->\n        <div class=\"card\">\n            <div class=\"row\">\n                <div class=\"col\">\n                    <div class=\"kv\">\n                        <label>Intestatario</label>\n                        <div class=\"v\" th:text=\"${ownerName}\">Nome Cognome</div>\n                    </div>\n                    <div class=\"kv\">\n                        <label>Email</label>\n                        <div class=\"v\" th:text=\"${ownerEmail}\">nome.cognome@example.com</div>\n                    </div>\n                    <div class=\"kv\">\n                        <label>Corso / Oggetto</label>\n                        <div class=\"v\" th:text=\"${courseName}\">SPRING-BOOT</div>\n                    </div>\n                </div>\n                <div class=\"col\">\n                    <div class=\"kv\">\n                        <label>Codice interno</label>\n                        <div class=\"v\"><span class=\"chip\" th:text=\"${courseCode}\">SPRING-K987</span></div>\n                    </div>\n                    <div class=\"kv\">\n                        <label>Ore / Esito</label>\n                        <div class=\"v\">\n                            <span th:text=\"${hours}\">45</span><span> ore — </span><span th:text=\"${grade}\">A</span>\n                        </div>\n                    </div>\n                </div>\n            </div>\n\n            <div class=\"divider\"></div>\n\n            <!-- Verifica pubblica -->\n            <div class=\"verify\">\n                <div class=\"qr\">\n                    <img th:src=\"\'data:image/png;base64,\' + ${qrBase64}\" alt=\"QR Code\"></img>\n                </div>\n                <div class=\"verify-text\">\n                    <div class=\"verify-label serif\">Verifica pubblica</div>\n                    <div class=\"note\">Inquadra il QR oppure visita:</div>\n                    <div class=\"verify-url\" th:text=\"${verifyUrl}\">https://example.org/v/ABCDE12345</div>\n                    <div class=\"note\">\n                        Seriale: <span class=\"serial\" th:text=\"${serial}\">ABCDEF123456</span>\n                    </div>\n                </div>\n            </div>\n        </div>\n\n        <!-- Firme -->\n        <div class=\"sign\">\n            <div class=\"sigbox\">\n                <div class=\"sigline serif\" th:text=\"${issuerName}\">Dott. Mario Rossi</div>\n                <div class=\"siglabel\" th:text=\"${issuerTitle}\">Direttore Formazione</div>\n                <div class=\"img\">\n                    <img th:src=\"${signatureImageUrl}\" width=\"100px\" height=\"70px\" alt=\"Signature\"></img>\n                </div>\n            </div>\n            <div class=\"sigbox\">\n                <div class=\"sigline serif\" th:text=\"${tenantName}\">Azienda Demo</div>\n                <div class=\"siglabel\">Autorità Emettente</div>\n            </div>\n        </div>\n\n        <!-- Footer -->\n        <div class=\"footer\">\n            <div class=\"foot-left\">\n                © <span th:text=\"${#temporals.format(#temporals.createNow(),\'yyyy\')}\">2025</span>\n                · <span th:text=\"${tenantName != null ? tenantName : \'Azienda Demo\'}\">Azienda Demo</span>\n            </div>\n            <div class=\"foot-right\">\n                Documento firmato digitalmente <br></br><span th:text=\"${verifyUrl}\">https://example.org/v/ABCDE12345</span>\n            </div>\n        </div>\n    </div>\n</div>\n</body>\n</html>\n','{\"grade\": \"A\", \"hours\": \"23\", \"ownerName\": \"Luca Rossi\", \"courseCode\": \"SPRING-K098\", \"courseName\": \"SPRING-BOOT\", \"ownerEmail\": \"malbasini@gmail.com\", \"tenantName\": \"Azienda Demo\"}','{\"grade\": {\"type\": \"string\", \"label\": \"Esito\", \"required\": false}, \"hours\": {\"type\": \"string\", \"label\": \"Ore\", \"required\": true}, \"ownerName\": {\"type\": \"string\", \"label\": \"Intestatario\", \"required\": true}, \"courseCode\": {\"type\": \"string\", \"label\": \"Codice corso\", \"required\": true}, \"courseName\": {\"type\": \"string\", \"label\": \"Nome corso\", \"required\": true}, \"ownerEmail\": {\"type\": \"string\", \"label\": \"Email\", \"required\": true}}',NULL,_binary '','2025-11-10 16:55:13','2025-12-14 23:43:08');
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
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant`
--

LOCK TABLES `tenant` WRITE;
/*!40000 ALTER TABLE `tenant` DISABLE KEYS */;
INSERT INTO `tenant` VALUES (16,'Azienda Demo','PRO','ACTIVE','2025-12-09 01:18:48');
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
  `storage_mb` decimal(38,2) DEFAULT NULL,
  `support` varchar(145) DEFAULT NULL,
  `provider` varchar(45) DEFAULT NULL,
  `checkout_session_id` varchar(450) DEFAULT NULL,
  `subscription_id` varchar(450) DEFAULT NULL,
  `last_invoice_id` varchar(450) DEFAULT NULL,
  `status` varchar(45) NOT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `notify_expiring` bit(1) DEFAULT b'0',
  PRIMARY KEY (`tenant_id`),
  CONSTRAINT `fk_tenant_settings_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant_settings`
--

LOCK TABLES `tenant_settings` WRITE;
/*!40000 ALTER TABLE `tenant_settings` DISABLE KEYS */;
INSERT INTO `tenant_settings` VALUES (16,'{\"profile\": {\"website\": \"https://www.acme.it\", \"displayName\": \"ACME Training S.r.l.\", \"contactEmail\": \"info@acme.it\"}, \"branding\": {\"logoUrl\": \"http://localhost:8080/files/16/logo.png\", \"issuerName\": \"Dott. Mario Rossi\", \"issuerRole\": \"Direttore Formazione\", \"primaryColor\": \"#0d6efd\", \"defaultTemplateId\": 4, \"signatureImageUrl\": \"http://localhost:8080/files/16/signature.png\"}}','PRO','MONTHLY','2025-12-14 08:43:36','2026-01-13 08:43:36',100,50000,5000.00,'Supporto email','PAYPAL','2',NULL,NULL,'ACTIVE','2025-12-14 09:43:36',_binary '\0');
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
INSERT INTO `usage_meter` VALUES (16,'2025-12-09',0,0.00,0,'2025-12-09 21:45:00',0),(16,'2025-12-10',5,0.15,42,'2025-12-10 20:01:17',42),(16,'2025-12-11',2,0.15,2,'2025-12-11 21:45:00',2),(16,'2025-12-12',1,0.19,2,'2025-12-12 16:19:07',2),(16,'2025-12-13',0,0.19,0,'2025-12-13 01:30:00',0),(16,'2025-12-14',0,0.19,1,'2025-12-14 21:45:00',1),(16,'2025-12-15',0,0.19,0,'2025-12-15 00:30:00',0);
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
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (33,'malbasini','$2a$10$U0gL/Zr4PBJx3prTAoA6YeC1VVYo/q7ejRu.cTFshUxsIpUIyO8Ba','malbasini@gmail.com','2025-12-09 01:18:48'),(34,'alessandra','$2a$10$m0McS7/OYJBh/WRrm45dF.1RLkjf01YzNbvC70JotcRu91EYsarTO','alessandra.albasini@hotmail.it','2025-12-09 04:27:40'),(35,'concetta','$2a$10$EAHxPMNitqENJFFLFlPUVO8ZXCQJ1eHY3bkyS5TFRoSHiiMf..DsS','giansanti.mariaconcetta@gmail.com','2025-12-14 22:41:46');
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
INSERT INTO `verification_token` VALUES ('3DW7748JCJR4DXQPB6UN4YG4',109,'2025-12-11 12:30:06','2026-12-11 12:30:06','key-2025-10-30','5a15dae4-952a-4e27-b1ba-f622fed20ffd','gOfL_AwNEzYMZO6jWKeUKD-PHTTGbGpoOyJJsGk5GXI','eyJraWQiOiJrZXktMjAyNS0xMC0zMCIsInR5cCI6IkpPU0UiLCJhbGciOiJFZERTQSJ9.eyJqdGkiOiI1YTE1ZGFlNC05NTJhLTRlMjctYjFiYS1mNjIyZmVkMjBmZmQiLCJ0ZW5hbnRJZCI6MTYsImlhdCI6MTc2NTQ1OTgwNiwiY2VydElkIjoxMDksImV4cCI6MTc5Njk5NTgwNiwic2hhMjU2IjoiZ09mTF9Bd05FellNWk82aldLZVVLRC1QSFRUR2JHcG9PeUpKc0drNUdYSSJ9.bhsa5-QIBXSa1ay6JM7cJQCKB46N3pfNgiAYxZdpbTczMVrvJao4bPKEbl3LX8MbZlvQyCD0GDPKnZ4zyv5uAQ'),('GTN3FJ6PGTLBRRVLXJVSJQVS',107,'2025-12-10 17:16:59','2026-12-10 17:16:59','key-2025-10-30','b8a6fca8-8156-4667-b6fa-3454371880ec','dUfF8KMPCKK-D5G36_fKoxvDXnjCEyY9en6L6V7Ikxg','eyJraWQiOiJrZXktMjAyNS0xMC0zMCIsInR5cCI6IkpPU0UiLCJhbGciOiJFZERTQSJ9.eyJzaGEyNTYiOiJkVWZGOEtNUENLSy1ENUczNl9mS294dkRYbmpDRXlZOWVuNkw2VjdJa3hnIiwiZXhwIjoxNzk2OTI2NjE5LCJjZXJ0SWQiOjEwNywiaWF0IjoxNzY1MzkwNjE5LCJ0ZW5hbnRJZCI6MTYsImp0aSI6ImI4YTZmY2E4LTgxNTYtNDY2Ny1iNmZhLTM0NTQzNzE4ODBlYyJ9.YK5gq1BvBReETdRcWnAATw6XwvQ1yybWaUwy9UVOOHkknW6jMJ1ReAGSOpkiuKZVWcGs6BLYZ6Vczy1Hw1GpBg'),('P6G2E5XGU24VTS4VEXFPLXVH',111,'2025-12-11 23:13:00','2026-12-11 23:13:00','key-2025-10-30','a2f453ea-c9ce-424d-9482-55c0f4343c7a','tZS6FiHTVS6dR-GN70xntjOAZ5gB7dshCsX4hUdn0m8','eyJraWQiOiJrZXktMjAyNS0xMC0zMCIsInR5cCI6IkpPU0UiLCJhbGciOiJFZERTQSJ9.eyJjZXJ0SWQiOjExMSwiaWF0IjoxNzY1NDk4MzgwLCJ0ZW5hbnRJZCI6MTYsImp0aSI6ImEyZjQ1M2VhLWM5Y2UtNDI0ZC05NDgyLTU1YzBmNDM0M2M3YSIsInNoYTI1NiI6InRaUzZGaUhUVlM2ZFItR043MHhudGpPQVo1Z0I3ZHNoQ3NYNGhVZG4wbTgiLCJleHAiOjE3OTcwMzQzODB9.ZC8Z7pnH9WOQGOrX_OaJxqAHaU-0hi-LMWKpjGuCDXZ3TkGWZYt6PoGT3bxoKt6Q6gGJbat6ApNqRaKgdX23Dw');
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

-- Dump completed on 2025-12-16 23:17:19
