package hirs.appraiser;

import javax.persistence.Entity;

/**
 * A <code>DeviceInfoAppraiser</code> is an <code>Appraiser</code> that can appraise
 * <code>DeviceInfoReport</code>s. For now all appraisals succeed but future versions may implement
 * policy-based appraisals. A <code>DeviceManager</code> is used to save new <code>Device</code>
 * objects, or update existing ones.
 */
@Entity
public class DeviceInfoAppraiser extends Appraiser {

    /**
     * Name set for every instance of <code>DeviceInfoAppraiser</code>.
     */
    public static final String NAME = "Device Info Appraiser";

    /**
     * Default constructor required by Hibernate and <code>HIRSPolicy</code>.
     * <code>DeviceManager</code> is set to null.
     */
    public DeviceInfoAppraiser() {
        super(NAME);
    }

}
