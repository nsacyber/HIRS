package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import hirs.utils.signature.cose.Cbor.CborItems;

/**
 * Class to store CoRIM Validity map items.
 * <p>Defined in Section 7.3 of the IETF CoRIM specification.</p>
 *
 * <pre>
 * validity-map = {
 *     ? &amp;(not-before: 0) =&gt; time
 *     &amp;(not-after: 1)   =&gt; time
 * }
 * </pre>
 */
public class ValidityMapItems extends CborItems {
    /**
     * Integer label for the "not-before" field.
     */
    public static final int NOT_BEFORE_INT = 0;
    /**
     * Integer label for the "not-after" field.
     */
    public static final int NOT_AFTER_INT = 1;
    /**
     * String label for the "not-before" field.
     */
    public static final String NOT_BEFORE_STR = "not-before";
    /**
     * String label for the "not-after" field.
     */
    public static final String NOT_AFTER_STR = "not-after";

    private static final String[][] INDEX_NAMES = {
            {Integer.toString(NOT_BEFORE_INT), NOT_BEFORE_STR},
            {Integer.toString(NOT_AFTER_INT), NOT_AFTER_STR}
    };
}
