package hirs.tpm.eventlog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import hirs.utils.HexUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class to handle the "SHA1" Format for TCG Event Logs.
 * "SHA1" Format is defined in the TCG Platform Firmware Profile (PFP).
 * This is to support older versions of UEFI Firmware or OS that create logs with SHA1.
 */
public class SHA1EventLog implements TCGEventLog {
    private static final Logger LOGGER
            = LogManager.getLogger(TCGEventLog.class);
    /**
     * SHA256 length = 24 bytes.
     */
    private static final int PCR_LENGTH = TpmPcrEvent.SHA1_LENGTH;
    /**
     * Each PCR bank holds 24 registers.
     */
    private static final int PCR_COUNT = 24;
    /**
     * PFP defined EV_NO_ACTION identifier.
     */
    private static final int NO_ACTION_EVENT = 0x00000003;
    /**
     * List of parsed events within the log.
     */
    private final ArrayList<TpmPcrEvent> eventList = new ArrayList<TpmPcrEvent>();
    /**
     * 2 dimensional array holding the PCR values.
     */
    private final byte[][] pcrList1 = new byte[PCR_COUNT][PCR_LENGTH];

    /**
     * Constructor.
     *
     * @param rawlog the entire tcg log
     * @throws IOException if the inspur stream cannot access the log data
     */
    public SHA1EventLog(final byte[] rawlog) throws IOException {
        ByteArrayInputStream is = new ByteArrayInputStream(rawlog);
        // put all events into an event list for further processing
        while (is.available() > 0) {
            eventList.add(new TpmPcrEvent1(is));
        }
        calculatePcrValues();
    }

    /**
     * Returns all 24 PCR values for display purposes.
     *
     * @return Returns an array of strings representing the expected hash values for all 24 PCRs
     */
    public String[] getExpectedPCRValues() {
        String[] pcrs = new String[PCR_COUNT];
        for (int i = 0; i < PCR_COUNT; i++) {
            pcrs[i] = HexUtils.byteArrayToHexString(pcrList1[i]);
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
        return HexUtils.byteArrayToHexString(pcrList1[index]);
    }

    /**
     * Calculates the "Expected Values for TPM PCRs based upon Event digests in the Event Log.
     * Uses the algorithm and eventList passed into the constructor,
     */
    private void calculatePcrValues() {
        byte[] extendedPCR = null;
        for (int i = 0; i < PCR_COUNT; i++) {  // Initialize the PCRlist1 array
            System.arraycopy(HexUtils.hexStringToByteArray(
                    "0000000000000000000000000000000000000000"),
                    0, pcrList1[i], 0, PCR_LENGTH);
        }
        for (TpmPcrEvent currentEvent : eventList) {
            if (currentEvent.getPcrIndex() >= 0) {   // Ignore NO_EVENTS which can have a PCR=-1
                try {
                    if (currentEvent.getEventType() != NO_ACTION_EVENT) {
                        // Don't include EV_NO_ACTION event
                        extendedPCR = extendPCRsha1(pcrList1[currentEvent.getPcrIndex()],
                                currentEvent.getEventDigest());
                        System.arraycopy(extendedPCR, 0, pcrList1[currentEvent.getPcrIndex()],
                                0, currentEvent.getDigestLength());
                    }
                } catch (NoSuchAlgorithmException e) {
                    LOGGER.error(e);
                }
            }
        }
    }

    /**
     * Extends a sha1 hash with a hash of new data.
     *
     * @param currentValue value to extend
     * @param newEvent     value to extend with
     * @return new hash resultant hash
     * @throws NoSuchAlgorithmException if hash algorithm not supported
     */
    private byte[] extendPCRsha1(final byte[] currentValue, final byte[] newEvent)
            throws NoSuchAlgorithmException {
        return sha1hash(HexUtils.hexStringToByteArray(HexUtils.byteArrayToHexString(currentValue)
                + HexUtils.byteArrayToHexString(newEvent)));
    }

    /**
     * Creates a sha1 hash of a given byte array.
     *
     * @param blob byte array of data to hash
     * @return byte array holding the hash of the input array
     * @throws NoSuchAlgorithmException id hash algorithm not supported
     */
    private byte[] sha1hash(final byte[] blob) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(blob);
        return md.digest();
    }
}
