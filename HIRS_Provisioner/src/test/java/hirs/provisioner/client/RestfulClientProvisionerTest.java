package hirs.provisioner.client;

import hirs.client.collector.DeviceInfoCollectorHelper;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.FirmwareInfo;
import hirs.data.persist.HardwareInfo;
import hirs.data.persist.NetworkInfo;
import hirs.data.persist.OSInfo;
import hirs.data.persist.OSName;
import hirs.data.persist.TPMInfo;
import hirs.structs.converters.StructConverter;
import hirs.structs.elements.tpm.AsymmetricPublicKey;
import hirs.tpm.tss.Tpm;
import hirs.tpm.tss.command.CommandException;
import hirs.tpm.tss.command.CommandResult;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.powermock.modules.testng.PowerMockTestCase;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.SerializationUtils;
import org.springframework.web.client.RestTemplate;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import hirs.structs.elements.aca.IdentityRequestEnvelope;
import hirs.structs.elements.aca.IdentityResponseEnvelope;
import hirs.structs.elements.aca.SymmetricAttestation;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test suite for the {@link RestfulClientProvisioner}.
 */
@PrepareForTest(DeviceInfoCollectorHelper.class)
public class RestfulClientProvisionerTest extends PowerMockTestCase {

    /**
     * Sets up PowerMockito.
     *
     * @return PowerMockObjectFactory
     */
    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    private static final String ACA_KEY_URL = "acaKeyUrl";

    private static final String ACA_IDENTITY_URL = "acaIdentityUrl";

    @Mock
    private Tpm mockTpm;

    @Mock
    private StructConverter mockStructConverter;

    @Mock
    private RestTemplate mockRestTemplate;

    @InjectMocks
    private RestfulClientProvisioner provisioner;

    /***
     * Initializes a unique test environment for each test.
     */
    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Verifies that there were no more additional invocations on the mocks after each test has been
     * completed.
     */
    @AfterMethod
    public void verifyMocks() {
        verifyNoMoreInteractions(mockStructConverter, mockRestTemplate, mockTpm);
    }

    /**
     * Tests {@link RestfulClientProvisioner#takeOwnership()}.
     */
    @Test
    public void testTakeOwnership() {
        // perform test
        provisioner.takeOwnership();

        // verify tpm was used appropriately
        verify(mockTpm).takeOwnership();
    }

    /**
     * Tests {@link RestfulClientProvisioner#getACAPublicKey()}.
     *
     * @throws Exception while generating key pair
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testGetACAPublicKey() throws Exception {
        // create a key pair generator to generate a temporary RSA key
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(512);

        // create the temporary key
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        // assign the ACA identity URL
        ReflectionTestUtils.setField(provisioner, "acaPublicKeyURL", ACA_KEY_URL);

        // mock response from the ACA
        ResponseEntity responseEntity = mock(ResponseEntity.class);

        // when our rest template is called, just return our mocked response
        when(mockRestTemplate.getForEntity(ACA_KEY_URL, byte[].class)).thenReturn(responseEntity);

        // when requesting the response, return our test public key
        when(responseEntity.getBody()).thenReturn(publicKey.getEncoded());

        // perform the actual test
        RSAPublicKey result = provisioner.getACAPublicKey();

        // verify that the key is correct
        assertEquals(result.getPublicExponent(), publicKey.getPublicExponent());
        assertEquals(result.getModulus(), publicKey.getModulus());

        // verify mock interactions
        verify(mockRestTemplate).getForEntity(ACA_KEY_URL, byte[].class);
        verify(responseEntity).getBody();
    }

    /**
     * Tests {@link RestfulClientProvisioner#createIdentityRequest(AsymmetricPublicKey,
     * String, DeviceInfoReport)}.
     * @throws Exception          if there is a problem encountered when mocking
     *                            DeviceInfoCollectorHelper
     */
    @Test
    public void createIdentityCredential() throws Exception {
        PowerMockito.spy(DeviceInfoCollectorHelper.class);
        final InetAddress ipAddress = getTestIpAddress();
        final byte[] macAddress = new byte[] {11, 22, 33, 44, 55, 66};

        // test parameters
        String uuid = "uuid";

        byte[] asymmetricBlob = new byte[]{1};
        byte[] request = new byte[]{2};
        byte[] ekModulus = new byte[]{3};
        byte[] ek = new byte[]{50};

        // test specific mocks
        AsymmetricPublicKey publicKey = mock(AsymmetricPublicKey.class);

        // when the converter is used to serialize public key, return known blob
        when(mockStructConverter.convert(publicKey)).thenReturn(asymmetricBlob);

        // when tpm collate identity is invoked using test blob and uuid, return known request
        when(mockTpm.collateIdentityRequest(asymmetricBlob, uuid)).thenReturn(request);

        // return known credential when asked.
        when(mockTpm.getEndorsementCredentialModulus()).thenReturn(ekModulus);
        when(mockTpm.getEndorsementCredential()).thenReturn(ek);


        PowerMockito.doReturn("AB12345").when(DeviceInfoCollectorHelper.class,
                "collectDmiDecodeValue", OSName.LINUX, "system-serial-number");

        PowerMockito.doReturn("AB12346").when(DeviceInfoCollectorHelper.class,
                "collectDmiDecodeValue", OSName.LINUX, "chassis-serial-number");

        // perform test
        final DeviceInfoReport deviceInfoReport = new DeviceInfoReport(
                new NetworkInfo("test hostname", ipAddress, macAddress),
                new OSInfo(), new FirmwareInfo(), new HardwareInfo(), new TPMInfo());
        IdentityRequestEnvelope envelope = provisioner.createIdentityRequest(publicKey, uuid,
                deviceInfoReport);

        // validate the identity request
        assertEquals(envelope.getEndorsementCredentialModulus(), ekModulus);
        assertEquals(envelope.getEndorsementCredentialModulusLength(), ekModulus.length);
        assertEquals(envelope.getEndorsementCredential(), ek);
        assertEquals(envelope.getEndorsementCredentialLength(), ek.length);
        assertEquals(envelope.getRequest(), request);
        assertEquals(envelope.getRequestLength(), request.length);

        int deviceInfoReportLength = envelope.getDeviceInfoReportLength();
        assertTrue(deviceInfoReportLength > 0);
        assertEquals(envelope.getDeviceInfoReport().length, deviceInfoReportLength);
        assertEquals(envelope.getDeviceInfoReport(),
                SerializationUtils.serialize(deviceInfoReport));

        // verify mock interactions
        verify(mockStructConverter).convert(publicKey);
        verify(mockTpm).collateIdentityRequest(asymmetricBlob, uuid);
        verify(mockTpm).getEndorsementCredentialModulus();
        verify(mockTpm).getEndorsementCredential();
        verifyNoMoreInteractions(publicKey);
    }


    /**
     * Tests {@link RestfulClientProvisioner#createIdentityRequest(AsymmetricPublicKey,
     * String, DeviceInfoReport)}.
     * @throws Exception          if there is a problem encountered when mocking
     *                            DeviceInfoCollectorHelper
     */
    @Test
    public void createIdentityCredentialEkNotFound() throws Exception {
        PowerMockito.spy(DeviceInfoCollectorHelper.class);
        final InetAddress ipAddress = getTestIpAddress();
        final byte[] macAddress = new byte[] {11, 22, 33, 44, 55, 66};

        // test parameters
        String uuid = "uuid";

        byte[] asymmetricBlob = new byte[]{1};
        byte[] request = new byte[]{2};
        byte[] ekModulus = new byte[]{3};

        // test specific mocks
        AsymmetricPublicKey publicKey = mock(AsymmetricPublicKey.class);

        // when the converter is used to serialize public key, return known blob
        when(mockStructConverter.convert(publicKey)).thenReturn(asymmetricBlob);

        // when tpm collate identity is invoked using test blob and uuid, return known request
        when(mockTpm.collateIdentityRequest(asymmetricBlob, uuid)).thenReturn(request);

        // return known credential when asked.
        when(mockTpm.getEndorsementCredentialModulus()).thenReturn(ekModulus);
        when(mockTpm.getEndorsementCredential()).thenThrow(new CommandException("no good",
                new CommandResult("EK not found", -1)));


        PowerMockito.doReturn("AB12345").when(DeviceInfoCollectorHelper.class,
                "collectDmiDecodeValue", OSName.LINUX, "system-serial-number");

        PowerMockito.doReturn("AB12346").when(DeviceInfoCollectorHelper.class,
                "collectDmiDecodeValue", OSName.LINUX, "chassis-serial-number");

        // perform test
        final DeviceInfoReport deviceInfoReport = new DeviceInfoReport(
                new NetworkInfo("test hostname", ipAddress, macAddress),
                new OSInfo(), new FirmwareInfo(), new HardwareInfo(), new TPMInfo());
        IdentityRequestEnvelope envelope = provisioner.createIdentityRequest(publicKey, uuid,
                deviceInfoReport);

        // validate the identity request
        assertEquals(envelope.getEndorsementCredentialModulus(), ekModulus);
        assertEquals(envelope.getEndorsementCredentialModulusLength(), ekModulus.length);
        assertNotNull(envelope.getEndorsementCredential());
        assertEquals(envelope.getEndorsementCredentialLength(), 1);
        assertEquals(envelope.getRequest(), request);
        assertEquals(envelope.getRequestLength(), request.length);

        int deviceInfoReportLength = envelope.getDeviceInfoReportLength();
        assertTrue(deviceInfoReportLength > 0);
        assertEquals(envelope.getDeviceInfoReport().length, deviceInfoReportLength);
        assertEquals(envelope.getDeviceInfoReport(),
                SerializationUtils.serialize(deviceInfoReport));

        // verify mock interactions
        verify(mockStructConverter).convert(publicKey);
        verify(mockTpm).collateIdentityRequest(asymmetricBlob, uuid);
        verify(mockTpm).getEndorsementCredentialModulus();
        verify(mockTpm).getEndorsementCredential();
        verifyNoMoreInteractions(publicKey);
    }

    /**
     * Tests {@link RestfulClientProvisioner#attestIdentityRequest(IdentityRequestEnvelope)}.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testAttestIdentityRequest() {

        // create some test variables
        byte[] requestBytes = new byte[]{1};
        byte[] responseBytes = new byte[]{2};

        // create some test mocks
        IdentityRequestEnvelope request = mock(IdentityRequestEnvelope.class);
        IdentityResponseEnvelope response = mock(IdentityResponseEnvelope.class);
        ResponseEntity responseEntity = mock(ResponseEntity.class);

        // assign the ACA identity URL
        ReflectionTestUtils.setField(provisioner, "acaIdentityURL", ACA_IDENTITY_URL);

        // the mock converter should serialize the identity request to bytes and likewise
        // deserialize the identity response
        when(mockStructConverter.convert(request)).thenReturn(requestBytes);
        when(mockStructConverter.convert(responseBytes, IdentityResponseEnvelope.class))
                .thenReturn(response);

        // the template should be used to send a post to the assigned url
        when(mockRestTemplate.exchange(eq(ACA_IDENTITY_URL), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(byte[].class))).thenReturn(responseEntity);

        // the aca response body should be acquired in order to convert into a response object
        when(responseEntity.getBody()).thenReturn(responseBytes);

        // perform the test
        provisioner.attestIdentityRequest(request);

        // verify converter mock
        verify(mockStructConverter).convert(request);
        verify(mockStructConverter).convert(responseBytes, IdentityResponseEnvelope.class);

        // setup captor for rest template verification.
        ArgumentCaptor<HttpEntity> argument = ArgumentCaptor.forClass(HttpEntity.class);

        // verify the rest template and capture the argument for the response entity type
        verify(mockRestTemplate).exchange(eq(ACA_IDENTITY_URL), eq(HttpMethod.POST),
                argument.capture(), eq(byte[].class));

        // verify response entity body is acquired
        verify(responseEntity).getBody();

        // assert that the http entity object being sent to the aca contained the request bytes
        assertEquals(argument.getValue().getBody(), requestBytes);

        // verify test specific mocks had no other unintended interactions
        verifyNoMoreInteractions(request, response, responseEntity);
    }

    /**
     * Tests {@link RestfulClientProvisioner#activateCredential(IdentityResponseEnvelope,
     * String)}.
     */
    @Test
    public void testActivateCredential() {
        // create test parameters
        String uuid = "uuid";
        byte[] asymmetricContents = new byte[]{1};
        byte[] symmetricContents = new byte[]{2};
        byte[] response = new byte[]{3};

        // create test specific mocks
        IdentityResponseEnvelope envelope = mock(IdentityResponseEnvelope.class);
        SymmetricAttestation attestation = mock(SymmetricAttestation.class);

        // both asymmetric and symmetric contents should be obtained
        when(envelope.getAsymmetricContents()).thenReturn(asymmetricContents);
        when(envelope.getSymmetricAttestation()).thenReturn(attestation);

        // the tpm should be sent both contents and the specified uuid
        when(mockTpm.activateIdentity(asymmetricContents, symmetricContents, uuid))
                .thenReturn(response);

        // the symmetric contents need to be converted
        when(mockStructConverter.convert(attestation)).thenReturn(symmetricContents);

        // perform the actual test
        provisioner.activateCredential(envelope, uuid);

        // verify mock interactions
        verify(mockTpm).activateIdentity(asymmetricContents, symmetricContents, uuid);
        verify(envelope).getAsymmetricContents();
        verify(envelope).getSymmetricAttestation();
        verify(mockStructConverter).convert(attestation);

        // ensure test specific mocks have no unintended interactions
        verifyNoMoreInteractions(envelope, attestation);
    }

    private static InetAddress getTestIpAddress() {
        try {
            return InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
