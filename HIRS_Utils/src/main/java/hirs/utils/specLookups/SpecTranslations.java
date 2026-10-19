package hirs.utils.specLookups;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class SpecTranslations {

    /** Print name for SHA1. */
    public static final String SHA1_STR = "SHA1";
    /** Print name for SHA256. */
    public static final String SHA256_STR = "SHA256";
    /** Print name for SHA384. */
    public static final String SHA384_STR = "SHA384";
    /** Print name for SHA512. */
    public static final String SHA512_STR = "SHA512";
    /** Print name for SHA3-256. */
    public static final String SHA3_256_STR = "SHA3-256";
    /** Print name for SHA3-384. */
    public static final String SHA3_384_STR = "SHA3-384";
    /** Print name for SHA3-512. */
    public static final String SHA3_512_STR = "SHA3-512";

    /** Print name for ECDSA. */
    public static final String SIG_FAM_ECC = "ECC";
    /** Print name for RSA. */
    public static final String SIG_FAM_RSA = "RSA";
    /** Print name for SHA256 with ECDSA. */
    public static final String SHA256_ECDSA_STR = "SHA256withECDSA";
    /** Print name for SHA384 with ECDSA. */
    public static final String SHA384_ECDSA_STR = "SHA384withECDSA";
    /** Print name for SHA512 with ECDSA. */
    public static final String SHA512_ECDSA_STR = "SHA512withECDSA";
    /** Print name for SHA256 with RSA. */
    public static final String SHA256_RSA_STR = "SHA256withRSA";
    /** Print name for SHA384 with RSA. */
    public static final String SHA384_RSA_STR = "SHA384withRSA";
    /** Print name for SHA512 with RSA. */
    public static final String SHA512_RSA_STR = "SHA512withRSA";
    /** Print name for SHA256 with RSA PSS. */
    public static final String SHA256_RSA_PSS_STR = "SHA256withRSAandPSS";
    /** Print name for SHA384 with RSA PSS. */
    public static final String SHA384_RSA_PSS_STR = "SHA384withRSAandPSS";
    /** Print name for SHA512 with RSA PSS. */
    public static final String SHA512_RSA_PSS_STR = "SHA512withRSAandPSS";

    @Getter
    @AllArgsConstructor
    public enum HashAlg {

        // Specification order: Print, TCG, X509, XML, CoSWID, COSE
        SHA1(SHA1_STR, TcgAlgRegLookups.SHA1_STR, X509Lookups.SHA1_STR, XmlLookups.SHA1_STR, CoswidLookups.SHA1_STR, CoseLookups.SHA1_STR),
        SHA256(SHA256_STR, TcgAlgRegLookups.SHA256_STR, X509Lookups.SHA256_STR, XmlLookups.SHA256_STR, CoswidLookups.SHA256_STR, CoseLookups.SHA256_STR),
        SHA384(SHA384_STR, TcgAlgRegLookups.SHA384_STR, X509Lookups.SHA384_STR, XmlLookups.SHA384_STR, CoswidLookups.SHA384_STR, CoseLookups.SHA384_STR),
        SHA512(SHA512_STR, TcgAlgRegLookups.SHA512_STR, X509Lookups.SHA512_STR, XmlLookups.SHA512_STR, CoswidLookups.SHA512_STR, CoseLookups.SHA512_STR),
        SHA3_256(SHA3_256_STR, TcgAlgRegLookups.SHA3_256_STR, X509Lookups.SHA3_256_STR, XmlLookups.SHA3_256_STR, CoswidLookups.SHA3_256_STR, CoseLookups.SHA3_256_STR),
        SHA3_384(SHA3_384_STR, TcgAlgRegLookups.SHA3_384_STR, X509Lookups.SHA3_384_STR, XmlLookups.SHA3_384_STR, CoswidLookups.SHA3_384_STR, CoseLookups.SHA3_384_STR),
        SHA3_512(SHA3_512_STR, TcgAlgRegLookups.SHA3_512_STR, X509Lookups.SHA3_512_STR, XmlLookups.SHA3_512_STR, CoswidLookups.SHA3_512_STR, CoseLookups.SHA3_512_STR);

        private final String algPrintStr;
        private final String algTcg;
        private final String algX509;
        private final String algXml;
        private final String algCoSwid;
        private final String algCose;

        // Outer constants mapping to integer indexes for readability
        public static final int PRINT = 0;
        public static final int TCG = 1;
        public static final int X509 = 2;
        public static final int XML = 3;
        public static final int COSWID = 4;
        public static final int COSE = 5;

        // Helper to represent a row as an indexed array
        private String[] asArray() {
            return new String[]{algPrintStr, algTcg, algX509, algXml, algCoSwid, algCose};
        }

        /**
         * Translates an algorithm identifier from one specification format to another.
         *
         * @param sourceSpec Index of the source format (e.g., HashAlg.TCG)
         * @param originalAlg  The string value to look up
         * @param targetSpec      Index of the target format (e.g., HashAlg.XML)
         * @return The translated format string, or null if not found
         */
        public static String translate(int sourceSpec, String originalAlg, int targetSpec) {
            if (originalAlg == null) {
                return null;
            }

            // Pull a reference array length to determine max bounds dynamically
            int columnCount = HashAlg.values()[0].asArray().length;

            // Validate input ranges dynamically
            if (sourceSpec < 0 || sourceSpec >= columnCount) {
                throw new IllegalArgumentException("Source Spec index out of bounds. Valid range: 0 to " + (columnCount - 1));
            }
            if (targetSpec < 0 || targetSpec >= columnCount) {
                throw new IllegalArgumentException("Target Spec index out of bounds. Valid range: 0 to " + (columnCount - 1));
            }

            // Loop through rows to find the match at the specific input column
            for (HashAlg alg : HashAlg.values()) {
                String[] row = alg.asArray();

                // Only check the cell belonging to the originalSpec index
                if (originalAlg.equals(row[sourceSpec])) {
                    return row[targetSpec];
                }
            }

            return null; // Algorithm string not found at the given column index
        }
    }

    @Getter
    @AllArgsConstructor
    public enum SigAlg {

        // Specification order: Print, Signature family(Ecc vs Rsa), TCG assymmetric & TCG hash, X509, XML, CoSWID, COSE
        //
        // Note: TCG does not combine assymmetric & hash into one name for a signing algorithm,
        //       so table combines these 2 names with a *
        SHA256_ECDSA(SHA256_ECDSA_STR, SIG_FAM_ECC, TcgAlgRegLookups.SHA256_ECDSA_STR, X509Lookups.SHA256_ECDSA_STR, XmlLookups.SHA256_ECDSA_STR, CoseLookups.SHA256_ECDSA_STR),
        SHA384_ECDSA(SHA384_ECDSA_STR, SIG_FAM_ECC, TcgAlgRegLookups.SHA384_ECDSA_STR, X509Lookups.SHA384_ECDSA_STR, XmlLookups.SHA384_ECDSA_STR, CoseLookups.SHA384_ECDSA_STR),
        SHA512_ECDSA(SHA384_ECDSA_STR, SIG_FAM_ECC, TcgAlgRegLookups.SHA384_ECDSA_STR, X509Lookups.SHA384_ECDSA_STR, XmlLookups.SHA384_ECDSA_STR, CoseLookups.SHA384_ECDSA_STR),
        SHA256_RSA_STR(SHA384_ECDSA_STR, SIG_FAM_RSA, TcgAlgRegLookups.SHA256_RSA_STR, X509Lookups.SHA256_RSA_STR, XmlLookups.SHA256_RSA_STR, CoseLookups.SHA256_RSA_STR),
        SHA384_RSA_STR(SHA384_ECDSA_STR, SIG_FAM_RSA, TcgAlgRegLookups.SHA384_RSA_STR, X509Lookups.SHA384_RSA_STR, XmlLookups.SHA384_RSA_STR, CoseLookups.SHA384_RSA_STR),
        SHA512_RSA_STR(SHA384_ECDSA_STR, SIG_FAM_RSA, TcgAlgRegLookups.SHA512_RSA_STR, X509Lookups.SHA512_RSA_STR, XmlLookups.SHA512_RSA_STR, CoseLookups.SHA512_RSA_STR),
        SHA256_RSA_PSS_STR(SHA384_ECDSA_STR, SIG_FAM_RSA, TcgAlgRegLookups.SHA256_RSA_PSS_STR, X509Lookups.SHA256_RSA_PSS_STR, XmlLookups.SHA256_RSA_PSS_STR, CoseLookups.SHA256_RSA_PSS_STR),
        SHA384_RSA_PSS_STR(SHA384_ECDSA_STR, SIG_FAM_RSA, TcgAlgRegLookups.SHA384_RSA_PSS_STR, X509Lookups.SHA384_RSA_PSS_STR, XmlLookups.SHA384_RSA_PSS_STR, CoseLookups.SHA384_RSA_PSS_STR),
        SHA512_RSA_PSS_STR(SHA384_ECDSA_STR, SIG_FAM_RSA, TcgAlgRegLookups.SHA512_RSA_PSS_STR, X509Lookups.SHA512_RSA_PSS_STR, XmlLookups.SHA512_RSA_PSS_STR, CoseLookups.SHA512_RSA_PSS_STR);

        private final String algPrintStr;
        private final String algFamily;
        private final String algTcg;
        private final String algX509;
        private final String algXml;
        private final String algCose;

        // Outer constants mapping to integer indexes for readability
        public static final int PRINT = 0;
        public static final int FAMILY = 1;
        public static final int TCG = 2;
        public static final int X509 = 3;
        public static final int XML = 4;
        public static final int COSE = 5;

        // Helper to represent a row as an indexed array
        private String[] asArray() {
            return new String[]{algPrintStr, algFamily, algTcg, algX509, algXml, algCose};
        }

    }
}
