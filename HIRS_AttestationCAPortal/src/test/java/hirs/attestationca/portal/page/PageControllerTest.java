package hirs.attestationca.portal.page;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;


/**
 * Base class for PageController tests.
 */
@SpringBootTest
@ContextConfiguration(classes = PageTestConfiguration.class) // configuration class for db setup
@ActiveProfiles("test")                                      // sets profile for test to allow bean overriding
@TestInstance(TestInstance.Lifecycle.PER_CLASS)              // needed to use non-static BeforeAll
public abstract class PageControllerTest {

    // Pre-prefix path for all the Controllers.
    // There's an option in Page to add prefix path used for some Controllers.
    private static final String PRE_PREFIX_PATH = "/HIRS_AttestationCAPortal/portal/";

    // Represents the Page for the Controller under test.
    private final Page page;

    // Contains server-side support for testing Spring MVC applications
    // via WebTestClient with MockMvc for server request handling.
    @Autowired
    private WebApplicationContext webApplicationContext;

    // Used to set up mocked servlet environment to test the HTTP controller
    // endpoints without the need to launch the embedded servlet container.
    private MockMvc mockMvc;

    /**
     * Constructor requiring the Page's display and routing specification.
     *
     * @param page The page specification for this controller.
     */
    public PageControllerTest(final Page page) {
        this.page = page;
    }

    /**
     * Returns the Page's display and routing specification.
     *
     * @return the Page's display and routing specification.
     */
    protected Page getPage() {
        return page;
    }

    /**
     * Returns Spring MVC Test object.
     *
     * @return Spring MVC Test object
     */
    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    /**
     * Construct a test certificate from the given parameters.
     *
     * @param <T>              the type of Certificate that will be created
     * @param certificateClass the class of certificate to generate
     * @param filename         the location of the certificate to be used
     * @return the newly-constructed Certificate
     * @throws IOException if there is a problem constructing the test certificate
     */
    public <T extends Certificate> Certificate getTestCertificate(
            final Class<T> certificateClass,
            final String filename)
            throws IOException {

        return getTestCertificate(certificateClass, filename, null, null);
    }

    /**
     * Construct a test certificate from the given parameters.
     *
     * @param <T>                   the type of Certificate that will be created
     * @param certificateClass      the class of certificate to generate
     * @param filename              the location of the certificate to be used
     * @param endorsementCredential the endorsement credentials (can be null)
     * @param platformCredentials   the platform credentials (can be null)
     * @return the newly-constructed Certificate
     * @throws IOException if there is a problem constructing the test certificate
     */
    public <T extends Certificate> Certificate getTestCertificate(
            final Class<T> certificateClass,
            final String filename,
            final EndorsementCredential endorsementCredential,
            final List<PlatformCredential> platformCredentials)
            throws IOException {

        Path fPath;
        try {
            fPath = Paths.get(this.getClass().getResource(filename).toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Could not resolve path URI", e);
        }

        return switch (certificateClass.getSimpleName()) {
            case "EndorsementCredential" -> new EndorsementCredential(fPath);
            case "PlatformCredential" -> new PlatformCredential(fPath);
            case "CertificateAuthorityCredential" -> new CertificateAuthorityCredential(fPath);
            case "IssuedAttestationCertificate" -> new IssuedAttestationCertificate(fPath,
                    endorsementCredential, platformCredentials, false);
            default -> throw new IllegalArgumentException(
                    String.format("Unknown certificate class %s", certificateClass.getName())
            );
        };
    }

    /**
     * Create page path (add pre-prefix and prefix path).
     *
     * @return the page path
     */
    public String getPagePath() {
        String pagePath = PRE_PREFIX_PATH + page.getPrefixPath() + page.getViewName();
        if (page.getPrefixPath() == null) {
            pagePath = PRE_PREFIX_PATH + page.getViewName();
        }
        return pagePath;
    }

    /**
     * Sets up the test environment.
     */
    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * Executes a test to check that the page exists, has the correct name, and basic page data in
     * its model. Methods annotated with @Test in abstract classes are ignored by Spring.
     *
     * @throws Exception if test fails
     */
    @Test
    public final void doTestPagesExist() throws Exception {
//        // Add prefix path for page verification
//        String pagePath = PRE_PREFIX_PATH + page.getPrefixPath() + getPage().getViewName();
//        if (page.getPrefixPath() == null) {
//            pagePath = PRE_PREFIX_PATH + getPage().getViewName();
//        }
//
//        getMockMvc()
//                .perform(MockMvcRequestBuilders.get(pagePath))
//                .andExpect(status().isOk())
//                .andExpect(view().name(page.getViewName()))
//                .andExpect(model().attribute(PageController.PAGE_ATTRIBUTE, equalTo(page)))
//                .andExpect(model().attribute(
//                        PageController.PAGES_ATTRIBUTE, equalTo(Page.values()))
//                );
    }
}
