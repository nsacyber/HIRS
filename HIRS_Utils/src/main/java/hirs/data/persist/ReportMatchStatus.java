package hirs.data.persist;

/**
 * This enum represents the result of a search for a record in a baseline.
 */
public enum ReportMatchStatus {

    /**
     * Indicates the record matched exactly, file path and hash.
     */
    MATCH,

    /**
     * Indicates that at least one entry in the baseline has a matching file
     * path but none have a matching file path and hash.
     */
    MISMATCH,

    /**
     * Indicates the baseline has no entries matching the file path.
     */
    UNKNOWN

}
