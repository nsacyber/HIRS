package hirs.utils.tpm.eventlog;

import hirs.utils.HexUtils;
import hirs.utils.digest.AbstractDigest;
import hirs.utils.tpm.eventlog.events.EvConstants;
import hirs.utils.tpm.eventlog.events.EvEfiSpecIdEvent;
import hirs.utils.tpm.eventlog.events.EvNoAction;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import static hirs.utils.crypto.AlgorithmsIds.ALG_TYPE_HASH;
import static hirs.utils.crypto.AlgorithmsIds.SPEC_TCG_ALG;
import static hirs.utils.crypto.AlgorithmsIds.findAlgId;
import static hirs.utils.tpm.eventlog.TcgTpmtHa.TPM_ALG_SHA256_STR;
import static hirs.utils.tpm.eventlog.TcgTpmtHa.TPM_ALG_SHA384_STR;
import static hirs.utils.tpm.eventlog.TcgTpmtHa.TPM_ALG_SHA512_STR;

/**
 * Class for handling different formats of TCG Event logs.
 */
public final class TCGEventLog {

    // The TCG PC Client Platform TPM Profile Specification for TPM 2.0 defines 5 localities
    //    (In this code these localities are referred to as 'environment localities')
    //    Locality 0: The Static RTM, its chain of trust and its environment.
    //    Locality 1: An environment for use by the Dynamic OS.
    //    Locality 2: Dynamically Launched OS (Dynamic OS) “runtime” environment.
    //    Locality 3: Auxiliary components. Use of this is optional and, if used, is implementation dependent.
    //    Locality 4: Usually associated with the CPU executing microcode. Used to establish the Dynamic RTM.
    //
    // Global System Power States /Sleeping States, described in TCG PFP 2.2.14
    //    (Transitions described in TCG PFP 8.3)
    //    G0 = S0: On
    //    G1: Sleeping State
    //      S1: Stand-by with low wakeup latency
    //      S2: Stand-by with CPU context lost
    //      S3: Suspend to RAM                   --> PCR0 will be initialized differently
    //      S4: Hibernate (OS Initiated)         --> PCR0 will be initialized differently
    //      S4: Hibernate (BIOS Initiated)       --> PCR0 will be initialized differently
    //    G2 = S5: Soft Off State
    //    G3: Mechanical Off State
    //
    // Boot Startup Locality event - This event records the (power state) locality from which the
    //                               TPM2_Startup command was sent.
    //    (In this code StartupLocality is referred to as 'startup locality')
    //    (In this code Locality is referred to as 'locality', and essentially corresponds
    //    to S0-S5 State Transitions)
    //    The TCG PFP section 10.4.5.3 mentions Startup Locality:
    //       StartupLocality 0: is Locality 0 without an H-CRTM sequence
    //       StartupLocality 3: is Locality 3 without an H-CRTM sequence (S3 -> S0)
    //       StartupLocality 4: is Locality 4 with an H-CRTM sequence initialized (S4 -> S0)
    //
    // 1) TPM Reset -- Tpm2Startup(CLEAR) after Tpm2Shutdown(CLEAR)
    //    PCRs with default initialization state go back to their default initialization state.
    // 2) TPM Restart -- Tpm2Startup(CLEAR) after Tpm2Shutdown(STATE)
    //    Preserves much of the previous state of the TPM, with some exceptions.
    // 3) TPM Resume -- Tpm2Startup(STATE) after Tpm2Shutdown(STATE).
    //    Preserves the previous state of the TPM.
    //
    // Ex. An EV_NO_ACTION Boot Event with StartupLocality 3 refers to Locality 3, which refers to a
    //     state transition S3 to S0, corresponding to TPM Restart. Requires PCR0 to be initialized to 3.

    /**
     * String value of SHA1 hash.
     */
    public static final String HASH_SHA1_STRING = "SHA1";
    /**
     * String value of SHA256 hash.
     */
    public static final String HASH_SHA256_STRING = "SHA-256";
    /**
     * String value of SHA256 hash.
     */
    public static final String HASH_SHA384_STRING = "SHA-384";
    /**
     * String value of SHA256 hash.
     */
    public static final String HASH_SHA512_STRING = "SHA-512";

    byte PCR_INIT = (byte) 0x00;
    byte PCR_INIT_ENVLOCALITY4 = (byte) 0xff;

//    /**
//     * Initial PCR value for SHA 1 if environment locality is 0-3 (PCRs 0-16).
//     */
//    public static final String PCR_SHA1_INIT =
//            "0000000000000000000000000000000000000000";

//    /**
//     * Initial PCR value for SHA 1 if environment locality is 4 (PCRs 17-22).
//     */
//    public static final String PCR_SHA1_INIT_ENVLOCALITY4 =
//            "ffffffffffffffffffffffffffffffffffffffff";
//    /**
//     * Initial PCR value for SHA 256 if environment locality is 0-3 (PCRs 0-16).
//     */
//    public static final String PCR_SHA256_INIT =
//              "00000000000000000000000000000000"
//            + "00000000000000000000000000000000";
//    /**
//     * Initial PCR value for SHA 256 if environment locality is 4 (PCRs 17-22).
//     */
//    public static final String PCR_SHA256_INIT_ENVLOCALITY4 =
//              "ffffffffffffffffffffffffffffffff"
//            + "ffffffffffffffffffffffffffffffff";
//    /**
//     * Initial PCR value for SHA 384 if environment locality is 0-3 (PCRs 0-16).
//     */
//    public static final String PCR_SHA384_INIT =
//              "00000000000000000000000000000000"
//            + "00000000000000000000000000000000"
//            + "00000000000000000000000000000000";
//    /**
//     * Initial PCR value for SHA 384 if environment locality is 4 (PCRs 17-22).
//     */
//    public static final String PCR_SHA384_INIT_ENVLOCALITY4 =
//            "ffffffffffffffffffffffffffffffff"
//          + "ffffffffffffffffffffffffffffffff"
//          + "ffffffffffffffffffffffffffffffff";
//    /**
//     * Initial PCR value for SHA 512 if environment locality is 0-3 (PCRs 0-16).
//     */
//    public static final String PCR_SHA512_INIT =
//            "00000000000000000000000000000000"
//          + "00000000000000000000000000000000"
//          + "00000000000000000000000000000000"
//          + "00000000000000000000000000000000";
//    /**
//     * Initial PCR value for SHA 512 if environment locality is 4 (PCRs 17-22).
//     */
//    public static final String PCR_SHA512_INIT_ENVLOCALITY4 =
//            "ffffffffffffffffffffffffffffffff"
//          + "ffffffffffffffffffffffffffffffff"
//          + "ffffffffffffffffffffffffffffffff"
//          + "ffffffffffffffffffffffffffffffff";
    /**
     * Each PCR bank holds 24 registers.
     */
    public static final int PCR_COUNT = 24;
    /**
     * Environment Locality 4 starts at PCR 17.
     */
    public static final int PCR_ENVLOCALITY4_MIN = 17;
    /**
     * Environment Locality 4 ends at PCR 22.
     */
    public static final int PCR_ENVLOCALITY4_MAX = 22;
    /**
     * PFP defined EV_NO_ACTION identifier.
     */
    public static final int NO_ACTION_EVENT = 0x00000003;
    /**
     * Startup locality 3 defined in the TCG PFP section 10.4.5.3.
     * Used for NO_ACTION_EVENT with "StartupLocality" in the signature.
     */
    public static final int STARTUP_LOCALITY3 = 0x03;
    /**
     * Startup locality 4 defined in the TCG PFP section 10.4.5.3.
     * Used for NO_ACTION_EVENT with "StartupLocality" in the signature.
     */
    public static final int STARTUP_LOCALITY4 = 0x04;
//    /**
//     * Initial PCR0 value for SHA1 with startup locality 3.
//     */
//    public static final String PCR0_SHA1_INIT_STARTUP_LOCALITY3 =
//              "0000000000000000000000000000000000000003";
//    /**
//     * Initial PCR0 value for SHA256 with startup locality 3.
//     */
//    public static final String PCR0_SHA256_INIT_STARTUP_LOCALITY3 =
//              "00000000000000000000000000000000"
//            + "00000000000000000000000000000003";
//    /**
//     * Initial PCR0 value for SHA256 with startup locality 4.
//     */
//    public static final String PCR0_SHA256_INIT_STARTUP_LOCALITY4 =
//              "00000000000000000000000000000000"
//            + "00000000000000000000000000000004";
//    /**
//     * Initial PCR0 value for SHA384 with startup locality 3.
//     */
//    public static final String PCR0_SHA384_INIT_STARTUP_LOCALITY3 =
//              "00000000000000000000000000000000"
//            + "00000000000000000000000000000000"
//            + "00000000000000000000000000000003";
//    /**
//     * Initial PCR0 value for SHA384 with startup locality 4.
//     */
//    public static final String PCR0_SHA384_INIT_STARTUP_LOCALITY4 =
//              "00000000000000000000000000000000"
//            + "00000000000000000000000000000000"
//            + "00000000000000000000000000000004";
//    /**
//     * Initial PCR0 value for SHA512 with startup locality 3.
//     */
//    public static final String PCR0_SHA512_INIT_STARTUP_LOCALITY3 =
//              "00000000000000000000000000000000"
//            + "00000000000000000000000000000000"
//            + "00000000000000000000000000000000"
//            + "00000000000000000000000000000003";
//    /**
//     * Initial PCR0 value for SHA512 with startup locality 4.
//     */
//    public static final String PCR0_SHA512_INIT_STARTUP_LOCALITY4 =
//              "00000000000000000000000000000000"
//            + "00000000000000000000000000000000"
//            + "00000000000000000000000000000000"
//            + "00000000000000000000000000000004";
    /**
     * Logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(TCGEventLog.class);
    /**
     * Parsed event log array; signature offset of EV_NO_ACTION event
     */
    private static final int SIG_OFFSET = 32;
    /**
     * Parsed event log array.
     */
//    private static final int EVENT_ID_OFFSET = 4;
//    private static final int ALG_COUNT_OFFSET = 56;
//    private static final int ALG_ONE_OFFSET = 60;
//    private static final int ALG_TWO_OFFSET = 64;
    /**
     * TEV_NO_ACTION signature size.
     */
    private static final int SIG_SIZE = 16;
    /**
     * Name of the hash algorithm used to process the Event Log, default is SHA256.
     */
    @Getter
    private String eventLogHashAlgorithm = "TPM_ALG_SHA256";
    /**
     * 2-dimensional array holding the PCR values.
     */
    private byte[][] pcrList;
    /**
     * List of parsed events within the log.
     */
    private final LinkedHashMap<Integer, TpmPcrEvent> eventList = new LinkedHashMap<>();
    /**
     * Length of PCR. Indicates which hash algorithm is used.
     */
    private int pcrLength;
    /**
     * Name of hash algorithm.
     */
    private String hashType;
//    /**
//     * Initial PCR Value to use.
//     */
//    private String initPcrValue;
//    /**
//     * Initial PcR Value to use for environment locality 4.
//     */
//    private String initPcrValueEnvLocality4;
    /**
     * Startup locality. If none, then value is -1.
     */
    private int startupLocality = -0x01;
    /**
     * Content Output Flag use.
     */
    private boolean bContent = false;
    /**
     * Event Output Flag use.
     */
    private boolean bHexEvent = false;
    /**
     * Event Output Flag use.
     */
    private boolean bEvent = false;
    /**
     * Event Output Flag use.
     */
    @Getter
    private boolean bCryptoAgile = false;
    /**
     * Track status of vendor-table.json
     * This is only used if there is an event that uses a UefiVariable data structure.
     * Default is normal status (normal status is from-filesystem).
     * Status will only change IF there is a UefiVariable event in this log,
     * and if that event causes a different status.
     */
    @Getter
    private String vendorTableFileStatus = UefiConstants.FILESTATUS_FROM_FILESYSTEM;
    /**
     * Track status of pci.ids
     * This is only used if there is an event that uses functions from the pciids class.
     * Default is normal status (normal status is from-filesystem).
     * Status will only change IF there is an event that uses pciids file, and the file
     * causes a different status.
     */
    @Getter
    private String pciidsFileStatus = UefiConstants.FILESTATUS_FROM_FILESYSTEM;

    /**
     * Default blank object constructor.
     */
    public TCGEventLog() throws UnsupportedEncodingException {
        this.pcrList = new byte[PCR_COUNT][EvConstants.SHA1_LENGTH];
//        initPcrValue = PCR_SHA1_INIT;
//        initPcrValueEnvLocality4 = PCR_SHA1_INIT_ENVLOCALITY4;
        pcrLength = EvConstants.SHA1_LENGTH;
        hashType = HASH_SHA1_STRING;
        eventLogHashAlgorithm = "TPM_ALG_SHA1";
        initPcrList();
    }

    /**
     * Simple constructor for Event Log.
     *
     * @param rawlog data for the event log file.
     * @throws java.security.NoSuchAlgorithmException  if an unknown algorithm is encountered.
     * @throws java.security.cert.CertificateException if a certificate in the log cannot be parsed.
     * @throws java.io.IOException                     IO Stream if event cannot be parsed.
     */
    public TCGEventLog(final byte[] rawlog)
            throws CertificateException, NoSuchAlgorithmException, IOException {
        this(rawlog, false, false, false);
    }

    /**
     * Default constructor for just the rawlog that'll set up SHA1 Log.
     *
     * @param rawlog        data for the event log file.
     * @param bEventFlag    if true provides human-readable event descriptions.
     * @param bContentFlag  if true provides hex output for Content in the description.
     * @param bHexEventFlag if true provides hex event structure in the description.
     * @throws java.security.NoSuchAlgorithmException  if an unknown algorithm is encountered.
     * @throws java.security.cert.CertificateException if a certificate in the log cannot be parsed.
     * @throws java.io.IOException                     IO Stream if event cannot be parsed.
     */
    public TCGEventLog(final byte[] rawlog, final boolean bEventFlag,
                       final boolean bContentFlag, final boolean bHexEventFlag)
            throws CertificateException, NoSuchAlgorithmException, IOException {


//        String hexString = "48656C6C6F"; // Represents "Hello" in hex
//        hexString = "ffffffffff";
//        try {
//            byte[] decodedBytes = Hex.decodeHex(hexString.toCharArray());
//            String decodedString = new String(decodedBytes, "UTF-8"); // Assuming UTF-8 encoding
//            System.out.println("Decoded string: " + decodedString);
//
//            byte[] initPcrValueB = new byte[6];
//            Arrays.fill(initPcrValueB, (byte) 0xff);
//            String stop = "stop";
//        } catch (org.apache.commons.codec.DecoderException e) {
//            System.err.println("Error decoding hex: " + e.getMessage());
//        } catch (java.io.UnsupportedEncodingException e) {
//            System.err.println("Unsupported encoding: " + e.getMessage());
//        }


//        bCryptoAgile = isLogCryptoAgile(rawlog);
        bContent = bContentFlag;
        bEvent = bEventFlag;
        bHexEvent = bHexEventFlag;

        int eventNumber = 0;
        ByteArrayInputStream is = new ByteArrayInputStream(rawlog);

        // Process the 1st entry as a SHA1 format (per the spec)
//        eventList.put(eventNumber, new TpmPcrEvent1(is, eventNumber++));
        TpmPcrEvent1 firstEvent = new TpmPcrEvent1(is, eventNumber++);
        eventList.put(eventNumber, firstEvent);
        useFirstEventToInitValues(firstEvent);

        // put all events into an event list for further processing
        while (is.available() > 0) {
            if (bCryptoAgile) {
                TpmPcrEvent2 event2 = new TpmPcrEvent2(is, eventNumber++);
                eventList.put(eventNumber, event2);
                if (event2.isStartupLocalityEvent()) {
                    EvNoAction event = new EvNoAction(event2.getEventContent());
                    startupLocality = event.getStartupLocality();
                }
            } else {
                TpmPcrEvent1 event1 = new TpmPcrEvent1(is, eventNumber++);
                eventList.put(eventNumber, event1);

                // ????
                // can a TpmPcrEvent1 ever have a startup locality event?
                if (event1.isStartupLocalityEvent()) {
                    EvNoAction event = new EvNoAction(event1.getEventContent());
                    startupLocality = event.getStartupLocality();
                }
            }

            // first check if any previous event has not been able to access vendor-table.json,
            // and if that is the case, the first comparison in the if-statement returns false and
            // the if-statement is not executed
            // [previous event file status = vendorTableFileStatus]
            // (ie. keep the file status to reflect that file was not accessible at some point)
            // next, check if the new event has any status other than the default 'filesystem',
            // and if that is the case, the 2nd comparison in the if-statement returns true and
            // the if-statement is executed
            // [new event file status = eventList.get(eventNumber-1).getVendorTableFileStatus()]
            // (ie. if the new file status is not-accessible or from-code, then want to update)
            if ((vendorTableFileStatus != UefiConstants.FILESTATUS_NOT_ACCESSIBLE)
                    && (eventList.get(eventNumber - 1).getVendorTableFileStatus()
                    != UefiConstants.FILESTATUS_FROM_FILESYSTEM)) {
                vendorTableFileStatus = eventList.get(eventNumber - 1).getVendorTableFileStatus();
            }
            //similar to above with vendor-table.json file, but here with pci.ids file
            if ((pciidsFileStatus != UefiConstants.FILESTATUS_NOT_ACCESSIBLE)
                    && (eventList.get(eventNumber - 1).getPciidsFileStatus()
                    != UefiConstants.FILESTATUS_FROM_FILESYSTEM)) {
                pciidsFileStatus = eventList.get(eventNumber - 1).getPciidsFileStatus();
            }
        }
        calculatePcrValues();
    }

    /**
     * If first event is Spec Id, then the log is crypto agile. Need info from this event about algorithms.
     *
     * @param firstEvent                                the first event in the log
     * @throws java.security.NoSuchAlgorithmException   if an unknown algorithm is encountered.
     */
    private void useFirstEventToInitValues(TpmPcrEvent1 firstEvent) throws NoSuchAlgorithmException {

        // if first event is Spec Id Event, the log is crypto agile
        if (firstEvent.isNoActionSpecIdEvent()) {

            bCryptoAgile = true;
            EvEfiSpecIdEvent specEvent = new EvEfiSpecIdEvent(firstEvent.getEventContent());
            List<String> algList = specEvent.getAlgList();

            String strongestAlg = algList.get(0);
            int currentStrongestAlgRow = findAlgId(ALG_TYPE_HASH, SPEC_TCG_ALG, strongestAlg);
            for (int i = 1; i < algList.size(); i++) {
                String newAlg = algList.get(i);
                int newAlgRow = findAlgId(ALG_TYPE_HASH, SPEC_TCG_ALG, newAlg);
                if (newAlgRow > currentStrongestAlgRow) {
                    strongestAlg = newAlg;
                }
            }

            // if more than one set of PCR banks exists in this log, store the one with strongest algorithm
            switch (strongestAlg) {
                case TPM_ALG_SHA256_STR:
//                    initPcrValue = PCR_SHA256_INIT;
//                    initPcrValueEnvLocality4 = PCR_SHA256_INIT_ENVLOCALITY4;
                    eventLogHashAlgorithm = "TPM_ALG_SHA256";
                    hashType = HASH_SHA256_STRING;
                    pcrLength = EvConstants.SHA256_LENGTH;
                    break;
                case TPM_ALG_SHA384_STR:
//                    initPcrValue = PCR_SHA384_INIT;
//                    initPcrValueEnvLocality4 = PCR_SHA384_INIT_ENVLOCALITY4;
                    eventLogHashAlgorithm = "TPM_ALG_SHA384";
                    hashType = HASH_SHA384_STRING;
                    pcrLength = EvConstants.SHA384_LENGTH;
                    break;
                case TPM_ALG_SHA512_STR:
//                    initPcrValue = PCR_SHA512_INIT;
//                    initPcrValueEnvLocality4 = PCR_SHA512_INIT_ENVLOCALITY4;
                    eventLogHashAlgorithm = "TPM_ALG_SHA512";
                    hashType = HASH_SHA512_STRING;
                    pcrLength = EvConstants.SHA512_LENGTH;
                    break;
                default:
                    break;
            }
        } else {    // not crypto agile
//            initPcrValue = PCR_SHA1_INIT;
//            initPcrValueEnvLocality4 = PCR_SHA1_INIT_ENVLOCALITY4;
            hashType = HASH_SHA1_STRING;
            eventLogHashAlgorithm = "TPM_ALG_SHA1";
            pcrLength = EvConstants.SHA1_LENGTH;
        }

        // if more than one set of PCR banks exists in this log, store the one with the strongest algorithm
        this.pcrList = new byte[PCR_COUNT][pcrLength];
    }

//    /**
//     * Determines if an event is an EfiSpecIdEvent indicating that the log format is crypto agile.
//     * The EfiSpecIdEvent should be the first event in the TCG TPM Event Log.
//     *
//     * @param log The Event Log
//     * @return true if EfiSpecIDEvent is found and indicates that the format is crypto agile
//     */
//    private boolean isLogCryptoAgile(final byte[] log) {
//        /*
//        byte[] eType = new byte[UefiConstants.SIZE_4];
//        System.arraycopy(log, UefiConstants.SIZE_4, eType, 0, UefiConstants.SIZE_4);
//        byte[] eventType = HexUtils.leReverseByte(eType);
//        int eventID = new BigInteger(eventType).intValue();
//        */
//        int eventID = getLogInt(log, EVENT_ID_OFFSET, UefiConstants.SIZE_4);
//        if (eventID != TCGEventLog.NO_ACTION_EVENT) {
//            return false;
//        }  // Event Type should be EV_NO_ACTION
//        byte[] signature = new byte[SIG_SIZE];
//        // should be "Spec ID Event03"
//        System.arraycopy(log, SIG_OFFSET, signature, 0, SIG_SIZE);
//        // remove null char
//        String sig = new String(signature, StandardCharsets.UTF_8).substring(0, SIG_SIZE - 1);
//
//        return sig.equals("Spec ID Event03");
//    }
//
//    private int getLogInt(byte[] log, int offset, int length) {
//        byte[] data = new byte [length];
//        System.arraycopy(log,offset, data, 0, length);
//        byte[] revData = HexUtils.leReverseByte(data);
//        int logInt = new BigInteger(revData).intValue();
//        return logInt;
//    }

    /**
     * This method initializes the pcrList.
     */
    private void initPcrList() throws UnsupportedEncodingException {
        //            String pcrInit = getPcr0InitValue();
//            System.arraycopy(Hex.decodeHex(pcrInit.toCharArray()),
//                    0, pcrList[0], 0, pcrLength);

        for (int i = 0; i < PCR_COUNT; i++) {
//       for (int i = 1; i < PCR_COUNT; i++) {
//                System.arraycopy(Hex.decodeHex(initPcrValue.toCharArray()),
//                        0, pcrList[i], 0, pcrLength);
            byte[] initPcrValueB = new byte[pcrLength];
            Arrays.fill(initPcrValueB, PCR_INIT);
            System.arraycopy(initPcrValueB,0, pcrList[i], 0, pcrLength);
        }
        for (int i = PCR_ENVLOCALITY4_MIN; i <= PCR_ENVLOCALITY4_MAX; i++) {
//                System.arraycopy(Hex.decodeHex(initPcrValueEnvLocality4.toCharArray()),
//                        0, pcrList[i], 0, pcrLength);
            byte[] initPcrValueB = new byte[pcrLength];
            Arrays.fill(initPcrValueB, PCR_INIT_ENVLOCALITY4);
            System.arraycopy(initPcrValueB,0, pcrList[i], 0, pcrLength);
        }
        if (startupLocality == STARTUP_LOCALITY3) {
            pcrList[0][pcrLength-1] = (byte) STARTUP_LOCALITY3;
        }
        else if (startupLocality == STARTUP_LOCALITY4) {
            LOGGER.error("Error Processing TGC Event Log: "
                    + "Event of type EV_NO_ACTION with a Startup Locality 4 with an H-CRTM "
                    + "encountered, but no support is currently provided by this application");
        }
    }

//    /**
//     * Search for the startup locality in the event log and set the
//     * initial value of PCRO in accordance with the TCG PFP spec.
//     * @return string representing the initial value for PCR0
//     */
//     private String getPcr0InitValue() throws UnsupportedEncodingException {
//
//
//
//         for (TpmPcrEvent currentEvent : eventList.values()) {
//             if (currentEvent.getEventType() == NO_ACTION_EVENT) {
//                 EvNoAction event = new EvNoAction(currentEvent.getEventContent());
//                 if (event.isSpecIdEvent()) {
//                     EvEfiSpecIdEvent specEvent = new EvEfiSpecIdEvent(currentEvent.getEventContent());
//                     List<String> algList = specEvent.getAlgList();
//                     int algCount = algList.size();
//                 }
//                 if (event.isStartupLocality()) {
//             if (currentEvent.isStartupLocalityEvent()) {
//                 EvNoAction event = new EvNoAction(currentEvent.getEventContent());
//                 int locality = event.getStartupLocality();
//                 if (locality == STARTUP_LOCALITY3) {
//                     if (eventLogHashAlgorithm.compareToIgnoreCase("TPM_ALG_SHA1") == 0) {
//                         return PCR0_SHA1_INIT_STARTUP_LOCALITY3;
//                     } else if (eventLogHashAlgorithm.compareToIgnoreCase("TPM_ALG_SHA256") == 0) {
//                         return PCR0_SHA256_INIT_STARTUP_LOCALITY3;
//                     } else if (eventLogHashAlgorithm.compareToIgnoreCase("TPM_ALG_SHA384") == 0) {
//                         return PCR0_SHA384_INIT_STARTUP_LOCALITY3;
//                     } else if (eventLogHashAlgorithm.compareToIgnoreCase("TPM_ALG_SHA512") == 0) {
//                         return PCR0_SHA512_INIT_STARTUP_LOCALITY3;
//                     } else {
//                         LOGGER.error("Error Processing TGC Event Log: "
//                                 + "Event of type EV_NO_ACTION with StartupLocality and non supported Hash algorithm.");
//                         return "";
//                     }
//                 } else if (locality == STARTUP_LOCALITY4) {
//                     LOGGER.error("Error Processing TGC Event Log: "
//                           + "Event of type EV_NO_ACTION with a Startup Locality 4 with an H-CRTM "
//                           + "encountered, but no support is currently provided by this application");
//                     return "";
//                 } else {
//                     return "";
//                 }
//             }
//         }
//         return "";
//     }

    /**
     * Calculates the "Expected Values for TPM PCRs based upon Event digests in the Event Log.
     * Uses the algorithm and eventList passed into the constructor,
     */
    private void calculatePcrValues() throws UnsupportedEncodingException {
        byte[] extendedPCR;
        initPcrList();
        for (TpmPcrEvent currentEvent : eventList.values()) {
            if (currentEvent.getPcrIndex() >= 0) {   // Ignore NO_EVENTS which can have a PCR=-1
                try {
                    if (currentEvent.getEventType() != NO_ACTION_EVENT) {
                        // Don't include EV_NO_ACTION event
                        extendedPCR = extendPCR(pcrList[currentEvent.getPcrIndex()],
                                currentEvent.getEventDigest());
                        System.arraycopy(extendedPCR, 0, pcrList[currentEvent.getPcrIndex()],
                                0, currentEvent.getDigestLength());
                    }
                } catch (NoSuchAlgorithmException e) {
                    LOGGER.error(e);
                }
            }
        }
    }

    /**
     * Extends a hash with a hash of new data.
     *
     * @param currentValue value to extend
     * @param newEvent     value to extend with
     * @return new hash resultant hash
     * @throws java.security.NoSuchAlgorithmException if hash algorithm not supported
     */
    private byte[] extendPCR(final byte[] currentValue, final byte[] newEvent)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(hashType);
        StringBuilder sb = new StringBuilder(AbstractDigest.SHA512_DIGEST_LENGTH);
        sb.append(Hex.encodeHexString(currentValue).toCharArray());
        sb.append(Hex.encodeHexString(newEvent).toCharArray());

        try {
            md.update(Hex.decodeHex(sb.toString().toCharArray()));
        } catch (DecoderException deEx) {
            LOGGER.error(deEx);
        }
        return md.digest();
    }

    /**
     * Returns all 24 PCR values for display purposes.
     *
     * @return Returns an array of strings representing the expected hash values for all 24 PCRs
     */
    public String[] getExpectedPCRValues() {
        String[] pcrs = new String[PCR_COUNT];
        for (int i = 0; i < PCR_COUNT; i++) {
            pcrs[i] = Hex.encodeHexString(pcrList[i]);
        }
        return pcrs;
    }

    /**
     * Returns a list of event found in the Event Log.
     *
     * @return an arraylist of event.
     */
    public Collection<TpmPcrEvent> getEventList() {
        return eventList.values();
    }

    /**
     * Returns a specific element of the Event Log that corresponds to the requested
     * event number.
     *
     * @param eventNumber specific event to find in the list.
     * @return TPM Event in the position of the list
     */
    public TpmPcrEvent getEventByNumber(final int eventNumber) {
        return eventList.get(eventNumber);
    }

    /**
     * Returns a single PCR value given an index (PCR Number).
     *
     * @param index pcr index
     * @return String representing the PCR contents
     */
    public String getExpectedPCRValue(final int index) {
        return HexUtils.byteArrayToHexString(pcrList[index]);
    }

    /**
     * Human-readable string representing the contents of the Event Log.
     *
     * @return Description of the log.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TpmPcrEvent event : eventList.values()) {
            sb.append(event.toString(bEvent, bHexEvent, bContent));
        }
        sb.append("Event Log processing completed.\n");
        return sb.toString();
    }

    /**
     * Human-readable string representing the contents of the Event Log.
     *
     * @param event    flag to set
     * @param hexEvent flag to set
     * @param content  flag to set
     * @return Description of the log.
     */
    public String toString(final boolean event,
                           final boolean hexEvent,
                           final boolean content) {
        this.bEvent = event;
        this.bHexEvent = hexEvent;
        this.bContent = content;

        return this.toString();
    }

    /**
     * Returns the TCG Algorithm Registry defined ID for the Digest Algorithm
     * used in the event log.
     *
     * @return TCG Defined Algorithm name
     */
    public int getEventLogHashAlgorithmID() {
        return TcgTpmtHa.tcgAlgStringToId(eventLogHashAlgorithm);
    }
}
