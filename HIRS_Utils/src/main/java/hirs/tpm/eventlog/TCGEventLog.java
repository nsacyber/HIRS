package hirs.tpm.eventlog;

/**
 * Interface for handling different formats of TCG Event logs.
 */
public interface TCGEventLog {
    /** Retrieves a all expected PCR values.
     * @return String array holding all PCR Values
     */
    String[] getExpectedPCRValues();
    /** Retrieves a single expected PCR values.
     * @param index the PCR reference
     * @return a String holding an expected PCR value
     */
    String getExpectedPCRValue(int index);
}
