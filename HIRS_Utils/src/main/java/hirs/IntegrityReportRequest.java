package hirs;

import hirs.data.persist.IntegrityReport;
import hirs.data.persist.Report;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * <code>IntegrityReportRequest</code> is used to manage the request of specific
 * report types such as TPM reports or IMA reports.
 * <code>IntegrityReportRequest</code> also specifies configuration parameters
 * that are used by clients to determine where to submit client generated
 * reports.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(namespace = "hirs.com.init")
public class IntegrityReportRequest implements ReportRequest {

    private static final Logger LOGGER = getLogger(IntegrityReportRequest.class);

    /**
     * The report request order is used to sort the list of report requests as items get added.
     * Devices run collectors in this order.  The order is defined because there is a dependency
     * between the IMA and TPM collections if PCR 10 verification is enabled (PCR 10's value must
     * not include IMA measurements beyond what the collected IMA log contains, or verification will
     * always fail.)
     */
    private static final List<Class<? extends ReportRequest>> REPORT_REQUEST_ORDER =
            Collections.unmodifiableList(Arrays.asList(
                    IntegrityReportRequest.class,
                    DeviceInfoReportRequest.class,
                    TPMReportRequest.class
            ));

    /**
     * This Comparator is simply used to order ReportRequests as they are added to an
     * IntegrityReport.
     */
    private static final Comparator<ReportRequest> REPORT_REQUEST_COLLECTION_ORDER_COMPARATOR =
            new Comparator<ReportRequest>() {
                @Override
                public int compare(final ReportRequest r1, final ReportRequest r2) {
                    int r1Ordinal = REPORT_REQUEST_ORDER.indexOf(r1.getClass());
                    int r2Ordinal = REPORT_REQUEST_ORDER.indexOf(r2.getClass());
                    if (r1Ordinal == -1) {
                        r1Ordinal = Integer.MAX_VALUE;
                    }
                    if (r2Ordinal == -1) {
                        r2Ordinal = Integer.MAX_VALUE;
                    }
                    return Integer.compare(r1Ordinal, r2Ordinal);
                }
            };

    @XmlElement
    private final List<ReportRequest> reportRequests = new ArrayList<>();

    @XmlElement
    private boolean waitForAppraisalCompletionEnabled;

    /**
     * Method adds a new ReportRequest to the list of ReportRequests.
     *
     * @param request
     *            ReportRequest to add
     */
    public final void addReportRequest(final ReportRequest request) {
        LOGGER.debug("Entering addReportRequest");
        if (request == null) {
            throw new NullPointerException("request");
        }
        reportRequests.add(request);
        Collections.sort(reportRequests, REPORT_REQUEST_COLLECTION_ORDER_COMPARATOR);
        LOGGER.debug("Added report request {}", request.getReportType());
        LOGGER.debug("Exiting addReportRequest");
    }

    /**
     * Method returns list of ReportRequests.
     *
     * @return list of reportRequests
     *
     */
    public List<ReportRequest> getReportRequest() {
        return Collections.unmodifiableList(reportRequests);
    }

    /**
     * Returns <code>IntegrityReport</code> class.
     * @return IntegrityReport class
     */
    @Override
    public final Class<? extends Report> getReportType() {
        return IntegrityReport.class;
    }

    /**
     * Sets flag indicating if the device associated with this Integrity Report should wait
     * for appraisal completion.
     * @see hirs.data.persist.DeviceGroup#setWaitForAppraisalCompletionEnabled(boolean)
     * @param waitForAppraisalCompletionEnabled true if device receiving this integrity report
     *                                          will wait for appraisal completion, false otherwise.
     */
    public void setWaitForAppraisalCompletionEnabled(
            final boolean waitForAppraisalCompletionEnabled) {
        this.waitForAppraisalCompletionEnabled = waitForAppraisalCompletionEnabled;
    }

    /**
     * Returns the flag indicating if the device associated with this Integrity Report should wait
     * for appraisal completion.
     * @see hirs.data.persist.DeviceGroup#isWaitForAppraisalCompletionEnabled()
     * @return true if device receiving this integrity report will wait
     * for appraisal completion, false otherwise.
     */
    public boolean isWaitForAppraisalCompletionEnabled() {
        return waitForAppraisalCompletionEnabled;
    }
}
