DROP PROCEDURE IF EXISTS upgrade_schema_to_1_0_4;
DELIMITER '//'

CREATE PROCEDURE upgrade_schema_to_1_0_4()
BEGIN
IF(NOT EXISTS(SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='hirs_db' AND TABLE_NAME='Certificate' AND COLUMN_NAME='isDeltaChain')) THEN
ALTER TABLE Certificate ADD isDeltaChain bit(1) DEFAULT NULL;
END IF;

IF(NOT EXISTS(SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='hirs_db' AND TABLE_NAME='Certificate' AND COLUMN_NAME='platformBase')) THEN
ALTER TABLE Certificate ADD platformBase bit(1) DEFAULT NULL;
END IF;

IF(NOT EXISTS(SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='hirs_db' AND TABLE_NAME='Certificate' AND COLUMN_NAME='platformChainType')) THEN
ALTER TABLE Certificate ADD platformChainType varchar(255) DEFAULT NULL;
END IF;

IF(NOT EXISTS(SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='hirs_db' AND TABLE_NAME='SupplyChainValidationSummary' AND COLUMN_NAME='message')) THEN
ALTER TABLE SupplyChainValidationSummary ADD message longtext;
END IF;

IF(NOT EXISTS(SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='hirs_db' AND TABLE_NAME='TPMReport' AND COLUMN_NAME='rawQuote')) THEN
ALTER TABLE TPMReport ADD rawQuote blob;
END IF;
END//
DELIMITER ';'

CALL upgrade_schema_to_1_0_4;
DROP PROCEDURE upgrade_schema_to_1_0_4;

