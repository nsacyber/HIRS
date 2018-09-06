package hirs.utils;

import org.hibernate.dialect.MySQL5InnoDBDialect;

/**
 * This class is a shim on top of the standard MySQL5InnoDBDialect to set the row format
 * for 'CREATE TABLE' statements to 'dynamic'.
 *
 * Background: as of MySQL/MariaDB 5.5 the maximum data length of index columns is 767 bytes.
 * For tables that have indexes spanning a few columns this is an easy limit to hit.
 * Configuration options can be used to enable server-side settings for handling large
 * indexes, and if tables are created with a row format of DYNAMIC or COMPRESSED,
 * the length increases to 3072 bytes.
 *
 * If Hibernate is configured to use this dialect, tables will be created
 * with a specified row format instead of the database's default.
 *
 * from: https://stackoverflow.com/questions/28632984/
 * getting-jpa-to-generate-tables-with-specified-row-format/28635892#28635892
 *
 * also see:
 * https://dev.mysql.com/doc/refman/5.5/en/innodb-restrictions.html
 * https://dev.mysql.com/doc/refman/5.7/en/innodb-row-format-specification.html
 * https://dev.mysql.com/doc/refman/5.7/en/innodb-row-format-dynamic.html
 * https://dev.mysql.com/doc/refman/5.5/en/innodb-restrictions.html
 */
public class MySqlUtf8CompatibleDialect extends MySQL5InnoDBDialect {
    /**
     * Returns the table 'type' string, which specifies the storage engine (via the
     * super call) and appends the desired row format.
     *
     * @return the table type string specifying InnoDB and dynamic row format
     */
    public String getTableTypeString() {
        return String.format("%s %s", super.getTableTypeString(), " ROW_FORMAT=DYNAMIC");
    }
}
