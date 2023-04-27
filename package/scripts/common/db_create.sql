CREATE DATABASE IF NOT EXISTS `hirs_db` CHARACTER SET = 'utf8mb4' COLLATE = 'utf8mb4_general_ci';
GRANT ALL ON hirs_db.* TO "hirs_db"@"localhost" IDENTIFIED BY "$HIRS_DB_PWD";
