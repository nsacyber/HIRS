package hirs.utils.dice;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bouncycastle.asn1.ASN1BitString;

import java.util.HexFormat;

/**
 * An immutable, JSON-friendly bit string, preserving the count of unused bits in its last byte.
 * Bit values are raw evidence; their security meaning depends on the associated mask and profile.
 */
@EqualsAndHashCode
public final class DiceBitString {
    private final byte[] bytes;
    @Getter
    private final int padBits;

    /**
     * Copies a decoded ASN.1 bit string into a plain evidence value.
     * @param value the validated bit string
     */
    DiceBitString(final ASN1BitString value) {
        bytes = value.getBytes();
        padBits = value.getPadBits();
    }

    /**
     * Returns the bit-string contents with unused bits cleared.
     * @return a copy of the bytes
     */
    public byte[] getBytes() {
        return bytes.clone();
    }

    @Override
    public String toString() {
        return HexFormat.of().formatHex(bytes) + " (unused bits: " + padBits + ")";
    }
}
