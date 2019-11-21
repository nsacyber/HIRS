DROP PROCEDURE IF EXISTS upgrade_schema_to_1_1_1;
DELIMITER '//'

CREATE PROCEDURE upgrade_schema_to_1_1_1()
BEGIN
IF(NOT EXISTS(SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='hirs_db' AND TABLE_NAME='Certificate' AND COLUMN_NAME='tcgCredentialMajorVersion')) THEN
ALTER TABLE Certificate ADD tcgCredentialMajorVersion int(11) DEFAULT NULL;
END IF;

IF(NOT EXISTS(SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='hirs_db' AND TABLE_NAME='Certificate' AND COLUMN_NAME='tcgCredentialMinorVersion')) THEN
ALTER TABLE Certificate ADD tcgCredentialMinorVersion int(11) DEFAULT NULL;
END IF;

IF(NOT EXISTS(SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='hirs_db' AND TABLE_NAME='Certificate' AND COLUMN_NAME='tcgCredentialRevisionLevel')) THEN
ALTER TABLE Certificate ADD tcgCredentialRevisionLevel int(11) DEFAULT NULL;
END IF;

END//
DELIMITER ';'

CALL upgrade_schema_to_1_1_1;
DROP PROCEDURE upgrade_schema_to_1_1_1;

