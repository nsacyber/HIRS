package hirs.appraiser;

import hirs.data.persist.Policy;
import org.springframework.stereotype.Component;

/**
 * This is a class used to help test the {@link AppraiserPluginManager} and related functionality.
 */
@Component
public class TestAppraiserPlugin extends AppraiserPlugin {

    @Override
    public Policy getDefaultPolicy() {
        return null;
    }


}
