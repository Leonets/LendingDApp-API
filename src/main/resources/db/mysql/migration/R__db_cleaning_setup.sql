DROP EVENT IF EXISTS `DB_CLEANING_EVENT`;

DROP TABLE IF EXISTS `DB_CLEANING_EVENT_RUN_LOG`;

CREATE TABLE `DB_CLEANING_EVENT_RUN_LOG` (
  `LOG_ID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `LOG_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `STATUS` enum('SUCCEEDED','FAILED') NOT NULL,
  `START_DATE` timestamp NULL DEFAULT NULL,
  `END_DATE` timestamp NULL DEFAULT NULL,
  `REMOVED_MAO_CACHE_ENTRIES` bigint NULL DEFAULT NULL,
  `REMOVED_RETURN_LOG_ENTRIES` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`LOG_ID`),
  KEY `DB_CLEANING_EVENT_RUN_LOG_START_DATE_IDX` (`START_DATE`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `DB_CLEANING_CACHE_ENTRIES_TO_DELETE`;

CREATE TABLE `DB_CLEANING_CACHE_ENTRIES_TO_DELETE` (
  `CACHE_ID`  varchar(255) NOT NULL,
  `ORDER_ID` varchar(255),
  `LAST_UPDATE` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `MARKED_ON` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`CACHE_ID`),
  KEY `DB_CLEANING_CACHE_ENTRIES_TO_DELETE_LAST_UPDATE_IDX` (`LAST_UPDATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER ;;
CREATE EVENT `DB_CLEANING_EVENT` ON SCHEDULE EVERY 15 MINUTE STARTS CURRENT_TIMESTAMP ON COMPLETION NOT PRESERVE ENABLE DO BEGIN

	DECLARE lookback_days TINYINT UNSIGNED DEFAULT 30;

    DECLARE mao_cache_affected_rows INT UNSIGNED DEFAULT 0;

	DECLARE start_ts TIMESTAMP;

	DECLARE end_ts TIMESTAMP;

	DECLARE EXIT HANDLER FOR SQLEXCEPTION

		BEGIN

			GET DIAGNOSTICS CONDITION 1 @p1 = RETURNED_SQLSTATE, @p2 = MESSAGE_TEXT;

			INSERT INTO DB_CLEANING_EVENT_RUN_LOG(`STATUS`) VALUES ('FAILED');

		END;

    SET start_ts = CURRENT_TIMESTAMP;

    DELETE FROM DB_CLEANING_CACHE_ENTRIES_TO_DELETE;

    INSERT INTO DB_CLEANING_CACHE_ENTRIES_TO_DELETE (SELECT ID, ORDER_ID, LAST_UPDATE, NOW() FROM MAO_PROCESSING_CACHE WHERE LAST_UPDATE IS NOT NULL AND LAST_UPDATE <= (CURRENT_TIMESTAMP - INTERVAL concat('', lookback_days) DAY) LIMIT 1000);

    START TRANSACTION;

    DELETE FROM MAO_PROCESSING_CACHE WHERE ID IN (SELECT CACHE_ID FROM DB_CLEANING_CACHE_ENTRIES_TO_DELETE ORDER BY LAST_UPDATE asc);
    SELECT ROW_COUNT() INTO mao_cache_affected_rows;

    COMMIT;

    SET end_ts = CURRENT_TIMESTAMP;

    INSERT INTO DB_CLEANING_EVENT_RUN_LOG(`STATUS`, START_DATE, END_DATE, REMOVED_MAO_CACHE_ENTRIES) VALUES ('SUCCEEDED', start_ts, end_ts, mao_cache_affected_rows);
END ;;
DELIMITER ;
