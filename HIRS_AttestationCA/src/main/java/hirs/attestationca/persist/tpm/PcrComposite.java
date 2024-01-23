package hirs.attestationca.persist.tpm;

import hirs.attestationca.persist.entity.userdefined.record.TPMMeasurementRecord;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.FetchType;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.extern.log4j.Log4j2;

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
@Log4j2
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PcrComposite",
        namespace = "http://www.trustedcomputinggroup.org/XML/SCHEMA/"
                + "Integrity_Report_v1_0#", propOrder = {"pcrSelection",
        "valueSize", "pcrValueList" })
@Embeddable
public class PcrComposite {

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
            log.error("null pcrSelection value");
            throw new NullPointerException("pcrSelection");
        }
        if (pcrValueList == null) {
            log.error("null pcrValueList value");
            throw new NullPointerException("pcrValueList");
        }
        this.pcrSelection = pcrSelection;
        this.pcrValueList = pcrValueList.stream().toList();
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
}
