package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.userdefined.AbstractUserdefinedEntityTest;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.PlatformConfigurationV1;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.PlatformProperty;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.TBBSecurityAssertion;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.URIReference;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.PlatformConfigurationV2;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that a PlatformCredential parses its fields correctly.
 */
public class PlatformCredentialTest extends AbstractUserdefinedEntityTest {

    /**
     * Platform Certificate 2.0 with all the expected data.
     */
    static final String TEST_PLATFORM_CERT2_1 =
            "/validation/platform_credentials_2/basic_plat_cert.pem";

    /**
     * Platform Certificate spec 2.
     */
    static final String TEST_PLATFORM_CERT2_SPEC2 =
            "/validation/platform_credentials_2/large_attribute_spec2.txt";

    /**
     * Platform Certificate 2.0 with all the expected data.
     */
    static final String TEST_PLATFORM_CERT2_2 =
            "/validation/platform_credentials_2/small_plat_cert.pem";

    /**
     * Platform Certificate 2.0 with all the expected data.
     */
    static final String TEST_PLATFORM_CERT2_3 =
            "/validation/platform_credentials_2/medium_plat_cert.pem";

    /**
     * words.
     */
    static final String TEST_BASE_PLATFORM_CERT_1 =
            "/validation/platform_credentials/plat_base_cert1.pem";

    /**
     * words.
     */
    static final String TEST_DELTA_PLATFORM_CERT_1 =
            "/validation/platform_credentials/plat_delta_cert1.pem";

    /**
     * Platform Certificate 2.0 with all the expected data.
     */
    static final String TEST_PLATFORM_CERT2_4 =
            "/validation/platform_credentials_2/large_plat_cert.pem";

    private static final String EXPECTED_CERT_SIGNATURE_FOR_CERTS_1 =
            "425F6B2203EC6C651F3DA38416A39DB9B4D954A45FB1D1396D079ABE7"
                    + "29E6299297CFB57A971559BB29E13E1AABBF5E99C11968FED7B53CE3F"
                    + "4C71A889E893168A90C05F0F0D936B8D7E87531C616749DB647684DD2"
                    + "E430B6FB3B62F286407E99B7EC2D20860528C4E4DB3C7617BC321DF1E"
                    + "0E5F8DF601CB257BDE941E43CB0A8ED2B9EF1E95872C3FAE5A7195E16"
                    + "9D14C05BD6051BA1AEED482E5322CA58D09CD9979EF8C166198C83BA7"
                    + "243A2C79B9346B92ABB8C14E3AE950D5EB2E23CBEF1F124981949A413"
                    + "7EBE52DB0F4C1E8DD515E9FF0A22CE852FA85C7648D160F39F391E868"
                    + "74660B7FAA9ED150A36F0210B28AB6F840FCC61D81CD4F6FFF11B2A8";

    private static final String EXPECTED_CERT_SIGNATURE_FOR_CERT_2 =
            "67ABFBB91E0F061CA8CCE5DAA45104978D1020DE11DA65FD7DFD0E7C5"
                    + "1B84218B033C32D82ACA0C14A48C39EE1603A5939F84711B1"
                    + "95092ACB33FBA35B198019002C2326894ED0F7D17FA90450E"
                    + "7ABDEEFD098C12838BEB4595B8A6B3E20D1164D4EF3D580AC"
                    + "C16B8654B6E743B2A1D0397523870D0125EA90C3198C1C981"
                    + "FFD5687EF8343EBC083388EC59301665677B05848CC5FABB1"
                    + "E65C30F118DF391757D297BEA0197A4889A75969B4B3C1A52"
                    + "D4AD7DB115D86D58513A512A2B771E8EC606D0485A3A6B334"
                    + "88FC85CE84B40BEA7B73E7B56BA739344FCB6E7ADD6016623"
                    + "F1680F2E021A6F5888197572BE226623262A0736AEE6E6724"
                    + "BBD33AF8A068F6";

    private static final String EXPECTED_CERT_SIGNATURE_FOR_CERT_3 =
            "17342F73AB2B008707DE08CAD5C7974C0036004E4AABE6AA266823043"
                    + "D0B9852A3E5B6BCB632F6363A025D0B6CA382512C04281432"
                    + "D0B370D681804456ADF30B34EA4A8BA556110D3977D01B05B"
                    + "3227E420CF7487AB133EE43CE6EA0C98BE10E6101DF9BFA71"
                    + "61A464914530CB2A2F0BEB3E6CB7B9102816206B4CDB179CD"
                    + "9B6C70B95F5CBABD225780B7F4164650F613A8BEAE4AA96DD"
                    + "BFD60AA3CDFCD00753E9F70A08A7CDC69AF674C415836F6A8"
                    + "73D5D481862029479AA73A275C9224D400115CF1C7DA64E57"
                    + "9C0BD39D27671A1F2C9B241DB06353D54CF68C34A8935C6CD"
                    + "E3C5D9D0847D3CFEB7EDA51DD31FBB77607CEE194C9B33BF5"
                    + "ECF576F7E90484";
    private static final String EXPECTED_CERT_SIGNATURE_FOR_CERT_4 =
            "77A3B38CD85DE0A7F24CE86A3B83C371B8EA9438863CEDDB04C7B16AD59"
                    + "3277B82E72D90B773CEC762A96F07A36D0AC0EE8189BAB87B607B"
                    + "4288F38A17B81B78B41D098134215796C61E66224808B3E3941BD"
                    + "48FB30066C01173E80CB531C5BE860EAFE17B6893A487F5FC512B"
                    + "5E5C75BC8FF66F95741480C4DB3826C64E41EA";

    private static final String EXPECTED_CERT_SIGNATURE_FOR_CERT_5 =
            "05EE695EB9161CB40CCF89F8D992494307BBC7A81E2D8B81BAF755D33ACE429"
                    + "277BA1453E900AF0C03BF1E5F09C886F7B86128CCD0CDF988FA469B"
                    + "D397BA967A028F5E1ED899B7999FA437F4FB748B75C509017A1284A"
                    + "D1098B0EB8C7E750D16FC99DADB0B32DF27B74F7BA1560DA56C3635"
                    + "47E84124E560B71D40B729326FC5";

    private static final String EXPECTED_CERT_SIGNATURE_FOR_CERT2_1 =
            "MIIDZTCCAk2gAwIBAgIBATANBgkqhkiG9w0BAQUFADBUMQswCQYDVQQGEwJVUzEUMBI"
                    + "GA1UECgwLRXhhbXBsZS5vcmcxDTALBgNVBAsMBHRlc3QxIDAeBgNVBAMMF1BsYXRmb"
                    + "3JtIENlcnRpZmljYXRlIENBMB4XDTE4MDQwNDE2NDUyMloXDTI4MDQwMzE2NDUyMlow"
                    + "VDELMAkGA1UEBhMCVVMxFDASBgNVBAoMC0V4YW1wbGUub3JnMQ0wCwYDVQQLDAR0Z"
                    + "XN0MSAwHgYDVQQDDBdQbGF0Zm9ybSBDZXJ0aWZpY2F0ZSBDQTCCASIwDQYJKoZIhv"
                    + "cNAQEBBQADggEPADCCAQoCggEBAKYnSJ7gHWl9BytxJQWWaYQzuYWjoeQ8PnLkYMm"
                    + "Kk8bV1v6hqRAg76p0QERubwtvDc3Rw0pVl5SqLku4ZzX7fzf3ra8IcrjR112f/ecAa"
                    + "gf+f4855anoYvBC5hELHnh6PQSyjl7wJJZiVLsB61gsumqfos5DnlaxoriUfW8Th26"
                    + "psnNIB+sbsn1f9WOHTDgXy81SGbgpG5+6joz1wXqpJvzZihIUNUSy8XQeusS22ZymI"
                    + "abL/Gs1P4doiJMeF651MNwjB/vdyG46KT56pDzc6TKJqo80Gb6HaeDS5RcakA9dRHz"
                    + "Vq7a3DOtzeNx84Cwl51tfE5MB/9mVP8grPjS5mQ8CAwEAAaNCMEAwDgYDVR0PAQH/B"
                    + "AQDAgIEMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0OBBYEFNszjDaflH5+feDx2e/3OHI"
                    + "Fx/XrMA0GCSqGSIb3DQEBBQUAA4IBAQBkAGcfS3yLGQ4s/UXJjpyr8yGPJSvpbP87d"
                    + "B+9dtncXhaHikOAXDXh+4uwhbU1vxWoatetJR0SYj+hFfPeyhqOz8NXP0L4IZFQOe7"
                    + "23VNyTIhjpcbV/aqQq6wUC8FAvfsUc9FGZFjyKrWv/r454Wt3YSca6nlSOSWAU3xmW"
                    + "32E3upuduJT4+a/VTvx2/4tPqPxe9fgQU+RkuZwWTL/1l0G/IbpnEVWB+BmY3VdNAy"
                    + "au2zASSlprrEHQ4yr2u4QoOxbOmFx9aQIBHGw2srb4/iWegwfLxFLRnvqSTQp2ZU8i"
                    + "AD2mtNMSHSGu26zfmjtu2EokCrFCa2cSbOZV9pTkQQ4";

    /**
     * Tests parsing of a platform credential.
     *
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void parseValidPlatformCertificate1() throws URISyntaxException {
        URL resource = this.getClass().getResource(TEST_PLATFORM_CERT_1);
        Path certPath = Paths.get(resource.toURI());

        try {
            new PlatformCredential(certPath);
            //fail if it manages to parse the certificate
            fail("Invalid certificate was parsed.");
        } catch (IOException ex) {
            if (ex == null || ex.getMessage().isEmpty()) {
                //fail if the exception is empty or null
                fail("Invalid Certificate produce null or empty exception");
            } else {
                Assertions.assertEquals(ex.getMessage(), "Invalid Attribute Credential Type: ");
            }
        }
    }

    /**
     * Tests the parsing of a platform credential that has the subject directory attribute
     * extension but is missing the subject alternative name extension.  This certificate
     * also has a policy extension, but it is not currently parsed.
     *
     * @throws IOException        if an IO error occurs during processing
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void parseValidPlatformCertificate3() throws IOException, URISyntaxException {
        URL resource = this.getClass().getResource(TEST_PLATFORM_CERT_2);
        Path certPath = Paths.get(resource.toURI());

        PlatformCredential credential = new PlatformCredential(certPath);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        final int year1 = 2017;
        final int month1 = 2;
        final int day1 = 23;
        final int hour1 = 22;
        final int minute1 = 34;
        final int second1 = 33;
        calendar.set(year1, month1, day1, hour1, minute1, second1);
        calendar.set(Calendar.MILLISECOND, 0);
        Assertions.assertEquals(credential.getBeginValidity().getTime(), calendar.getTime().getTime());

        final int year2 = 2030;
        final int month2 = 11;
        final int day2 = 31;
        final int hour2 = 23;
        final int minute2 = 59;
        final int second2 = 59;
        calendar.set(year2, month2, day2, hour2, minute2, second2);
        Assertions.assertEquals(credential.getEndValidity().getTime(), calendar.getTime().getTime());

        Assertions.assertNotNull(credential.getAttributeCertificate());
        byte[] sig = credential.getAttributeCertificate().getSignatureValue().getBytes();
        String sigStr = String.valueOf(Hex.encodeHex(sig));

        Assertions.assertEquals(sigStr.toUpperCase(), EXPECTED_CERT_SIGNATURE_FOR_CERT_2);

        String issuer = Certificate.getAttributeCertificateIssuerNames(
                credential.getAttributeCertificate().getAcinfo().getIssuer())[0].toString();

        Assertions.assertEquals(credential.getManufacturer(), "Intel");
        Assertions.assertEquals(credential.getModel(), "DE3815TYKH");
        Assertions.assertEquals(credential.getVersion(), "H26998-402");
        Assertions.assertEquals(issuer,
                "C=US,ST=CA,L=Santa Clara,O=Intel Corporation,"
                        + "OU=Transparent Supply Chain,CN=www.intel.com");

        Assertions.assertEquals(credential.getCredentialType(), "TCPA Trusted Platform Endorsement");

        // the platform certificate in this test does not contain the following information
        Assertions.assertNull(credential.getPlatformSerial());
        Assertions.assertEquals(credential.getMajorVersion(), 1);
        Assertions.assertEquals(credential.getMinorVersion(), 2);
        Assertions.assertEquals(credential.getRevisionLevel(), 1);
        Assertions.assertEquals(credential.getPlatformClass(), "1");
    }

    /**
     * Tests the parsing of another platform credential that has the subject directory attribute
     * extension but is missing the subject alternative name extension.  This certificate
     * also has a policy extension, but it is not currently parsed.
     *
     * @throws IOException        if an IO error occurs during processing
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void parseValidPlatformCertificate4() throws IOException, URISyntaxException {
        URL resource = this.getClass().getResource(TEST_PLATFORM_CERT_3);
        Path certPath = Paths.get(resource.toURI());

        PlatformCredential credential = new PlatformCredential(certPath);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        final int year1 = 2017;
        final int month1 = 2;
        final int day1 = 23;
        final int hour1 = 22;
        final int minute1 = 34;
        final int second1 = 33;
        calendar.set(year1, month1, day1, hour1, minute1, second1);
        calendar.set(Calendar.MILLISECOND, 0);
        Assertions.assertEquals(credential.getBeginValidity().getTime(), calendar.getTime().getTime());

        final int year2 = 2030;
        final int month2 = 11;
        final int day2 = 31;
        final int hour2 = 23;
        final int minute2 = 59;
        final int second2 = 59;
        calendar.set(year2, month2, day2, hour2, minute2, second2);
        Assertions.assertEquals(credential.getEndValidity().getTime(), calendar.getTime().getTime());

        Assertions.assertNotNull(credential.getAttributeCertificate());
        byte[] sig = credential.getAttributeCertificate().getSignatureValue().getBytes();
        String sigStr = String.valueOf(Hex.encodeHex(sig));

        Assertions.assertEquals(sigStr.toUpperCase(), EXPECTED_CERT_SIGNATURE_FOR_CERT_3);

        String issuer = Certificate.getAttributeCertificateIssuerNames(
                credential.getAttributeCertificate().getAcinfo().getIssuer()
        )[0].toString();

        Assertions.assertEquals(credential.getManufacturer(), "Intel");
        Assertions.assertEquals(credential.getModel(), "DE3815TYKH");
        Assertions.assertEquals(credential.getVersion(), "H26998-402");
        Assertions.assertEquals(issuer,
                "C=US,ST=CA,L=Santa Clara,O=Intel Corporation,"
                        + "OU=Transparent Supply Chain,CN=www.intel.com");

        Assertions.assertEquals(credential.getCredentialType(), "TCPA Trusted Platform Endorsement");

        // the platform certificate in this test does not contain the following information
        Assertions.assertNull(credential.getPlatformSerial());
        Assertions.assertEquals(credential.getMajorVersion(), 1);
        Assertions.assertEquals(credential.getMinorVersion(), 2);
        Assertions.assertEquals(credential.getRevisionLevel(), 1);
        Assertions.assertEquals(credential.getPlatformClass(), "1");
    }

    /**
     * Tests the parsing of a platform credential that has a combined baseboard and chassis
     * serial number in one attribute can be parsed.
     *
     * @throws IOException        if an IO error occurs during processing
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void parseValidPlatformCertificate5() throws IOException, URISyntaxException {
        URL resource = this.getClass().getResource(TEST_PLATFORM_CERT_4);
        Path certPath = Paths.get(resource.toURI());

        PlatformCredential credential = new PlatformCredential(certPath);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        final int year1 = 2017;
        final int month1 = 3;
        final int day1 = 21;
        final int hour1 = 17;
        final int minute1 = 5;
        final int second1 = 29;
        calendar.set(year1, month1, day1, hour1, minute1, second1);
        calendar.set(Calendar.MILLISECOND, 0);
        Assertions.assertEquals(credential.getBeginValidity().getTime(), calendar.getTime().getTime());

        final int year2 = 2030;
        final int month2 = 11;
        final int day2 = 31;
        final int hour2 = 23;
        final int minute2 = 59;
        final int second2 = 59;
        calendar.set(year2, month2, day2, hour2, minute2, second2);
        Assertions.assertEquals(credential.getEndValidity().getTime(), calendar.getTime().getTime());

        Assertions.assertNotNull(credential.getAttributeCertificate());
        byte[] sig = credential.getAttributeCertificate().getSignatureValue().getBytes();
        String sigStr = String.valueOf(Hex.encodeHex(sig));

        Assertions.assertEquals(sigStr.toUpperCase(), EXPECTED_CERT_SIGNATURE_FOR_CERT_4);

        String issuer = Certificate.getAttributeCertificateIssuerNames(
                credential.getAttributeCertificate().getAcinfo().getIssuer()
        )[0].toString();

        Assertions.assertEquals(credential.getManufacturer(), "Intel");
        Assertions.assertEquals(credential.getModel(), "DE3815TYKH");
        Assertions.assertEquals(credential.getVersion(), "H26998-402");
        Assertions.assertEquals(issuer,
                "C=US,ST=CA,L=Santa Clara,O=Intel Corporation,"
                        + "OU=Transparent Supply Chain,CN=www.intel.com");

        Assertions.assertEquals(credential.getCredentialType(), "TCPA Trusted Platform Endorsement");

        Assertions.assertEquals(credential.getChassisSerialNumber(), "G6YK42300C87");
        Assertions.assertEquals(credential.getPlatformSerial(), "GETY421001GV");
    }

    /**
     * Tests the parsing another platform credential that has a combined baseboard and chassis
     * serial number in one attribute can be parsed.
     *
     * @throws IOException        if an IO error occurs during processing
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void parseValidPlatformCertificate6() throws IOException, URISyntaxException {
        URL resource = this.getClass().getResource(TEST_PLATFORM_CERT_5);
        Path certPath = Paths.get(resource.toURI());

        PlatformCredential credential = new PlatformCredential(certPath);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        final int year1 = 2017;
        final int month1 = 3;
        final int day1 = 21;
        final int hour1 = 17;
        final int minute1 = 5;
        final int second1 = 30;
        calendar.set(year1, month1, day1, hour1, minute1, second1);
        calendar.set(Calendar.MILLISECOND, 0);
        Assertions.assertEquals(credential.getBeginValidity().getTime(), calendar.getTime().getTime());

        final int year2 = 2030;
        final int month2 = 11;
        final int day2 = 31;
        final int hour2 = 23;
        final int minute2 = 59;
        final int second2 = 59;
        calendar.set(year2, month2, day2, hour2, minute2, second2);
        Assertions.assertEquals(credential.getEndValidity().getTime(), calendar.getTime().getTime());

        Assertions.assertNotNull(credential.getAttributeCertificate());
        byte[] sig = credential.getAttributeCertificate().getSignatureValue().getBytes();
        String sigStr = String.valueOf(Hex.encodeHex(sig));

        Assertions.assertEquals(sigStr.toUpperCase(), EXPECTED_CERT_SIGNATURE_FOR_CERT_5);

        String issuer = Certificate.getAttributeCertificateIssuerNames(
                credential.getAttributeCertificate().getAcinfo().getIssuer()
        )[0].toString();

        Assertions.assertEquals(credential.getManufacturer(), "Intel");
        Assertions.assertEquals(credential.getModel(), "DE3815TYKH");
        Assertions.assertEquals(credential.getVersion(), "H26998-402");
        Assertions.assertEquals(issuer,
                "C=US,ST=CA,L=Santa Clara,O=Intel Corporation,"
                        + "OU=Transparent Supply Chain,CN=www.intel.com");

        Assertions.assertEquals(credential.getCredentialType(), "TCPA Trusted Platform Endorsement");

        Assertions.assertEquals(credential.getChassisSerialNumber(), "G6YK42300CB6");
        Assertions.assertEquals(credential.getPlatformSerial(), "GETY42100160");
    }

    /**
     * Tests isIssuer of a platform credential.
     *
     * @throws IOException        if an IO error occurs during processing
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void testIsIssuer() throws IOException, URISyntaxException {

        URL resource = this.getClass().getResource(TEST_PLATFORM_CERT2_1);
        Path certPath = Paths.get(resource.toURI());

        PlatformCredential platformCert = new PlatformCredential(certPath);

        Certificate issuer = new CertificateAuthorityCredential(
                Base64.decode(EXPECTED_CERT_SIGNATURE_FOR_CERT2_1));

        //Check if issuer certificate issued the platform credential
        Assertions.assertTrue(platformCert.isIssuer(issuer).isEmpty());
    }

    /**
     * Tests platform Configuration Values.
     *
     * @throws IOException        if an IO error occurs during processing
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void testPlatformConfiguration() throws IOException, URISyntaxException {

        URL resource = this.getClass().getResource(TEST_PLATFORM_CERT2_1);
        Path certPath = Paths.get(resource.toURI());

        PlatformCredential platformCert = new PlatformCredential(certPath);
        PlatformConfigurationV1 platformConfigV1 = platformCert.getPlatformConfigurationV1();

        //Check component identifier
        List<ComponentIdentifier> allComponents = platformConfigV1.getComponentIdentifiers();
        if (allComponents.isEmpty()) {
            Assertions.fail("Component Identifier is empty.");
        }

        final int expectedComponentsSize = 7;
        Assertions.assertEquals(expectedComponentsSize, allComponents.size());
        ComponentIdentifier component;

        //Check component #2
        component = allComponents.get(1);
        Assertions.assertEquals("Intel Corporation", component.getComponentManufacturer()
                .getString());
        Assertions.assertEquals("NUC7i5DNB", component.getComponentModel()
                .getString());
        Assertions.assertEquals("BTDN732000QM", component.getComponentSerial()
                .getString());

        //Check component #3
        component = allComponents.get(2);
        Assertions.assertEquals("Intel(R) Corporation", component.getComponentManufacturer()
                .getString());
        Assertions.assertEquals("Core i5", component.getComponentModel().getString());
        Assertions.assertTrue(component.getFieldReplaceable().isTrue());

        //Check component #5
        final int component5Position = 4;
        component = allComponents.get(component5Position);
        Assertions.assertEquals("Ethernet Connection I219-LM", component.getComponentModel()
                .getString());
        Assertions.assertEquals("8c:0f:6f:72:c6:c5", component.getComponentAddresses().get(0)
                .getAddressValue()
                .getString());
        Assertions.assertEquals("ethernet mac", component.getComponentAddresses().get(0)
                .getAddressTypeValue());

        //Check Platform Properties
        List<PlatformProperty> platformProperties = platformConfigV1.getPlatformProperties();
        if (platformProperties.isEmpty()) {
            Assertions.fail("Platform Properties is empty.");
        }

        Assertions.assertEquals(platformProperties.size(), 2);

        PlatformProperty property;

        //Check property #1
        property = platformProperties.get(0);
        Assertions.assertEquals("vPro", property.getPropertyName().getString());
        Assertions.assertEquals("true", property.getPropertyValue().getString());

        //Check property #2
        property = platformProperties.get(1);
        Assertions.assertEquals("AMT", property.getPropertyName().getString());
        Assertions.assertEquals("true", property.getPropertyValue().getString());

        //Check Platform Properties URI
        URIReference platformPropertyUri = platformConfigV1.getPlatformPropertiesUri();

        Assertions.assertNotNull(platformPropertyUri);
        Assertions.assertEquals("https://www.intel.com/platformproperties.xml",
                platformPropertyUri.getUniformResourceIdentifier()
                        .getString());
        Assertions.assertNull(platformPropertyUri.getHashAlgorithm());
        Assertions.assertNull(platformPropertyUri.getHashValue());
    }

    /**
     * Tests Platform Configuration Values. View platform Properties
     *
     * @throws IOException        if an IO error occurs during processing
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void testPlatformConfiguration2() throws IOException, URISyntaxException {

        URL resource = this.getClass().getResource(TEST_PLATFORM_CERT2_2);
        Path certPath = Paths.get(resource.toURI());

        PlatformCredential platformCert = new PlatformCredential(certPath);
        PlatformConfigurationV1 platformConfigV1 = platformCert.getPlatformConfigurationV1();

        //Check component identifier
        List<ComponentIdentifier> allComponents = platformConfigV1.getComponentIdentifiers();
        Assertions.assertTrue(allComponents.isEmpty());

        List<PlatformProperty> platformProperties = platformConfigV1.getPlatformProperties();
        if (platformProperties.isEmpty()) {
            Assertions.fail("Platform Properties is empty.");
        }
        Assertions.assertEquals(platformProperties.size(), 2);

        PlatformProperty property;

        //Check property #1
        property = platformProperties.get(0);
        Assertions.assertEquals("vPro", property.getPropertyName().getString());
        Assertions.assertEquals("true", property.getPropertyValue().getString());

        //Check property #2
        property = platformProperties.get(1);
        Assertions.assertEquals("AMT", property.getPropertyName().getString());
        Assertions.assertEquals("true", property.getPropertyValue().getString());
    }

    /**
     * Tests Platform Configuration Values. View platform Properties
     *
     * @throws IOException        if an IO error occurs during processing
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void testPlatformConfiguration3() throws IOException, URISyntaxException {

        URL resource = this.getClass().getResource(TEST_PLATFORM_CERT2_3);
        Path certPath = Paths.get(resource.toURI());

        PlatformCredential platformCert = new PlatformCredential(certPath);
        PlatformConfigurationV1 platformConfigV1 = platformCert.getPlatformConfigurationV1();

        //Check component identifier
        List<ComponentIdentifier> allComponents = platformConfigV1.getComponentIdentifiers();
        if (allComponents.isEmpty()) {
            Assertions.fail("Component Identifier is empty.");
        }

        final int expectedComponentsSize = 3;
        Assertions.assertEquals(expectedComponentsSize, allComponents.size());
        ComponentIdentifier component;

        //Check component #2
        component = allComponents.get(1);
        Assertions.assertEquals("Intel(R) Corporation", component.getComponentManufacturer()
                .getString());
        Assertions.assertEquals("Intel(R) Core(TM) i5-7300U CPU @ 2.60GHz", component.getComponentModel()
                .getString());

        //Check component #3
        component = allComponents.get(2);
        Assertions.assertEquals("BIOS", component.getComponentModel()
                .getString());
        Assertions.assertEquals(ComponentIdentifier.NOT_SPECIFIED_COMPONENT, component.getComponentSerial()
                .getString());
        Assertions.assertEquals("DNKBLi5v.86A.0019.2017.0804.1146", component.getComponentRevision()
                .getString());

        //Check Platform Properties
        List<PlatformProperty> platformProperties = platformConfigV1.getPlatformProperties();
        if (platformProperties.isEmpty()) {
            Assertions.fail("Platform Properties is empty.");
        }

        Assertions.assertEquals(platformProperties.size(), 2);

        //Check Platform Properties URI
        URIReference platformPropertyUri = platformConfigV1.getPlatformPropertiesUri();

        Assertions.assertNotNull(platformPropertyUri);
        Assertions.assertEquals("https://www.intel.com/platformproperties.xml",
                platformPropertyUri.getUniformResourceIdentifier()
                        .getString());
        Assertions.assertNull(platformPropertyUri.getHashAlgorithm());
        Assertions.assertNull(platformPropertyUri.getHashValue());

        //Test TBBSecurityAssertion
        TBBSecurityAssertion tbbSec = platformCert.getTBBSecurityAssertion();
        Assertions.assertNotNull(tbbSec);
        Assertions.assertEquals("3.1", tbbSec.getCcInfo().getVersion().getString());
        Assertions.assertEquals("1.2.3.4.5.6", tbbSec.getCcInfo().getProfileOid().getId());
        Assertions.assertEquals("140-2", tbbSec.getFipsLevel().getVersion().getString());
        Assertions.assertEquals("https://www.intel.com/isocertification.pdf",
                tbbSec.getIso9000Uri().getString());
    }

    /**
     * Tests Platform Configuration Values. View platform Properties
     *
     * @throws IOException        if an IO error occurs during processing
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void testPlatformConfiguration4() throws IOException, URISyntaxException {

        URL resource = this.getClass().getResource(TEST_PLATFORM_CERT2_4);
        Path certPath = Paths.get(resource.toURI());

        PlatformCredential platformCert = new PlatformCredential(certPath);
        PlatformConfigurationV1 platformConfigV1 = platformCert.getPlatformConfigurationV1();

        //Check component identifier
        List<ComponentIdentifier> allComponents = platformConfigV1.getComponentIdentifiers();
        if (allComponents.isEmpty()) {
            Assertions.fail("Component Identifier is empty.");
        }

        final int expectedComponentsSize = 7;
        Assertions.assertEquals(expectedComponentsSize, allComponents.size());
        ComponentIdentifier component;

        //Check component #1
        component = allComponents.get(0);
        Assertions.assertEquals("NUC7i5DNB", component.getComponentModel()
                .getString());
        Assertions.assertEquals("J57626-401", component.getComponentRevision()
                .getString());

        //Check component #7
        final int component7Position = 6;
        component = allComponents.get(component7Position);
        Assertions.assertFalse(component.getComponentAddresses().isEmpty());
        Assertions.assertEquals("8c:0f:6f:72:c6:c5", component.getComponentAddresses().get(0)
                .getAddressValue()
                .getString());
        Assertions.assertEquals("ethernet mac", component.getComponentAddresses().get(0)
                .getAddressTypeValue());

        //Check Platform Properties
        List<PlatformProperty> platformProperties = platformConfigV1.getPlatformProperties();
        if (platformProperties.isEmpty()) {
            Assertions.fail("Platform Properties is empty.");
        }

        Assertions.assertEquals(platformProperties.size(), 2);

        //Check Platform Properties URI
        URIReference platformPropertyUri = platformConfigV1.getPlatformPropertiesUri();

        Assertions.assertNotNull(platformPropertyUri);
        Assertions.assertEquals("https://www.intel.com/platformproperties.xml",
                platformPropertyUri.getUniformResourceIdentifier()
                        .getString());
        Assertions.assertNull(platformPropertyUri.getHashAlgorithm());
        Assertions.assertNull(platformPropertyUri.getHashValue());

        //Test TBBSecurityAssertion
        TBBSecurityAssertion tbbSec = platformCert.getTBBSecurityAssertion();
        Assertions.assertNotNull(tbbSec);
        Assertions.assertEquals("3.1", tbbSec.getCcInfo().getVersion().getString());
        Assertions.assertEquals("1.2.3.4.5.6", tbbSec.getCcInfo().getProfileOid().getId());
        Assertions.assertEquals("140-2", tbbSec.getFipsLevel().getVersion().getString());
        Assertions.assertEquals("https://www.intel.com/isocertification.pdf",
                tbbSec.getIso9000Uri().getString());

    }

    /**
     * Tests Platform Configuration Values. View platform Properties
     *
     * @throws IOException        if an IO error occurs during processing
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void testPlatformConfiguration5() throws IOException, URISyntaxException {

        URL resource = this.getClass().getResource(TEST_PLATFORM_CERT2_SPEC2);
        Path certPath = Paths.get(resource.toURI());

        PlatformCredential platformCert = new PlatformCredential(certPath);
        PlatformConfigurationV2 platformConfigV2 = platformCert.getPlatformConfigurationV2();

        //Check component identifier
        List<ComponentIdentifierV2> allComponents = platformConfigV2.getComponentIdentifiers();
        Assertions.assertFalse(allComponents.isEmpty());

        final int component6Position = 5;
        ComponentIdentifierV2 component = allComponents.get(component6Position);
        Assertions.assertTrue(component.isVersion2());

        List<PlatformProperty> platformProperties = platformConfigV2.getPlatformProperties();
        if (platformProperties.isEmpty()) {
            Assertions.fail("Platform Properties is empty.");
        }

        final int platformPropSize = 3;
        Assertions.assertEquals(platformPropSize, platformProperties.size());

        PlatformProperty property;

        //Check property #1
        property = platformProperties.get(0);
        Assertions.assertEquals("AMT", property.getPropertyName().getString());
        Assertions.assertEquals("true", property.getPropertyValue().getString());

        //Check property #2
        property = platformProperties.get(1);
        Assertions.assertEquals("vPro Enabled", property.getPropertyName().getString());
        Assertions.assertEquals("true", property.getPropertyValue().getString());

        //Check property #3
        property = platformProperties.get(2);
        Assertions.assertEquals("DropShip Enabled", property.getPropertyName().getString());
        Assertions.assertEquals("false", property.getPropertyValue().getString());
    }

    /**
     * Tests Platform Configuration Values. View platform Properties
     *
     * @throws IOException        if an IO error occurs during processing
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void testPlatformConfiguration6() throws IOException, URISyntaxException {

        URL resource = this.getClass().getResource(TEST_BASE_PLATFORM_CERT_1);
        Path certPath = Paths.get(resource.toURI());

        PlatformCredential platformCert = new PlatformCredential(certPath);
        PlatformConfigurationV2 platformConfigV2 = platformCert.getPlatformConfigurationV2();

        Assertions.assertInstanceOf(PlatformConfigurationV2.class, platformConfigV2);
        Assertions.assertEquals(platformConfigV2.getPlatformPropertiesUri()
                        .getUniformResourceIdentifier().toString(),
                "https://www.intel.com/platformproperties.xml");
//        Assertions.assertNotNull(platformConfigV1.getComponentIdentifiersUri());

//        Assertions.assertEquals(platformConfigV1.getComponentIdentifiersUri()
//                        .getUniformResourceIdentifier().toString(),
//                "https://www.intel.com/platformidentifiers.xml");

    }

    /**
     * Tests Platform Configuration Values. View platform Properties
     *
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     * @throws IOException        if there is a problem reading the cert file
     */
    @Test
    public final void testSmallNewPlatformCredential() throws URISyntaxException, IOException {
        Path path = Paths.get(this.getClass().getResource(
                "/validation/platform_credentials_2/small_attribute_cert_2187.pem").toURI());
        PlatformCredential credential = new PlatformCredential(path);
        Assertions.assertNotNull(credential);
    }

    /**
     * Tests Platform Configuration Values. View platform Properties
     *
     * @throws IOException        if an IO error occurs during processing
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void testMediumNewPlatformCredential() throws URISyntaxException, IOException {
        Path path = Paths.get(this.getClass().getResource(
                "/validation/platform_credentials_2/medium_attribute_cert_2187.pem").toURI());
        PlatformCredential credential = new PlatformCredential(path);
        Assertions.assertNotNull(credential);
    }

    /**
     * Tests Platform Configuration Values. View platform Properties
     *
     * @throws IOException        if an IO error occurs during processing
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void testLargeNewPlatformCredential() throws URISyntaxException, IOException {
        Path path = Paths.get(this.getClass().getResource(
                "/validation/platform_credentials_2/large_attribute_cert_2187.pem").toURI());
        PlatformCredential credential = new PlatformCredential(path);
        Assertions.assertNotNull(credential);
    }

    /**
     * Tests Platform Configuration Values. View platform Properties
     *
     * @throws IOException        if an IO error occurs during processing
     * @throws URISyntaxException if there is a problem constructing the cert's URI
     */
    @Test
    public final void testFlawedNewPlatformCredential() throws URISyntaxException, IOException {
        Path path = Paths.get(this.getClass().getResource(
                "/validation/platform_credentials_2/flawed_attribute_cert_2187.pem").toURI());
        PlatformCredential credential = new PlatformCredential(path);
        Assertions.assertNotNull(credential);
    }
}
