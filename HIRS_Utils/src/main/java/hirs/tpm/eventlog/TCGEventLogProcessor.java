package hirs.tpm.eventlog;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

import hirs.data.persist.TPMMeasurementRecord;
import hirs.data.persist.baseline.TpmWhiteListBaseline;
import hirs.utils.HexUtils;
import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;
import org.apache.commons.codec.DecoderException;

/**
 * Class for parsing a TCG EventLogs (both SHA1 and Crypto Agile Formats).
 * Also produces a TPM Baseline using he digests within the event log.
 * Constructor parses the input byte array into a List of TpmPcrEvents.
 */
public class TCGEventLogProcessor {
    /**
     * Name of the hash algorithm used to process the Event Log, default is SHA256.
     */
    private String algorithm = "SHA256";
    /**
     * Parsed event log array.
     */
    private TCGEventLog tcgLog = null;
    /**
     * EV_NO_ACTION signature offset.
     */
    private static final int SIG_OFFSET = 32;
    /**
     * TEV_NO_ACTION signature size.
     */
    private static final int SIG_SIZE = 16;

    /**
     * Default Constructor.
     */
    public TCGEventLogProcessor() {
        tcgLog = new TCGEventLog();
    }

    /**
     * Constructor.
     *
     * @param rawLog the byte array holding the contents of the TCG Event Log
     * @throws IOException if there is a parsing error
     */
    public TCGEventLogProcessor(final byte[] rawLog) throws IOException {
        if (isLogCrytoAgile(rawLog)) {
            tcgLog = new TCGEventLog(rawLog, TpmPcrEvent.SHA256_LENGTH,
                    TCGEventLog.HASH256_STRING, TCGEventLog.INIT_SHA256_LIST);
        } else {
            tcgLog = new TCGEventLog(rawLog);
            algorithm = "SHA";
        }
    }

    /**
     * Returns all 24 PCR values for display purposes.
     *
     * @return Returns an array of strings representing the expected hash values for all 24 PCRs
     */
    public String[] getExpectedPCRValues() {
        return tcgLog.getExpectedPCRValues();
    }

    /**
     * Returns a single PCR value given an index (PCR Number).
     *
     * @param index the PCR index
     * @return String representing the PCR contents
     */
    public String getExpectedPCRValue(final int index) {
        return tcgLog.getExpectedPCRString(index);
    }

    /**
     * Creates a TPM baseline using the expected PCR Values.
     * Expected PCR Values were Calculated from the EventLog (RIM Support file).
     *
     * @param name name to call the TPM Baseline
     * @return whitelist baseline
     * @throws DecoderException hex string problem.
     */
    public TpmWhiteListBaseline createTPMBaseline(final String name) throws DecoderException {
        TpmWhiteListBaseline baseline = new TpmWhiteListBaseline(name);
        TPMMeasurementRecord record;

        for (int i = 0; i < TpmPcrEvent.PCR_COUNT; i++) {
            if (algorithm.compareToIgnoreCase("SHA1") == 0) { // Log Was SHA1 Format
                record = new TPMMeasurementRecord(i,
                        new Digest(DigestAlgorithm.SHA1,
                        tcgLog.getExpectedPCRBytes(i)));
            } else {  // Log was Crypto Agile, currently assumes SHA256
                record = new TPMMeasurementRecord(i,
                        new Digest(DigestAlgorithm.SHA256,
                                tcgLog.getExpectedPCRBytes(i)));
            }
            baseline.addToBaseline(record);
        }
        return baseline;
    }

    /**
     * Determines if an event is an EfiSpecIdEvent indicating that the log format is crypto agile.
     * The EfiSpecIdEvent should be the first event in the TCG TPM Event Log.
     *
     * @param log The Event Log
     * @return true if EfiSpecIDEvent is found and indicates that the format is crypto agile
     * @throws UnsupportedEncodingException
     */
    private boolean isLogCrytoAgile(final byte[] log) throws UnsupportedEncodingException {
        byte[] eType = new byte[TpmPcrEvent.INT_LENGTH];
        System.arraycopy(log, TpmPcrEvent.INT_LENGTH, eType, 0, TpmPcrEvent.INT_LENGTH);
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
