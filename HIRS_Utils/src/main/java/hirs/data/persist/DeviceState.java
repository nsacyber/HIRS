package hirs.data.persist;

import org.hibernate.criterion.Criterion;

/**
 * <code>DeviceState</code> classes are used to contain information about the <code>Device</code>.
 * The information contained in <code>DeviceState</code>s is updated during appraisal.
 */
public abstract class DeviceState {

    /**
     * Constructs a Criterion object that can be used to retrieve alerts that are relevant to the
     * device health. The default is null, which means no special treatment should be given to that
     * particular type of state. This should be overridden for IMADeviceState for delta reports,
     * where this method should construct a criterion specific to the first delta report in the
     * most recent series.
     *
     * @return Criterion or null if not specific Criterion needed
     */
    public Criterion getDeviceTrustAlertCriterion() {
        return null;
    }
}
