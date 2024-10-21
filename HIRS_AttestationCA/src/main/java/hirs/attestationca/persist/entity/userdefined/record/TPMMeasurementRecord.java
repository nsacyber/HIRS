package hirs.attestationca.persist.entity.userdefined.record;

import hirs.attestationca.persist.entity.userdefined.ExaminableRecord;
import hirs.utils.digest.Digest;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * Class represents a Trusted Platform Module (TPM) Platform Configuration
 * Register (PCR). Each PCRMeasurementRecord contains a pcrID which is a direct
 * reference to the TPM PCR location. For example, a pcrID of 0 is a reference
 * to TPM PCR 0. Each PCRMeasurementRecord also contains a cryptographic hash
 * which represents the value stored at the associated TPM PCR location.
 */
@Log4j2
@Getter
@ToString
@EqualsAndHashCode
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

    /**
     * String length of a SHA 1 PCR value.
     */
    public static final int SHA_BYTE_LENGTH = 40;

    /**
     * String length of a 256 SHA PCR value.
     */
    public static final int SHA_256_BYTE_LENGTH = 64;


    @Column(name = "pcr", nullable = false)
    @XmlAttribute(name = "PcrNumber", required = true)
    private final int pcrId;
    @Embedded
    @XmlElement
    private final Digest hash;

    /**
     * Constructor initializes values associated with TPMMeasurementRecord.
     *
     * @param pcrId is the TPM PCR index. pcrId must be between 0 and 23.
     * @param hash  represents the measurement digest found at the particular PCR
     *              index.
     * @throws IllegalArgumentException if pcrId is not valid
     */
    public TPMMeasurementRecord(final int pcrId, final Digest hash)
            throws IllegalArgumentException {
        super();
        checkForValidPcrId(pcrId);
        if (hash == null) {
            log.error("null hash value");
            throw new NullPointerException("hash");
        }

        this.pcrId = pcrId;
        this.hash = hash;
    }

    /**
     * Constructor initializes values associated with TPMMeasurementRecord.
     *
     * @param pcrId is the TPM PCR index. pcrId must be between 0 and 23.
     * @param hash  represents the measurement digest found at the particular PCR
     *              index.
     * @throws DecoderException if there is a decode issue with string hex.
     */
    public TPMMeasurementRecord(final int pcrId, final String hash)
            throws DecoderException {
        this(pcrId, new Digest(Hex.decodeHex(hash.toCharArray())));
    }

    /**
     * Constructor initializes values associated with TPMMeasurementRecord.
     *
     * @param pcrId is the TPM PCR index. pcrId must be between 0 and 23.
     * @param hash  represents the measurement digest found at the particular PCR
     *              index.
     */
    public TPMMeasurementRecord(final int pcrId, final byte[] hash) {
        this(pcrId, new Digest(hash));
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    private TPMMeasurementRecord() {
        super();
        this.pcrId = -1;
        this.hash = null;
    }

    /**
     * Helper method to determine if a PCR ID number is valid.
     *
     * @param pcrId int to check
     */
    public static void checkForValidPcrId(final int pcrId) {
        if (pcrId < MIN_PCR_ID || pcrId > MAX_PCR_ID) {
            final String msg = String.format("invalid PCR ID: %d", pcrId);
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }
}
