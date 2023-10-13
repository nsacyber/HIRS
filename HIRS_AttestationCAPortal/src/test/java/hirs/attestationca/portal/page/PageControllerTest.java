package hirs.attestationca.portal.page;

//import hirs.attestationca.portal.PersistenceJPAConfig;
import hirs.attestationca.portal.HIRSApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
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

//@EnableAutoConfiguration(exclude= PersistenceJPAConfig.class)
//@ComponentScan(basePackages = {"hirs.attestationca.portal"},
//        excludeFilters = {@ComponentScan.Filter(
//                type = FilterType.ASSIGNABLE_TYPE,
//                value = {PersistenceJPAConfig.class})})
//@ComponentScan(excludeFilters={@ComponentScan.Filter(
//                type= FilterType.ASSIGNABLE_TYPE,
//                value=PersistenceJPAConfig.class)})
//@ComponentScan(basePackages = {"hirs.attestationca.portal"},
//        excludeFilters = {@ComponentScan.Filter(
//                type = FilterType.ASSIGNABLE_TYPE,
//                classes = {PersistenceJPAConfig.class})})
//@ComponentScan(excludeFilters  = {@ComponentScan.Filter(
//                type = FilterType.ASSIGNABLE_TYPE,
//                classes = {PersistenceJPAConfig.class})})
//@ComponentScan(basePackages = {"hirs"},
//        excludeFilters = {@ComponentScan.Filter(
//                type = FilterType.ASPECTJ,
//                classes = {PersistenceJPAConfig.class})})
//@WebAppConfiguration
//@ExtendWith(SpringExtension.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes={ PageTestConfiguration.class})
//@SpringBootTest(classes={ PageTestConfiguration.class})
@SpringBootTest
//@Profile("test")
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)  // needed to use non-static BeforeAll
@ContextConfiguration(classes = PageTestConfiguration.class)
//@ContextConfiguration(classes = {HIRSApplication.class, PageTestConfiguration.class})
public abstract class PageControllerTest {

    private String prePrefixPath = "HIRS_AttestationCAPortal/portal/";

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

