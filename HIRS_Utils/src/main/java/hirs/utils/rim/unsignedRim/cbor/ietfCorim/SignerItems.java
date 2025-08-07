package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import hirs.utils.signature.cose.Cbor.CborItems;

/**
 * Class to store CoRIM Signer map items.
 * <p>Defined in Section 4.2.2.1 of the IETF CoRIM specification.</p>
 *
 * <pre>
 * corim-signer-map = {
 *     &amp;(signer-name: 0) =&gt; $entity-name-type-choice
 *     ? &amp;(signer-uri: 1) =&gt; uri
 *     * $$corim-signer-map-extension
 * }
 * </pre>
 */
public class SignerItems extends CborItems {
    /**
     * Integer key for the signer-name entry in the CoRIM signer map.
     */
    public static final int SIGNER_NAME_INT = 0;
    /**
     * Integer key for the signer-uri entry in the CoRIM signer map.
     */
    public static final int SIGNER_URI_INT = 1;
    /**
     * String key for the signer-name entry in the CoRIM signer map.
     */
    public static final String SIGNER_NAME_STR = "signer-name";
    /**
     * String key for the signer-uri entry in the CoRIM signer map.
     */
    public static final String SIGNER_URI_STR = "signer-uri";

    private static final String[][] INDEX_NAMES = {
            {Integer.toString(SIGNER_NAME_INT), SIGNER_NAME_STR},
            {Integer.toString(SIGNER_URI_INT), SIGNER_URI_STR}
    };

}
