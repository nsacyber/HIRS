package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.enums.HealthStatus;
import hirs.attestationca.portal.page.PageControllerTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;

import static hirs.attestationca.portal.page.Page.DEVICES;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that test the URL End Points of DevicePageController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DevicePageControllerTest extends PageControllerTest {

    // Location of test certs
    private static final String TEST_ENDORSEMENT_CREDENTIAL
            = "/endorsement_credentials/tpmcert.pem";
    private static final String TEST_ENDORSEMENT_CREDENTIAL_2
            = "/endorsement_credentials/ab21ccf2-tpmcert.pem";
    private static final String TEST_PLATFORM_CREDENTIAL
            = "/platform_credentials/Intel_pc.cer";
    // Base path for the page
    private final String pagePath;
    // Repository manager to handle data access between device entity and data storage in db
    @Autowired
    private DeviceRepository deviceRepository;
    // Repository manager to handle data access between certificate entity and data storage in db
    @Autowired
    private CertificateRepository certificateRepository;


    /**
     * Constructor providing the Page's display and routing specification.
     */
    public DevicePageControllerTest() {
        super(DEVICES);
        pagePath = getPagePath();
    }

    /**
     * Prepares a testing environment.
     *
     * @throws IOException if there is a problem constructing the test certificate
     */
    @BeforeAll
    public void prepareTests() throws IOException {

        // Fake device to store in db for test
        Device device;

        // Create new device to be used in test and save it to db
        device = new Device("Test Device", null, HealthStatus.TRUSTED, AppraisalStatus.Status.PASS,
                null, false, "tmp_overrideReason", "tmp_summId");
        device = deviceRepository.save(device);

        // Upload and save EK Cert
        EndorsementCredential ec = (EndorsementCredential)
                getTestCertificate(EndorsementCredential.class,
                        TEST_ENDORSEMENT_CREDENTIAL);
        ec.setDeviceId(device.getId());
        certificateRepository.save(ec);

        //Add second EK Cert without a device
        ec = (EndorsementCredential)
                getTestCertificate(EndorsementCredential.class, TEST_ENDORSEMENT_CREDENTIAL_2);
        certificateRepository.save(ec);

        //Upload and save Platform Cert
        PlatformCredential pc = (PlatformCredential)
                getTestCertificate(PlatformCredential.class, TEST_PLATFORM_CREDENTIAL);
        pc.setDeviceId(device.getId());
        certificateRepository.save(pc);
    }

    /**
     * Tests retrieving the device list using a mocked device repository.
     *
     * @throws Exception if test fails
     */
    @Test
    public void getDeviceList() throws Exception {

        // perform test
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath + "/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andReturn();
    }

}
