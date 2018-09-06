package hirs.persist;

/**
 * Scope of a query to search for IMAMeasurementRecords.
 */
public enum IMARecordScope {

    /**
     * Scope not limited.
     */
    NONE,

    /**
     * Scope limited to a single report.
     */
    REPORT,

    /**
     * Scope limited to a single device.
     */
    DEVICE;

}
