package hirs.utils.specLookups;

import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Class for testing class that translates hash algorithms among the specs.
 */
public class SpecTranslationsTest {

    //Invalid types
    private static final String INVALID_ALG_TYPE = "InvalidAlgType";

    /**
     * Tests the translation of a hash or signature algorithm name from one specification to another
     * specification.
     *
     * @throws NoSuchAlgorithmException if an unknown algorithm type is encountered.
     */
    @Test
    public final void testTranslateAlg() throws NoSuchAlgorithmException {

        // Test 1 translate values in hash alg table from TCG to XML and visa-versa
        String hashAlgXmlSha512 = SpecTranslations.translate(
                SpecTranslations.HashAlg.class,
                SpecTranslations.HashAlg.Format.TCG,
                TcgAlgRegLookups.SHA512_STR,
                SpecTranslations.HashAlg.Format.XML
        );
        String hashAlgTcgSha512 = SpecTranslations.translate(
                SpecTranslations.HashAlg.class,
                SpecTranslations.HashAlg.Format.XML,
                XmlLookups.SHA512_STR,
                SpecTranslations.HashAlg.Format.TCG
        );
        assertEquals(XmlLookups.SHA512_STR, hashAlgXmlSha512);
        assertEquals(TcgAlgRegLookups.SHA512_STR, hashAlgTcgSha512);

        // Test 2 translate values in signature alg table from TCG to COSE and visa-versa
        String sigAlgCoseSha512 = SpecTranslations.translate(
                SpecTranslations.SigAlg.class,
                SpecTranslations.SigAlg.Format.TCG,
                TcgAlgRegLookups.SHA512_ECDSA_STR,
                SpecTranslations.SigAlg.Format.COSE
        );
        String sigAlgTcgSha512 = SpecTranslations.translate(
                SpecTranslations.SigAlg.class,
                SpecTranslations.SigAlg.Format.COSE,
                CoseLookups.SHA512_ECDSA_STR,
                SpecTranslations.SigAlg.Format.TCG
        );
        assertEquals(CoseLookups.SHA512_ECDSA_STR, sigAlgCoseSha512);
        assertEquals(TcgAlgRegLookups.SHA512_ECDSA_STR, sigAlgTcgSha512);
    }

    /**
     * Tests the translation of an algorithm name when there is an invalid algorithm name.
     */
    @Test
    public final void testInvalidAlg() {

        // Test 1 no such hash alg in this original spec
        assertNull(SpecTranslations.translate(
                SpecTranslations.HashAlg.class,
                SpecTranslations.HashAlg.Format.TCG,
                INVALID_ALG_TYPE,
                SpecTranslations.HashAlg.Format.XML
        ));

        // Test 2 no such signing alg in this original spec
        assertNull(SpecTranslations.translate(
                SpecTranslations.SigAlg.class,
                SpecTranslations.SigAlg.Format.TCG,
                INVALID_ALG_TYPE,
                SpecTranslations.SigAlg.Format.XML
        ));
    }
}
