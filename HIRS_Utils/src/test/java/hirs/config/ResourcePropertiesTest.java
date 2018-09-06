package hirs.config;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for the ResourceProperties class.
 */
public class ResourcePropertiesTest {
    private static final String LIST_PROP = "list.items";
    private static final Map<String, Map<String, String>> EMPTY_ITEM_MAP =
            new HashMap<>();
    private static final Map<String, String> EMPTY_MAP =
            new HashMap<>();

    /**
     * Tests basic classpath loading functionality.
     *
     * @throws Exception if the properties can't be loaded
     */
    @Test
    public final void loadFromClasspath() throws Exception {
        ResourceProperties resourceProperties = getResourceProperties();
        Assert.assertNotNull(resourceProperties);
        Assert.assertEquals(
                resourceProperties.getProperty(LIST_PROP),
                "item_one,item_two"
        );
    }

    /**
     * Tests loading a nonexistent resource.
     *
     * @throws Exception every time as it can't load a nonexistent file
     */
    @Test(expectedExceptions = IOException.class)
    public final void loadNonexistentResource() throws Exception {
        new ResourceProperties("/config/FakeFile.properties");
    }

    /**
     * Tests getItems with a null parameter.
     *
     * @throws Exception if the properties can't be loaded or if test fails
     */
    @Test
    public final void testGetItemsNullParameter() throws Exception {
        Assert.assertEquals(
                getResourceProperties().getItems(null),
                EMPTY_ITEM_MAP
        );
    }

    /**
     * Tests getItems with a parameter for a nonexistent property.
     *
     * @throws Exception if the properties can't be loaded or if test fails
     */
    @Test
    public final void testGetItemsNonexistentParameter() throws Exception {
        Assert.assertEquals(
                getResourceProperties().getItems("list.fake_items"),
                EMPTY_ITEM_MAP
        );
    }

    /**
     * Tests getItems with an empty parameter.
     *
     * @throws Exception if the properties can't be loaded or if test fails
     */
    @Test
    public final void testGetItemsEmptyParameter() throws Exception {
        Assert.assertEquals(
                getResourceProperties().getItems("list.empty_items"),
                EMPTY_ITEM_MAP
        );
    }

    /**
     * Tests that getItems returns a map of item names to their property/value
     * pairs.
     *
     * @throws Exception if the properties can't be loaded or if test fails
     */
    @Test
    public final void testGetItems() throws Exception {
        Map<String, Map<String, String>> items = getResourceProperties()
                .getItems(LIST_PROP);

        Assert.assertNotNull(items);
        Assert.assertEquals(items.get("item_one").get("size"), "small");
        Assert.assertEquals(items.get("item_one").get("priority"), "high");
        Assert.assertEquals(items.get("item_two").get("size"), "large");
        Assert.assertEquals(items.get("item_two").get("priority"), "low");
    }

    /**
     * Tests getItemMap with a null parameter.
     *
     * @throws Exception if the properties can't be loaded or if the test fails
     */
    @Test
    public final void testGetItemMapNullParameter() throws Exception {
        Assert.assertEquals(
                getResourceProperties().getItemMap(null),
                EMPTY_ITEM_MAP
        );
    }

    /**
     * Tests getItemMap with a parameter for a nonexistent property.
     *
     * @throws Exception if the properties can't be loaded or if the test fails
     */
    @Test
    public final void testGetItemMapNonexistentParameter() throws Exception {
        Assert.assertEquals(
                getResourceProperties().getItemMap("list.items.fake_item"),
                EMPTY_ITEM_MAP
        );
    }

    /**
     * Tests that getItemMap returns a map of a single item's properties &
     * values.
     *
     * @throws Exception if the properties can't be loaded or if test fails
     */
    @Test
    public final void testGetItemMap() throws Exception {
        Map<String, String> itemMap = getResourceProperties().
                getItemMap("list.items.item_one");

        Assert.assertNotNull(itemMap);
        Assert.assertEquals(itemMap.get("size"), "small");
        Assert.assertEquals(itemMap.get("priority"), "high");
    }

    /**
     * Tests that getItemList returns a list of a single item's values.
     *
     * @throws Exception if the properties can't be loaded or if test fails
     */
    @Test
    public final void testGetList() throws Exception {

        List<String> itemList = getResourceProperties().getList(LIST_PROP);

        Assert.assertNotNull(itemList);
        Assert.assertEquals(itemList.get(0), "item_one");
        Assert.assertEquals(itemList.get(1), "item_two");
    }

    /**
     * Tests that getItemList returns a list of a single item's properly
     * trimmed values.
     *
     * @throws Exception if the properties can't be loaded or if test fails
     */
    @Test
    public final void testGetListTrim() throws Exception {

        List<String> itemList = getResourceProperties()
                .getList("list.items.spaces");

        Assert.assertNotNull(itemList);
        Assert.assertEquals(itemList.get(0), "item_one");
        Assert.assertEquals(itemList.get(1), "item_two");
        Assert.assertEquals(itemList.get(2), "item_three");
    }

    /**
     * Tests that propertyEnabled returns true or false correctly.
     *
     * @throws Exception if the properties can't be loaded or if test fails
     */
    @Test
    public final void testPropertyEnabled() throws Exception {

        ResourceProperties props = getResourceProperties();

        Assert.assertEquals(props.propertyEnabled("testEnabled.one"), true);
        Assert.assertEquals(props.propertyEnabled("testEnabled.two"), true);
        Assert.assertEquals(props.propertyEnabled("testEnabled.three"), false);
        Assert.assertEquals(props.propertyEnabled("testEnabled.four"), false);
        Assert.assertEquals(props.propertyEnabled("testEnabled.five"), false);
        Assert.assertEquals(props.propertyEnabled("testEnabled.dne"), false);
    }

    private ResourceProperties getResourceProperties() throws Exception {
        return new ResourceProperties("/config/BasicTest.properties");
    }
}
