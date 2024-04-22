package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.enums.HealthStatus;
import hirs.attestationca.portal.page.PageControllerTest;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
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

    // Base path for the page
    private String pagePath;

    // Fake device to store in db for test
    private Device device;

    // Repository manager to handle data access between device entity and data storage in db
    @Autowired
    private DeviceRepository deviceRepository;

    // Repository manager to handle data access between certificate entity and data storage in db
    @Autowired
    private CertificateRepository certificateRepository;

    // Location of test certs
    private static final String TEST_ENDORSEMENT_CREDENTIAL
            = "/endorsement_credentials/tpmcert.pem";
    private static final String TEST_PLATFORM_CREDENTIAL
            = "/platform_credentials/Intel_pc.cer";
    private static final String TEST_PLATFORM_CREDENTIAL_2
            = "/platform_credentials/Intel_pc2.pem";
    private static final String ISSUED_CLIENT_CERT
            = "/certificates/sample_identity_cert.cer";

    // Certs objects
    private List<PlatformCredential> platformCredentialList;
    private IssuedAttestationCertificate issued;


    /**
     * Constructor providing the Page's display and routing specification.
     */
    public IssuedCertificatesPageControllerTest() {
        super(ISSUED_CERTIFICATES);
        pagePath = getPagePath();
    }

    /**
     * Prepares a testing environment.
     * @throws IOException if there is a problem constructing the test certificate
     */
    @BeforeAll
    public void beforeMethod() throws IOException {

        // Create new device to be used in test and save it to db
        device = new Device("Test Device",null, HealthStatus.TRUSTED, AppraisalStatus.Status.PASS,
                null,false,"temp", "temp");
        device = deviceRepository.save(device);

        // Upload and save EK Cert
        EndorsementCredential ec = (EndorsementCredential)
                getTestCertificate(
                        EndorsementCredential.class,
                        TEST_ENDORSEMENT_CREDENTIAL,
                        null,
                        null);
        ec.setDeviceId(device.getId());
        certificateRepository.save(ec);

        //Set up multi-platform cert Attestation Cert
        platformCredentialList = new LinkedList<>();

        //Upload and save Platform Cert, add it to platformCredentials list
        PlatformCredential pc = (PlatformCredential)
                getTestCertificate(
                        PlatformCredential.class,
                        TEST_PLATFORM_CREDENTIAL,
                        null,
                        null);
        pc.setDeviceId(device.getId());
        certificateRepository.save(pc);
        platformCredentialList.add(pc);

        //Upload and save a second Platform Cert, add it to platformCredentials list
        pc = (PlatformCredential)
                getTestCertificate(
                        PlatformCredential.class,
                        TEST_PLATFORM_CREDENTIAL_2,
                        null,
                        null);
        pc.setDeviceId(device.getId());
        certificateRepository.save(pc);
        platformCredentialList.add(pc);

        //Upload and save Issued Attestation Cert
        issued = (IssuedAttestationCertificate)
                getTestCertificate(
                        IssuedAttestationCertificate.class,
                        ISSUED_CLIENT_CERT,
                        ec,
                        platformCredentialList);
        issued.setDeviceId(device.getId());
        certificateRepository.save(issued);

    }

    /**
     * Tests retrieving the issued-certificates list using a mocked certificate repository.
     *
     * @throws Exception if test fails
     */
    @Test
    @Rollback
    public void getIssuedCertsList() throws Exception {

        // perform test
        getMockMvc().perform(MockMvcRequestBuilders.get(pagePath + "/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].platformCredentials",
                        hasSize(platformCredentialList.size())))
                .andReturn();

    }

    /**
     * Tests downloading the certificate.
     * @throws Exception when getting raw report
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
                                pagePath + "/download")
                        .param("id", issued.getId().toString())
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string("Content-Disposition",
                        fileName.toString()))
                .andExpect(content().bytes(issued.getRawBytes()));

    }
}
