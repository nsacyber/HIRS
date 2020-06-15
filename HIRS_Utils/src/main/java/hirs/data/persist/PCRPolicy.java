package hirs.data.persist;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public final class PCRPolicy extends Policy {

    // PCR 10
    private static final int IMA_PCR = 9;
    // PCR 17-19
    private static final int TBOOT_PCR = 16;
    private static final int NUM_OF_IMA_PCR = 1;
    private static final int NUM_OF_TBOOT_PCR = 3;

    @Column(nullable = false)
    private boolean ignoreIma = false;
    @Column(nullable = false)
    private boolean ignoretBoot = false;
    @Column(nullable = false)
    private boolean linuxOs = false;

    private String[] baselinePcrs;

    public PCRPolicy() {
        baselinePcrs = new String[TPMMeasurementRecord.MAX_PCR_ID];
    }

    /**
     * Constructor to parse PCR values
     *
     * @param pcrValues RIM provided baseline PCRs
     */
    public PCRPolicy(final String[] pcrValues) {
        for (int i = 0; i <= TPMMeasurementRecord.MAX_PCR_ID; i++) {
            baselinePcrs[i] = pcrValues[i];
        }
    }

    public String validatePcrs(final String quotePcrs[]) {
        String failureMsg = "Firmware validation failed: PCR %d does not"
                + " match%n";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i <= TPMMeasurementRecord.MAX_PCR_ID; i++) {
            if (ignoreIma && i == IMA_PCR) {
                i += NUM_OF_IMA_PCR;
            }

            if (ignoretBoot && i == TBOOT_PCR) {
                i += NUM_OF_TBOOT_PCR;
            }

            if (!baselinePcrs[i].equals(quotePcrs[i])) {
                sb.append(String.format(failureMsg, i));
            }
        }

        return sb.toString();
    }

    public boolean isIgnoreIma() {
        return ignoreIma;
    }

    public void setIgnoreIma(final boolean ignoreIma) {
        this.ignoreIma = ignoreIma;
    }

    public boolean isIgnoretBoot() {
        return ignoretBoot;
    }

    public void setIgnoretBoot(final boolean ignoretBoot) {
        this.ignoretBoot = ignoretBoot;
    }

    public boolean isLinuxOs() {
        return linuxOs;
    }

    public void setLinuxOs(final boolean linuxOs) {
        this.linuxOs = linuxOs;
    }
}
