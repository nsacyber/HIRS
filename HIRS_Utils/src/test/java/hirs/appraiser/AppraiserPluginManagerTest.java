package hirs.appraiser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;

import java.util.List;

/**
 * Unit tests for {@link AppraiserPluginManager}.
 *
 */
//@Test
//@ContextConfiguration(locations = { "classpath:spring-test-config.xml" })
//@TestPropertySource(locations = "classpath:collector.test.properties")
public class AppraiserPluginManagerTest extends AbstractTestNGSpringContextTests {
    @Autowired
    private AppraiserPluginManager appraiserPluginManager;

    /**
     * Tests that the plugin manager is populated with plugins using a valid spring configuration.
     * file.
     */
    //@Test
    public void pluginListPopulatedUsingSpringInjection() {
        Assert.assertNotNull(appraiserPluginManager,
                "Verify spring is configured to autowire the AppraiserPluginManager");
        List<AppraiserPlugin> pluginList = appraiserPluginManager.getAppraisers();

        Assert.assertNotNull(pluginList);
        Assert.assertEquals(pluginList.size(), 1, "Unexpected plugin count in list");
        Assert.assertTrue(pluginList.get(0) instanceof TestAppraiserPlugin,
                "First plugin in list was not a " + TestAppraiserPlugin.class);
    }

    /**
     * Test representing when there are no plugins to be injected by spring, that the list
     * of plugins returned is empty.
     */
    //@Test
    public void pluginListEmptyWithoutInjectedPlugins() {
        AppraiserPluginManager emptyManager = new AppraiserPluginManager();
        Assert.assertTrue(emptyManager.getAppraisers().isEmpty(),
                "collector list should be empty with no configured plugins");
    }
}
