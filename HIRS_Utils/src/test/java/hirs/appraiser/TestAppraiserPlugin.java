package hirs.appraiser;

import hirs.data.persist.policy.Policy;
import org.springframework.stereotype.Component;

/**
 * This is a class used to help test the {@link AppraiserPluginManager} and related functionality.
 */
@Component
public class TestAppraiserPlugin extends AppraiserPlugin {
    /**
     * Appraiser objects must setName.
     */
    public TestAppraiserPlugin() {
        setName("TestAppraiserPlugin");
    }

    @Override
    public Policy getDefaultPolicy() {
        return null;
    }


}
