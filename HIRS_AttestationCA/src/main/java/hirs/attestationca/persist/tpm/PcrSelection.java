package hirs.attestationca.persist.tpm;

import jakarta.persistence.Embeddable;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import lombok.extern.log4j.Log4j2;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Arrays;

/**
 * Java class for PcrSelection complex type, which was modified from code
 * auto-generated using the xjc command line tool. This code generates XML that
 * matches what is specified for the PcrSelectonType ComplexType in the
 * IntegrityReportManifest_v1_0_1 schema file.
 */
@Log4j2
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PcrSelection",
        namespace = "http://www.trustedcomputinggroup.org/XML/SCHEMA/"
                + "Integrity_Report_v1_0#")
@Embeddable
public class PcrSelection {

    /**
     * All PCRs are on.
     */
    public static final int ALL_PCRS_ON = 0xffffff;
    private static final int MAX_SIZE_PCR_ARRAY = 3;
    @XmlAttribute(name = "PcrSelect", required = true)
    private final byte[] pcrSelect;

    /**
     * Default constructor necessary for marshalling/unmarshalling xml.
     */
    protected PcrSelection() {
        this.pcrSelect = new byte[0];
    }

    /**
     * Constructor used to create a PcrSelection object.
     * <p>
     * PcrSelect is a contiguous bit map that shows which PCRs are selected.
     * Each byte represents 8 PCRs. Byte 0 indicates PCRs 0-7, byte 1 8-15 and
     * so on. For each byte, the individual bits represent a corresponding PCR.
     *
     * @param pcrSelect byte array indicating which PCRS are selected
     */
    public PcrSelection(final byte[] pcrSelect) {
        if (pcrSelect == null) {
            log.error("null pcrSelect value");
            throw new NullPointerException("pcrSelect");
        }
        if (pcrSelect.length > MAX_SIZE_PCR_ARRAY) {
            log.error(
                    "pcrSelect byte array is {}, must be length {} or less",
                    MAX_SIZE_PCR_ARRAY, pcrSelect.length);
            throw new InvalidParameterException("pcrSelect");
        }
        this.pcrSelect = new byte[MAX_SIZE_PCR_ARRAY];
        System.arraycopy(pcrSelect, 0, this.pcrSelect, 0, pcrSelect.length);
    }

    /**
     * Constructor used to create a PcrSelection object using a long as the
     * selection value. For example, to select the first 3 PCRs, one would use
     * the long value 7 (b0000 0000 0000 0111).
     *
     * @param pcrSelectLong long value representing the bits to be selected
     */
    public PcrSelection(final long pcrSelectLong) {
        if (pcrSelectLong > ALL_PCRS_ON) {
            log.error("pcrSelect long value must be less than 3 bytes");
            throw new InvalidParameterException("pcrSelect");
        }
        final int bytesInLong = 8;
        this.pcrSelect = Arrays.copyOfRange(
                ByteBuffer.allocate(bytesInLong)
                        .putLong(Long.reverse(pcrSelectLong)).array(),
                0, MAX_SIZE_PCR_ARRAY);
    }

    /**
     * Gets the value of the sizeOfSelect property, which represents the size in
     * bytes of the PcrSelect structure.
     *
     * @return int sizeOfSelect
     */
    @XmlAttribute(name = "SizeOfSelect", required = true)
    @XmlSchemaType(name = "unsignedShort")
    public final int getSizeOfSelect() {
        return this.pcrSelect.length;
    }

    /**
     * Gets the value of the pcrSelect property. PcrSelect is a contiguous bit
     * map that shows which PCRs are selected. Each byte represents 8 PCRs. Byte
     * 0 indicates PCRs 0-7, byte 1 8-15 and so on. For each byte, the
     * individual bits represent a corresponding PCR.
     *
     * @return possible object is byte[]
     */
    public final byte[] getPcrSelect() {
        return pcrSelect.clone();
    }

    /**
     * Returns the length of the PcrSelection object if it were represented as a byte array. The
     * byte array contains the size of the PCR selection mask in 2 bytes (a short) in addition to
     * the PCR selection mask.
     *
     * @return length of the byte array representing the PcrSelection object.
     */
    public final int getLength() {
        return 2 + this.getSizeOfSelect();
    }

    /**
     * Returns the value of the PcrSelection object as it is represented as a byte array. The first
     * two bytes of the byte array represent the size of the PCR selection mask and the rest of the
     * byte array is the PCR selection mask itself.
     *
     * @return byte array representing the PcrSelection object.
     */
    public final byte[] getValue() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(this.getLength());

        byteBuffer.putShort((short) this.getSizeOfSelect());
        byteBuffer.put(this.getPcrSelect());

        return byteBuffer.array();

    }
}
