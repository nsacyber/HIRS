package hirs.data.persist;

import com.fasterxml.jackson.annotation.JsonProperty;
import hirs.appraiser.Appraiser;
import hirs.appraiser.HIRSAppraiser;
import org.springframework.util.CollectionUtils;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * DTO representing a ReportSummary. Consolidates <code>Report</code>'s corresponding client,
 * report type, and timestamp. Includes a List of <code>AppraisalResult</code> objects, each
 * representing the appraisal result of a different <code>Appraiser</code>.
 */
@Entity
@Table(name = "ReportSummary")
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class ReportSummary implements Serializable {

    /**
     * The unique ID of the ReportSummary. The ID is randomly generated when the
     * summary is stored in the database.
     */
    @Id
    @Column(name = "report_summary_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    @XmlElement
    private final Long id = null;

    /**
     * Gets the id of the ReportSummary.
     *
     * @return long
     */
    public final Long getId() {
        return id;
    }

    /**
     * The hostname of the Client DTO associated with this ReportSummary.
     */
    @Column(name = "hostname")
    @XmlElement
    private String clientHostname;

    /**
     * Gets the hostname of the client for the Report in the summary.
     *
     * @return hostname
     */
    public final String getClientHostname() {
        return clientHostname;
    }

    /**
     * Sets the hostname of the client for the Report in the summary.
     *
     * @param clientHostname
     *            String hostname of client
     */
    public final void setClientHostname(final String clientHostname) {
        this.clientHostname = clientHostname;
    }

    /**
     * Time stamp for the report.
     */
    @Column(name = "timestamp")
    private Date timestamp;

    /**
     * Gets the date of the Report in the summary.
     *
     * @return TimeStamp
     */
    @XmlElement
    public final Date getTimestamp() {
        return new Date(timestamp.getTime());
    }

    /**
     * Sets the time stamp for the summary.
     *
     * @param date
     *            date for the summary
     */
    public final void setTimestamp(final Date date) {
        this.timestamp = new Date(date.getTime());
    }

    /**
     * Appraisal result object.
     */
    @XmlElement
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               mappedBy = "reportSummary", targetEntity = AppraisalResult.class)
    private Set<AppraisalResult> appraisalResults;

    /**
     * Gets the List of appraisal results, one from each <code>Appraiser</code>. An empty List
     * indicates that no Appraisers have yet completed.
     *
     * @return Set&lt;AppraisalResult&gt; appraisalResults
     */
    public final Set<AppraisalResult> getAppraisalResults() {
        return Collections.unmodifiableSet(appraisalResults);
    }

    /**
     * Sets the appraisal result.
     *
     * @param appraisalResults
     *          List of <code>AppraisalResult</code> objects
     */
    public final void setAppraisalResults(final Set<AppraisalResult> appraisalResults) {
        this.appraisalResults = appraisalResults;
        for (AppraisalResult result : appraisalResults) {
            result.setReportSummary(this);
        }
        updateHirsAppraisalResult();
    }

    /**
     * Overall result of all appraisals.
     */
    @XmlElement
    @JsonProperty("hirsAppraisalResult")
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, targetEntity =
            AppraisalResult.class)
    private AppraisalResult hirsAppraisalResult;

    /**
     * Gets the HIRS appraisal result which stores the result of the top level appraisal.
     *
     * @return AppraisalResult hirsAppraisalResult
     */
    public final AppraisalResult getHirsAppraisalResult() {
        return hirsAppraisalResult;
    }

    /**
     * Indicates whether appraisal succeeded (true) or failed (false).
     *
     */
    public final void updateHirsAppraisalResult() {
        boolean result = true;
        int numAppraisalFailures = 0;
        int numAppraisalErrors = 0;
        hirsAppraisalResult = new AppraisalResult(HIRSAppraiser.class, AppraisalStatus.Status.ERROR,
                "Error appraising report");
        if (!CollectionUtils.isEmpty(this.appraisalResults)) {
            for (AppraisalResult appraisalResult : this.appraisalResults) {
                if (appraisalResult.getAppraisalStatus() == AppraisalStatus.Status.FAIL) {
                    numAppraisalFailures++;
                } else if (appraisalResult.getAppraisalStatus() == AppraisalStatus.Status.ERROR) {
                    numAppraisalErrors++;
                }
            }
        }

        if (numAppraisalFailures > 0) {
            hirsAppraisalResult.setAppraisalStatus(AppraisalStatus.Status.FAIL);
            hirsAppraisalResult.setAppraisalResultMessage(numAppraisalFailures + " of "
                    + this.appraisalResults.size() + " appraisals failed");
            return;
        } else if (numAppraisalErrors > 0) {
            hirsAppraisalResult.setAppraisalStatus(AppraisalStatus.Status.ERROR);
            hirsAppraisalResult.setAppraisalResultMessage(numAppraisalErrors + " of "
                    + this.appraisalResults.size() + " appraisals generated an error. See"
                    + " HIRS_Appraiser.log for details");
            return;
        } else {
            hirsAppraisalResult.setAppraisalStatus(AppraisalStatus.Status.PASS);
            hirsAppraisalResult.setAppraisalResultMessage("All appraisals passed ("
                    + this.appraisalResults.size() + " of " + this.appraisalResults.size() + ")");
        }
    }

    /**
     * Get the <code>AppraisalResult</code> corresponding to the indicated <code>Appraiser</code>.
     *
     * @param appraiser
     *          <code>Appraiser</code> for which to retrieve the <code>AppraisalResult</code>
     * @return AppraisalResult appraisalResult
     */
    public final AppraisalResult getAppraisalResult(final Class<? extends Appraiser> appraiser) {
        AppraisalResult result = null;
        for (AppraisalResult appraisalResult : this.appraisalResults) {
            if (appraisalResult.getAppraiser().equals(appraiser)) {
                return appraisalResult;
            }
        }
        return result;
    }

    /**
     * Report type.
     */
    @XmlElement
    @Column(name = "report_type")
    private String reportType;

    /**
     * Gets the report type.
     *
     * @return report type
     */
    public final String getReportType() {
        return reportType;
    }

    /**
     * Sets the report type.
     *
     * @param reportType
     *            String representing the report type
     */
    public final void setReportType(final String reportType) {
        this.reportType = reportType;
    }

    /**
     * Report associated with this summary.
     */
    @OneToOne(targetEntity = Report.class)
    @JoinColumn(name = "report_id")
    private Report report;

    /**
     * Gets the report.
     *
     * @return report
     */
    public final Report getReport() {
        return report;
    }

    /**
     * Sets the report for this summary.
     *
     * @param report
     *            associated report
     */
    public final void setReport(final Report report) {
        this.report = report;
    }

}
