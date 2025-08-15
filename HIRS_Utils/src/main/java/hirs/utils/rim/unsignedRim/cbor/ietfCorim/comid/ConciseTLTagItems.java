package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import hirs.utils.signature.cose.Cbor.CborItems;

/**
 * Section 6.1 of the IETF CoRim specification.
 * <pre>
 *  concise-tl-tag = {
 *      &amp;(tag-identity: 0) => tag-identity-map
 *      &amp;(tags-list: 1) => [ + tag-identity-map ],
 *      &amp;(tl-validity: 2) => validity-map
 *    }
 * </pre>
 */
public class ConciseTLTagItems extends CborItems  {
    public static final int TAG_ID_INT = 0;
    public static final int TAGS_LIST_INT = 1;
    public static final int TL_VALIDITY_INT = 2;

    public static final String TAG_ID_STR = "tag-identity";
    public static final String TAG_LIST_STR = "tags-list";
    public static final String TL_VALIDITY_STR = "tl-validity";

    private static final String[][] INDEX_NAMES = {
            {Integer.toString(TAG_ID_INT), TAG_ID_STR },
            {Integer.toString(TAGS_LIST_INT), TAG_LIST_STR },
            {Integer.toString(TL_VALIDITY_INT), TL_VALIDITY_STR }
    };
}
