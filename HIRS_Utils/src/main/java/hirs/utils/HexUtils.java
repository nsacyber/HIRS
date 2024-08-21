package hirs.utils;

import java.math.BigInteger;

/**
 * Utilities for working with hex strings and byte arrays.
 */
public final class HexUtils {

    /**
     * Default private constructor so checkstyles doesn't complain
     */
    private HexUtils() { }
    /**
     * The mathematical base for the hexadecimal representation.
     */
    public static final int HEX_BASIS = 16;

    /**
     * An integer representation of the byte 0xff or 255.
     */
    public static final int FF_BYTE = 0xff;

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
        StringBuilder sb = new StringBuilder();
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

    /**
     * Takes in a byte array and reverses the order.
     * @param in  byte array to reverse
     * @return reversed byte array
     */
   public static byte[] leReverseByte(final byte[] in) {
        byte[] finished = new byte[in.length];
          for (int i = 0; i < finished.length; i++) {
             finished[i] = in[(in.length - 1) - i];
         }
      return finished;
    }

    /**
     * Takes in a byte array and reverses the order then converts to an int.
     * @param in  byte array to reverse
     * @return integer that represents the reversed byte array
     */
    public static int leReverseInt(final byte[] in) {
        byte[] finished = leReverseByte(in);
        return new BigInteger(finished).intValue();
    }

    /**
     * Takes in a byte array of 4 bytes and returns a long.
     * @param bytes  byte array to convert
     * @return long representation of the bytes
     */
    public static long bytesToLong(final byte[] bytes) {
      BigInteger lValue = new BigInteger(bytes);

      return  lValue.abs().longValue();
    }
}
