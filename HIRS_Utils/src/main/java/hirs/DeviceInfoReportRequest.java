package hirs;

import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.Report;

/**
 * A <code>DeviceInfoReportRequest</code> specifies that the information about a
 * device should be included in the report returned from the client.
 */
public class DeviceInfoReportRequest implements ReportRequest {

    /**
     * Default constructor.
     */
    public DeviceInfoReportRequest() {
        /* do nothing */
    }

    @Override
    public final Class<? extends Report> getReportType() {
        return DeviceInfoReport.class;
    }

}
