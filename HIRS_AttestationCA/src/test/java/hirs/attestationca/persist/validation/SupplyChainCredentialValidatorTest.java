package hirs.attestationca.persist.validation;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.CertificateTest;

import hirs.attestationca.persist.entity.userdefined.info.*;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.utils.enums.DeviceInfoEnums;
import org.apache.logging.log4j.core.util.IOUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.AttributeCertificateHolder;
import org.bouncycastle.cert.AttributeCertificateIssuer;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2AttributeCertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Tests the SupplyChainValidator class.
 */
@PrepareForTest({SupplyChainCredentialValidator.class,
        PlatformCredential.class, EndorsementCredential.class })
@PowerMockIgnore({"javax.xml.parsers.*", "org.apache.xerces.jaxp.*", "org.apache.logging.log4j.*",
        "javax.security.auth.*" })
public class SupplyChainCredentialValidatorTest {

    private static final String JSON_FILE = "/config/component-class.json";
    private static final String SAMPLE_PACCOR_OUTPUT_TXT = "sample_paccor_output.txt";
    private static final String SAMPLE_PACCOR_OUTPUT_NOT_SPECIFIED_TXT
            = "sample_paccor_output_not_specified_values.txt";
    private static final String SAMPLE_TEST_PACCOR_CERT
            = "/validation/platform_credentials_2/paccor_platform_cert.crt";

    private static final String SAMPLE_PACCOR_OUTPUT_WITH_EXTRA_COMPONENT_TXT
            = "sample_paccor_output_with_extra_component.txt";

    private final SupplyChainCredentialValidator supplyChainCredentialValidator =
            new SupplyChainCredentialValidator();

    private final CredentialValidator credentialValidator =
            new CredentialValidator();

    private static KeyStore keyStore;
    private static KeyStore emptyKeyStore;
    /**
     * File name used to initialize a test KeyStore.
     */
    static final String KEY_STORE_FILE_NAME = "TestKeyStore";
    /**
     * SecureRandom instance.
     */
    static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String TEST_SIGNING_KEY = "/validation/platform_credentials/ca.pub";

    private static final String TEST_PLATFORM_CRED =
            "/validation/platform_credentials/plat_cert1.pem";
    private static final String TEST_PLATFORM_CRED2 =
            "/validation/platform_credentials/plat_cert2.pem";

    private static final String TEST_PLATFORM_CRED_BASE_CHASIS_COMBO =
            "/validation/platform_credentials/Intel_pc5.pem";

    private static final String TEST_SERIAL_NUMBER = "BQKP52840678";
    private static final String TEST_COMPONENT_MANUFACTURER = "Intel";
    private static final String TEST_COMPONENT_MODEL = "platform2018";
    private static final String TEST_COMPONENT_REVISION = "1.0";
    private static final String BAD_SERIAL = "BAD_SERIAL";

    //-------Actual ST Micro Endorsement Credential Certificate Chain!--------------
    private static final String EK_CERT = "/certificates/ab21ccf2-tpmcert.pem";
    private static final String INT_CA_CERT02 = "/certificates/fakestmtpmekint02.pem";

    //-------Generated Intel Credential Certificate Chain--------------
    private static final String INTEL_PLATFORM_CERT =
            "/validation/platform_credentials/plat_cert3.pem";
    private static final String INTEL_INT_CA =
            "/validation/platform_credentials/intel_chain/root/intermediate1.crt";
    private static final String FAKE_ROOT_CA =
            "/validation/platform_credentials/intel_chain/root/rootca.crt";
    private static final String PLATFORM_MANUFACTURER = "Intel";
    private static final String PLATFORM_MODEL = "S2600KP";
    private static final String PLATFORM_VERSION = "H76962-350";

    //-------Original Intel Credential Certificate Chain--------------
    private static final String INTEL_PLATFORM_CERT_ORIG =
            "/certificates/fakeIntel_S2600KP_F00F00F00F00.pem";
    private static final String INTEL_ORIG_INT_CA_ORIG =
            "/certificates/fakeIntelIntermediateCA.pem";
    private static final String FAKE_ROOT_CA_ORIG =
            "/certificates/fakeCA.pem";

    //-------Fake SGI Credential Certificate Chain--------------
    private static final String SGI_PLATFORM_CERT = "/certificates/fakeSGI_J2_F00F00F0.pem";
    private static final String SGI_INT_CA = "/certificates/fakeSGIIntermediateCA.pem";
    private static final String SGI_CRED_SERIAL_NUMBER = "F00F00F0";

    //-------Actual Intel NUC Platform --------------
    private static final String NUC_PLATFORM_CERT =
            "/certificates/Intel_nuc_pc.pem";
    private static final String NUC_PLATFORM_CERT_SERIAL_NUMBER = "GETY421001DY";

    private static final String NUC_PLATFORM_CERT2 =
            "/certificates/Intel_nuc_pc2.pem";
    private static final String NUC_PLATFORM_CERT_SERIAL_NUMBER2 = "GETY4210001M";

    private static final String INTEL_SIGNING_KEY = "/certificates/IntelSigningKey_20April2017.pem";

    private static final String NEW_NUC1 =
            "/validation/platform_credentials/Intel_pc3.cer";

    /**
     * Sets up a KeyStore for testing.
     *
     * @throws KeyStoreException
     *             if no Provider supports a KeyStoreSpi implementation for the specified type.
     * @throws NoSuchAlgorithmException
     *             if the algorithm used to check the integrity of the keystore cannot be found
     * @throws CertificateException
     *             if any of the certificates in the keystore could not be loaded
     * @throws IOException
     *             if there is an I/O or format problem with the keystore data, if a password is
     *             required but not given, or if the given password was incorrect
     */
    @BeforeAll
    public static void setUp() throws KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException {
        Security.addProvider(new BouncyCastleProvider());

        keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        emptyKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        char[] password = "password".toCharArray();
        keyStore.load(null, password);

        try (FileOutputStream fos = new FileOutputStream(KEY_STORE_FILE_NAME)) {
            keyStore.store(fos, password);
        }
    }

    /**
     * Ensures the key store file is deleted after testing.
     */
    @AfterAll
    public static void tearDown() {
        File f = new File(KEY_STORE_FILE_NAME);
        if (!f.delete()) {
            fail("file was not cleaned up");
        }

    }

    /**
     * Checks if the ST Micro Endorsement Credential can be validated against the
     * ST/GlobalSIgn Certificate Chain.
     * @throws IOException if error occurs while reading files
     * @throws URISyntaxException if error occurs while reading files
     * @throws CertificateException if error occurs while processing X509 Certs
     * @throws KeyStoreException if error occurs while processing Keystore
     */
    @Test
    public final void testValidateEndorsementCredential()
            throws URISyntaxException, IOException, CertificateException, KeyStoreException {
        Certificate rootcacert, intermediateca02cert;

        EndorsementCredential ekcert = new EndorsementCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(EK_CERT).toURI()))
        );

        intermediateca02cert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(INT_CA_CERT02).toURI()))
        );

        rootcacert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(FAKE_ROOT_CA_ORIG).toURI()))
        );

        try {
            keyStore.setCertificateEntry("CA cert", rootcacert.getX509Certificate());
            keyStore.setCertificateEntry("Intel Intermediate Cert",
                    intermediateca02cert.getX509Certificate());

            AppraisalStatus result = credentialValidator.validateEndorsementCredential(
                    ekcert, keyStore, true);
            assertEquals(result.getAppStatus(), AppraisalStatus.Status.PASS);
            assertEquals(result.getMessage(),
                    SupplyChainCredentialValidator.ENDORSEMENT_VALID);
        }  finally {
            keyStore.deleteEntry("Intel Intermediate Cert");
            keyStore.deleteEntry("CA cert");
        }
    }

    /**
     * Validates a generated cert chain pretending to be from Intel. Credential was generated
     * with an intermediate CA. This tests the entire chain of validation back to the root CA.
     *
     * @throws IOException if error occurs while reading files
     * @throws KeyStoreException if there's an issue string certs to the keystore
     * @throws CertificateException if error occurs while ingesting a certificate
     * @throws URISyntaxException if a URI can't be processed
     */
    @Test
    public final void validateIntelPlatformCredentials()
            throws URISyntaxException, IOException, CertificateException, KeyStoreException {
        Certificate rootcacert, intermediatecacert;

        intermediatecacert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(INTEL_INT_CA).toURI()))
        );

        rootcacert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(FAKE_ROOT_CA).toURI()))
        );

        try {
            keyStore.setCertificateEntry("CA cert", rootcacert.getX509Certificate());
            keyStore.setCertificateEntry("Intel Intermediate Cert",
                    intermediatecacert.getX509Certificate());

            byte[] certBytes = Files.readAllBytes(Paths.get(CertificateTest.class.
                    getResource(INTEL_PLATFORM_CERT).toURI()));

            PlatformCredential pc = new PlatformCredential(certBytes);

            // The test certificate has expired. Test will accept expired certs.
            AppraisalStatus result = credentialValidator.validatePlatformCredential(
                    pc, keyStore, true);

            assertEquals(result.getAppStatus(), AppraisalStatus.Status.PASS);
            assertEquals(result.getMessage(), SupplyChainCredentialValidator.PLATFORM_VALID);
        } finally {
            keyStore.deleteEntry("Intel Intermediate Cert");
            keyStore.deleteEntry("CA cert");
        }
    }


    /**
     * Checks if the generated Intel Platform Credential can be validated with its attributes.
     *
     * @throws Exception If there are errors.
     */
    @Test
    public final void validateIntelPlatformCredentialAttributes()
            throws Exception {

        byte[] certBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class.
                getResource(INTEL_PLATFORM_CERT)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        DeviceInfoReport deviceInfoReport = buildReport(
                new HardwareInfo(PLATFORM_MANUFACTURER, PLATFORM_MODEL,
                        PLATFORM_VERSION, TEST_SERIAL_NUMBER,
                        DeviceInfoEnums.NOT_SPECIFIED, TEST_SERIAL_NUMBER));

        EndorsementCredential ec = PowerMockito.spy(new EndorsementCredential(
                Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getResource(EK_CERT)).toURI()))));
        PowerMockito.when(ec, "getSerialNumber").thenReturn(pc.getHolderSerialNumber());

        AppraisalStatus result =
                CredentialValidator.validatePlatformCredentialAttributes(pc,
                        deviceInfoReport, ec);
        assertEquals(result.getAppStatus(), AppraisalStatus.Status.PASS);
        assertEquals(result.getMessage(),
                SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID);
    }

//    /**
//     * Checks if the Platform Credential contains the serial number from
//     * the device in the platform serial number field.
//     * @throws Exception If there are errors.
//     *
//     * */
//    @Test
//    public final void validatePlatformCredentialWithDeviceBaseboard()
//            throws Exception {
//        DeviceInfoReport deviceInfoReport = buildReport(new HardwareInfo(
//                PLATFORM_MANUFACTURER, PLATFORM_MODEL,
//                PLATFORM_VERSION, DeviceInfoEnums.NOT_SPECIFIED,
//                DeviceInfoEnums.NOT_SPECIFIED, TEST_SERIAL_NUMBER));
//
//        byte[] certBytes = Files.readAllBytes(Paths.get(CertificateTest.class.
//                getResource(TEST_PLATFORM_CRED2).toURI()));
//
//        PlatformCredential pc = new PlatformCredential(certBytes);
//
//        EndorsementCredential ec = PowerMockito.spy(new EndorsementCredential(
//                Files.readAllBytes(Paths.get(getClass().getResource(EK_CERT).toURI()))
//        ));
//
//        PowerMockito.when(ec, "getSerialNumber").thenReturn(pc.getHolderSerialNumber());
//
//        AppraisalStatus result =
//                credentialValidator.validatePlatformCredentialAttributes(pc,
//                        deviceInfoReport, ec);
//        assertEquals(result.getAppStatus(), AppraisalStatus.Status.PASS);
//        assertEquals(result.getMessage(),
//                SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID);
//    }
//
//    /**
//     * Checks if the Platform Credential contains the serial number from
//     * the device in the chassis serial number field.
//     *
//     * In the platform credential spec, how to place a chassis serial number
//     * is poorly defined and there is no guidance on what a correct implementation
//     * looks like. Unfortunately, there is also no generally accepted practice to
//     * substitute for this spec. This test assumes that the Chassis is described
//     * in a component field of the platform cred.
//     * @throws Exception If there are errors.
//     */
//    @Test
//    public final void validatePlatformCredentialWithDeviceChassis()
//            throws Exception {
//
//        DeviceInfoReport deviceInfoReport = buildReport(
//                new HardwareInfo(PLATFORM_MANUFACTURER, PLATFORM_MODEL,
//                        PLATFORM_VERSION, DeviceInfoEnums.NOT_SPECIFIED,
//                        TEST_SERIAL_NUMBER, TEST_SERIAL_NUMBER));
//        deviceInfoReport = PowerMockito.spy(deviceInfoReport);
//
//        URL url = SupplyChainCredentialValidator.class.getResource(
//                SAMPLE_PACCOR_OUTPUT_WITH_EXTRA_COMPONENT_TXT);
//       // String paccorOutputString = IOUtils.toString(url, "UTF_8");
//       // when(deviceInfoReport.getPaccorOutputString()).thenReturn(paccorOutputString);
//
//        byte[] certBytes = Files.readAllBytes(Paths.get(CertificateTest.class.
//                getResource(TEST_PLATFORM_CRED).toURI()));
//
//        PlatformCredential pc = new PlatformCredential(certBytes);
//
//        EndorsementCredential ec = PowerMockito.spy(new EndorsementCredential(
//                Files.readAllBytes(Paths.get(getClass().getResource(EK_CERT).toURI()))
//        ));
//        when(ec, "getSerialNumber").thenReturn(pc.getHolderSerialNumber());
//
//        AppraisalStatus result =
//                credentialValidator.validatePlatformCredentialAttributes(
//                        pc, deviceInfoReport, ec);
//        assertEquals(result.getAppStatus(), AppraisalStatus.Status.PASS);
//        assertEquals(result.getMessage(),
//                SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID);
//    }
//
//
//    /**
//     * Checks if the NUC Platform Credential contains the serial number from
//     * the device as a baseboard component in the serial number field.
//     * @throws Exception If there are errors.
//     */
//    @Test
//    public final void validatePlatformCredentialWithDeviceSystemSerialNumber()
//            throws Exception {
//
//        DeviceInfoReport deviceInfoReport = buildReport(
//                new HardwareInfo(PLATFORM_MANUFACTURER, PLATFORM_MODEL,
//                        PLATFORM_VERSION, TEST_SERIAL_NUMBER,
//                        DeviceInfoEnums.NOT_SPECIFIED, TEST_SERIAL_NUMBER));
//
//        deviceInfoReport = PowerMockito.spy(deviceInfoReport);
//
//        URL url = SupplyChainCredentialValidator.class.getResource(
//                SAMPLE_PACCOR_OUTPUT_WITH_EXTRA_COMPONENT_TXT);
//     //   String paccorOutputString = IOUtils.toString(url, StandardCharsets.UTF_8);
//    //    when(deviceInfoReport.getPaccorOutputString()).thenReturn(paccorOutputString);
//
//        byte[] certBytes = Files.readAllBytes(Paths.get(CertificateTest.class.
//                getResource(TEST_PLATFORM_CRED).toURI()));
//
//        PlatformCredential pc = new PlatformCredential(certBytes);
//
//        EndorsementCredential ec = PowerMockito.spy(new EndorsementCredential(
//                Files.readAllBytes(Paths.get(getClass().getResource(EK_CERT).toURI()))
//        ));
//        PowerMockito.when(ec, "getSerialNumber").thenReturn(pc.getHolderSerialNumber());
//
//        AppraisalStatus result =
//                credentialValidator.validatePlatformCredentialAttributes(
//                        pc, deviceInfoReport, ec);
//        assertEquals(result.getAppStatus(), AppraisalStatus.Status.PASS);
//        assertEquals(result.getMessage(),
//                SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID);
//    }
//
//    /**
//     * Checks if the Platform Credential validator appropriately fails
//     * when there are no serial numbers returned from the device.
//     * @throws Exception If there are errors.
//     */
//    @Test
//    public final void validatePlatformCredentialWithNoDeviceSerialNumbers()
//            throws Exception {
//
//        DeviceInfoReport deviceInfoReport = buildReport(
//                new HardwareInfo(DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED));
//
//        byte[] certBytes = Files.readAllBytes(Paths.get(CertificateTest.class.
//                getResource(TEST_PLATFORM_CRED2).toURI()));
//
//        PlatformCredential pc = new PlatformCredential(certBytes);
//
//        EndorsementCredential ec = PowerMockito.spy(new EndorsementCredential(
//                Files.readAllBytes(Paths.get(getClass().getResource(EK_CERT).toURI()))
//        ));
//        PowerMockito.when(ec, "getSerialNumber").thenReturn(pc.getHolderSerialNumber());
//
//        String expectedMessage = "Platform manufacturer did not match\n"
//                + "Platform model did not match\n"
//                + "Platform version did not match\n"
//                + "Platform serial did not match\n"
//                + "There are unmatched components:\n";
//
//        AppraisalStatus result =
//                credentialValidator.validatePlatformCredentialAttributes(
//                        pc, deviceInfoReport, ec);
//        assertEquals(result.getAppStatus(), AppraisalStatus.Status.FAIL);
//        assertEquals(result.getMessage(), expectedMessage);
//    }
//
//
//    /**
//     * Checks if the Platform Credential validator passes
//     * when the device info chassis number matches the platform chassis number.
//     * @throws Exception If there are errors.
//     */
//    @Test
//    public final void validatePlatformCredentialCombinedWithChassisSerialNumbersMatchedChassis()
//            throws Exception {
//
//        DeviceInfoReport deviceInfoReport = buildReport(
//                new HardwareInfo(DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        "G6YK42300CB6", DeviceInfoEnums.NOT_SPECIFIED));
//
//        byte[] certBytes = Files.readAllBytes(Paths.get(CertificateTest.class.
//                getResource(TEST_PLATFORM_CRED_BASE_CHASIS_COMBO).toURI()));
//
//        PlatformCredential pc = new PlatformCredential(certBytes);
//
//        EndorsementCredential ec = PowerMockito.spy(new EndorsementCredential(
//                Files.readAllBytes(Paths.get(getClass().getResource(EK_CERT).toURI()))
//        ));
//        PowerMockito.when(ec, "getSerialNumber").thenReturn(pc.getHolderSerialNumber());
//
//        AppraisalStatus result =
//                credentialValidator.validatePlatformCredentialAttributes(
//                        pc, deviceInfoReport, ec);
//        assertEquals(result.getAppStatus(), AppraisalStatus.Status.PASS);
//        assertEquals(result.getMessage(),
//                SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID);
//    }
//
//    /**
//     * Checks if the Platform Credential validator passes
//     * when the device info chassis number matches the platform baseboard number.
//     * @throws Exception If there are errors.
//     */
//    @Test
//    public final void validatePlatformCredentialCombinedWithChassisSerialNumbersMatchedBaseboard()
//            throws Exception {
//
//        DeviceInfoReport deviceInfoReport = buildReport(
//                new HardwareInfo(DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        "GETY42100160", DeviceInfoEnums.NOT_SPECIFIED));
//
//        byte[] certBytes = Files.readAllBytes(Paths.get(CertificateTest.class.
//                getResource(TEST_PLATFORM_CRED_BASE_CHASIS_COMBO).toURI()));
//
//        PlatformCredential pc = new PlatformCredential(certBytes);
//
//        EndorsementCredential ec = PowerMockito.spy(new EndorsementCredential(
//                Files.readAllBytes(Paths.get(getClass().getResource(EK_CERT).toURI()))
//        ));
//        PowerMockito.when(ec, "getSerialNumber").thenReturn(pc.getHolderSerialNumber());
//
//        AppraisalStatus result =
//                credentialValidator.validatePlatformCredentialAttributes(
//                        pc, deviceInfoReport, ec);
//        assertEquals(result.getAppStatus(), AppraisalStatus.Status.PASS);
//        assertEquals(result.getMessage(),
//                SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID);
//    }
//
//    /**
//     * Checks if the Platform Credential validator passes
//     * when the device info baseboard number matches the platform chassis number.
//     * @throws Exception If there are errors.
//     */
//    @Test
//    public final void
//    validatePlatformCredentialCombinedWithChassisSerialNumbersMatchedDeviceBaseboard()
//            throws Exception {
//
//        DeviceInfoReport deviceInfoReport = buildReport(
//                new HardwareInfo(DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, "G6YK42300CB6"));
//
//        byte[] certBytes = Files.readAllBytes(Paths.get(CertificateTest.class.
//                getResource(TEST_PLATFORM_CRED_BASE_CHASIS_COMBO).toURI()));
//
//        PlatformCredential pc = new PlatformCredential(certBytes);
//
//        EndorsementCredential ec = PowerMockito.spy(new EndorsementCredential(
//                Files.readAllBytes(Paths.get(getClass().getResource(EK_CERT).toURI()))
//        ));
//        PowerMockito.when(ec, "getSerialNumber").thenReturn(pc.getHolderSerialNumber());
//
//        AppraisalStatus result =
//                credentialValidator.validatePlatformCredentialAttributes(
//                        pc, deviceInfoReport, ec);
//        assertEquals(result.getAppStatus(), AppraisalStatus.Status.PASS);
//        assertEquals(result.getMessage(),
//                SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID);
//    }
//
//
//    /**
//     * Checks if the Platform Credential validator passes.
//     * when the device info system number matches the platform chassis number
//     * @throws Exception If there are errors.
//     */
//    @Test
//    public final void
//    validatePlatformCredentialCombinedWithChassisSerialNumbersMatchedDeviceSystem()
//            throws Exception {
//
//        DeviceInfoReport deviceInfoReport = buildReport(
//                new HardwareInfo(DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, "G6YK42300CB6",
//                        DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED));
//
//        byte[] certBytes = Files.readAllBytes(Paths.get(CertificateTest.class.
//                getResource(TEST_PLATFORM_CRED_BASE_CHASIS_COMBO).toURI()));
//
//        PlatformCredential pc = new PlatformCredential(certBytes);
//
//        EndorsementCredential ec = PowerMockito.spy(new EndorsementCredential(
//                Files.readAllBytes(Paths.get(getClass().getResource(EK_CERT).toURI()))
//        ));
//        PowerMockito.when(ec, "getSerialNumber").thenReturn(pc.getHolderSerialNumber());
//
//        AppraisalStatus result =
//                credentialValidator.validatePlatformCredentialAttributes(
//                        pc, deviceInfoReport, ec);
//        assertEquals(result.getAppStatus(), AppraisalStatus.Status.PASS);
//        assertEquals(result.getMessage(),
//                SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID);
//    }
//
//    /**
//     * Checks if the Platform Credential validator appropriately fails
//     * when there are no serial numbers returned from the device.
//     * @throws Exception If there are errors.
//     */
//    @Test
//    public final void validatePlatformCredentialCombinedWithNoDeviceSerialNumbers()
//            throws Exception {
//
//        DeviceInfoReport deviceInfoReport = buildReport(
//                new HardwareInfo(DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED));
//
//        byte[] certBytes = Files.readAllBytes(Paths.get(CertificateTest.class.
//                getResource(TEST_PLATFORM_CRED_BASE_CHASIS_COMBO).toURI()));
//
//        PlatformCredential pc = new PlatformCredential(certBytes);
//
//        EndorsementCredential ec = PowerMockito.spy(new EndorsementCredential(
//                Files.readAllBytes(Paths.get(getClass().getResource(EK_CERT).toURI()))
//        ));
//        PowerMockito.when(ec, "getSerialNumber").thenReturn(pc.getHolderSerialNumber());
//
//        String expectedMessage = "Platform serial did not match device info";
//
//        AppraisalStatus result =
//                credentialValidator.validatePlatformCredentialAttributes(
//                        pc, deviceInfoReport, ec);
//        assertEquals(result.getAppStatus(), AppraisalStatus.Status.FAIL);
//        assertEquals(result.getMessage(), expectedMessage);
//    }
//
//
//    /**
//     * Checks if the Platform Credential validator appropriately fails
//     * when there are no serial numbers matching any of the platform info from the device.
//     * @throws Exception If there are errors.
//     */
//    @Test
//    public final void validatePlatformCredentialCombinedWithNoMatchedDeviceSerialNumbers()
//            throws Exception {
//
//        DeviceInfoReport deviceInfoReport = buildReport(
//                new HardwareInfo(DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, "zzz", "aaaa", "bbb"));
//
//        byte[] certBytes = Files.readAllBytes(Paths.get(CertificateTest.class.
//                getResource(TEST_PLATFORM_CRED_BASE_CHASIS_COMBO).toURI()));
//
//        PlatformCredential pc = new PlatformCredential(certBytes);
//
//        EndorsementCredential ec = PowerMockito.spy(new EndorsementCredential(
//                Files.readAllBytes(Paths.get(getClass().getResource(EK_CERT).toURI()))
//        ));
//        PowerMockito.when(ec, "getSerialNumber").thenReturn(pc.getHolderSerialNumber());
//
//        String expectedMessage = "Platform serial did not match device info";
//
//        AppraisalStatus result =
//                credentialValidator.validatePlatformCredentialAttributes(
//                        pc, deviceInfoReport, ec);
//        assertEquals(credentialValidator.validatePlatformCredentialAttributes(
//                pc, deviceInfoReport, ec).getAppStatus(), AppraisalStatus.Status.FAIL);
//        assertEquals(result.getMessage(), expectedMessage);
//    }
//
//
//
//    /**
//     * Checks if the Intel NUC Platform Credential contains the SHA1 of the
//     * device serial number in the certificate serial number field when the
//     * Platform Credential does not have a board serial number.
//     * @throws Exception If there are errors.
//     */
//    @Test
//    public final void validateIntelNucPlatformCredentialWithDeviceBaseboardSHA1()
//            throws Exception {
//
//        CredentialValidator cv = credentialValidator;
//
//        // Other tests will validate the cert chain
//
//        DeviceInfoReport deviceInfoReport = buildReport(
//                new HardwareInfo(DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, NUC_PLATFORM_CERT_SERIAL_NUMBER));
//
//        byte[] certBytes = Files.readAllBytes(Paths.get(CertificateTest.class.
//                getResource(NUC_PLATFORM_CERT).toURI()));
//
//        PlatformCredential pc = new PlatformCredential(certBytes, false);
//
//        EndorsementCredential ec = PowerMockito.spy(new EndorsementCredential(
//                Files.readAllBytes(Paths.get(getClass().getResource(EK_CERT).toURI()))
//        ));
//        PowerMockito.when(ec, "getSerialNumber").thenReturn(pc.getHolderSerialNumber());
//
//        AppraisalStatus result =
//                cv.validatePlatformCredentialAttributes(pc, deviceInfoReport, ec);
//        assertEquals(result.getAppStatus(), AppraisalStatus.Status.PASS);
//        assertEquals(result.getMessage(),
//                SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID);
//    }
//
//    /**
//     * Checks if the Intel NUC Platform Credential contains the truncated SHA1 of the
//     * device serial number in the certificate serial number field when the
//     * Platform Credential does not have a board serial number.
//     * @throws Exception If there are errors.
//     */
//    @Test
//    public final void validateIntelNucPlatformCredentialWithDeviceBaseboardTruncatedSHA1()
//            throws Exception {
//
//        CredentialValidator cv = credentialValidator;
//
//        // Other tests will validate the cert chain
//
//        DeviceInfoReport deviceInfoReport = buildReport(
//                new HardwareInfo(DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, NUC_PLATFORM_CERT_SERIAL_NUMBER2));
//
//        byte[] certBytes = Files.readAllBytes(Paths.get(CertificateTest.class.
//                getResource(NUC_PLATFORM_CERT2).toURI()));
//
//        PlatformCredential pc = new PlatformCredential(certBytes, false);
//
//        EndorsementCredential ec = PowerMockito.spy(new EndorsementCredential(
//                Files.readAllBytes(Paths.get(getClass().getResource(EK_CERT).toURI()))
//        ));
//        PowerMockito.when(ec, "getSerialNumber").thenReturn(pc.getHolderSerialNumber());
//
//        AppraisalStatus result =
//                cv.validatePlatformCredentialAttributes(pc, deviceInfoReport, ec);
//        assertEquals(result.getAppStatus(), AppraisalStatus.Status.PASS);
//        assertEquals(result.getMessage(),
//                SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID);
//    }
//
//    /**
//     * Checks if the Intel NUC Platform Credential validator will appropriately
//     * fail if the certificate serial number field does not contain a SHA1 hash of
//     * the baseboard serial number on the device.
//     * @throws Exception If there are errors.
//     */
//    @Test
//    public final void validateIntelNucPlatformCredentialWithNoDeviceBaseboardSHA1()
//            throws Exception {
//        CredentialValidator cv = credentialValidator;
//
//        // Other tests will validate the cert chain
//        DeviceInfoReport deviceInfoReport = buildReport(
//                new HardwareInfo(DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
//                        DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED));
//
//        byte[] certBytes = Files.readAllBytes(Paths.get(CertificateTest.class.
//                getResource(TEST_PLATFORM_CRED).toURI()));
//        PlatformCredential pc = new PlatformCredential(certBytes, false);
//
//        EndorsementCredential ec = PowerMockito.spy(new EndorsementCredential(
//                Files.readAllBytes(Paths.get(getClass().getResource(EK_CERT).toURI()))
//        ));
//        PowerMockito.when(ec, "getSerialNumber").thenReturn(pc.getHolderSerialNumber());
//
//        String expectedMessage = "Device Serial Number was null";
//
//        AppraisalStatus result = cv.validatePlatformCredentialAttributes(
//                pc, deviceInfoReport, ec);
//        assertEquals(result.getAppStatus(), AppraisalStatus.Status.FAIL);
//        assertEquals(result.getMessage(), expectedMessage);
//    }

    /**
     * Checks if a cert can be validated against the given public key.
     *
     * @throws IOException if error occurs while reading files
     * @throws InvalidKeySpecException if error occurs while generating the PublicKey
     * @throws NoSuchAlgorithmException if error occurs while getting RSA KeyFactory
     * @throws URISyntaxException if error occurs constructing test cert path
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void validateTestCertificateAgainstPublicKey()
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException,
            URISyntaxException, SupplyChainValidatorException {
        PlatformCredential credential = new PlatformCredential(Paths.get(
                Objects.requireNonNull(this.getClass().getResource(TEST_PLATFORM_CRED)).toURI()
        ));

        InputStream stream = this.getClass().getResourceAsStream(TEST_SIGNING_KEY);
        PEMParser pemParser =
                new PEMParser(new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        SubjectPublicKeyInfo info = (SubjectPublicKeyInfo) pemParser.readObject();
        pemParser.close();
        PublicKey signingKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(info.getEncoded()));

        assertTrue(supplyChainCredentialValidator.signatureMatchesPublicKey(
                credential.getX509AttributeCertificateHolder(), signingKey)
        );
    }

    /**
     * Negative test to check if validation against a public key can fail. Generates a random
     * key pair and attempts to validate it against the Intel cert, which is expected to fail.
     *
     * @throws IOException if error occurs while reading files
     * @throws URISyntaxException if an error occurs while constructing test resource's URI
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void checkTestCertificateAgainstInvalidPublicKey()
            throws IOException, URISyntaxException, SupplyChainValidatorException {
        PlatformCredential credential = new PlatformCredential(
                Paths.get(Objects.requireNonNull(this.getClass().getResource(TEST_PLATFORM_CRED)).toURI())
        );

        PublicKey invalidPublicKey = createKeyPair().getPublic();

        assertFalse(SupplyChainCredentialValidator.signatureMatchesPublicKey(
                credential.getX509AttributeCertificateHolder(), invalidPublicKey)
        );
    }

    /**
     * Creates a self-signed "CA" cert, intermediate cert signed by the "CA", and a sample "client"
     * cert signed by the intermediate cert, and an attribute cert using the "client" cert. Attempts
     * to validate the attribute cert against a Set including the "CA" cert and the intermediate
     * cert. Validation should pass because the entire chain is included in the Set.
     *
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void verifyX509AttributeCertificateAgainstIntermediate()
            throws SupplyChainValidatorException {
        KeyPair caKeyPair = createKeyPair();
        KeyPair intermediateKeyPair = createKeyPair();
        KeyPair targetKeyPair = createKeyPair();
        Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();

        X509Certificate caCert = createSelfSignedCertificate(caKeyPair);
        X509Certificate intermediateCert =
                createCertSignedByAnotherCert(intermediateKeyPair, caKeyPair.getPrivate(), caCert);
        X509Certificate targetCert =
                createCertSignedByAnotherCert(targetKeyPair, intermediateKeyPair.getPrivate(),
                        intermediateCert);
        X509AttributeCertificateHolder attrCert =
                createAttributeCert(targetCert, intermediateCert,
                        intermediateKeyPair.getPrivate());

        trustedCerts.add(caCert);
        trustedCerts.add(intermediateCert);
        boolean assertion = SupplyChainCredentialValidator.validateCertChain(attrCert,
                trustedCerts).isEmpty();

        assertTrue(assertion);

        try {
            keyStore.setCertificateEntry("CA cert", caCert);
            keyStore.setCertificateEntry("Intermediate Cert", intermediateCert);
            assertion = SupplyChainCredentialValidator.verifyCertificate(attrCert,
                    keyStore).isEmpty();
            assertTrue(assertion);
        } catch (Exception e) {
            fail("Unexpected error occurred while verifying certificate", e);
        }
    }

    /**
     * Creates a self-signed "CA" cert, intermediate cert signed by the "CA", and a sample "client"
     * cert signed by the intermediate cert, and an attribute cert based on the "client" cert.
     * Attempts to validate the attribute cert only against the self-signed "CA" cert, which fails
     * as expected.
     *
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void verifyX509AttributeCertificateFailsIfSigningCertNotInList()
            throws SupplyChainValidatorException {
        KeyPair caKeyPair = createKeyPair();
        KeyPair intermediateKeyPair = createKeyPair();
        KeyPair targetKeyPair = createKeyPair();
        Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();

        X509Certificate caCert = createSelfSignedCertificate(caKeyPair);
        X509Certificate intermediateCert =
                createCertSignedByAnotherCert(intermediateKeyPair, caKeyPair.getPrivate(), caCert);
        X509Certificate targetCert =
                createCertSignedByAnotherCert(targetKeyPair, intermediateKeyPair.getPrivate(),
                        intermediateCert);
        X509AttributeCertificateHolder attrCert =
                createAttributeCert(targetCert, intermediateCert,
                        intermediateKeyPair.getPrivate());

        trustedCerts.add(caCert);

        boolean assertion = SupplyChainCredentialValidator.validateCertChain(attrCert,
                trustedCerts).isEmpty();
        assertFalse(assertion);

        try {
            keyStore.setCertificateEntry("CA cert", caCert);

            assertion = SupplyChainCredentialValidator.verifyCertificate(attrCert,
                    keyStore).isEmpty();
            assertFalse(assertion);
        } catch (Exception e) {
            fail("Unexpected error occurred while verifying certificate", e);
        }
    }

    /**
     * Tests that an X509AttributeCertificate signed by a self-signed cert will pass validation.
     *
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void verifyX509AttributeCertificateAgainstCA()
            throws SupplyChainValidatorException {
        KeyPair caKeyPair = createKeyPair();
        KeyPair targetKeyPair = createKeyPair();
        Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();

        X509Certificate caCert = createSelfSignedCertificate(caKeyPair);
        X509Certificate targetCert =
                createCertSignedByAnotherCert(targetKeyPair, caKeyPair.getPrivate(), caCert);
        X509AttributeCertificateHolder attrCert =
                createAttributeCert(targetCert, caCert, caKeyPair.getPrivate());

        trustedCerts.add(caCert);

        boolean assertion = SupplyChainCredentialValidator.validateCertChain(
                attrCert, trustedCerts).isEmpty();
        assertTrue(assertion);

        try {
            keyStore.setCertificateEntry("CA cert", caCert);

            assertion = SupplyChainCredentialValidator.verifyCertificate(
                    attrCert, keyStore).isEmpty();
            assertTrue(assertion);
        } catch (Exception e) {
            fail("Unexpected error occurred while verifying certificate", e);
        }
    }

    /**
     * Creates a self-signed "CA" cert, intermediate cert signed by the "CA", and a sample "client"
     * cert signed by the intermediate cert. Attempts to validate the cert only against a Set
     * including the "CA" cert and the intermediate cert. Validation should pass because the entire
     * chain is included in the Set.
     *
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void verifyX509CertificateAgainstIntermediate()
            throws SupplyChainValidatorException {
        KeyPair caKeyPair = createKeyPair();
        KeyPair intermediateKeyPair = createKeyPair();
        KeyPair targetKeyPair = createKeyPair();
        Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();

        X509Certificate caCert = createSelfSignedCertificate(caKeyPair);
        X509Certificate intermediateCert =
                createCertSignedByAnotherCert(intermediateKeyPair, caKeyPair.getPrivate(), caCert);
        X509Certificate targetCert =
                createCertSignedByAnotherCert(targetKeyPair, intermediateKeyPair.getPrivate(),
                        intermediateCert);

        trustedCerts.add(caCert);
        trustedCerts.add(intermediateCert);

        boolean assertion = SupplyChainCredentialValidator.validateCertChain(targetCert,
                trustedCerts).isEmpty();
        assertTrue(assertion);

        try {
            keyStore.setCertificateEntry("CA cert", caCert);
            keyStore.setCertificateEntry("Intermediate Cert", intermediateCert);

            assertTrue(SupplyChainCredentialValidator.verifyCertificate(targetCert,
                    keyStore));
        } catch (Exception e) {
            fail("Unexpected error occurred while verifying certificate", e);
        }
    }

    /**
     * Creates a self-signed "CA" cert, intermediate cert signed by the "CA", and a sample client
     * cert signed by the intermediate cert. Attempts to validate the cert only against the
     * self-signed cert, which fails as expected.
     *
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void verifyX509CertificateFailsIfSigningCertNotInList()
            throws SupplyChainValidatorException {
        KeyPair caKeyPair = createKeyPair();
        KeyPair intermediateKeyPair = createKeyPair();
        KeyPair targetKeyPair = createKeyPair();
        Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();

        X509Certificate caCert = createSelfSignedCertificate(caKeyPair);
        X509Certificate intermediateCert =
                createCertSignedByAnotherCert(intermediateKeyPair, caKeyPair.getPrivate(), caCert);
        X509Certificate targetCert =
                createCertSignedByAnotherCert(targetKeyPair, intermediateKeyPair.getPrivate(),
                        intermediateCert);

        trustedCerts.add(caCert);

        boolean assertion = SupplyChainCredentialValidator.validateCertChain(targetCert,
                trustedCerts).isEmpty();
        assertFalse(assertion);

        try {
            keyStore.setCertificateEntry("CA cert", caCert);

            assertFalse(SupplyChainCredentialValidator.verifyCertificate(targetCert,
                    keyStore));
        } catch (Exception e) {
            fail("Unexpected error occurred while verifying certificate", e);
        }
    }

    /**
     * Tests that an X509Certificate signed by a self-signed cert will pass validation.
     *
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void verifyX509CertificateAgainstCA() throws SupplyChainValidatorException {
        KeyPair caKeyPair = createKeyPair();
        KeyPair targetKeyPair = createKeyPair();
        Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();

        X509Certificate caCert = createSelfSignedCertificate(caKeyPair);
        X509Certificate targetCert =
                createCertSignedByAnotherCert(targetKeyPair, caKeyPair.getPrivate(), caCert);

        trustedCerts.add(caCert);

        boolean assertion = SupplyChainCredentialValidator.validateCertChain(targetCert,
                trustedCerts).isEmpty();
        assertTrue(assertion);

        try {
            keyStore.setCertificateEntry("CA cert", caCert);

            assertTrue(SupplyChainCredentialValidator.verifyCertificate(targetCert,
                    keyStore));
        } catch (Exception e) {
            fail("Unexpected error occurred while verifying certificate", e);
        }
    }

    /**
     * Tests that issuer/subject distinguished names can be properly verified as equal even
     * if their elements are in different orders.
     * @throws URISyntaxException failed to read certificate
     * @throws IOException failed to read certificate
     * @throws KeyStoreException failed to read key store
     * @throws SupplyChainValidatorException missing credential
     */

    @Test
    public final void testPlatformDnEquals() throws URISyntaxException, IOException,
            KeyStoreException, SupplyChainValidatorException {
        Certificate signingCert;
        signingCert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getResource(INTEL_SIGNING_KEY)).toURI()))
        );

        byte[] certBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class.
                getResource(NEW_NUC1)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        X509AttributeCertificateHolder attributeCert = pc.getX509AttributeCertificateHolder();

        X509Certificate caX509 = signingCert.getX509Certificate();

        assertTrue(SupplyChainCredentialValidator.issuerMatchesSubjectDN(
                attributeCert, caX509));
    }

    /**
     * Tests that issuer/subject distinguished names can be properly verified as being unequal
     * if their elements don't match.
     * @throws URISyntaxException failed to read certificate
     * @throws IOException failed to read certificate
     * @throws KeyStoreException failed to read key store
     * @throws SupplyChainValidatorException missing credential
     */
    @Test
    public final void testPlatformDnNotEquals() throws URISyntaxException, IOException,
            KeyStoreException, SupplyChainValidatorException {
        Certificate signingCert;
        signingCert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getResource(INTEL_INT_CA)).toURI()))
        );

        byte[] certBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class.
                getResource(NEW_NUC1)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        X509AttributeCertificateHolder attributeCert = pc.getX509AttributeCertificateHolder();

        X509Certificate caX509 = signingCert.getX509Certificate();

        assertFalse(SupplyChainCredentialValidator.issuerMatchesSubjectDN(
                attributeCert, caX509));
    }

    /**
     * Tests that issuer/subject distinguished names can be properly verified as equal.
     * @throws URISyntaxException failed to read certificate
     * @throws IOException failed to read certificate
     * @throws KeyStoreException failed to read key store
     * @throws SupplyChainValidatorException missing credential
     */
    @Test
    public final void testEndorsementDnEquals() throws URISyntaxException, IOException,
            KeyStoreException, SupplyChainValidatorException {
        Certificate signingCert;
        signingCert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getResource(INT_CA_CERT02)).toURI()))
        );

        byte[] certBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class.
                getResource(EK_CERT)).toURI()));

        EndorsementCredential ec = new EndorsementCredential(certBytes);

        X509Certificate x509Cert = ec.getX509Certificate();

        X509Certificate caX509 = signingCert.getX509Certificate();

        assertTrue(SupplyChainCredentialValidator.issuerMatchesSubjectDN(
                x509Cert, caX509));
    }

    /**
     * Tests that issuer/subject distinguished names can be properly verified as being unequal
     * if their elements don't match.
     * @throws URISyntaxException failed to read certificate
     * @throws IOException failed to read certificate
     * @throws KeyStoreException failed to read key store
     * @throws SupplyChainValidatorException missing credential
     */
    @Test
    public final void testEndorsementDnNotEquals() throws URISyntaxException, IOException,
            KeyStoreException, SupplyChainValidatorException {
        Certificate signingCert;
        signingCert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getResource(INTEL_INT_CA)).toURI()))
        );

        byte[] certBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class.
                getResource(EK_CERT)).toURI()));

        EndorsementCredential ec = new EndorsementCredential(certBytes);

        X509Certificate x509Cert = ec.getX509Certificate();

        X509Certificate caX509 = signingCert.getX509Certificate();

        assertFalse(SupplyChainCredentialValidator.issuerMatchesSubjectDN(
                x509Cert, caX509));
    }

    /**
     * Create a new X.509 attribute certificate given the holder cert, the signing cert, and the
     * signing key.
     *
     * @param targetCert
     *            X509Certificate that will be the holder of the attribute cert
     * @param signingCert
     *            X509Certificate used to sign the new attribute cert
     * @param caPrivateKey
     *            PrivateKey used to sign the new attribute cert
     * @return new X509AttributeCertificate
     */
    private static X509AttributeCertificateHolder createAttributeCert(
            final X509Certificate targetCert, final X509Certificate signingCert,
            final PrivateKey caPrivateKey) {
        X509AttributeCertificateHolder cert = null;
        try {
            final int timeRange = 50000;
            AttributeCertificateHolder holder =
                    new AttributeCertificateHolder(new X509CertificateHolder(
                            targetCert.getEncoded()));
            AttributeCertificateIssuer issuer =
                    new AttributeCertificateIssuer(new X500Name(signingCert
                            .getSubjectX500Principal().getName()));
            BigInteger serialNumber = BigInteger.ONE;
            Date notBefore = new Date(System.currentTimeMillis() - timeRange);
            Date notAfter = new Date(System.currentTimeMillis() + timeRange);
            X509v2AttributeCertificateBuilder builder =
                    new X509v2AttributeCertificateBuilder(holder, issuer, serialNumber, notBefore,
                            notAfter);

            ContentSigner signer =
                    new JcaContentSignerBuilder("SHA1WithRSA").setProvider("BC")
                            .build(caPrivateKey);

            cert = builder.build(signer);
        } catch (CertificateEncodingException | IOException | OperatorCreationException e) {
            fail("Exception occurred while creating a cert", e);
        }

        return cert;

    }

    /**
     * Creates a new RSA 1024-bit KeyPair using a Bouncy Castle Provider.
     *
     * @return new KeyPair
     */
    private static KeyPair createKeyPair() {
        final int keySize = 1024;
        KeyPairGenerator gen;
        KeyPair keyPair = null;
        try {
            gen = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
            gen.initialize(keySize, SECURE_RANDOM);
            keyPair = gen.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            fail("Error occurred while generating key pair", e);
        }
        return keyPair;
    }

    /**
     * Create a new X.509 public-key certificate signed by the given certificate.
     *
     * @param keyPair
     *            KeyPair to create the cert for
     * @param signingKey
     *            PrivateKey of the signing cert
     * @param signingCert
     *            signing cert
     * @return new X509Certificate
     */
    private static X509Certificate createCertSignedByAnotherCert(final KeyPair keyPair,
                                                                 final PrivateKey signingKey, final X509Certificate signingCert) {
        final int timeRange = 10000;
        X509Certificate cert = null;
        try {

            X500Name issuerName = new X500Name(signingCert.getSubjectX500Principal().getName());
            X500Name subjectName = new X500Name("CN=Test V3 Certificate");
            BigInteger serialNumber = BigInteger.ONE;
            Date notBefore = new Date(System.currentTimeMillis() - timeRange);
            Date notAfter = new Date(System.currentTimeMillis() + timeRange);
            X509v3CertificateBuilder builder =
                    new JcaX509v3CertificateBuilder(issuerName, serialNumber, notBefore, notAfter,
                            subjectName, keyPair.getPublic());
            ContentSigner signer =
                    new JcaContentSignerBuilder("SHA1WithRSA").setProvider("BC").build(signingKey);
            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(
                    builder.build(signer));
        } catch (Exception e) {
            fail("Exception occurred while creating a cert", e);
        }
        return cert;
    }

    /**
     * Creates a self-signed X.509 public-key certificate.
     *
     * @param pair
     *            KeyPair to create the cert for
     * @return self-signed X509Certificate
     */
    private static X509Certificate createSelfSignedCertificate(final KeyPair pair) {
        Security.addProvider(new BouncyCastleProvider());
        final int timeRange = 10000;
        X509Certificate cert = null;
        try {

            X500Name issuerName = new X500Name("CN=Test Self-Signed V3 Certificate");
            X500Name subjectName = new X500Name("CN=Test Self-Signed V3 Certificate");
            BigInteger serialNumber = BigInteger.ONE;
            Date notBefore = new Date(System.currentTimeMillis() - timeRange);
            Date notAfter = new Date(System.currentTimeMillis() + timeRange);
            X509v3CertificateBuilder builder =
                    new JcaX509v3CertificateBuilder(issuerName, serialNumber, notBefore, notAfter,
                            subjectName, pair.getPublic());
            ContentSigner signer =
                    new JcaContentSignerBuilder("SHA1WithRSA").setProvider("BC").build(
                            pair.getPrivate());
            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(
                    builder.build(signer));
        } catch (Exception e) {
            fail("Exception occurred while creating a cert", e);
        }
        return cert;
    }

    private DeviceInfoReport buildReport(final HardwareInfo hardwareInfo) {
        final InetAddress ipAddress = getTestIpAddress();
        final byte[] macAddress = new byte[] {11, 22, 33, 44, 55, 66};

        OSInfo osInfo = new OSInfo();
        NetworkInfo networkInfo = new NetworkInfo("test", ipAddress, macAddress);
        FirmwareInfo firmwareInfo = new FirmwareInfo();
        TPMInfo tpmInfo = new TPMInfo();

        return new DeviceInfoReport(networkInfo, osInfo,
                firmwareInfo, hardwareInfo, tpmInfo);
    }
    private static InetAddress getTestIpAddress() {
        try {
            return InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
