package hirs.data.persist;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Class represents a Trusted Platform Module (TPM) Platform Configuration
 * Register (PCR). Each PCRMeasurementRecord contains a pcrID which is a direct
 * reference to the TPM PCR location. For example, a pcrID of 0 is a reference
 * to TPM PCR 0. Each PCRMeasurementRecord also contains a cryptographic hash
 * which represents the value stored at the associated TPM PCR location.
 */
@Embeddable
@XmlAccessorType(XmlAccessType.FIELD)
public final class TPMMeasurementRecord extends ExaminableRecord {

    /**
     * Minimum possible value for a PCR ID. This is 0.
     */
    public static final int MIN_PCR_ID = 0;

    /**
     * Maximum possible value for a PCR ID. This is 23.
     */
    public static final int MAX_PCR_ID = 23;

    private static final Logger LOGGER =
            LogManager.getLogger(TPMMeasurementRecord.class);

    @Column(name = "pcr", nullable = false)
    @XmlAttribute(name = "PcrNumber", required = true)
    private final int pcrId;
    @Embedded
    @XmlElement
    private final Digest hash;

    /**
     * Constructor initializes values associated with PCRMeasurementRecord.
     *
     * @param pcrId
     *            is the TPM PCR index. pcrId must be between 0 and 23.
     * @param hash
     *            represents the measurement digest found at the particular PCR
     *            index.
     * @throws IllegalArgumentException
     *             if digest algorithm is not SHA-1
     */
    public TPMMeasurementRecord(final int pcrId, final Digest hash)
            throws IllegalArgumentException {
        super();
        checkForValidPcrId(pcrId);
        if (hash == null) {
            LOGGER.error("null hash value");
            throw new NullPointerException("hash");
        }

        this.pcrId = pcrId;
        this.hash = hash;
    }

    /**
     * Helper method to determine if a PCR ID number is valid.
     *
     * @param pcrId
     *            int to check
     */
    public static void checkForValidPcrId(final int pcrId) {
        if (pcrId < MIN_PCR_ID || pcrId > MAX_PCR_ID) {
            final String msg = String.format("invalid PCR ID: %d", pcrId);
            LOGGER.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected TPMMeasurementRecord() {
        super();
        this.pcrId = -1;
        this.hash = null;
    }

    /**
     * Returns the pcr index of the PCRMeasurementRecord.
     *
     * @return pcr index
     */
    public int getPcrId() {
        return pcrId;
    }

    /**
     * Returns the hash value of the PCRMeasurementRecord.
     *
     * @return pcr hash
     */
    public Digest getHash() {
        return hash;
    }

    /**
     * Returns a boolean if other is equal to this.
     * <code>TPMMeasurementRecord</code>s are identified by their PCR ID and
     * hash value, so this returns true if <code>other</code> is an instance of
     * <code>TPMMeasurementRecord</code> and its PCR ID and hash are the same as
     * this <code>TPMMeasurementRecord</code>. Otherwise this returns false.
     *
     * @param obj
     *            other object to test for equals
     * @return true if other is <code>TPMMeasurementRecord</code> and has same
     *         name
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TPMMeasurementRecord other = (TPMMeasurementRecord) obj;
        if (!hash.equals(other.hash)) {
            return false;
        }
        if (pcrId != other.pcrId) {
            return false;
        }
        return true;
    }

    /**
     * Returns the hash code for this <code>TPMMeasurementRecord</code>.
     * <code>TPMMeasurementRecord</code>s are identified by their PCR ID and
     * hash, so the returned hash is the hash of these two properties.
     *
     * @return hash
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + hash.hashCode();
        result = prime * result + pcrId;
        return result;
    }

    @Override
    public String toString() {
        return String.format("(%d, %s)", pcrId, hash);
    }
}
