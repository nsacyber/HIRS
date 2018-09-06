package hirs.appraiser;

import javax.persistence.Entity;

/**
 * An <code>IMAAppraiser</code> is an <code>Appraiser</code> that can appraise Integrity Measurement
 * Architecture (IMA) reports. See http://linux-ima.sourceforge.net/ for more details on IMA.
 */
@Entity
public class IMAAppraiser extends Appraiser {

    /**
     * Name set for every instance of <code>IMAAppraiser</code>.
     */
    public static final String NAME = "IMA Appraiser";

    /**
     * Creates a new <code>IMAAppraiser</code>. The name is set to {@link #NAME}. The device manager
     * and state manager are set to null.
     */
    public IMAAppraiser() {
        super(NAME);
    }

}
