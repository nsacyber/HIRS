package hirs.utils.specLookups;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Class to translate algorithm identifiers whose names differ between the following specs.
 * <ul>
 *   <li>TCG (TCG Algorithm Registry):<br>
 *     <ul>
 *       <li>https://trustedcomputinggroup.org/resource/tcg-algorithm-registry/<br>
 *       Table 3 - Definition of (UINT16) TPM_ALG_ID Constants</li>
 *     </ul>
 *   </li>
 *   <li>XML (XML Security Algorithm Cross-Reference):
 *     <ul>
 *       <li>https://www.w3.org/TR/xmlsec-algorithms/</li>
 *     </ul>
 *   </li>
 *   <li>RFC6931 (Additional XML Security Uniform Resource Identifiers (URIs)):
 *     <ul>
 *       <li>https://datatracker.ietf.org/doc/rfc6931/</li>
 *     </ul>
 *   </li>
 *   <li>CoSWID (Named Information Hash Algorithm Registry):
 *     <ul>
 *       <li>https://www.rfc-editor.org/rfc/rfc9393.html</li>
 *       <li>https://www.iana.org/assignments/named-information/named-information.xhtml</li>
 *     </ul>
 *   </li>
 *   <li>COSE (COSE Header Algorithm Parameters):<br>
 *     <ul>
 *       <li>https://www.iana.org/assignments/cose/cose.xhtml#algorithms</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>Notes:</p>
 * <ul>
 *   <li>Only includes asymmetric signature algorithms and hash algorithms.</li>
 *   <li>Only includes algorithms listed in the TCG Registry, since RIMs are defined by
 *       the TCG and must use TCG-defined algorithms.</li>
 * </ul>
 */
public final class SpecTranslations {

    /**
     * Default constructor.
     */
    private SpecTranslations() {
    }

    /**
     * Shared interface implemented by enums that provide array-based indexing mappings.
     */
    public interface TranslatableAlg {
        /** String array mapped to enum value.
         *
         * @return enum value as string array
         * **/
        String[] asArray();
    }

    /**
     * Shared interface for all column definition enums.
     */
    public interface ColumnSpec {
        /** Index of 'column' for enum value (mapped within the enum Format which implements ColumnSpec).
         *
         * @return index
         * **/
        int getIndex();
    }

    /**
     * Translates an algorithm identifier from one specification format to another for any supported Enum class.
     * This method works with any type T, as long as T is an Enum and it implements the TranslatableAlg interface.
     *
     * @param <T> Specifies enum for desired algorithm type (hash, signature)
     * @param enumClass  The specific Enum class to search through (e.g., HashAlg.class or SigAlg.class)
     * @param sourceSpec Index of the source format
     * @param sourceAlg The string value to look up
     * @param targetSpec Index of the target format
     * @return The translated format string, or null if not found
     */
    public static <T extends Enum<T> & TranslatableAlg> String translate(
            final Class<T> enumClass, final ColumnSpec sourceSpec, final String sourceAlg,
            final ColumnSpec targetSpec) {

        // check that source algorithm is not null
        if (sourceAlg == null) {
            return null;
        }

        // Whatever specific Enum class was passed into the enumClass variable: pull out its list of constants
        // (similar to the .values() method when the enumClass is known)
        T[] values = enumClass.getEnumConstants();

        // Check that there are values in the enum
        if (values == null) {
            return null;
        }

        // Loop through rows
        for (T alg : values) {
            String[] row = alg.asArray();
            if (sourceAlg.equals(row[sourceSpec.getIndex()])) {
                return row[targetSpec.getIndex()];
            }
        }
        return null;
    }

    @Getter
    @AllArgsConstructor
    public enum HashAlg implements TranslatableAlg {

        // Hash Algorithm Specification order: Print, TCG, X509, XML, CoSWID, COSE
        /** SHA1. **/
        SHA1("SHA1", TcgAlgRegLookups.SHA1_STR, X509Lookups.SHA1_STR, XmlLookups.SHA1_STR,
                CoswidLookups.SHA1_STR, CoseLookups.SHA1_STR),
        /** SHA256. **/
        SHA256("SHA256", TcgAlgRegLookups.SHA256_STR, X509Lookups.SHA256_STR, XmlLookups.SHA256_STR,
                CoswidLookups.SHA256_STR, CoseLookups.SHA256_STR),
        /** SHA384. **/
        SHA384("SHA384", TcgAlgRegLookups.SHA384_STR, X509Lookups.SHA384_STR, XmlLookups.SHA384_STR,
                CoswidLookups.SHA384_STR, CoseLookups.SHA384_STR),
        /** SHA512. **/
        SHA512("SHA512", TcgAlgRegLookups.SHA512_STR, X509Lookups.SHA512_STR, XmlLookups.SHA512_STR,
                CoswidLookups.SHA512_STR, CoseLookups.SHA512_STR),
        /** SHA3_256. **/
        SHA3_256("SHA3-256", TcgAlgRegLookups.SHA3_256_STR, X509Lookups.SHA3_256_STR, XmlLookups.SHA3_256_STR,
                CoswidLookups.SHA3_256_STR, CoseLookups.SHA3_256_STR),
        /** SHA3_384. **/
        SHA3_384("SHA3-384", TcgAlgRegLookups.SHA3_384_STR, X509Lookups.SHA3_384_STR, XmlLookups.SHA3_384_STR,
                CoswidLookups.SHA3_384_STR, CoseLookups.SHA3_384_STR),
        /** SHA3_512. **/
        SHA3_512("SHA3-512", TcgAlgRegLookups.SHA3_512_STR, X509Lookups.SHA3_512_STR, XmlLookups.SHA3_512_STR,
                CoswidLookups.SHA3_512_STR, CoseLookups.SHA3_512_STR);

        private final String algPrintStr;
        private final String algTcg;
        private final String algX509;
        private final String algXml;
        private final String algCoSwid;
        private final String algCose;

        // Outer constants mapping to integer indexes for readability
        @AllArgsConstructor @Getter
        public enum Format implements ColumnSpec {
            /** PRINT. **/
            PRINT(0),
            /** TCG. **/
            TCG(1),
            /** X509. **/
            X509(2),
            /** XML. **/
            XML(3),
            /** COSWID. **/
            COSWID(4),
            /** COSE. **/
            COSE(5);
            private final int index;
        }

        // Helper to represent a row as an indexed array
        @Override
        public String[] asArray() {
            return new String[]{algPrintStr, algTcg, algX509, algXml, algCoSwid, algCose};
        }
    }

    @Getter
    @AllArgsConstructor
    public enum SigAlg implements TranslatableAlg {

        // Signature Algorithm Specification order:
        //      Print, Signature family (ECC vs RSA), TCG assymmetric & TCG hash,
        //      X509, XML, CoSWID, COSE
        /** SHA256_ECDSA. **/
        SHA256_ECDSA("SHA256withECDSA", "ECC", TcgAlgRegLookups.SHA256_ECDSA_STR,
                X509Lookups.SHA256_ECDSA_STR, XmlLookups.SHA256_ECDSA_STR, CoseLookups.SHA256_ECDSA_STR),
        /** SHA384_ECDSA. **/
        SHA384_ECDSA("SHA384withECDSA", "ECC", TcgAlgRegLookups.SHA384_ECDSA_STR,
                X509Lookups.SHA384_ECDSA_STR, XmlLookups.SHA384_ECDSA_STR, CoseLookups.SHA384_ECDSA_STR),
        /** SHA512_ECDSA. **/
        SHA512_ECDSA("SHA512withECDSA", "ECC", TcgAlgRegLookups.SHA512_ECDSA_STR,
                X509Lookups.SHA512_ECDSA_STR, XmlLookups.SHA512_ECDSA_STR, CoseLookups.SHA512_ECDSA_STR),
        /** SHA256_RSA_STR. **/
        SHA256_RSA_STR("SHA256withRSA", "RSA", TcgAlgRegLookups.SHA256_RSA_STR,
                X509Lookups.SHA256_RSA_STR, XmlLookups.SHA256_RSA_STR, CoseLookups.SHA256_RSA_STR),
        /** SHA384_RSA_STR. **/
        SHA384_RSA_STR("SHA384withRSA", "RSA", TcgAlgRegLookups.SHA384_RSA_STR,
                X509Lookups.SHA384_RSA_STR, XmlLookups.SHA384_RSA_STR, CoseLookups.SHA384_RSA_STR),
        /** SHA512_RSA_STR. **/
        SHA512_RSA_STR("SHA512withRSA", "RSA", TcgAlgRegLookups.SHA512_RSA_STR,
                X509Lookups.SHA512_RSA_STR, XmlLookups.SHA512_RSA_STR, CoseLookups.SHA512_RSA_STR),
        /** SHA256_RSA_PSS_STR. **/
        SHA256_RSA_PSS_STR("SHA256withRSAandPSS", "RSA", TcgAlgRegLookups.SHA256_RSA_PSS_STR,
                X509Lookups.SHA256_RSA_PSS_STR, XmlLookups.SHA256_RSA_PSS_STR, CoseLookups.SHA256_RSA_PSS_STR),
        /** SHA384_RSA_PSS_STR. **/
        SHA384_RSA_PSS_STR("SHA384withRSAandPSS", "RSA", TcgAlgRegLookups.SHA384_RSA_PSS_STR,
                X509Lookups.SHA384_RSA_PSS_STR, XmlLookups.SHA384_RSA_PSS_STR, CoseLookups.SHA384_RSA_PSS_STR),
        /** SHA512_RSA_PSS_STR. **/
        SHA512_RSA_PSS_STR("SHA512withRSAandPSS", "RSA", TcgAlgRegLookups.SHA512_RSA_PSS_STR,
                X509Lookups.SHA512_RSA_PSS_STR, XmlLookups.SHA512_RSA_PSS_STR, CoseLookups.SHA512_RSA_PSS_STR);

        private final String algPrintStr;
        private final String algFamily;
        private final String algTcg;
        private final String algX509;
        private final String algXml;
        private final String algCose;

        // Outer constants mapping to integer indexes for readability
        @AllArgsConstructor @Getter
        public enum Format implements ColumnSpec {
            /** PRINT. **/
            PRINT(0),
            /** FAMILY. **/
            FAMILY(1),
            /** TCG. **/
            TCG(2),
            /** X509. **/
            X509(3),
            /** XML. **/
            XML(4),
            /** COSE. **/
            COSE(5);
            private final int index;
        }

        // Helper to represent a row as an indexed array
        @Override
        public String[] asArray() {
            return new String[]{algPrintStr, algFamily, algTcg, algX509, algXml, algCose};
        }

        /**
         * Returns true if the algorithm is from the ECC family.
         *
         * @param spec SigAlg Format id of specification for algorithm
         * @param alg string id of algorithm
         * @return true if algorithm is ECC
         */
        public static boolean isEcc(final SpecTranslations.SigAlg.Format spec, final String alg) {
            return "ECC".equals(translate(SigAlg.class, spec, alg, Format.FAMILY));
        }

        /**
         * Returns true if the algorithm is from the RSA family.
         *
         * @param spec SigAlg Format id of specification for algorithm
         * @param alg string id of algorithm
         * @return true if algorithm is ECC
         */
        public static boolean isRsa(final SpecTranslations.SigAlg.Format spec, final String alg) {
            return "RSA".equals(translate(SigAlg.class, spec, alg, Format.FAMILY));
        }
    }
}
