package hirs.attestationca.persist.validation;

import hirs.attestationca.persist.entity.ArchivableEntity;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentClass;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.AttributeStatus;
import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import hirs.attestationca.persist.entity.userdefined.info.HardwareInfo;
import hirs.attestationca.persist.entity.userdefined.info.OSInfo;
import hirs.attestationca.persist.entity.userdefined.info.NetworkInfo;
import hirs.attestationca.persist.entity.userdefined.info.FirmwareInfo;
import hirs.attestationca.persist.entity.userdefined.info.TPMInfo;
import hirs.attestationca.persist.entity.userdefined.info.component.NICComponentInfo;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.utils.enums.DeviceInfoEnums;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.DERUTF8String;
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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * Tests the SupplyChainCredentialValidator and CredentialValidator class.
 * Migration note: Tests specifically for test Intel Nuc Platform Credentials
 * have been omitted, as there is no existing matching test Endorsement Credential
 * in the project resources.
 */
public class SupplyChainCredentialValidatorTest {

    private static final String JSON_FILE = "/config/component-class.json";
    private static final String SAMPLE_PACCOR_OUTPUT_TXT = "/hirs/validation/sample_paccor_output.txt";
    private static final String SAMPLE_PACCOR_OUTPUT_NOT_SPECIFIED_TXT
            = "/hirs/validation/sample_paccor_output_not_specified_values.txt";
    private static final String SAMPLE_TEST_PACCOR_CERT
            = "/validation/platform_credentials_2/paccor_platform_cert.crt";

    private static final String SAMPLE_PACCOR_OUTPUT_WITH_EXTRA_COMPONENT_TXT
            = "/hirs/validation/sample_paccor_output_with_extra_component.txt";
    private static HardwareInfo hardwareInfo;
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
            "/validation/platform_credentials/pciids_plat_cert_2-0.pem";

    private static final String TEST_PLATFORM_CRED_BASE_CHASIS_COMBO =
            "/validation/platform_credentials/Intel_pc5.pem";

    private static final String TEST_BOARD_SERIAL_NUMBER = "GETY421001GV";
    private static final String TEST_CHASSIS_SERIAL_NUMBER = "G6YK42300C87";
    private static final String TEST_EK_CERT = "/certificates/nuc-2/tpmcert.pem";
    private static final String TEST_EK_CERT_2 = "/certificates/nuc-1/tpmcert.pem";
    private static final String TEST_COMPONENT_MANUFACTURER = "Intel";
    private static final String TEST_COMPONENT_MODEL = "platform2018";
    private static final String TEST_COMPONENT_REVISION = "1.0";
    private static final String BAD_SERIAL = "BAD_SERIAL";

    //-------Actual ST Micro Endorsement Credential Certificate Chain!--------------
    private static final String EK_CERT = "";
    private static final String INT_CA_CERT02 = "/certificates/fakestmtpmekint02.pem";

    //-------Generated Intel Credential Certificate Chain--------------
    private static final String INTEL_PLATFORM_CERT =
            "/validation/platform_credentials/plat_cert3.pem";
    private static final String INTEL_PLATFORM_CERT_2 =
            "/validation/platform_credentials/Intel_pc2.pem";

    private static final String INTEL_PLATFORM_CERT_3 =
            "/validation/platform_credentials/pciids_plat_cert_2-0.pem";

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

        EndorsementCredential ekcert = new EndorsementCredential(Files.readAllBytes(
                Paths.get(Objects.requireNonNull(getClass().getResource(TEST_EK_CERT)).toURI()))
        );

        Certificate intermediateca02cert = new CertificateAuthorityCredential(Files.readAllBytes(Paths.get(
                Objects.requireNonNull(getClass().getResource(INT_CA_CERT02)).toURI()))
        );

        Certificate rootcacert = new CertificateAuthorityCredential(Files.readAllBytes(Paths.get(
                Objects.requireNonNull(getClass().getResource(FAKE_ROOT_CA_ORIG)).toURI()))
        );

        try {
            keyStore.setCertificateEntry("CA cert", rootcacert.getX509Certificate());
            keyStore.setCertificateEntry("Intel Intermediate Cert",
                    intermediateca02cert.getX509Certificate());

            AppraisalStatus result = CredentialValidator.validateEndorsementCredential(
                    ekcert, keyStore, true);
            assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
            assertEquals(SupplyChainCredentialValidator.ENDORSEMENT_VALID, result.getMessage());
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

        Certificate intermediatecacert =
                new CertificateAuthorityCredential(Files.readAllBytes(Paths.get(
                Objects.requireNonNull(getClass().getResource(INTEL_INT_CA)).toURI()))
        );

        Certificate rootcacert =
                new CertificateAuthorityCredential(Files.readAllBytes(Paths.get(
                Objects.requireNonNull(getClass().getResource(FAKE_ROOT_CA)).toURI()))
        );

        try {
            keyStore.setCertificateEntry("CA cert", rootcacert.getX509Certificate());
            keyStore.setCertificateEntry("Intel Intermediate Cert",
                    intermediatecacert.getX509Certificate());

            byte[] certBytes = Files.readAllBytes(Paths.get(
                    Objects.requireNonNull(SupplyChainCredentialValidatorTest.class.getResource(
                            INTEL_PLATFORM_CERT)).toURI()));

            PlatformCredential pc = new PlatformCredential(certBytes);

            // The test certificate has expired. Test will accept expired certs.
            AppraisalStatus result = CredentialValidator.validatePlatformCredential(
                    pc, keyStore, true);

            assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
            assertEquals(SupplyChainCredentialValidator.PLATFORM_VALID, result.getMessage());
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
//    @Test
    public final void validateIntelPlatformCredentialAttributes()
            throws Exception {

        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidatorTest.class.getResource(
                        INTEL_PLATFORM_CERT_2)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        DeviceInfoReport deviceInfoReport = buildReport(
                new HardwareInfo(PLATFORM_MANUFACTURER, PLATFORM_MODEL,
                        PLATFORM_VERSION, TEST_BOARD_SERIAL_NUMBER,
                        TEST_CHASSIS_SERIAL_NUMBER, TEST_BOARD_SERIAL_NUMBER));

        EndorsementCredential ec = new EndorsementCredential(Files.readAllBytes(Paths.get(
                Objects.requireNonNull(getClass().getResource(TEST_EK_CERT)).toURI())));

        AppraisalStatus result =
                CredentialValidator.validatePlatformCredentialAttributes(pc,
                        deviceInfoReport, ec, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
    }

    /**
     * Checks if the Platform Credential contains the serial number from
     * the device in the platform serial number field.
     * @throws Exception If there are errors.
     *
     * */
//    @Test
    public final void validatePlatformCredentialWithDeviceBaseboard()
            throws Exception {
        DeviceInfoReport deviceInfoReport = buildReport(new HardwareInfo(
                DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED, TEST_BOARD_SERIAL_NUMBER));

        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidatorTest.class.getResource(
                        INTEL_PLATFORM_CERT_2)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        EndorsementCredential ec = new EndorsementCredential(Files.readAllBytes(Paths.get(
                Objects.requireNonNull(getClass().getResource(TEST_EK_CERT)).toURI())));

        AppraisalStatus result =
                CredentialValidator.validatePlatformCredentialAttributes(pc,
                        deviceInfoReport, ec, null, null,
                Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
    }

    /**
     * Checks if the Platform Credential contains the serial number from
     * the device in the chassis serial number field.
     */
//    @Test
    public final void validatePlatformCredentialWithDeviceChassis()
            throws Exception {

        DeviceInfoReport deviceInfoReport = buildReport(new HardwareInfo(
                DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
                TEST_CHASSIS_SERIAL_NUMBER, DeviceInfoEnums.NOT_SPECIFIED));

        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidatorTest.class.getResource(
                        INTEL_PLATFORM_CERT_2)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        EndorsementCredential ec = new EndorsementCredential(Files.readAllBytes(Paths.get(
                Objects.requireNonNull(getClass().getResource(TEST_EK_CERT)).toURI())));

        AppraisalStatus result =
                CredentialValidator.validatePlatformCredentialAttributes(pc,
                        deviceInfoReport, ec, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
    }


    /**
     * Checks if the NUC Platform Credential contains the serial number from
     * the device as a baseboard component in the serial number field.
     * @throws Exception If there are errors.
     */
//    @Test
    public final void validatePlatformCredentialWithDeviceSystemSerialNumber()
            throws Exception {

        DeviceInfoReport deviceInfoReport = buildReport(new HardwareInfo(
                DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED, TEST_BOARD_SERIAL_NUMBER,
                DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED));

        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidatorTest.class.getResource(
                        INTEL_PLATFORM_CERT_2)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        EndorsementCredential ec = new EndorsementCredential(Files.readAllBytes(Paths.get(
                Objects.requireNonNull(getClass().getResource(TEST_EK_CERT)).toURI())));

        AppraisalStatus result =
                CredentialValidator.validatePlatformCredentialAttributes(pc,
                        deviceInfoReport, ec, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
    }

    /**
     * Checks if validation occurs when the Platform Credential baseboard
     * serial number is in the device chassis serial number field.
     */
//    @Test
    public final void validatePlatformCredentialCombinedWithChassisSerialNumbersMatchedBaseboard()
            throws Exception {

        DeviceInfoReport deviceInfoReport = buildReport(new HardwareInfo(
                DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
                TEST_BOARD_SERIAL_NUMBER, DeviceInfoEnums.NOT_SPECIFIED));

        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidatorTest.class.
                getResource(INTEL_PLATFORM_CERT_2)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        EndorsementCredential ec = new EndorsementCredential(
                Files.readAllBytes(Paths.get(
                        Objects.requireNonNull(getClass().getResource(TEST_EK_CERT)).toURI())));

        AppraisalStatus result =
                CredentialValidator.validatePlatformCredentialAttributes(pc,
                        deviceInfoReport, ec, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
    }

    /**
     * Checks if validation occurs when the Platform Credential chassis
     * serial number is in the device baseboard serial number field.
     */
//    @Test
    public final void validatePlatformCredentialCombinedWithBaseboardSerialNumbersMatchedChassis()
            throws Exception {

        DeviceInfoReport deviceInfoReport = buildReport(new HardwareInfo(
                DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED, TEST_CHASSIS_SERIAL_NUMBER));

        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidatorTest.class.
                        getResource(INTEL_PLATFORM_CERT_2)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        EndorsementCredential ec = new EndorsementCredential(Files.readAllBytes(Paths.get(
                Objects.requireNonNull(getClass().getResource(TEST_EK_CERT)).toURI())));

        AppraisalStatus result =
                CredentialValidator.validatePlatformCredentialAttributes(pc,
                        deviceInfoReport, ec, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
    }

    /**
     * Checks if validation occurs when the Platform Credential chassis
     * serial number is in the device system serial number field.
     */
//    @Test
    public final void validatePlatformCredentialCombinedWithSystemSerialNumbersMatchedChassis()
            throws Exception {

        DeviceInfoReport deviceInfoReport = buildReport(new HardwareInfo(
                DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED, TEST_CHASSIS_SERIAL_NUMBER,
                DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED));

        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidatorTest.class.
                        getResource(INTEL_PLATFORM_CERT_2)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        EndorsementCredential ec = new EndorsementCredential(Files.readAllBytes(Paths.get(
                        Objects.requireNonNull(getClass().getResource(TEST_EK_CERT)).toURI())));

        AppraisalStatus result =
                CredentialValidator.validatePlatformCredentialAttributes(pc,
                        deviceInfoReport, ec, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
    }

    /**
     * Checks if the Platform Credential validator appropriately fails
     * when there are no serial numbers returned from the device.
     * @throws Exception If there are errors.
     */
//    @Test
    public final void validatePlatformCredentialWithNoDeviceSerialNumbers()
            throws Exception {

        DeviceInfoReport deviceInfoReport = buildReport(
                new HardwareInfo(PLATFORM_MANUFACTURER, PLATFORM_MODEL,
                        PLATFORM_VERSION, DeviceInfoEnums.NOT_SPECIFIED,
                        DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED));

        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidatorTest.class.
                        getResource(INTEL_PLATFORM_CERT_2)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        EndorsementCredential ec = new EndorsementCredential(
                Files.readAllBytes(Paths.get(
                        Objects.requireNonNull(getClass().getResource(TEST_EK_CERT)).toURI())));

        String expectedMessage = "Platform serial did not match device info";

        AppraisalStatus result =
                CredentialValidator.validatePlatformCredentialAttributes(
                        pc, deviceInfoReport, ec, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals(expectedMessage, result.getMessage());
    }

    /**
     * Checks if the Platform Credential validator appropriately fails
     * when there are no serial numbers matching any of the platform info from the device.
     * @throws Exception If there are errors.
     */
//    @Test
    public final void validatePlatformCredentialCombinedWithNoMatchedDeviceSerialNumbers()
            throws Exception {

        DeviceInfoReport deviceInfoReport = buildReport(
                new HardwareInfo(DeviceInfoEnums.NOT_SPECIFIED, DeviceInfoEnums.NOT_SPECIFIED,
                        DeviceInfoEnums.NOT_SPECIFIED, "zzz", "aaa", "bbb"));

        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidatorTest.class.
                        getResource(INTEL_PLATFORM_CERT_2)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        EndorsementCredential ec = new EndorsementCredential(
                Files.readAllBytes(Paths.get(
                        Objects.requireNonNull(getClass().getResource(TEST_EK_CERT)).toURI())));

        String expectedMessage = "Platform serial did not match device info";

        AppraisalStatus result =
                CredentialValidator.validatePlatformCredentialAttributes(
                        pc, deviceInfoReport, ec, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals(expectedMessage, result.getMessage());
    }

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
        assert stream != null;
        PEMParser pemParser =
                new PEMParser(new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        SubjectPublicKeyInfo info = (SubjectPublicKeyInfo) pemParser.readObject();
        pemParser.close();
        PublicKey signingKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(info.getEncoded()));

        assertTrue(SupplyChainCredentialValidator.signatureMatchesPublicKey(
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
     * Verifies that when the test device's serial number matches the platform credential's
     * board serial number, and the credential can be validated with the keystore,
     * validation passes. This should result in an error as keystores should never
     * be empty.
     *
     * @throws IOException an error occurs when parsing the certificate
     * @throws URISyntaxException an error occurs parsing the certificate file path
     */
    @Test
    public final void verifyPlatformCredentialWithBadKeyStore()
            throws URISyntaxException, IOException {
        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidatorTest.class.getResource(
                INTEL_PLATFORM_CERT)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        String expectedMessage = "Can't validate platform credential without an "
                + "initialized trust store";

        AppraisalStatus result = CredentialValidator.validatePlatformCredential(
                pc, emptyKeyStore, true);
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals(expectedMessage, result.getMessage());
    }

    /**
     * Verifies that a null check is performed on the platform credential path
     * when validating platform credentials.
     */
    @Test
    public final void verifyPlatformCredentialNullCredentialPath() {
        String expectedMessage = "Can't validate platform credential without "
                + "a platform credential";

        AppraisalStatus result = CredentialValidator.validatePlatformCredential(
                null, keyStore, true);
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals(expectedMessage, result.getMessage());
    }

    /**
     * Verifies that a null check is performed on the keyStore
     * when validating platform credentials.
     *
     * @throws IOException an error occurs when parsing the certificate
     * @throws URISyntaxException an error occurs parsing the certificate file path
     */
    @Test
    public final void verifyPlatformCredentialNullKeyStore()
            throws URISyntaxException, IOException {
        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidatorTest.class.getResource(
                INTEL_PLATFORM_CERT)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        String expectedMessage = "Can't validate platform credential without an "
                + "Issuer Cert in the Trust Store";

        AppraisalStatus result = CredentialValidator.validatePlatformCredential(pc, null,
                true);
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals(expectedMessage, result.getMessage());
    }

    /**
     * Verifies that a null check is performed on the device info report
     * when validating platform credentials.
     *
     * @throws IOException an error occurs when parsing the certificate
     * @throws URISyntaxException an error occurs parsing the certificate file path
     */
//    @Test
    public final void verifyPlatformCredentialNullDeviceInfoReport()
            throws URISyntaxException, IOException {
        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidatorTest.class.getResource(
                INTEL_PLATFORM_CERT_2)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        EndorsementCredential ec = new EndorsementCredential(Files.readAllBytes(Paths.get(
                Objects.requireNonNull(getClass().getResource(TEST_EK_CERT)).toURI())));

        String expectedMessage = "Can't validate platform credential attributes without a "
                + "device info report";

        AppraisalStatus result =
                CredentialValidator.validatePlatformCredentialAttributes(pc, null, ec, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals(expectedMessage, result.getMessage());
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
        signingCert = new CertificateAuthorityCredential(Files.readAllBytes(Paths.get(
                Objects.requireNonNull(getClass().getResource(INTEL_SIGNING_KEY)).toURI()))
        );

        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidator.class.getResource(
                        NEW_NUC1)).toURI()));

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
        signingCert = new CertificateAuthorityCredential(Files.readAllBytes(Paths.get(
                Objects.requireNonNull(getClass().getResource(INTEL_INT_CA)).toURI()))
        );

        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidator.class.
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
        signingCert = new CertificateAuthorityCredential(Files.readAllBytes(Paths.get(
                Objects.requireNonNull(getClass().getResource(INT_CA_CERT02)).toURI()))
        );

        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidator.class.getResource(
                        TEST_EK_CERT)).toURI()));

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
        signingCert = new CertificateAuthorityCredential(Files.readAllBytes(Paths.get(
                Objects.requireNonNull(getClass().getResource(INTEL_INT_CA)).toURI()))
        );

        byte[] certBytes = Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidator.class.
                getResource(TEST_EK_CERT)).toURI()));

        EndorsementCredential ec = new EndorsementCredential(certBytes);

        X509Certificate x509Cert = ec.getX509Certificate();

        X509Certificate caX509 = signingCert.getX509Certificate();

        assertFalse(SupplyChainCredentialValidator.issuerMatchesSubjectDN(
                x509Cert, caX509));
    }

    private static DeviceInfoReport setupDeviceInfoReport() {
        hardwareInfo = new HardwareInfo(
                "ACME",
                "anvil",
                "3.0",
                "1234",
                "567",
                "890");

        DeviceInfoReport deviceInfoReport = mock(DeviceInfoReport.class);
        when(deviceInfoReport.getHardwareInfo()).thenReturn(hardwareInfo);
        return deviceInfoReport;
    }

    private static DeviceInfoReport setupDeviceInfoReportWithComponents() throws IOException {
        return setupDeviceInfoReportWithComponents(SAMPLE_PACCOR_OUTPUT_TXT);
    }

    private static DeviceInfoReport setupDeviceInfoReportWithNotSpecifiedComponents()
            throws IOException {
        return setupDeviceInfoReportWithComponents(SAMPLE_PACCOR_OUTPUT_NOT_SPECIFIED_TXT);
    }

    private static DeviceInfoReport setupDeviceInfoReportWithComponents(
            final String paccorOutputResource) throws IOException {
        DeviceInfoReport deviceInfoReport = setupDeviceInfoReport();
        URL url = SupplyChainCredentialValidator.class.getResource(paccorOutputResource);
        String paccorOutputString = IOUtils.toString(url, StandardCharsets.UTF_8);
        when(deviceInfoReport.getPaccorOutputString()).thenReturn(paccorOutputString);
        return deviceInfoReport;
    }

    /**
     * Tests that isMatch works correctly in comparing component info to component identifier.
     */
    @Test
    public void testMatcher() {
        NICComponentInfo nicComponentInfo = new NICComponentInfo("Intel Corporation",
                "Ethernet Connection I217-V",
                "23:94:17:ba:86:5e",
                "00");

        ComponentIdentifier pcComponentIdentifier = new ComponentIdentifier(
                new DERUTF8String(nicComponentInfo.getComponentManufacturer()),
                new DERUTF8String(nicComponentInfo.getComponentModel()),
                new DERUTF8String(nicComponentInfo.getComponentSerial()),
                new DERUTF8String(nicComponentInfo.getComponentRevision()),
                null,
                ASN1Boolean.TRUE,
                Collections.emptyList()
        );

        assertTrue(
                CertificateAttributeScvValidator.isMatch(pcComponentIdentifier,
                        nicComponentInfo)
        );

        pcComponentIdentifier = new ComponentIdentifier(
                new DERUTF8String(nicComponentInfo.getComponentManufacturer()),
                new DERUTF8String(nicComponentInfo.getComponentModel()),
                new DERUTF8String("ab:cd:ef:fe:dc:ba"),
                new DERUTF8String(nicComponentInfo.getComponentRevision()),
                null,
                ASN1Boolean.TRUE,
                Collections.emptyList()
        );

        assertFalse(
                CertificateAttributeScvValidator.isMatch(pcComponentIdentifier,
                        nicComponentInfo)
        );
    }

    private PlatformCredential setupMatchingPlatformCredential(
            final DeviceInfoReport deviceInfoReport) throws IOException {
        PlatformCredential platformCredential = mock(PlatformCredential.class);

        when(platformCredential.getCredentialType()).thenReturn(
                PlatformCredential.CERTIFICATE_TYPE_2_0);
        when(platformCredential.getManufacturer())
                .thenReturn(hardwareInfo.getManufacturer());
        when(platformCredential.getModel())
                .thenReturn(hardwareInfo.getProductName());
        when(platformCredential.getPlatformSerial())
                .thenReturn(hardwareInfo.getBaseboardSerialNumber());
        when(platformCredential.getVersion())
                .thenReturn(hardwareInfo.getVersion());

        List<ComponentInfo> deviceInfoComponents
                = SupplyChainCredentialValidator.getComponentInfoFromPaccorOutput(
                        deviceInfoReport.getNetworkInfo().getHostname(),
                deviceInfoReport.getPaccorOutputString());
        List<ComponentIdentifier> componentIdentifierList = new ArrayList<>();
        for (ComponentInfo deviceInfoComponent : deviceInfoComponents) {
            DERUTF8String serial = null;
            DERUTF8String revision = null;
            if (deviceInfoComponent.getComponentSerial() != null) {
                serial = new DERUTF8String(deviceInfoComponent.getComponentSerial());
            }
            if (deviceInfoComponent.getComponentRevision() != null) {
                revision = new DERUTF8String(deviceInfoComponent.getComponentRevision());
            }
            componentIdentifierList.add(new ComponentIdentifier(
                    new DERUTF8String(deviceInfoComponent.getComponentManufacturer()),
                    new DERUTF8String(deviceInfoComponent.getComponentModel()),
                    serial,
                    revision,
                    null,
                    ASN1Boolean.TRUE,
                    Collections.emptyList()
            ));

        }

        when(platformCredential.getComponentIdentifiers()).thenReturn(componentIdentifierList);

        return platformCredential;
    }

    /**
     * Tests that TPM 2.0 Platform Credentials validate correctly against the device info report
     * when there are no components.
     * @throws IOException if unable to set up DeviceInfoReport from resource file
     */
//    @Test
    public final void testValidatePlatformCredentialAttributesV2p0NoComponentsPass()
            throws IOException {
        DeviceInfoReport deviceInfoReport = setupDeviceInfoReport();
        PlatformCredential platformCredential = setupMatchingPlatformCredential(deviceInfoReport);

        AppraisalStatus appraisalStatus = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential, deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS,
                appraisalStatus.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                appraisalStatus.getMessage());
    }

    /**
     * Tests that TPM 2.0 Platform Credentials validate correctly against the device info report
     * when there are components present.
     * @throws IOException if unable to set up DeviceInfoReport from resource file
     */
//    @Test
    public final void testValidatePlatformCredentialAttributesV2p0WithComponentsPass()
            throws IOException {
        DeviceInfoReport deviceInfoReport = setupDeviceInfoReportWithComponents();
        PlatformCredential platformCredential = setupMatchingPlatformCredential(deviceInfoReport);

        AppraisalStatus appraisalStatus = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential, deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, appraisalStatus.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                appraisalStatus.getMessage());
    }

    /**
     * Tests that TPM 2.0 Platform Credentials validate correctly against the device info report
     * when there are components present, and when the PlatformSerial field holds the system's
     * serial number instead of the baseboard serial number.
     * @throws IOException if unable to set up DeviceInfoReport from resource file
     */
//    @Test
    public final void testValPCAttributesV2p0WithComponentsPassPlatformSerialWithSystemSerial()
            throws IOException {
        DeviceInfoReport deviceInfoReport = setupDeviceInfoReportWithComponents();
        PlatformCredential platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        when(platformCredential.getPlatformSerial())
                .thenReturn(hardwareInfo.getSystemSerialNumber());

        AppraisalStatus appraisalStatus = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential, deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, appraisalStatus.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                appraisalStatus.getMessage());
    }

    /**
     * Tests that TPM 2.0 Platform Credentials validate correctly against the device info report
     * when there are components present, and when the PlatformSerial field holds the system's
     * serial number instead of the baseboard serial number.
     * @throws IOException if unable to set up DeviceInfoReport from resource file
     * @throws URISyntaxException failed to read certificate
     */
//    @Test
    public final void testValPCAttributesV2p0WithComponentsPassPlatformSerialWithSystemSerial2()
            throws IOException, URISyntaxException {
        DeviceInfoReport deviceInfoReport = setupDeviceInfoReportWithNotSpecifiedComponents();
        PlatformCredential platformCredential = new PlatformCredential(
                Files.readAllBytes(Paths.get(
                Objects.requireNonNull(SupplyChainCredentialValidator.class.getResource(
                SAMPLE_TEST_PACCOR_CERT)).toURI())));

        AppraisalStatus appraisalStatus = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential, deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, appraisalStatus.getAppStatus());
    }

    /**
     * Tests that the SupplyChainCredentialValidator fails when required fields are null.
     * @throws IOException if unable to set up DeviceInfoReport from resource file
     */
//    @Test
    public final void testValidatePlatformCredentialAttributesV2p0RequiredFieldsNull()
            throws IOException {
        DeviceInfoReport deviceInfoReport = setupDeviceInfoReportWithComponents();

        PlatformCredential platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        AppraisalStatus result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
        when(platformCredential.getManufacturer()).thenReturn(null);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals("Platform manufacturer did not match\n", result.getMessage());

        platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
        when(platformCredential.getModel()).thenReturn(null);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(result.getAppStatus(), AppraisalStatus.Status.FAIL);
        assertEquals(result.getMessage(), "Platform model did not match\n");

        platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
        when(platformCredential.getPlatformSerial()).thenReturn(null);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
        when(platformCredential.getVersion()).thenReturn(null);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());

        platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
        List<ComponentIdentifier> modifiedComponentIdentifiers
                = platformCredential.getComponentIdentifiers();
        modifiedComponentIdentifiers.get(0).setComponentManufacturer(null);
        when(platformCredential.getComponentIdentifiers()).thenReturn(modifiedComponentIdentifiers);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals("Component manufacturer is empty\n", result.getMessage());

        platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
        modifiedComponentIdentifiers = platformCredential.getComponentIdentifiers();
        modifiedComponentIdentifiers.get(0).setComponentModel(null);
        when(platformCredential.getComponentIdentifiers()).thenReturn(modifiedComponentIdentifiers);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals("Component model is empty\n", result.getMessage());

    }

    /**
     * Tests that the SupplyChainCredentialValidator fails when required fields contain only empty
     * strings.
     * @throws IOException if unable to set up DeviceInfoReport from resource file
     */
//    @Test
    public final void testValidatePlatformCredentialAttributesV2p0RequiredFieldsEmpty()
            throws IOException {
        DeviceInfoReport deviceInfoReport = setupDeviceInfoReportWithComponents();

        PlatformCredential platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        AppraisalStatus result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
        when(platformCredential.getManufacturer()).thenReturn("");
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals("Platform manufacturer did not match\n", result.getMessage());

        platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
        when(platformCredential.getModel()).thenReturn("");
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals("Platform model did not match\n", result.getMessage());

        platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
        when(platformCredential.getPlatformSerial()).thenReturn("");
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals("Platform serial did not match\n", result.getMessage());

        platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
        when(platformCredential.getVersion()).thenReturn("");
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals("Platform version did not match\n", result.getMessage());

        platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
        List<ComponentIdentifier> modifiedComponentIdentifiers
                = platformCredential.getComponentIdentifiers();
        modifiedComponentIdentifiers.get(0).setComponentManufacturer(new DERUTF8String(""));
        when(platformCredential.getComponentIdentifiers()).thenReturn(modifiedComponentIdentifiers);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals("Component manufacturer is empty\n"
                + "There are unmatched components:\n"
                + "Manufacturer=, Model=Core i7, Serial=Not Specified,"
                + " Revision=Intel(R) Core(TM) i7-4790 CPU @ 3.60GHz;\n",
                result.getMessage());

        platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
        modifiedComponentIdentifiers = platformCredential.getComponentIdentifiers();
        modifiedComponentIdentifiers.get(0).setComponentModel(new DERUTF8String(""));
        when(platformCredential.getComponentIdentifiers()).thenReturn(modifiedComponentIdentifiers);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals("Component model is empty\n", result.getMessage());
    }

    /**
     * Tests that {@link SupplyChainCredentialValidator} failes when a component exists in the
     * platform credential, but not in the device info report.
     * @throws IOException if unable to set up DeviceInfoReport from resource file
     */
//    @Test
    public final void testValidatePlatformCredentialAttributesV2p0MissingComponentInDeviceInfo()
            throws IOException {
        DeviceInfoReport deviceInfoReport = setupDeviceInfoReportWithComponents();

        PlatformCredential platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        AppraisalStatus result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());

        List<ComponentIdentifier> modifiedComponentIdentifiers
                = platformCredential.getComponentIdentifiers();
        modifiedComponentIdentifiers.add(new ComponentIdentifier(
                new DERUTF8String("ACME"),
                new DERUTF8String("TNT"),
                new DERUTF8String("2"),
                new DERUTF8String("1.1"),
                null,
                ASN1Boolean.FALSE,
                Collections.emptyList()
        ));
        when(platformCredential.getComponentIdentifiers()).thenReturn(
                modifiedComponentIdentifiers
        );
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals("There are unmatched components:\n"
                + "Manufacturer=ACME, Model=TNT, Serial=2, Revision=1.1;\n",
                result.getMessage());
    }

    /**
     * Tests that SupplyChainCredentialValidator passes when everything matches but there are
     * extra components in the device info report that are not represented in the platform
     * credential.
     * @throws IOException if unable to set up DeviceInfoReport from resource file
     */
//    @Test
    public final void testValidatePlatformCredentialAttributesV2p0ExtraComponentInDeviceInfo()
            throws IOException {
        PlatformCredential platformCredential = setupMatchingPlatformCredential(
                setupDeviceInfoReportWithComponents(SAMPLE_PACCOR_OUTPUT_TXT));

        // The device info report will contain one extra component.
        DeviceInfoReport deviceInfoReport = setupDeviceInfoReportWithComponents(
                SAMPLE_PACCOR_OUTPUT_WITH_EXTRA_COMPONENT_TXT);

        AppraisalStatus result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());

        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
    }

    /**
     * Tests that SupplyChainCredentialValidator fails when a component is found in the platform
     * credential without a manufacturer or model.
     * @throws IOException if unable to set up DeviceInfoReport from resource file
     */
//    @Test
    public final void testValidatePlatformCredentialAttributesV2p0RequiredComponentFieldEmpty()
            throws IOException {
        DeviceInfoReport deviceInfoReport = setupDeviceInfoReportWithComponents();

        PlatformCredential platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        AppraisalStatus result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());

        List<ComponentIdentifier> componentIdentifiers
                = platformCredential.getComponentIdentifiers();
        componentIdentifiers.get(0).setComponentManufacturer(new DERUTF8String(""));
        when(platformCredential.getComponentIdentifiers()).thenReturn(componentIdentifiers);

        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals("Component manufacturer is empty\n"
                + "There are unmatched components:\n"
                + "Manufacturer=, Model=Core i7, Serial=Not Specified,"
                + " Revision=Intel(R) Core(TM) i7-4790 CPU @ 3.60GHz;\n",
                result.getMessage());

        platformCredential = setupMatchingPlatformCredential(deviceInfoReport);
        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());

        componentIdentifiers = platformCredential.getComponentIdentifiers();
        componentIdentifiers.get(0).setComponentModel(null);
        when(platformCredential.getComponentIdentifiers()).thenReturn(componentIdentifiers);

        result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
        assertEquals("Component model is empty\n", result.getMessage());
    }

    /**
     * Tests that SupplyChainCredentialValidator passes when a component on the system has a
     * matching component in the platform certificate, except the serial value is missing.
     * @throws IOException if unable to set up DeviceInfoReport from resource file
     */
//    @Test
    public final void testValidatePlatformCredentialAttributesV2p0RequiredComponentNoSerial()
            throws IOException {
        DeviceInfoReport deviceInfoReport = setupDeviceInfoReportWithComponents();
        PlatformCredential platformCredential = setupMatchingPlatformCredential(deviceInfoReport);

        ArrayList<ComponentIdentifier> modifiedIdentifiers = new ArrayList<>();
        for (ComponentIdentifier identifier : platformCredential.getComponentIdentifiers()) {
            if (identifier.getComponentSerial() != null
                    && identifier.getComponentSerial().toString().equals("23:94:17:ba:86:5e")) {
                identifier.setComponentSerial(new DERUTF8String(""));
            }
            modifiedIdentifiers.add(identifier);
        }

        when(platformCredential.getComponentIdentifiers()).thenReturn(modifiedIdentifiers);

        AppraisalStatus result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
    }

    /**
     * Tests that SupplyChainCredentialValidator passes when a component on the system has a
     * matching component in the platform certificate, except the revision value is missing.
     * @throws IOException if unable to set up DeviceInfoReport from resource file
     */
//    @Test
    public final void testValidatePlatformCredentialAttributesV2p0RequiredComponentNoRevision()
            throws IOException {
        DeviceInfoReport deviceInfoReport = setupDeviceInfoReportWithComponents();
        PlatformCredential platformCredential = setupMatchingPlatformCredential(deviceInfoReport);

        ArrayList<ComponentIdentifier> modifiedIdentifiers = new ArrayList<>();
        for (ComponentIdentifier identifier : platformCredential.getComponentIdentifiers()) {
            if (identifier.getComponentRevision() != null
                    && identifier.getComponentRevision().toString().equals("00")) {
                identifier.setComponentSerial(new DERUTF8String(""));
            }
            modifiedIdentifiers.add(identifier);
        }

        when(platformCredential.getComponentIdentifiers()).thenReturn(modifiedIdentifiers);

        AppraisalStatus result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
    }

    /**
     * Tests that SupplyChainCredentialValidator passes when a component on the system has a
     * matching component in the platform certificate, except the serial and revision values
     * are missing.
     * @throws IOException if unable to set up DeviceInfoReport from resource file
     */
//    @Test
    public final void testValPlatCredentialAttributesV2p0RequiredComponentNoSerialOrRevision()
            throws IOException {
        DeviceInfoReport deviceInfoReport = setupDeviceInfoReportWithComponents();
        PlatformCredential platformCredential = setupMatchingPlatformCredential(deviceInfoReport);

        ArrayList<ComponentIdentifier> modifiedIdentifiers = new ArrayList<>();
        for (ComponentIdentifier identifier : platformCredential.getComponentIdentifiers()) {
            if (identifier.getComponentSerial() != null
                    && identifier.getComponentSerial().toString().equals("23:94:17:ba:86:5e")) {
                identifier.setComponentSerial(new DERUTF8String(""));
                identifier.setComponentRevision(new DERUTF8String(""));
            }
            modifiedIdentifiers.add(identifier);
        }

        when(platformCredential.getComponentIdentifiers()).thenReturn(modifiedIdentifiers);

        AppraisalStatus result = CertificateAttributeScvValidator
                .validatePlatformCredentialAttributesV2p0(platformCredential,
                        deviceInfoReport, null, null,
                        Collections.emptyList(), UUID.randomUUID());
        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
                result.getMessage());
    }

    /**
     * Tests that SupplyChainCredentialValidator passes with a base and delta certificate where
     * the base serial number and delta holder serial number match.
     * @throws java.io.IOException Reading file for the certificates
     * @throws java.net.URISyntaxException when loading certificates bytes
     */
//    @Test
    public final void testValidateDeltaPlatformCredentialAttributes()
            throws IOException, URISyntaxException {
//        DeviceInfoReport deviceInfoReport = setupDeviceInfoReportWithComponents(
//                SAMPLE_PACCOR_OUTPUT_TXT);

//        PlatformCredential base = mock(PlatformCredential.class);
//        PlatformCredential delta1 = mock(PlatformCredential.class);
//        PlatformCredential delta2 = mock(PlatformCredential.class);
//
//        ComponentIdentifierV2 compId1 = new ComponentIdentifierV2(
//                new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
//                        .getResource(JSON_FILE)).toURI()), "0x00010002"),
//                new DERUTF8String("Intel"),
//                new DERUTF8String("Core i7"), new DERUTF8String("Not Specified"),
//                new DERUTF8String("Intel(R) Core(TM) i7-4790 CPU @ 3.60GHz"), null,
//                ASN1Boolean.TRUE, new ArrayList<>(0), null, null,
//                null);
//        ComponentIdentifierV2 compId2 = new ComponentIdentifierV2(
//                new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
//                        .getResource(JSON_FILE)).toURI()), "0x00050004"),
//                new DERUTF8String("Intel Corporation"),
//                new DERUTF8String("Ethernet Connection I217-V-faulty"),
//                new DERUTF8String("23:94:17:ba:86:5e"), new DERUTF8String("00"), null,
//                ASN1Boolean.FALSE, new ArrayList<>(0), null, null,
//                null);
//        ComponentIdentifierV2 compId3 = new ComponentIdentifierV2(
//                new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
//                        .getResource(JSON_FILE)).toURI()), "0x00090002"),
//                new DERUTF8String("Intel Corporation"),
//                new DERUTF8String("82580 Gigabit Network Connection-faulty"),
//                new DERUTF8String("90:e2:ba:31:83:10"), new DERUTF8String(""), null,
//                ASN1Boolean.FALSE, new ArrayList<>(0), null, null,
//                null);
//        ComponentIdentifierV2 deltaCompId2 = new ComponentIdentifierV2(
//                new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
//                        .getResource(JSON_FILE)).toURI()), "0x00050004"),
//                new DERUTF8String("Intel Corporation"),
//                new DERUTF8String("Ethernet Connection I217-V"),
//                new DERUTF8String("23:94:17:ba:86:5e"), new DERUTF8String("00"), null,
//                ASN1Boolean.FALSE, new ArrayList<>(0), null, null,
//                AttributeStatus.ADDED);
//        ComponentIdentifierV2 deltaCompId3 = new ComponentIdentifierV2(
//                new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
//                        .getResource(JSON_FILE)).toURI()), "0x00090002"),
//                new DERUTF8String("Intel Corporation"),
//                new DERUTF8String("82580 Gigabit Network Connection"),
//                new DERUTF8String("90:e2:ba:31:83:10"), new DERUTF8String(""), null,
//                ASN1Boolean.FALSE, new ArrayList<>(0), null, null,
//                AttributeStatus.ADDED);
//
//        ComponentIdentifierV2 ciV21Faulty = new ComponentIdentifierV2();
//        ComponentIdentifierV2 ciV22Faulty = new ComponentIdentifierV2();
//        ciV21Faulty.setComponentManufacturer(compId2.getComponentManufacturer());
//        ciV21Faulty.setComponentClass(compId2.getComponentClass());
//        ciV21Faulty.setComponentModel(compId2.getComponentModel());
//        ciV21Faulty.setComponentSerial(compId2.getComponentSerial());
//        ciV21Faulty.setComponentRevision(compId2.getComponentRevision());
//        ciV21Faulty.setComponentManufacturerId(compId2.getComponentManufacturerId());
//        ciV21Faulty.setFieldReplaceable(compId2.getFieldReplaceable());
//        ciV21Faulty.setComponentAddress(compId2.getComponentAddress());
//        ciV21Faulty.setAttributeStatus(AttributeStatus.REMOVED);
//        ciV22Faulty.setComponentManufacturer(compId3.getComponentManufacturer());
//        ciV22Faulty.setComponentClass(compId3.getComponentClass());
//        ciV22Faulty.setComponentModel(compId3.getComponentModel());
//        ciV22Faulty.setComponentSerial(compId3.getComponentSerial());
//        ciV22Faulty.setComponentRevision(compId3.getComponentRevision());
//        ciV22Faulty.setComponentManufacturerId(compId3.getComponentManufacturerId());
//        ciV22Faulty.setFieldReplaceable(compId3.getFieldReplaceable());
//        ciV22Faulty.setComponentAddress(compId3.getComponentAddress());
//        ciV22Faulty.setAttributeStatus(AttributeStatus.REMOVED);
//
//        List<ComponentIdentifier> compList = new ArrayList<>(3);
//        compList.add(compId1);
//        compList.add(compId2);
//        compList.add(compId3);
//
//        List<ComponentIdentifier> delta1List = new ArrayList<>(2);
//        delta1List.add(ciV21Faulty);
//        delta1List.add(deltaCompId2);
//        List<ComponentIdentifier> delta2List = new ArrayList<>(2);
//        delta1List.add(ciV22Faulty);
//        delta1List.add(deltaCompId3);
//
//        when(base.isPlatformBase()).thenReturn(true);
//        when(delta1.isPlatformBase()).thenReturn(false);
//        when(delta2.isPlatformBase()).thenReturn(false);
//        when(base.getManufacturer()).thenReturn("innotek GmbH");
//        when(base.getModel()).thenReturn("VirtualBox");
//        when(base.getVersion()).thenReturn("1.2");
//        when(base.getPlatformSerial()).thenReturn("62UIAE5");
//        when(delta1.getPlatformSerial()).thenReturn("62UIAE5");
//        when(delta2.getPlatformSerial()).thenReturn("62UIAE5");
//        when(base.getPlatformChainType()).thenReturn("base");
//        when(delta1.getPlatformChainType()).thenReturn("delta");
//        when(delta2.getPlatformChainType()).thenReturn("delta");
//        when(base.getSerialNumber()).thenReturn(BigInteger.valueOf(01));
//        when(delta1.getSerialNumber()).thenReturn(BigInteger.valueOf(39821));
//        when(delta2.getSerialNumber()).thenReturn(BigInteger.valueOf(39822));
//        when(delta1.getHolderSerialNumber()).thenReturn(BigInteger.valueOf(02));
//        when(delta2.getHolderSerialNumber()).thenReturn(BigInteger.valueOf(39821));
//        when(base.getComponentIdentifiers()).thenReturn(compList);
//        when(delta1.getComponentIdentifiers()).thenReturn(delta1List);
//        when(delta2.getComponentIdentifiers()).thenReturn(delta2List);
//
//        Map<PlatformCredential, SupplyChainValidation> chainCredentials = new HashMap<>(0);
//        List<ArchivableEntity> certsUsed = new ArrayList<>();
//        certsUsed.add(base);
//        chainCredentials.put(base, new SupplyChainValidation(
//                SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
//                AppraisalStatus.Status.PASS, certsUsed, ""));
//        certsUsed.clear();
//        certsUsed.add(delta1);
//        chainCredentials.put(delta1, new SupplyChainValidation(
//                SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
//                AppraisalStatus.Status.PASS, certsUsed, ""));
//        certsUsed.clear();
//        certsUsed.add(delta2);
//        chainCredentials.put(delta2, new SupplyChainValidation(
//                SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
//                AppraisalStatus.Status.PASS, certsUsed, ""));

//        AppraisalStatus result = CredentialValidator
//                .validateDeltaPlatformCredentialAttributes(delta2,
//                        deviceInfoReport, base, chainCredentials);
//        assertEquals(AppraisalStatus.Status.PASS, result.getAppStatus());
//        assertEquals(SupplyChainCredentialValidator.PLATFORM_ATTRIBUTES_VALID,
//                result.getMessage());
    }

    /**
     * Tests that SupplyChainCredentialValidator fails when a component needs to
     * be replaced but hasn't been by a delta certificate.
     * @throws java.io.IOException Reading file for the certificates
     * @throws java.net.URISyntaxException when loading certificates bytes
     */
//    @Test
    public final void testValidateChainFailure()
            throws IOException, URISyntaxException {
//        DeviceInfoReport deviceInfoReport = setupDeviceInfoReportWithComponents(
//                SAMPLE_PACCOR_OUTPUT_TXT);

//        PlatformCredential base = mock(PlatformCredential.class);
//        PlatformCredential delta1 = mock(PlatformCredential.class);
//
//        ComponentIdentifierV2 compId1 = new ComponentIdentifierV2(
//                new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
//                        .getResource(JSON_FILE)).toURI()), "0x00010002"),
//                new DERUTF8String("Intel"),
//                new DERUTF8String("Core i7"), new DERUTF8String("Not Specified"),
//                new DERUTF8String("Intel(R) Core(TM) i7-4790 CPU @ 3.60GHz"), null,
//                ASN1Boolean.TRUE, new ArrayList<>(0), null, null,
//                null);
//        ComponentIdentifierV2 compId2 = new ComponentIdentifierV2(
//                new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
//                        .getResource(JSON_FILE)).toURI()), "0x00050004"),
//                new DERUTF8String("Intel Corporation"),
//                new DERUTF8String("Ethernet Connection I217-V-faulty"),
//                new DERUTF8String("23:94:17:ba:86:5e"), new DERUTF8String("00"), null,
//                ASN1Boolean.FALSE, new ArrayList<>(0), null, null,
//                null);
//        ComponentIdentifierV2 compId3 = new ComponentIdentifierV2(
//                new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
//                        .getResource(JSON_FILE)).toURI()), "0x00090002"),
//                new DERUTF8String("Intel Corporation"),
//                new DERUTF8String("82580 Gigabit Network Connection-faulty"),
//                new DERUTF8String("90:e2:ba:31:83:10"), new DERUTF8String(""), null,
//                ASN1Boolean.FALSE, new ArrayList<>(0), null, null,
//                null);
//        ComponentIdentifierV2 deltaCompId2 = new ComponentIdentifierV2(
//                new ComponentClass(Paths.get(Objects.requireNonNull(this.getClass()
//                        .getResource(JSON_FILE)).toURI()), "0x00050004"),
//                new DERUTF8String("Intel Corporation"),
//                new DERUTF8String("Ethernet Connection I217-V"),
//                new DERUTF8String("23:94:17:ba:86:5e"), new DERUTF8String("00"), null,
//                ASN1Boolean.FALSE, new ArrayList<>(0), null, null,
//                AttributeStatus.ADDED);
//
//        ComponentIdentifierV2 ciV21Faulty = new ComponentIdentifierV2();
//        ComponentIdentifierV2 ciV22Faulty = new ComponentIdentifierV2();
//        ciV21Faulty.setComponentManufacturer(compId2.getComponentManufacturer());
//        ciV21Faulty.setComponentModel(compId2.getComponentModel());
//        ciV21Faulty.setComponentSerial(compId2.getComponentSerial());
//        ciV21Faulty.setComponentRevision(compId2.getComponentRevision());
//        ciV21Faulty.setComponentManufacturerId(compId2.getComponentManufacturerId());
//        ciV21Faulty.setFieldReplaceable(compId2.getFieldReplaceable());
//        ciV21Faulty.setComponentAddress(compId2.getComponentAddress());
//        ciV21Faulty.setAttributeStatus(AttributeStatus.REMOVED);
//        ciV22Faulty.setComponentManufacturer(compId3.getComponentManufacturer());
//        ciV22Faulty.setComponentModel(compId3.getComponentModel());
//        ciV22Faulty.setComponentSerial(compId3.getComponentSerial());
//        ciV22Faulty.setComponentRevision(compId3.getComponentRevision());
//        ciV22Faulty.setComponentManufacturerId(compId3.getComponentManufacturerId());
//        ciV22Faulty.setFieldReplaceable(compId3.getFieldReplaceable());
//        ciV22Faulty.setComponentAddress(compId3.getComponentAddress());
//        ciV22Faulty.setAttributeStatus(AttributeStatus.REMOVED);
//
//        List<ComponentIdentifier> compList = new ArrayList<>(3);
//        compList.add(compId1);
//        compList.add(compId2);
//        compList.add(compId3);
//
//        List<ComponentIdentifier> delta1List = new ArrayList<>(2);
//        delta1List.add(ciV21Faulty);
//        delta1List.add(deltaCompId2);
//
//        when(base.isPlatformBase()).thenReturn(true);
//        when(delta1.isPlatformBase()).thenReturn(false);
//        when(base.getManufacturer()).thenReturn("innotek GmbH");
//        when(base.getModel()).thenReturn("VirtualBox");
//        when(base.getVersion()).thenReturn("1.2");
//        when(base.getPlatformSerial()).thenReturn("0");
//        when(delta1.getPlatformSerial()).thenReturn("0");
//        when(base.getPlatformChainType()).thenReturn("base");
//        when(delta1.getPlatformChainType()).thenReturn("delta");
//        when(base.getSerialNumber()).thenReturn(BigInteger.ZERO);
//        when(delta1.getSerialNumber()).thenReturn(BigInteger.ONE);
//        when(delta1.getHolderSerialNumber()).thenReturn(BigInteger.ZERO);
//        when(base.getComponentIdentifiers()).thenReturn(compList);
//        when(delta1.getComponentIdentifiers()).thenReturn(delta1List);
//
//        Map<PlatformCredential, SupplyChainValidation> chainCredentials = new HashMap<>(0);
//        List<ArchivableEntity> certsUsed = new ArrayList<>();
//        certsUsed.add(base);
//        chainCredentials.put(base, new SupplyChainValidation(
//                SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
//                AppraisalStatus.Status.PASS, certsUsed, ""));
//        certsUsed.clear();
//        certsUsed.add(delta1);
//        chainCredentials.put(delta1, new SupplyChainValidation(
//                SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
//                AppraisalStatus.Status.PASS, certsUsed, ""));

//        AppraisalStatus result = CredentialValidator
//                .validateDeltaPlatformCredentialAttributes(delta1,
//                        deviceInfoReport, base, chainCredentials);
//        assertEquals(AppraisalStatus.Status.FAIL, result.getAppStatus());
//        assertEquals("There are unmatched components:\n"
//                + "Manufacturer=Intel Corporation, Model=82580 Gigabit Network "
//                + "Connection-faulty, Serial=90:e2:ba:31:83:10, Revision=;\n",
//                result.getMessage());
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

    private DeviceInfoReport buildReport(final HardwareInfo givenHardwareInfo) {
        final InetAddress ipAddress = getTestIpAddress();
        final byte[] macAddress = new byte[] {11, 22, 33, 44, 55, 66};

        OSInfo osInfo = new OSInfo();
        NetworkInfo networkInfo = new NetworkInfo("test", ipAddress, macAddress);
        FirmwareInfo firmwareInfo = new FirmwareInfo();
        TPMInfo tpmInfo = new TPMInfo();

        return new DeviceInfoReport(networkInfo, osInfo,
                firmwareInfo, givenHardwareInfo, tpmInfo);
    }
    private static InetAddress getTestIpAddress() {
        try {
            return InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
