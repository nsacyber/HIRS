package hirs.data.persist;

import javax.persistence.Column;
import javax.persistence.Entity;

import static org.apache.logging.log4j.LogManager.getLogger;

import hirs.data.persist.tpm.PcrComposite;
import hirs.data.persist.tpm.PcrInfoShort;
import hirs.data.persist.tpm.PcrSelection;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * The class handles the flags that ignore certain PCRs for validation.
 */
@Entity
public final class PCRPolicy extends Policy {

    private static final Logger LOGGER = getLogger(PCRPolicy.class);

    // PCR 10
    private static final int IMA_PCR = 10;
    // PCR 17-19
    private static final int TBOOT_PCR = 17;
    private static final int NUM_OF_IMA_PCR = 1;
    private static final int NUM_OF_TBOOT_PCR = 3;

    @Column(nullable = false)
    private boolean enableIgnoreIma = false;
    @Column(nullable = false)
    private boolean enableIgnoretBoot = false;
    @Column(nullable = false)
    private boolean linuxOs = false;

    private String[] baselinePcrs;

    /**
     * Default constructor.
     */
    public PCRPolicy() {
        baselinePcrs = new String[TPMMeasurementRecord.MAX_PCR_ID + 1];
    }

    /**
     * Constructor to parse PCR values.
     *
     * @param pcrValues RIM provided baseline PCRs
     */
    public PCRPolicy(final String[] pcrValues) {
        baselinePcrs = new String[TPMMeasurementRecord.MAX_PCR_ID + 1];
        for (int i = 0; i <= TPMMeasurementRecord.MAX_PCR_ID; i++) {
            baselinePcrs[i] = pcrValues[i];
        }
    }

    /**
     * Compares the baseline pcr list and the quote pcr list.  If the
     * ignore flags are set, 10 and 17-19 will be skipped for comparison.
     *
     * @param storedPcrs non-baseline pcr list
     * @return a StringBuilder that is empty if everything passes.
     */
    public StringBuilder validatePcrs(final String[] storedPcrs) {
        StringBuilder sb = new StringBuilder();
        String failureMsg = "PCR %d does not match%n";

        for (int i = 0; i <= TPMMeasurementRecord.MAX_PCR_ID; i++) {
            if (enableIgnoreIma && i == IMA_PCR) {
                LOGGER.info("PCR Policy IMA Ignore enabled.");
                i += NUM_OF_IMA_PCR;
            }

            if (enableIgnoretBoot && i == TBOOT_PCR) {
                LOGGER.info("PCR Policy TBoot Ignore enabled.");
                i += NUM_OF_TBOOT_PCR;
            }

            if (!baselinePcrs[i].equals(storedPcrs[i])) {
                sb.append(String.format(failureMsg, i));
            }
        }

        return sb;
    }

    /**
     * Compares hashs to validate the quote from the client.
     *
     * @param tpmQuote the provided quote
     * @param storedPcrs values from the RIM file
     * @return true if validated, false if not
     */
    public boolean validateQuote(final byte[] tpmQuote, final String[] storedPcrs) {
        LOGGER.info("Validating quote from associated device.");
        boolean validated = false;
        short localityAtRelease = 0;
        String quoteString = new String(tpmQuote, StandardCharsets.UTF_8);

        TPMMeasurementRecord[] measurements = new TPMMeasurementRecord[baselinePcrs.length];
        try {
            for (int i = 0; i <= TPMMeasurementRecord.MAX_PCR_ID; i++) {
                measurements[i] = new TPMMeasurementRecord(i, storedPcrs[i]);
            }
        } catch (DecoderException deEx) {
            LOGGER.error(deEx);
        }
        PcrSelection pcrSelection = new PcrSelection(PcrSelection.ALL_PCRS_ON);
        PcrComposite pcrComposite = new PcrComposite(
                pcrSelection,
                Arrays.asList(measurements));
        PcrInfoShort pcrInfoShort = new PcrInfoShort(pcrSelection,
                localityAtRelease,
                tpmQuote, pcrComposite);

        try {
            /**
             * The calculated string is being used in the contains method
             * because the TPM Quote's hash isn't just for PCR values,
             * it contains the calculated digest of the PCRs, along with
             * other information.
             */
            String calculatedString = Hex.encodeHexString(
                    pcrInfoShort.getCalculatedDigest());
            validated = quoteString.contains(calculatedString);
        } catch (NoSuchAlgorithmException naEx) {
            LOGGER.error(naEx);
        }

        return validated;
    }

    /**
     * Getter for the array of baseline PCRs.
     * @return instance of the PCRs.
     */
    public String[] getBaselinePcrs() {
        return baselinePcrs.clone();
    }

    /**
     * Setter for the array of baseline PCRs.
     * @param baselinePcrs instance of the PCRs.
     */
    public void setBaselinePcrs(final String[] baselinePcrs) {
        this.baselinePcrs = baselinePcrs.clone();
    }

    /**
     * Getter for the IMA ignore flag.
     * @return true if IMA is to be ignored.
     */
    public boolean isEnableIgnoreIma() {
        return enableIgnoreIma;
    }

    /**
     * Setter for the IMA ignore flag.
     * @param enableIgnoreIma true if IMA is to be ignored.
     */
    public void setEnableIgnoreIma(final boolean enableIgnoreIma) {
        this.enableIgnoreIma = enableIgnoreIma;
    }

    /**
     * Getter for the TBoot ignore flag.
     * @return true if TBoot is to be ignored.
     */
    public boolean isEnableIgnoretBoot() {
        return enableIgnoretBoot;
    }

    /**
     * Setter for the TBoot ignore flag.
     * @param enableIgnoretBoot true if TBoot is to be ignored.
     */
    public void setEnableIgnoretBoot(final boolean enableIgnoretBoot) {
        this.enableIgnoretBoot = enableIgnoretBoot;
    }

    /**
     * Getter for a flag to indicate the type of OS.
     * @return true if the system is linux.
     */
    public boolean isLinuxOs() {
        return linuxOs;
    }

    /**
     * Setter for the type of OS.
     * @param linuxOs value of the flag depending on the OS
     */
    public void setLinuxOs(final boolean linuxOs) {
        this.linuxOs = linuxOs;
    }
}
