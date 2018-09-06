package hirs.repository.spacewalk;

import java.net.URL;
import java.util.Map;

/**
 * Class representing a Spacewalk channel.
 *
 */
public final class SpacewalkChannel {

    private final int id;
    private final String label;
    private final String name;
    private final String providerName;
    private final int packageCount;
    private final String architectureName;
    private final URL spacewalkUrl;

    /**
     * Constructs a Spacewalk channel using a map. The expected keys are as defined in the Spacewalk
     * 2.4 API.
     *
     * @param channelMap
     *            the map of key/value pairs of spacewalk channel fields.
     * @param url
     *            the URL for Spacewalk.
     * @throws NullPointerException
     *             if any argument passed in is null
     */
    public SpacewalkChannel(final Map<String, Object> channelMap, final URL url)
                    throws NullPointerException {

        if (null == channelMap) {
            throw new NullPointerException("channelMap is null");
        }
        if (null == url) {
            throw new NullPointerException("url is null");
        }

        spacewalkUrl = url;
        id = (int) tryGet(channelMap, "id");
        label = tryGet(channelMap, "label").toString();
        name = tryGet(channelMap, "name").toString();
        providerName = tryGet(channelMap, "provider_name").toString();
        packageCount = (int) tryGet(channelMap, "packages");
        architectureName = tryGet(channelMap, "arch_name").toString();
    }

    /**
     * Gets this channel's ID.
     *
     * @return the ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets this channel's label.
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Gets this channel's name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets this channel's provider name.
     *
     * @return the provider name
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * Gets this channel's package count.
     *
     * @return the package count
     */
    public int getPackageCount() {
        return packageCount;
    }

    /**
     * Gets this channel's architecture name.
     *
     * @return the provider name
     */
    public String getArchitectureName() {
        return architectureName;
    }

    /**
     * Gets this channel's associated base Spacewalk URL.
     *
     * @return the provider name
     */
    public URL getSpacewalkUrl() {
        return spacewalkUrl;
    }

    private Object tryGet(final Map<String, Object> map, final Object key) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException("map does not contain the key: " + key);
        }
        return map.get(key);
    }
}
