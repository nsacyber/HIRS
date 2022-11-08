package hirs.data.persist;

import hirs.data.persist.enums.AlertSeverity;
import hirs.data.persist.enums.AlertSource;
import hirs.data.persist.enums.AlertType;

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

    @Column(name = "display_title", nullable = true)
    private String displayTitle;

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
    private AlertSource source = AlertSource.UNSPECIFIED;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private AlertType type = AlertType.UNSPECIFIED;

    @Column(name = "expected")
    private String expected;

    @Column(name = "received")
    private String received;

    @Column(name = "severity")
    @Enumerated(EnumType.STRING)
    private AlertSeverity severity = AlertSeverity.UNSPECIFIED;

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
     * Get the display title associated with this Alert.
     *
     * @return the display title if one has been set, and null otherwise
     */
    public String getDisplayTitle() {
        return displayTitle;
    }

    /**
     * Set the display title for this alert.
     *
     * @param displayTitle the desired display title (may be null)
     */
    public void setDisplayTitle(final String displayTitle) {
        this.displayTitle = displayTitle;
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
     * Sets the id of the <code>baselineId</code> that was in use, if any, when the
     * <code>Alert</code> was generated. This will occur during the appraisal
     * process. The <code>Appraiser</code> validates the integrity of a client
     * platform using a <code>baselineId</code> instance. If the appraisal fails
     * then an <code>Alert</code> is generated and the id of the <code>Policy</code> used
     * during the appraisal is set here.
     *
     * @param baselineId - id of the <code>baselineId</code> associated with this <code>Alert</code>
     */
    public final void setBaselineId(final UUID baselineId) {
        this.baselineId = baselineId;
    }

    /**
     * Returns the id of the <code>baselineId</code> that was in use, if any, when the
     * <code>Alert</code> was generated. This will occur during the appraisal
     * process. The <code>Appraiser</code> validates the integrity of a client
     * platform using a <code>Policy</code> instance.
     *
     * @return the id of the <code>baselineId</code> associated with this <code>Alert</code>
     */
    public final UUID getBaselineId() {
        return baselineId;
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
     *
     */
    @XmlAttribute(name = "source")
    public final AlertSource getSource() {
        return source;
    }

    /**
     * Sets the source of this <code>Alert</code>.
     *
     * @param source of this <code>Alert</code>
     */
    public final void setSource(final AlertSource source) {
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
     * Set the severity of the alert regardless of baseline.
     * @param severity Alert.Severity.
     */
    public final void setSeverity(final AlertSeverity severity) {
        // only overwrite severity if the new one is non-null
        if (severity != null) {
            this.severity = severity;
        }
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
     *
     */
    @XmlAttribute(name = "severity")
    public final AlertSeverity getSeverity() {
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
    private AlertSeverity getPrioritizedSeverityLevel(final AlertSeverity checkSeverity) {
        AlertSeverity severityLevel = this.severity;
        if (severityLevel.getCriticality() < checkSeverity.getCriticality()) {
            severityLevel = checkSeverity;
        }

        return severityLevel;
    }
}
