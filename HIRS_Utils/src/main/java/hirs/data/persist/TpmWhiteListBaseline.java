package hirs.data.persist;

import javax.persistence.Entity;

/**
 * A {@link TPMBaseline} that represents expected TPM measurement values.
 * If a device provides a TPM Report that includes TPM measurements not matching the whitelist
 * in its TPM Policy, an alert will be generated.
 */
@Entity
public class TpmWhiteListBaseline extends TPMBaseline {

    /**
     * Creates a new baseline with no valid PCR entries and no device-specific PCRs.
     *
     * @param name
     *            a name used to uniquely identify and reference the PCR baseline
     */
    public TpmWhiteListBaseline(final String name) {
        super(name);
    }

    /**
     * Default constructor necessary for Hibernate and BaselineAlertResolver.
     */
    public TpmWhiteListBaseline() {
        super();
    }
}
