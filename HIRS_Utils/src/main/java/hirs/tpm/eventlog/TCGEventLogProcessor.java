package hirs.tpm.eventlog;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

import hirs.data.persist.TPMMeasurementRecord;
import hirs.data.persist.TpmWhiteListBaseline;
import hirs.tpm.eventlog.events.EvConstants;
import hirs.tpm.eventlog.uefi.UefiConstants;
import hirs.utils.HexUtils;
import hirs.data.persist.Digest;
import hirs.data.persist.DigestAlgorithm;;

/**
 * Class for parsing a TCG EventLogs (both SHA1 and Crypto Agile Formats).
 * Also produces a TPM Baseline using he digests within the event log.
 * Constructor parses the input byte array into a List of TpmPcrEvents.
 */
public class TCGEventLogProcessor {
    /** Name of the hash algorithm used to process the Event Log, default is SHA256.  */
    private String algorithm = "TPM_ALG_SHA256";
    /** Parsed event log array. */
    private TCGEventLog tcgLog = null;
    /** EV_NO_ACTION signature offset. */
    private static final int SIG_OFFSET = 32;
    /**  TEV_NO_ACTION signature size. */
    private static final int SIG_SIZE = 16;
    /**  Number of PCRs in a TPM PCR Bank. */
    private static final int PCR_COUNT = 24;

    /**
     * Default Constructor.
     */
    public TCGEventLogProcessor() {
        tcgLog = new TCGEventLog();
    }

    /**
     * Constructor.
     *
     * @param rawLog the byte array holding the contents of the TCG Event Log.
     * @throws IOException IO Stream for the event log.
     * @throws NoSuchAlgorithmException if an unknown algorithm is encountered.
     * @throws CertificateException f a certificate in the log cannot be parsed.
     */
    public TCGEventLogProcessor(final byte[] rawLog) throws IOException, CertificateException,
                                                                      NoSuchAlgorithmException {
        if (isLogCrytoAgile(rawLog)) {
            tcgLog = new TCGEventLog(rawLog, EvConstants.SHA256_LENGTH,
                    TCGEventLog.HASH256_STRING, TCGEventLog.INIT_SHA256_LIST);
        } else {
            tcgLog = new TCGEventLog(rawLog);
            algorithm = "TPM_ALG_SHA1";
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
        return tcgLog.getExpectedPCRValue(index);
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
     * Returns a list of event found in the Event Log.
     * @return an arraylist of event.
     */
    public ArrayList<TpmPcrEvent>  getEventList() {
        return tcgLog.getEventList();
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
                pcrValue = tcgLog.getExpectedPCRValue(i);
                byte[] hexValue = HexUtils.hexStringToByteArray(pcrValue);
                final Digest hash = new Digest(DigestAlgorithm.SHA1, hexValue);
                record = new TPMMeasurementRecord(i, hash);
            } else {  // Log was Crypto Agile, currently assumes SHA256
                pcrValue = tcgLog.getExpectedPCRValue(i);
                byte[] hexValue = HexUtils.hexStringToByteArray(pcrValue);
                final Digest hash = new Digest(DigestAlgorithm.SHA256, hexValue);
                record = new TPMMeasurementRecord(i, hash);
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
     * @throws UnsupportedEncodingException if parsing error occurs.
     */
    public boolean isLogCrytoAgile(final byte[] log) throws UnsupportedEncodingException {
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

    /**
     * Human readable string representing the contents of the Event Log.
     * @return Description of the log.
     */
    public String toString() {
       return tcgLog.toString();
    }
}
