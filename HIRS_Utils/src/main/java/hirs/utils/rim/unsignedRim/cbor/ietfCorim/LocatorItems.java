package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import hirs.utils.signature.cose.Cbor.CborItems;

/**
 * Class to store CoRIM Locator map items.
 * <p>Defined in Section 4.1.3 of the IETF CoRIM specification.</p>
 *
 * <pre>
 * corim-locator-map = {
 *     &amp;(href: 0)        =&gt; uri / [ + uri ]
 *     ? &amp;(thumbprint: 1) =&gt; digest
 * }
 * </pre>
 */
public class LocatorItems extends CborItems {

    /**
     * Integer key for the href entry in the CoRIM locator map.
     */
    public static final int HREF_INT = 0;
    /**
     * Integer key for the thumbprint entry in the CoRIM locator map.
     */
    public static final int THUMBPRINT_INT = 1;
    /**
     * String key for the href entry in the CoRIM locator map.
     */
    public static final String HREF_STR = "href";
    /**
     * String key for the thumbprint entry in the CoRIM locator map.
     */
    public static final String THUMBPRINT_STR = "thumbprint";

    private static final String[][] INDEX_NAMES = {
            {Integer.toString(HREF_INT), HREF_STR},
            {Integer.toString(THUMBPRINT_INT), THUMBPRINT_STR }
    };
}
