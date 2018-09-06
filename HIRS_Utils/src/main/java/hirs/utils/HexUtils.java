package hirs.utils;

/**
 * Utilities for working with hex strings and byte arrays.
 */
public final class HexUtils {

    /**
     * The mathematical base for the hexadecimal representation.
     */
    public static final int HEX_BASIS = 16;

    /**
     * An integer representation of the byte 0xff or 255.
     */
    public static final int FF_BYTE = 0xff;

    private HexUtils() { }

    /**
     * Converts a binary hex string to a byte array.
     * @param s string to convert
     * @return byte array representation of s
     */
    public static byte[] hexStringToByteArray(final String s) {
        int sizeInt = s.length() / 2;
        byte[] returnArray = new byte[sizeInt];
        String byteVal;
        for (int i = 0; i < sizeInt; i++) {
            int index = 2 * i;
            byteVal = s.substring(index, index + 2);
            returnArray[i] = (byte) (Integer.parseInt(byteVal, HEX_BASIS));
        }
        return returnArray;
    }

    /**
     * Converts a byte array to a hex represented binary string.
     * @param b byte array to convert
     * @return hex string representation of array
     */
    public static String byteArrayToHexString(final byte[] b) {
        StringBuffer sb = new StringBuffer();
        String returnStr = "";
        for (int i = 0; i < b.length; i++) {
            String singleByte = Integer.toHexString(b[i] & FF_BYTE);
            if (singleByte.length() != 2) {
                singleByte = "0" + singleByte;
            }
            returnStr = sb.append(singleByte).toString();
        }
        return returnStr;
    }

    /**
     * Converts an individual hex string to an integer.
     * @param s an individual hex string
     * @return an integer representation of a hex string
     */
    public static Integer hexToInt(final String s) {
        Integer i = Integer.parseInt(s, HEX_BASIS);
        return i;
    }

    /**
     * Takes a byte array returns a subset of the array.
     * @param b the array to take a subset of
     * @param start the first index to copy
     * @param end the last index to copy (inclusive)
     * @return a new array of bytes from start to end
     */
    public static byte[] subarray(final byte[] b, final int start, final int end) {
        byte[] copy = new byte[end - start + 1];
        System.arraycopy(b, start, copy, 0, end - start + 1);
        return copy;
    }
}
