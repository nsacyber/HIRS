package hirs.attestationca.portal.page.controllers;

import com.google.protobuf.ByteString;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentEvidenceResultRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentEvidenceResult;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.attestationca.persist.service.ComponentEvidencePageService.LinkEdge;
import hirs.attestationca.persist.service.ComponentEvidencePageService.LinkStep;
import hirs.attestationca.persist.validation.ComponentEvidenceValidator;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageControllerTest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renders the component evidence page and the platform certificate's check mark with evidence from an HP EpSC.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ComponentEvidencePageControllerTest extends PageControllerTest {
    private static final String RESOURCES = "/component_evidence/";
    // SMBIOS byte order of 01b5ed98-a5d0-439e-a88b-b8e15625e167
    private static final byte[] PLATFORM_UUID = Hex.decode("98edb501d0a59e43a88bb8e15625e167");

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private ComponentResultRepository componentResultRepository;

    @Autowired
    private ComponentEvidenceResultRepository componentEvidenceResultRepository;

    private PlatformCredential platformCredential;

    private ComponentEvidenceResult evidenceResult;

    private ComponentEvidenceResult staleResult;

    private CertificateAuthorityCredential testCa;

    private byte[] deviceId;

    /**
     * Constructor.
     */
    public ComponentEvidencePageControllerTest() {
        super(Page.COMPONENT_EVIDENCE);
    }

    /**
     * Saves the platform certificate, its components, and a verified evidence result for the EpSC.
     *
     * @throws Exception if a resource cannot be read
     */
    @BeforeAll
    public void prepareTests() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        testCa = certificateRepository.save((CertificateAuthorityCredential)
                getTestCertificate(CertificateAuthorityCredential.class, RESOURCES + "test_ca_mldsa87.pem"));
        platformCredential = certificateRepository.save((PlatformCredential)
                getTestCertificate(PlatformCredential.class, RESOURCES + "platform_cert.pem"));
        ComponentIdentifierV2 epsc = null;
        ComponentResult epscResult = null;
        for (ComponentIdentifierV2 component
                : platformCredential.getPlatformConfigurationV2().getComponentIdentifiers()) {
            ComponentResult saved = componentResultRepository.save(new ComponentResult(
                    platformCredential.getPlatformSerial(), platformCredential.getSerialNumber().toString(),
                    platformCredential.getPlatformChainType(), component));
            if (component.getComponentPlatformCert() != null) {
                epsc = component;
                epscResult = saved;
            }
        }

        deviceId = read("deviceid.cer");
        byte[] certIdentifier = epsc.getComponentPlatformCert().getHashOverSignatureValue();
        evidenceResult = verifiedResult(epscResult.getId(), platformCredential.getId(), certIdentifier);
        // As after the platform certificate was deleted and uploaded again: the recorded id no longer exists
        staleResult = verifiedResult(epscResult.getId(), UUID.randomUUID(), certIdentifier);
    }

    private ComponentEvidenceResult verifiedResult(final UUID componentId, final UUID platformCredentialId,
                                                   final byte[] certIdentifier) throws Exception {
        byte[] nonce = Hex.decode(new String(read("nonce.hex"), StandardCharsets.US_ASCII).trim());
        ComponentEvidenceResult result = new ComponentEvidenceResult(componentId, platformCredentialId,
                "hp-test", ProvisionerTpm2.EvidenceFormat.HP_EPSC_BOOTLOG.name(), new byte[0], nonce);
        ComponentEvidenceValidator.validateEpscBootlog(result, certIdentifier, PLATFORM_UUID,
                ProvisionerTpm2.ExternalEvidenceResult.newBuilder()
                        .setEvidenceFormat(ProvisionerTpm2.EvidenceFormat.HP_EPSC_BOOTLOG)
                        .setEvidenceVersion("1")
                        .setEvidence(ByteString.copyFrom(read("evidence.bin")))
                        .addAllCertificateChain(List.of(ByteString.copyFrom(read("attestation.cer")),
                                ByteString.copyFrom(deviceId)))
                        .build());
        return componentEvidenceResultRepository.save(result);
    }

    /**
     * The trust links run from the Device ID certificate through the component identifier and the platform
     * certificate to the ML-DSA-87 CA that issued it, and each one verifies.
     *
     * @throws Exception if the request fails
     */
    @Test
    public void testTrustLinksVerified() throws Exception {
        List<LinkStep> graph = linkGraph(evidenceResult);
        assertEquals(List.of("EpSC Device ID certificate", "Component identifier", "Platform certificate",
                "Platform certificate issuer"), graph.stream().map(step -> step.node().title()).toList());
        assertTrue(graph.stream().map(LinkStep::next).filter(Objects::nonNull).allMatch(LinkEdge::verified));
        assertNull(graph.getLast().next());
        assertEquals("SERIALNUMBER=0x01775AB4FCD34768DB, CN=\"HP EpSC 24 ROM v21.22.00.03, L0 v21.21.01.80\"",
                graph.getFirst().node().fields().get("Subject"));
    }

    /**
     * The platform certificate and its trust chain download.
     *
     * @throws Exception if the request fails
     */
    @Test
    public void testDownloadPlatformCertificateAndTrustChain() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(getPagePath() + "/download")
                        .param("id", evidenceResult.getId().toString())
                        .param("file", "platform"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(platformCredential.getRawBytes()));
        String chain = getMockMvc().perform(MockMvcRequestBuilders.get(getPagePath() + "/download")
                        .param("id", evidenceResult.getId().toString())
                        .param("file", "chain"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String base64 = chain.replace("-----BEGIN CERTIFICATE-----", "").replace("-----END CERTIFICATE-----", "");
        assertArrayEquals(testCa.getRawBytes(), Base64.getMimeDecoder().decode(base64));
    }

    /**
     * When the platform certificate recorded with the result is gone, the page finds it from the component.
     *
     * @throws Exception if the request fails
     */
    @Test
    public void testStalePlatformCertificateIdFallsBack() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(getPagePath())
                        .param("id", staleResult.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("initialData",
                        hasEntry("platformCredentialId", platformCredential.getId().toString())));
    }

    /**
     * A platform certificate that does not exist reports that it was not found.
     *
     * @throws Exception if the request fails
     */
    @Test
    public void testMissingPlatformCertificate() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get("/HIRS_AttestationCAPortal/portal/certificate-details")
                        .param("id", UUID.randomUUID().toString())
                        .param("type", "platform"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Unable to find certificate with ID")));
    }

    /**
     * The platform certificate details page marks the EpSC and links to its latest evidence.
     *
     * @throws Exception if the request fails
     */
    @Test
    public void testPlatformCertificateShowsCheckMark() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get("/HIRS_AttestationCAPortal/portal/certificate-details")
                        .param("id", platformCredential.getId().toString())
                        .param("type", "platform"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Key possession verified")))
                .andExpect(content().string(containsString("component-evidence?id=" + staleResult.getId())));
    }

    /**
     * The ML-DSA-87 CA's details page shows its algorithm and parameter set.
     *
     * @throws Exception if the request fails
     */
    @Test
    public void testMlDsaCaDetails() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get("/HIRS_AttestationCAPortal/portal/certificate-details")
                        .param("id", testCa.getId().toString())
                        .param("type", "certificateauthority"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("initialData", hasEntry("publicKeySize", "ML-DSA-87")));
    }

    /**
     * The ML-DSA-87 CA verifies the platform certificate's signature, so the platform certificate's chain is complete.
     *
     * @throws Exception if a certificate cannot be read
     */
    @Test
    public void testMlDsaCaIssuedPlatformCertificate() throws Exception {
        assertEquals("", platformCredential.isIssuer(testCa));
        assertEquals("ML-DSA-87", platformCredential.getSignatureAlgorithm());
    }

    /**
     * The evidence page lists the checks and the downloads.
     *
     * @throws Exception if the request fails
     */
    @Test
    public void testEvidencePageListsChecks() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(getPagePath())
                        .param("id", evidenceResult.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("Platform certificate identifies the Device ID certificate")))
                .andExpect(content().string(containsString("Evidence contains the ACA nonce")))
                .andExpect(content().string(containsString("01b5ed98-a5d0-439e-a88b-b8e15625e167")))
                .andExpect(content().string(containsString("deviceid.cer")));
    }

    /**
     * The Device ID certificate downloads as DER.
     *
     * @throws Exception if the request fails
     */
    @Test
    public void testDownloadDeviceIdCertificate() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(getPagePath() + "/download")
                        .param("id", evidenceResult.getId().toString())
                        .param("file", "deviceid"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(deviceId));
    }

    @SuppressWarnings("unchecked")
    private List<LinkStep> linkGraph(final ComponentEvidenceResult result) throws Exception {
        MvcResult page = getMockMvc().perform(MockMvcRequestBuilders.get(getPagePath())
                        .param("id", result.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> data = (Map<String, Object>) page.getModelAndView().getModel().get("initialData");
        return (List<LinkStep>) data.get("linkGraph");
    }

    private byte[] read(final String name) throws IOException, URISyntaxException {
        return Files.readAllBytes(Paths.get(getClass().getResource(RESOURCES + name).toURI()));
    }
}
