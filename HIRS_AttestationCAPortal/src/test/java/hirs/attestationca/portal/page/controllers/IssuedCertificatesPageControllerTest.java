package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.page.PageControllerTest;
import hirs.data.persist.Device;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.IssuedAttestationCertificate;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.persist.CertificateManager;
import hirs.persist.DeviceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static hirs.attestationca.portal.page.Page.ISSUED_CERTIFICATES;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that test the URL End Points of IssuedCertificatesPageController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IssuedCertificatesPageControllerTest extends PageControllerTest {

    private static final String TEST_ENDORSEMENT_CREDENTIAL
            = "/endorsement_credentials/tpmcert.pem";
    private static final String TEST_PLATFORM_CREDENTIAL
            = "/platform_credentials/Intel_pc.cer";
    private static final String TEST_PLATFORM_CREDENTIAL_2
            = "/platform_credentials/Intel_pc2.pem";
    private static final String ISSUED_CLIENT_CERT
            = "/certificates/sample_identity_cert.cer";

    private Set<PlatformCredential> platformCredentials;
    private IssuedAttestationCertificate issued;

    private Device device;

    @Autowired
    private DeviceManager deviceManager;


    @Autowired
    private CertificateManager certificateManager;

    /**
     * Prepares a testing environment.
     * @throws IOException if there is a problem constructing the test certificate
     */
    @BeforeClass
    public void beforeMethod() throws IOException {
        //Create new device and save it
        device = new Device("Test");
        device = deviceManager.saveDevice(device);

        //Upload and save EK Cert
        EndorsementCredential ec = (EndorsementCredential)
                    getTestCertificate(
                            EndorsementCredential.class,
                            TEST_ENDORSEMENT_CREDENTIAL,
                            null,
                            null);
        ec.setDevice(device);
        certificateManager.saveCertificate(ec);

        //Set up multi-platform cert Attestation Cert
        platformCredentials = new HashSet<>();

        //Upload and save Platform Cert
        PlatformCredential pc = (PlatformCredential)
                    getTestCertificate(
                            PlatformCredential.class,
                            TEST_PLATFORM_CREDENTIAL,
                            null,
                            null);
        pc.setDevice(device);
        certificateManager.saveCertificate(pc);
        platformCredentials.add(pc);

        pc = (PlatformCredential)
                    getTestCertificate(
                            PlatformCredential.class,
                            TEST_PLATFORM_CREDENTIAL_2,
                            null,
                            null);
        pc.setDevice(device);
        certificateManager.saveCertificate(pc);
        platformCredentials.add(pc);

        issued = (IssuedAttestationCertificate)
                    getTestCertificate(
                            IssuedAttestationCertificate.class,
                            ISSUED_CLIENT_CERT,
                            ec,
                            platformCredentials);
        issued.setDevice(device);
        certificateManager.saveCertificate(issued);

    }

    /**
     * Constructor providing the Page's display and routing specification.
     */
    public IssuedCertificatesPageControllerTest() {
        super(ISSUED_CERTIFICATES);
    }

    /**
     * Tests retrieving the device list using a mocked device manager.
     *
     * @throws Exception if test fails
     */
    @Test
    @Rollback
    public void getDeviceList() throws Exception {
        // perform test
        getMockMvc().perform(MockMvcRequestBuilders
                .get("/certificate-request/issued-certificates/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].platformCredentials",
                            hasSize(platformCredentials.size())))
                .andReturn();
    }

     /**
     * Tests downloading the certificate.
     * @throws java.lang.Exception when getting raw report
     */
    @Test
    @Rollback
    public void testDownloadCert() throws Exception {

        StringBuilder fileName = new StringBuilder("attachment;filename=\"");
        fileName.append("IssuedAttestationCertificate_");
        fileName.append(issued.getSerialNumber());
        fileName.append(".cer\"");

        // verify cert file attachment and content
        getMockMvc()
            .perform(MockMvcRequestBuilders.get(
                        "/certificate-request/issued-certificates/download")
                .param("id", issued.getId().toString())
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/octet-stream"))
            .andExpect(header().string("Content-Disposition",
                    fileName.toString()))
            .andExpect(content().bytes(issued.getRawBytes()));

    }

    /**
     * Construct a test certificate from the given parameters.
     * @param <T> the type of Certificate that will be created
     * @param certificateClass the class of certificate to generate
     * @param filename the location of the certificate to be used
     * @param endorsementCredential the endorsement credentials (can be null)
     * @param platformCredentials the platform credentials (can be null)
     * @return the newly-constructed Certificate
     * @throws IOException if there is a problem constructing the test certificate
     */
    public <T extends Certificate> Certificate getTestCertificate(
            final Class<T> certificateClass,
            final String filename,
            final EndorsementCredential endorsementCredential,
            final Set<PlatformCredential> platformCredentials)
            throws IOException {

        Path fPath;
        try {
            fPath = Paths.get(this.getClass().getResource(filename).toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Could not resolve path URI", e);
        }

        switch (certificateClass.getSimpleName()) {
            case "EndorsementCredential":
                return new EndorsementCredential(fPath);
            case "PlatformCredential":
                return new PlatformCredential(fPath);
            case "IssuedAttestationCertificate":
                return new IssuedAttestationCertificate(fPath,
                        endorsementCredential, platformCredentials);
            default:
                throw new IllegalArgumentException(
                        String.format("Unknown certificate class %s", certificateClass.getName())
                );
        }
    }
}
