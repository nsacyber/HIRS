package hirs.data.persist;

import javax.persistence.Entity;

/**
 * A {@link TPMBaseline} that represents TPM measurement values that are not allowed in the system.
 * If a device provides a TPM Report that includes TPM measurements matching a blacklist in its
 * TPM Policy, an alert will be generated.
 */
@Entity
public class TpmBlackListBaseline extends TPMBaseline {

    /**
     * Creates a new baseline with no PCR.
     *
     * @param name
     *            a name used to uniquely identify and reference the PCR baseline
     */
    public TpmBlackListBaseline(final String name) {
        super(name);
    }

    /**
     * Default constructor necessary for Hibernate and BaselineAlertResolver.
     */
    public TpmBlackListBaseline() {
        super();
    }
}
