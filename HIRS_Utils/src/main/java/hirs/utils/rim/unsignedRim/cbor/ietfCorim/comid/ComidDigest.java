package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import com.fasterxml.jackson.annotation.JsonProperty;
import hirs.utils.rim.unsignedRim.common.IanaHashAlg;
import lombok.Getter;
import lombok.Setter;

/**
 *  Class that processes a {@code digest} containing hash information relevant to CoMID measurements. See
 *  Section 7.7 of the IETF CoRIM specification.
 */
@Getter
@Setter
public class ComidDigest {
    /** Corresponds to the {@code alg} field, identifying the digest algorithm used. Available algorithms are
     * detailed in the IANA Hash Algorithm Registry.
     * @see <a href="https://www.iana.org/assignments/named-information/named-information.xhtml">IANA Hash
     * Algorithm Registry</a>*/
    @JsonProperty("alg")
    private IanaHashAlg alg;
    /** Corresponds to the {@code val} field. An array of bytes containing the digest value. */
    private byte[] val;

    /**
     * Parses a {@code ComidDigest} from a given {@link CBORItem}.
     * @param comidDigest The item to parse.
     */
    public ComidDigest(final CBORItem comidDigest) {
        var iter = ((CBORItemList) comidDigest).getItems().iterator();

        // alg can be either int or text (see section 7.7 of the IETF CoRIM specification)
        var currAlg = iter.next().parse();
        if (currAlg instanceof String) {
            alg = IanaHashAlg.getAlgFromName((String) currAlg);
        } else {
            alg = IanaHashAlg.getAlgFromId((Integer) currAlg);
        }

        val = (byte[]) iter.next().parse(); // val
    }
}
