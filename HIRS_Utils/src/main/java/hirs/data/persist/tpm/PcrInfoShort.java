package hirs.data.persist.tpm;

import hirs.data.persist.TPMMeasurementRecord;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Column;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Java class for PcrInfoShort complex type, which was modified from code
 * auto-generated using the xjc command line tool. This code generates XML that
 * matches what is specified for the PcrInfoShortType ComplexType in the
 * IntegrityReportManifest_v1_0_1 schema file.
 * <p>
 * The PcrInfoShortType complex type is an XML representation of the TPM's
 * TPM_PCR_INFO_SHORT structure. PcrComposite is not part of the TPM
 * PCR_INFO_SHORT structure, however CompositeHash is a hash of PcrComposite,
 * thus PcrComposite is included to provide the data necessary to compute and
 * validate CompositeHash.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PcrInfoShort",
        namespace = "http://www.trustedcomputinggroup.org/XML/SCHEMA/"
                + "Integrity_Report_v1_0#", propOrder = {"pcrSelection",
                "localityAtRelease", "compositeHash", "pcrComposite" })
@Embeddable
public class PcrInfoShort {
    private static final Logger LOGGER = LogManager
            .getLogger(PcrInfoShort.class);

    @XmlElement(name = "PcrSelection", required = true)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "pcrSelect",
                    column = @Column(name = "selectionPcrSelect")
            )
    })
    private final PcrSelection pcrSelection;

    @XmlElement(name = "LocalityAtRelease")
    @XmlSchemaType(name = "unsignedByte")
    private final short localityAtRelease;

    @XmlElement(name = "CompositeHash", required = true)
    private final byte[] compositeHash;

    @XmlElement(name = "PcrComposite", required = true)
    @Embedded
    private final PcrComposite pcrComposite;

    /**
     * Default constructor necessary for marshalling/unmarshalling xml.
     */
    protected PcrInfoShort() {
        this.pcrSelection = new PcrSelection();
        this.pcrComposite = new PcrComposite();
        this.localityAtRelease = 0;
        this.compositeHash = new byte[0];
    }

    /**
     * Constructor used to create a PcrInfoShort object.
     *
     * @param pcrSelection
     *            PcrSelection defines which TPM PCRs are used in the TPM Quote.
     * @param localityAtRelease
     *            short value includes locality information to provide the
     *            requestor a more complete view of the current platform
     *            configuration
     * @param compositeHash
     *            A hash of PcrComposite
     * @param pcrComposite
     *            A structure containing the actual values of the PCRs quoted.
     */
    public PcrInfoShort(final PcrSelection pcrSelection,
            final short localityAtRelease, final byte[] compositeHash,
            final PcrComposite pcrComposite) {
        if (pcrSelection == null) {
            LOGGER.error("null pcrSelection value");
            throw new NullPointerException("pcrSelection");
        }
        if (compositeHash == null) {
            LOGGER.error("null compositeHash value");
            throw new NullPointerException("compositeHash");
        }
        if (pcrComposite == null) {
            LOGGER.error("null pcrComposite value");
            throw new NullPointerException("pcrComposite");
        }

        this.pcrSelection = pcrSelection;
        this.localityAtRelease = localityAtRelease;
        this.compositeHash = compositeHash.clone();
        this.pcrComposite = pcrComposite;
    }

    /**
     * Gets the value of the pcrSelection property, which defines which TPM PCRs
     * are used in the TPM Quote.
     *
     * @return possible object is {@link PcrSelection }
     *
     */
    public final PcrSelection getPcrSelection() {
        return pcrSelection;
    }

    /**
     * Gets the value of the localityAtRelease property, which is included to
     * provide the requestor a more complete view of the current platform
     * configuration.
     *
     * @return short indicating the locality
     */
    public final short getLocalityAtRelease() {
        return localityAtRelease;
    }

    /**
     * Gets the value of the compositeHash property, a hash of PcrComposite.
     *
     * @return possible object is byte[]
     */
    public final byte[] getCompositeHash() {
        return compositeHash.clone();
    }

    /**
     * Gets the value of the pcrComposite property, a structure containing the
     * actual values of the PCRs quoted.
     *
     * @return possible object is {@link PcrComposite }
     */
    public final PcrComposite getPcrComposite() {
        return pcrComposite;
    }

    /**
     * Used to retrieve the size of select.
     *
     * @return int size in bytes
     */
    public final int getLength() {
        return this.pcrSelection.getLength() + 1
                + this.compositeHash.length;
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PcrInfoShort that = (PcrInfoShort) o;

        if (localityAtRelease != that.localityAtRelease) {
            return false;
        }
        if (!Arrays.equals(compositeHash, that.compositeHash)) {
            return false;
        }
        if (!pcrComposite.equals(that.pcrComposite)) {
            return false;
        }
        if (!pcrSelection.equals(that.pcrSelection)) {
            return false;
        }

        return true;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = pcrSelection.hashCode();
        result = prime * result + localityAtRelease;
        result = prime * result + Arrays.hashCode(compositeHash);
        result = prime * result + pcrComposite.hashCode();
        return result;
    }

    /**
     * Calculates the digest of the PCR values the same way a TPM computes the digest contained in
     * the quote. Useful for TPM appraisal and for ensuring the digest of the collected PCR values
     * match the digest in the quote.
     *
     * @return byte array containing the digest
     * @throws NoSuchAlgorithmException
     *             if MessageDigest doesn't recognize "SHA-1"
     */
    public final byte[] getCalculatedDigest() throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        byte[] computedDigest;

        final int sizeOfInt = 4;
        int sizeOfByteBuffer =
                this.pcrSelection.getLength() + sizeOfInt
                        + this.pcrComposite.getValueSize();

        ByteBuffer byteBuffer = ByteBuffer.allocate(sizeOfByteBuffer);
        LOGGER.debug("Size of the buffer allocated to hash: {}", sizeOfByteBuffer);

        byteBuffer.put(this.pcrSelection.getValue());
        byteBuffer.putInt(pcrComposite.getValueSize());

        for (TPMMeasurementRecord record: pcrComposite.getPcrValueList()) {
            byteBuffer.put(record.getHash().getDigest());
        }

        LOGGER.debug("PCR composite buffer to be hashed: {}",
                Hex.encodeHexString(byteBuffer.array()));
        computedDigest = messageDigest.digest(byteBuffer.array());
        LOGGER.debug("Calculated digest: {}", Hex.encodeHexString(computedDigest));

        return computedDigest;
    }

    /**
     * Returns the value of PcrInfoShort flattened into a byte array. The array contains the value
     * of PCR selection, the locality at release, and the composite hash.
     *
     * @return byte array representing the PcrInfoShort object
     */
    public final byte[] getValue() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(getLength());
        byteBuffer.put(pcrSelection.getValue());
        byteBuffer.put((byte) localityAtRelease);
        byteBuffer.put(compositeHash);

        return byteBuffer.array();
    }
}
