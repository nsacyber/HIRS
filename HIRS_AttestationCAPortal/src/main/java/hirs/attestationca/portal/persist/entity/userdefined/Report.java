package hirs.attestationca.portal.persist.entity.userdefined;

import hirs.attestationca.persist.entity.AbstractEntity;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * A <code>Report</code> represents an integrity report to be appraised by an
 * <code>Appraiser</code>. An <code>Appraiser</code> validates the integrity of
 * a client's platform with an integrity report. Example reports include an IMA
 * report and TPM report.
 * <p>
 * This <code>Report</code> class contains minimal information because each
 * report is vastly different. There is an identification number in case the
 * <code>Report</code> is stored in a database, and there is a report type. The
 * report type is used to determine which <code>Appraiser</code>s can appraise
 * the report.
 */
@Entity
@Access(AccessType.FIELD)
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Report extends AbstractEntity {
    /**
     * Default constructor.
     */
    protected Report() {
        super();
    }

    /**
     * Returns a <code>String</code> that indicates this report type. The report
     * type is used to find an <code>Appraiser</code> that can appraise this
     * <code>Report</code>.
     *
     * @return report type
     */
    public abstract String getReportType();
}
