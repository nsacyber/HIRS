package hirs.attestationca.portal.utils;

import hirs.attestationca.portal.entity.userdefined.SupplyChainSettings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The class handles the flags that ignore certain PCRs for validation.
 */
@NoArgsConstructor
public class PCRQuoteValidator {

    private static final Logger LOGGER = LogManager.getLogger(PCRQuoteValidator.class);

    /**
     * Minimum possible value for a PCR ID. This is 0.
     */
    public static final int MIN_PCR_ID = 0;

    /**
     * Maximum possible value for a PCR ID. This is 23.
     */
    public static final int MAX_PCR_ID = 23;

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

    private String[] baselinePCRS = new String[MAX_PCR_ID + 1];
    @Getter
    @Setter
    private SupplyChainSettings settings;

    /**
     * Constructor to parse PCR values.
     * @param pcrValues pcrValues RIM provided baseline PCRs
     * @param settings settings for the supply chain portal settings for provisioning
     */
    public PCRQuoteValidator(final String[] pcrValues,
                             final SupplyChainSettings settings) {
        if (pcrValues != null) {
            baselinePCRS = new String[MAX_PCR_ID + 1];
            for (int i = 0; i <= MAX_PCR_ID; i++) {
                baselinePCRS[i] = pcrValues[i];
            }
        }

        this.settings = settings;
    }

    /**
     * Getter for the array of baseline PCRs.
     * @return instance of the PCRs.
     */
    public String[] getBaselinePCRS() {
        return baselinePCRS.clone();
    }

    /**
     * Setter for the array of baseline PCRs.
     * @param baselinePCRS instance of the PCRs.
     */
    public void setBaselinePCRS(final String[] baselinePCRS) {
        this.baselinePCRS = baselinePCRS.clone();
    }

    /**
     * Compares the baseline pcr list and the quote pcr list.  If the
     * ignore flags are set, 10 and 17-19 will be skipped for comparison.
     *
     * @param storedPCRS non-baseline pcr list
     * @return a StringBuilder that is empty if everything passes.
     */
    public StringBuilder validatePCRS(final String[] storedPCRS) {
        StringBuilder sb = new StringBuilder();
        String failureMsg = "PCR %d does not match%n";
        if (storedPCRS[0] == null || storedPCRS[0].isEmpty()) {
            sb.append("failureMsg");
        } else {
            for (int i = 0; i <= MAX_PCR_ID; i++) {
                if (settings.isIgnoreImaEnabled() && i == IMA_PCR) {
                    LOGGER.info("PCR Policy IMA Ignore enabled.");
                    i += NUM_TO_SKIP;
                }

                if (settings.isIgnoretBootEnabled() && i == TBOOT_PCR_START) {
                    LOGGER.info("PCR Policy TBoot Ignore enabled.");
                    i += NUM_OF_TBOOT_PCR;
                }

                if (settings.isIgnoreGptEnabled() && i == GPT_PCR) {
                    LOGGER.info("PCR Policy GPT Ignore enabled.");
                    i += NUM_TO_SKIP;
                }

                if (!baselinePCRS[i].equals(storedPCRS[i])) {
                    //error
                    LOGGER.error(String.format("%s =/= %s", baselinePCRS[i], storedPCRS[i]));
                    sb.append(String.format(failureMsg, i));
                }
            }
        }

        return sb;
    }

    /**
     * Checks that the expected FM events occurring. There are policy options that
     * will ignore certain PCRs, Event Types and Event Variables present.
     * @param tcgMeasurementLog Measurement log from the client
     * @param eventValueMap The events stored as baseline to compare
     * @return the events that didn't pass
     */
//    public List<TpmPcrEvent> validateTpmEvents(final TCGEventLog tcgMeasurementLog,
//                                               final Map<String, ReferenceDigestValue> eventValueMap) {
//        List<TpmPcrEvent> tpmPcrEvents = new LinkedList<>();
//        for (TpmPcrEvent tpe : tcgMeasurementLog.getEventList()) {
//            if (enableIgnoreIma && tpe.getPcrIndex() == IMA_PCR) {
//                LOGGER.info(String.format("IMA Ignored -> %s", tpe));
//            } else if (enableIgnoretBoot && (tpe.getPcrIndex() >= TBOOT_PCR_START
//                    && tpe.getPcrIndex() <= TBOOT_PCR_END)) {
//                LOGGER.info(String.format("TBOOT Ignored -> %s", tpe));
//            } else if (enableIgnoreOsEvt && (tpe.getPcrIndex() >= PXE_PCR_START
//                    && tpe.getPcrIndex() <= PXE_PCR_END)) {
//                LOGGER.info(String.format("OS Evt Ignored -> %s", tpe));
//            } else {
//                if (enableIgnoreGpt && tpe.getEventTypeStr().contains(EVT_EFI_GPT)) {
//                    LOGGER.info(String.format("GPT Ignored -> %s", tpe));
//                } else if (enableIgnoreOsEvt && (tpe.getEventTypeStr().contains(EVT_EFI_BOOT)
//                        || tpe.getEventTypeStr().contains(EVT_EFI_VAR))) {
//                    LOGGER.info(String.format("OS Evt Ignored -> %s", tpe));
//                } else if (enableIgnoreOsEvt && (tpe.getEventTypeStr().contains(EVT_EFI_CFG)
//                        && tpe.getEventContentStr().contains("SecureBoot"))) {
//                    LOGGER.info(String.format("OS Evt Config Ignored -> %s", tpe));
//                } else {
//                    if (!eventValueMap.containsKey(tpe.getEventDigestStr())) {
//                        tpmPcrEvents.add(tpe);
//                    }
//                }
//            }
//        }
//
//        return tpmPcrEvents;
//    }

    /**
     * Compares hashes to validate the quote from the client.
     *
     * @param tpmQuote the provided quote
     * @param storedPCRS values from the RIM file
     * @return true if validated, false if not
     */
//    public boolean validateQuote(final byte[] tpmQuote, final String[] storedPCRS) {
//        System.out.println("Validating quote from associated device.");
//        boolean validated = false;
//        short localityAtRelease = 0;
//        String quoteString = new String(tpmQuote, StandardCharsets.UTF_8);
//        int pcrMaskSelection = PcrSelection.ALL_PCRS_ON;
//
//        if (enableIgnoreIma) {
//            pcrMaskSelection = IMA_MASK;
//        }
//
//        ArrayList<TPMMeasurementRecord> measurements = new ArrayList<>();
//
//        try {
//            for (int i = 0; i < storedPcrs.length; i++) {
//                if (i == IMA_PCR && enableIgnoreIma) {
//                    LOGGER.info("Ignore IMA PCR policy is enabled.");
//                } else {
//                    measurements.add(new TPMMeasurementRecord(i, storedPcrs[i]));
//                }
//            }
//        } catch (DecoderException deEx) {
//            //error
//            System.out.println(deEx);
//        }
//
//        PcrSelection pcrSelection = new PcrSelection(pcrMaskSelection);
//        PcrComposite pcrComposite = new PcrComposite(pcrSelection);
//        PcrInfoShort pcrInfoShort = new PcrInfoShort(pcrSelection,
//                localityAtRelease,
//                tpmQuote, pcrComposite);
//
//        try {
//            /**
//             * The calculated string is being used in the contains method
//             * because the TPM Quote's hash isn't just for PCR values,
//             * it contains the calculated digest of the PCRs, along with
//             * other information.
//             */
//            String calculatedString = Hex.encodeHexString(
//                    pcrInfoShort.getCalculatedDigest());
//            validated = quoteString.contains(calculatedString);
//            if (!validated) {
//                // warn
//                System.out.println(calculatedString + " not found in " + quoteString);
//            }
//        } catch (NoSuchAlgorithmException naEx) {
//            // error
//            System.out.println(naEx);
//        }
//
//        return validated;
//    }
}
