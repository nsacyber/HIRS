package hirs.data.persist.tpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Java class for QuoteSignature complex type, which was modified from code
 * auto-generated using the xjc command line tool. This code generates XML that
 * matches what is specified for the QuoteSignatureType ComplexType in the
 * IntegrityReportManifest_v1_0_1 schema file.
 * <p>
 * The original auto-generated code included:
 * <ul>
 *   <li>CanonicalizationMethod
 *   <ul>
 *     <li>removed</li>
 *     <li>not included in original HIRS report</li>
 *     <li>not required by TCG schema</li>
 *   </ul></li>
 *   <li>SignatureMethod
 *   <ul>
 *     <li>removed</li>
 *     <li>not included in original HIRS report</li>
 *     <li>required by TCG schema, will need to be re-added for TCG compliance</li>
 *   </ul></li>
 *   <li>SignatureValue
 *   <ul>
 *     <li>included in original HIRS report</li>
 *     <li>required by TCG schema</li>
 *   </ul></li>
 *   <li>KeyInfo
 *   <ul>
 *     <li>removed</li>
 *     <li>not included in original HIRS report</li>
 *     <li>required by TCG schema, will need to be re-added for TCG compliance</li>
 *   </ul></li>
 *   <li>ObjectType
 *   <ul>
 *     <li>removed</li>
 *     <li>not included in original HIRS report</li>
 *     <li>not required by TCG schema</li>
 *   </ul></li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "QuoteSignature",
        namespace = "http://www.trustedcomputinggroup.org/XML/SCHEMA/"
                + "Integrity_Report_v1_0#",
        propOrder = {"rawQuote", "signatureValue" })
@Embeddable
public class QuoteSignature implements Serializable {
    private static final int SIGNATURE_LENGTH = 10000;

    @XmlElement(name = "RawQuote", required = false)
    @Column(length = SIGNATURE_LENGTH)
    private final byte[] rawQuote;

    @XmlElement(name = "SignatureValue", required = true)
    @Column(length = SIGNATURE_LENGTH)
    private final byte[] signatureValue;

    /**
     * Default constructor necessary for marshalling/unmarshalling xml.
     */
    protected QuoteSignature() {
        this.rawQuote = new byte[0];
        this.signatureValue = new byte[0];
    }

    /**
     * Constructor used to create a QuoteSignature. The signature value may be
     * null.
     *
     * @param signatureValue
     *            SignatureValue that contains the Quote2 signature, may be null
     */
    public QuoteSignature(final byte[] signatureValue) {
        this(signatureValue, null);
    }
    /**
     * Constructor used to create a QuoteSignature. The signature value may be
     * null.
     *
     * @param signatureValue
     *            SignatureValue that contains the Quote2 signature, may be null
     * @param rawQuote
     *            Value that was signed, may be null
     */
    public QuoteSignature(final byte[] signatureValue, final byte[] rawQuote) {
        if (rawQuote == null) {
            this.rawQuote = null;
        } else {
            this.rawQuote = rawQuote.clone();
        }
        if (signatureValue == null) {
            this.signatureValue = null;
        } else {
            this.signatureValue = signatureValue.clone();
        }
    }

    /**
     * Gets the value of the signatureValue property.
     *
     * @return possible object is a byte array containing the signature value, may return null
     *
     */
    public final byte[] getSignatureValue() {
        if (signatureValue != null) {
            return signatureValue.clone();
        }

        return null;
    }

    /**
     * Gets the value of the rawQuote property.
     *
     * @return possible object is a byte array containing the raw quote value, may return null
     *
     */
    public byte[] getRawQuote() {
        if (rawQuote != null) {
            return rawQuote.clone();
        }
        return null;
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        QuoteSignature that = (QuoteSignature) o;

        if (!Arrays.equals(signatureValue, that.signatureValue)) {
            return false;
        }

        return true;
    }

    @Override
    public final int hashCode() {
        if (signatureValue != null) {
            return Arrays.hashCode(signatureValue);
        }
        return 0;
    }
}
