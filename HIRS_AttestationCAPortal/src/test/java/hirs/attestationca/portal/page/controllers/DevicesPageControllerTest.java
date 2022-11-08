package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.page.PageControllerTest;
import hirs.data.persist.AppraisalStatus;
import hirs.data.persist.Device;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.persist.CertificateManager;
import hirs.persist.DeviceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static hirs.attestationca.portal.page.Page.DEVICES;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that test the URL End Points of DevicePageController.
 */
@WebAppConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DevicesPageControllerTest extends PageControllerTest {

    private static final String DEVICE_NAME = "Test Device";
    private static final String DEVICE_GROUP_NAME = "Test Device Group";
    private static final String TEST_ENDORSEMENT_CREDENTIAL
            = "/endorsement_credentials/tpmcert.pem";
    private static final String TEST_ENDORSEMENT_CREDENTIAL_2
            = "/endorsement_credentials/ab21ccf2-tpmcert.pem";
    private static final String TEST_PLATFORM_CREDENTIAL
            = "/platform_credentials/Intel_pc.cer";

    private Device device;

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private CertificateManager certificateManager;

    /**
     * Constructor providing the Page's display and routing specification.
     */
    public DevicesPageControllerTest() {
        super(DEVICES);
    }

    /**
     * Prepares a testing environment.
     * @throws IOException if there is a problem constructing the test certificate
     */
    @BeforeClass
    public void beforeMethod() throws IOException {
        //Create new device and save it
        device = new Device(DEVICE_NAME);
        device.setSupplyChainStatus(AppraisalStatus.Status.PASS);
        device = deviceManager.saveDevice(device);

        //Upload and save EK Cert
        EndorsementCredential ec = (EndorsementCredential)
                    getTestCertificate(EndorsementCredential.class, TEST_ENDORSEMENT_CREDENTIAL);
        ec.setDevice(device);
        certificateManager.saveCertificate(ec);

        //Add second EK Cert without a device
        ec = (EndorsementCredential)
                    getTestCertificate(EndorsementCredential.class, TEST_ENDORSEMENT_CREDENTIAL_2);
        certificateManager.saveCertificate(ec);

        //Upload and save Platform Cert
        PlatformCredential pc = (PlatformCredential)
                    getTestCertificate(PlatformCredential.class, TEST_PLATFORM_CREDENTIAL);
        pc.setDevice(device);
        certificateManager.saveCertificate(pc);

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
                .get("/devices/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andReturn();
    }

    /**
     * Construct a test certificate from the given parameters.
     * @param <T> the type of Certificate that will be created
     * @param certificateClass the class of certificate to generate
     * @param filename the location of the certificate to be used
     * @return the newly-constructed Certificate
     * @throws IOException if there is a problem constructing the test certificate
     */
    public <T extends Certificate> Certificate getTestCertificate(
            final Class<T> certificateClass,
            final String filename)
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
            default:
                throw new IllegalArgumentException(
                        String.format("Unknown certificate class %s", certificateClass.getName())
                );
        }
    }
}
