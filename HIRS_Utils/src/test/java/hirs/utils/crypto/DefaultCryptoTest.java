package hirs.utils.crypto;

import hirs.utils.signature.cose.CoseAlgorithm;
import hirs.utils.signature.cose.CoseSignature;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the {@link DefaultCrypto} class. This class tests various aspects of the crypto
 * implementation, including key loading, signing, and signature processing.
 * <p>
 * Major signing and verification methods are tested in system tests and are not included here.
 */
public class DefaultCryptoTest {

    /**
     *  Test the load of a JSON Web Key used for testing.
     *  Uses the algorithm ID and kid that the JSON Web Key provides to prove it was loaded and ready for use.
     *  <p>
     *  Note that PEM key loads are tested in {@link hirs.rim.signature.cose.CoseSignatureTest}.
     */
    @Test
    public final void testLoadJsonWebTokenEccPrivateKey()  throws Exception {
        String keyFile = "src/test/resources/keys/signed-01.json.key";

        DefaultCrypto crypto = new DefaultCrypto();
        crypto.loadPrivateKey(keyFile, null, "");

        String alg = crypto.getAlgorithm();
        String kid = crypto.getKid();

        assertEquals("ES256", alg);
        assertEquals("11", kid);
    }

    /**
     * Test loading a PEM private key (with cert option). Uses the getAlgorithm() method to test that the key
     * was loaded.
     * @throws Exception
     */
    @Test
    public final void testLoadPemPrivateKeyWithCert()  throws Exception {
        String keyFile = "src/test/resources/keys/COMP_OEM1_rim_signer_rsa_3k_sha384.key";
        String certFile = "certificates/COMP_OEM1_rim_signer_rsa_3k_sha384.pem";
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream certStream = classLoader.getResourceAsStream(certFile);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) certFactory.generateCertificate(certStream);

        DefaultCrypto crypto = new DefaultCrypto();
        crypto.loadPrivateKey(keyFile, cert, "");

        String alg = crypto.getAlgorithm();

        assertEquals("RS384", alg);

    }
    /**
     * Tests loading a PEM private key (with alg option). Uses the getAlgorithm() method to test that the key
     * was loaded.
     * @throws Exception
     */
    @Test
    public final void testLoadPemPrivateKeyWithAlg()  throws Exception {
        String keyFile = "src/test/resources/keys/COMP_OEM1_rim_signer_rsa_3k_sha384.key";
        String certFile = "certificates/COMP_OEM1_rim_signer_rsa_3k_sha384.pem";

        DefaultCrypto crypto = new DefaultCrypto();
        crypto.loadPrivateKey(keyFile, null, "RS384");

        String alg = crypto.getAlgorithm();

        assertEquals("RS384", alg);

    }

    /**
     * Tests the derEncodeRawSignature() method.
     * <ol>
     *   <li>Removes DER encoding introduced by java.security when signing data.</li>
     *   <li>Converts data to a "raw" format used by COSE.</li>
     *   <li>Notes: ECC and RSA-PSS use random inputs, making it impossible to compare against known
     *   data.</li>
     * </ol>
     */
    @Test
    public final void testRemoveDerFromSignature() throws Exception {
        String keyFile = "src/test/resources/keys/COMP_OEM1_rim_signer_rsa_3k_sha384.key";
        String certFile = "certificates/COMP_OEM1_rim_signer_rsa_3k_sha384.pem";
        String contentFile = "cose/sign-pass-cose-content.bin";

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream contStream = classLoader.getResourceAsStream(contentFile);
        byte[] contData = contStream.readAllBytes();
        InputStream certStream = classLoader.getResourceAsStream(certFile);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) certFactory.generateCertificate(certStream);

        DefaultCrypto crypto = new DefaultCrypto();
        crypto.loadPrivateKey(keyFile, cert, "");

        CoseSignature coseSig = new CoseSignature();

        byte[] toBeSigned = coseSig.createToBeSigned(CoseAlgorithm.getAlgId("ES256"),
                "11".getBytes(StandardCharsets.UTF_8), contData, cert, true, false,
                "test");

        byte[] signedData = crypto.sign(toBeSigned);
        final int signedLength = 384;

        assertEquals(signedLength, signedData.length);
    }
}
