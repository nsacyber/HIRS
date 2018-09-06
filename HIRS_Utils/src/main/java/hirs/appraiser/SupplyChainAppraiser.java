package hirs.appraiser;

import javax.persistence.Entity;

/**
 * A <code>SupplyChainAppraiser</code> is responsible for facilitating the verification of supply
 * chain attributes such as TPM Endorsement Credential and Platform Credential during TPM
 * provisioning. This class facilitates a pairing with a SupplyChainPolicy to help specify supply
 * chain verification behavior.
 */
@Entity
public class SupplyChainAppraiser extends Appraiser {
    /**
     * Name set for every instance of <code>SupplyChainAppraiser</code>.
     */
    public static final String NAME = "Supply Chain Appraiser";

    /**
     * Creates a new <code>SupplyChainAppraiser</code>. The name is set to {@link #NAME}.
     */
    public SupplyChainAppraiser() {
        super(NAME);
    }
}
