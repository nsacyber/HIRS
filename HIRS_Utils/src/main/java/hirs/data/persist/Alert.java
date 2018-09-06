package hirs.data.persist;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * DTO representing an Alert.
 * <p>
 * Alerts are persisted in the HIRS server database as rows in the 'Alert' table
 * via Hibernate/JPA. Annotations on this class's properties specify the mapping
 * between database columns and the class properties.
 * <p>
 * Alerts have an XML representation, generated via JAXB annotations on the
 * class and elements. When an Alert is the root XML element of a document, it
 * is represented as an &lt;alert&gt; element.
 *
 */
@Entity
@Table(name = "Alert", indexes = { @Index(name = "archived_index", columnList = "archived_time") })
@XmlRootElement(name = "alert")
@Access(AccessType.FIELD)
public class Alert extends ArchivableEntity {

    private static final int DEFAULT_MAX_STRING_LENGTH = 255;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "details")
    private String details;

    @ManyToOne
    @JoinColumn(name = "report_id")
    private Report report;

    // policies can be quite large, so we only reference the policy's id
    private UUID policyId;

    // baselines can also be quite large, so we only reference the baseline's id
    private UUID baselineId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "AlertBaselineIds",
            joinColumns = { @JoinColumn(name = "alert_id", nullable = false) })
    private final Set<UUID> baselineIds = new HashSet<>();

    @Column(name = "source")
    @Enumerated(EnumType.STRING)
    private Source source = Source.UNSPECIFIED;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private AlertType type = AlertType.UNSPECIFIED;

    @Column(name = "expected")
    private String expected;

    @Column(name = "received")
    private String received;

    @Column(name = "severity")
    @Enumerated(EnumType.STRING)
    private Severity severity = Severity.UNSPECIFIED;

    /**
     * The 'source' of the <code>Alert</code>, which is a string enumeration
     * representing the component within the HIRS system that caused the
     * <code>Alert</code> to be generated. For example, if a record mismatch is
     * detected by the <code>IMAAppraiser</code>, the source of the
     * <code>Alert</code> will be "IMAAppraiser". In some cases the class name
     * may be used, and in other cases a more abstract name may be used to
     * provide clarity to the user, such as the <code>REPORT_PROCESSOR</code>
     * type, which can come from the <code>SOAPMessageProcessor</code>, the
     * <code>SOAPReportProcessor</code>, or the <code>HIRSAppraiser</code>.
     */
    @XmlType(name = "AlertSource")
    public enum Source {
        /**
         * The alerts generated from an unspecified source.
         */
        UNSPECIFIED,
        /**
         * Alerts generated within <code>SOAPMessageProcessor</code>,
         * <code>SOAPReportProcessor</code>, or <code>HIRSAppraiser</code> will
         * all use the same source. This makes sense right now because those
         * Alerts will all be related to <code>Report</code>s that do not match
         * the expected format.
         */
        REPORT_PROCESSOR,
        /**
         * Alerts generated within the <code>IMAAppraiser</code>.
         */
        IMA_APPRAISER,
        /**
         * Alerts generated within the <code>TPMAppraiser</code>.
         */
        TPM_APPRAISER,
        /**
         * Alerts generated within <code>OnDemandReportRequestManager</code>.
         */
        REPORT_REQUESTOR
    }



    /**
     * The 'type' of the Alert, which is the category of problem identified by
     * the 'source'.
     */
    @XmlType(name = "AlertType")
    public enum AlertType {
        /**
         * The alert type has not been specified.
         */
        UNSPECIFIED,

        /**
         * The <code>Report</code> does not contain the necessary elements or it
         * contains certain unnecessary elements.
         */
        MALFORMED_REPORT,

        /**
         * The <code>Report</code> does not contain the correct
         * <code>TPMMeasurementRecord</code>s or the PCR values are not correct.
         */
        WHITE_LIST_PCR_MISMATCH,

        /**
         * The <code>Report</code> contains a <code>TPMMeasurementRecord</code>
         * matching a TPM BlackList.
         */
        BLACK_LIST_PCR_MATCH,

        /**
         * The <code>TPMReport</code> does not contain a valid nonce.
         */
        INVALID_NONCE,

        /**
         * The <code>TPMReport</code> does not contain a valid TPM Quote (PCR Digest).
         */
        INVALID_TPM_QUOTE,

        /**
         * The <code>TPMReport</code> does not contain a valid signature.
         */
        INVALID_SIGNATURE,

        /**
         * The <code>TPMReport</code> does not contain a valid certificate.
         */
        INVALID_CERTIFICATE,

        /**
         * The <code>IMAReport</code> contains a whitelist hash mismatch.
         */
        WHITELIST_MISMATCH,

        /**
         * The <code>IMAReport</code> contains a required set hash mismatch.
         */
        REQUIRED_SET_MISMATCH,

        /**
         * The <code>Report</code> is missing a required record.
         */
        MISSING_RECORD,

        /**
         * The <code>IMAReport</code> contains an unknown filepath.
         */
        UNKNOWN_FILE,

        /**
         * The client's <code>ReportRequest</code> query messages missing.
         */
        REPORT_REQUESTS_MISSING,

        /**
         * Client periodic <code>IntegrityReport</code> missing.
         */
        PERIODIC_REPORT_MISSING,

        /**
         * On-demand <code>IntegrityReport</code> missing.
         */
        ON_DEMAND_REPORT_MISSING,

        /**
         * The client sent a report that indicates IMA was not enabled correctly.
         */
        IMA_MISCONFIGURED,

        /**
         * PCR mismatches and device info changes indicated a kernel update.
         */
        KERNEL_UPDATE_DETECTED,

        /**
         * The <code>Report</code> does not contain the correct
         * <code>TPMMeasurementRecord</code>s associated with IMA measurements.
         */
        IMA_PCR_MISMATCH,

        /**
         * Indicates an IMA measurement had a path which matched an entry in a blacklist baseline.
         */
        IMA_BLACKLIST_PATH_MATCH,

        /**
         * Indicates an IMA measurement had a hash which matched an entry in a blacklist baseline.
         */
        IMA_BLACKLIST_HASH_MATCH,

        /**
         * Indicates an IMA measurement had both a path and hash which matched an entry in a
         * blacklist baseline.
         */
        IMA_BLACKLIST_PATH_AND_HASH_MATCH,

        /**
         * Indicates an IMA measurement had a path that matched an entry in a blacklist baseline,
         * and also had a hash that matched another entry in the same (or another) baseline.
         */
        IMA_BLACKLIST_MIXED_MATCH
    }

    /**
     * The 'severity' of the <code>Alert</code>, which is a string enumeration
     * representing the predicted importance of the problem identified.
     *
     * A constructor with the enum is used to set a criticality number for each severity level.
     * Severity levels can be compared against each other by using the getCriticality method.
     *
     */
    @XmlType(name = "AlertSeverity")
    public enum Severity {

        /**
         * Used for situations where Severity remains to be implemented or the
         * exact level has not been determined for a specific use case.
         */
        UNSPECIFIED(5),
        /**
         * Equivalent to "Ignore" or "Quiet". This is not used for general logging,
         * but for Alert level messages that, in specific cases, are not applicable
         * or can be or need to be ignored.
         */
        INFO(10),
        /**
         * Applies to a non-system critical file or condition.
         */
        LOW(15),
        /**
         *  Involves a stable or system-critical file or a stable PCR value.
         */
        HIGH(25),
        /**
         * Equivalent to "Fatal".  Involves Alerts so clearly indicative of malicious
         * intent that an automated response, such as network disconnection, is warranted.
         */
        SEVERE(30);

        /**
         * Criticality number assigned to a severity level.
         */
        private int criticality;

        /**
         * Constructor used to set the criticality level.
         *
         * @param c criticality level
         */
        Severity(final int c) {
            criticality = c;
        }

        /**
         * Return criticality level assigned to severity level.
         *
         * @return criticality level
         */
        int getCriticality() {
            return criticality;
        }
    }

    /**
     * Creates a new <code>Alert</code> with the message details. The details
     * may not be null.
     *
     * @param details details
     */
    public Alert(final String details) {
        if (details == null) {
            throw new NullPointerException("details");
        }
        setDetails(details);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected Alert() {
        super();
    }

    /**
     * Returns the name of the <code>Device</code> associated with this
     * <code>Alert</code>. This will likely be the <code>Device</code> that
     * submitted the <code>Report</code> or is receiving the
     * <code>ReportRequest</code>.
     *
     * @return device name string
     */
    @XmlElement(name = "device_name")
    public final String getDeviceName() {
        return deviceName;
    }

    /**
     * Sets the name of the <code>Device</code> associated with this
     * <code>Alert</code>. This will likely be the <code>Device</code> that
     * submitted the <code>Report</code> or is receiving the
     * <code>ReportRequest</code>.
     *
     * @param deviceName the device name string
     */
    public final void setDeviceName(final String deviceName) {
        this.deviceName = deviceName;
    }

    /**
     * Returns a user-readable description of the details of the alert. The
     * exact contents will be source-dependent.
     *
     * @return details
     */
    @XmlElement(name = "details")
    public final String getDetails() {
        return details;
    }

    /**
     * Sets a user-readable description of the details of the alert. The exact
     * contents will be source-dependent.
     *
     * @param details
     *            details
     */
    public final void setDetails(final String details) {
        this.details = truncate(details, DEFAULT_MAX_STRING_LENGTH);
    }

    /**
     * Returns the highest-level <code>Report</code> associated with this
     * <code>Alert</code>. For example, if a <code>TPMAppraiser</code> throws an
     * <code>Alert</code> based on a <code>TPMReport</code> contained within
     * an <code>IntegrityReport</code>, the <code>IntegrityReport</code> is
     * referenced in this field.
     *
     * @return report highest level <code>Report</code>
     */
    @XmlElement(name = "report")
    public final Report getReport() {
        return report;
    }

    /**
     * Sets the highest-level <code>Report</code> associated with this
     * <code>Alert</code>. For example, if a <code>TPMAppraiser</code> throws an
     * <code>Alert</code> based on a <code>TPMReport</code> contained within
     * an <code>IntegrityReport</code>, the <code>IntegrityReport</code> is
     * referenced in this field.
     *
     * @param report the <code>Report</code> to set
     */
    public final void setReport(
            final Report report) {
        this.report = report;
    }

    /**
     * Sets the id of the <code>Policy</code> that was in use, if any, when the
     * <code>Alert</code> was generated. This will occur during the appraisal
     * process. The <code>Appraiser</code> validates the integrity of a client
     * platform using a <code>Policy</code> instance. If the appraisal fails
     * then an <code>Alert</code> is generated and the id of the <code>Policy</code> used
     * during the appraisal is set here.
     *
     * @param policyId - id of the <code>Policy</code> associated with this <code>Alert</code>
     */
    public final void setPolicyId(final UUID policyId) {
        this.policyId = policyId;
    }

    /**
     * Returns the id of the <code>Policy</code> that was in use, if any, when the
     * <code>Alert</code> was generated. This will occur during the appraisal
     * process. The <code>Appraiser</code> validates the integrity of a client
     * platform using a <code>Policy</code> instance.
     *
     * @return the id of the <code>Policy</code> associated with this <code>Alert</code>
     */
    public final UUID getPolicyId() {
        return policyId;
    }

    /**
     * Returns the IDs of the <code>Baseline</code>s that were in use, if any, when the
     * <code>Alert</code> was generated.
     *
     * @return UUID of the <code>Baseline</code>s associated with this <code>Alert</code>
     */
    @XmlElement(name = "baselineIds")
    public final Set<UUID> getBaselineIds() {
        return Collections.unmodifiableSet(baselineIds);
    }

    /**
     * Returns the source of this <code>Alert</code>.
     *
     * @return source of this <code>Alert</code>
     * @see Source
     */
    @XmlAttribute(name = "source")
    public final Source getSource() {
        return source;
    }

    /**
     * Sets the source of this <code>Alert</code>.
     *
     * @param source of this <code>Alert</code>
     */
    public final void setSource(final Source source) {
        this.source = source;
    }

    /**
     * Returns the <code>AlertType</code> of this <code>Alert</code>.
     *
     * @return type of this <code>Alert</code>
     * @see AlertType
     */
    @XmlAttribute(name = "type")
    public final AlertType getType() {
        return type;
    }

    /**
     * Sets the <code>AlertType</code> of this <code>Alert</code>.
     *
     * @param type of this <code>Alert</code>
     */
    public final void setType(final AlertType type) {
        this.type = type;
    }

    /**
     * Returns a <code>String</code> representation of the expected value, which
     * depends on the <code>AlertType</code> and <code>Source</code>.
     *
     * @return expected string
     */
    public final String getExpected() {
        return expected;
    }

    /**
     * Returns a <code>String</code> representation of the received value, which
     * depends on the <code>AlertType</code> and <code>Source</code>.
     *
     * @return received string
     */
    public final String getReceived() {
        return received;
    }

    /**
     * Sets the <code>String</code> representation of the expected content in
     * the relevant section of the <code>Report</code> and the
     * <code>String</code> representation of the content actually received in
     * the <code>Report</code>.
     *
     * @param expected the expected string
     * @param received the received string
     */
    public final void setExpectedAndReceived(final String expected,
            final String received) {
        if (expected == null && received == null) {
            throw new NullPointerException("expected and received cannot both "
                    + "be null");
        }
        this.expected = truncate(expected, DEFAULT_MAX_STRING_LENGTH);
        this.received = truncate(received, DEFAULT_MAX_STRING_LENGTH);
    }

    /**
     * Sets the id of baselines associated with the alert as well as the severity of the
     * <code>Baseline</code> that was in use, if any, when the <code>Alert</code> was generated.
     * Should only be used when initially generating an <code>Alert</code>.
     *
     * @param baselines - a collection of <code>Baseline</code>s related to this alert
     */
    public final void setBaselineIdsAndSeverity(final Set<Baseline> baselines) {
        if (baselines != null) {
            for (Baseline baseline : baselines) {
                if (baseline != null) {
                    this.baselineIds.add(baseline.getId());

                    /**
                     * This is a temporary solution to resolve any failures in
                     * live code or unit tests.  BaselineId is used to count the number
                     * of alerts associated with a baseline.  The <code>AlertManager</code>
                     * class uses baselineId for this count.
                     *
                     */
                    this.baselineId = baseline.getId();

                    // only overwrite severity if the new one is non-null
                    if (baseline.getSeverity() != null) {
                        // Assign the most critical severity level of the collection of baselines to
                        // the alert
                        this.severity = getPrioritizedSeverityLevel(baseline.getSeverity());
                    }
                }
            }
        }
    }

    /**
     * Set the severity of the alert regardless of baseline.
     * @param severity Alert.Severity.
     */
    public final void setSeverity(final Alert.Severity severity) {
        // only overwrite severity if the new one is non-null
        if (severity != null) {
            this.severity = severity;
        }
    }

    /**
     * Remove this alert's reference to a baseline, but keep the <code>Severity</code> the same.
     */
    public final void clearBaseline() {
        this.baselineId = null;
    }

    /**
     * Remove this alert's reference to baselines, but keep the <code>Severity</code> the same.
     */
    public final void clearBaselines() {
        this.baselineIds.clear();
    }

    /**
     * Returns the <code>Severity</code> of this <code>Alert</code>.
     *
     * @return severity of this <code>Alert</code>
     * @see Severity
     */
    @XmlAttribute(name = "severity")
    public final Severity getSeverity() {
        return severity;
    }

    @Override
    public final String toString() {
        return this.details;
    }

    private String truncate(final String s, final int maxlen) {
        if (s == null || s.length() <= maxlen) {
            return s;
        }
        return s.substring(0, maxlen);
    }

    /**
     * Method determines the severity level to associate with an <code>Alert</code>.  Only one
     * severity level can be assigned to an alert, so this method is used to determine which
     * severity is most critical.  Criticality is based on the following order:
     *
     * <ul>
     *     <li>SEVERE</li>
     *     <li>HIGH</li>
     *     <li>LOW</li>
     *     <li>INFO</li>
     *     <li>UNSPECIFIED</li>
     * </ul>
     *
     * @param checkSeverity - severity to compare against current alert severity level
     * @return prioritized severity level based on criticality
     *
     */
    private Alert.Severity getPrioritizedSeverityLevel(final Alert.Severity checkSeverity) {
        Alert.Severity severityLevel = this.severity;
        if (severityLevel.getCriticality() < checkSeverity.getCriticality()) {
            severityLevel = checkSeverity;
        }

        return severityLevel;
    }
}
