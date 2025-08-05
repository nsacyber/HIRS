package hirs.attestationca.persist.validation;

import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.persist.entity.userdefined.record.TPMMeasurementRecord;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.tpm.PcrComposite;
import hirs.attestationca.persist.tpm.PcrInfoShort;
import hirs.attestationca.persist.tpm.PcrSelection;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static hirs.attestationca.persist.tpm.PcrSelection.ALL_PCRS_ON;

/**
 * The class handles the flags that ignore certain PCRs for validation.
 */
@Log4j2
public class PcrValidator {

    private static final int NUM_TO_SKIP = 1;
    private static final int NUM_OF_TBOOT_PCR = 3;
    private static final int BOOT_DRIVER_PCR = 4;
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

    private String[] baselinePcrs;

    /**
     * Default constructor.
     */
    public PcrValidator() {
        baselinePcrs = new String[TPMMeasurementRecord.MAX_PCR_ID + 1];
    }

    /**
     * Constructor to parse PCR values.
     *
     * @param pcrValues RIM provided baseline PCRs
     */
    public PcrValidator(final String[] pcrValues) {
        baselinePcrs = Arrays.copyOf(pcrValues, TPMMeasurementRecord.MAX_PCR_ID + 1);
    }

    /**
     * Builds a string array of stored pcrs.
     *
     * @param pcrContent      string representation of the pcr content
     * @param algorithmLength length of the algorithm
     * @return string array representation of the stored pcrs.
     */
    public static String[] buildStoredPcrs(final String pcrContent, final int algorithmLength) {
        // we have a full set of PCR values
        String[] pcrSet = pcrContent.split("\\n");
        String[] storedPcrs = new String[TPMMeasurementRecord.MAX_PCR_ID + 1];

        // we need to scroll through the entire list until we find
        // a matching hash length
        int offset = 1;

        for (int i = 0; i < pcrSet.length; i++) {
            if (pcrSet[i].contains("sha")) {
                // entered a new set, check size
                if (pcrSet[i + offset].split(":")[1].trim().length()
                        == algorithmLength) {
                    // found the matching set
                    for (int j = 0; j <= TPMMeasurementRecord.MAX_PCR_ID; j++) {
                        storedPcrs[j] = pcrSet[++i].split(":")[1].trim();
                    }
                    break;
                }
            }
        }

        return storedPcrs;
    }

    /**
     * Getter for the array of baseline PCRs.
     *
     * @return instance of the PCRs.
     */
    public String[] getBaselinePcrs() {
        return baselinePcrs.clone();
    }

    /**
     * Setter for the array of baseline PCRs.
     *
     * @param baselinePcrs instance of the PCRs.
     */
    public void setBaselinePcrs(final String[] baselinePcrs) {
        this.baselinePcrs = baselinePcrs.clone();
    }

    /**
     * Compares the baseline pcr list and the quote pcr list.  If the
     * ignore flags are set, 10 and 17-19 will be skipped for comparison.
     *
     * @param storedPcrs     non-baseline pcr list
     * @param policySettings db entity that holds all of policy
     * @return a StringBuilder that is empty if everything passes.
     */
    public StringBuilder validatePcrs(final String[] storedPcrs,
                                      final PolicySettings policySettings) {
        StringBuilder sb = new StringBuilder();
        String failureMsg = "PCR %d does not match%n";
        if (storedPcrs[0] == null || storedPcrs[0].isEmpty()) {
            sb.append("failureMsg");
        } else {
            for (int i = 0; i <= TPMMeasurementRecord.MAX_PCR_ID; i++) {
                if (policySettings.isIgnoreImaEnabled() && i == IMA_PCR) {
                    log.info("PCR Policy IMA Ignore enabled.");
                    i += NUM_TO_SKIP;
                }

                if (policySettings.isIgnoretBootEnabled() && i == TBOOT_PCR_START) {
                    log.info("PCR Policy TBoot Ignore enabled.");
                    i += NUM_OF_TBOOT_PCR;
                }

                if (policySettings.isIgnoreGptEnabled() && i == GPT_PCR) {
                    log.info("PCR Policy GPT Ignore enabled.");
                    i += NUM_TO_SKIP;
                }

                if (!baselinePcrs[i].equals(storedPcrs[i])) {
                    log.debug(String.format("PCR[%d]: %s =/= %s", i, baselinePcrs[i], storedPcrs[i]));
                    sb.append(String.format(failureMsg, i));
                }
            }
        }

        return sb;
    }

    /**
     * Checks that the expected FM events occurring. There are policy options that
     * will ignore certin PCRs, Event Types and Event Variables present.
     *
     * @param tcgMeasurementLog Measurement log from the client
     * @param eventLogRecords   The events stored as baseline to compare
     * @param policySettings    db entity that holds all of policy
     * @return the events that didn't pass
     */
    public List<TpmPcrEvent> validateTpmEvents(final TCGEventLog tcgMeasurementLog,
                                               final Map<String, ReferenceDigestValue> eventLogRecords,
                                               final PolicySettings policySettings) {
        List<TpmPcrEvent> tpmPcrEvents = new LinkedList<>();
        for (TpmPcrEvent tpe : tcgMeasurementLog.getEventList()) {
            if (policySettings.isIgnoreImaEnabled() && tpe.getPcrIndex() == IMA_PCR) {
                log.info(String.format("IMA Ignored -> %s", tpe));
            } else if (policySettings.isIgnoretBootEnabled() && (tpe.getPcrIndex() >= TBOOT_PCR_START
                    && tpe.getPcrIndex() <= TBOOT_PCR_END)) {
                log.info(String.format("TBOOT Ignored -> %s", tpe));
            } else if (policySettings.isIgnoreOsEvtEnabled() && (tpe.getPcrIndex() >= PXE_PCR_START
                    && tpe.getPcrIndex() <= PXE_PCR_END)) {
                log.info(String.format("OS Evt Ignored -> %s", tpe));
            } else {
                if (policySettings.isIgnoreGptEnabled() && tpe.getEventTypeStr().contains(EVT_EFI_GPT)) {
                    log.info(String.format("GPT Ignored -> %s", tpe));
                } else if (policySettings.isIgnoreOsEvtEnabled() && (
                        tpe.getEventTypeStr().contains(EVT_EFI_BOOT)
                                || tpe.getEventTypeStr().contains(EVT_EFI_VAR))) {
                    log.info(String.format("OS Evt Ignored -> %s", tpe));
                } else if (policySettings.isIgnoreOsEvtEnabled() && (
                        tpe.getEventTypeStr().contains(EVT_EFI_CFG)
                                && tpe.getEventContentStr().contains("SecureBoot"))) {
                    log.info(String.format("OS Evt Config Ignored -> %s", tpe));
                } else if (policySettings.isIgnoreOsEvtEnabled() &&
                        tpe.getPcrIndex() == BOOT_DRIVER_PCR) {
                    log.debug(String.format("PCR[4]: %s", tpe.getEventTypeString()));
                } else {
                    if (!eventLogRecords.containsKey(tpe.getEventDigestStr())) {
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
     * @param tpmQuote       the provided quote
     * @param storedPcrs     values from the RIM file
     * @param policySettings db entity that holds all of policy
     * @return true if validated, false if not
     */
    public boolean validateQuote(final byte[] tpmQuote, final String[] storedPcrs,
                                 final PolicySettings policySettings) {
        log.info("Validating quote from associated device.");
        boolean validated = false;
        short localityAtRelease = 0;
        String quoteString = new String(tpmQuote, StandardCharsets.UTF_8);
        int pcrMaskSelection = ALL_PCRS_ON;

        if (policySettings.isIgnoreImaEnabled()) {
            pcrMaskSelection = IMA_MASK;
        }

        ArrayList<TPMMeasurementRecord> measurements = new ArrayList<>();

        try {
            for (int i = 0; i < storedPcrs.length; i++) {
                if (i == IMA_PCR && policySettings.isIgnoreImaEnabled()) {
                    log.info("Ignore IMA PCR policy is enabled.");
                } else {
                    measurements.add(new TPMMeasurementRecord(i, storedPcrs[i]));
                }
            }
        } catch (DecoderException deEx) {
            log.error(deEx);
        }

        PcrSelection pcrSelection = new PcrSelection(pcrMaskSelection);
        PcrComposite pcrComposite = new PcrComposite(
                pcrSelection, measurements);
        PcrInfoShort pcrInfoShort = new PcrInfoShort(pcrSelection,
                localityAtRelease,
                tpmQuote, pcrComposite);

        try {

            // The calculated string is being used in the contains method
            // because the TPM Quote's hash isn't just for PCR values,
            // it contains the calculated digest of the PCRs, along with
            // other information.
            String calculatedString = Hex.encodeHexString(
                    pcrInfoShort.getCalculatedDigest());
            log.debug("Validating PCR information with the following:"
                    + System.lineSeparator() + "calculatedString = " + calculatedString
                    + System.lineSeparator() + "quoteString = " + quoteString);
            validated = quoteString.contains(calculatedString);
            if (!validated) {
                log.warn(calculatedString + " not found in " + quoteString);
            }
        } catch (NoSuchAlgorithmException naEx) {
            log.error(naEx);
        }

        return validated;
    }
}
