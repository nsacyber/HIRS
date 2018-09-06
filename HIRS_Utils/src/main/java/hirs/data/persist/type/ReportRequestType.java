package hirs.data.persist.type;

/**
 * This enumeration describes the various types of {@link hirs.ReportRequest}.
 */
public enum ReportRequestType {

    /**
     * Represents that the client was invoked manually from the terminal. This corresponds to the
     * 'hirs report' command in hirs.sh
     */
    CLIENT_INITIATED_REPORT,

    /**
     * Indicates that the {@link hirs.ReportRequest} was issued as a result of a periodic
     * interval.
     */
    PERIODIC_REPORT,

    /**
     * Indicates that the {@link hirs.ReportRequest} was issues as a result of a server side
     * initiated request. This corresponds to clicking the request report button on the Portal.
     */
    ON_DEMAND_REPORT

}
