package hirs.data.persist.tpm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Embeddable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Arrays;

/**
 * Java class for PcrSelection complex type, which was modified from code
 * auto-generated using the xjc command line tool. This code generates XML that
 * matches what is specified for the PcrSelectonType ComplexType in the
 * IntegrityReportManifest_v1_0_1 schema file.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PcrSelection",
        namespace = "http://www.trustedcomputinggroup.org/XML/SCHEMA/"
                + "Integrity_Report_v1_0#")
@Embeddable
public class PcrSelection implements Serializable {

    private static final Logger LOGGER = LogManager
            .getLogger(PcrSelection.class);
    private static final int MAX_SIZE_PCR_ARRAY = 3;
    /**
     * All PCRs are on.
     */
    public static final int ALL_PCRS_ON = 0xffffff;

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
     * @param pcrSelect
     *            byte array indicating which PCRS are selected
     *
     */
    public PcrSelection(final byte[] pcrSelect) {
        if (pcrSelect == null) {
            LOGGER.error("null pcrSelect value");
            throw new NullPointerException("pcrSelect");
        }
        if (pcrSelect.length > MAX_SIZE_PCR_ARRAY) {
            LOGGER.error(
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
     * @param pcrSelectLong
     *            long value representing the bits to be selected
     */
    public PcrSelection(final long pcrSelectLong) {
        if (pcrSelectLong > ALL_PCRS_ON) {
            LOGGER.error("pcrSelect long value must be less than 3 bytes");
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
     * Returns a boolean indicating if the bit representing a given PCR number
     * is selected. An IndexOutOfBoundsException is thrown if an invalid index
     * is given.
     *
     * @param pcrNumber
     *            int value of the PCR number
     * @return boolean indicating if the PCR is selected
     */
    public final boolean isPcrSelected(final int pcrNumber) {
        final int numBitsInByte = 8;
        final int mostSigBitInByte = 128;
        if (pcrNumber < 0) {
            LOGGER.error("negative PCR number");
            throw new IndexOutOfBoundsException("negative PCR number");
        }
        int byteIndex = pcrNumber / numBitsInByte;
        int bitIndex = pcrNumber % numBitsInByte;
        if (byteIndex > this.pcrSelect.length) {
            String msg = String.format(
                "index %d out of bounds, pcrSelect size is %d bytes",
                byteIndex, this.pcrSelect.length);
            LOGGER.error(msg);
            throw new IndexOutOfBoundsException(msg);
        }

        return (this.pcrSelect[byteIndex]
                & (mostSigBitInByte >>> bitIndex)) != 0;
    }

    /**
     * Returns a mask for use with the tpm_module. The mask is only expected
     * to be 6 characters. If the mask is longer than 3 bytes, it is truncated
     * to the first 3. If the mask is shorter, it is extended with zeroes. Each
     * bit in the hex String represents the PCRs in the following order:
     *
     * <pre>
     * 7 6 5 4 3 2 1 0 15 14 13 12 11 10 9 8 23 22 21 20 19 18 17 16
     * </pre>
     *
     * @return 6 character String representing the mask
     */
    public final String getMaskForTPM() {
        final int expectedNumBytesInMask = 3;
        StringBuilder hex = new StringBuilder();

        for (int i = 0; i < expectedNumBytesInMask && i < this.pcrSelect.length;
                i++) {
            hex.append(String.format("%02x", reverseBits(this.pcrSelect[i])));
        }
        while (hex.length() < expectedNumBytesInMask * 2) {
            hex.append("00");
        }
        return hex.toString();
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(pcrSelect);
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PcrSelection)) {
            return false;
        }
        PcrSelection other = (PcrSelection) obj;
        if (!Arrays.equals(pcrSelect, other.pcrSelect)) {
            return false;
        }
        return true;
    }

    private byte reverseBits(final byte in) {
        byte reversed = 0;
        final int numBitsInByte = 8;
        for (int i = 0; i < numBitsInByte; i++) {
            byte bit = (byte) (in >> i & 1);
            reversed = (byte) ((reversed << 1) | bit);
        }
        return reversed;
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
