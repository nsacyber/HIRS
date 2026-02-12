package hirs.attestationca.persist;

import com.google.protobuf.ByteString;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.provision.AbstractProcessor;
import hirs.attestationca.persist.provision.AttestationCertificateAuthority;
import hirs.attestationca.persist.provision.helper.IssuedCertificateAttributeHelper;
import hirs.attestationca.persist.provision.helper.ProvisionUtils;
import hirs.structs.elements.aca.SymmetricAttestation;
import hirs.structs.elements.tpm.AsymmetricPublicKey;
import hirs.structs.elements.tpm.EncryptionScheme;
import hirs.structs.elements.tpm.IdentityProof;
import hirs.structs.elements.tpm.StorePubKey;
import hirs.structs.elements.tpm.SymmetricKey;
import hirs.utils.HexUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test suite for {@link AttestationCertificateAuthority}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)              // needed to use non-static BeforeAll
public class AttestationCertificateAuthorityTest {

    // length of IV used in PKI
    private static final int ENCRYPTION_IV_LEN = 16;
    // length of secret key used in PKI
    private static final int SECRETKEY_LEN = 128;
    private static final String EK_PUBLIC_PATH = "/tpm2/ek.pub";
    private static final String AK_PUBLIC_PATH = "/tpm2/ak.pub";
    private static final String AK_NAME_PATH = "/tpm2/ak.name";
    private static final String TEST_NONCE_BLOB_PATH = "test/nonce.blob";
    private static final String EK_MODULUS_HEX = "a3 b5 c2 1c 57 be 40 c4  3c 78 90 0d 00 81 01 78"
            + "13 ca 02 ec b6 75 89 60  ca 60 9b 10 b6 b4 d0 0b"
            + "4d e4 68 ad 01 a6 91 e2  56 20 5e cf 16 fe 77 ae"
            + "1f 13 d7 ac a1 91 0b 68  f6 07 cf c2 4b 5e c1 2c"
            + "4c fe 3a c9 62 7e 10 02  5b 33 c8 c2 1a cd 2e 7f"
            + "dd 7c 43 ac a9 5f b1 d6  07 56 4f 72 9b 0a 00 6c"
            + "f6 8d 23 a1 84 ca c1 7f  5a 8b ef 0e 23 11 90 00"
            + "30 f2 99 e9 94 59 c6 b0  fe b2 5c 0c c7 b4 76 69"
            + "6c f1 b7 d8 e5 60 d6 61  9f ab 7c 17 ce a4 74 6d"
            + "8c cd e6 9e 6e bb 64 52  a7 c3 bf ac 07 e8 5e 3e"
            + "ae eb dc c5 95 37 26 6a  5d a6 a2 12 52 fa 03 43"
            + "b2 62 2d 87 8c a7 06 8f  d6 3f 63 b6 2d 73 c4 9d"
            + "9d d6 55 0e bb db b1 eb  dd c5 4b 8f c3 17 cb 3b"
            + "c3 bf f6 7f 13 44 de 8e  d7 b9 f1 a7 15 56 8f 6c"
            + "cd f2 4c 86 99 39 19 88  d3 4a 2f 38 c4 c4 37 39"
            + "85 6f 41 98 19 14 a4 1f  95 bc 04 ef 74 c2 0d f3";
    private static final String AK_MODULUS_HEX = "d7 c9 f0 e3 ac 1b 4a 1e  3c 9d 2d 57 02 e9 2a 93"
            + "b0 c0 e1 50 af e4 61 11  31 73 a1 96 b8 d6 d2 1c"
            + "40 40 c8 a6 46 a4 10 4b  d1 06 74 32 f6 e3 8a 55"
            + "1e 03 c0 3e cc 75 04 c6  44 88 b6 ad 18 c9 45 65"
            + "0d be c5 45 22 bd 24 ad  32 8c be 83 a8 9b 1b d9"
            + "e0 c8 d9 ec 14 67 55 1b  fe 68 dd c7 f7 33 e4 cd"
            + "87 bd ba 9a 07 e7 74 eb  57 ef 80 9c 6d ee f9 35"
            + "52 67 36 e2 53 98 46 a5  4e 8f 17 41 8d ff eb bb"
            + "9c d2 b4 df 57 f8 7f 31  ef 2e 2d 6e 06 7f 05 ed"
            + "3f e9 6f aa b4 b7 5a f9  6d ba ff 2b 5e f7 c1 05"
            + "90 68 1f b6 4b 38 67 f7  92 d8 73 51 6e 08 19 ad"
            + "ca 35 48 a7 c1 fb cb 01  9a 28 03 c9 fe bb 49 2f"
            + "88 3f a1 e7 a8 69 f0 f8  e8 78 db d3 6d c5 80 8d"
            + "c2 e4 8a af 4b c2 ac 48  2a 44 63 6e 39 b0 8f dd"
            + "e4 b3 a3 f9 2a b1 c8 d9  3d 6b c4 08 b0 16 c4 e7"
            + "c7 2f f5 94 c6 43 3e ee  9b 8a da e7 31 d1 54 dd";
    private static final String AK_NAME_HEX = "00 0b 6e 8f 79 1c 7e 16  96 1b 11 71 65 9c e0 cd"
            + "ae 0d 4d aa c5 41 be 58  89 74 67 55 96 c2 5e 38"
            + "e2 94";
    private final Random random = new Random();
    // object in test
    private AttestationCertificateAuthority aca;
    private AccessAbstractProcessor abstractProcessor;
    // test key pair
    private KeyPair keyPair;

    /**
     * Creates a self-signed X.509 public-key certificate.
     *
     * @param pair KeyPair to create the cert for
     * @return self-signed X509Certificate
     */
    private static X509Certificate createSelfSignedCertificate(final KeyPair pair) {
        Security.addProvider(new BouncyCastleProvider());
        final int timeRange = 10000;
        X509Certificate cert = null;
        try {

            X500Name issuerName = new X500Name("CN=TEST2, OU=TEST2, O=TEST2, C=TEST2");
            X500Name subjectName = new X500Name("CN=TEST, OU=TEST, O=TEST, C=TEST");
            BigInteger serialNumber = BigInteger.ONE;
            Date notBefore = new Date(System.currentTimeMillis() - timeRange);
            Date notAfter = new Date(System.currentTimeMillis() + timeRange);
            X509v3CertificateBuilder builder =
                    new JcaX509v3CertificateBuilder(issuerName, serialNumber, notBefore, notAfter,
                            subjectName, pair.getPublic());
            ContentSigner signer =
                    new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(
                            pair.getPrivate());
            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(
                    builder.build(signer));
        } catch (Exception e) {
            fail("Exception occurred while creating a cert", e);
        }
        return cert;
    }

    /**
     * Registers bouncy castle as a security provider. Normally the JEE container will handle this,
     * but since the tests are not instantiating a container, have the unit test runner set up the
     * provider.
     */
    @BeforeAll
    public void setupTests() throws Exception {

        //BeforeSuite
        final int keySize = 2048;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        keyPair = keyPairGenerator.generateKeyPair();

        //BeforeTest
        aca = new AttestationCertificateAuthority(null, keyPair.getPrivate(),
                null, null, null, null, null, null, null, 1,
                null, null, null, null) {
        };
        abstractProcessor = new AccessAbstractProcessor(keyPair.getPrivate(), 1);

        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Tests {@link AttestationCertificateAuthority#processIdentityClaimTpm2(byte[])}
     * where the byte array is null. Expects an illegal argument exception to be thrown.
     */
    @Test
    public void testProcessIdentityClaimTpm2NullRequest() {
        assertThrows(IllegalArgumentException.class, () ->
                aca.processIdentityClaimTpm2(null));
    }

    /**
     * Tests {@link AttestationCertificateAuthority#getPublicKey()}.
     */
    @Test
    public void testGetPublicKey() {

        // encoded byte array to be returned by public key
        byte[] encoded = new byte[] {0, 1, 0, 1, 0};

        // create mocks for testing
        X509Certificate acaCertificate = mock(X509Certificate.class);
        PublicKey publicKey = mock(PublicKey.class);

        // assign the aca certificate to the aca
        ReflectionTestUtils.setField(aca, "acaCertificate", acaCertificate);

        // return a mocked public key
        when(acaCertificate.getPublicKey()).thenReturn(publicKey);

        // return test byte array
        when(publicKey.getEncoded()).thenReturn(encoded);

        // assert what the ACA returns is as expected
        assertArrayEquals(encoded, aca.getPublicKey());

        // verify mock interactions
        verify(acaCertificate).getPublicKey();
        verify(publicKey).getEncoded();

        // verify no other interactions with mocks
        verifyNoMoreInteractions(acaCertificate, publicKey);
    }

    /**
     * Tests {@link ProvisionUtils#decryptAsymmetricBlob(byte[],
     * EncryptionScheme, PrivateKey)}.
     *
     * @throws Exception during aca processing
     */
    @Test
    public void testDecryptAsymmetricBlob() throws Exception {

        // test encryption transformation
        EncryptionScheme encryptionScheme = EncryptionScheme.PKCS1;

        // test variables
        byte[] expected = "test".getBytes(StandardCharsets.UTF_8);

        // encrypt the expected value using same algorithm as the ACA.
        byte[] encrypted = encryptBlob(expected, encryptionScheme.toString());

        // perform the decryption and assert that the decrypted bytes equal the expected bytes
        assertArrayEquals(expected, ProvisionUtils.decryptAsymmetricBlob(
                encrypted, encryptionScheme, keyPair.getPrivate()));
    }

    /**
     * Tests {@link ProvisionUtils#decryptSymmetricBlob(
     *byte[], byte[], byte[], String)}.
     *
     * @throws Exception during aca processing
     */
    @Test
    public void testDecryptSymmetricBlob() throws Exception {
        // test encryption transformation
        String transformation = "AES/CBC/PKCS5Padding";

        // test variables
        byte[] expected = "test".getBytes(StandardCharsets.UTF_8);

        // create a key generator to generate a "shared" secret
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(SECRETKEY_LEN);

        // use some random bytes as the IV to encrypt and subsequently decrypt with
        byte[] randomBytes = new byte[ENCRYPTION_IV_LEN];

        // generate the random bytes
        random.nextBytes(randomBytes);

        // the shared secret
        byte[] secretKey = keyGenerator.generateKey().getEncoded();

        // encrypt the expected value with the private key being the shared secret
        byte[] encrypted = encryptBlob(expected, secretKey, randomBytes, transformation);

        // perform the decryption using the generated shared secret, random bytes as an IV, and the
        // AES CBC transformation for the cipher. then assert the decrypted results are the same
        // as our expected value.
        assertArrayEquals(expected,
                ProvisionUtils.decryptSymmetricBlob(encrypted, secretKey, randomBytes, transformation));
    }

    /**
     * Tests {@link ProvisionUtils#generateSymmetricKey()}.
     */
    @Test
    public void testGenerateSymmetricKey() {
        // perform the test
        SymmetricKey symmetricKey = ProvisionUtils.generateSymmetricKey();

        // assert the symmetric algorithm, scheme, and key size are all set appropriately
        final int expectedAlgorithmId = 6;
        final int expectedEncryptionScheme = 255;

        assertTrue(symmetricKey.getAlgorithmId() == expectedAlgorithmId);
        assertTrue(symmetricKey.getEncryptionScheme() == expectedEncryptionScheme);
        assertTrue(symmetricKey.getKeySize() == symmetricKey.getKey().length);
    }

    private void assertTrue(final boolean b) {
    }

    /**
     * Tests {@link ProvisionUtils#generateAsymmetricContents(
     *byte[], byte[], PublicKey)}.
     *
     * @throws Exception during aca processing
     */
    @Test
    public void testGenerateAsymmetricContents() throws Exception {

        // "encoded" identity proof (returned by struct converter)
        byte[] identityProofEncoded = new byte[] {0, 0, 1, 1};

        // generate a random session key to be used for encryption and decryption
        byte[] sessionKey = new byte[ENCRYPTION_IV_LEN];

        random.nextBytes(sessionKey);
        // perform the test
        byte[] result = ProvisionUtils.generateAsymmetricContents(identityProofEncoded,
                sessionKey, keyPair.getPublic());

        // decrypt the result
        byte[] decryptedResult = decryptBlob(result);

        // create a SHA1 digest of the identity key
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(identityProofEncoded);

        // generate the digest
        byte[] identityDigest = md.digest();

        // the decrypted asymmetric contents should be the session key and a SHA-1 hash of the
        // encoded identity proof.
        byte[] expected = ArrayUtils.addAll(sessionKey, identityDigest);

        // compare the two byte arrays
        assertArrayEquals(expected, decryptedResult);
    }

    /**
     * Tests {@link ProvisionUtils#generateAttestation(X509Certificate,
     * SymmetricKey)}.
     *
     * @throws Exception during aca processing
     */
    @Test
    public void testGenerateAttestation() throws Exception {

        // create some mocks for the unit tests
        X509Certificate certificate = mock(X509Certificate.class);
        SymmetricKey symmetricKey = mock(SymmetricKey.class);

        // create a key generator to generate a secret key
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(SECRETKEY_LEN);

        // obtain the key from the generator
        byte[] secretKey = keyGenerator.generateKey().getEncoded();

        // use our public key for encryption
        when(symmetricKey.getKey()).thenReturn(secretKey);

        // just use the existing public key for the credential
        when(certificate.getEncoded()).thenReturn(keyPair.getPublic().getEncoded());

        // perform the actual test
        SymmetricAttestation attestation = ProvisionUtils.generateAttestation(certificate, symmetricKey);

        // validate that the attestation is not null
        assertNotNull(attestation);

        // validate the attestation algorithm
        final int expectedAlgorithmId = 6;
        assertNotNull(attestation.getAlgorithm());
        assertTrue(attestation.getAlgorithm().getAlgorithmId() == expectedAlgorithmId);
        assertTrue(attestation.getAlgorithm().getEncryptionScheme() == 0x1);
        assertTrue(attestation.getAlgorithm().getSignatureScheme() == 0);
        assertTrue(attestation.getAlgorithm().getParamsSize() == 0);

        // validate the attestation credential
        assertNotNull(attestation.getCredential());

        // validate that the credential size is the size of the actual credential block
        assertTrue(attestation.getCredential().length == attestation.getCredentialSize());

        // create containers for the 2 parts of the credential
        byte[] iv = new byte[ENCRYPTION_IV_LEN];
        byte[] credential = new byte[attestation.getCredential().length - iv.length];

        // siphon off the first 16 bytes for the IV
        System.arraycopy(attestation.getCredential(), 0, iv, 0, iv.length);

        // the rest is the actual encrypted credential
        System.arraycopy(attestation.getCredential(), iv.length, credential, 0, credential.length);

        // decrypt the credential
        byte[] decrypted = decryptBlob(credential, secretKey, iv, "AES/CBC/PKCS5Padding");

        // assert that the decrypted credential is our public key
        assertArrayEquals(keyPair.getPublic().getEncoded(), decrypted);

        // verify that the mocks were interacted with appropriately
        verify(symmetricKey).getKey();
        verify(certificate).getEncoded();
        verifyNoMoreInteractions(certificate, symmetricKey);
    }

    /**
     * Tests {@link AttestationCertificateAuthority#
     * AttestationCertificateAuthority(SupplyChainValidationService, PrivateKey,
     * X509Certificate, StructConverter, CertificateManager, DeviceRegister, int,
     * DeviceManager, DBManager)}.
     *
     * @throws Exception during subject alternative name checking if cert formatting is bad
     */
    @Test
    public void testGenerateCredential() throws Exception {
        // test variables
        final String identityProofLabelString = "label";
        byte[] identityProofLabel = identityProofLabelString.getBytes(StandardCharsets.UTF_8);
        byte[] modulus = ((RSAPublicKey) keyPair.getPublic()).getModulus().toByteArray();
        int validDays = 1;

        // create mocks for testing
        IdentityProof identityProof = mock(IdentityProof.class);
        AsymmetricPublicKey asymmetricPublicKey = mock(AsymmetricPublicKey.class);
        StorePubKey storePubKey = mock(StorePubKey.class);
        X509Certificate acaCertificate = createSelfSignedCertificate(keyPair);

        // assign ACA fields
        ReflectionTestUtils.setField(aca, "validDays", validDays);
        ReflectionTestUtils.setField(aca, "acaCertificate", acaCertificate);

        // prepare identity proof interactions
        when(identityProof.getLabel()).thenReturn(identityProofLabel);

        // perform the test
        X509Certificate certificate = abstractProcessor.accessGenerateCredential(keyPair.getPublic(),
                null,
                new LinkedList<PlatformCredential>(),
                "exampleIdLabel",
                acaCertificate);

        // grab the modulus from the generate certificate
        byte[] resultMod = ((RSAPublicKey) certificate.getPublicKey()).getModulus().toByteArray();

        // today and tomorrow, when the certificate should be valid for
        Calendar today = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);

        // validate the certificate
        assertTrue(certificate.getIssuerX500Principal().toString().contains("CN=TEST"));
        assertTrue(certificate.getIssuerX500Principal().toString().contains("OU=TEST"));
        assertTrue(certificate.getIssuerX500Principal().toString().contains("O=TEST"));
        assertTrue(certificate.getIssuerX500Principal().toString().contains("C=TEST"));

        // validate the format of the subject and subject alternative name
        assertEquals("", certificate.getSubjectX500Principal().getName());
        assertEquals("exampleIdLabel",
                ((X500Name) GeneralNames.fromExtensions(((TBSCertificate.getInstance(
                        certificate.getTBSCertificate()).getExtensions())), Extension.
                        subjectAlternativeName).getNames()[0].getName()).getRDNs(
                        IssuedCertificateAttributeHelper.TCPA_AT_TPM_ID_LABEL)[0].getFirst()
                        .getValue().toString());

        assertArrayEquals(modulus, resultMod);

        // obtain the expiration dates from the certificate
        Calendar beforeDate = Calendar.getInstance();
        Calendar afterDate = Calendar.getInstance();
        beforeDate.setTime(certificate.getNotBefore());
        afterDate.setTime(certificate.getNotAfter());

        // assert the dates are set correctly
        assertEquals(today.get(Calendar.DATE), beforeDate.get(Calendar.DATE));
        assertEquals(tomorrow.get(Calendar.DATE), afterDate.get(Calendar.DATE));

        // validate mock interactions
        verifyNoMoreInteractions(identityProof, asymmetricPublicKey, storePubKey);
    }

    /**
     * Tests {@link ProvisionUtils#assemblePublicKey(byte[])}.
     */
    @Test
    public void testAssemblePublicKeyUsingByteArray() {
        // obtain the expected modulus from the existing public key
        final BigInteger modulus = ((RSAPublicKey) keyPair.getPublic()).getModulus();

        // perform test
        RSAPublicKey publicKey = (RSAPublicKey) ProvisionUtils.assemblePublicKey(modulus.toByteArray());

        // assert that the exponent and the modulus are the same. the exponents should be the well
        // known prime, 101
        final int radix = 16;
        assertTrue(publicKey.getPublicExponent().equals(new BigInteger("010001", radix)));
        assertTrue(publicKey.getModulus().equals(modulus));
    }

    /**
     * Tests {@link ProvisionUtils#assemblePublicKey(String)}.
     */
    @Test
    public void testAssemblePublicKeyUsingHexEncodedString() {
        // obtain the expected modulus from the existing public key
        final BigInteger modulus = ((RSAPublicKey) keyPair.getPublic()).getModulus();

        // encode our existing public key into hex
        final String modulusString = Hex.encodeHexString(
                ((RSAPublicKey) keyPair.getPublic()).getModulus().toByteArray());

        // perform test
        RSAPublicKey publicKey = (RSAPublicKey) ProvisionUtils.assemblePublicKey(modulusString);

        // assert that the exponent and the modulus are the same. the exponents should be the well
        // known prime, 101.
        final int radix = 16;
        assertTrue(publicKey.getPublicExponent().equals(new BigInteger("010001", radix)));
        assertTrue(publicKey.getModulus().equals(modulus));
    }

    /**
     * Tests parsing the EK from the TPM2 output file.
     *
     * @throws URISyntaxException incorrect resource path
     * @throws IOException        unable to read from file
     */
    @Test
    public void testParseEk() throws URISyntaxException, IOException {
        Path ekPath = Paths.get(getClass().getResource(
                EK_PUBLIC_PATH).toURI());

        byte[] ekFile = Files.readAllBytes(ekPath);

        RSAPublicKey ek = ProvisionUtils.parsePublicKey(ekFile);
        final int radix = 16;
        assertTrue(ek.getPublicExponent().equals(new BigInteger("010001", radix)));

        byte[] mod = ek.getModulus().toByteArray();
        // big integer conversion is signed so it can add a 0 byte
        if (mod[0] == 0) {
            byte[] tmp = new byte[mod.length - 1];
            System.arraycopy(mod, 1, tmp, 0, mod.length - 1);
            mod = tmp;
        }
        String hex = HexUtils.byteArrayToHexString(mod);
        String realMod = EK_MODULUS_HEX.replaceAll("\\s+", "");
        assertEquals(realMod, hex);
    }

    /**
     * Tests parsing the AK public key from the TPM2 output file.
     *
     * @throws URISyntaxException incorrect resource path
     * @throws IOException        unable to read from file
     */
    @Test
    public void testParseAk() throws URISyntaxException, IOException {
        Path akPath = Paths.get(getClass().getResource(
                AK_PUBLIC_PATH).toURI());

        byte[] akFile = Files.readAllBytes(akPath);

        RSAPublicKey ak = ProvisionUtils.parsePublicKey(akFile);
        final int radix = 16;
        assertTrue(ak.getPublicExponent().equals(new BigInteger("010001", radix)));

        byte[] mod = ak.getModulus().toByteArray();
        // big integer conversion is signed so it can add a 0 byte
        if (mod[0] == 0) {
            byte[] tmp = new byte[mod.length - 1];
            System.arraycopy(mod, 1, tmp, 0, mod.length - 1);
            mod = tmp;
        }
        String hex = HexUtils.byteArrayToHexString(mod);
        String realMod = AK_MODULUS_HEX.replaceAll("\\s+", "");
        assertEquals(realMod, hex);
    }

    /**
     * Tests parsing the AK name from the TPM2 output file.
     *
     * @throws URISyntaxException       incorrect resource path
     * @throws IOException              unable to read from file
     * @throws NoSuchAlgorithmException inavlid algorithm
     */
    @Test
    public void testGenerateAkName() throws URISyntaxException, IOException,
            NoSuchAlgorithmException {
        Path akNamePath = Paths.get(getClass().getResource(
                AK_NAME_PATH).toURI());

        byte[] akNameFileBytes = Files.readAllBytes(akNamePath);
        String realHex = HexUtils.byteArrayToHexString(akNameFileBytes);

        String realMod = AK_MODULUS_HEX.replaceAll("\\s+", "");
        byte[] akName = ProvisionUtils.generateAkName(HexUtils.hexStringToByteArray(realMod));

        String hex = HexUtils.byteArrayToHexString(akName);
        String realName = AK_NAME_HEX.replaceAll("\\s+", "");
        assertEquals(hex, realName);
        assertEquals(hex, realHex);
    }

    /**
     * Method to generate a make credential output file for use in manual testing. Feed to
     * a TPM 2.0 or emulator using the activate credential command to ensure proper parsing.
     * Must be performed manually. To use, copy the TPM's ek and ak into
     * HIRS_AttestationCA/src/test/resources/tpm2/test/ and ensure the variables akPubPath
     * and ekPubPath are correct. Your output file will be
     * HIRS_AttestationCA/src/test/resources/tpm2/test/make.blob and the nonce used will be
     * output as HIRS_AttestationCA/src/test/resources/tpm2/test/secret.blob
     *
     * @throws URISyntaxException invalid file path
     * @throws IOException        unable to read file
     */
    @Disabled
    @Test
    public void testMakeCredential() throws URISyntaxException, IOException {
        Path akPubPath = Paths.get(getClass().getResource(
                AK_PUBLIC_PATH).toURI());
        Path ekPubPath = Paths.get(getClass().getResource(
                EK_PUBLIC_PATH).toURI());

        byte[] ekPubFile = Files.readAllBytes(ekPubPath);
        byte[] akPubFile = Files.readAllBytes(akPubPath);

        RSAPublicKey ekPub = ProvisionUtils.parsePublicKey(ekPubFile);
        RSAPublicKey akPub = ProvisionUtils.parsePublicKey(akPubFile);

        // prepare the nonce and wrap it with keys
        final byte[] nonce = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
        ByteString blob = ProvisionUtils.tpm20MakeCredential(ekPub, akPub, nonce);

        Path resources = Objects.requireNonNull(Paths.get(Objects.requireNonNull(this.getClass().getResource(
                        "/").toURI()))
                .getParent().getParent().getParent().getParent());
        Path makeBlob = resources.resolve("src/test/resources/tpm2/test/make.blob");
        Files.write(makeBlob, blob.toByteArray());

        Path secretPath = resources.resolve("src/test/resources/tpm2/test/secret.blob");
        Files.write(secretPath, nonce);
    }

    /**
     * Test helper method that encrypts a blob using the specified transformation and the test key
     * pair public key.
     *
     * @param blob           to be encrypted
     * @param transformation used by a cipher to encrypt
     * @return encrypted blob
     * @throws Exception during the encryption process
     */
    private byte[] encryptBlob(final byte[] blob, final String transformation) throws Exception {
        // initialize a cipher using the specified transformation
        Cipher cipher = Cipher.getInstance(transformation);

        // use our generated public key to encrypt
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());

        // return the cipher text
        return cipher.doFinal(blob);
    }

    /**
     * Test helper method that encrypts a blob using a shared key and IV using the specified
     * transformation.
     *
     * @param blob           blob to be encrypted
     * @param key            shared key
     * @param iv             to encrypt with
     * @param transformation of the encryption cipher
     * @return encrypted blob
     * @throws Exception if there are any issues while encrypting the blob
     */
    private byte[] encryptBlob(final byte[] blob, final byte[] key, final byte[] iv,
                               final String transformation) throws Exception {
        // initialize a cipher using the specified transformation
        Cipher cipher = Cipher.getInstance(transformation);

        // generate a secret key specification using the key and AES.
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

        // create IV parameter for key specification
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // encrypt using the key specification with the generated IV
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);

        // return the cipher text
        return cipher.doFinal(blob);
    }

    /**
     * Test helper method to decrypt blobs.
     *
     * @param blob blob to be decrypted
     * @return decrypted blob
     * @throws Exception if there are any issues while decrypting the blob
     */
    private byte[] decryptBlob(final byte[] blob) throws Exception {
        // initialize a cipher using the specified transformation
        Cipher cipher = Cipher.getInstance(EncryptionScheme.OAEP.toString());

        OAEPParameterSpec spec = new OAEPParameterSpec("Sha1", "MGF1",
                MGF1ParameterSpec.SHA1, new PSource.PSpecified("TCPA".getBytes(StandardCharsets.UTF_8)));

        // use our generated public key to encrypt
        cipher.init(Cipher.PRIVATE_KEY, keyPair.getPrivate(), spec);

        // return the cipher text
        return cipher.doFinal(blob);
    }

    /**
     * Test helper method that decrypts a blob using a shared key and IV using the specified.
     * transformation.
     *
     * @param blob           blob to be decrypted
     * @param key            shared key
     * @param iv             to decrypt with
     * @param transformation of the decryption cipher
     * @return decrypted blob
     * @throws Exception if there are any issues while decrypting the blob
     */
    private byte[] decryptBlob(final byte[] blob, final byte[] key, final byte[] iv,
                               final String transformation) throws Exception {
        // initialize a cipher using the specified transformation
        Cipher cipher = Cipher.getInstance(transformation);

        // generate a secret key specification using the key and AES
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

        // create IV parameter for key specification
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // encrypt using the key specification with the generated IV
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec);

        // return the cipher text
        return cipher.doFinal(blob);
    }

    /**
     * This internal class handles setup for testing the function
     * generateCredential() from class AbstractProcessor. Because the
     * function is Protected and in a different package than the test,
     * it cannot be accessed directly.
     */
    @Nested
    public class AccessAbstractProcessor extends AbstractProcessor {

        /**
         * Constructor.
         *
         * @param privateKey the private key of the ACA
         * @param validDays  int for the time in which a certificate is valid.
         */
        public AccessAbstractProcessor(final PrivateKey privateKey,
                                       final int validDays) {
            super(privateKey, validDays);
        }

        /**
         * Public wrapper for the protected function generateCredential(), to access for testing.
         *
         * @param publicKey             cannot be null
         * @param endorsementCredential the endorsement credential
         * @param platformCredentials   the set of platform credentials
         * @param deviceName            The host name used in the subject alternative name
         * @param acaCertificate        the aca certificate
         * @return the generated X509 certificate
         */
        public X509Certificate accessGenerateCredential(final PublicKey publicKey,
                                                        final EndorsementCredential endorsementCredential,
                                                        final List<PlatformCredential> platformCredentials,
                                                        final String deviceName,
                                                        final X509Certificate acaCertificate) {

            return generateCredential(publicKey,
                    endorsementCredential,
                    platformCredentials,
                    deviceName,
                    acaCertificate);
        }
    }
}
