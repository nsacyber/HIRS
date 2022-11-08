package hirs.attestationca;

import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.PlatformCredential;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.x509.Extension;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Tests for {@see IssuedCertificateAttributeHelper}.
 */
public class IssuedCertificateAttributeHelperTest {

    private static final String NUC1_EC = "/certificates/nuc-1/tpmcert.pem";

    private static final String INTEL_PC = "/certificates/platform_certs_2/"
            + "Intel_pc.pem";

    private static final String TEST_HOSTNAME = "box1";

    private static final String TPM_MANUFACTURER = "2.23.133.2.1";

    private static final String TPM_MODEL = "2.23.133.2.2";

    private static final String TPM_VERSION = "2.23.133.2.3";

    private static final String TPM_ID_LABEL_OID = "2.23.133.2.15";

    private static final String PLATFORM_MANUFACTURER = "2.23.133.2.4";

    private static final String PLATFORM_MODEL = "2.23.133.2.5";

    private static final String PLATFORM_VERSION = "2.23.133.2.6";

    /**
     * Test that provide a null host name and is rejected.
     * @throws IOException an IO error occurs
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectNullHostName() throws IOException {
        IssuedCertificateAttributeHelper.buildSubjectAlternativeNameFromCerts(null, null, "");
    }

    /**
     * Test that subject alt name can be built without an EC or PC.
     * @throws IOException an IO error occurs
     */
    @Test
    public void buildAttributesNoEndorsementNoPlatform() throws IOException {
        Extension subjectAlternativeName =
            IssuedCertificateAttributeHelper.buildSubjectAlternativeNameFromCerts(
                    null, new ArrayList<PlatformCredential>(), TEST_HOSTNAME);

        Map<String, String> subjectAlternativeNameAttrMap = getSubjectAlternativeNameAttributes(
                subjectAlternativeName);

        assertNull(subjectAlternativeNameAttrMap.get(TPM_MANUFACTURER));
        assertNull(subjectAlternativeNameAttrMap.get(TPM_MODEL));
        assertNull(subjectAlternativeNameAttrMap.get(TPM_VERSION));
        assertNull(subjectAlternativeNameAttrMap.get(PLATFORM_MANUFACTURER));
        assertNull(subjectAlternativeNameAttrMap.get(PLATFORM_MODEL));
        assertNull(subjectAlternativeNameAttrMap.get(PLATFORM_VERSION));
        assertEquals(subjectAlternativeNameAttrMap.get(TPM_ID_LABEL_OID), TEST_HOSTNAME);
    }

    /**
     * Test that subject alt name can be built with an EC but no PC.
     * @throws IOException an IO error occurs
     * @throws URISyntaxException unrecognized URI for EC Path
     */
    @Test
    public void buildAttributesEndorsementNoPlatform() throws IOException, URISyntaxException {
        Path endorsementCredentialPath = Paths.get(getClass().getResource(
                NUC1_EC).toURI());
        EndorsementCredential endorsementCredential = new EndorsementCredential(
                endorsementCredentialPath);
        Extension subjectAlternativeName =
                IssuedCertificateAttributeHelper.buildSubjectAlternativeNameFromCerts(
                        endorsementCredential, new ArrayList<PlatformCredential>(), TEST_HOSTNAME);

        Map<String, String> subjectAlternativeNameAttrMap = getSubjectAlternativeNameAttributes(
                subjectAlternativeName);

        assertEquals(subjectAlternativeNameAttrMap.get(TPM_MANUFACTURER),
                endorsementCredential.getManufacturer());
        assertEquals(subjectAlternativeNameAttrMap.get(TPM_MODEL),
                endorsementCredential.getModel());
        assertEquals(subjectAlternativeNameAttrMap.get(TPM_VERSION),
                endorsementCredential.getVersion());
        assertNull(subjectAlternativeNameAttrMap.get(PLATFORM_MANUFACTURER));
        assertNull(subjectAlternativeNameAttrMap.get(PLATFORM_MODEL));
        assertNull(subjectAlternativeNameAttrMap.get(PLATFORM_VERSION));
        assertEquals(subjectAlternativeNameAttrMap.get(TPM_ID_LABEL_OID),
                TEST_HOSTNAME);
    }

    /**
     * Test that subject alt name can be built with an PC but no EC.
     * @throws IOException an IO error occurs
     * @throws URISyntaxException unrecognized URI for PC Path
     */
    @Test
    public void buildAttributesPlatformNoEndorsement() throws IOException, URISyntaxException {
        Path platformCredentialPath = Paths.get(getClass().getResource(
                INTEL_PC).toURI());
        PlatformCredential platformCredential = new PlatformCredential(
                platformCredentialPath);
        List<PlatformCredential> platformCredentialList = new ArrayList<>();
        platformCredentialList.add(platformCredential);
        Extension subjectAlternativeName =
                IssuedCertificateAttributeHelper.buildSubjectAlternativeNameFromCerts(
                        null, platformCredentialList, TEST_HOSTNAME);

        Map<String, String> subjectAlternativeNameAttrMap = getSubjectAlternativeNameAttributes(
                subjectAlternativeName);

        assertNull(subjectAlternativeNameAttrMap.get(TPM_MANUFACTURER));
        assertNull(subjectAlternativeNameAttrMap.get(TPM_MODEL));
        assertNull(subjectAlternativeNameAttrMap.get(TPM_VERSION));
        assertEquals(subjectAlternativeNameAttrMap.get(PLATFORM_MANUFACTURER),
                platformCredential.getManufacturer());
        assertEquals(subjectAlternativeNameAttrMap.get(PLATFORM_MODEL),
                platformCredential.getModel());
        assertEquals(subjectAlternativeNameAttrMap.get(PLATFORM_VERSION),
                platformCredential.getVersion());
        assertEquals(subjectAlternativeNameAttrMap.get(TPM_ID_LABEL_OID),
                TEST_HOSTNAME);
    }

    /**
     * Test that subject alt name can be built with a PC and an EC.
     * @throws IOException an IO error occurs
     * @throws URISyntaxException unrecognized URI for EC or PC Path
     */
    @Test
    public void buildAttributesPlatformAndEndorsement() throws IOException, URISyntaxException {
        Path endorsementCredentialPath = Paths.get(getClass().getResource(
                NUC1_EC).toURI());
        Path platformCredentialPath = Paths.get(getClass().getResource(
                INTEL_PC).toURI());
        EndorsementCredential endorsementCredential = new EndorsementCredential(
                endorsementCredentialPath);
        PlatformCredential platformCredential = new PlatformCredential(
                platformCredentialPath);
        List<PlatformCredential> platformCredentialList = new ArrayList<>();
        platformCredentialList.add(platformCredential);
        Extension subjectAlternativeName =
                IssuedCertificateAttributeHelper.buildSubjectAlternativeNameFromCerts(
                        endorsementCredential, platformCredentialList, TEST_HOSTNAME);

        Map<String, String> subjectAlternativeNameAttrMap = getSubjectAlternativeNameAttributes(
                subjectAlternativeName);

        assertEquals(subjectAlternativeNameAttrMap.get(TPM_MANUFACTURER),
                endorsementCredential.getManufacturer());
        assertEquals(subjectAlternativeNameAttrMap.get(TPM_MODEL),
                endorsementCredential.getModel());
        assertEquals(subjectAlternativeNameAttrMap.get(TPM_VERSION),
                endorsementCredential.getVersion());
        assertEquals(subjectAlternativeNameAttrMap.get(PLATFORM_MANUFACTURER),
                platformCredential.getManufacturer());
        assertEquals(subjectAlternativeNameAttrMap.get(PLATFORM_MODEL),
                platformCredential.getModel());
        assertEquals(subjectAlternativeNameAttrMap.get(PLATFORM_VERSION),
                platformCredential.getVersion());
        assertEquals(subjectAlternativeNameAttrMap.get(TPM_ID_LABEL_OID),
                TEST_HOSTNAME);
    }

    private Map<String, String> getSubjectAlternativeNameAttributes(
            Extension subjectAlternativeName) {
        Map<String, String> subjectAlternativeNameAttrMap = new HashMap<>();

        DLSequence dlSequence = (DLSequence) subjectAlternativeName.getParsedValue();
        DERTaggedObject derTaggedObject = (DERTaggedObject) dlSequence.getObjectAt(0);
        DERSequence derSequence = (DERSequence) derTaggedObject.getLoadedObject();

        Enumeration enumeration = derSequence.getObjects();
        while (enumeration.hasMoreElements()) {
            DERSet set = (DERSet) enumeration.nextElement();
            DERSequence innerDerSequence = (DERSequence) set.getObjectAt(0);

            subjectAlternativeNameAttrMap.put(innerDerSequence.getObjectAt(0).toString(),
                    innerDerSequence.getObjectAt(1).toString());
        }
        return subjectAlternativeNameAttrMap;
    }
}
