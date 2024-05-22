SELECT ac.collateral, c.symbol, c.name, c.icon_url
            FROM accepted_collateral ac
            JOIN coin c ON ac.collateral = c.coin
            WHERE ac.component = 'component_tdx_2_1cr93ty470kgznq48793nhgws3zp8jnxjwd5jgygrzmwkjycl872kl4'



CREATE TABLE `coin` (
  `coin` varchar(100) NOT NULL,
  `symbol` varchar(5) NOT NULL DEFAULT '',
  `name` varchar(100) NOT NULL DEFAULT '',
  `icon_url` varchar(100) NOT NULL DEFAULT '',
  PRIMARY KEY (`coin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci


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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci



--
-- Table structure for table `accepted_collateral`
--

CREATE TABLE `accepted_collateral` (
  `component` varchar(100) NOT NULL,
  `collateral` varchar(100) NOT NULL,
  PRIMARY KEY (`component`,`collateral`),
  KEY `collateral` (`collateral`),
  CONSTRAINT `accepted_collateral_ibfk_1` FOREIGN KEY (`component`) REFERENCES `loan_market` (`component`),
  CONSTRAINT `accepted_collateral_ibfk_2` FOREIGN KEY (`collateral`) REFERENCES `coin` (`coin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


INSERT INTO `coin` VALUES
('resource_tdx_2_1tk72ka8we5sq748kq2n8sz3lzqmnkjs4trwc7dx6v842jalkk00uhs','','collateral',''),
('resource_tdx_2_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxtfd2jc','XRD','Radix','https://assets.radixdlt.com/icons/icon-xrd-32x32.png');

INSERT INTO `loan_market` VALUES
('component_tdx_2_1cr93ty470kgznq48793nhgws3zp8jnxjwd5jgygrzmwkjycl872kl4','resource_tdx_2_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxtfd2jc','resource_tdx_2_1nfnc4203a3p6rj7g3pfp5nussfkwrcpva0rt6jxngvfxg8vzu4ffcd','resource_tdx_2_1n2jrf9g40yrv04c858w5xf8u4dshlltd69rtwrzkx0wv4tdrnudlhh','ALL','FIXED','FIXED');

--
-- Dumping data for table `accepted_collateral`
--


/*!40000 ALTER TABLE `accepted_collateral` DISABLE KEYS */;
INSERT INTO `accepted_collateral` VALUES
('component_tdx_2_1cr93ty470kgznq48793nhgws3zp8jnxjwd5jgygrzmwkjycl872kl4','resource_tdx_2_1tk72ka8we5sq748kq2n8sz3lzqmnkjs4trwc7dx6v842jalkk00uhs');



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


CREATE TABLE `pool_capital` (
  `coin` varchar(100) NOT NULL,
  `capital` varchar(78) NOT NULL DEFAULT '0',
  `available_capital` varchar(78) NOT NULL DEFAULT '0',
  PRIMARY KEY (`coin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


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




CREATE TABLE `trapezite_component` (
  `component` varchar(100) NOT NULL,
  `state_version` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`component`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



CREATE TABLE `trpz` (
  `team` varchar(78) NOT NULL DEFAULT '0',
  `governance` varchar(78) NOT NULL DEFAULT '0',
  `total` varchar(78) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



INSERT INTO `pool_capital` VALUES
('resource_tdx_2_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxtfd2jc','3.422232876712328767','3.422232876712328767');


INSERT INTO `t2seller` VALUES
(1,'resource_tdx_2_1nfnc4203a3p6rj7g3pfp5nussfkwrcpva0rt6jxngvfxg8vzu4ffcd',2,'805',4,'0.003',1708814067);

INSERT INTO `tborrower` VALUES
('component_tdx_2_1cr93ty470kgznq48793nhgws3zp8jnxjwd5jgygrzmwkjycl872kl4',1,2,'1.1','resource_tdx_2_1tk72ka8we5sq748kq2n8sz3lzqmnkjs4trwc7dx6v842jalkk00uhs','1','REFUNDED',0,1708812991);

INSERT INTO `tlender` VALUES
('component_tdx_2_1cr93ty470kgznq48793nhgws3zp8jnxjwd5jgygrzmwkjycl872kl4',2,3,'0.1',2000000000,'800',0,'OPEN',1708813259);

INSERT INTO `trapezite_component` VALUES
('component_tdx_2_1cpey9lfpc3jueyc32yaltvltjlage3cnl5eqk2n9f6ptnu4jvtkss5',52146513),
('component_tdx_2_1cqs0eguhjprpkm58xrf69gswu5uuz4fa0rxvg57rmys2mkgzsnx8j6',51885400),
('component_tdx_2_1cr93ty470kgznq48793nhgws3zp8jnxjwd5jgygrzmwkjycl872kl4',52146513),
('component_tdx_2_1crd2kc30qzhpvfr933dsk0stsl4cujcrn0kcrgvxrrkk7mzj5a4y4z',52118071);



INSERT INTO `trpz` VALUES
('0.080169437814637276','0.050105898634148298','1.002117972682965961');


INSERT INTO `tuser` VALUES
(1,NULL,NULL,NULL,'0',0),
(2,'account_tdx_2_12xj24s2wukswk0m8m24zjw0np0mvvqj74xj44egz5qalaxzjw3549g',NULL,NULL,'0',0),
(3,'account_tdx_2_12xj24s2wukswk0m8m24zjw0np0mvvqj74xj44egz5qalaxzjw3549g',NULL,NULL,'0',0),
(4,'account_tdx_2_12xj24s2wukswk0m8m24zjw0np0mvvqj74xj44egz5qalaxzjw3549g',NULL,NULL,'0',0);


INSERT INTO `flash_loan` VALUES
(2,'resource_tdx_2_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxtfd2jc','2','0.05',1708885302);


INSERT INTO `loan` VALUES
('component_tdx_2_1cr93ty470kgznq48793nhgws3zp8jnxjwd5jgygrzmwkjycl872kl4',1,2,1,'0.11','800',1708813259,'REFUNDED');