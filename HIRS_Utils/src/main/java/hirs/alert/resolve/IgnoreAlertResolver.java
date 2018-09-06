package hirs.alert.resolve;

import hirs.data.persist.Alert;
import org.springframework.stereotype.Component;

/**
 * Ignores alerts by always indicating them as successfully resolved.
 */
@Component
public class IgnoreAlertResolver extends AlertResolver {

    /**
     * Returns true to indicate that AlertResolver should mark the alert as resolved.
     *
     * @param alert the alert to ignore
     * @return true
     */
    @Override
    public boolean resolve(final Alert alert) {
        return true;
    }

}
