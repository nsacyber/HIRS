package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.enums.HealthStatus;
import hirs.attestationca.portal.page.PageControllerTest;
import hirs.attestationca.persist.entity.userdefined.Device;
import static hirs.attestationca.portal.page.Page.DEVICES;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests that test the URL End Points of DevicePageController.
 */
public class DevicePageControllerTest extends PageControllerTest {

    private static final String DEVICE_NAME = "Test Device";
    private static final String TEST_ENDORSEMENT_CREDENTIAL
            = "/endorsement_credentials/tpmcert.pem";
    private static final String TEST_ENDORSEMENT_CREDENTIAL_2
            = "/endorsement_credentials/ab21ccf2-tpmcert.pem";
    private static final String TEST_PLATFORM_CREDENTIAL
            = "/platform_credentials/Intel_pc.cer";

    private Device device;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private CertificateRepository certificateRepository;


    /**
     * Constructor providing the Page's display and routing specification.
     */
    public DevicePageControllerTest() { super(DEVICES); }

    /**
     * Prepares a testing environment.
     * @throws IOException if there is a problem constructing the test certificate
     */
    @BeforeAll
    public void beforeMethod() throws IOException {

        device = new Device(DEVICE_NAME,null, HealthStatus.TRUSTED, AppraisalStatus.Status.PASS,null,false,"tmp_overrideReason", "tmp_summId");
        device = deviceRepository.save(device);

        //Upload and save EK Cert
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

    /**
     * Tests retrieving the device list using a mocked device manager.
     *
     * @throws Exception if test fails
     */
    @Test
    public void getDeviceList() throws Exception {

        // Add prefix path for page verification
        String pagePath = "/" + getPrePrefixPath() + getPage().getPrefixPath() + getPage().getViewName() + "/list";
        if (getPage().getPrefixPath() == null) {
            pagePath = "/" + getPrePrefixPath() + getPage().getViewName() + "/list";
        }

        // perform test
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andReturn()
        ;
    }

}


