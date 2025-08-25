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
        assertEquals(CoswidItems.getItemName(0), "tag-id");
        assertEquals(CoswidItems.getItemName(1), "software-name");
        assertEquals(CoswidItems.getItemName(2), "entity");
        assertEquals(CoswidItems.getItemName(3), "evidence");
        assertEquals(CoswidItems.getItemName(4), "link");
        assertEquals(CoswidItems.getItemName(5), "software-meta");
        assertEquals(CoswidItems.getItemName(6), "payload");
        assertEquals(CoswidItems.getItemName(7), "hash");
        assertEquals(CoswidItems.getItemName(8), "corpus");
        assertEquals(CoswidItems.getItemName(9), "patch");
        assertEquals(CoswidItems.getItemName(10), "media");
        assertEquals(CoswidItems.getItemName(11), "supplemental");
        assertEquals(CoswidItems.getItemName(12), "tag-version");
        assertEquals(CoswidItems.getItemName(13), "software-version");
        assertEquals(CoswidItems.getItemName(14), "version-scheme");
        assertEquals(CoswidItems.getItemName(15), "lang");
        assertEquals(CoswidItems.getItemName(16), "directory");
        assertEquals(CoswidItems.getItemName(17), "file");
        assertEquals(CoswidItems.getItemName(18), "process");
        assertEquals(CoswidItems.getItemName(19), "resource");
        assertEquals(CoswidItems.getItemName(20), "size");
        assertEquals(CoswidItems.getItemName(21), "file-version");
        assertEquals(CoswidItems.getItemName(22), "key");
        assertEquals(CoswidItems.getItemName(23), "location");
        assertEquals(CoswidItems.getItemName(24), "fs-name");
        assertEquals(CoswidItems.getItemName(25), "root");
        assertEquals(CoswidItems.getItemName(26), "path-elements");
        assertEquals(CoswidItems.getItemName(27), "process-name");
        assertEquals(CoswidItems.getItemName(28), "pid");
        assertEquals(CoswidItems.getItemName(29), "type");
        assertEquals(CoswidItems.getItemName(30), "Unassigned");
        assertEquals(CoswidItems.getItemName(31), "entity-name");
        assertEquals(CoswidItems.getItemName(32), "reg-id");
        assertEquals(CoswidItems.getItemName(33), "role");
        assertEquals(CoswidItems.getItemName(34), "thumbprint");
        assertEquals(CoswidItems.getItemName(35), "date");
        assertEquals(CoswidItems.getItemName(36), "device-id");
        assertEquals(CoswidItems.getItemName(37), "artifact");
        assertEquals(CoswidItems.getItemName(38), "href");
        assertEquals(CoswidItems.getItemName(39), "ownership");
        assertEquals(CoswidItems.getItemName(40), "rel");
        assertEquals(CoswidItems.getItemName(41), "media-type");
        assertEquals(CoswidItems.getItemName(42), "use");
        assertEquals(CoswidItems.getItemName(43), "activation-status");
        assertEquals(CoswidItems.getItemName(44), "channel-type");
        assertEquals(CoswidItems.getItemName(45), "colloquial-version");
        assertEquals(CoswidItems.getItemName(46), "description");
        assertEquals(CoswidItems.getItemName(47), "edition");
        assertEquals(CoswidItems.getItemName(48), "entitlement-data-required");
        assertEquals(CoswidItems.getItemName(49), "entitlement-key");
        assertEquals(CoswidItems.getItemName(50), "generator");
        assertEquals(CoswidItems.getItemName(51), "persistent-id");
        assertEquals(CoswidItems.getItemName(52), "product");
        assertEquals(CoswidItems.getItemName(53), "product-family");
        assertEquals(CoswidItems.getItemName(54), "revision");
        assertEquals(CoswidItems.getItemName(55), "summary");
        assertEquals(CoswidItems.getItemName(56), "unspsc-code");
        assertEquals(CoswidItems.getItemName(57), "unspsc-version");
        //CHECKSTYLE:ON: MagicNumber
    }
}
