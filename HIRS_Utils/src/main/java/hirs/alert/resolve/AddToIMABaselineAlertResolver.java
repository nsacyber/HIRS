package hirs.alert.resolve;

import hirs.data.persist.Alert;
import hirs.data.persist.baseline.IMABaselineRecord;
import org.springframework.stereotype.Component;

/**
 * Resolves alerts by adding a record to an IMA Baseline.
 */
@Component
public class AddToIMABaselineAlertResolver extends IMABaselineAlertResolver {

    @Override
    public boolean resolve(final Alert alert) {
        IMABaselineRecord record = parseImaRecord(alert, alert.getReceived());
        if (record == null) {
            return false;
        } else {
            record.setBaselineForRecordManager(imaBaseline);
            imaBaselineRecordManager.saveRecord(record);
            return true;
        }
    }

}
