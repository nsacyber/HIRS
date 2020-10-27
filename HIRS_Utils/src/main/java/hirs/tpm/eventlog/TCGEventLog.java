package hirs.tpm.eventlog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.LinkedHashMap;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import hirs.data.persist.AbstractDigest;
import hirs.data.persist.Digest;
import hirs.data.persist.TPMMeasurementRecord;
import hirs.data.persist.baseline.TpmWhiteListBaseline;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.tpm.eventlog.events.EvConstants;
import hirs.tpm.eventlog.uefi.UefiConstants;
import hirs.utils.HexUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class for handling different formats of TCG Event logs.
 */
public final class TCGEventLog {

    /** Logger. */
    private static final Logger LOGGER = LogManager.getLogger(TCGEventLog.class);
    /** Name of the hash algorithm used to process the Event Log, default is SHA256.  */
    private String algorithm = "TPM_ALG_SHA256";
    /** Parsed event log array. */
    private static final int SIG_OFFSET = 32;
    /**  TEV_NO_ACTION signature size. */
    private static final int SIG_SIZE = 16;
    /** Initial value for SHA 256 values.*/
    public static final String INIT_SHA256_LIST = "00000000000000000000000000"
            + "00000000000000000000000000000000000000";
    /** Initial value for SHA 256 values.*/
    public static final String LOCALITY4_SHA256_LIST = "ffffffffffffffffffffffffff"
            + "ffffffffffffffffffffffffffffffffffffff";
    /** Initial value for SHA 1 values. */
    public static final String INIT_SHA1_LIST = "0000000000000000000000000000000000000000";
    /** Initial value for SHA 1 values. */
    public static final String LOCALITY4_SHA1_LIST = "ffffffffffffffffffffffffffffffffffffffff";
    /** PFP defined EV_NO_ACTION identifier. */
    public static final int NO_ACTION_EVENT = 0x00000003;
    /** String value of SHA1 hash.*/
    public static final String HASH_STRING = "SHA1";
    /** String value of SHA256 hash.  */
    public static final String HASH256_STRING = "SHA-256";
    /** Each PCR bank holds 24 registers. */
    public static final int PCR_COUNT = 24;
    /** Locality 4 starts at PCR 17. */
    public static final int PCR_LOCALITY4_MIN = 17;
    /** Locality 4 Ends at PCR 23. */
    public static final int PCR_LOCALITY4_MAX = 23;
    /** 2 dimensional array holding the PCR values. */
    private byte[][] pcrList;
    /** List of parsed events within the log. */
    private LinkedHashMap<Integer, TpmPcrEvent> eventList = new LinkedHashMap<>();
    /** Length of PCR. Indicates which hash algorithm is used. */
    private int pcrLength;
    /** Name of hash algorithm. */
    private String hashType;
    /** Initial PCR Value to use. */
    private String initValue;
    /** Initial PcR Value to use for locality 4. */
    private String initLocalityFourValue;
    /** Content Output Flag use. */
    private boolean bContent = false;
    /** Event Output Flag use. */
    private boolean bHexEvent = false;
    /** Event Output Flag use. */
    private boolean bEvent = false;
    /** Event Output Flag use. */
    private boolean bCryptoAgile = false;

    /**
     * Default blank object constructor.
     */
    public TCGEventLog() {
        this.pcrList = new byte[PCR_COUNT][EvConstants.SHA1_LENGTH];
        initValue = INIT_SHA1_LIST;
        initLocalityFourValue = LOCALITY4_SHA1_LIST;
        pcrLength = EvConstants.SHA1_LENGTH;
        hashType = HASH_STRING;
        algorithm = "TPM_ALG_SHA1";
        initPcrList();
    }

    /**
     * Simple constructor for Event Log.
     * @param rawlog data for the event log file.
     * @throws NoSuchAlgorithmException if an unknown algorithm is encountered.
     * @throws CertificateException if a certificate in the log cannot be parsed.
     * @throws IOException IO Stream if event cannot be parsed.
     */
    public TCGEventLog(final byte[] rawlog)
                       throws CertificateException, NoSuchAlgorithmException, IOException {
        this(rawlog, false, false, false);
    }

    /**
     * Default constructor for just the rawlog that'll set up SHA1 Log.
     * @param rawlog data for the event log file.
     * @param bEventFlag if true provides human readable event descriptions.
     * @param bContentFlag if true provides hex output for Content in the description.
     * @param bHexEventFlag if true provides hex event structure in the description.
     * @throws NoSuchAlgorithmException if an unknown algorithm is encountered.
     * @throws CertificateException if a certificate in the log cannot be parsed.
     * @throws IOException IO Stream if event cannot be parsed.
     */
    public TCGEventLog(final byte[] rawlog, final boolean bEventFlag,
                       final boolean bContentFlag, final boolean bHexEventFlag)
                       throws CertificateException, NoSuchAlgorithmException, IOException {

        bCryptoAgile = isLogCrytoAgile(rawlog);
        if (bCryptoAgile) {
            initValue = INIT_SHA256_LIST;
            initLocalityFourValue = LOCALITY4_SHA256_LIST;
            algorithm = "TPM_ALG_SHA256";
            hashType = HASH256_STRING;
            pcrLength = EvConstants.SHA256_LENGTH;
        } else {
            initValue = INIT_SHA1_LIST;
            initLocalityFourValue = LOCALITY4_SHA1_LIST;
            hashType = HASH_STRING;
            algorithm = "TPM_ALG_SHA1";
            pcrLength = EvConstants.SHA1_LENGTH;
        }
        this.pcrList = new byte[PCR_COUNT][pcrLength];
        int eventNumber = 0;
        bContent = bContentFlag;
        bEvent = bEventFlag;
        bHexEvent = bHexEventFlag;
        ByteArrayInputStream is = new ByteArrayInputStream(rawlog);
        // Process the 1st entry as a SHA1 format (per the spec)
        eventList.put(eventNumber, new TpmPcrEvent1(is, eventNumber++));
        // put all events into an event list for further processing

        while (is.available() > 0) {
            if (bCryptoAgile) {
                eventList.put(eventNumber, new TpmPcrEvent2(is, eventNumber++));
            } else {
                eventList.put(eventNumber, new TpmPcrEvent1(is, eventNumber++));
            }
        }
        calculatePcrValues();
    }

    /**
     * This method puts blank values in the pcrList.
     */
    private void initPcrList() {
            try {
                for (int i = 0; i < PCR_COUNT; i++) {
                    System.arraycopy(Hex.decodeHex(initValue.toCharArray()),
                        0, pcrList[i], 0, pcrLength);
                }
                for (int i = PCR_LOCALITY4_MIN; i < PCR_LOCALITY4_MAX; i++) {
                        System.arraycopy(Hex.decodeHex(initLocalityFourValue.toCharArray()),
                            0, pcrList[i], 0, pcrLength);
                    }
            } catch (DecoderException deEx) {
                LOGGER.error(deEx);
            }
    }

    /**
     * Creates a TPM baseline using the expected PCR Values.
     * Expected PCR Values were Calculated from the EventLog (RIM Support file).
     *
     * @param name name to call the TPM Baseline
     * @return whitelist baseline
     */
    public TpmWhiteListBaseline createTPMBaseline(final String name) {
        TpmWhiteListBaseline baseline = new TpmWhiteListBaseline(name);
        TPMMeasurementRecord record;
        String pcrValue;
        for (int i = 0; i < PCR_COUNT; i++) {
            if (algorithm.compareToIgnoreCase("TPM_ALG_SHA1") == 0) { // Log Was SHA1 Format
                pcrValue = getExpectedPCRValue(i);
                byte[] hexValue = HexUtils.hexStringToByteArray(pcrValue);
                final Digest hash = new Digest(DigestAlgorithm.SHA1, hexValue);
                record = new TPMMeasurementRecord(i, hash);
            } else {  // Log was Crypto Agile, currently assumes SHA256
                pcrValue = getExpectedPCRValue(i);
                byte[] hexValue = HexUtils.hexStringToByteArray(pcrValue);
                final Digest hash = new Digest(DigestAlgorithm.SHA256, hexValue);
                record = new TPMMeasurementRecord(i, hash);
            }
            baseline.addToBaseline(record);
        }
        return baseline;
    }

    /**
     * Calculates the "Expected Values for TPM PCRs based upon Event digests in the Event Log.
     * Uses the algorithm and eventList passed into the constructor,
     */
    private void calculatePcrValues() {
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
     * @throws NoSuchAlgorithmException if hash algorithm not supported
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
     * Returns a flag which is set if the event log follows the "Crypto Agile" Format Type.
     * A false implies the type is SHA1 format.
     * @return true if log follows the Crypto Agile format.
     */
    public boolean  isCryptoAgile() {
        return bCryptoAgile;
    }

    /**
     * Returns a list of event found in the Event Log.
     * @return an arraylist of event.
     */
    public Collection<TpmPcrEvent> getEventList() {
        return eventList.values();
    }

    /**
     * Returns a specific element of the Event Log that corresponds to the requested
     * event number.
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
     * Human readable string representing the contents of the Event Log.
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
     * Human readable string representing the contents of the Event Log.
     * @param bEvent flag to set
     * @param bHexEvent flag to set
     * @param bContent flag to set
     * @return Description of the log.
     */
    public String toString(final boolean bEvent,
                           final boolean bHexEvent,
                           final boolean bContent) {
        this.bEvent = bEvent;
        this.bHexEvent = bHexEvent;
        this.bContent = bContent;

        return this.toString();
    }

    /**
     * Returns the TCG Algorithm Registry defined string for the Digest Algorithm
     * used in the event log.
     * @return TCG Defined Algorithm name
     */
    public String getEventLogHashAlgorithm() {
        return algorithm;
    }
    /**
     * Returns the TCG Algorithm Registry defined ID for the Digest Algorithm
     * used in the event log.
     * @return TCG Defined Algorithm name
     */
    public int getEventLogHashAlgorithmID() {
       return TcgTpmtHa.tcgAlgStringtoId(algorithm);
    }

    /**
     * Determines if an event is an EfiSpecIdEvent indicating that the log format is crypto agile.
     * The EfiSpecIdEvent should be the first event in the TCG TPM Event Log.
     *
     * @param log The Event Log
     * @return true if EfiSpecIDEvent is found and indicates that the format is crypto agile
     * @throws UnsupportedEncodingException if parsing error occurs.
     */
    private boolean isLogCrytoAgile(final byte[] log) throws UnsupportedEncodingException {
        byte[] eType = new byte[UefiConstants.SIZE_4];
        System.arraycopy(log, UefiConstants.SIZE_4, eType, 0, UefiConstants.SIZE_4);
        byte[] eventType = HexUtils.leReverseByte(eType);
        int eventID = new BigInteger(eventType).intValue();
        if (eventID != TCGEventLog.NO_ACTION_EVENT) {
            return false;
        }  // Event Type should be EV_NO_ACTION
        byte[] signature = new byte[SIG_SIZE];
        System.arraycopy(log, SIG_OFFSET, signature, 0, SIG_SIZE); // should be "Spec ID Event03"
        String sig = new String(signature, "UTF-8").substring(0, SIG_SIZE - 1);  // remove null char

        return sig.equals("Spec ID Event03");
    }
}
