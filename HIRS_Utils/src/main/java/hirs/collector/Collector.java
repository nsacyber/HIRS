package hirs.collector;

import hirs.ReportRequest;
import hirs.data.persist.Report;

/**
 * The <code>Collector</code> class represents the interface for objects that take in a
 * <code>ReportRequest</code>, collect the necessary information, and generates the appropriate
 * <code>Report</code>.
 * <p>
 * This interface should only be implemented directly by non-plugin (legacy) collectors. All future
 * Collectors should implement {@link CollectorPlugin}, which uses this interface as a base.
 */
public interface Collector {

    /**
     * Returns a Report that was collected according to the ReportRequest. The
     * <code>Collector</code> will only collect for {@link ReportRequest} types that it supports.
     *
     * @param reportRequest reportRequest specifies which reports to collect
     * @return the request Report
     * @throws CollectorException exception is thrown if unexpected error occurs during collection
     * @see #reportRequestTypeSupported()
     */
    Report collect(ReportRequest reportRequest) throws CollectorException;

    /**
     * Returns the type of {@link ReportRequest} that this <code>Collector</code> supports.
     *
     * @return the report request type supported by the collector
     */
    Class<? extends ReportRequest> reportRequestTypeSupported();

    /**
     * @return if this collector is enabled.
     */
    boolean isCollectionEnabled();
}
