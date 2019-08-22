#commands here if there are schema changes in 1.1.0
DROP PROCEDURE IF EXISTS upgrade_schema_to_1_1_0;
DELIMITER '//'

CREATE PROCEDURE upgrade_schema_to_1_1_0()
BEGIN
IF(NOT EXISTS(SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='hirs_db' AND TABLE_NAME='Certificate' AND COLUMN_NAME='componentFailures')) THEN
ALTER TABLE Certificate ADD componentFailures varchar(255) DEFAULT NULL;
END IF;

END//
DELIMITER ';'

CALL upgrade_schema_to_1_1_0;
DROP PROCEDURE upgrade_schema_to_1_1_0;

