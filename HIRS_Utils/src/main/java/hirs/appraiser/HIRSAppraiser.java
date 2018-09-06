package hirs.appraiser;

import javax.persistence.Entity;

/**
 * A <code>HIRSAppraiser</code> is responsible for facilitating the appraisal of HIRS supported
 * reports. The <code>HIRSAppraiser</code> will iterate over the collection
 * of supported appraisers when constructing a ReportRequest and when invoking the appraise method
 * of the supported appraisers.
 */
@Entity
public class HIRSAppraiser extends Appraiser {

    /**
     * Name set for every instance of <code>HIRSAppraiser</code>.
     */
    public static final String NAME = "HIRS Appraiser";

    /**
     * Creates a new <code>HIRSAppraiser</code>. The name is set to {@link #NAME}.
     */
    public HIRSAppraiser() {
        super(NAME);
    }

}
