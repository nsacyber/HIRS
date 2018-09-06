package hirs.alert.resolve;

import hirs.data.persist.Alert;
import hirs.data.persist.TPMMeasurementRecord;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Resolves alerts by adding a record to a TPM Baseline.
 */
@Component
public class AddToTPMBaselineAlertResolver extends TPMBaselineAlertResolver {

    @Override
    public boolean resolve(final Alert alert) {
        Set<TPMMeasurementRecord> records = parseTpmRecords(alert, alert.getReceived());
        if (records == null) {
            return false;
        } else {
            for (TPMMeasurementRecord record : records) {
                tpmBaseline.addToBaseline(record);
            }
            return true;
        }
    }

}
