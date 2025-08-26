package hirs.utils.crypto;

import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;

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
 *   <li>CoSwid (Named Information Hash Algorithm Registry):
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

public final class AlgorithmsIds {

    /**
     * Hash Algorithm.
     */
    public static final String ALG_TYPE_HASH = "hash";
    /**
     * Signature Algorithm.
     */
    public static final String ALG_TYPE_SIG = "signature";
    /**
     * Algorithm from TCG Spec (matches with table column).
     */
    public static final int SPEC_TCG_ALG = 0;
    /**
     * Algorithm from XML Spec (matches with table column).
     */
    public static final int SPEC_XML_ALG = 1;
    /**
     * Algorithm from CoSwid Spec (matches with table column).
     */
    public static final int SPEC_COSWID_ALG = 2;
    /**
     * Algorithm from COSE Spec (matches with table column).
     */
    public static final int SPEC_COSE_ALG = 3;
    /**
     * String Description Column.
     */
    public static final int STR_DESC_COL = 4;

    /**
     * Algorithm from X.509.
     */
    public static final int SPEC_X509_ALG = 5;
    /**
     * Algorithm Family (Such as "ECC", "RSA", etc).
     */
    public static final int FAMILY_ALG = 6;

    /**
     * Array that holds the human-readable name of the spec, in the same column
     * order as the subsequent tables.
     */
    private static final String[][] ALG_TABLES_SPEC_COLUMNS = {
            // Specification order: TCG, XML, CoSwid, COSE, X509
            {"TCG", "XML", "CoSwid", "COSE", "X509"},
            {"TCG Algorithm Registry", "XML Security Algorithm Cross-Reference",
                    "Named Information Hash Algorithm Registry", "COSE Header Algorithm Parameters",
                    "Java X509Certificate algorithm identifiers"}};

    /**
     * Array that holds the hash alg names used in each of 4 specifications.
     */
    private static final String[][] HASH_ALGORITHMS = {
            // Specification order: TCG, XML, CoSwid, COSE, X509 string
            // description
            {"TPM_ALG_SHA1", "SHA-1", "", "SHA-1", "SHA1", "SHA1"},
            {"TPM_ALG_SHA256", "SHA-256", "sha-256", "SHA-256", "SHA256", "SHA256"},
            {"TPM_ALG_SHA384", "SHA-384", "sha-384", "SHA-384", "SHA384", "SHA384"},
            {"TPM_ALG_SHA512", "SHA-512", "sha-512", "SHA-512", "SHA512", "SHA512"},
            {"TPM_ALG_SHA3_256", "", "sha3-256", "", "SHA3-256", "SHA3256"},
            {"TPM_ALG_SHA3_384", "", "sha3-384", "", "SHA3-384", "SHA384"},
            {"TPM_ALG_SHA3_512", "", "sha3-512", "SHA3-512", "SHA512"}};

    /**
     * Array that holds the signing alg names used in each of 4 specifications.
     */
    private static final String[][] SIG_ALGORITHMS = {
            // RFC8017: Signature scheme for RSASSA-PSS and RSASSA-PKCS1-v1_5, Sections 8.1 and 8.2
            //    https://datatracker.ietf.org/doc/html/rfc8017
            //    https://www.rfc-editor.org/rfc/rfc8017
            // TPM
            //    TPM_ALG_RSASSA:
            //       RSASSA-PKCS1-v1_5 combines RSASP1 and RSAVP1 primitives w/ EMSA-PKCS1-v1_5 encoding
            //                          method
            //    TPM_ALG_RSAPSS:
            //       RSASSA-PSS combines RSASP1 and RSAVP1 primitives w/ EMSA-PSS encoding method
            // XML RSA-SHA256, RSA-SHA384, RSA-SHA512:
            //    This implies the PKCS#1 v1.5 padding algorithm [RFC3447]
            //    XML doesn't account for PSS padding so these will be blank
            // COSWID
            //    COSWID has only hash algorithms so there's no column for this
            //
            // COSE
            //    RS256: RSASSA-PKCS1-v1_5 using SHA-256
            //    PS256: RSASSA-PSS w/ SHA-256

            // Specification order: TCG assymmetric & TCG hash, XML, CoSwid, COSE, X509 string descriptions,
            //                          Signature family(Ecc vd Rsa)
            // Note: TCG does not combine assymmetric & hash into one name for a signing algorithm,
            //       so table combines these 2 names with a * (in the first columnn)
            //       CoSwid empty column left in for column # alignment consistent with the other tables
            {"TPM_ALG_ECDSA*TPM_ALG_SHA256", "ECDSA-SHA256", "", "ES256", "ECDSA and SHA256",
                    "SHA256withECDSA", "ECC"},
            {"TPM_ALG_ECDSA*TPM_ALG_SHA384", "ECDSA-SHA384", "", "ES384", "ECDSA and SHA384",
                    "SHA384withECDSA", "ECC"},
            {"TPM_ALG_ECDSA*TPM_ALG_SHA512", "ECDSA-SHA512", "", "ES512", "ECDSA and SHA512",
                    "SHA512withECDSA", "ECC"},
            {"TPM_ALG_RSASSA*TPM_ALG_SHA256", "RSA-SHA256", "", "RS256",
                    "RSA with PKCS1-v1_5 padding and SHA256", "SHA256withRSA", "RSA"},
            {"TPM_ALG_RSASSA*TPM_ALG_SHA384", "RSA-SHA384", "", "RS384",
                    "RSA with PKCS1-v1_5 padding and SHA384", "SHA384withRSA", "RSA"},
            {"TPM_ALG_RSASSA*TPM_ALG_SHA512", "RSA-SHA512", "", "RS512",
                    "RSA with PKCS1-v1_5 padding and SHA512", "SHA512withRSA", "RSA"},
            {"TPM_ALG_RSAPSS*TPM_ALG_SHA256", "", "", "PS256", "RSA with PSS padding and SHA256", "TBD",
                    "RSA"},
            {"TPM_ALG_RSAPSS*TPM_ALG_SHA384", "", "", "PS384", "RSA with PSS padding and SHA384", "TBD",
                    "RSA"},
            {"TPM_ALG_RSAPSS*TPM_ALG_SHA512", "", "", "PS512", "RSA with PSS padding and SHA512", "TBD",
                    "RSA"}};

    /**
     * Default constructor.
     */
    private AlgorithmsIds() {

    }

    /**
     * Searches algorithm array for match to original spec's alg string,
     * translates that to desired spec alg name.
     *
     * @param algType type of algorithm (hash, signature)
     * @param originalSpec int id of specification for original algorithm
     * @param originalAlg string id of original algorithm
     * @param newSpec int id of specification for new algorithm
     * @throws NoSuchAlgorithmException if the original algorithm cannot be
     * found
     * @return Name of new algorithm ID
     */
    public static String translateAlgId(final String algType, final int originalSpec,
                                        final String originalAlg, final int newSpec)
            throws NoSuchAlgorithmException {

        String newAlgId = "";

        if ((newSpec != SPEC_TCG_ALG) && (newSpec != SPEC_XML_ALG) && (newSpec != SPEC_COSWID_ALG)
                && (newSpec != SPEC_COSE_ALG) && (newSpec != SPEC_X509_ALG)) {
            throw new IllegalArgumentException("Invalid new spec");
        }

        final int algIdRow = findAlgId(algType, originalSpec, originalAlg);
        if (algIdRow >= 0) {
            if (algType.compareTo(ALG_TYPE_HASH) == 0) {
                newAlgId = HASH_ALGORITHMS[algIdRow][newSpec];
            } else if (algType.compareTo(ALG_TYPE_SIG) == 0) {
                newAlgId = SIG_ALGORITHMS[algIdRow][newSpec];
            }

            if (newAlgId.compareTo("") == 0) {
                throw new NoSuchElementException("Algorithm " + algType + " from "
                        + ALG_TABLES_SPEC_COLUMNS[0][originalSpec] + " spec is not defined in "
                        + ALG_TABLES_SPEC_COLUMNS[0][newSpec] + " spec");
            }
        }

        return newAlgId;
    }

    /**
     * Searches algorithm array for match to spec's alg string, returns true if
     * found.
     *
     * @param algType type of algorithm (hash, signature)
     * @param spec int id of specification for algorithm
     * @param alg string id of algorithm
     * @return true if alg found
     * @throws NoSuchAlgorithmException if the original algorithm is not found
     */
    public static boolean isValid(final String algType, final int spec, final String alg)
            throws NoSuchAlgorithmException {
        return findAlgId(algType, spec, alg) >= 0;
    }

    /**
     * Searches algorithm array for match to spec's alg string, returns row in
     * array where found.
     *
     * @param algType type of algorithm (hash, signature)
     * @param spec int id of specification for original algorithm
     * @param alg string id of algorithm
     * @return row in array if alg found, -1 if not found
     * @throws NoSuchAlgorithmException if the original algorithm type is
     * invalid
     */
    public static int findAlgId(final String algType, final int spec, final String alg)
            throws NoSuchAlgorithmException {

        int index = -1;

        if ((spec != SPEC_TCG_ALG) && (spec != SPEC_XML_ALG) && (spec != SPEC_COSWID_ALG)
                && (spec != SPEC_COSE_ALG) && (spec != SPEC_X509_ALG)) {
            throw new IllegalArgumentException("Invalid original spec");

        }

        if (algType.compareTo(ALG_TYPE_HASH) == 0) {
            for (int i = 0; i < HASH_ALGORITHMS.length; i++) {
                if (alg.compareTo(HASH_ALGORITHMS[i][spec]) == 0) {
                    index = i;
                }
            }
        } else if (algType.compareTo(ALG_TYPE_SIG) == 0) {
            if (spec == SPEC_COSWID_ALG) {
                throw new NoSuchElementException("There is no COSWID signing algorithm");
            }
            for (int i = 0; i < SIG_ALGORITHMS.length; i++) {
                if (alg.compareTo(SIG_ALGORITHMS[i][spec]) == 0) {
                    index = i;
                }
            }
        } else {
            throw new NoSuchAlgorithmException("Invalid algorithm type " + algType);
        }
        if (index == -1) {
            throw new NoSuchElementException("Algorithm " + algType + " is not defined in "
                    + ALG_TABLES_SPEC_COLUMNS[0][spec] + " spec");
        }
        return index;
    }

    /**
     * Returns string with name of specification for algorithm and name of
     * algorithm.
     *
     * @param algType type of algorithm (hash, signature)
     * @param originalSpec int id of specification for original algorithm
     * @param originalAlg string id of original algorithm
     * @param newSpec int id of specification for new algorithm
     * @return human-readable name of spec and algorithm
     * @throws NoSuchAlgorithmException if the original algorithm cannot be
     * found
     */
    public static String toString(final String algType, final int originalSpec, final String originalAlg,
                                  final int newSpec) throws NoSuchAlgorithmException {
        final String newAlg = translateAlgId(algType, originalSpec, originalAlg, newSpec);

        return "Original specification: " + ALG_TABLES_SPEC_COLUMNS[0][originalSpec] + " ("
                + ALG_TABLES_SPEC_COLUMNS[1][originalSpec] + ")\nOriginal " + algType + " algorithm: "
                + originalAlg + "\nNew specification: " + ALG_TABLES_SPEC_COLUMNS[0][newSpec] + " ("
                + ALG_TABLES_SPEC_COLUMNS[1][newSpec] + ")\nNew " + algType + " algorithm: " + newAlg
                + "\nDescription of algorithm: " + STR_DESC_COL;
    }

    /**
     * Returns the algorithm family for a given algorithm in the specified
     * specification.
     *
     * @param spec int id of specification for original algorithm
     * @param alg string id of algorithm
     * @return string of algorithm family
     * @throws NoSuchAlgorithmException if the original algorithm cannot be
     * found
     */
    public static String algFamily(final int spec, final String alg) throws NoSuchAlgorithmException {
        final int row = findAlgId(ALG_TYPE_SIG, spec, alg);
        return SIG_ALGORITHMS[row][FAMILY_ALG];
    }

    /**
     * Returns true if the algorithm is from the ECC family.
     *
     * @param spec int id of specification for original algorithm
     * @param alg string id of algorithm
     * @return true if algorithm is ECC
     * @throws NoSuchAlgorithmException if the original algorithm cannot be
     * found
     */
    public static boolean isEcc(final int spec, final String alg) throws NoSuchAlgorithmException {
        final String algFamily = algFamily(spec, alg);
        return algFamily.compareToIgnoreCase("ECC") == 0;
    }

    /**
     * Returns true if the algorithm is from the RSA family.
     *
     * @param spec int id of specification for original algorithm
     * @param alg string id of algorithm
     * @return true if algorithm is RSA
     * @throws NoSuchAlgorithmException if the original algorithm cannot be
     * found
     */
    public static boolean isRsa(final int spec, final String alg) throws NoSuchAlgorithmException {
        final String algFamily = algFamily(spec, alg);
        return algFamily.compareToIgnoreCase("RSA") == 0;
    }
}
