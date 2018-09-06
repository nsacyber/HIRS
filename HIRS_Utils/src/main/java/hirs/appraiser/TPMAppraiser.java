package hirs.appraiser;

import javax.persistence.Entity;

/**
 * A <code>TPMAAppraiser</code> is an <code>Appraiser</code> that can appraise Trusted Computing
 * Group (TCG) Trusted Platform Module (TPM) reports. See http://www.trustedcomputinggroup.org/ for
 * more details on TCG and TPM standard specifications.
 */
@Entity
public class TPMAppraiser extends Appraiser {

    /**
     * Name set for every instance of <code>TPMAppraiser</code>.
     */
    public static final String NAME = "TPM Appraiser";

    /**
     * Creates a new <code>TPMAppraiser</code>. The name is set to {@link #NAME} .
     */
    public TPMAppraiser() {
        super(NAME);
    }

}
