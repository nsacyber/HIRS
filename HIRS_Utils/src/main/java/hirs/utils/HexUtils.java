package hirs.utils;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;

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
}
