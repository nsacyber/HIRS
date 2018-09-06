package hirs.alert.resolve;

import hirs.data.persist.Alert;
import hirs.data.persist.TPMMeasurementRecord;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Resolves alerts by removing a record from a TPM Baseline.
 */
@Component
public class RemoveFromTPMBaselineAlertResolver extends TPMBaselineAlertResolver {

    /**
     * Resolves alerts by removing a record from a TPM Baseline.
     * @param alert the alert to resolve
     * @return true if the alert was successfully resolved
     */
    @Override
    public boolean resolve(final Alert alert) {
        Set<TPMMeasurementRecord> records = parseTpmRecords(alert, alert.getExpected());
        if (records == null) {
            return false;
        } else {
            for (TPMMeasurementRecord record : records) {
                tpmBaseline.removeFromBaseline(record);
            }
            return true;
        }
    }

}
