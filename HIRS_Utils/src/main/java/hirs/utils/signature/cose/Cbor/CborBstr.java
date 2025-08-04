package hirs.utils.signature.cose.Cbor;

import lombok.Getter;

import java.nio.ByteBuffer;

/**
 * Supports COSE rfc 9052 by decoding encoded CBOR structures in a Byte String (CBOR Major Type 2)
 * "The payload is wrapped in a bstr (Byte string - major type 2)"
 * Note: use getContent() to retrieve the data with the byteSting encoding stripped off.
 */
public class CborBstr {
    @Getter
    private byte[] contents = null;
    private static int typeMask = 0xE0;
    private static int infoMask = 0x1F;
    private static int shiftOffset = 0x05;
    private static int byteStringType = 0x02;
    private static int byteStringLength = 0x03;
    private static int coseNilByte = 0xa0; // Cose defined nil byte for empty payloads.
    /**
     * Constructor for the Cbor Byte String.
     * @param data data holding the Cbor Byte String data.
     */
    public CborBstr(final byte[] data) {

        byte type = data[0];
        byte info = (byte) (type & infoMask);
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
     * @param data byte array holding the cbor data to check.
     * @return  true if the byte array holds a string.
     */
    public static boolean isByteString(final byte[] data) {
        byte type = data[0];
        byte info = (byte) (type & infoMask);
        // Check if byte 0 is of major type 0x02 (Byte String)
        byte cborType = (byte) ((type & typeMask) >> shiftOffset);
        if (cborType == byteStringType) {
            return true;
        }
        return false;
    }
    /**
     * Checks to see if a byte array is empty.
     * @param data  byte array to check.
     * @return true of the byte array is empty.
     */
    @SuppressWarnings("MagicNumber")
    public static boolean isEmptyByteString(final byte[] data) {
        if (!isByteString(data)) {
            return false;
        }
        // per the cose spec 0xa0 is equivalent to {}
        if (data[3] == coseNilByte) {
            return true;
        }
        return false;
    }
    /**
     * Processes byte string length rfc 8489.
     * @param data
     * @return length of the byte string in bytes
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static int getByteStringLength(final byte[] data) {
        int length = 0;
        byte type = data[0];
        byte tagInfo = (byte) (type & infoMask);
        if (tagInfo < CborTagProcessor.cborOneByteUnsignedInt) {
            length = tagInfo; // values 0 to 0x17
        } else if (tagInfo == CborTagProcessor.cborOneByteUnsignedInt) {
            length = (int) data[1];
        } else if (tagInfo == CborTagProcessor.cborTwoByteUnsignedInt) {
            byte[] tmpArray = {0, 0, data[1], data[2] };
            ByteBuffer buf = ByteBuffer.wrap(tmpArray);
            length = buf.getInt();
        } else if (tagInfo == CborTagProcessor.cborFourByteUnsignedInt) {
            byte[] tmpArray = {data[1], data[2], data[3], data[4]};
            ByteBuffer buf = ByteBuffer.wrap(tmpArray);
            length = buf.getInt();
        }
        return length;
    }
    /**
     * Determines length of the byte sting header per rfc 8489.
     * @param data byte array holding cbor data
     * @return length of the byte string tag in bytes
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static int getByteStringTagLength(final byte[] data) {
        int length = 0;
        byte type = data[0];
        byte tagInfo = (byte) (type & infoMask);
        if (tagInfo <  CborTagProcessor.cborOneByteUnsignedInt) {
            length = 1; // values 0 to 0x17
        } else if (tagInfo == CborTagProcessor.cborOneByteUnsignedInt) {
            length = 2;
        } else if (tagInfo == CborTagProcessor.cborTwoByteUnsignedInt) {
            length = 3;
        } else if (tagInfo == CborTagProcessor.cborFourByteUnsignedInt) {
            length = 4;
        }
        return length;
    }
    /**
     * Removes a preceeding byte string from the byte array.
     * @param data bate array holding cbor data.
     * @return new byte array with the byte string stripped off.
     */
    public static byte[] removeByteStringIfPresent(final byte[] data) {
        byte type = data[0];
        if (!isByteString(data)) {
            return data;
        }
        int length = getByteStringTagLength(data);
        byte[] contents = new byte[data.length - length];
        System.arraycopy(data, length, contents, 0, data.length - length);
        return contents;
    }
}
