package hirs.attestationca.persist;

import hirs.attestationca.persist.exceptions.CertificateProcessingException;
import hirs.attestationca.persist.provision.CertificateRequestProcessor;
import hirs.attestationca.persist.provision.IdentityClaimProcessor;
import hirs.attestationca.persist.provision.helper.ProvisionUtils;
import hirs.utils.HexUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link AttestationCertificateAuthorityServiceImpl} service class.
 */
public class AttestationCertificateAuthorityServiceTest {
    private static final String EK_PUBLIC_KEY_PATH = "/public_keys/ek.pub";

    private static final String AK_PUBLIC_KEY_PATH = "/public_keys/ak.pub";

    private static final String AK_NAME_PATH = "/public_keys/ak.name";

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

    private AutoCloseable mocks;

    private KeyPair keyPair;

    @InjectMocks
    private AttestationCertificateAuthorityServiceImpl attestationCertificateAuthorityService;

    @Mock
    private CertificateRequestProcessor certificateRequestProcessor;

    @Mock
    private IdentityClaimProcessor identityClaimProcessor;

    /**
     * Setups configuration prior to each test method.
     *
     * @throws NoSuchAlgorithmException if issues arise while generating keypair.
     */
    @BeforeEach
    public void setupTests() throws NoSuchAlgorithmException {
        // Initializes mocks before each test
        mocks = MockitoAnnotations.openMocks(this);

        final int keySize = 2048;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        keyPair = keyPairGenerator.generateKeyPair();
    }

    /**
     * Closes mocks after the completion of each test method.
     *
     * @throws Exception if any issues arise while closing mocks.
     */
    @AfterEach
    public void afterEach() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * Tests {@link AttestationCertificateAuthorityService#processIdentityClaimTpm2(byte[])}
     * where the byte array is null or empty. Expects an {@link IllegalArgumentException} to be thrown.
     */
    @Test
    public void testProcessIdentityClaimTpm2NullOrEmptyRequest() {
        final String expectedExceptionMsg = "The IdentityClaim sent by the client cannot be null or empty.";

        // test 1: test null identity claim
        when(identityClaimProcessor.processIdentityClaimTpm2(null)).thenThrow(
                new IllegalArgumentException(expectedExceptionMsg));

        // Act & Assert: Verify that the exception is thrown
        String actualExceptionMsg = assertThrows(IllegalArgumentException.class,
                () -> attestationCertificateAuthorityService.processIdentityClaimTpm2(null)).getMessage();

        assertEquals(expectedExceptionMsg, actualExceptionMsg);

        // test 2: test empty identity claim

        // initialize an empty byte array
        final byte[] emptyArr = {};

        when(identityClaimProcessor.processIdentityClaimTpm2(emptyArr)).thenThrow(
                new IllegalArgumentException(expectedExceptionMsg));

        // Act & Assert: Verify that the exception is thrown
        actualExceptionMsg = assertThrows(IllegalArgumentException.class,
                () -> attestationCertificateAuthorityService.processIdentityClaimTpm2(emptyArr)).getMessage();

        assertEquals(expectedExceptionMsg, actualExceptionMsg);
    }

    /**
     * Tests {@link AttestationCertificateAuthorityService#processIdentityClaimTpm2(byte[])}.
     */
    @Test
    public void testProcessIdentityClaimTpm2() {
        final byte[] identityClaim = {0, 1, 0, 1, 2, 2, 2};

        final byte[] expectedIdentityClaimResponse = {1, 1, 1, 1, 2, 2, 2, 2};

        when(identityClaimProcessor.processIdentityClaimTpm2(identityClaim)).thenReturn(
                expectedIdentityClaimResponse);

        final byte[] actualCertificateResponse =
                identityClaimProcessor.processIdentityClaimTpm2(identityClaim);

        // Assert that the byte arrays match
        assertArrayEquals(expectedIdentityClaimResponse, actualCertificateResponse);
    }

    /**
     * Tests {@link AttestationCertificateAuthorityService#processCertificateRequest(byte[])}
     * where the byte array is null or empty. Expects an {@link IllegalArgumentException} to be thrown.
     */
    @Test
    public void testProcessCertificateRequestNullOrEmptyRequest() {
        final String expectedExceptionMsg = "The CertificateRequest sent by the client cannot be null or empty.";

        // test 1: test null certificate request
        when(certificateRequestProcessor.processCertificateRequest(null)).thenThrow(
                new IllegalArgumentException(expectedExceptionMsg));

        // Act & Assert: Verify that the exception is thrown
        String actualExceptionMsg = assertThrows(IllegalArgumentException.class,
                () -> attestationCertificateAuthorityService.processCertificateRequest(null)).getMessage();

        assertEquals(expectedExceptionMsg, actualExceptionMsg);

        // test 2: test empty certificate request

        // initialize an empty byte array
        final byte[] emptyArr = {};

        when(certificateRequestProcessor.processCertificateRequest(emptyArr)).thenThrow(
                new IllegalArgumentException(expectedExceptionMsg));

        // Act & Assert: Verify that the exception is thrown
        actualExceptionMsg = assertThrows(IllegalArgumentException.class,
                () -> attestationCertificateAuthorityService.processCertificateRequest(emptyArr)).getMessage();

        assertEquals(expectedExceptionMsg, actualExceptionMsg);
    }

    /**
     * Tests {@link AttestationCertificateAuthorityService#processCertificateRequest(byte[])}
     * where the byte array is invalid. Expects a {@link CertificateProcessingException} to be thrown.
     */
    @Test
    public void testProcessCertificateRequestProcessorDeserializationError() {
        final String expectedExceptionMsg = "Could not deserialize Protobuf Certificate Request object";

        final byte[] badCertificateRequest = {0, 0, 0, 0, 0, 1, 0, 0};

        when(certificateRequestProcessor.processCertificateRequest(badCertificateRequest)).thenThrow(
                new CertificateProcessingException(expectedExceptionMsg));

        // Act & Assert: Verify that the exception is thrown
        String actualExceptionMsg = assertThrows(CertificateProcessingException.class,
                () -> attestationCertificateAuthorityService.processCertificateRequest(
                        badCertificateRequest)).getMessage();

        assertEquals(expectedExceptionMsg, actualExceptionMsg);
    }

    /**
     * Tests {@link AttestationCertificateAuthorityService#processCertificateRequest(byte[])}.
     */
    @Test
    public void testProcessCertificateRequest() {
        final byte[] certificateRequest = {0, 1, 0, 1};

        final byte[] expectedCertificateResponse = {1, 1, 1, 1};

        when(certificateRequestProcessor.processCertificateRequest(certificateRequest)).thenReturn(
                expectedCertificateResponse);

        final byte[] actualCertificateResponse =
                attestationCertificateAuthorityService.processCertificateRequest(certificateRequest);

        // Assert that the byte arrays match
        assertArrayEquals(expectedCertificateResponse, actualCertificateResponse);
    }

    /**
     * Tests {@link AttestationCertificateAuthorityService#getLeafACACertPublicKey()}.
     */
    @Test
    public void testGetPublicKey() {
        // encoded byte array to be returned by public key
        final byte[] expectedByteArray = new byte[]{0, 1, 0, 1, 0};

        // create mocks for testing
        X509Certificate mockCertificate = mock(X509Certificate.class);
        PublicKey mockPublicKey = mock(PublicKey.class);

        // Mock the behavior of getPublicKey().getEncoded() to return the desired byte array
        when(mockCertificate.getPublicKey()).thenReturn(mockPublicKey);
        when(mockPublicKey.getEncoded()).thenReturn(expectedByteArray);

        // grab the public key encoding
        byte[] mockedByteArrayResult = mockPublicKey.getEncoded();

        // Mock the behavior of retrieving the public key from the service class
        when(attestationCertificateAuthorityService.getLeafACACertPublicKey()).thenReturn(mockedByteArrayResult);

        // Test: Call the service method and assert the return value
        byte[] actualByteArray = attestationCertificateAuthorityService.getLeafACACertPublicKey();

        // Assert that the mocked and actual byte arrays match
        assertArrayEquals(expectedByteArray, actualByteArray);
    }

    /**
     * Tests {@link ProvisionUtils#assembleRSAPublicKey(byte[])}.
     */
    @Test
    public void testAssembleRSAPublicKeyUsingByteArray() {
        // obtain the expected modulus from the existing public key
        final BigInteger modulus = ((RSAPublicKey) keyPair.getPublic()).getModulus();

        // perform test
        RSAPublicKey publicKey = (RSAPublicKey) ProvisionUtils.assembleRSAPublicKey(modulus.toByteArray());

        // assert that the exponent and the modulus are the same. the exponents should be the well
        // known prime, 101
        final int radix = 16;
        assertEquals(new BigInteger("010001", radix), publicKey.getPublicExponent());
        assertEquals(publicKey.getModulus(), modulus);
    }

    /**
     * Tests parsing the EK from the TPM2 output file.
     *
     * @throws URISyntaxException incorrect resource path
     * @throws IOException        unable to read from file
     */
    @Test
    public void testParseEk() throws URISyntaxException, IOException {
        Path ekPath = Paths.get(Objects.requireNonNull(getClass().getResource(EK_PUBLIC_KEY_PATH)).toURI());

        byte[] ekFile = Files.readAllBytes(ekPath);

        RSAPublicKey ek = ProvisionUtils.parseRSAKeyFromPublicDataSegment(ekFile);
        final int radix = 16;
        assertEquals(new BigInteger("010001", radix), ek.getPublicExponent());

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
        Path akPath = Paths.get(Objects.requireNonNull(getClass().getResource(AK_PUBLIC_KEY_PATH)).toURI());

        byte[] akFile = Files.readAllBytes(akPath);

        RSAPublicKey ak = ProvisionUtils.parseRSAKeyFromPublicDataSegment(akFile);
        final int radix = 16;
        assertEquals(new BigInteger("010001", radix), ak.getPublicExponent());

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
}
