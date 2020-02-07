package hirs.tpm.eventlog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import hirs.utils.HexUtils;

/**
 * Class to handle the "Crypto Agile" Format for TCG Event Logs as defined in the
 * TCG Platform Firmware Profile (PFP).
 * The Format can provide multiple digests with different algorithm,
 * however currently on SHA256 is supported.
 * All other are currently ignored.
 */
public class CryptoAgileEventLog implements TCGEventLog {
    /**
     * SHA256 length = 24 bytes.
     */
    private static final int PCR_LENGTH = TpmPcrEvent.SHA256_LENGTH;
    /**
     * Each PCR bank holds 24 registers.
     */
    private static final int PCR_COUNT = 24;
    /**
     * PFP defined EV_NO_ACTION identifier.
     */
    private static final int NO_ACTION_EVENT = 0x00000003;
    /**
     * 2 dimensional array holding the PCR values.
     */
    private byte[][] pcrList = new byte[PCR_COUNT][PCR_LENGTH];
    /**
     * List of parsed events within the log.
     */
    private ArrayList<TpmPcrEvent> eventList = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param rawlog the entire tcg log
     * @throws IOException if the input stream cannot access the log data
     */
    public CryptoAgileEventLog(final byte[] rawlog) throws IOException {
        ByteArrayInputStream is = new ByteArrayInputStream(rawlog);
        // process the EfiSpecId Event in SHA1 format per the PFP
        TpmPcrEvent1 idEvent = new TpmPcrEvent1(is);
        eventList.add(idEvent);
        // put all events into an event list for further processing
        while (is.available() > 0) {   // All other events should be Crypto agile
            eventList.add(new TpmPcrEvent2(is));
        }
        calculatePCRValues();
    }

    /**
     * Returns a single PCR value given an index (PCR Number).
     */
    @Override
    public String[] getExpectedPCRValues() {
        String[] pcrs = new String[PCR_COUNT];
        for (int i = 0; i < PCR_COUNT; i++) {
            pcrs[i] = HexUtils.byteArrayToHexString(pcrList[i]);
        }
        return pcrs;
    }

    /**
     * Returns all 24 PCR values for display purposes.
     *
     * @param index pcr index
     * @return Returns an array of strings for all 24 PCRs
     */
    @Override
    public String getExpectedPCRValue(final int index) {
        return HexUtils.byteArrayToHexString(pcrList[index]);
    }

    /**
     * Calculates the "Expected Values for TPM PCRs based upon Event digests in the Event Log.
     * Uses the algorithm and eventList passed into the constructor.
     *
     * @return a 2 dimensional bye array holding the hashes of the PCRs.
     */
    private void calculatePCRValues() {
        byte[] extendedPCR = null;
        for (int i = 0; i < PCR_COUNT; i++) { // Initialize the PCRlist array
            System.arraycopy(HexUtils.hexStringToByteArray(
                    "0000000000000000000000000000000000000000000000000000000000000000"),
                    0, pcrList[i], 0, PCR_LENGTH);
        }
        for (TpmPcrEvent currentEvent : eventList) {
            if (currentEvent.getPcrIndex() >= 0) {   // Ignore NO_EVENTS which can have a PCR=-1
                try {
                    if (currentEvent.getEventType() != NO_ACTION_EVENT) {
                        // Don't include EV_NO_ACTION
                        extendedPCR = extendPCRsha256(pcrList[currentEvent.getPcrIndex()],
                                currentEvent.getEventDigest());
                        System.arraycopy(extendedPCR, 0, pcrList[currentEvent.getPcrIndex()],
                                0, PCR_LENGTH);
                    }
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Extends a sha256 hash with a hash of new data.
     *
     * @param currentValue byte array holding the current hash value
     * @param newEvent     byte array holding the value to extend
     * @return new hash value
     * @throws NoSuchAlgorithmException if hash algorithm is not supported.
     */
    private byte[] extendPCRsha256(final byte[] currentValue, final byte[] newEvent)
            throws NoSuchAlgorithmException {
        return sha256hash(
                HexUtils.hexStringToByteArray(HexUtils.byteArrayToHexString(currentValue)
                        + HexUtils.byteArrayToHexString(newEvent)));
    }

    /**
     * Creates a sha356 hash of a given byte array.
     *
     * @param blob byte array hold the value to hash.
     * @return byte array holding the hash of the input array.
     * @throws NoSuchAlgorithmException if hash algorithm is not supported.
     */
    private byte[] sha256hash(final byte[] blob) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(blob);
        return md.digest();
    }
}
