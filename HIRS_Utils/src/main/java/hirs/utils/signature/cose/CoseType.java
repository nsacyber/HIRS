package hirs.utils.signature.cose;

import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.CoswidItems;

/**
 * Helper class for processing rfc 9052 (COSE) defined tags.
 */
public class CoseType {
    /** COSE Defined CBOR tag for cose-sign. */
    public static final int COSE_SIGN = 98;
    /** COSE Defined CBOR tag for cose-sign1. */
    public static final int COSE_SIGN_1 = 18;
    /** COSE Defined CBOR tag for cose-encrypt. */
    public static final int COSE_ENCRYPT = 96;
    /** COSE Defined CBOR tag for cose-encrypt0. */
    public static final int COSE_ENCRYPT_0 = 16;
    /** COSE Defined CBOR tag for cose-mac. */
    public static final int COSE_MAC = 97;
    /** COSE Defined CBOR tag for cose-mac0. */
    public static final int COSE_MAC_0 = 17;

    private static final String[][] INDEX_NAMES = {
            {"98", "cose-sign"},
            {"18", "cose-sign1"},
            {"96", "cose-encrypt"},
            {"16", "cose-encrypt0"},
            {"97", "cose-mac"},
            {"17", "cose-mac0"}
    };

    /**
     * Searches Rfc 9393 Items Names for match to a specified item name and returns the index.
     * @param coseType  Iem Name specified in rfc 8152
     * @return int tag of the cose type
     */
    public int getType(final String coseType) {
        int algId = 0;
        for (int i = 0; i < INDEX_NAMES.length; i++) {
            if (coseType.compareToIgnoreCase(INDEX_NAMES[i][1]) == 0) {
                return i;
            }
        }
        return CoswidItems.UNKNOWN_INT;
    }
    /**
     * Searches for an Rfc 8152 specified index and returns the item name associated with the index.
     * @param index int rfc 8152 specified index value
     * @return String item name associated with the index
     */
    public static String getItemName(final int index) {
        for (int i = 0; i < INDEX_NAMES.length; i++) {
            if (index == Integer.parseInt(INDEX_NAMES[i][0])) {
                return INDEX_NAMES[i][1];
            }
        }
        return CoswidItems.UNKNOWN_STR;
    }

    /**
     * Determines if given int is a defined COSE tag.
     * @param tag possible tag
     * @return true if the int provided is a defined COSE tag.
     */
    public static boolean isCoseTag(final int tag) {
        for (int i = 0; i < INDEX_NAMES.length; i++) {
            if (tag == Integer.parseInt(INDEX_NAMES[i][0])) {
                return true;
            }
        }
        return false;
    }
}
