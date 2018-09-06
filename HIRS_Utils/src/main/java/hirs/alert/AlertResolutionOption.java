package hirs.alert;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * A serializable response for a possible alert resolution option.
 */
@SuppressFBWarnings("URF_UNREAD_FIELD")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AlertResolutionOption {

    // the operation that will be attempted if this option is executed
    private final AlertResolutionAction action;

    // why the chosen option is provided
    private final String reason;

    // the names of the baselines that can be affected by this option
    private List<String> whitelistNames;
    private List<String> requiredSetNames;
    private List<String> ignoreSetNames;
    private List<String> tpmBaselineNames;

    /**
     * Create a new <code>AlertResolutionOption</code>.
     *
     * @param action to take if this option is chosen
     * @param reason why the chosen option is provided
     */
    public AlertResolutionOption(final AlertResolutionAction action, final String reason) {
        this.action = action;
        this.reason = reason;
    }

    /**
     * Set the names of whitelists that can be edited by choosing this option.
     *
     * @param whitelistNames that can be edited
     */
    public void setWhitelistNames(final List<String> whitelistNames) {
        this.whitelistNames = whitelistNames;
    }

    /**
     * Set the names of required sets that can be edited by choosing this option.
     *
     * @param requiredSetNames that can be edited
     */
    public void setRequiredSetNames(final List<String> requiredSetNames) {
        this.requiredSetNames = requiredSetNames;
    }

    /**
     * Set the names of ignore sets that can be edited by choosing this option.
     *
     * @param ignoreSetNames that can be edited
     */
    public void setIgnoreSetNames(final List<String> ignoreSetNames) {
        this.ignoreSetNames = ignoreSetNames;
    }

    /**
     * Set the names of TPM baselines that can be edited by choosing this option.
     *
     * @param tpmBaselineNames that can be edited
     */
    public void setTpmBaselineNames(final List<String> tpmBaselineNames) {
        this.tpmBaselineNames = tpmBaselineNames;
    }

    /**
     * Checks if the specified action matches this option's action.
     *
     * @param action the action to check
     * @return true if the specified action matches this option's action
     */
    public boolean hasAction(final AlertResolutionAction action) {
        return action == this.action;
    }

}
