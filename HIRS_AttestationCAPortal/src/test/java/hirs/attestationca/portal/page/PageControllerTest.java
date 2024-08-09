package hirs.attestationca.portal.page;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.equalTo;


/**
 * Base class for PageController tests.
 *
 */

@SpringBootTest
@ContextConfiguration(classes = PageTestConfiguration.class) // configuration class for db setup
@ActiveProfiles("test")                                      // sets profile for test to allow bean overriding
@TestInstance(TestInstance.Lifecycle.PER_CLASS)              // needed to use non-static BeforeAll
public abstract class PageControllerTest {

    // Contains server-side support for testing Spring MVC applications
    // via WebTestClient with MockMvc for server request handling.
    @Autowired
    private WebApplicationContext webApplicationContext;

    // Used to set up mocked servlet environment to test the HTTP controller
    // endpoints without the need to launch the embedded servlet container.
    private MockMvc mockMvc;

    // Represents the Page for the Controller under test.
    private final Page page;

    // Pre-prefix path for all the Controllers.
    // There's an option in Page to add prefix path used for some Controllers.
    private static final String PRE_PREFIX_PATH = "/HIRS_AttestationCAPortal/portal/";

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
     * Returns a blank model for initPage tests.
     *
     * @return a blank model for initPage tests.
     */
    protected static final Model getBlankModel() {
        return new ExtendedModelMap();
    }

    /**
     * If the AssertionError is a redirected URL error, check the results of the executed request
     * for the actual redirected URL and throw a new error containing the comparison to the expected
     * URL.
     *
     * If the error is not a redirected URL error, rethrow the original error.
     *
     * @param expectedURL the expected redirected URL AntMatcher pattern
     * @param actions the results of the executed request
     * @param err the AssertionError to indicate if the error is a redirected URL error
     * @throws AssertionError with added information if a redirected URL error or the original error
     */
    protected static final void enhanceRedirectedUrlError(
            final String expectedURL,
            final ResultActions actions,
            final AssertionError err) throws AssertionError {
        if ("Redirected URL".equals(err.getMessage())) {
            final String actualURL = actions.andReturn().getResponse().getRedirectedUrl();
            final String msg
                    = err.getMessage() + ": "
                    + " expected [" + expectedURL + "]"
                    + " but found [" + actualURL + "]";
            throw new AssertionError(msg);
        } else {
            throw err;
        }
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

        return getTestCertificate(certificateClass, filename, null, null);
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
            final List<PlatformCredential> platformCredentials)
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
            case "CertificateAuthorityCredential":
                return new CertificateAuthorityCredential(fPath);
            case "IssuedAttestationCertificate":
                return new IssuedAttestationCertificate(fPath,
                        endorsementCredential, platformCredentials, false);
            default:
                throw new IllegalArgumentException(
                        String.format("Unknown certificate class %s", certificateClass.getName())
                );
        }
    }

    /**
     * Create page path (add pre-prefix and prefix path)
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

        // Add prefix path for page verification
        String pagePath = PRE_PREFIX_PATH + page.getPrefixPath() + getPage().getViewName();
        if (page.getPrefixPath() == null) {
            pagePath = PRE_PREFIX_PATH + getPage().getViewName();
        }

        getMockMvc()
                .perform(MockMvcRequestBuilders.get(pagePath))
                .andExpect(status().isOk())
                .andExpect(view().name(page.getViewName()))
                .andExpect(forwardedUrl("/WEB-INF/jsp/" + page.getViewName() + ".jsp"))
                .andExpect(model().attribute(PageController.PAGE_ATTRIBUTE, equalTo(page)))
                .andExpect(model().attribute(
                        PageController.PAGES_ATTRIBUTE, equalTo(Page.values()))
                );
    }
}