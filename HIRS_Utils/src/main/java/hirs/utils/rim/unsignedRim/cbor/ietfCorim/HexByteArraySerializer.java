package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Custom serialization helper class for Jackson printing of byte arrays.
 */
public class HexByteArraySerializer extends StdSerializer<byte[]> {
    /** Default constructor. */
    public HexByteArraySerializer() {
        super(byte[].class);
    }

    /**
     * Serializes a given byte array to hexadecimal.
     *
     * @param value The input array of bytes.
     * @param gen The {@link JsonGenerator} used for printing.
     * @param provider The provider used for serialization.
     */
    @Override
    public void serialize(final byte[] value, final JsonGenerator gen,
                          final SerializerProvider provider) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (byte b : value) {
            sb.append(String.format("%02x", b));
        }
        gen.writeString(sb.toString());
    }
}
