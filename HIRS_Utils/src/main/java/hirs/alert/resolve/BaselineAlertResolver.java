package hirs.alert.resolve;

import hirs.alert.AlertResolutionAction;
import hirs.data.persist.Baseline;
import hirs.persist.BaselineManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class for resolutions that involve the modification of baselines.
 */
public abstract class BaselineAlertResolver extends AlertResolver {

    private static final Logger LOGGER = LogManager.getLogger(AlertResolver.class);

    /**
     * The baseline manager used to load and save baselines.
     */
    @Autowired
    @SuppressWarnings("checkstyle:visibilitymodifier")
    protected BaselineManager baselineManager;

    /**
     * The baseline to be modified by this resolver.
     */
    @SuppressWarnings("checkstyle:visibilitymodifier")
    protected Baseline baseline;

    /**
     * Checks the baseline name and loads and checks the baseline or creates a new baseline.
     *
     * @return true if successful
     */
    @Override
    protected boolean beforeLoop() {

        final AlertResolutionAction action = getRequest().getAction();

        // get baseline name
        final String name = getRequest().getBaselineName();
        if (StringUtils.isBlank(name)) {
            addError("No baseline name was specified.");
            return false;
        }

        // add baseline name to default reason
        if (reason != null && reason.equals(action.getDefaultReason())) {
            reason += " '" + name + "'";
        }

        // get baseline type
        final Class<? extends Baseline> type = action.getBaselineType();

        // get or create baseline
        baseline = baselineManager.getBaseline(name);
        if (baseline != null) {

            // check type
            if (!action.canResolve(baseline)) {
                addError("Baseline '" + name + "' is a "
                        + baseline.getClass().getSimpleName() + ", but was expected to be a "
                        + type.getSimpleName() + ".");
                return false;
            }

        } else if (action.name().startsWith("ADD_TO")) {

            // create new baseline
            try {
                baseline = type.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                String msg = "Error creating new baseline named '" + name + "'.";
                LOGGER.error(msg, ex);
                addError(msg);
                return false;
            }

            baseline.setName(name);
            baselineManager.saveBaseline(baseline);

        } else {
            addError("Could not find baseline named '" + name + "'.");
        }

        return true;

    }

}
