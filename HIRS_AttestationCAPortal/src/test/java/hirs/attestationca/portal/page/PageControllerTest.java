package hirs.attestationca.portal.page;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
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

    /**
     * Pre-prefix path for all the Controllers.
     * There's an option in Page to add prefix path used for some Controllers.
     */
    private String prePrefixPath = "HIRS_AttestationCAPortal/portal/";

    /**
     * Contains server-side support for testing Spring MVC applications
     * via WebTestClient with MockMvc for server request handling.
     */
    @Autowired
    private WebApplicationContext webApplicationContext;

    /**
     * Used to set up mocked servlet environment to test the HTTP controller
     * endpoints without the need to launch the embedded servlet container.
     */
    private MockMvc mockMvc;

    /**
     * Represents the Page for the Controller under test.
     */
    private final Page page;

    /**
     * Constructor requiring the Page's display and routing specification.
     *
     * @param page The page specification for this controller.
     */
    public PageControllerTest(final Page page) {
        this.page = page;
    }

    /**
     * Returns the Page's pre-prePrefix path for routing.
     *
     * @return the Page's pre-prePrefix path
     */
    public String getPrePrefixPath() {
        return prePrefixPath;
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
        String pagePath = "/" + prePrefixPath + page.getPrefixPath() + getPage().getViewName();
        if (page.getPrefixPath() == null) {
            pagePath = "/" + prePrefixPath + getPage().getViewName();
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

