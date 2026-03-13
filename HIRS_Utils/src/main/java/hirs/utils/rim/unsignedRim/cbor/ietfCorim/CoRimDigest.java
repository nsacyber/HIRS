package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import com.authlete.cbor.CBORByteArray;
import com.authlete.cbor.CBORInteger;
import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a {@code digest} containing hash information relevant to CoMID
 * measurements. See Section 7.7 of the IETF CoRIM specification.
 * <p>
 * Note that this is conceptually the same as
 * {@link hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.ComidDigest}, though this
 * class is used exclusively for CoRIM CBOR building.
 */
public class CoRimDigest {
    @Getter
    @Setter
    private int alg;
    private byte[] val;

    /**
     * Builds a CBOR representation of the digest.
     *
     * @return The CBOR object representing the digest.
     */
    public CBORItemList build() {
        final List<CBORItem> itemList = new ArrayList<>();
        itemList.add(new CBORInteger(alg)); // alg
        itemList.add(new CBORByteArray(val)); // val
        return new CBORItemList(itemList);
    }

    /**
     * Returns a copy of the byte array value.
     *
     * @return a defensive copy of val
     */
    public byte[] getVal() {
        return val.clone();
    }

    /**
     * Sets the byte array value using a defensive copy.
     *
     * @param val the byte array to set
     */
    public void setVal(final byte[] val) {
        this.val = val.clone();
    }

    /**
     * Returns a deep copy of this CoRimDigest.
     *
     * @return a new CoRimDigest with the same values
     */
    public CoRimDigest copy() {
        CoRimDigest copy = new CoRimDigest();
        copy.setAlg(this.alg);
        copy.setVal(this.getVal());
        return copy;
    }
}
