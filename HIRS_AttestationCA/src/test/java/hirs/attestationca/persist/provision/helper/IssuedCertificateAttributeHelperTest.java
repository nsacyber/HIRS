package hirs.attestationca.persist.provision.helper;

import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.x509.Extension;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    @Test
    public void rejectNullHostName() throws IOException {
        assertThrows(IllegalArgumentException.class, () ->
        IssuedCertificateAttributeHelper.buildSubjectAlternativeNameFromCerts(null, null, ""));
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
        assertEquals(TEST_HOSTNAME, subjectAlternativeNameAttrMap.get(TPM_ID_LABEL_OID));
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

        assertEquals(endorsementCredential.getManufacturer(),
                subjectAlternativeNameAttrMap.get(TPM_MANUFACTURER));
        assertEquals(endorsementCredential.getModel(),
                subjectAlternativeNameAttrMap.get(TPM_MODEL));
        assertEquals(endorsementCredential.getVersion(),
                subjectAlternativeNameAttrMap.get(TPM_VERSION));
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
        assertEquals(platformCredential.getManufacturer(),
                subjectAlternativeNameAttrMap.get(PLATFORM_MANUFACTURER));
        assertEquals(platformCredential.getModel(),
                subjectAlternativeNameAttrMap.get(PLATFORM_MODEL));
        assertEquals(platformCredential.getVersion(),
                subjectAlternativeNameAttrMap.get(PLATFORM_VERSION));
        assertEquals(TEST_HOSTNAME,
                subjectAlternativeNameAttrMap.get(TPM_ID_LABEL_OID));
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

        assertEquals(endorsementCredential.getManufacturer(),
                subjectAlternativeNameAttrMap.get(TPM_MANUFACTURER));
        assertEquals(endorsementCredential.getModel(),
                subjectAlternativeNameAttrMap.get(TPM_MODEL));
        assertEquals(endorsementCredential.getVersion(),
                subjectAlternativeNameAttrMap.get(TPM_VERSION));
        assertEquals(platformCredential.getManufacturer(),
                subjectAlternativeNameAttrMap.get(PLATFORM_MANUFACTURER));
        assertEquals(platformCredential.getModel(),
                subjectAlternativeNameAttrMap.get(PLATFORM_MODEL));
        assertEquals(platformCredential.getVersion(),
                subjectAlternativeNameAttrMap.get(PLATFORM_VERSION));
        assertEquals(TEST_HOSTNAME,
                subjectAlternativeNameAttrMap.get(TPM_ID_LABEL_OID));
    }

    private Map<String, String> getSubjectAlternativeNameAttributes(
            Extension subjectAlternativeName) {
        Map<String, String> subjectAlternativeNameAttrMap = new HashMap<>();

        DLSequence dlSequence = (DLSequence) subjectAlternativeName.getParsedValue();
        ASN1TaggedObject asn1TaggedObject = (ASN1TaggedObject) dlSequence.getObjectAt(0);
        ASN1Sequence asn1Sequence = (ASN1Sequence) asn1TaggedObject.getBaseObject();

        Enumeration enumeration = asn1Sequence.getObjects();
        while (enumeration.hasMoreElements()) {
            ASN1Set set = (ASN1Set) enumeration.nextElement();
            ASN1Sequence innerAsn1Sequence = (ASN1Sequence) set.getObjectAt(0);

            subjectAlternativeNameAttrMap.put(innerAsn1Sequence.getObjectAt(0).toString(),
                    innerAsn1Sequence.getObjectAt(1).toString());
        }
        return subjectAlternativeNameAttrMap;
    }
}
