package hirs.attestationca.persist.provision.service;

import hirs.attestationca.persist.enums.TpmEccCurve;
import hirs.attestationca.persist.exceptions.CertificateProcessingException;
import hirs.attestationca.persist.provision.helper.ProvisionUtils;
import hirs.attestationca.persist.provision.helper.TpmPublicHelper;
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
import java.security.InvalidAlgorithmParameterException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
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
    private static final String EK_PUBLIC_KEY_PATH = "/tpm2/ek.pub";

    private static final String AK_PUBLIC_KEY_PATH = "/tpm2/ak.pub";

    private static final String AK_NAME_PATH = "/tpm2/ak.name";

    private static final String EK_MODULUS_PATH = "/tpm2/ek.mod";

    private static final String AK_MODULUS_PATH = "/tpm2/ak.mod";

    private static final String AK_NAME_HEX = "00 0b 6e 8f 79 1c 7e 16  96 1b 11 71 65 9c e0 cd"
            + "ae 0d 4d aa c5 41 be 58  89 74 67 55 96 c2 5e 38"
            + "e2 94";

    private AutoCloseable mocks;

    private KeyPair keyPair;
    private KeyPair ecKeyPair;
    private static final String EC_JAVA_CURVE_NAME = "secp256r1";

    @InjectMocks
    private AttestationCertificateAuthorityServiceImpl attestationCertificateAuthorityService;

    @Mock
    private CertificateRequestProcessorService certificateRequestProcessorService;

    @Mock
    private IdentityClaimProcessorService identityClaimProcessorService;

    /**
     * Setups configuration prior to each test method.
     *
     * @throws NoSuchAlgorithmException if issues arise while generating keypair.
     */
    @BeforeEach
    public void setupTests() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        // Initializes mocks before each test
        mocks = MockitoAnnotations.openMocks(this);

        final int keySize = 2048;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        keyPair = keyPairGenerator.generateKeyPair();

        KeyPairGenerator ecKeyPairGenerator = KeyPairGenerator.getInstance("EC"); // EC key pair generation
        ECGenParameterSpec ecSpec = new ECGenParameterSpec(EC_JAVA_CURVE_NAME);
        ecKeyPairGenerator.initialize(ecSpec);
        ecKeyPair = ecKeyPairGenerator.generateKeyPair();
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
     * Tests {@link AttestationCertificateAuthorityServiceImpl#processIdentityClaimTpm2(byte[])}
     * where the byte array is null or empty. Expects an {@link IllegalArgumentException} to be thrown.
     */
    @Test
    public void testProcessIdentityClaimTpm2NullOrEmptyRequest() {
        final String expectedExceptionMsg = "The IdentityClaim sent by the client cannot be null or empty.";

        // test 1: test null identity claim
        when(identityClaimProcessorService.processIdentityClaimTpm2(null)).thenThrow(
                new IllegalArgumentException(expectedExceptionMsg));

        // Act & Assert: Verify that the exception is thrown
        String actualExceptionMsg = assertThrows(IllegalArgumentException.class,
                () -> attestationCertificateAuthorityService.processIdentityClaimTpm2(null)).getMessage();

        assertEquals(expectedExceptionMsg, actualExceptionMsg);

        // test 2: test empty identity claim

        // initialize an empty byte array
        final byte[] emptyArr = {};

        when(identityClaimProcessorService.processIdentityClaimTpm2(emptyArr)).thenThrow(
                new IllegalArgumentException(expectedExceptionMsg));

        // Act & Assert: Verify that the exception is thrown
        actualExceptionMsg = assertThrows(IllegalArgumentException.class,
                () -> attestationCertificateAuthorityService.processIdentityClaimTpm2(emptyArr)).getMessage();

        assertEquals(expectedExceptionMsg, actualExceptionMsg);
    }

    /**
     * Tests {@link AttestationCertificateAuthorityServiceImpl#processIdentityClaimTpm2(byte[])}.
     */
    @Test
    public void testProcessIdentityClaimTpm2() {
        final byte[] identityClaim = {0, 1, 0, 1, 2, 2, 2};

        final byte[] expectedIdentityClaimResponse = {1, 1, 1, 1, 2, 2, 2, 2};

        when(identityClaimProcessorService.processIdentityClaimTpm2(identityClaim)).thenReturn(
                expectedIdentityClaimResponse);

        final byte[] actualCertificateResponse =
                identityClaimProcessorService.processIdentityClaimTpm2(identityClaim);

        // Assert that the byte arrays match
        assertArrayEquals(expectedIdentityClaimResponse, actualCertificateResponse);
    }

    /**
     * Tests {@link AttestationCertificateAuthorityServiceImpl#processCertificateRequest(byte[])}
     * where the byte array is null or empty. Expects an {@link IllegalArgumentException} to be thrown.
     */
    @Test
    public void testProcessCertificateRequestNullOrEmptyRequest() {
        final String expectedExceptionMsg = "The CertificateRequest sent by the client cannot be null or empty.";

        // test 1: test null certificate request
        when(certificateRequestProcessorService.processCertificateRequest(null)).thenThrow(
                new IllegalArgumentException(expectedExceptionMsg));

        // Act & Assert: Verify that the exception is thrown
        String actualExceptionMsg = assertThrows(IllegalArgumentException.class,
                () -> attestationCertificateAuthorityService.processCertificateRequest(null)).getMessage();

        assertEquals(expectedExceptionMsg, actualExceptionMsg);

        // test 2: test empty certificate request

        // initialize an empty byte array
        final byte[] emptyArr = {};

        when(certificateRequestProcessorService.processCertificateRequest(emptyArr)).thenThrow(
                new IllegalArgumentException(expectedExceptionMsg));

        // Act & Assert: Verify that the exception is thrown
        actualExceptionMsg = assertThrows(IllegalArgumentException.class,
                () -> attestationCertificateAuthorityService.processCertificateRequest(emptyArr)).getMessage();

        assertEquals(expectedExceptionMsg, actualExceptionMsg);
    }

    /**
     * Tests {@link AttestationCertificateAuthorityServiceImpl#processCertificateRequest(byte[])}
     * where the byte array is invalid. Expects a {@link CertificateProcessingException} to be thrown.
     */
    @Test
    public void testProcessCertificateRequestProcessorDeserializationError() {
        final String expectedExceptionMsg = "Could not deserialize Protobuf Certificate Request object";

        final byte[] badCertificateRequest = {0, 0, 0, 0, 0, 1, 0, 0};

        when(certificateRequestProcessorService.processCertificateRequest(badCertificateRequest)).thenThrow(
                new CertificateProcessingException(expectedExceptionMsg));

        // Act & Assert: Verify that the exception is thrown
        String actualExceptionMsg = assertThrows(CertificateProcessingException.class,
                () -> attestationCertificateAuthorityService.processCertificateRequest(
                        badCertificateRequest)).getMessage();

        assertEquals(expectedExceptionMsg, actualExceptionMsg);
    }

    /**
     * Tests {@link AttestationCertificateAuthorityServiceImpl#processCertificateRequest(byte[])}.
     */
    @Test
    public void testProcessCertificateRequest() {
        final byte[] certificateRequest = {0, 1, 0, 1};

        final byte[] expectedCertificateResponse = {1, 1, 1, 1};

        when(certificateRequestProcessorService.processCertificateRequest(certificateRequest)).thenReturn(
                expectedCertificateResponse);

        final byte[] actualCertificateResponse =
                attestationCertificateAuthorityService.processCertificateRequest(certificateRequest);

        // Assert that the byte arrays match
        assertArrayEquals(expectedCertificateResponse, actualCertificateResponse);
    }

    /**
     * Tests {@link AttestationCertificateAuthorityServiceImpl#getLeafACACertPublicKey()}.
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
     * Tests {@link TpmPublicHelper#assembleRSAPublicKey}.
     */
    @Test
    public void testAssembleRSAPublicKeyUsingByteArray() {
        // obtain the expected modulus from the existing public key
        final BigInteger modulus = ((RSAPublicKey) keyPair.getPublic()).getModulus();
        final BigInteger exponent = ((RSAPublicKey) keyPair.getPublic()).getPublicExponent();

        // perform test
        RSAPublicKey publicKey = TpmPublicHelper.assembleRSAPublicKey(modulus, exponent);

        // assert that the exponent and the modulus are the same. the exponents should be the well
        // known prime, 101
        final int radix = 16;
        assertEquals(new BigInteger("010001", radix), publicKey.getPublicExponent());
        assertEquals(publicKey.getModulus(), modulus);
    }

    /**
     * Tests {@link TpmPublicHelper#assembleECCPublicKey}.
     */
    @Test
    public void testAssembleECCPublicKey() {
        // obtain the expected curve and point from the existing EC public key
        final EllipticCurve ecCurve = ((ECPublicKey) ecKeyPair.getPublic()).getParams().getCurve();
        final ECPoint ecPoint = ((ECPublicKey) ecKeyPair.getPublic()).getW();
        final TpmEccCurve ecTpmCurve = TpmEccCurve.fromJavaName(EC_JAVA_CURVE_NAME).orElseThrow();

        ECPublicKey outputPubKey = TpmPublicHelper.assembleECCPublicKey(ecTpmCurve, ecPoint); // perform test

        // assert that the EC family and curve points match
        final ECParameterSpec outputParamSpec = outputPubKey.getParams();
        assertEquals(outputParamSpec.getCurve(), ecCurve);
        assertEquals(ecPoint, outputPubKey.getW());
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
        Path ekModPath = Paths.get(Objects.requireNonNull(getClass().getResource(EK_MODULUS_PATH)).toURI());

        byte[] ekFile = Files.readAllBytes(ekPath);
        String realMod = Files.readString(ekModPath).replaceAll("\\s+", "");

        RSAPublicKey ek = (RSAPublicKey) ProvisionUtils.parsePublicKeyFromPublicDataSegment(ekFile);
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
        Path akModPath = Paths.get(Objects.requireNonNull(getClass().getResource(AK_MODULUS_PATH)).toURI());

        byte[] akFile = Files.readAllBytes(akPath);
        String realMod = Files.readString(akModPath).replaceAll("\\s+", "");

        RSAPublicKey ak = (RSAPublicKey) ProvisionUtils.parsePublicKeyFromPublicDataSegment(akFile);
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
        assertEquals(realMod, hex);
    }
}
