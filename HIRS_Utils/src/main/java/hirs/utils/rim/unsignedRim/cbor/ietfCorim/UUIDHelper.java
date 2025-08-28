package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Helper class to aid with UUID serialization or deserialization.
 */
public final class UUIDHelper {

    private static final int UUID_BYTE_LENGTH = 16;

    private UUIDHelper() {
        // Prevent instantiation
    }

    /**
     * Converts a given UUID to a byte array.
     *
     * @param uuid The UUID to convert.
     * @return A byte array containing the UUID bytes.
     */
    public static byte[] toBytes(final UUID uuid) {
        @SuppressWarnings("MagicNumber")
        ByteBuffer bb = ByteBuffer.allocate(UUID_BYTE_LENGTH);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
