package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a <i>raw value</i>. A raw value is the actual (non-hashed) value of an element. It can comprise
 * either a byte array alone, or include a <i>mask value</i> that selects the particular bits used to compare
 * during appraisal.
 * <p>
 * See section 5.1.4.1.4.6 of the IETF CoRIM specification.
 */
@Getter
@Setter
public class RawValue {
    /** The {@code value} field for a {@code tagged-masked-raw-value}, or otherwise the bytes pertaining to
     * the raw value.*/
    private byte[] value;
    /** The {@code mask} field for a {@code tagged-masked-raw-value}, containing the mask value used for
     * selecting bits to be compared during appraisal.*/
    private byte[] mask;

    /** Constructs an empty raw value. */
    public RawValue() {
        value = null;
        mask = null;
    }
}
