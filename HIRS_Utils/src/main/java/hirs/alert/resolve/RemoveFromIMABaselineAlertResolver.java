package hirs.alert.resolve;

import hirs.data.persist.Alert;
import hirs.data.persist.IMABaselineRecord;
import org.springframework.stereotype.Component;

/**
 * Resolves alerts by adding a record to an IMA baseline.
 */
@Component
public class RemoveFromIMABaselineAlertResolver extends IMABaselineAlertResolver {

    /**
     * Resolves alerts by adding a record to an IMA baseline.
     *
     * @param alert the alert to resolve
     * @return true if the alert is successfully resolved
     */
    @Override
    public boolean resolve(final Alert alert) {

        IMABaselineRecord find = parseImaRecord(alert, alert.getExpected());
        find.setBaselineForRecordManager(imaBaseline);
        IMABaselineRecord found = imaBaselineRecordManager.getRecord(
                find.getPath(), find.getHash(), imaBaseline);

        if (found == null) {
            addError("Could not find IMA Baseline Record for alert " + "[" + alert.getId() + "].");
            return false;
        }

        if (!imaBaselineRecordManager.deleteRecord(found)) {
            addError("Could not delete IMA Baseline Record for alert "
                    + "[" + alert.getId() + "].");
            return false;
        }

        return true;

    }

}
