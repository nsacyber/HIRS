package hirs.attestationca.persist.entity.userdefined.record;

import hirs.attestationca.persist.entity.userdefined.ExaminableRecord;
import hirs.utils.digest.Digest;
import hirs.utils.digest.DigestAlgorithm;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <code>PCRMeasurementRecordTest</code> represents a unit test class for
 * <code>PCRMeasurementRecord</code>.
 */
public class TPMMeasurementRecordTest {

    private static final Logger LOGGER
            = LogManager.getLogger(TPMMeasurementRecordTest.class);
    private static final int DEFAULT_PCR_ID = 3;
    private static final String DEFAULT_HASH =
            "3d5f3c2f7f3003d2e4baddc46ed4763a4954f648";
    private static final ExaminableRecord.ExamineState DEFAULT_STATE =
            ExaminableRecord.ExamineState.UNEXAMINED;

    /**
     * Tests instantiation of new <code>PCRMeasurementRecord</code>.
     */
    @Test
    public final void tpmMeasurementRecord() {
        TPMMeasurementRecord pcrRecord = new TPMMeasurementRecord(0,
                getDigest(DEFAULT_HASH));
        assertNotNull(pcrRecord);
    }

    /**
     * Tests that <code>PCRMeasurementRecord</code> constructor throws a
     * NullPointerException with null hash.
     */
    @Test
    public final void tpmMeasurementRecordNullHash() {
        Digest digest = null;
        assertThrows(NullPointerException.class, () ->
                new TPMMeasurementRecord(0, digest));
    }

    /**
     * Tests that <code>PCRMeasurementRecord</code> constructor throws a
     * IllegalArgumentException with negative value for pcr id.
     */
    @Test
    public final void tpmMeasurementRecordNegativePcrId() {
        assertThrows(IllegalArgumentException.class, () ->
                new TPMMeasurementRecord(-1, getDigest(DEFAULT_HASH)));
    }

    /**
     * Tests that <code>PCRMeasurementRecord</code> constructor throws a
     * IllegalArgumentException with pcr id greater than 23.
     */
    @Test
    public final void tpmMeasurementRecordInvalidPcrId() {
        final int invalidPCR = 24;
        assertThrows(IllegalArgumentException.class, () ->
                new TPMMeasurementRecord(invalidPCR, getDigest(DEFAULT_HASH)));
    }

    /**
     * Tests that <code>getHash()</code> returns the measurement hash.
     */
    @Test
    public final void getHash() {
        TPMMeasurementRecord pcrRecord = new TPMMeasurementRecord(0,
                getDigest(DEFAULT_HASH));
        assertNotNull(pcrRecord.getHash());
    }

    /**
     * Tests that <code>getPcrId()</code> returns the pcr id.
     */
    @Test
    public final void getPcrId() {
        int id;
        TPMMeasurementRecord pcrRecord = new TPMMeasurementRecord(0,
                getDigest(DEFAULT_HASH));
        id = pcrRecord.getPcrId();
        assertNotNull(id);
    }

    /**
     * Tests that <code>getExamineState</code> returns the correct state.
     */
    @Test
    public final void getExamineState() {
        final TPMMeasurementRecord record = getDefaultRecord();
        assertEquals(DEFAULT_STATE, record.getExamineState());
    }

    /**
     * Tests that two <code>IMAMeasurementRecord</code>s are equal if they have
     * the same name and the same path.
     */
    @Test
    public final void testEquals() {
        TPMMeasurementRecord r1 = getDefaultRecord();
        TPMMeasurementRecord r2 = getDefaultRecord();
        assertEquals(r1, r2);
        assertEquals(r2, r1);
        assertEquals(r1, r1);
        assertEquals(r2, r2);
    }

    /**
     * Tests that two <code>TPMMeasurementRecord</code>s are not equal if the
     * PCR IDs are different.
     */
    @Test
    public final void testNotEqualsPcr() {
        final int pcrId = 5;
        TPMMeasurementRecord r1 = getDefaultRecord();
        TPMMeasurementRecord r2 = new TPMMeasurementRecord(pcrId,
                getDigest(DEFAULT_HASH));
        assertNotEquals(r1, r2);
        assertNotEquals(r2, r1);
        assertEquals(r1, r1);
        assertEquals(r2, r2);
    }

    /**
     * Tests that two <code>TPMMeasurementRecord</code>s are not equal if the
     * hashes are different.
     */
    @Test
    public final void testNotEqualsHash() {
        final String hash = "aacc3c2f7f3003d2e4baddc46ed4763a4954f648";
        TPMMeasurementRecord r1 = getDefaultRecord();
        TPMMeasurementRecord r2 =
                new TPMMeasurementRecord(DEFAULT_PCR_ID, getDigest(hash));
        assertNotEquals(r1, r2);
        assertNotEquals(r2, r1);
        assertEquals(r1, r1);
        assertEquals(r2, r2);
    }

    /**
     * Tests that the hash code of two <code>TPMMeasurementRecord</code>s are
     * the same.
     */
    @Test
    public final void testHashCodeEquals() {
        TPMMeasurementRecord r1 = getDefaultRecord();
        TPMMeasurementRecord r2 = getDefaultRecord();
        assertEquals(r1.hashCode(), r2.hashCode());
        assertEquals(r2.hashCode(), r1.hashCode());
        assertEquals(r1.hashCode(), r1.hashCode());
        assertEquals(r2.hashCode(), r2.hashCode());
    }

    /**
     * Tests that the hash code of two <code>TPMBaselineRecord</code>s is
     * different if they have different names.
     */
    @Test
    public final void testHashCodeNotEqualsPcrs() {
        final int pcrId = 5;
        TPMMeasurementRecord r1 = getDefaultRecord();
        TPMMeasurementRecord r2 = new TPMMeasurementRecord(pcrId,
                getDigest(DEFAULT_HASH));
        assertNotEquals(r1.hashCode(), r2.hashCode());
        assertNotEquals(r2.hashCode(), r1.hashCode());
        assertEquals(r1.hashCode(), r1.hashCode());
        assertEquals(r2.hashCode(), r2.hashCode());
    }

    /**
     * Tests that the hash code of two <code>TPMMeasurementRecord</code>s is
     * different if they have different hashes.
     */
    @Test
    public final void testHashCodeNotEqualsHashes() {
        final String hash = "aacc3c2f7f3003d2e4baddc46ed4763a4954f648";
        TPMMeasurementRecord r1 = getDefaultRecord();
        TPMMeasurementRecord r2 =
                new TPMMeasurementRecord(DEFAULT_PCR_ID, getDigest(hash));
        assertNotEquals(r1.hashCode(), r2.hashCode());
        assertNotEquals(r2.hashCode(), r1.hashCode());
        assertEquals(r1.hashCode(), r1.hashCode());
        assertEquals(r2.hashCode(), r2.hashCode());
    }

    /**
     * Tests that the expected valid PCR IDs do not throw an IllegalArgumentException.
     */
    @Test
    public final void testCheckForValidPcrId() {
        final int minPcrId = TPMMeasurementRecord.MIN_PCR_ID;
        final int maxPcrId = TPMMeasurementRecord.MAX_PCR_ID;
        for (int i = minPcrId; i < maxPcrId; i++) {
            TPMMeasurementRecord.checkForValidPcrId(i);
        }
    }

    /**
     * Tests that a negative PCR ID throws an IllegalArgumentException.
     */
    @Test
    public final void testCheckForValidPcrIdNegative() {
        final int pcrId = -1;
        assertThrows(IllegalArgumentException.class, () ->
                TPMMeasurementRecord.checkForValidPcrId(pcrId));
    }

    /**
     * Tests that a high invalid PCR ID throws an IllegalArgumentException.
     */
    @Test
    public final void testCheckForValidPcrIdInvalidId() {
        final int pcrId = 35;
        assertThrows(IllegalArgumentException.class, () ->
                TPMMeasurementRecord.checkForValidPcrId(pcrId));
    }

    /**
     * Tests that the ExamineState can be successfully set to EXAMINED.
     */
    @Test
    public final void testSetExamineStateExamined() {
        final ExaminableRecord.ExamineState state = ExaminableRecord.ExamineState.EXAMINED;
        TPMMeasurementRecord r1 = getDefaultRecord();
        r1.setExamineState(state);
        assertEquals(state, r1.getExamineState());
    }

    /**
     * Tests that the ExamineState can be successfully set to IGNORED.
     */
    @Test
    public final void testSetExamineStateIgnored() {
        final ExaminableRecord.ExamineState state = ExaminableRecord.ExamineState.IGNORED;
        TPMMeasurementRecord r1 = getDefaultRecord();
        r1.setExamineState(state);
        assertEquals(state, r1.getExamineState());
    }

    /**
     * Tests that the ExamineState is successfully initialized to UNEXAMINED.
     */
    @Test
    public final void testSetExamineStateInitial() {
        TPMMeasurementRecord r1 = getDefaultRecord();
        assertEquals(ExaminableRecord.ExamineState.UNEXAMINED, r1.getExamineState());
    }

    /**
     * Tests that setting the ExamineState to UNEXAMINED throws an IllegalArgumentException.
     */
    @Test
    public final void testSetExamineStateUnexamined() {
        final ExaminableRecord.ExamineState state = ExaminableRecord.ExamineState.UNEXAMINED;
        TPMMeasurementRecord r1 = getDefaultRecord();
        assertThrows(IllegalArgumentException.class, () ->
                r1.setExamineState(state));
    }

    private TPMMeasurementRecord getDefaultRecord() {
        return new TPMMeasurementRecord(DEFAULT_PCR_ID,
                getDigest(DEFAULT_HASH));
    }

    private Digest getDigest(final String hash) {
        try {
            final byte[] bytes = Hex.decodeHex(hash.toCharArray());
            return new Digest(DigestAlgorithm.SHA1, bytes);
        } catch (DecoderException e) {
            LOGGER.error("unable to create digest", e);
            throw new RuntimeException("unable to create digest", e);
        }
    }
}
