package hirs.attestationca.portal.persist.entity.userdefined.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.utils.StringValidator;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.security.cert.X509Certificate;

/**
 * This class is used to represent the TPM information for a device.
 */
@Getter
@EqualsAndHashCode
@Embeddable
public class TPMInfo implements Serializable {
    private static final Logger LOGGER = LogManager.getLogger(TPMInfo.class);
    private static final int MAX_BLOB_SIZE = 65535;

    @XmlElement
    @Column(length = DeviceInfoReport.MED_STRING_LENGTH, nullable = true)
    private String tpmMake;

    @XmlElement
    @Column(nullable = true)
    private short tpmVersionMajor;

    @XmlElement
    @Column(nullable = true)
    private short tpmVersionMinor;

    @XmlElement
    @Column(nullable = true)
    private short tpmVersionRevMajor;

    @XmlElement
    @Column(nullable = true)
    private short tpmVersionRevMinor;

    @XmlElement
//    @XmlJavaTypeAdapter(X509CertificateAdapter.class)
    @Lob
//    @Type(type = "hirs.attestationca.persist.type.X509CertificateType")
    @JsonIgnore
    private X509Certificate identityCertificate;

    @Column(nullable = true, length = MAX_BLOB_SIZE)
    private byte[] pcrValues;

    @Column(nullable = true, length = MAX_BLOB_SIZE)
    private byte[] tpmQuoteHash;

    @Column(nullable = true, length = MAX_BLOB_SIZE)
    private byte[] tpmQuoteSignature;

    /**
     * Constructor used to create a TPMInfo object.
     *
     * @param tpmMake
     *            String representing the make information for the TPM,
     *            NullPointerException thrown if null
     * @param tpmVersionMajor
     *            short representing the major version number for the TPM
     * @param tpmVersionMinor
     *            short representing the minor version number for the TPM
     * @param tpmVersionRevMajor
     *            short representing the major revision number for the TPM
     * @param tpmVersionRevMinor
     *            short representing the minor revision number for the TPM
     * @param identityCertificate
     *            byte array with the value of the identity certificate
     * @param pcrValues
     *            short representing the major revision number for the TPM
     * @param tpmQuoteHash
     *            short representing the minor revision number for the TPM
     * @param tpmQuoteSignature
     *            byte array with the value of the identity certificate
     */
    @SuppressWarnings("parameternumber")
    public TPMInfo(final String tpmMake, final short tpmVersionMajor,
                   final short tpmVersionMinor, final short tpmVersionRevMajor,
                   final short tpmVersionRevMinor,
                   final X509Certificate identityCertificate, final byte[] pcrValues,
                   final byte[] tpmQuoteHash, final byte[] tpmQuoteSignature) {
        setTPMMake(tpmMake);
        setTPMVersionMajor(tpmVersionMajor);
        setTPMVersionMinor(tpmVersionMinor);
        setTPMVersionRevMajor(tpmVersionRevMajor);
        setTPMVersionRevMinor(tpmVersionRevMinor);
        setIdentityCertificate(identityCertificate);
        setPcrValues(pcrValues);
        setTpmQuoteHash(tpmQuoteHash);
        setTpmQuoteSignature(tpmQuoteSignature);
    }

    /**
     * Constructor used to create a TPMInfo object without an identity
     * certificate.
     *
     * @param tpmMake
     *            String representing the make information for the TPM,
     *            NullPointerException thrown if null
     * @param tpmVersionMajor
     *            short representing the major version number for the TPM
     * @param tpmVersionMinor
     *            short representing the minor version number for the TPM
     * @param tpmVersionRevMajor
     *            short representing the major revision number for the TPM
     * @param tpmVersionRevMinor
     *            short representing the minor revision number for the TPM
     * @param pcrValues
     *            short representing the major revision number for the TPM
     * @param tpmQuoteHash
     *            short representing the minor revision number for the TPM
     * @param tpmQuoteSignature
     *            byte array with the value of the identity certificate
     */
    @SuppressWarnings("parameternumber")
    public TPMInfo(final String tpmMake, final short tpmVersionMajor,
                   final short tpmVersionMinor, final short tpmVersionRevMajor,
                   final short tpmVersionRevMinor, final byte[] pcrValues,
                   final byte[] tpmQuoteHash, final byte[] tpmQuoteSignature) {
        setTPMMake(tpmMake);
        setTPMVersionMajor(tpmVersionMajor);
        setTPMVersionMinor(tpmVersionMinor);
        setTPMVersionRevMajor(tpmVersionRevMajor);
        setTPMVersionRevMinor(tpmVersionRevMinor);
        setPcrValues(pcrValues);
        setTpmQuoteHash(tpmQuoteHash);
        setTpmQuoteSignature(tpmQuoteSignature);
    }

    /**
     * Constructor used to create a TPMInfo object without an identity
     * certificate.
     *
     * @param tpmMake
     *            String representing the make information for the TPM,
     *            NullPointerException thrown if null
     * @param tpmVersionMajor
     *            short representing the major version number for the TPM
     * @param tpmVersionMinor
     *            short representing the minor version number for the TPM
     * @param tpmVersionRevMajor
     *            short representing the major revision number for the TPM
     * @param tpmVersionRevMinor
     *            short representing the minor revision number for the TPM
     */
    public TPMInfo(final String tpmMake, final short tpmVersionMajor,
                   final short tpmVersionMinor, final short tpmVersionRevMajor,
                   final short tpmVersionRevMinor) {
        this(tpmMake, tpmVersionMajor, tpmVersionMinor, tpmVersionRevMajor,
                tpmVersionRevMinor, null,
                new byte[0], new byte[0], new byte[0]);
    }

    /**
     * Constructor used to create a TPMInfo object without an identity
     * certificate.
     *
     * @param tpmMake
     *            String representing the make information for the TPM,
     *            NullPointerException thrown if null
     * @param tpmVersionMajor
     *            short representing the major version number for the TPM
     * @param tpmVersionMinor
     *            short representing the minor version number for the TPM
     * @param tpmVersionRevMajor
     *            short representing the major revision number for the TPM
     * @param tpmVersionRevMinor
     *            short representing the minor revision number for the TPM
     * @param identityCertificate
     *            byte array with the value of the identity certificate
     */
    public TPMInfo(final String tpmMake, final short tpmVersionMajor,
                   final short tpmVersionMinor, final short tpmVersionRevMajor,
                   final short tpmVersionRevMinor,
                   final X509Certificate identityCertificate) {
        this(tpmMake, tpmVersionMajor, tpmVersionMinor, tpmVersionRevMajor,
                tpmVersionRevMinor, identityCertificate,
                new byte[0], new byte[0], new byte[0]);
    }

    /**
     * Default constructor used for marshalling/unmarshalling XML objects.
     */
    public TPMInfo() {
        this(DeviceInfoReport.NOT_SPECIFIED,
                (short) 0,
                (short) 0,
                (short) 0,
                (short) 0,
                new byte[0],
                new byte[0],
                new byte[0]);
        identityCertificate = null;
    }

    /**
     * Getter for the tpmQuote passed up by the client.
     * @return a byte blob of quote
     */
    public final byte[] getTpmQuoteHash() {
        return tpmQuoteHash.clone();
    }

    /**
     * Getter for the quote signature.
     * @return a byte blob.
     */
    public final byte[] getTpmQuoteSignature() {
        return tpmQuoteSignature.clone();
    }

    /**
     * Getter for the pcr values.
     * @return a byte blob for the pcrValues.
     */
    public final byte[] getPcrValues() {
        return pcrValues.clone();
    }

    private void setTPMMake(final String tpmMake) {
        LOGGER.debug("setting TPM make info: {}", tpmMake);
        this.tpmMake = StringValidator.check(tpmMake, "tpmMake")
                .notNull().maxLength(DeviceInfoReport.MED_STRING_LENGTH).getValue();
    }

    private void setTPMVersionMajor(final short tpmVersionMajor) {
        if (tpmVersionMajor < 0) {
            LOGGER.error("TPM major version number cannot be negative: {}",
                    tpmVersionMajor);
            throw new IllegalArgumentException(
                    "negative TPM major version number");
        }
        LOGGER.debug("setting TPM major version number: {}", tpmVersionMajor);
        this.tpmVersionMajor = tpmVersionMajor;
    }

    private void setTPMVersionMinor(final short tpmVersionMinor) {
        if (tpmVersionMinor < 0) {
            LOGGER.error("TPM minor version number cannot be negative: {}",
                    tpmVersionMinor);
            throw new IllegalArgumentException(
                    "negative TPM minor version number");
        }
        LOGGER.debug("setting TPM minor version number: {}", tpmVersionMinor);
        this.tpmVersionMinor = tpmVersionMinor;
    }

    private void setTPMVersionRevMajor(final short tpmVersionRevMajor) {
        if (tpmVersionRevMajor < 0) {
            LOGGER.error("TPM major revision number cannot be negative: {}",
                    tpmVersionRevMajor);
            throw new IllegalArgumentException(
                    "negative TPM major revision number");
        }
        LOGGER.debug("setting TPM major revision version number: {}",
                tpmVersionRevMajor);
        this.tpmVersionRevMajor = tpmVersionRevMajor;
    }

    private void setTPMVersionRevMinor(final short tpmVersionRevMinor) {
        if (tpmVersionRevMinor < 0) {
            LOGGER.error("TPM minor revision number cannot be negative: {}",
                    tpmVersionRevMinor);
            throw new IllegalArgumentException(
                    "negative TPM minor revision number");
        }
        LOGGER.debug("setting TPM minor revision version number: {}",
                tpmVersionRevMinor);
        this.tpmVersionRevMinor = tpmVersionRevMinor;
    }

    private void setIdentityCertificate(
            final X509Certificate identityCertificate) {
        if (identityCertificate == null) {
            LOGGER.error("identity certificate cannot be null");
            throw new NullPointerException("identityCertificate");
        }
        LOGGER.debug("setting identity certificate");
        this.identityCertificate = identityCertificate;
    }

    private void setPcrValues(final byte[] pcrValues) {
        if (pcrValues == null) {
            this.pcrValues = new byte[0];
        } else {
            this.pcrValues = pcrValues.clone();
        }
    }

    private void setTpmQuoteHash(final byte[] tpmQuoteHash) {
        if (tpmQuoteHash == null) {
            this.tpmQuoteHash = new byte[0];
        } else {
            this.tpmQuoteHash = tpmQuoteHash.clone();
        }
    }

    private void setTpmQuoteSignature(final byte[] tpmQuoteSignature) {
        if (tpmQuoteSignature == null) {
            this.tpmQuoteSignature = new byte[0];
        } else {
            this.tpmQuoteSignature = tpmQuoteSignature.clone();
        }
    }
}
