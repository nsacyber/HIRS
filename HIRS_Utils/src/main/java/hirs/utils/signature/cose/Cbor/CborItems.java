package hirs.utils.signature.cose.Cbor;

/**
 * Support class for handling CBOR (rfc 8949) Items with a map.
 * Classes that extend this class must populate the indexNames [String index,String itemName] array
 * where i is the numerical index converted to a string and  iem name is taken from the specification
 */
public class CborItems {

    /**
     * Default item name.
     */
    public static final String UNKNOWN_STR = "Unknown";
    /**
     * Default item id.
     */
    public static final int UNKNOWN_INT = 99;
    /**
     * Array of item names.
     */
    private static final String[][] INDEX_NAMES = new String[0][0];

    /**
     * Default constructor.
     */
    protected CborItems() {
    }

    /**
     * Converts the Item name to an item id.
     *
     * @param itemName item name
     * @return id of the item.
     */
    public static int getIndex(final String itemName) {
        for (int i = 0; i < INDEX_NAMES.length; i++) {
            if (itemName.compareToIgnoreCase(INDEX_NAMES[i][1]) == 0) {
                return i;
            }
        }
        return UNKNOWN_INT;
    }

    /**
     * Searches for an Rfc 9393 specified index and returns the item name associated with the index.
     *
     * @param index int rfc 939 sepcified index value
     * @return String item name associated with the index
     */
    public static String getItemName(final int index) {
        for (int i = 0; i < INDEX_NAMES.length; i++) {
            if (index == Integer.parseInt(INDEX_NAMES[i][0])) {
                return INDEX_NAMES[i][1];
            }
        }
        return UNKNOWN_STR;
    }
}
