-- MariaDB dump 10.19  Distrib 10.11.4-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: trapezite
-- ------------------------------------------------------
-- Server version	10.11.4-MariaDB-1~deb12u1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `accepted_collateral`
--

DROP TABLE IF EXISTS `accepted_collateral`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `accepted_collateral` (
  `component` varchar(100) NOT NULL,
  `collateral` varchar(100) NOT NULL,
  PRIMARY KEY (`component`,`collateral`),
  KEY `collateral` (`collateral`),
  CONSTRAINT `accepted_collateral_ibfk_1` FOREIGN KEY (`component`) REFERENCES `loan_market` (`component`),
  CONSTRAINT `accepted_collateral_ibfk_2` FOREIGN KEY (`collateral`) REFERENCES `coin` (`coin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `accepted_collateral`
--

LOCK TABLES `accepted_collateral` WRITE;
/*!40000 ALTER TABLE `accepted_collateral` DISABLE KEYS */;
INSERT INTO `accepted_collateral` VALUES
('component_tdx_2_1cr93ty470kgznq48793nhgws3zp8jnxjwd5jgygrzmwkjycl872kl4','resource_tdx_2_1tk72ka8we5sq748kq2n8sz3lzqmnkjs4trwc7dx6v842jalkk00uhs');
/*!40000 ALTER TABLE `accepted_collateral` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `coin`
--

DROP TABLE IF EXISTS `coin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `coin` (
  `coin` varchar(100) NOT NULL,
  `symbol` varchar(5) NOT NULL DEFAULT '',
  `name` varchar(100) NOT NULL DEFAULT '',
  `icon_url` varchar(100) NOT NULL DEFAULT '',
  PRIMARY KEY (`coin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coin`
--

LOCK TABLES `coin` WRITE;
/*!40000 ALTER TABLE `coin` DISABLE KEYS */;
INSERT INTO `coin` VALUES
('resource_tdx_2_1tk72ka8we5sq748kq2n8sz3lzqmnkjs4trwc7dx6v842jalkk00uhs','','collateral',''),
('resource_tdx_2_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxtfd2jc','XRD','Radix','https://assets.radixdlt.com/icons/icon-xrd-32x32.png');
/*!40000 ALTER TABLE `coin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flash_loan`
--

DROP TABLE IF EXISTS `flash_loan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `flash_loan` (
  `tuser_id` bigint(20) unsigned NOT NULL,
  `coin` varchar(100) NOT NULL,
  `amount` varchar(78) NOT NULL DEFAULT '0',
  `fee_dollar_value` varchar(78) NOT NULL DEFAULT '0',
  `timestamp` bigint(20) NOT NULL,
  KEY `tuser_id` (`tuser_id`,`timestamp`),
  KEY `coin` (`coin`),
  CONSTRAINT `flash_loan_ibfk_1` FOREIGN KEY (`tuser_id`) REFERENCES `tuser` (`id`),
  CONSTRAINT `flash_loan_ibfk_2` FOREIGN KEY (`coin`) REFERENCES `pool_capital` (`coin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flash_loan`
--

LOCK TABLES `flash_loan` WRITE;
/*!40000 ALTER TABLE `flash_loan` DISABLE KEYS */;
INSERT INTO `flash_loan` VALUES
(2,'resource_tdx_2_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxtfd2jc','2','0.05',1708885302);
/*!40000 ALTER TABLE `flash_loan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loan`
--

DROP TABLE IF EXISTS `loan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `loan` (
  `component` varchar(100) NOT NULL,
  `id` bigint(20) unsigned NOT NULL,
  `tlender_id` bigint(20) unsigned NOT NULL,
  `tborrower_id` bigint(20) unsigned NOT NULL,
  `interest_rate` varchar(78) NOT NULL DEFAULT '0',
  `amount` varchar(78) NOT NULL DEFAULT '0',
  `timestamp` bigint(20) NOT NULL,
  `state` enum('NEW','ONGOING','LIQUIDATED','REFUNDED') NOT NULL DEFAULT 'NEW',
  PRIMARY KEY (`component`,`id`),
  KEY `state` (`state`),
  KEY `component` (`component`,`tlender_id`),
  KEY `component_2` (`component`,`tborrower_id`),
  CONSTRAINT `loan_ibfk_1` FOREIGN KEY (`component`, `tlender_id`) REFERENCES `tlender` (`component`, `order_id`),
  CONSTRAINT `loan_ibfk_2` FOREIGN KEY (`component`, `tborrower_id`) REFERENCES `tborrower` (`component`, `order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loan`
--

LOCK TABLES `loan` WRITE;
/*!40000 ALTER TABLE `loan` DISABLE KEYS */;
INSERT INTO `loan` VALUES
('component_tdx_2_1cr93ty470kgznq48793nhgws3zp8jnxjwd5jgygrzmwkjycl872kl4',1,2,1,'0.11','800',1708813259,'REFUNDED');
/*!40000 ALTER TABLE `loan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loan_market`
--

DROP TABLE IF EXISTS `loan_market`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `loan_market` (
  `component` varchar(100) NOT NULL,
  `coin` varchar(100) NOT NULL,
  `tlender` varchar(100) NOT NULL,
  `tborrower` varchar(100) NOT NULL,
  `allowed_tusers` enum('ALL','REGISTERED','KYC','NONE') NOT NULL DEFAULT 'ALL',
  `rate_type` enum('FIXED','VARIABLE') NOT NULL DEFAULT 'FIXED',
  `duration_type` enum('FIXED','UNLIMITED') NOT NULL DEFAULT 'FIXED',
  PRIMARY KEY (`component`),
  UNIQUE KEY `tlender` (`tlender`),
  UNIQUE KEY `tborrower` (`tborrower`),
  KEY `coin` (`coin`),
  CONSTRAINT `loan_market_ibfk_1` FOREIGN KEY (`coin`) REFERENCES `coin` (`coin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loan_market`
--

LOCK TABLES `loan_market` WRITE;
/*!40000 ALTER TABLE `loan_market` DISABLE KEYS */;
INSERT INTO `loan_market` VALUES
('component_tdx_2_1cr93ty470kgznq48793nhgws3zp8jnxjwd5jgygrzmwkjycl872kl4','resource_tdx_2_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxtfd2jc','resource_tdx_2_1nfnc4203a3p6rj7g3pfp5nussfkwrcpva0rt6jxngvfxg8vzu4ffcd','resource_tdx_2_1n2jrf9g40yrv04c858w5xf8u4dshlltd69rtwrzkx0wv4tdrnudlhh','ALL','FIXED','FIXED');
/*!40000 ALTER TABLE `loan_market` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pool_capital`
--

DROP TABLE IF EXISTS `pool_capital`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pool_capital` (
  `coin` varchar(100) NOT NULL,
  `capital` varchar(78) NOT NULL DEFAULT '0',
  `available_capital` varchar(78) NOT NULL DEFAULT '0',
  PRIMARY KEY (`coin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pool_capital`
--

LOCK TABLES `pool_capital` WRITE;
/*!40000 ALTER TABLE `pool_capital` DISABLE KEYS */;
INSERT INTO `pool_capital` VALUES
('resource_tdx_2_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxtfd2jc','3.422232876712328767','3.422232876712328767');
/*!40000 ALTER TABLE `pool_capital` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t2seller`
--

DROP TABLE IF EXISTS `t2seller`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `t2seller` (
  `id` bigint(20) unsigned NOT NULL,
  `tlender` varchar(100) NOT NULL,
  `tlender_id` bigint(20) unsigned NOT NULL,
  `price` varchar(78) NOT NULL DEFAULT '0',
  `buyer` bigint(20) unsigned DEFAULT NULL,
  `fee` varchar(78) NOT NULL DEFAULT '0',
  `timestamp` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `tlender` (`tlender`,`tlender_id`),
  KEY `buyer` (`buyer`),
  CONSTRAINT `t2seller_ibfk_1` FOREIGN KEY (`tlender`) REFERENCES `loan_market` (`tlender`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t2seller`
--

LOCK TABLES `t2seller` WRITE;
/*!40000 ALTER TABLE `t2seller` DISABLE KEYS */;
INSERT INTO `t2seller` VALUES
(1,'resource_tdx_2_1nfnc4203a3p6rj7g3pfp5nussfkwrcpva0rt6jxngvfxg8vzu4ffcd',2,'805',4,'0.003',1708814067);
/*!40000 ALTER TABLE `t2seller` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tborrower`
--

DROP TABLE IF EXISTS `tborrower`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tborrower` (
  `component` varchar(100) NOT NULL,
  `order_id` bigint(20) unsigned NOT NULL,
  `tuser_id` bigint(20) unsigned NOT NULL DEFAULT 0,
  `minimum_collateralization_ratio` varchar(78) NOT NULL DEFAULT '0',
  `collateral` varchar(100) NOT NULL,
  `collateral_amount` varchar(78) NOT NULL DEFAULT '0',
  `state` enum('OPEN','PARTIALLYFILLED','FILLED','LIQUIDATED','REFUNDED') NOT NULL DEFAULT 'OPEN',
  `kyc_tusers_only` tinyint(1) NOT NULL DEFAULT 0,
  `timestamp` bigint(20) NOT NULL,
  PRIMARY KEY (`component`,`order_id`),
  KEY `tuser_id` (`tuser_id`),
  KEY `state` (`state`),
  KEY `component` (`component`,`collateral`),
  CONSTRAINT `tborrower_ibfk_1` FOREIGN KEY (`component`, `collateral`) REFERENCES `accepted_collateral` (`component`, `collateral`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tborrower`
--

LOCK TABLES `tborrower` WRITE;
/*!40000 ALTER TABLE `tborrower` DISABLE KEYS */;
INSERT INTO `tborrower` VALUES
('component_tdx_2_1cr93ty470kgznq48793nhgws3zp8jnxjwd5jgygrzmwkjycl872kl4',1,2,'1.1','resource_tdx_2_1tk72ka8we5sq748kq2n8sz3lzqmnkjs4trwc7dx6v842jalkk00uhs','1','REFUNDED',0,1708812991);
/*!40000 ALTER TABLE `tborrower` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tlender`
--

DROP TABLE IF EXISTS `tlender`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tlender` (
  `component` varchar(100) NOT NULL,
  `order_id` bigint(20) unsigned NOT NULL,
  `tuser_id` bigint(20) unsigned NOT NULL DEFAULT 0,
  `minimum_interest_rate` varchar(78) NOT NULL DEFAULT '0',
  `expiration_time` bigint(20) DEFAULT NULL,
  `amount` varchar(78) NOT NULL DEFAULT '0',
  `kyc_tusers_only` tinyint(1) NOT NULL DEFAULT 0,
  `state` enum('OPEN','FILLED','EXPIRED') NOT NULL DEFAULT 'OPEN',
  `timestamp` bigint(20) NOT NULL,
  PRIMARY KEY (`component`,`order_id`),
  KEY `tuser_id` (`tuser_id`),
  KEY `state` (`state`,`expiration_time`),
  CONSTRAINT `tlender_ibfk_1` FOREIGN KEY (`component`) REFERENCES `loan_market` (`component`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tlender`
--

LOCK TABLES `tlender` WRITE;
/*!40000 ALTER TABLE `tlender` DISABLE KEYS */;
INSERT INTO `tlender` VALUES
('component_tdx_2_1cr93ty470kgznq48793nhgws3zp8jnxjwd5jgygrzmwkjycl872kl4',2,3,'0.1',2000000000,'800',0,'OPEN',1708813259);
/*!40000 ALTER TABLE `tlender` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trapezite_component`
--

DROP TABLE IF EXISTS `trapezite_component`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trapezite_component` (
  `component` varchar(100) NOT NULL,
  `state_version` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`component`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trapezite_component`
--

LOCK TABLES `trapezite_component` WRITE;
/*!40000 ALTER TABLE `trapezite_component` DISABLE KEYS */;
INSERT INTO `trapezite_component` VALUES
('component_tdx_2_1cpey9lfpc3jueyc32yaltvltjlage3cnl5eqk2n9f6ptnu4jvtkss5',52146513),
('component_tdx_2_1cqs0eguhjprpkm58xrf69gswu5uuz4fa0rxvg57rmys2mkgzsnx8j6',51885400),
('component_tdx_2_1cr93ty470kgznq48793nhgws3zp8jnxjwd5jgygrzmwkjycl872kl4',52146513),
('component_tdx_2_1crd2kc30qzhpvfr933dsk0stsl4cujcrn0kcrgvxrrkk7mzj5a4y4z',52118071);
/*!40000 ALTER TABLE `trapezite_component` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trpz`
--

DROP TABLE IF EXISTS `trpz`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trpz` (
  `team` varchar(78) NOT NULL DEFAULT '0',
  `governance` varchar(78) NOT NULL DEFAULT '0',
  `total` varchar(78) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trpz`
--

LOCK TABLES `trpz` WRITE;
/*!40000 ALTER TABLE `trpz` DISABLE KEYS */;
INSERT INTO `trpz` VALUES
('0.080169437814637276','0.050105898634148298','1.002117972682965961');
/*!40000 ALTER TABLE `trpz` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tuser`
--

DROP TABLE IF EXISTS `tuser`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tuser` (
  `id` bigint(20) unsigned NOT NULL,
  `address` varchar(100) DEFAULT NULL,
  `telegram` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `debt` varchar(78) NOT NULL DEFAULT '0',
  `kyc` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `address` (`address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tuser`
--

LOCK TABLES `tuser` WRITE;
/*!40000 ALTER TABLE `tuser` DISABLE KEYS */;
INSERT INTO `tuser` VALUES
(1,NULL,NULL,NULL,'0',0),
(2,'account_tdx_2_12xj24s2wukswk0m8m24zjw0np0mvvqj74xj44egz5qalaxzjw3549g',NULL,NULL,'0',0),
(3,'account_tdx_2_12xj24s2wukswk0m8m24zjw0np0mvvqj74xj44egz5qalaxzjw3549g',NULL,NULL,'0',0),
(4,'account_tdx_2_12xj24s2wukswk0m8m24zjw0np0mvvqj74xj44egz5qalaxzjw3549g',NULL,NULL,'0',0);
/*!40000 ALTER TABLE `tuser` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-02-29 10:24:13
