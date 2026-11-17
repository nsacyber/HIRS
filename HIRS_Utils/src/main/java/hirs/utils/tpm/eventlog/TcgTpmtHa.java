package hirs.utils.tpm.eventlog;

import hirs.utils.HexUtils;
import hirs.utils.specLookups.TcgAlgRegLookups;
import lombok.AccessLevel;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Class to for the TCG defined TPMT_HA structure used to support the Crypto Agile Log format.
 * <p>
 * typedef struct {
 * .     TPMI_ALG_HASH hashAlg;
 * .     TPMU_HA       digest;
 * } TPMT_HA;
 */
public class TcgTpmtHa {

    /**
     * TCG Defined Algorithm Identifiers.
     */
    @Getter
    private int hashAlgId = 0;

    /**
     * Length of the  hash.
     */
    @Getter
    private int hashLength = 0;

    /**
     * Human-readable name of the hash algorithm.
     */
    @Getter
    private String hashName = "";

    /**
     * Hash data.
     */
    @Getter(value = AccessLevel.PROTECTED)
    private byte[] digest = null;

    /**
     * buffer to hold the structure.
     */
    private byte[] buffer = null;

    /**
     * Constructor.
     *
     * @param is ByteArrayInputStream holding the TCG TPMT_HA structured data
     * @throws java.io.IOException if TPMT_HA structure cannot be parsed
     */
    public TcgTpmtHa(final ByteArrayInputStream is) throws IOException {
        byte[] algID = new byte[2];
        is.read(algID);
        byte[] rAlgID = HexUtils.leReverseByte(algID);
        hashAlgId = new BigInteger(rAlgID).intValue();
        hashName = tcgAlgIdToName(algID[0]);
        hashLength = tcgAlgLength(algID[0]);            // as of 2026, TCG Alg Registry only uses the upper byte
        if (hashLength == 0) {
            throw new IOException("Digest length for algorithm " + hashName
                    + " is 0, cannot read a digest with length 0");
        }
        digest = new byte[hashLength];
        is.read(digest);
        buffer = new byte[algID.length + digest.length];
        System.arraycopy(algID, 0, buffer, 0, algID.length);
        System.arraycopy(digest, 0, buffer, algID.length, digest.length);
    }

    /**
     * Returns the hash name via a lookup.
     * Lookup based upon section 6.3 for the TPM-Rev-2.0-Part-2-Structures.pdf document.
     * Only hash algorithms found in Table 7 are used.
     *
     * @param algId int to convert to string
     * @return name of the algorithm
     */
    public static String tcgAlgIdToName(final int algId) {

        TcgAlgRegLookups.TcgHashAlg alg = TcgAlgRegLookups.TcgHashAlg.fromAlgId(algId);
        if (alg != null) {
            return alg.getAlgName();
        }
        return "";
    }

    /**
     * Returns the TCG defined ID via a lookup o the TCG Defined Algorithm String.
     * Lookup based upon section 6.3 for the TPM-Rev-2.0-Part-2-Structures.pdf document.
     * Only hash algorithms found in Table 7 are used.
     *
     * @param algName String to convert to an id
     * @return id of hash algorithm
     */
    public static int tcgAlgNameToId(final String algName) {

        TcgAlgRegLookups.TcgHashAlg alg = TcgAlgRegLookups.TcgHashAlg.fromAlgName(algName);
        if (alg != null) {
            return alg.getAlgId();
        }
        return 0;
    }

    /**
     * Returns the length of a given TPM ALG Identifier.
     *
     * @param algId TCG defined Algorithm identifier
     * @return length of hash data in bytes
     */
    public static int tcgAlgLength(final int algId) {

        TcgAlgRegLookups.TcgHashAlg alg = TcgAlgRegLookups.TcgHashAlg.fromAlgId(algId);
        if (alg != null) {
            return alg.getAlgLen();
        }
        return 0;
    }

    /**
     * Returns the contents of the TPMT_HA structure buffer.
     *
     * @return contents of the TPMT_HA structure.
     */
    public byte[] getBuffer() {
        return java.util.Arrays.copyOf(buffer, buffer.length);
    }

    /**
     * Readable description of the Algorithm.
     *
     * @return Readable Algorithm name
     */
    @Override
    public String toString() {
        return String.format("%s hash = %s", hashName, HexUtils.byteArrayToHexString(digest));
    }
}
