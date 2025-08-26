package hirs.utils.crypto;

import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Class for testing TCG RIM Tool's helper class for translating hash algorithms among the specs.
 */
public class AlgorithmsIdsTest {

    //HASH SHA512
    private static final String HASH_ALG_TCG_SHA512 = "TPM_ALG_SHA512";
    private static final String HASH_ALG_XML_SHA512 = "SHA-512";
    //HASH SHA3-256
    private static final String HASH_ALG_TCG_SHA3_256 = "TPM_ALG_SHA3_256";
    //SIGNATURE ECDSA & SHA512
    private static final String SIG_ALG_TCG_ECDSA_SHA512 = "TPM_ALG_ECDSA*TPM_ALG_SHA512";
    private static final String SIG_ALG_COSE_ECDSA_SHA512 = "ES512";
    private static final String SIG_ALG_TCG_RSAPSS_SHA256 = "TPM_ALG_RSAPSS*TPM_ALG_SHA256";
    //Invalid types
    private static final String INVALID_ALG_TYPE = "InvalidAlgType";
    private static final int INVALID_SPEC = 9;
    private static final String ALG_TCG_INVALID = "TPM_ALG_INVALID";

    /**
     * Tests the translation of a hash or signature algorithm name from one specification to another
     * specification.
     *
     * @throws NoSuchAlgorithmException if an unknown algorithm type is encountered.
     */
    @Test
    public final void testTranslateAlgId() throws NoSuchAlgorithmException {

        // Test 1 translate values in hash alg table
        String hashAlgXmlSha512 = AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_HASH,
                AlgorithmsIds.SPEC_TCG_ALG, HASH_ALG_TCG_SHA512, AlgorithmsIds.SPEC_XML_ALG);
        String hashAlgTcgSha512 = AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_HASH,
                AlgorithmsIds.SPEC_XML_ALG, HASH_ALG_XML_SHA512, AlgorithmsIds.SPEC_TCG_ALG);
        assertEquals(HASH_ALG_XML_SHA512, hashAlgXmlSha512);
        assertEquals(HASH_ALG_TCG_SHA512, hashAlgTcgSha512);

        // Test 1 translate values in signature alg table
        String sigAlgCoseSha512 = AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_SIG,
                AlgorithmsIds.SPEC_TCG_ALG, SIG_ALG_TCG_ECDSA_SHA512, AlgorithmsIds.SPEC_COSE_ALG);
        String sigAlgTcgSha512 = AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_SIG,
                AlgorithmsIds.SPEC_COSE_ALG, SIG_ALG_COSE_ECDSA_SHA512, AlgorithmsIds.SPEC_TCG_ALG);
        assertEquals(SIG_ALG_COSE_ECDSA_SHA512, sigAlgCoseSha512);
        assertEquals(SIG_ALG_TCG_ECDSA_SHA512, sigAlgTcgSha512);
    }

    /**
     * Tests the translation of an algorithm name when there is an invalid algorithm type.
     */
    @Test
    public final void testTranslateAlgIdInvalidAlgType() {

        assertThrows(NoSuchAlgorithmException.class, () ->
                AlgorithmsIds.translateAlgId(INVALID_ALG_TYPE,
                        AlgorithmsIds.SPEC_TCG_ALG, HASH_ALG_TCG_SHA512, AlgorithmsIds.SPEC_XML_ALG)
        );
    }

    /**
     * Tests the translation of an algorithm name when there is an invalid original specification to search.
     */
    @Test
    public final void testTranslateAlgIdInvalidOrigSpec() {

        assertThrows(IllegalArgumentException.class, () ->
                AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_HASH,
                        INVALID_SPEC, HASH_ALG_TCG_SHA512, AlgorithmsIds.SPEC_XML_ALG)
        );
    }

    /**
     * Tests the translation of an algorithm name when there is an invalid new specification to search.
     */
    @Test
    public final void testTranslateAlgIdInvalidNewSpec() {

        assertThrows(IllegalArgumentException.class, () ->
                AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_HASH,
                        AlgorithmsIds.SPEC_TCG_ALG, HASH_ALG_TCG_SHA512, INVALID_SPEC)
        );
    }

    /**
     * Tests the translation of an algorithm name when there is an invalid algorithm name.
     */
    @Test
    public final void testTranslateAlgIdInvalidAlg() {

        // Test 1 no such hash alg in this original spec
        assertThrows(NoSuchElementException.class, () ->
                AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_HASH,
                        AlgorithmsIds.SPEC_TCG_ALG, ALG_TCG_INVALID,
                        AlgorithmsIds.SPEC_XML_ALG)
        );
        // Test 2 no such signing alg in this original spec
        assertThrows(NoSuchElementException.class, () ->
                AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_SIG,
                        AlgorithmsIds.SPEC_TCG_ALG, ALG_TCG_INVALID,
                        AlgorithmsIds.SPEC_XML_ALG)
        );

        // Test 3 no coswid signing alg in the new spec
        assertThrows(NoSuchElementException.class, () ->
                AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_SIG,
                        AlgorithmsIds.SPEC_TCG_ALG, SIG_ALG_TCG_ECDSA_SHA512,
                        AlgorithmsIds.SPEC_COSWID_ALG)
        );

        // Test 4 no such hash alg in the new spec
        assertThrows(NoSuchElementException.class, () ->
                AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_HASH,
                        AlgorithmsIds.SPEC_TCG_ALG, HASH_ALG_TCG_SHA3_256,
                        AlgorithmsIds.SPEC_XML_ALG)
        );
        // Test 5 no such signature alg in the new spec
        assertThrows(NoSuchElementException.class, () ->
                AlgorithmsIds.translateAlgId(AlgorithmsIds.ALG_TYPE_SIG,
                        AlgorithmsIds.SPEC_TCG_ALG, SIG_ALG_TCG_RSAPSS_SHA256,
                        AlgorithmsIds.SPEC_XML_ALG)
        );
    }
}
