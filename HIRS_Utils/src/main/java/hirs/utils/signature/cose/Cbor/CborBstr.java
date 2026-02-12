package hirs.utils.signature.cose.Cbor;

import java.nio.ByteBuffer;

/**
 * Supports COSE rfc 9052 by decoding encoded CBOR structures in a Byte String (CBOR Major Type 2)
 * "The payload is wrapped in a bstr (Byte string - major type 2)"
 * Note: use getContent() to retrieve the data with the byteSting encoding stripped off.
 */
public class CborBstr {
    private static final int typeMask = 0xE0;
    private static final int infoMask = 0x1F;
    private static final int shiftOffset = 0x05;
    private static final int byteStringType = 0x02;
    private static final int byteStringLength = 0x03;
    private static final int coseNilByte = 0xa0; // Cose defined nil byte for empty payloads.
    private byte[] contents = null;

    /**
     * Constructor for the Cbor Byte String.
     *
     * @param data data holding the Cbor Byte String data.
     */
    public CborBstr(final byte[] data) {

        byte type = data[0];
        // Check if byte 0 is of major type 0x02 (Byte String)
        byte cborType = (byte) ((type & typeMask) >> shiftOffset);
        if (cborType != byteStringType) {
            throw new RuntimeException("Byte Array Decode Error, expecting a byte String (Type 2) but found "
                    + cborType);
        }
        contents = new byte[data.length - byteStringLength];
        System.arraycopy(data, byteStringLength, contents, 0, data.length - byteStringLength);
    }

    /**
     * Checks to see if byte array is a string.
     *
     * @param data byte array holding the cbor data to check.
     * @return true if the byte array holds a string.
     */
    public static boolean isByteString(final byte[] data) {
        byte type = data[0];
        // Check if byte 0 is of major type 0x02 (Byte String)
        byte cborType = (byte) ((type & typeMask) >> shiftOffset);
        return cborType == byteStringType;
    }

    /**
     * Checks to see if a byte array is empty.
     *
     * @param data byte array to check.
     * @return true of the byte array is empty.
     */
    public static boolean isEmptyByteString(final byte[] data) {
        if (!isByteString(data)) {
            return false;
        }
        // per the cose spec 0xa0 is equivalent to {}
        return (data[3] & 0xFF) == coseNilByte;
    }

    /**
     * Processes byte string length rfc 8489.
     *
     * @param data byte array representation of the data
     * @return length of the byte string in bytes
     */
    public static int getByteStringLength(final byte[] data) {
        int length = 0;
        byte type = data[0];
        byte tagInfo = (byte) (type & infoMask);
        if (tagInfo < CborTagProcessor.CBOR_ONE_BYTE_UNSIGNED_INT) {
            length = tagInfo; // values 0 to 0x17
        } else if (tagInfo == CborTagProcessor.CBOR_ONE_BYTE_UNSIGNED_INT) {
            length = data[1];
        } else if (tagInfo == CborTagProcessor.CBOR_TWO_BYTE_UNSIGNED_INT) {
            byte[] tmpArray = {0, 0, data[1], data[2]};
            ByteBuffer buf = ByteBuffer.wrap(tmpArray);
            length = buf.getInt();
        } else if (tagInfo == CborTagProcessor.CBOR_FOUR_BYTE_UNSIGNED_INT) {
            byte[] tmpArray = {data[1], data[2], data[3], data[4]};
            ByteBuffer buf = ByteBuffer.wrap(tmpArray);
            length = buf.getInt();
        }
        return length;
    }

    /**
     * Determines length of the byte sting header per rfc 8489.
     *
     * @param data byte array holding cbor data
     * @return length of the byte string tag in bytes
     */
    public static int getByteStringTagLength(final byte[] data) {
        int length = 0;
        byte type = data[0];
        byte tagInfo = (byte) (type & infoMask);
        if (tagInfo < CborTagProcessor.CBOR_ONE_BYTE_UNSIGNED_INT) {
            length = 1; // values 0 to 0x17
        } else if (tagInfo == CborTagProcessor.CBOR_ONE_BYTE_UNSIGNED_INT) {
            length = 2;
        } else if (tagInfo == CborTagProcessor.CBOR_TWO_BYTE_UNSIGNED_INT) {
            length = 3;
        } else if (tagInfo == CborTagProcessor.CBOR_FOUR_BYTE_UNSIGNED_INT) {
            length = 4;
        }
        return length;
    }

    /**
     * Removes a preceding byte string from the byte array.
     *
     * @param data bate array holding cbor data.
     * @return new byte array with the byte string stripped off.
     */
    public static byte[] removeByteStringIfPresent(final byte[] data) {
        if (!isByteString(data)) {
            return data;
        }
        int length = getByteStringTagLength(data);
        byte[] contents = new byte[data.length - length];
        System.arraycopy(data, length, contents, 0, data.length - length);
        return contents;
    }

    /**
     * Returns a defensive copy of the contents byte array.
     *
     * @return a copy of the contents array
     */
    public byte[] getContents() {
        return contents.clone();
    }
}
