package hirs.attestationca;

import com.google.protobuf.ByteString;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.utils.HexUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

import hirs.structs.converters.StructConverter;
import hirs.structs.elements.aca.SymmetricAttestation;
import hirs.structs.elements.tpm.AsymmetricKeyParams;
import hirs.structs.elements.tpm.AsymmetricPublicKey;
import hirs.structs.elements.tpm.EncryptionScheme;
import hirs.structs.elements.tpm.IdentityProof;
import hirs.structs.elements.tpm.IdentityRequest;
import hirs.structs.elements.tpm.StorePubKey;
import hirs.structs.elements.tpm.SymmetricKey;
import hirs.structs.elements.tpm.SymmetricKeyParams;
import hirs.structs.elements.tpm.SymmetricSubParams;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test suite for {@link AbstractAttestationCertificateAuthority}.
 */
public class AbstractAttestationCertificateAuthorityTest {

    // object in test
    private AbstractAttestationCertificateAuthority aca;

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

    // test key pair
    private KeyPair keyPair;

    /**
     * Registers bouncy castle as a security provider. Normally the JEE container will handle this,
     * but since the tests are not instantiating a container, have the unit test runner setup the
     * provider.
     */
    @BeforeClass
    public static void setupTests() {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Instantiates an anonymous instance of the {@link AbstractAttestationCertificateAuthority}.
     * This is sufficient as this class does not have any unimplemented or abstract methods.
     */
    @BeforeTest
    public void setup() {
        aca = new AbstractAttestationCertificateAuthority(null, keyPair.getPrivate(),
                null, null, null, null, null, 1,
                null, null) {
        };
    }

    /**
     * Generates a key pair that can be used by the test suite.
     *
     * @throws Exception during key generation
     */
    @BeforeSuite
    public void suiteSetup() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();
    }

    /**
     * Tests {@link AbstractAttestationCertificateAuthority#processIdentityRequest(byte[])}
     * where the byte array is null. Expects an illegal argument exception to be thrown.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testProcessIdentityRequestNullRequest() {
        aca.processIdentityRequest(null);
    }

    /**
     * Tests {@link AbstractAttestationCertificateAuthority#processIdentityClaimTpm2(byte[])}
     * where the byte array is null. Expects an illegal argument exception to be thrown.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testProcessIdentityClaimTpm2NullRequest() {
        aca.processIdentityClaimTpm2(null);
    }

    /**
     * Tests {@link AbstractAttestationCertificateAuthority#getPublicKey()}.
     */
    @Test
    public void testGetPublicKey() {

        // encoded byte array to be returned by public key
        byte[] encoded = new byte[]{0, 1, 0, 1, 0};

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
        assertEquals(aca.getPublicKey(), encoded);

        // verify mock interactions
        verify(acaCertificate).getPublicKey();
        verify(publicKey).getEncoded();

        // verify no other interactions with mocks
        verifyNoMoreInteractions(acaCertificate, publicKey);
    }

    /**
     * Tests {@link AbstractAttestationCertificateAuthority#unwrapIdentityRequest(byte[])}.
     *
     * @throws Exception during aca processing
     */
    @Test
    public void testUnwrapIdentityRequest() throws Exception {
        // create a key generator to generate a "shared" secret
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);

        // test variables
        byte[] request = new byte[1];
        byte[] iv = new byte[16];
        byte[] asymmetricBlob = new byte[]{1, 2, 3};
        byte[] symmetricBlob = new byte[]{3, 2, 1};
        byte[] secretKey = keyGenerator.generateKey().getEncoded();

        // fill the IV with random bytes
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.nextBytes(iv);

        // encrypt the asymmetric blob
        byte[] encryptedAsymmetricBlob =
                encryptBlob(asymmetricBlob, EncryptionScheme.OAEP.toString());

        // encrypt the symmetric blob
        byte[] encryptedSymmetricBlob =
                encryptBlob(symmetricBlob, secretKey, iv, "AES/CBC/PKCS5Padding");

        // create some mocks for the tests
        IdentityRequest identityRequest = mock(IdentityRequest.class);
        AsymmetricKeyParams asymmetricKeyParams = mock(AsymmetricKeyParams.class);
        SymmetricKeyParams symmetricKeyParams = mock(SymmetricKeyParams.class);
        SymmetricSubParams symmetricSubParams = mock(SymmetricSubParams.class);
        StructConverter structConverter = mock(StructConverter.class);
        SymmetricKey symmetricKey = mock(SymmetricKey.class);

        // assign the mocked struct converter to the test object
        ReflectionTestUtils.setField(aca, "structConverter", structConverter);

        // when converting our test request byte array, return our mocked identity request
        // when converting our test asymmetric blob to a symmetric key, return the mocked key
        when(structConverter.convert(request, IdentityRequest.class)).thenReturn(identityRequest);
        when(structConverter.convert(asymmetricBlob, SymmetricKey.class)).thenReturn(symmetricKey);

        // mock out the identity request by returning other mocks as needed
        when(identityRequest.getSymmetricAlgorithm()).thenReturn(symmetricKeyParams);
        when(identityRequest.getAsymmetricAlgorithm()).thenReturn(asymmetricKeyParams);
        when(identityRequest.getAsymmetricBlob()).thenReturn(encryptedAsymmetricBlob);
        when(identityRequest.getSymmetricBlob()).thenReturn(encryptedSymmetricBlob);

        // use OAEP encryption scheme when asked
        when(asymmetricKeyParams.getEncryptionScheme()).thenReturn(
                (short) EncryptionScheme.OAEP_VALUE);

        // use the mocked sub params when asked
        when(symmetricKeyParams.getParams()).thenReturn(symmetricSubParams);

        // use the test IV when asked
        when(symmetricSubParams.getIv()).thenReturn(iv);

        // use the test secret key when asked
        when(symmetricKey.getKey()).thenReturn(secretKey);

        // perform test
        byte[] unwrappedRequest = aca.unwrapIdentityRequest(request);

        // verify test results
        assertEquals(unwrappedRequest, symmetricBlob);

        // verify mock interactions
        verify(structConverter).convert(request, IdentityRequest.class);
        verify(structConverter).convert(asymmetricBlob, SymmetricKey.class);
        verify(identityRequest).getSymmetricAlgorithm();
        verify(identityRequest).getAsymmetricAlgorithm();
        verify(identityRequest).getAsymmetricBlob();
        verify(identityRequest).getSymmetricBlob();
        verify(asymmetricKeyParams).getEncryptionScheme();
        verify(symmetricKeyParams, times(2)).getParams();
        verify(symmetricSubParams).getIv();
        verify(symmetricKey).getKey();
        verifyNoMoreInteractions(identityRequest, symmetricKeyParams, symmetricSubParams,
                asymmetricKeyParams, structConverter, symmetricKey);
    }

    /**
     * Tests {@link AbstractAttestationCertificateAuthority#unwrapIdentityRequest(byte[])}.
     *
     * @throws Exception during aca processing
     */
    @Test
    public void testUnwrapIdentityRequestNoKeyParams() throws Exception {
        // create a key generator to generate a "shared" secret
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);

        // test variables
        byte[] request = new byte[1];
        byte[] iv = new byte[16];
        byte[] asymmetricBlob = new byte[]{1, 2, 3};
        byte[] symmetricBlob = new byte[]{3, 2, 1};
        byte[] secretKey = keyGenerator.generateKey().getEncoded();

        // fill the IV with random bytes
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.nextBytes(iv);

        // encrypt the asymmetric blob
        byte[] encryptedAsymmetricBlob =
                encryptBlob(asymmetricBlob, EncryptionScheme.OAEP.toString());

        // encrypt the symmetric blob
        byte[] encryptedSymmetricBlob =
                encryptBlob(symmetricBlob, secretKey, iv, "AES/CBC/PKCS5Padding");

        // since there is no symmetric key params, put the IV in front of encrypted blob.
        byte[] totalBlob = ArrayUtils.addAll(iv, encryptedSymmetricBlob);

        // create some mocks for the tests
        IdentityRequest identityRequest = mock(IdentityRequest.class);
        AsymmetricKeyParams asymmetricKeyParams = mock(AsymmetricKeyParams.class);
        StructConverter structConverter = mock(StructConverter.class);
        SymmetricKey symmetricKey = mock(SymmetricKey.class);

        // assign the mocked struct converter to the test object
        ReflectionTestUtils.setField(aca, "structConverter", structConverter);

        // when converting our test request byte array, return our mocked identity request
        // when converting our test asymmetric blob to a symmetric key, return the mocked key
        when(structConverter.convert(request, IdentityRequest.class)).thenReturn(identityRequest);
        when(structConverter.convert(asymmetricBlob, SymmetricKey.class)).thenReturn(symmetricKey);

        // mock out the identity request by returning other mocks as needed
        when(identityRequest.getSymmetricAlgorithm()).thenReturn(null);
        when(identityRequest.getAsymmetricAlgorithm()).thenReturn(asymmetricKeyParams);
        when(identityRequest.getAsymmetricBlob()).thenReturn(encryptedAsymmetricBlob);

        // the first request should return IV + encrypted blob. Subsequent requests should return
        // just the encrypted portion of the blob
        when(identityRequest.getSymmetricBlob()).thenReturn(totalBlob)
                .thenReturn(encryptedSymmetricBlob);

        // use OAEP encryption scheme when asked
        when(asymmetricKeyParams.getEncryptionScheme()).thenReturn(
                (short) EncryptionScheme.OAEP_VALUE);

        // use the test secret key when asked
        when(symmetricKey.getKey()).thenReturn(secretKey);

        // perform test
        byte[] unwrappedRequest = aca.unwrapIdentityRequest(request);

        // verify test results
        assertEquals(unwrappedRequest, symmetricBlob);

        // verify mock interactions
        verify(structConverter).convert(request, IdentityRequest.class);
        verify(structConverter).convert(asymmetricBlob, SymmetricKey.class);
        verify(identityRequest).getSymmetricAlgorithm();
        verify(identityRequest).getAsymmetricAlgorithm();
        verify(identityRequest).getAsymmetricBlob();
        verify(identityRequest, times(2)).getSymmetricBlob();
        verify(identityRequest).setSymmetricBlob(encryptedSymmetricBlob);
        verify(asymmetricKeyParams).getEncryptionScheme();
        verify(symmetricKey).getKey();
        verifyNoMoreInteractions(identityRequest, asymmetricKeyParams, structConverter,
                symmetricKey);
    }

    /**
     * Tests {@link AbstractAttestationCertificateAuthority#decryptAsymmetricBlob(byte[],
     * EncryptionScheme)}.
     *
     * @throws Exception during aca processing
     */
    @Test
    public void testDecryptAsymmetricBlob() throws Exception {

        // test encryption transformation
        EncryptionScheme encryptionScheme = EncryptionScheme.PKCS1;

        // test variables
        byte[] expected = "test".getBytes();

        // encrypt the expected value using same algorithm as the ACA.
        byte[] encrypted = encryptBlob(expected, encryptionScheme.toString());

        // perform the decryption and assert that the decrypted bytes equal the expected bytes
        assertEquals(aca.decryptAsymmetricBlob(encrypted, encryptionScheme), expected);
    }

    /**
     * Tests {@link AbstractAttestationCertificateAuthority#decryptSymmetricBlob(
     * byte[], byte[], byte[], String)}.
     *
     * @throws Exception during aca processing
     */
    @Test
    public void testDecryptSymmetricBlob() throws Exception {
        // test encryption transformation
        String transformation = "AES/CBC/PKCS5Padding";

        // test variables
        byte[] expected = "test".getBytes();

        // create a key generator to generate a "shared" secret
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);

        // use some random bytes as the IV to encrypt and subsequently decrypt with
        byte[] randomBytes = new byte[16];

        // generate the random bytes
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.nextBytes(randomBytes);

        // the shared secret
        byte[] secretKey = keyGenerator.generateKey().getEncoded();

        // encrypt the expected value with the private key being the shared secret
        byte[] encrypted = encryptBlob(expected, secretKey, randomBytes, transformation);

        // perform the decryption using the generated shared secert, random bytes as an IV, and the
        // AES CBC transformation for the cipher. then assert the decrypted results are the same
        // as our expected value.
        assertEquals(aca.decryptSymmetricBlob(encrypted, secretKey, randomBytes, transformation),
                expected);
    }

    /**
     * Tests {@link AbstractAttestationCertificateAuthority#generateSymmetricKey()}.
     */
    @Test
    public void testGenerateSymmetricKey() {
        // perform the test
        SymmetricKey symmetricKey = aca.generateSymmetricKey();

        // assert the symmetric algorithm, scheme, and key size are all set appropriately
        assertTrue(symmetricKey.getAlgorithmId() == 6);
        assertTrue(symmetricKey.getEncryptionScheme() == 255);
        assertTrue(symmetricKey.getKeySize() == symmetricKey.getKey().length);
    }

    /**
     * Tests {@link AbstractAttestationCertificateAuthority#generateAsymmetricContents(
     * IdentityProof, SymmetricKey, PublicKey)}.
     *
     * @throws Exception during aca processing
     */
    @Test
    public void testGenerateAsymmetricContents() throws Exception {

        // mocks for test
        IdentityProof proof = mock(IdentityProof.class);
        AsymmetricPublicKey publicKey = mock(AsymmetricPublicKey.class);
        StructConverter structConverter = mock(StructConverter.class);
        SymmetricKey symmetricKey = mock(SymmetricKey.class);

        // assign the mocked struct converter to the test object
        ReflectionTestUtils.setField(aca, "structConverter", structConverter);

        // "encoded" identity proof (returned by struct converter)
        byte[] identityProofEncoded = new byte[]{0, 0, 1, 1};

        // generate a random session key to be used for encryption and decryption
        byte[] sessionKey = new byte[16];
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.nextBytes(sessionKey);

        // when requesting the identity key from the proof, return the mocked public key
        when(proof.getIdentityKey()).thenReturn(publicKey);

        // when requesting to convert the public key, return the encoded identity proof
        when(structConverter.convert(publicKey)).thenReturn(identityProofEncoded);
        when(structConverter.convert(symmetricKey)).thenReturn(sessionKey);

        // perform the test
        byte[] result = aca.generateAsymmetricContents(proof, symmetricKey, keyPair.getPublic());

        // verify mock interactions
        verify(proof).getIdentityKey();
        verify(structConverter).convert(publicKey);
        verify(structConverter).convert(symmetricKey);
        verifyZeroInteractions(proof, structConverter, publicKey, symmetricKey);

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
        assertEquals(decryptedResult, expected);
    }

    /**
     * Tests {@link AbstractAttestationCertificateAuthority#generateAttestation(X509Certificate,
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
        keyGenerator.init(128);

        // obtain the key from the generator
        byte[] secretKey = keyGenerator.generateKey().getEncoded();

        // use our public key for encryption
        when(symmetricKey.getKey()).thenReturn(secretKey);

        // just use the existing public key for the credential
        when(certificate.getEncoded()).thenReturn(keyPair.getPublic().getEncoded());

        // perform the actual test
        SymmetricAttestation attestation = aca.generateAttestation(certificate, symmetricKey);

        // validate that the attestation is not null
        assertNotNull(attestation);

        // validate the attestation algorithm
        assertNotNull(attestation.getAlgorithm());
        assertTrue(attestation.getAlgorithm().getAlgorithmId() == 6);
        assertTrue(attestation.getAlgorithm().getEncryptionScheme() == 0x1);
        assertTrue(attestation.getAlgorithm().getSignatureScheme() == 0);
        assertTrue(attestation.getAlgorithm().getParamsSize() == 0);

        // validate the attestation credential
        assertNotNull(attestation.getCredential());

        // validate that the credential size is the size of the actual credential block
        assertTrue(attestation.getCredential().length == attestation.getCredentialSize());

        // create containers for the 2 parts of the credential
        byte[] iv = new byte[16];
        byte[] credential = new byte[attestation.getCredential().length - iv.length];

        // siphon off the first 16 bytes for the IV
        System.arraycopy(attestation.getCredential(), 0, iv, 0, iv.length);

        // the rest is the actual encrypted credential
        System.arraycopy(attestation.getCredential(), iv.length, credential, 0, credential.length);

        // decrypt the credential
        byte[] decrypted = decryptBlob(credential, secretKey, iv, "AES/CBC/PKCS5Padding");

        // assert that the decrypted credential is our public key
        assertEquals(keyPair.getPublic().getEncoded(), decrypted);

        // verify that the mocks were interacted with appropriately
        verify(symmetricKey).getKey();
        verify(certificate).getEncoded();
        verifyNoMoreInteractions(certificate, symmetricKey);
    }

    /**
     * Tests {@link AbstractAttestationCertificateAuthority#
     * AbstractAttestationCertificateAuthority(SupplyChainValidationService, PrivateKey,
     * X509Certificate, StructConverter, CertificateManager, DeviceRegister, int,
     * DeviceManager, DBManager)}.
     *
     * @throws Exception during subject alternative name checking if cert formatting is bad
     */
    @Test
    public void testGenerateCredential() throws Exception {
        // test variables
        final String identityProofLabelString = "label";
        byte[] identityProofLabel = identityProofLabelString.getBytes();
        byte[] modulus = ((RSAPublicKey) keyPair.getPublic()).getModulus().toByteArray();
        X500Principal principal = new X500Principal("CN=TEST, OU=TEST, O=TEST, C=TEST");
        int validDays = 1;

        // create mocks for testing
        IdentityProof identityProof = mock(IdentityProof.class);
        AsymmetricPublicKey asymmetricPublicKey = mock(AsymmetricPublicKey.class);
        StorePubKey storePubKey = mock(StorePubKey.class);
        X509Certificate acaCertificate = mock(X509Certificate.class);

        // assign ACA fields
        ReflectionTestUtils.setField(aca, "validDays", validDays);
        ReflectionTestUtils.setField(aca, "acaCertificate", acaCertificate);

        // prepare identity proof interactions
        when(identityProof.getLabel()).thenReturn(identityProofLabel);

        // prepare other mocks
        when(acaCertificate.getSubjectX500Principal()).thenReturn(principal);
        when(acaCertificate.getIssuerX500Principal()).thenReturn(principal);

        // perform the test
        X509Certificate certificate = aca.generateCredential(keyPair.getPublic(),
                null,
                new HashSet<PlatformCredential>(),
                "exampleIdLabel");

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
        assertEquals(certificate.getSubjectX500Principal().getName(), "");
        assertEquals(((X500Name) GeneralNames.fromExtensions(((TBSCertificate.getInstance(
                certificate.getTBSCertificate()).getExtensions())), Extension.
                subjectAlternativeName).getNames()[0].getName()).getRDNs(
                        IssuedCertificateAttributeHelper.TCPA_AT_TPM_ID_LABEL)[0].getFirst()
                .getValue().toString(), "exampleIdLabel");

        assertEquals(resultMod, modulus);

        // obtain the expiration dates from the certificate
        Calendar beforeDate = Calendar.getInstance();
        Calendar afterDate = Calendar.getInstance();
        beforeDate.setTime(certificate.getNotBefore());
        afterDate.setTime(certificate.getNotAfter());

        // assert the dates are set correctly
        assertEquals(beforeDate.get(Calendar.DATE), today.get(Calendar.DATE));
        assertEquals(afterDate.get(Calendar.DATE), tomorrow.get(Calendar.DATE));

        // validate mock interactions
        verify(acaCertificate).getSubjectX500Principal();
        verifyNoMoreInteractions(identityProof, asymmetricPublicKey, storePubKey, acaCertificate);
    }

    /**
     * Tests {@link AbstractAttestationCertificateAuthority#assemblePublicKey(byte[])}.
     */
    @Test
    public void testAssemblePublicKeyUsingByteArray() {
        // obtain the expected modulus from the existing public key
        final BigInteger modulus = ((RSAPublicKey) keyPair.getPublic()).getModulus();

        // perform test
        RSAPublicKey publicKey = (RSAPublicKey) aca.assemblePublicKey(modulus.toByteArray());

        // assert that the exponent and the modulus are the same. the exponents should be the well
        // known prime, 101
        assertTrue(publicKey.getPublicExponent().equals(new BigInteger("010001", 16)));
        assertTrue(publicKey.getModulus().equals(modulus));
    }

    /**
     * Tests {@link AbstractAttestationCertificateAuthority#assemblePublicKey(String)}.
     */
    @Test
    public void testAssemblePublicKeyUsingHexEncodedString() {
        // obtain the expected modulus from the existing public key
        final BigInteger modulus = ((RSAPublicKey) keyPair.getPublic()).getModulus();

        // encode our existing public key into hex
        final String modulusString = Hex.encodeHexString(
                ((RSAPublicKey) keyPair.getPublic()).getModulus().toByteArray());

        // perform test
        RSAPublicKey publicKey = (RSAPublicKey) aca.assemblePublicKey(modulusString);

        // assert that the exponent and the modulus are the same. the exponents should be the well
        // known prime, 101.
        assertTrue(publicKey.getPublicExponent().equals(new BigInteger("010001", 16)));
        assertTrue(publicKey.getModulus().equals(modulus));
    }

    /**
     * Tests parsing the EK from the TPM2 output file.
     * @throws URISyntaxException incorrect resource path
     * @throws IOException unable to read from file
     */
    @Test
    public void testParseEk() throws URISyntaxException, IOException {
        Path ekPath = Paths.get(getClass().getResource(
                EK_PUBLIC_PATH).toURI());

        byte[] ekFile = Files.readAllBytes(ekPath);

        RSAPublicKey ek = aca.parsePublicKey(ekFile);
        assertTrue(ek.getPublicExponent().equals(new BigInteger("010001", 16)));

        byte[] mod = ek.getModulus().toByteArray();
        // big integer conversion is signed so it can add a 0 byte
        if (mod[0] == 0) {
            byte[] tmp = new byte[mod.length - 1];
            System.arraycopy(mod, 1, tmp, 0, mod.length - 1);
            mod = tmp;
        }
        String hex = HexUtils.byteArrayToHexString(mod);
        String realMod = EK_MODULUS_HEX.replaceAll("\\s+", "");
        assertEquals(hex, realMod);
    }

    /**
     * Tests parsing the AK public key from the TPM2 output file.
     * @throws URISyntaxException incorrect resource path
     * @throws IOException unable to read from file
     */
    @Test
    public void testParseAk() throws URISyntaxException, IOException {
        Path akPath = Paths.get(getClass().getResource(
                AK_PUBLIC_PATH).toURI());

        byte[] akFile = Files.readAllBytes(akPath);

        RSAPublicKey ak = aca.parsePublicKey(akFile);
        assertTrue(ak.getPublicExponent().equals(new BigInteger("010001", 16)));

        byte[] mod = ak.getModulus().toByteArray();
        // big integer conversion is signed so it can add a 0 byte
        if (mod[0] == 0) {
            byte[] tmp = new byte[mod.length - 1];
            System.arraycopy(mod, 1, tmp, 0, mod.length - 1);
            mod = tmp;
        }
        String hex = HexUtils.byteArrayToHexString(mod);
        String realMod = AK_MODULUS_HEX.replaceAll("\\s+", "");
        assertEquals(hex, realMod);
    }

    /**
     * Tests parsing the AK name from the TPM2 output file.
     * @throws URISyntaxException incorrect resource path
     * @throws IOException unable to read from file
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
        byte[] akName = aca.generateAkName(HexUtils.hexStringToByteArray(realMod));

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
     * @throws URISyntaxException invalid file path
     * @throws IOException unable to read file
     */
    @Test(enabled = false)
    public void testMakeCredential() throws URISyntaxException, IOException {
        Path akPubPath = Paths.get(getClass().getResource(
                AK_PUBLIC_PATH).toURI());
        Path ekPubPath = Paths.get(getClass().getResource(
                EK_PUBLIC_PATH).toURI());

        byte[] ekPubFile = Files.readAllBytes(ekPubPath);
        byte[] akPubFile = Files.readAllBytes(akPubPath);

        RSAPublicKey ekPub = aca.parsePublicKey(ekPubFile);
        RSAPublicKey akPub = aca.parsePublicKey(akPubFile);

        // prepare the nonce and wrap it with keys
        byte[] nonce = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
        ByteString blob = aca.tpm20MakeCredential(ekPub, akPub, nonce);

        Path resources = Paths.get(getClass().getResource(
                "/").toURI()).getParent().getParent().getParent().getParent();
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
    private byte[] encryptBlob(byte[] blob, String transformation) throws Exception {
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
     * @param blob           to be encrypted
     * @param key            shared key
     * @param iv             to encrypt with
     * @param transformation of the encryption cipher
     * @return encrypted blob
     * @throws Exception
     */
    private byte[] encryptBlob(byte[] blob, byte[] key, byte[] iv, String transformation)
            throws Exception {
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
     * @param blob to be decrypted
     * @return decrypted blob
     * @throws Exception
     */
    private byte[] decryptBlob(byte[] blob) throws Exception {
        // initialize a cipher using the specified transformation
        Cipher cipher = Cipher.getInstance(EncryptionScheme.OAEP.toString());

        OAEPParameterSpec spec = new OAEPParameterSpec("Sha1", "MGF1",
                MGF1ParameterSpec.SHA1, new PSource.PSpecified("TCPA".getBytes()));

        // use our generated public key to encrypt
        cipher.init(Cipher.PRIVATE_KEY, keyPair.getPrivate(), spec);

        // return the cipher text
        return cipher.doFinal(blob);
    }

    /**
     * Test helper method that decrypts a blob using a shared key and IV using the specified.
     * transformation.
     *
     * @param blob           to be decrypted
     * @param key            shared key
     * @param iv             to decrypt with
     * @param transformation of the decryption cipher
     * @return decrypted blob
     * @throws Exception
     */
    private byte[] decryptBlob(byte[] blob, byte[] key, byte[] iv, String transformation)
            throws Exception {
        // initialize a cipher using the specified transformation
        Cipher cipher = Cipher.getInstance(transformation);

        // generate a secret key specification using the key and AES.
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

        // create IV parameter for key specification
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // encrypt using the key specification with the generated IV
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec);

        // return the cipher text
        return cipher.doFinal(blob);
    }

}
