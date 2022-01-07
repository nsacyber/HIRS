package hirs.data.persist;

import hirs.data.persist.tpm.PcrComposite;
import hirs.data.persist.tpm.PcrInfoShort;
import hirs.data.persist.tpm.PcrSelection;
import hirs.tpm.eventlog.TCGEventLog;
import hirs.tpm.eventlog.TpmPcrEvent;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * The class handles the flags that ignore certain PCRs for validation.
 */
@Entity
public final class PCRPolicy extends Policy {

    private static final Logger LOGGER = getLogger(PCRPolicy.class);

    private static final int NUM_TO_SKIP = 1;
    private static final int NUM_OF_TBOOT_PCR = 3;
    // PCR 5-16
    private static final int PXE_PCR_START = 5;
    private static final int PXE_PCR_END = 16;
    // PCR 10
    private static final int IMA_PCR = 10;
    // PCR 17-19
    private static final int TBOOT_PCR_START = 17;
    private static final int TBOOT_PCR_END = 19;
    // PCR 5
    private static final int GPT_PCR = 5;
    private static final int IMA_MASK = 0xfffbff;

    // Event Log Event Types
    private static final String EVT_EFI_BOOT = "EV_EFI_BOOT_SERVICES_APPLICATION";
    private static final String EVT_EFI_VAR = "EV_EFI_VARIABLE_BOOT";
    private static final String EVT_EFI_GPT = "EV_EFI_GPT_EVENT";
    private static final String EVT_EFI_CFG = "EV_EFI_VARIABLE_DRIVER_CONFIG";

    @Column(nullable = false)
    private boolean enableIgnoreIma = false;
    @Column(nullable = false)
    private boolean enableIgnoretBoot = false;
    @Column(nullable = false)
    private boolean linuxOs = false;
    @Column(nullable = false)
    private boolean enableIgnoreGpt = true;
    @Column(nullable = false)
    private boolean enableIgnoreOsEvt = false;

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
        if (storedPcrs[0] == null || storedPcrs[0].isEmpty()) {
            sb.append("failureMsg");
        } else {
            for (int i = 0; i <= TPMMeasurementRecord.MAX_PCR_ID; i++) {
                if (enableIgnoreIma && i == IMA_PCR) {
                    LOGGER.info("PCR Policy IMA Ignore enabled.");
                    i += NUM_TO_SKIP;
                }

                if (enableIgnoretBoot && i == TBOOT_PCR_START) {
                    LOGGER.info("PCR Policy TBoot Ignore enabled.");
                    i += NUM_OF_TBOOT_PCR;
                }

                if (enableIgnoreGpt && i == GPT_PCR) {
                    LOGGER.info("PCR Policy GPT Ignore enabled.");
                    i += NUM_TO_SKIP;
                }

                if (!baselinePcrs[i].equals(storedPcrs[i])) {
                    LOGGER.error(String.format("%s =/= %s", baselinePcrs[i], storedPcrs[i]));
                    sb.append(String.format(failureMsg, i));
                }
            }
        }

        return sb;
    }

    /**
     * Checks that the expected FM events occurring. There are policy options that
     * will ignore certin PCRs, Event Types and Event Variables present.
     * @param tcgMeasurementLog Measurement log from the client
     * @param eventValueMap The events stored as baseline to compare
     * @return the events that didn't pass
     */
    public List<TpmPcrEvent> validateTpmEvents(final TCGEventLog tcgMeasurementLog,
                final Map<String, ReferenceDigestValue> eventValueMap) {
        List<TpmPcrEvent> tpmPcrEvents = new LinkedList<>();
        for (TpmPcrEvent tpe : tcgMeasurementLog.getEventList()) {
            if (enableIgnoreIma && tpe.getPcrIndex() == IMA_PCR) {
                LOGGER.info(String.format("IMA Ignored -> %s", tpe));
            } else if (enableIgnoretBoot && (tpe.getPcrIndex() >= TBOOT_PCR_START
                    && tpe.getPcrIndex() <= TBOOT_PCR_END)) {
                LOGGER.info(String.format("TBOOT Ignored -> %s", tpe));
            } else if (enableIgnoreOsEvt && (tpe.getPcrIndex() >= PXE_PCR_START
                    && tpe.getPcrIndex() <= PXE_PCR_END)) {
                LOGGER.info(String.format("OS Evt Ignored -> %s", tpe));
            } else {
                if (enableIgnoreGpt && tpe.getEventTypeStr().contains(EVT_EFI_GPT)) {
                    LOGGER.info(String.format("GPT Ignored -> %s", tpe));
                } else if (enableIgnoreOsEvt && (tpe.getEventTypeStr().contains(EVT_EFI_BOOT)
                        || tpe.getEventTypeStr().contains(EVT_EFI_VAR))) {
                    LOGGER.info(String.format("OS Evt Ignored -> %s", tpe));
                } else if (enableIgnoreOsEvt && (tpe.getEventTypeStr().contains(EVT_EFI_CFG)
                        && tpe.getEventContentStr().contains("SecureBoot"))) {
                    LOGGER.info(String.format("OS Evt Config Ignored -> %s", tpe));
                } else {
                    if (!eventValueMap.containsKey(tpe.getEventDigestStr())) {
                        tpmPcrEvents.add(tpe);
                    }
                }
            }
        }

        return tpmPcrEvents;
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
        int pcrMaskSelection = PcrSelection.ALL_PCRS_ON;

        if (enableIgnoreIma) {
            pcrMaskSelection = IMA_MASK;
        }

        ArrayList<TPMMeasurementRecord> measurements = new ArrayList<>();

        try {
            for (int i = 0; i < storedPcrs.length; i++) {
                if (i == IMA_PCR && enableIgnoreIma) {
                    LOGGER.info("Ignore IMA PCR policy is enabled.");
                } else {
                    measurements.add(new TPMMeasurementRecord(i, storedPcrs[i]));
                }
            }
        } catch (DecoderException deEx) {
            LOGGER.error(deEx);
        }

        PcrSelection pcrSelection = new PcrSelection(pcrMaskSelection);
        PcrComposite pcrComposite = new PcrComposite(
                pcrSelection, measurements);
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
            if (!validated) {
                LOGGER.warn(calculatedString + " not found in " + quoteString);
            }
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
     * Getter for the GPT ignore flag.
     * @return true if GPT is to be ignored.
     */
    public boolean isEnableIgnoreGpt() {
        return enableIgnoreGpt;
    }

    /**
     * Setter for the GPT ignore flag.
     * @param enableIgnoreGpt true if GPT is to be ignored.
     */
    public void setEnableIgnoreGpt(final boolean enableIgnoreGpt) {
        this.enableIgnoreGpt = enableIgnoreGpt;
    }

    /**
     * Getter for the Os Events ignore flag.
     * @return true if Os Events is to be ignored.
     */
    public boolean isEnableIgnoreOsEvt() {
        return enableIgnoreOsEvt;
    }

    /**
     * Setter for the Os Evt ignore flag.
     * @param enableIgnoreOsEvt true if Os Evt is to be ignored.
     */
    public void setEnableIgnoreOsEvt(final boolean enableIgnoreOsEvt) {
        this.enableIgnoreOsEvt = enableIgnoreOsEvt;
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
