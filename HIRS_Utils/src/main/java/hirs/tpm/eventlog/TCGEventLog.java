package hirs.tpm.eventlog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import hirs.tpm.eventlog.events.EvConstants;
import hirs.utils.HexUtils;

/**
 * Class for handling different formats of TCG Event logs.
 */
public final class TCGEventLog {

//    private static final Logger LOGGER = (Logger) LogManager.getLogger(TCGEventLog.class);

    /** Initial value for SHA 256 values.*/
    public static final String INIT_SHA256_LIST = "00000000000000000000000000"
            + "00000000000000000000000000000000000000";
    /** Initial value for SHA 1 values. */
    public static final String INIT_SHA1_LIST = "0000000000000000000000000000000000000000";
    /** PFP defined EV_NO_ACTION identifier. */
    public static final int NO_ACTION_EVENT = 0x00000003;
    /** String value of SHA1 hash.*/
    public static final String HASH_STRING = "SHA1";
    /** String value of SHA256 hash.  */
    public static final String HASH256_STRING = "SHA-256";
    /** Each PCR bank holds 24 registers. */
    public static final int PCR_COUNT = 24;
    /** 2 dimensional array holding the PCR values. */
    private byte[][] pcrList;
    /** List of parsed events within the log. */
    private ArrayList<TpmPcrEvent> eventList = new ArrayList<>();
    /** Length of PCR. Indicates which hash algorithm is used. */
    private int pcrLength;
    /** Name of hash algorithm. */
    private String hashType;
    /** Initial Value to use. */
    private String initValue;

    /**
     * Default blank object constructor.
     */
    public TCGEventLog() {
        this.pcrList = new byte[PCR_COUNT][EvConstants.SHA1_LENGTH];
        initValue = INIT_SHA1_LIST;
        pcrLength = EvConstants.SHA1_LENGTH;
        initPcrList();
    }

    /**
     * Default constructor for just the rawlog that'll set up SHA1 Log.
     * @param rawlog data for the event log file.
     * @throws NoSuchAlgorithmException if an unknown algorithm is encountered.
     * @throws CertificateException if a certificate in the log cannot be parsed.
     * @throws IOException IO Stream if event cannot be parsed.
     */
    public TCGEventLog(final byte[] rawlog) throws CertificateException, NoSuchAlgorithmException,
                                                                                     IOException {
        this(rawlog, EvConstants.SHA1_LENGTH, HASH_STRING, INIT_SHA1_LIST);
    }

    /**
     * Default constructor for specific log.
     * @param rawlog data for the event log file
     * @param pLength determined by SHA1 or 256
     * @param hType the type of algorithm
     * @param iValue the default blank value.
     * @throws IOException IO Stream for the event log
     * @throws NoSuchAlgorithmException if an unknown algorithm is encountered.
     * @throws CertificateException f a certificate in the log cannot be parsed.
     */
    public TCGEventLog(final byte[] rawlog, final int pLength, final String hType,
                            final String iValue) throws IOException, CertificateException,
                                                                   NoSuchAlgorithmException {
        pcrLength = pLength;
        this.pcrList = new byte[PCR_COUNT][pcrLength];
        hashType = hType;
        initValue = iValue;
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
        for (int i = 0; i < PCR_COUNT; i++) {  // Initialize the PCRlist1 array
            System.arraycopy(HexUtils.hexStringToByteArray(
                    initValue),
                    0, pcrList[i], 0, pcrLength);
        }
    }

    /**
     * Calculates the "Expected Values for TPM PCRs based upon Event digests in the Event Log.
     * Uses the algorithm and eventList passed into the constructor,
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
              //      ((org.apache.logging.log4j.Logger) LOGGER).error(e);
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
        md.update(HexUtils.hexStringToByteArray(HexUtils.byteArrayToHexString(currentValue)
                + HexUtils.byteArrayToHexString(newEvent)));
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
            pcrs[i] = HexUtils.byteArrayToHexString(pcrList[i]);
        }
        return pcrs;
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

}
