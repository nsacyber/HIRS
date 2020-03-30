package hirs.tpm.eventlog;

import hirs.data.persist.AbstractDigest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class for handling different formats of TCG Event logs.
 */
public class TCGEventLog {

    private static final Logger LOGGER
            = LogManager.getLogger(TCGEventLog.class);

    /**
     * Init value for SHA 256 values.
     */
    public static final String INIT_SHA256_LIST = "00000000000000000000000000"
            + "00000000000000000000000000000000000000";
    /**
     * Init value for SHA 1 values.
     */
    public static final String INIT_SHA1_LIST = "0000000000000000000000000000000000000000";

    /**
     * PFP defined EV_NO_ACTION identifier.
     */
    public static final int NO_ACTION_EVENT = 0x00000003;
    /**
     * String value of SHA1 hash.
     */
    public static final String HASH_STRING = "SHA1";
    /**
     * String value of SHA256 hash.
     */
    public static final String HASH256_STRING = "SHA-256";
    /**
     * Each PCR bank holds 24 registers.
     */
    public static final int PCR_COUNT = 24;
    /**
     * 2 dimensional array holding the PCR values.
     */
    private final byte[][] pcrList;
    /**
     * List of parsed events within the log.
     */
    private final ArrayList<TpmPcrEvent> eventList = new ArrayList<>();

    private int pcrLength;
    private String hashType;
    private String initValue;

    /**
     * Default blank object constructor.
     */
    public TCGEventLog() {
        this.pcrList = new byte[PCR_COUNT][TpmPcrEvent.SHA1_LENGTH];
        initValue = INIT_SHA1_LIST;
        pcrLength = TpmPcrEvent.SHA1_LENGTH;
        initPcrList();
    }

    /**
     * Default constructor for just the rawlog that'll set up SHA1 Log.
     *
     * @param rawlog data for the event log file
     * @throws IOException IO Stream for the event log
     */
    public TCGEventLog(final byte[] rawlog) throws IOException {
        this(rawlog, TpmPcrEvent.SHA1_LENGTH, HASH_STRING, INIT_SHA1_LIST);
    }

    /**
     * Default constructor for specific log.
     *
     * @param rawlog data for the event log file
     * @param pcrLength determined by SHA1 or 256
     * @param hashType the type of algorithm
     * @param initValue the default blank value
     * @throws IOException IO Stream for the event log
     */
    public TCGEventLog(final byte[] rawlog, final int pcrLength,
            final String hashType, final String initValue) throws IOException {
        this.pcrLength = pcrLength;
        this.pcrList = new byte[PCR_COUNT][pcrLength];
        this.hashType = hashType;
        this.initValue = initValue;
        ByteArrayInputStream is = new ByteArrayInputStream(rawlog);
        // Process the 1st entry as a SHA1 format (per the spec)
        eventList.add(new TpmPcrEvent1(is));
        // put all events into an event list for further processing
        while (is.available() > 0) {
            if (hashType.compareToIgnoreCase(HASH_STRING) == 0) {
                eventList.add(new TpmPcrEvent1(is));
            } else {
                eventList.add(new TpmPcrEvent2(is));
            }
        }
        calculatePcrValues();
    }

    /**
     * This method puts blank values in the pcrList.
     */
    private void initPcrList() {
        for (int i = 0; i < PCR_COUNT; i++) {
            try {
                // Initialize the PCRlist1 array
                System.arraycopy(Hex.decodeHex(initValue.toCharArray()),
                        0, pcrList[i], 0, pcrLength);
            } catch (DecoderException deEx) {
                LOGGER.error(deEx);
            }
        }
    }

    /**
     * Calculates the "Expected Values for TPM PCRs based upon Event digests in
     * the Event Log. Uses the algorithm and eventList passed into the
     * constructor,
     */
    private void calculatePcrValues() {
        byte[] extendedPCR;
        initPcrList();
        for (TpmPcrEvent currentEvent : eventList) {
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
     * @param newEvent value to extend with
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
     * @return Returns an array of strings representing the expected hash values
     * for all 24 PCRs
     */
    public String[] getExpectedPCRValues() {
        String[] pcrs = new String[PCR_COUNT];
        for (int i = 0; i < PCR_COUNT; i++) {
            pcrs[i] = Hex.encodeHexString(pcrList[i]);
        }
        return pcrs;
    }

    /**
     * Returns a single PCR value given an index (PCR Number).
     *
     * @param index pcr index
     * @return String representing the PCR contents
     */
    public String getExpectedPCRString(final int index) {
        return Hex.encodeHexString(pcrList[index]);
    }

    /**
     * Returns a single PCR value given an index (PCR Number).
     *
     * @param index pcr index.
     * @return byte array of the pcr contents.
     */
    public byte[] getExpectedPCRBytes(final int index) {
        return pcrList[index];
    }
}
