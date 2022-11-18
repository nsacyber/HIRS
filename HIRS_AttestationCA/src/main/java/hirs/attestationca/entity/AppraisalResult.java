package hirs.attestationca.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import hirs.appraiser.Appraiser;
import hirs.attestationca.AppraisalResultSerializer;
import hirs.data.persist.AbstractEntity;
import hirs.data.persist.AppraisalStatus;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * An <code>AppraisalResult</code> represents the result of an appraisal.
 *
 */
@Entity
@Table(name = "AppraisalResult")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonSerialize(using = AppraisalResultSerializer.class)
public class AppraisalResult extends AbstractEntity {

    /**
     * Corresponding <code>Appraiser</code>. Can be NULL.
     */
    @Column
    @XmlElement
    private Class<? extends Appraiser> appraiser;

    /**
     * Appraisal result, represented as an <code>AppraiserStatus</code>.
     */
    @Column
    @Enumerated(EnumType.STRING)
    @XmlElement
    private AppraisalStatus.Status appraisalStatus = AppraisalStatus.Status.ERROR;

    /**
     * Appraisal result message.
     */
    @Column(length = RESULT_MESSAGE_LENGTH)
    @XmlElement
    @Lob
    private String appraisalResultMessage = "No appraisal result message found";

    /**
     * Associated report summary.
     */
    @ManyToOne
    @JoinColumn(name = "reportSummary")
    private ReportSummary reportSummary;

    /**
     * Creates a new AppraisalResult with an <code>AppraisalStatus</code> and appraisal result
     * message.
     *
     * @param appraiser
     *          Corresponding <code>Appraiser</code>
     * @param appraisalStatus
     *          enum representing the appraisal status
     * @param appraisalResultMessage
     *          String representing the appraisal message
     */
    public AppraisalResult(final Class<? extends Appraiser> appraiser,
                           final AppraisalStatus.Status appraisalStatus,
                           final String appraisalResultMessage) {
        super();
        this.appraiser = appraiser;
        this.appraisalStatus = appraisalStatus;
        this.appraisalResultMessage = appraisalResultMessage;
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected AppraisalResult() {
        super();
    }

    /**
     * Gets the corresponding <code>Appraiser</code>.
     *
     * @return Appraiser appraiser
     */
    public final Class<? extends Appraiser> getAppraiser() {
        return appraiser;
    }

    /**
     * Gets the appraisal result.
     *
     * @return AppraisalStatus appraisalResult
     */
    public final AppraisalStatus.Status getAppraisalStatus() {
        return appraisalStatus;
    }

    /**
     * Sets the appraisal result.
     *
     * @param appraisalResultStatus
     *          enum representing the appraisal status
     */
    public final void setAppraisalStatus(final AppraisalStatus.Status appraisalResultStatus) {
        this.appraisalStatus = appraisalResultStatus;
    }

    /**
     * Gets the appraisal result message.
     *
     * @return String appraisalResultMessage
     */
    public final String getAppraisalResultMessage() {
        return appraisalResultMessage;
    }

    /**
     * Sets the appraisal result message.
     *
     * @param appraisalResultMessage
     *          String representing appraisal message
     */
    public final void setAppraisalResultMessage(final String appraisalResultMessage) {
        this.appraisalResultMessage = appraisalResultMessage;
    }

    /**
     * Sets the appraisal status and result message.
     *
     * @param status
     *          enum representing the appraisal status
     * @param appraisalResultMessage
     *          String representing appraisal message
     */
    public final void setAppraisalStatusAndResultMessage(final AppraisalStatus.Status status,
                                                         final String appraisalResultMessage) {
        this.appraisalStatus = status;
        this.appraisalResultMessage = appraisalResultMessage;
    }


    /**
     * Get associated <code>ReportSummary</code>.
     *
     * @return ReportSummary reportSummary
     */
    public ReportSummary getReportSummary() {
        return reportSummary;
    }

    /**
     * Set associated <code>ReportSummary</code>.
     *
     * @param reportSummary
     *          Associated <code>ReportSummary</code>
     */
    public void setReportSummary(final ReportSummary reportSummary) {
        this.reportSummary = reportSummary;
    }
}
