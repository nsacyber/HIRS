package hirs.appraiser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Spring Component that holds the list of {@link AppraiserPlugin}s
 * that are available in the system.
 */
@Component
public class AppraiserPluginManager {

    @Autowired(required = false)
    private List<AppraiserPlugin> appraiserPlugins = new ArrayList<>();

    /**
     * Gets the list of appraiser plugins.
     * @return the configured appraiser plugins
     */
    public List<AppraiserPlugin> getAppraisers() {
        return Collections.unmodifiableList(appraiserPlugins);
    }
}
