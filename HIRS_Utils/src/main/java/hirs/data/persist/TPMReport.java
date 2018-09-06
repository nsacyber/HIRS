package hirs.data.persist;

import hirs.data.persist.tpm.QuoteData;

import java.util.List;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * A TPMReport is a listing of {@link QuoteData} objects, which was modified
 * from code auto-generated using the xjc command line tool. This code
 * corresponds to the ReportType ComplexType in the
 * IntegrityReportManifest_v1_0_1 schema file.
 * <p>
 * The original auto-generated ReportType also included a SignerInfoType,
 * ConfidenceValueType, a list of Snapshots, a list of SyncSnapshotRefs, and a
 * list of transitiveTrustPaths. These values were not included in the original
 * ReportSubmitRequest, so they are removed from this version. The only one that
 * was removed that is also required by the TCG schema is the list of
 * SyncSnapshotRefs, so these must be re-added to be fully TCG compliant. The
 * original auto-generated ReportType also included a List of QuoteData objects,
 * but this was simplified to only one QuoteData object, as our use case is only
 * one QuoteData object.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TPMReport",
        namespace = "http://www.trustedcomputinggroup.org/XML/SCHEMA/"
                + "Integrity_Report_v1_0#", propOrder = {"quoteData" })
@XmlRootElement(name = "Report",
        namespace = "http://www.trustedcomputinggroup.org/XML/SCHEMA/"
                + "Integrity_Report_v1_0#")
@Entity
public class TPMReport extends Report {

    private static final Logger LOGGER = LogManager.getLogger(TPMReport.class);

    @XmlElement(name = "QuoteData")
    @Embedded
    private final QuoteData quoteData;

    /**
     * Default constructor required for Hibernate and marshalling/unmarshalling
     * xml reports.
     */
    protected TPMReport() {
        this.quoteData = null;
    }

    /**
     * Constructor used to instantiate a <code>TPMReport</code> object.
     *
     * @param quoteData
     *            {@link QuoteData} object
     */
    public TPMReport(final QuoteData quoteData) {
        if (quoteData == null) {
            LOGGER.error("null quoteData value");
            throw new NullPointerException("quoteData");
        }
        this.quoteData = quoteData;
    }

    /**
     * Gets the value of the quoteData property.
     *
     * @return {@link QuoteData} object
     */
    public final QuoteData getQuoteData() {
        return quoteData;
    }

    /**
     * Gets the list of TPMMeasurementRecords.
     *
     * @return list of TPMMeasurementRecords
     */
    public final List<TPMMeasurementRecord> getTPMMeasurementRecords() {
        return this.quoteData.getQuote2().getQuoteInfo2().getPcrInfoShort()
                .getPcrComposite().getPcrValueList();
    }
    
    /**
     * Returns the <code>TPMMeasurementRecord</code> for a specific PCR. If
     * there is no measurement record for that PCR index then null is returned.
     *
     * @param pcr
     *            PCR ID
     * @return measurement record at that PCR ID or null if not found
     */
    public final TPMMeasurementRecord getTPMMeasurementRecord(final int pcr) {
        final List<TPMMeasurementRecord> records = getTPMMeasurementRecords();
        for (TPMMeasurementRecord record : records) {
            if (record.getPcrId() == pcr) {
                return record;
            }
        }
        return null;
    }

    @Override
    public final String getReportType() {
        return this.getClass().getName();
    }

}
