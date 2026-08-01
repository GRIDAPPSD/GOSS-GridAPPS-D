-- GridAPPS-D MySQL Initialization Script
-- Creates the database, user, and required tables for logging and test results

CREATE DATABASE IF NOT EXISTS gridappsd;

CREATE USER IF NOT EXISTS 'gridappsd'@'%' IDENTIFIED BY 'gridappsd1234';
GRANT ALL PRIVILEGES ON gridappsd.* TO 'gridappsd'@'%';
FLUSH PRIVILEGES;

USE gridappsd;

-- Table for platform and simulation logs
DROP TABLE IF EXISTS `log`;
CREATE TABLE `log` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `source` varchar(255) NOT NULL,
  `process_id` varchar(255) DEFAULT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `log_message` TEXT NOT NULL,
  `log_level` varchar(20) NOT NULL,
  `process_status` varchar(20) NOT NULL,
  `username` varchar(20) NOT NULL,
  `process_type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8
  COMMENT='Log messages and status from various processes in GridAPPS-D platform.';

-- Table for test expected results comparison
DROP TABLE IF EXISTS `expected_results`;
CREATE TABLE `expected_results` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `app_id` varchar(255) DEFAULT NULL,
  `test_id` varchar(255) DEFAULT NULL,
  `process_id_one` varchar(255) DEFAULT NULL,
  `process_id_two` varchar(255) DEFAULT NULL,
  `index_one` LONG DEFAULT NULL,
  `index_two` LONG DEFAULT NULL,
  `mrid` varchar(255) NOT NULL,
  `property` varchar(255) NOT NULL,
  `expected` varchar(255) NOT NULL,
  `actual` varchar(255) NOT NULL,
  `difference_direction` varchar(255) NOT NULL,
  `difference_mrid` varchar(255) NOT NULL,
  `match_flag` BOOL NOT NULL,
  `simulation_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8
  COMMENT='Expected vs actual results for test validation.';
