package hirs.data.persist.tpm;

import org.apache.commons.codec.binary.Base64;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * PcrSelectionTest is a unit test class for the PcrSelection class.
 */
public class PcrSelectionTest {

    private static final byte ON_BITS = (byte) 0xFF;
    private static final byte OFF_BITS = (byte) 0x00;
    private static final int NUM_BITS_IN_BYTE = 8;

    /**
     * Tests that a PCR selection object with 2 bytes returns the expected
     * values. The first half of the bits are set to on, and the second half are
     * set to off.
     */
    @Test
    public final void isPcrSelected() {
        byte[] selectionBytes = {ON_BITS, OFF_BITS, OFF_BITS};
        PcrSelection pcrSelection = new PcrSelection(selectionBytes);
        for (int i = 0; i < NUM_BITS_IN_BYTE; i++) {
            Assert.assertTrue(pcrSelection.isPcrSelected(i));
        }
        for (int i = NUM_BITS_IN_BYTE;
                i < NUM_BITS_IN_BYTE * selectionBytes.length; i++) {
            Assert.assertFalse(pcrSelection.isPcrSelected(i));
        }
    }

    /**
     * The TCG spec includes an example, where the base64 encoded string "AAQ="
     * (2 bytes) corresponds to PCR 13. This tests that the bytes are correct
     * based on the base64 encoding and that only PCR 13 is selected.
     */
    @Test
    public final void testTCGExample() {
        byte[] selectionBytes = {0x00, 0x04};
        final String expectedMask = "002000";
        PcrSelection pcrSelection = new PcrSelection(selectionBytes);
        Assert.assertEquals(
                Base64.encodeBase64URLSafeString(pcrSelection.getPcrSelect()),
                "AAQA");
        for (int i = 0; i < selectionBytes.length * NUM_BITS_IN_BYTE; i++) {
            if (i == 13) {
                Assert.assertTrue(pcrSelection.isPcrSelected(i));
            } else {
                Assert.assertFalse(pcrSelection.isPcrSelected(i));
            }
        }
        Assert.assertEquals(pcrSelection.getMaskForTPM(), expectedMask);
    }

    /**
     * Tests that an exception is thrown if a negative index is given.
     */
    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public final void isSelectedNegativeValue() {
        byte[] selectionBytes = {ON_BITS};
        PcrSelection pcrSelection = new PcrSelection(selectionBytes);
        pcrSelection.isPcrSelected(-1);
    }

    /**
     * Tests that an exception is thrown if the index given is larger than the
     * highest number represented by the bytes.
     */
    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public final void isSelectedTooBig() {
        byte[] selectionBytes = {ON_BITS};
        PcrSelection pcrSelection = new PcrSelection(selectionBytes);
        pcrSelection.isPcrSelected(
                pcrSelection.getPcrSelect().length * NUM_BITS_IN_BYTE + 1);
    }

    /**
     * Tests that a long value can be used to create a PcrSelection object. If
     * the long value 256 (b0000 00001 0000 0000) is used, the expected selected
     * PCR is 8.
     */
    @Test
    public final void testLongConstructor() {
        long testValue = 0x100;
        final String expectedMask = "000100";
        PcrSelection pcrSelection = new PcrSelection(testValue);
        for (int i = 0; i < (pcrSelection.getSizeOfSelect() * NUM_BITS_IN_BYTE);
                i++) {
            if (i == 8) {
                Assert.assertTrue(pcrSelection.isPcrSelected(i));
            } else {
                Assert.assertFalse(pcrSelection.isPcrSelected(i));
            }
        }
        Assert.assertEquals(pcrSelection.getMaskForTPM(), expectedMask);

    }

    /**
     * Tests that if the byte array representing the mask was created with a
     * byte array less than 3 characters, the mask created is still 6
     * characters.
     */
    @Test
    public final void testGetMaskLarge() {
        byte[] selectionBytes = {ON_BITS, ON_BITS};
        String expectedMask = "ffff00";
        PcrSelection pcrSelection = new PcrSelection(selectionBytes);
        Assert.assertEquals(pcrSelection.getMaskForTPM(), expectedMask);
    }

    /**
     * Tests that two PcrSelection objects are equal even if created from
     * different methods.
     */
    @Test
    public final void testEquals() {
        byte[] selectionBytes = {(byte) 0x80};
        byte[] expectedSelectionBytes = {(byte) 0x80, OFF_BITS, OFF_BITS};
        PcrSelection pcrSelectionFromArray = new PcrSelection(selectionBytes);
        PcrSelection pcrSelectionFromLong = new PcrSelection(1);
        Assert.assertEquals(pcrSelectionFromArray, pcrSelectionFromLong);
        Assert.assertTrue(pcrSelectionFromArray.isPcrSelected(0));
        Assert.assertTrue(pcrSelectionFromLong.isPcrSelected(0));
        Assert.assertEquals(
                pcrSelectionFromArray.getPcrSelect(), expectedSelectionBytes);
        Assert.assertEquals(
                pcrSelectionFromLong.getPcrSelect(), expectedSelectionBytes);
    }
}
