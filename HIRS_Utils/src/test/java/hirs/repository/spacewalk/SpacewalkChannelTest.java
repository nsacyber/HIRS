package hirs.repository.spacewalk;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Unit tests for <code>SpacewalkChannel</code>.
 *
 */
public class SpacewalkChannelTest {

    private static final String SPACEWALK_BASE_URL_STRING = "http://earth.moon.spacewalk.unittest";
    private static final int CHANNEL_ID = 88;
    private static final int CHANNEL_PACKAGE_COUNT = 999;
    private static final String CHANNEL_LABEL = "my-label";
    private static final String CHANNEL_NAME = "test-name";
    private static final String CHANNEL_PROVIDER = "test-provider";
    private static final String CHANNEL_ARCH = "test-x86";

    private URL spacewalkBaseUrl = null;
    private Map<String, Object> validChannelMap = null;

    /**
     * Sets up test resources.
     *
     * @throws MalformedURLException
     *             if the URL is malformed
     */
    @BeforeClass
    public final void setup() throws MalformedURLException {
        spacewalkBaseUrl = new URL(SPACEWALK_BASE_URL_STRING);
        validChannelMap = new HashMap<>();

        validChannelMap.put("id", CHANNEL_ID);
        validChannelMap.put("label", CHANNEL_LABEL);
        validChannelMap.put("name", CHANNEL_NAME);
        validChannelMap.put("provider_name", CHANNEL_PROVIDER);
        validChannelMap.put("packages", CHANNEL_PACKAGE_COUNT);
        validChannelMap.put("arch_name", CHANNEL_ARCH);
    }

    /**
     * Verifies exception when channel map is not provided.
     *
     * @throws SpacewalkException
     *             if a Spacewalk error occurrs during the test.
     */
    @Test(
          expectedExceptions = NullPointerException.class,
          expectedExceptionsMessageRegExp = ".*channelMap.*null*")
    public void exceptionWithNullMap() throws SpacewalkException {
        new SpacewalkChannel(null, spacewalkBaseUrl);
    }

    /**
     * Verifies exception when channel map is not provided.
     *
     * @throws SpacewalkException
     *             if a Spacewalk error occurrs during the test.
     */
    @Test(
          expectedExceptions = NullPointerException.class,
          expectedExceptionsMessageRegExp = ".*url.*null*")
    public void exceptionWithNullUrl() throws SpacewalkException {
        new SpacewalkChannel(validChannelMap, null);
    }

    /**
     * Tests that the SpacewalkChannel throws an exception when the provided map is missing an
     * expected key/value pair.
     */
    @Test(
          expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = ".*packages*")
    public void parseInvalidChannelMap() {
        Map<String, Object> insufficientMap = new HashMap<String, Object>(validChannelMap);
        insufficientMap.remove("packages");

        new SpacewalkChannel(insufficientMap, spacewalkBaseUrl);
    }

    /**
     * Tests that the SpacewalkChannel can parse a valid map.
     */
    @Test
    public void parseValidChannelMap() {
        SpacewalkChannel channel = new SpacewalkChannel(validChannelMap, spacewalkBaseUrl);
        Assert.assertEquals(channel.getId(), CHANNEL_ID);
        Assert.assertEquals(channel.getLabel(), CHANNEL_LABEL);
        Assert.assertEquals(channel.getName(), CHANNEL_NAME);
        Assert.assertEquals(channel.getProviderName(), CHANNEL_PROVIDER);
        Assert.assertEquals(channel.getPackageCount(), CHANNEL_PACKAGE_COUNT);
        Assert.assertEquals(channel.getArchitectureName(), CHANNEL_ARCH);
    }
}
