package hirs.utils.rim.unsignedRim.cbor.ietfCoswid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class contains unit tests for the CoswidItems class.
 * <p>
 * It verifies the correctness of the index mapping and item name mapping.
 */
public class CoswidItemsTest {

    /**
     * Tests the getIndex() method of the CoswidItems class to ensure that
     * the correct index is returned for each item name.
     */
    @Test
    public final void testIndex() {
        assertEquals(0, CoswidItems.getIndex("tag-id"));
        assertEquals(1, CoswidItems.getIndex("software-name"));
        assertEquals(2, CoswidItems.getIndex("entity"));
        assertEquals(3, CoswidItems.getIndex("evidence"));
        assertEquals(4, CoswidItems.getIndex("link"));
        assertEquals(5, CoswidItems.getIndex("software-meta"));
        assertEquals(6, CoswidItems.getIndex("payload"));
        assertEquals(7, CoswidItems.getIndex("hash"));
        assertEquals(8, CoswidItems.getIndex("corpus"));
        assertEquals(9, CoswidItems.getIndex("patch"));
        assertEquals(10, CoswidItems.getIndex("media"));
        assertEquals(11, CoswidItems.getIndex("supplemental"));
        assertEquals(12, CoswidItems.getIndex("tag-version"));
        assertEquals(13, CoswidItems.getIndex("software-version"));
        assertEquals(14, CoswidItems.getIndex("version-scheme"));
        assertEquals(15, CoswidItems.getIndex("lang"));
        assertEquals(16, CoswidItems.getIndex("directory"));
        assertEquals(17, CoswidItems.getIndex("file"));
        assertEquals(18, CoswidItems.getIndex("process"));
        assertEquals(19, CoswidItems.getIndex("resource"));
        assertEquals(20, CoswidItems.getIndex("size"));
        assertEquals(21, CoswidItems.getIndex("file-version"));
        assertEquals(22, CoswidItems.getIndex("key"));
        assertEquals(23, CoswidItems.getIndex("location"));
        assertEquals(24, CoswidItems.getIndex("fs-name"));
        assertEquals(25, CoswidItems.getIndex("root"));
        assertEquals(26, CoswidItems.getIndex("path-elements"));
        assertEquals(27, CoswidItems.getIndex("process-name"));
        assertEquals(28, CoswidItems.getIndex("pid"));
        assertEquals(29, CoswidItems.getIndex("type"));
        assertEquals(30, CoswidItems.getIndex("Unassigned"));
        assertEquals(31, CoswidItems.getIndex("entity-name"));
        assertEquals(32, CoswidItems.getIndex("reg-id"));
        assertEquals(33, CoswidItems.getIndex("role"));
        assertEquals(34, CoswidItems.getIndex("thumbprint"));
        assertEquals(35, CoswidItems.getIndex("date"));
        assertEquals(36, CoswidItems.getIndex("device-id"));
        assertEquals(37, CoswidItems.getIndex("artifact"));
        assertEquals(38, CoswidItems.getIndex("href"));
        assertEquals(39, CoswidItems.getIndex("ownership"));
        assertEquals(40, CoswidItems.getIndex("rel"));
        assertEquals(41, CoswidItems.getIndex("media-type"));
        assertEquals(42, CoswidItems.getIndex("use"));
        assertEquals(43, CoswidItems.getIndex("activation-status"));
        assertEquals(44, CoswidItems.getIndex("channel-type"));
        assertEquals(45, CoswidItems.getIndex("colloquial-version"));
        assertEquals(46, CoswidItems.getIndex("description"));
        assertEquals(47, CoswidItems.getIndex("edition"));
        assertEquals(48, CoswidItems.getIndex("entitlement-data-required"));
        assertEquals(49, CoswidItems.getIndex("entitlement-key"));
        assertEquals(50, CoswidItems.getIndex("generator"));
        assertEquals(51, CoswidItems.getIndex("persistent-id"));
        assertEquals(52, CoswidItems.getIndex("product"));
        assertEquals(53, CoswidItems.getIndex("product-family"));
        assertEquals(54, CoswidItems.getIndex("revision"));
        assertEquals(55, CoswidItems.getIndex("summary"));
        assertEquals(56, CoswidItems.getIndex("unspsc-code"));
        assertEquals(57, CoswidItems.getIndex("unspsc-version"));
    }

    /**
     * Tests the getItemName() method of the CoswidItems class to ensure that
     * the correct item name is returned for each index.
     */
    @Test
    public final void testGetItemName() {
        //CHECKSTYLE:OFF: MagicNumber
        assertEquals("tag-id", CoswidItems.getItemName(0));
        assertEquals("software-name", CoswidItems.getItemName(1));
        assertEquals("entity", CoswidItems.getItemName(2));
        assertEquals("evidence", CoswidItems.getItemName(3));
        assertEquals("link", CoswidItems.getItemName(4));
        assertEquals("software-meta", CoswidItems.getItemName(5));
        assertEquals("payload", CoswidItems.getItemName(6));
        assertEquals("hash", CoswidItems.getItemName(7));
        assertEquals("corpus", CoswidItems.getItemName(8));
        assertEquals("patch", CoswidItems.getItemName(9));
        assertEquals("media", CoswidItems.getItemName(10));
        assertEquals("supplemental", CoswidItems.getItemName(11));
        assertEquals("tag-version", CoswidItems.getItemName(12));
        assertEquals("software-version", CoswidItems.getItemName(13));
        assertEquals("version-scheme", CoswidItems.getItemName(14));
        assertEquals("lang", CoswidItems.getItemName(15));
        assertEquals("directory", CoswidItems.getItemName(16));
        assertEquals("file", CoswidItems.getItemName(17));
        assertEquals("process", CoswidItems.getItemName(18));
        assertEquals("resource", CoswidItems.getItemName(19));
        assertEquals("size", CoswidItems.getItemName(20));
        assertEquals("file-version", CoswidItems.getItemName(21));
        assertEquals("key", CoswidItems.getItemName(22));
        assertEquals("location", CoswidItems.getItemName(23));
        assertEquals("fs-name", CoswidItems.getItemName(24));
        assertEquals("root", CoswidItems.getItemName(25));
        assertEquals("path-elements", CoswidItems.getItemName(26));
        assertEquals("process-name", CoswidItems.getItemName(27));
        assertEquals("pid", CoswidItems.getItemName(28));
        assertEquals("type", CoswidItems.getItemName(29));
        assertEquals("Unassigned", CoswidItems.getItemName(30));
        assertEquals("entity-name", CoswidItems.getItemName(31));
        assertEquals("reg-id", CoswidItems.getItemName(32));
        assertEquals("role", CoswidItems.getItemName(33));
        assertEquals("thumbprint", CoswidItems.getItemName(34));
        assertEquals("date", CoswidItems.getItemName(35));
        assertEquals("device-id", CoswidItems.getItemName(36));
        assertEquals("artifact", CoswidItems.getItemName(37));
        assertEquals("href", CoswidItems.getItemName(38));
        assertEquals("ownership", CoswidItems.getItemName(39));
        assertEquals("rel", CoswidItems.getItemName(40));
        assertEquals("media-type", CoswidItems.getItemName(41));
        assertEquals("use", CoswidItems.getItemName(42));
        assertEquals("activation-status", CoswidItems.getItemName(43));
        assertEquals("channel-type", CoswidItems.getItemName(44));
        assertEquals("colloquial-version", CoswidItems.getItemName(45));
        assertEquals("description", CoswidItems.getItemName(46));
        assertEquals("edition", CoswidItems.getItemName(47));
        assertEquals("entitlement-data-required", CoswidItems.getItemName(48));
        assertEquals("entitlement-key", CoswidItems.getItemName(49));
        assertEquals("generator", CoswidItems.getItemName(50));
        assertEquals("persistent-id", CoswidItems.getItemName(51));
        assertEquals("product", CoswidItems.getItemName(52));
        assertEquals("product-family", CoswidItems.getItemName(53));
        assertEquals("revision", CoswidItems.getItemName(54));
        assertEquals("summary", CoswidItems.getItemName(55));
        assertEquals("unspsc-code", CoswidItems.getItemName(56));
        assertEquals("unspsc-version", CoswidItems.getItemName(57));
        //CHECKSTYLE:ON: MagicNumber
    }
}
