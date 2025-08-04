package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import hirs.utils.signature.cose.Cbor.CborItems;

/**
 * Class to store CoRIM metadata map items.
 * <p>Defined in Section 4.2.2 of the IETF CoRIM specification.</p>
 *
 * <pre>
 * corim-meta-map = {
 *     &amp;(signer: 0)               =&gt; corim-signer-map
 *     ? &amp;(signature-validity: 1) =&gt; validity-map
 * }
 * </pre>
 */
@SuppressWarnings("JavaDocVariable")
public class MetaItems extends CborItems {
    public static final int SIGNER_INT = 0;
    public static final int SIGNATURE_VALIDITY_INT = 1;

    public static final String SIGNER_STR = "href";
    public static final String SIGNATURE_VALIDITY_STR = "thumbprint";

    private static final String[][] INDEX_NAMES = {
            {Integer.toString(SIGNER_INT), SIGNER_STR},
            {Integer.toString(SIGNATURE_VALIDITY_INT), SIGNATURE_VALIDITY_STR}
    };
}
