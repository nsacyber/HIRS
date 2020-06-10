package hirs.alert.resolve;

import hirs.data.persist.Alert;
import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.baseline.SimpleImaBaseline;
import hirs.persist.ImaBaselineRecordManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class providing common functionality for IMA Baseline alert resolvers.
 */
public abstract class IMABaselineAlertResolver extends BaselineAlertResolver {

    /**
     * The IMA baseline record manager used to load and save IMA baseline records.
     */
    @Autowired
    @SuppressWarnings("checkstyle:visibilitymodifier")
    protected ImaBaselineRecordManager imaBaselineRecordManager;

    /**
     * The IMA baseline to be modified by this resolver.
     */
    @SuppressWarnings("checkstyle:visibilitymodifier")
    protected SimpleImaBaseline imaBaseline = null;

    /**
     * Casts BaselineAlertResolver's baseline to a SimpleImaBaseline.
     *
     * @return true to indicate success
     */
    @Override
    protected boolean beforeLoop() {
        if (super.beforeLoop()) {
            imaBaseline = (SimpleImaBaseline) baseline;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Parses the received/expected value of an {@link Alert} into an {@link IMABaselineRecord}.
     *
     * @param alert the Alert the record is from
     * @param record - received/expected record from Alert
     * @return IMABaselineRecord created from parsing the record String
     */
    protected IMABaselineRecord parseImaRecord(final Alert alert, final String record) {
        try {
            return IMABaselineRecord.fromString(record);
        } catch (Exception ex) {
            String error = "Error parsing IMA record '" + record + "' for alert ["
                    + alert.getId() + "]: " + getMessage(ex);
            addError(error);
            return null;
        }
    }

}
