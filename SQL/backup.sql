-- MySQL dump 10.13  Distrib 8.0.44, for Linux (x86_64)
--
-- Host: localhost    Database: vericert
-- ------------------------------------------------------
-- Server version	8.0.44

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
  `user_vars_json` json DEFAULT NULL,
  `signing_kid` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `serial` (`serial`),
  KEY `status` (`status`),
  KEY `fk_certficate_tenant_idx` (`tenant_id`),
  KEY `idx_cert_signing_kid` (`signing_kid`),
  CONSTRAINT `fk_certficate_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `certificate`
--

LOCK TABLES `certificate` WRITE;
/*!40000 ALTER TABLE `certificate` DISABLE KEYS */;
truncate table `certificate`;
/*!40000 ALTER TABLE `certificate` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `invoice_customer`
--

DROP TABLE IF EXISTS `invoice_customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invoice_customer` (
  `invoice_id` bigint NOT NULL,
  `display_name` varchar(255) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `vat_number` varchar(32) DEFAULT NULL,
  `tax_code` varchar(32) DEFAULT NULL,
  `address_line1` varchar(255) DEFAULT NULL,
  `postal_code` varchar(16) DEFAULT NULL,
  `city` varchar(128) DEFAULT NULL,
  `province` varchar(8) DEFAULT NULL,
  `country` varchar(2) NOT NULL DEFAULT 'IT',
  `pec_email` varchar(255) DEFAULT NULL,
  `sdi_code` varchar(16) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`invoice_id`),
  CONSTRAINT `fk_invoice_customer_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `invoice_customer`
--

LOCK TABLES `invoice_customer` WRITE;
/*!40000 ALTER TABLE `invoice_customer` DISABLE KEYS */;
/*!40000 ALTER TABLE `invoice_customer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `invoice_lines`
--

DROP TABLE IF EXISTS `invoice_lines`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invoice_lines` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint NOT NULL,
  `description` varchar(255) NOT NULL,
  `qty` int NOT NULL DEFAULT '1',
  `unit_price_minor` bigint NOT NULL DEFAULT '0',
  `net_minor` bigint NOT NULL DEFAULT '0',
  `vat_rate` int NOT NULL DEFAULT '22',
  `vat_minor` bigint NOT NULL DEFAULT '0',
  `gross_minor` bigint NOT NULL DEFAULT '0',
  `sort_order` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_invoice_lines_invoice_id` (`invoice_id`),
  CONSTRAINT `fk_invoice_lines_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `invoice_lines`
--

LOCK TABLES `invoice_lines` WRITE;
/*!40000 ALTER TABLE `invoice_lines` DISABLE KEYS */;
/*!40000 ALTER TABLE `invoice_lines` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `invoices`
--

DROP TABLE IF EXISTS `invoices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invoices` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `public_code` varchar(16) NOT NULL,
  `status` varchar(16) NOT NULL,
  `issue_year` int DEFAULT (year(`issued_at`)),
  `number_seq` bigint DEFAULT NULL,
  `number_display` varchar(32) DEFAULT NULL,
  `issued_at` timestamp(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
  `customer_name` varchar(120) DEFAULT NULL,
  `customer_vat` varchar(32) DEFAULT NULL,
  `customer_tax_code` varchar(32) DEFAULT NULL,
  `customer_email` varchar(160) DEFAULT NULL,
  `customer_address_line1` varchar(160) DEFAULT NULL,
  `customer_address_line2` varchar(160) DEFAULT NULL,
  `customer_city` varchar(80) DEFAULT NULL,
  `customer_province` varchar(8) DEFAULT NULL,
  `customer_postal_code` varchar(16) DEFAULT NULL,
  `customer_country` varchar(200) DEFAULT NULL,
  `customer_pec` varchar(160) DEFAULT NULL,
  `customer_sdi` varchar(16) DEFAULT NULL,
  `currency` varchar(3) NOT NULL DEFAULT 'EUR',
  `vat_rate` int NOT NULL DEFAULT '22',
  `net_total_minor` bigint NOT NULL DEFAULT '0',
  `vat_total_minor` bigint NOT NULL DEFAULT '0',
  `gross_total_minor` bigint NOT NULL DEFAULT '0',
  `pdf_blob` longblob,
  `pdf_sha256` varchar(255) DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `template_id` bigint NOT NULL,
  `description` varchar(1000) NOT NULL,
  `invoice_code` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `invoice_saved` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoices_public_code` (`public_code`),
  UNIQUE KEY `uk_invoices_tenant_year_seq` (`tenant_id`,`issue_year`,`number_seq`),
  KEY `ix_invoices_tenant` (`tenant_id`),
  KEY `ix_invoices_issued_at` (`issued_at`),
  KEY `ix_invoices_tenant_status` (`tenant_id`,`status`),
  CONSTRAINT `fk_invoices_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `invoices`
--

LOCK TABLES `invoices` WRITE;
/*!40000 ALTER TABLE `invoices` DISABLE KEYS */;
/*!40000 ALTER TABLE `invoices` ENABLE KEYS */;
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
truncate table `membership`;
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
  `purchase_email_sent_stripe` tinyint(1) NOT NULL DEFAULT '0',
  `renew_email_sent_stripe` tinyint(1) NOT NULL DEFAULT '0',
  `purchase_email_sent_paypal` tinyint(1) NOT NULL DEFAULT '0',
  `renew_email_sent_paypal` tinyint(1) NOT NULL DEFAULT '0',
  `purchase_invoice_sent_paypal` tinyint(1) DEFAULT '0',
  `purchase_invoice_sent_stripe` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `checkout_session_id` (`checkout_session_id`),
  UNIQUE KEY `idempotency_key` (`idempotency_key`),
  KEY `idx_payments_session` (`checkout_session_id`),
  KEY `idx_payments_intent` (`provider_intent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
truncate table `payments`;
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
truncate table `persistent_logins`;
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
INSERT INTO `plan_definitions` VALUES (1,'FREE','FREE',5,100,100,'Supporto community',0,0,'0','0','0','0','0','0','2025-11-15 00:31:46'),(2,'PRO','PRO',100,50000,5000,'Supporto email',990,792,'price_1Ski4eIX50JfMIoYSFVTlRRk','price_1Sff2WIX50JfMIoYoo9jYBj3','P-95E1655116985253PNFJKFAQ','P-712880856H8572933NFKU4AI','22%','20%','2025-11-15 01:10:08'),(3,'BUSINESS','BUSINESS',500,200000,25000,'Supporto email',2990,2392,'price_1SWu7PIX50JfMIoYKsSqKdbU','price_1SWu8kIX50JfMIoYF7KjQD0b','P-5N7019469X090525ENFKU5AY',' P-7BR4749672596041SNFKU55A','22%','20%','2025-11-15 01:15:25'),(4,'ENTERPRISE','ENTERPRISE',5000,1000000,200000,'Supporto prioritario',9990,7992,'price_1SWuGuIX50JfMIoY774Nflhw','price_1SWuJbIX50JfMIoYEqqcxria','P-80V91018RA589712LNFKVA6A','P-9E712471SG4650119NFKVBJI','22%','20%','2025-11-15 01:20:30');
/*!40000 ALTER TABLE `plan_definitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `processed_webhook_event`
--

DROP TABLE IF EXISTS `processed_webhook_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `processed_webhook_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `provider` varchar(20) NOT NULL,
  `event_id` varchar(128) NOT NULL,
  `event_type` varchar(128) DEFAULT NULL,
  `received_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_event` (`provider`,`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `processed_webhook_event`
--

LOCK TABLES `processed_webhook_event` WRITE;
/*!40000 ALTER TABLE `processed_webhook_event` DISABLE KEYS */;
truncate table `processed_webhook_event`;
/*!40000 ALTER TABLE `processed_webhook_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `signing_key`
--

DROP TABLE IF EXISTS `signing_key`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `signing_key` (
  `kid` varchar(255) NOT NULL,
  `public_key_pem` longtext NOT NULL,
  `status` varchar(255) NOT NULL,
  `not_before_ts` timestamp NULL DEFAULT NULL,
  `not_after_ts` timestamp NULL DEFAULT NULL,
  `cert_pem` longtext,
  `p12_bytes` longblob,
  `p12_password_enc` longtext,
  `p12_blob` longblob,
  PRIMARY KEY (`kid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `signing_key`
--

LOCK TABLES `signing_key` WRITE;
/*!40000 ALTER TABLE `signing_key` DISABLE KEYS */;
truncate table `signing_key`;
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
  `user_vars_schema` json DEFAULT NULL,
  `sys_vars_schema` json DEFAULT NULL,
  `active` bit(1) NOT NULL DEFAULT b'0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `tenant_id` (`tenant_id`),
  KEY `idx_template_tenant_updated` (`tenant_id`,`updated_at` DESC),
  CONSTRAINT `fk_template_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `template`
--

LOCK TABLES `template` WRITE;
/*!40000 ALTER TABLE `template` DISABLE KEYS */;
INSERT INTO `template` VALUES (1,0,'TEMPLATE ATTESTATI','1.0','<!DOCTYPE html>\n<html lang=\"it\" xmlns:th=\"http://www.thymeleaf.org\">\n<head>\n  <meta charset=\"UTF-8\"></meta>\n  <title th:text=\"\'Certificato \' + ${serial}\">Certificato</title>\n  <style>\n    /* --- Pagina / palette PRO --- */\n    @page { size: A4; margin: 10mm; }           /* prima 14mm */\n    .page { background:#f8fafc; }               /* ok */\n    .frame { margin: 4mm; padding: 7mm;\n      background:#fff; border:2px solid #0a376e; border-radius:8px;\n    }\n    /* spacing piÃ¹ âtightâ e prevedibile */\n    body { line-height: 1.22; }\n\n    /* header layout */\n    .header { margin-top: 2mm; margin-bottom: 4mm; }\n    .meta { margin-top: 2mm; }\n    .meta .issuer { margin-bottom: 2mm; }\n    .meta .issued { margin-top: 2mm; }\n\n    /* riduci spazi globali un poâ eccessivi */\n    .security-band { margin-bottom: 5mm; }      /* prima 8mm */\n    .title { margin: 6mm 0 4mm 0; }             /* prima 10mm 0 8mm 0 */\n    .card { padding: 6mm; margin-bottom: 6mm; } /* prima 8mm/8mm */\n    .kv { margin: 2mm 0; }                      /* prima 3mm */\n    .divider { margin: 4mm 0; }                 /* prima 6mm */\n    .sign { margin-top: 6mm; }                  /* prima 10mm */\n    .footer { margin-top: 8mm; padding-top: 3mm; } /* prima 12mm/4mm */\n    * { box-sizing: border-box; }\n    html, body { margin:0; padding:0; color:#0f172a; font-family: Arial, Helvetica, sans-serif; }\n    /* Sostituisci le variabili con classi o valori diretti */\n    .pro { background: #f8fafc; }\n    /* --- Cornice principale --- */\n    .page { background: #f8fafc; border: 1px solid #d1d5db; border-radius: 10px; }\n    .frame { margin: 8mm; padding: 10mm; background: #ffffff; border: 2px solid #0a376e; border-radius: 8px; }\n    /* --- Banda superiore di sicurezza --- */\n    .security-band {\n      background-color: #0a376e; /* Niente gradiente, usa il colore primario */\n      color: #ffffff; padding: 3mm 6mm; border-radius: 6px; margin-bottom: 8mm;\n    }\n    .brand-title {width:220px; display:inline-block; vertical-align: middle; color: #0a376e;font-weight: 800; font-size: 18pt; letter-spacing:.3px; }\n    .serial { display:inline-block; padding:2px 6px; border:1px solid #d1d5db; background:#f3f4f6; border-radius:6px;font-family: \"Courier New\", Courier, monospace; font-size:10pt; color: #0f172a; }\n    /* --- Titolo documento --- */\n    .title { text-align:center; margin: 10mm 0 8mm 0; }\n    .title h1 { margin:0; font-size: 24pt; color: #0f172a; }\n    .subtitle { margin-top: 2mm; color: #6b7280; font-size: 11pt; }\n    .badge { display:inline-block; margin-top: 4mm; padding: 3px 10px; border-radius: 999px;background: #0a376e; color: #ffffff; font-weight: 700; font-size: 10pt; border:1px solid #163c6a; }\n    /* --- Griglie semplici --- */\n    .row { width:100%; }\n    .col { display:inline-block; vertical-align: top; width: 48.5%; }\n    .spacer { height: 2mm; }\n    /* --- Card dati --- */\n    .card { border:1px solid #d1d5db; border-radius:10px; background:#ffffff; padding:8mm; margin-bottom:8mm; }\n    .kv { margin: 3mm 0; }\n    .kv label { display:block; font-size:9pt; color: #6b7280; margin-bottom: 1mm; letter-spacing: .2px; }\n    .kv .v { font-size:12.5pt; font-weight:700; color: #0f172a; }\n    /* --- Sezione verifica --- */\n    .verify { border:1px dashed #9aa3b2; background: #f3f4f6; border-radius:10px; padding:6mm; }\n    .verify-label { font-weight: 800; margin-bottom: 2mm; letter-spacing: .2px; }\n    .verify-url { word-break: break-all; color: #082a53; font-weight: 800; font-size: 10.5pt; margin: 2mm 0 3mm; }\n    .note { color: #6b7280; font-size: 9.5pt; }\n    /* --- Firme --- */\n    .sign { margin-top: 10mm; }\n    .sigbox { display:inline-block; width: 48.5%; vertical-align: top; text-align: center; }\n    .sigline { border-top:1px solid #0f172a; margin-top:18mm; padding-top:2mm; font-size:10pt; color:#374151; }\n    .siglabel { font-size:9pt; color: #6b7280; }\n    /* --- Footer --- */\n    .footer { margin-top: 12mm; border-top:1px solid #d1d5db; padding-top:4mm; color: #6b7280; font-size: 9pt;display: table; width:100%; }\n    .foot-left, .foot-right { display: table-cell; width:50%; vertical-align: middle; }\n    /* --- Divider --- */\n    .divider { height:1px; background: #b58b00; margin: 6mm 0; }\n    /* Evita orfani/righe spezzate brutte in PDF */\n    .kv .v, .verify, .sigbox, .title { page-break-inside: avoid; }\n    .verify-url { word-break: break-word; overflow-wrap: anywhere; line-height: 1.15; }\n    .footer .foot-right span { word-break: break-word; overflow-wrap: anywhere; }\n  </style>\n</head>\n<body>\n<div class=\"page pro\">\n  <div class=\"frame\">\n    <!-- Banda superiore -->\n    <div class=\"security-band\">\n      <div class=\"left\">\n        <div class=\"security-kicker\">Documento digitale firmato e verificabile</div>\n        <div class=\"security-title serif\">Attestato di conseguimento</div>\n      </div>\n      <div class=\"right\">\n        <span class=\"chip\">Seriale</span>\n        <span class=\"serial\" th:text=\"${serial}\">ABCDEF123456</span>\n      </div>\n    </div>\n\n    <!-- Header -->\n    <div class=\"header\">\n      <div class=\"brand\">\n        <div class=\"img\">\n          <img th:src=\"${logoUrl}\" alt=\"${logoUrl}\"></img>\n        </div>\n        <span class=\"crest\"></span>\n        <span class=\"brand-title serif\" th:text=\"${tenantName != null ? tenantName : \'Azienda Demo\'}\">Azienda Demo</span>\n      </div>\n      <br></br>\n      <div class=\"meta\">\n        <div class=\"issuer\">\n          <div th:text=\"${issuerName != null ? issuerName : \'Dott. Mario Rossi\'}\">Dott. Mario Rossi</div>\n          <div th:text=\"${issuerTitle != null ? issuerTitle : \'Direttore Formazione\'}\">Direttore Formazione</div>\n        </div>\n\n        <div class=\"issued\">\n          <span>Emesso il:</span>\n          <span th:text=\"${#temporals.format(issuedAt, \'dd/MM/yyyy\')}\">01/01/2025</span>\n        </div>\n      </div>\n    </div>\n\n    <!-- Titolo -->\n    <div class=\"title\">\n      <h1 class=\"serif\">Certificato di Completamento</h1>\n      <div class=\"subtitle\">Si attesta che il/la candidato/a ha completato con profitto il seguente percorso.</div>\n      <div class=\"badge\">Attestato digitale verificabile</div>\n    </div>\n\n    <!-- Dati principali -->\n    <div class=\"card\">\n      <div class=\"row\">\n        <div class=\"col\">\n          <div class=\"kv\">\n            <label>Intestatario</label>\n            <div class=\"v\" th:text=\"${ownerName}\">Nome Cognome</div>\n          </div>\n          <div class=\"kv\">\n            <label>Email</label>\n            <div class=\"v\" th:text=\"${ownerEmail}\">nome.cognome@example.com</div>\n          </div>\n          <div class=\"kv\">\n            <label>Corso / Oggetto</label>\n            <div class=\"v\" th:text=\"${courseName}\">SPRING-BOOT</div>\n          </div>\n        </div>\n        <div class=\"col\">\n          <div class=\"kv\">\n            <label>Codice interno</label>\n            <div class=\"v\"><span class=\"chip\" th:text=\"${courseCode}\">SPRING-K987</span></div>\n          </div>\n          <div class=\"kv\">\n            <label>Ore / Esito</label>\n            <div class=\"v\">\n              <span th:text=\"${hours}\">45</span><span> ore â </span><span th:text=\"${grade}\">A</span>\n            </div>\n          </div>\n        </div>\n      </div>\n\n      <div class=\"divider\"></div>\n\n      <!-- Verifica pubblica -->\n      <div class=\"verify\">\n        <div class=\"qr\">\n          <img th:src=\"\'data:image/png;base64,\' + ${qrBase64}\" alt=\"QR Code\"></img>\n        </div>\n        <div class=\"verify-text\">\n          <div class=\"verify-label serif\">Verifica pubblica</div>\n          <div class=\"note\">Inquadra il QR oppure visita:</div>\n          <div class=\"verify-url\" th:text=\"${verifyUrl}\">https://example.org/v/ABCDE12345</div>\n          <div class=\"note\">\n            Seriale: <span class=\"serial\" th:text=\"${serial}\">ABCDEF123456</span>\n          </div>\n        </div>\n      </div>\n    </div>\n\n    <!-- Firme -->\n    <div class=\"sign\">\n      <div class=\"sigbox\">\n        <div class=\"sigline serif\" th:text=\"${issuerName}\">Dott. Mario Rossi</div>\n        <div class=\"siglabel\" th:text=\"${issuerTitle}\">Direttore Formazione</div>\n        <div class=\"img\">\n          <img th:src=\"${signatureImageUrl}\" width=\"100px\" height=\"70px\" alt=\"Signature\"></img>\n        </div>\n      </div>\n      <div class=\"sigbox\">\n        <div class=\"sigline serif\" th:text=\"${tenantName}\">Azienda Demo</div>\n        <div class=\"siglabel\">AutoritÃ  Emettente</div>\n      </div>\n    </div>\n\n    <!-- Footer -->\n    <div class=\"footer\">\n      <div class=\"foot-left\">\n        Â© <span th:text=\"${#temporals.format(#temporals.createNow(),\'yyyy\')}\">2025</span>\n        Â· <span th:text=\"${tenantName != null ? tenantName : \'Azienda Demo\'}\">Azienda Demo</span>\n      </div>\n      <div class=\"foot-right\">\n        Documento firmato digitalmente <br></br><span th:text=\"${verifyUrl}\">https://example.org/v/ABCDE12345</span>\n      </div>\n    </div>\n  </div>\n</div>\n</body>\n</html>\n','{\"grade\": {\"type\": \"string\", \"label\": \"Esito\", \"required\": false}, \"hours\": {\"type\": \"string\", \"label\": \"Ore\", \"required\": true}, \"ownerName\": {\"type\": \"string\", \"label\": \"Intestatario\", \"required\": true}, \"courseCode\": {\"type\": \"string\", \"label\": \"Codice corso\", \"required\": true}, \"courseName\": {\"type\": \"string\", \"label\": \"Nome corso\", \"required\": true}, \"ownerEmail\": {\"type\": \"string\", \"label\": \"Email\", \"required\": true}}',NULL,_binary '','2026-01-11 07:25:19','2026-01-21 05:13:18'),(3,1,'TEMPLATE ATTESTATI','1.0','<!DOCTYPE html>\n<html lang=\"it\" xmlns:th=\"http://www.thymeleaf.org\">\n<head>\n  <meta charset=\"UTF-8\"></meta>\n  <title th:text=\"\'Certificato \' + ${serial}\">Certificato</title>\n  <style>\n    /* --- Pagina / palette PRO --- */\n    @page { size: A4; margin: 10mm; }           /* prima 14mm */\n    .page { background:#f8fafc; }               /* ok */\n    .frame { margin: 4mm; padding: 7mm;\n      background:#fff; border:2px solid #0a376e; border-radius:8px;\n    }\n    /* spacing piÃ¹ âtightâ e prevedibile */\n    body { line-height: 1.22; }\n\n    /* header layout */\n    .header { margin-top: 2mm; margin-bottom: 4mm; }\n    .meta { margin-top: 2mm; }\n    .meta .issuer { margin-bottom: 2mm; }\n    .meta .issued { margin-top: 2mm; }\n\n    /* riduci spazi globali un poâ eccessivi */\n    .security-band { margin-bottom: 5mm; }      /* prima 8mm */\n    .title { margin: 6mm 0 4mm 0; }             /* prima 10mm 0 8mm 0 */\n    .card { padding: 6mm; margin-bottom: 6mm; } /* prima 8mm/8mm */\n    .kv { margin: 2mm 0; }                      /* prima 3mm */\n    .divider { margin: 4mm 0; }                 /* prima 6mm */\n    .sign { margin-top: 6mm; }                  /* prima 10mm */\n    .footer { margin-top: 8mm; padding-top: 3mm; } /* prima 12mm/4mm */\n    * { box-sizing: border-box; }\n    html, body { margin:0; padding:0; color:#0f172a; font-family: Arial, Helvetica, sans-serif; }\n    /* Sostituisci le variabili con classi o valori diretti */\n    .pro { background: #f8fafc; }\n    /* --- Cornice principale --- */\n    .page { background: #f8fafc; border: 1px solid #d1d5db; border-radius: 10px; }\n    .frame { margin: 8mm; padding: 10mm; background: #ffffff; border: 2px solid #0a376e; border-radius: 8px; }\n    /* --- Banda superiore di sicurezza --- */\n    .security-band {\n      background-color: #0a376e; /* Niente gradiente, usa il colore primario */\n      color: #ffffff; padding: 3mm 6mm; border-radius: 6px; margin-bottom: 8mm;\n    }\n    .brand-title {width:220px; display:inline-block; vertical-align: middle; color: #0a376e;font-weight: 800; font-size: 18pt; letter-spacing:.3px; }\n    .serial { display:inline-block; padding:2px 6px; border:1px solid #d1d5db; background:#f3f4f6; border-radius:6px;font-family: \"Courier New\", Courier, monospace; font-size:10pt; color: #0f172a; }\n    /* --- Titolo documento --- */\n    .title { text-align:center; margin: 10mm 0 8mm 0; }\n    .title h1 { margin:0; font-size: 24pt; color: #0f172a; }\n    .subtitle { margin-top: 2mm; color: #6b7280; font-size: 11pt; }\n    .badge { display:inline-block; margin-top: 4mm; padding: 3px 10px; border-radius: 999px;background: #0a376e; color: #ffffff; font-weight: 700; font-size: 10pt; border:1px solid #163c6a; }\n    /* --- Griglie semplici --- */\n    .row { width:100%; }\n    .col { display:inline-block; vertical-align: top; width: 48.5%; }\n    .spacer { height: 2mm; }\n    /* --- Card dati --- */\n    .card { border:1px solid #d1d5db; border-radius:10px; background:#ffffff; padding:8mm; margin-bottom:8mm; }\n    .kv { margin: 3mm 0; }\n    .kv label { display:block; font-size:9pt; color: #6b7280; margin-bottom: 1mm; letter-spacing: .2px; }\n    .kv .v { font-size:12.5pt; font-weight:700; color: #0f172a; }\n    /* --- Sezione verifica --- */\n    .verify { border:1px dashed #9aa3b2; background: #f3f4f6; border-radius:10px; padding:6mm; }\n    .verify-label { font-weight: 800; margin-bottom: 2mm; letter-spacing: .2px; }\n    .verify-url { word-break: break-all; color: #082a53; font-weight: 800; font-size: 10.5pt; margin: 2mm 0 3mm; }\n    .note { color: #6b7280; font-size: 9.5pt; }\n    /* --- Firme --- */\n    .sign { margin-top: 10mm; }\n    .sigbox { display:inline-block; width: 48.5%; vertical-align: top; text-align: center; }\n    .sigline { border-top:1px solid #0f172a; margin-top:18mm; padding-top:2mm; font-size:10pt; color:#374151; }\n    .siglabel { font-size:9pt; color: #6b7280; }\n    /* --- Footer --- */\n    .footer { margin-top: 12mm; border-top:1px solid #d1d5db; padding-top:4mm; color: #6b7280; font-size: 9pt;display: table; width:100%; }\n    .foot-left, .foot-right { display: table-cell; width:50%; vertical-align: middle; }\n    /* --- Divider --- */\n    .divider { height:1px; background: #b58b00; margin: 6mm 0; }\n    /* Evita orfani/righe spezzate brutte in PDF */\n    .kv .v, .verify, .sigbox, .title { page-break-inside: avoid; }\n    .verify-url { word-break: break-word; overflow-wrap: anywhere; line-height: 1.15; }\n    .footer .foot-right span { word-break: break-word; overflow-wrap: anywhere; }\n  </style>\n</head>\n<body>\n<div class=\"page pro\">\n  <div class=\"frame\">\n    <!-- Banda superiore -->\n    <div class=\"security-band\">\n      <div class=\"left\">\n        <div class=\"security-kicker\">Documento digitale firmato e verificabile</div>\n        <div class=\"security-title serif\">Attestato di conseguimento</div>\n      </div>\n      <div class=\"right\">\n        <span class=\"chip\">Seriale</span>\n        <span class=\"serial\" th:text=\"${serial}\">ABCDEF123456</span>\n      </div>\n    </div>\n\n    <!-- Header -->\n    <div class=\"header\">\n      <div class=\"brand\">\n        <div class=\"img\">\n          <img th:src=\"${logoUrl}\" alt=\"${logoUrl}\"></img>\n        </div>\n        <span class=\"crest\"></span>\n        <span class=\"brand-title serif\" th:text=\"${tenantName != null ? tenantName : \'Azienda Demo\'}\">Azienda Demo</span>\n      </div>\n      <br></br>\n      <div class=\"meta\">\n        <div class=\"issuer\">\n          <div th:text=\"${issuerName != null ? issuerName : \'Dott. Mario Rossi\'}\">Dott. Mario Rossi</div>\n          <div th:text=\"${issuerTitle != null ? issuerTitle : \'Direttore Formazione\'}\">Direttore Formazione</div>\n        </div>\n\n        <div class=\"issued\">\n          <span>Emesso il:</span>\n          <span th:text=\"${#temporals.format(issuedAt, \'dd/MM/yyyy\')}\">01/01/2025</span>\n        </div>\n      </div>\n    </div>\n\n    <!-- Titolo -->\n    <div class=\"title\">\n      <h1 class=\"serif\">Certificato di Completamento</h1>\n      <div class=\"subtitle\">Si attesta che il/la candidato/a ha completato con profitto il seguente percorso.</div>\n      <div class=\"badge\">Attestato digitale verificabile</div>\n    </div>\n\n    <!-- Dati principali -->\n    <div class=\"card\">\n      <div class=\"row\">\n        <div class=\"col\">\n          <div class=\"kv\">\n            <label>Intestatario</label>\n            <div class=\"v\" th:text=\"${ownerName}\">Nome Cognome</div>\n          </div>\n          <div class=\"kv\">\n            <label>Email</label>\n            <div class=\"v\" th:text=\"${ownerEmail}\">nome.cognome@example.com</div>\n          </div>\n          <div class=\"kv\">\n            <label>Corso / Oggetto</label>\n            <div class=\"v\" th:text=\"${courseName}\">SPRING-BOOT</div>\n          </div>\n        </div>\n        <div class=\"col\">\n          <div class=\"kv\">\n            <label>Codice interno</label>\n            <div class=\"v\"><span class=\"chip\" th:text=\"${courseCode}\">SPRING-K987</span></div>\n          </div>\n          <div class=\"kv\">\n            <label>Ore / Esito</label>\n            <div class=\"v\">\n              <span th:text=\"${hours}\">45</span><span> ore â </span><span th:text=\"${grade}\">A</span>\n            </div>\n          </div>\n        </div>\n      </div>\n\n      <div class=\"divider\"></div>\n\n      <!-- Verifica pubblica -->\n      <div class=\"verify\">\n        <div class=\"qr\">\n          <img th:src=\"\'data:image/png;base64,\' + ${qrBase64}\" alt=\"QR Code\"></img>\n        </div>\n        <div class=\"verify-text\">\n          <div class=\"verify-label serif\">Verifica pubblica</div>\n          <div class=\"note\">Inquadra il QR oppure visita:</div>\n          <div class=\"verify-url\" th:text=\"${verifyUrl}\">https://example.org/v/ABCDE12345</div>\n          <div class=\"note\">\n            Seriale: <span class=\"serial\" th:text=\"${serial}\">ABCDEF123456</span>\n          </div>\n        </div>\n      </div>\n    </div>\n\n    <!-- Firme -->\n    <div class=\"sign\">\n      <div class=\"sigbox\">\n        <div class=\"sigline serif\" th:text=\"${issuerName}\">Dott. Mario Rossi</div>\n        <div class=\"siglabel\" th:text=\"${issuerTitle}\">Direttore Formazione</div>\n        <div class=\"img\">\n          <img th:src=\"${signatureImageUrl}\" width=\"100px\" height=\"70px\" alt=\"Signature\"></img>\n        </div>\n      </div>\n      <div class=\"sigbox\">\n        <div class=\"sigline serif\" th:text=\"${tenantName}\">Azienda Demo</div>\n        <div class=\"siglabel\">AutoritÃ  Emettente</div>\n      </div>\n    </div>\n\n    <!-- Footer -->\n    <div class=\"footer\">\n      <div class=\"foot-left\">\n        Â© <span th:text=\"${#temporals.format(#temporals.createNow(),\'yyyy\')}\">2025</span>\n        Â· <span th:text=\"${tenantName != null ? tenantName : \'Azienda Demo\'}\">Azienda Demo</span>\n      </div>\n      <div class=\"foot-right\">\n        Documento firmato digitalmente <br></br><span th:text=\"${verifyUrl}\">https://example.org/v/ABCDE12345</span>\n      </div>\n    </div>\n  </div>\n</div>\n</body>\n</html>\n','{\"grade\": {\"type\": \"string\", \"label\": \"Esito\", \"required\": false}, \"hours\": {\"type\": \"string\", \"label\": \"Ore\", \"required\": true}, \"ownerName\": {\"type\": \"string\", \"label\": \"Intestatario\", \"required\": true}, \"courseCode\": {\"type\": \"string\", \"label\": \"Codice corso\", \"required\": true}, \"courseName\": {\"type\": \"string\", \"label\": \"Nome corso\", \"required\": true}, \"ownerEmail\": {\"type\": \"string\", \"label\": \"Email\", \"required\": true}}',NULL,_binary '','2026-01-25 13:45:19','2026-01-29 00:27:02');
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant`
--

LOCK TABLES `tenant` WRITE;
/*!40000 ALTER TABLE `tenant` DISABLE KEYS */;
truncate table `tenant`;
/*!40000 ALTER TABLE `tenant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant_profile`
--

DROP TABLE IF EXISTS `tenant_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_profile` (
  `tenant_id` bigint NOT NULL,
  `company_name` varchar(160) NOT NULL,
  `vat_number` varchar(32) NOT NULL,
  `tax_code` varchar(32) DEFAULT '22',
  `sdi_code` varchar(16) DEFAULT NULL,
  `pec_email` varchar(160) DEFAULT NULL,
  `address_line1` varchar(160) DEFAULT NULL,
  `address_line2` varchar(160) DEFAULT NULL,
  `city` varchar(80) DEFAULT NULL,
  `province` varchar(8) DEFAULT NULL,
  `postal_code` varchar(16) DEFAULT NULL,
  `country` varchar(2) NOT NULL DEFAULT 'IT',
  `support_email` varchar(160) DEFAULT NULL,
  `website_url` varchar(255) DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`tenant_id`),
  CONSTRAINT `fk_tenant_profile_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant_profile`
--

LOCK TABLES `tenant_profile` WRITE;
/*!40000 ALTER TABLE `tenant_profile` DISABLE KEYS */;
truncate table `tenant_profile`;
/*!40000 ALTER TABLE `tenant_profile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant_settings`
--

DROP TABLE IF EXISTS `tenant_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_settings` (
  `tenant_id` bigint NOT NULL,
  `email` varchar(95) NOT NULL,
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
truncate table `tenant_settings`;
/*!40000 ALTER TABLE `tenant_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant_signing_key`
--

DROP TABLE IF EXISTS `tenant_signing_key`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_signing_key` (
  `tenant_id` bigint NOT NULL,
  `kid` varchar(255) NOT NULL,
  `status` varchar(32) NOT NULL,
  `assigned_ts` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`tenant_id`),
  KEY `fk_tenant_signing_key_kid` (`kid`),
  CONSTRAINT `fk_tenant_signing_key_kid` FOREIGN KEY (`kid`) REFERENCES `signing_key` (`kid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant_signing_key`
--

LOCK TABLES `tenant_signing_key` WRITE;
/*!40000 ALTER TABLE `tenant_signing_key` DISABLE KEYS */;
truncate table `tenant_signing_key`;
/*!40000 ALTER TABLE `tenant_signing_key` ENABLE KEYS */;
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
  `pdf_storage_mb` decimal(12,3) NOT NULL DEFAULT '0.000',
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
truncate table `usage_meter`;
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
  `full_name` varchar(45) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `username_UNIQUE` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
truncate table `user`;
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
truncate table `verification_token`;
/*!40000 ALTER TABLE `verification_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'vericert'
--

--
-- Dumping routines for database 'vericert'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-29  2:15:02
