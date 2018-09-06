package hirs.data.persist.tpm;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Java class for QuoteInfo2 complex type, which was modified from code
 * auto-generated using the xjc command line tool. This code generates XML that
 * matches what is specified for the QuoteInfo2Type ComplexType in the
 * IntegrityReportManifest_v1_0_1 schema file.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuoteInfo2",
        namespace = "http://www.trustedcomputinggroup.org/XML/SCHEMA/"
                + "Integrity_Report_v1_0#", propOrder = {"pcrInfoShort" })
@Embeddable
public class QuoteInfo2 {

    private static final Logger LOGGER = LogManager.getLogger(QuoteInfo2.class);

    /**
     * Used to represent a known value returned by the tpm_module.
     */
    public static final String FIXED = "QUT2";

    /**
     * Used to represent a known value returned by the tpm_module.
     */
    public static final short TAG = 0x0036;

    @XmlElement(name = "PcrInfoShort", required = true)
    @Embedded
    private final PcrInfoShort pcrInfoShort;

    @XmlAttribute(name = "Tag", required = true)
    @XmlSchemaType(name = "unsignedShort")
    private final int tag;

    @XmlAttribute(name = "Fixed", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    @XmlSchemaType(name = "normalizedString")
    private final String fixed;

    @XmlAttribute(name = "ExternalData", required = true)
    private final byte[] externalData;

    /**
     * Default constructor necessary for marshalling/unmarshalling xml.
     */
    protected QuoteInfo2() {
        this.pcrInfoShort = new PcrInfoShort();
        this.tag = TAG;
        this.fixed = FIXED;
        this.externalData = new byte[0];
    }

    /**
     * Constructor used to create a QuoteInfo2 object.
     * <p>
     * The TPM specifications state the tag attribute is TPM_TAG_QUOTE_INFO2
     * which has a value of: 0x0036
     * <p>
     * The TPM specifications state the fixed attribute is the string "QUT2"
     *
     * @param pcrInfoShort
     *            {@link PcrInfoShort } object
     * @param externalData
     *            the TPM specifications state this attribute is 160 bits of
     *            externally supplied data - usually a freshness nonce
     */
    public QuoteInfo2(final PcrInfoShort pcrInfoShort,
            final byte[] externalData) {
        if (pcrInfoShort == null) {
            LOGGER.error("null pcrInfoShort value");
            throw new NullPointerException("pcrInfoShort");
        }
        if (externalData == null) {
            LOGGER.error("null externalData value");
            throw new NullPointerException("externalData");
        }
        this.pcrInfoShort = pcrInfoShort;
        this.tag = TAG;
        this.fixed = FIXED;
        this.externalData = externalData.clone();
    }

    /**
     * Gets the value of the pcrInfoShort property, a representation of the
     * TPM's TPM_PCR_INFO_SHORT structure.
     *
     * @return possible object is {@link PcrInfoShort }
     */
    public final PcrInfoShort getPcrInfoShort() {
        return pcrInfoShort;
    }

    /**
     * Gets the value of the tag property. The TPM specifications state this
     * attribute is TPM_TAG_QUOTE_INFO2 which has a value of: 0x0036.
     *
     * @return int tag value
     */
    public final int getTag() {
        return tag;
    }

    /**
     * Gets the value of the fixed property. The TPM specifications state this
     * attribute is the string "QUT2".
     *
     * @return String value fixed
     */
    public final String getFixed() {
        return fixed;
    }

    /**
     * Gets the value of the externalData property. The TPM specifications state
     * this attribute is 160 bits of externally supplied data - usually a
     * freshness nonce.
     *
     * @return possible object is byte[]
     */
    public final byte[] getExternalData() {
        return externalData.clone();
    }

    /**
     * Returns the length of the data structure if it had been represented as bytes, like the value
     * returned from tpm_module.
     *
     * @return int representing the number of bytes
     */
    public final int getLength() {
        return 2 + this.fixed.getBytes(Charset.defaultCharset()).length + this.externalData.length
                + this.pcrInfoShort.getLength();
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        QuoteInfo2 that = (QuoteInfo2) o;

        if (tag != that.tag) {
            return false;
        }
        if (!Arrays.equals(externalData, that.externalData)) {
            return false;
        }
        if (!fixed.equals(that.fixed)) {
            return false;
        }
        if (!pcrInfoShort.equals(that.pcrInfoShort)) {
            return false;
        }

        return true;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = pcrInfoShort.hashCode();
        result = prime * result + tag;
        result = prime * result + fixed.hashCode();
        result = prime * result + Arrays.hashCode(externalData);
        return result;
    }

    /**
     * Used to retrieve the value of the QuoteInfo2 as a byte array. The byte array is used during
     * appraisal to update the signature.
     *
     * @return byte array representing the QuoteInfo2 object
     */
    public final byte[] getValue() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(this.getLength());
        byteBuffer.putShort(TAG);
        byteBuffer.put(FIXED.getBytes(Charset.defaultCharset()));
        byteBuffer.put(externalData);
        byteBuffer.put(pcrInfoShort.getValue());
        return byteBuffer.array();
    }

    @Override
    public final String toString() {
        return Hex.encodeHexString(getValue());
    }
}
