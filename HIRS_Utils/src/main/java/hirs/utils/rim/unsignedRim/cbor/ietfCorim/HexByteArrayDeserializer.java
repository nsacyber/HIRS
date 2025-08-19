package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Custom deserializer helper class for Jackson to parse hex strings into byte arrays.
 */
public final class HexByteArrayDeserializer extends StdDeserializer<byte[]> {

    /** Default constructor. */
    public HexByteArrayDeserializer() {
        super(byte[].class);
    }

    /**
     * Deserializes a hex-encoded string from the JSON input into a byte array.
     *
     * @param p the {@link JsonParser} used to read the JSON content
     * @param ctxt the {@link DeserializationContext} provided by Jackson
     * @return the resulting byte array parsed from the hex string
     * @throws IOException if an I/O error occurs while reading from the parser
     */
    @Override
    public byte[] deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        String hexString = p.getText().trim();
        return hexStringToByteArray(hexString);
    }

    /**
     * Converts a hex string to a byte array.
     * Assumes the input string length is even and contains only valid hex characters.
     *
     * @param s the hex string
     * @return the byte array
     */
    private byte[] hexStringToByteArray(final String s) {
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(s.charAt(i), 16);
            int low = Character.digit(s.charAt(i + 1), 16);
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Invalid hex character in string");
            }
            data[i / 2] = (byte) ((high << 4) + low);
        }
        return data;
    }
}
