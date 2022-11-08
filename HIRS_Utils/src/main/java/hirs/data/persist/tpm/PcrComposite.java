package hirs.data.persist.tpm;

import hirs.data.persist.TPMMeasurementRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.FetchType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Java class for PcrComposite, based on the code auto-generated using xjc
 * command line tool. The PcrCompositeType complex type is used to aggregate
 * multiple PCR values in a single structure and represents, in XML, the TPM's
 * TPM_PCR_COMPOSITE structure returned from a call to Quote TPM PCRs. In the
 * original class, the PcrValue was a list of PcrCompositeType.PcrValue objects.
 * This was replaced as a list of TPMMeasurementRecords, and the TPMValue class
 * was removed. This change was not TCG-compliant, as the auto-generated code
 * would produce something like:
 * <p>&nbsp;
 *   <pre>
 *     &lt;PcrValue PcrNumber="0"&gt;06fl7EXo34MWxuLq9kcXI9la9NA=&lt;/ns3:PcrValue&gt;
 * </pre>
 * <p>
 *   but using TPMMeasurementRecords result in something like:
 * <p>&nbsp;
 *   <pre>
 *     &lt;PcrValue PcrNumber="2"&gt;
 *       &lt;hash&gt;
 *         &lt;digest&gt;AAECAwQFBgcICQoLDA0ODxAREhM=&lt;/digest&gt;
 *         &lt;algorithm&gt;SHA1&lt;/algorithm&gt;
 *     &lt;/hash&gt;
 *   &lt;/PcrValue&gt;
 * </pre>
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PcrComposite",
        namespace = "http://www.trustedcomputinggroup.org/XML/SCHEMA/"
                + "Integrity_Report_v1_0#", propOrder = {"pcrSelection",
                "valueSize", "pcrValueList" })
@Embeddable
public class PcrComposite implements Serializable {

    private static final Logger LOGGER = LogManager
            .getLogger(PcrComposite.class);

    @XmlElement(name = "PcrSelection", required = true)
    @Embedded
    private final PcrSelection pcrSelection;

    @XmlElement(name = "PcrValue", required = true)
    @ElementCollection(fetch = FetchType.EAGER)
    private final List<TPMMeasurementRecord> pcrValueList;

    /**
     * Default constructor necessary for marshalling/unmarshalling xml.
     */
    protected PcrComposite() {
        pcrSelection = null;
        pcrValueList = new ArrayList<>();
    }

    /**
     * Constructor used to create a PcrComposite object.
     *
     * @param pcrSelection
     *            {@link PcrSelection } object, identifies which TPM PCRs are
     *            quoted
     * @param pcrValueList
     *            List of TPMMeasurementRecords representing the PCR values
     */
    public PcrComposite(final PcrSelection pcrSelection,
            final List<TPMMeasurementRecord> pcrValueList) {
        if (pcrSelection == null) {
            LOGGER.error("null pcrSelection value");
            throw new NullPointerException("pcrSelection");
        }
        if (pcrValueList == null) {
            LOGGER.error("null pcrValueList value");
            throw new NullPointerException("pcrValueList");
        }
        this.pcrSelection = pcrSelection;
        this.pcrValueList = pcrValueList;
    }

    /**
     * Gets the value of the pcrSelection property, which identifies which TPM
     * PCRs are quoted.
     *
     * @return {@link PcrSelection } object
     *
     */
    public final PcrSelection getPcrSelection() {
        return pcrSelection;
    }

    /**
     * Gets the value of the valueSize property, the length in bytes of the
     * array of PcrValue complex types.
     *
     * @return int value representing the valueSize
     *
     */
    @XmlElement(name = "ValueSize", required = true)
    public final int getValueSize() {
        int valueSize = 0;
        for (TPMMeasurementRecord record : this.pcrValueList) {
            valueSize += record.getHash().getDigest().length;
        }
        return valueSize;
    }

    /**
     * Gets the list of PCR values, represented as a List of
     * TPMMeasurementRecord objects.
     *
     * @return list of TPMeasurementRecords
     */
    public final List<TPMMeasurementRecord> getPcrValueList() {
        return Collections.unmodifiableList(pcrValueList);
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PcrComposite that = (PcrComposite) o;

        if (!pcrSelection.equals(that.pcrSelection)) {
            return false;
        }

        /*
        * Only testing equality on the list sizes because Hibernate's
        * PersistentBag (which implements the returned List) delegates to
        * Object#equals, which will return false unless they're
        * the same object.
        */
        if (pcrValueList.size() != that.pcrValueList.size()) {
            return false;
        }

        return true;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = pcrSelection.hashCode();
        result = prime * result + pcrValueList.hashCode();
        return result;
    }
}
