package hirs.appraiser;

import hirs.data.persist.policy.Policy;
import org.springframework.plugin.core.Plugin;

/**
 * This abstract class defines the functionality that an Appraiser Spring Plugin should implement.
 * All implementing classes must extend this class and be persisted with Hibernate.
 * <p>
 * The Appraiser abstract class defines the basic functionality of an Appraiser.  This extending
 * class is used to identify plugins that may be developed separately from the HIRS project and
 * included separately at runtime.  This class specifies no additional methods, as all required
 * methods are already specified in Appraiser.
 */
public abstract class AppraiserPlugin extends Appraiser implements Plugin<String> {

    /**
     * If this appraiser needs a default policy set, construct it and return it here.
     *
     * @return the default policy to associate with this appraiser, or null if none
     */
    public abstract Policy getDefaultPolicy();

    @Override
    public boolean supports(final String option) {
        return true;
    }
}
