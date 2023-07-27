package hirs.attestationca.portal.page;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base class for PageController tests.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class PageControllerTest {

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
//    @BeforeMethod
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
}