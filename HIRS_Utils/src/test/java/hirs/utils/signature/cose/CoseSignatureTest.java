package hirs.utils.signature.cose;

import hirs.utils.crypto.DefaultCrypto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Tests "to be signed" data for COSE_Sign1 structures, used by TCG Component RIMs.
 * <p>
 * Uses data from https://github.com/cose-wg/Examples/tree/master/sign1-tests
 */
public class CoseSignatureTest {
    /**
     * Tests the generation of "to be verified" data from a known good test pattern.
     * <ol>
     *   <li>Reads a test COSE file into a byte array.</li>
     *   <li>Reads the expected "to be signed" data into a byte array.</li>
     *   <li>Generates "to be verified" data using the {@link CoseSignature} builder.</li>
     *   <li>Compares the generated data with the expected data and throws an error if they differ.</li>
     * </ol>
     */
    @Test
    public final void testToBeVerified() throws IOException {
        String coseFile = "cose/sign-pass-01.cose";
        String toBeSignedFile = "cose/cose-pass-01-toBeSigned.bin";

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream coseStream = classLoader.getResourceAsStream(coseFile);
        byte[] coseData;
        coseData = coseStream.readAllBytes();
        InputStream tbsStream = classLoader.getResourceAsStream(toBeSignedFile);
        byte[] tbsData;
        tbsData = tbsStream.readAllBytes();

        CoseSignature coseSig = new CoseSignature();
        byte[] toBeSigned;
        toBeSigned = coseSig.getToBeVerified(coseData);

        assertArrayEquals(tbsData, toBeSigned);
    }
    /**
     * Tests the generation of "to be signed" data from a known good test pattern.
     * <ol>
     *   <li>Reads a test COSE file into a byte array.</li>
     *   <li>Reads the expected "to be signed" data into a byte array.</li>
     *   <li>Generates "to be signed" data using the {@link CoseSignature} builder.</li>
     *   <li>Compares the generated data with the expected data and throws an error if they differ.</li>
     * </ol>
     */
    @Test
    public final void testToBeSigned() throws IOException, CertificateException, NoSuchAlgorithmException {
        String toBeSignedFile = "cose/cose-pass-03-toBeSigned.bin";
        String contentFile = "cose/sign-pass-cose-content.bin";
        String certFile = "certificates/COMP_OEM1_rim_signer_rsa_3k_sha384.pem";

        ClassLoader classLoader;
        classLoader = getClass().getClassLoader();
        InputStream tbsStream;
        tbsStream = classLoader.getResourceAsStream(toBeSignedFile);
        byte[] tbsData;
        tbsData = tbsStream.readAllBytes();
        InputStream contStream;
        contStream = classLoader.getResourceAsStream(contentFile);
        byte[] contData;
        contData = contStream.readAllBytes();
        InputStream certStream;
        certStream = classLoader.getResourceAsStream(certFile);
        CertificateFactory certFactory;
        certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert;
        cert = (X509Certificate) certFactory.generateCertificate(certStream);

        CoseSignature coseSig = new CoseSignature();
        byte[] toBeSigned = coseSig.createToBeSigned(CoseAlgorithm.getAlgId("ES256"),
                "11".getBytes(StandardCharsets.UTF_8), contData, cert, true, false,
                "test");

        assertArrayEquals(tbsData, toBeSigned);
    }

    /**
     * Tests the getSignature() method by comparing its output against expected values.
     * <ol>
     *   <li>Reads a test COSE file into a byte array.</li>
     *   <li>Reads the expected signature data into a byte array.</li>
     *   <li>Generates "to be verified" data using the CoseSignature builder.</li>
     *   <li>Extracts the signature data using getSignature() and compares it with the expected data.</li>
     * </ol>
     */
    @Test
    public final void testGetSignatureData() throws IOException {
        String coseFile = "cose/sign-pass-02.cose";
        String sigFile = "cose/cose-pass-02-sig.bin";

        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream sigStream = classLoader.getResourceAsStream(sigFile)) {
            byte[] sigData = sigStream.readAllBytes();
            try (InputStream coseStream = classLoader.getResourceAsStream(coseFile)) {
                byte[] coseData = coseStream.readAllBytes();
                CoseSignature coseSig = new CoseSignature();
                coseSig.getToBeVerified(coseData);
                assertArrayEquals(sigData, coseSig.getSignature());
            }
        }
    }

    /**
     * Tests the addSignature() method by creating a COSE_Sign1 signature and comparing it against a known
     * good test pattern.
     * <ol>
     *   <li>Creates a COSE_Sign1 signature using RSA PKCS #1.</li>
     *   <li>Signs test data and compares the result with a known good test pattern.</li>
     *   <li>Notes: ECC and RSA-PSS use random inputs, making it impossible to compare against known
     *   data.</li>
     * </ol>
     */
    @Test
    public final void testAddSignature() throws Exception {
        String keyFile = "src/test/resources/keys/COMP_OEM1_rim_signer_rsa_3k_sha384.key";
        String certFile = "certificates/COMP_OEM1_rim_signer_rsa_3k_sha384.pem";
        String contentFile = "cose/sign-pass-cose-content.bin";
        String signedFile = "cose/sign_pass_rsa_3072_sha384.cose";
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream contStream = classLoader.getResourceAsStream(contentFile);
        byte[] contData = contStream.readAllBytes();
        InputStream certStream = classLoader.getResourceAsStream(certFile);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) certFactory.generateCertificate(certStream);
        InputStream signedStream = classLoader.getResourceAsStream(signedFile);
        byte[] signedCoseData = signedStream.readAllBytes();

        DefaultCrypto crypto = new DefaultCrypto();
        crypto.loadPrivateKey(keyFile, cert, "");

        CoseSignature coseSig = new CoseSignature();
        byte[] toBeSigned;
        toBeSigned = coseSig.createToBeSigned(CoseAlgorithm.getAlgId("ES256"),
                "11".getBytes(StandardCharsets.UTF_8), contData, cert, true, false,
                "test");

        byte[] signature;
        signature = crypto.sign(toBeSigned);

        coseSig.addSignature(signature);
        byte[] signedRim;
        signedRim = coseSig.getSignedData();

        assertArrayEquals(signedCoseData, signedRim);
    }
}
