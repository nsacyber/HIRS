package hirs.attestationca.portal.page;

import static org.hamcrest.Matchers.equalTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.context.WebApplicationContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Base class for PageController tests.
 */

@WebAppConfiguration
@ContextConfiguration(classes = PageTestConfiguration.class)
public abstract class PageControllerTest extends AbstractTestNGSpringContextTests {

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

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
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
    @BeforeMethod
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
    public final void doTestPageExists() throws Exception {
        // Add prefix path for page verification
        String pagePath = "/" + page.getPrefixPath() + page.getViewName();
        if (page.getPrefixPath() == null) {
            pagePath = "/" + page.getViewName();
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
