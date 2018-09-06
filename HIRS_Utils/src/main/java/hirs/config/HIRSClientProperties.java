package hirs.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to read and use .properties files used to configure
 * <code>HIRSClient</code>.
 */
public class HIRSClientProperties extends ResourceProperties {

    private static final long serialVersionUID = -8795228911687870916L;

    /**
     * Represents supported auth mods.
     */
    public enum AuthMode {
        /**
         * Plain auth.
         */
        PLAIN,

        /**
         * SHA1 auth.
         */
        SHA1
    }

    /**
     * The default .properties file name.
     */
    public static final String DEFAULT_PROPERTY_FILENAME = "/HIRSClient.properties";

    /**
     * Property name for tpm collector enabled.
     */
    public static final String TPM_ENABLED = "hirs.client.collector.tpm";

    /**
     * Property name for ima collector enabled.
     */
    public static final String IMA_ENABLED = "hirs.client.collector.ima";

    /**
     * Property name for device info collector enabled.
     */
    public static final String DEVICE_ENABLED = "hirs.client.collector.device";

    /**
     * Property name for appraiser list.
     */
    public static final String APPRAISER_LIST = "hirs.client.appraisers";

    /**
     * Property name for appraiser url.
     */
    public static final String APPRAISER_URL = "url";

    /**
     * Property name for appraiser truststore.
     */
    public static final String APPRAISER_TRUSTSTORE = "truststore";

    /**
     * Property name for appraiser truststore password.
     */
    public static final String APPRAISER_TRUSTSTORE_PASSWORD = "truststorePassword";

    /**
     * Property name for appraiser identity UUID.
     */
    public static final String APPRAISER_IDENTITY_UUID = "identityUuid";

    /**
     * Property name for appraiser identity auth mode.
     */
    public static final String APPRAISER_IDENTITY_AUTH_MODE = "identityAuth.mode";

    /**
     * Property name for appraiser identity auth value.
     */
    public static final String APPRAISER_IDENTITY_AUTH_VALUE = "identityAuth.value";

    /**
     * Property name for appraiser identity cert.
     */
    public static final String APPRAISER_IDENTITY_CERT = "identityCert";

    /**
     * Property name for Portal url that will be used for golden baseline uploads.
     */
    public static final String GOLDEN_BASELINE_PORTAL_URL =
            "hirs.client.golden_baseline.portal_url";

    /**
     * Property name for the default golden baseline name.
     */
    public static final String GOLDEN_BASELINE_DEFAULT_NAME =
            "hirs.client.golden_baseline.default_name";

    /**
     * Constructs default <code>HIRSClientProperties</code>.
     *
     * @throws IOException
     *             if an error is encountered reading default properties file
     */
    public HIRSClientProperties() throws IOException {
        this(DEFAULT_PROPERTY_FILENAME);
    }

    /**
     * Constructs <code>HIRSClientProperties</code> from custom resource.
     *
     * @param resourceName
     *            the resource name of the .properties file to load
     *
     * @throws IOException
     *             if an error is encountered reading properties file
     */
    public HIRSClientProperties(final String resourceName) throws IOException {
        super(resourceName);
    }

    /**
     * Assembles a mapping of configs for each defined appraiser. Resulting map
     * will have the names of the appraiser as keys and their values will be
     * maps of their property/value pairs.
     * <p>
     * For example, assume a .properties file contains:
     *
     * <code>
     * hirs.client.appraisers=test_appraiser,another_test_appraiser
     *
     * hirs.client.appraisers.test_appraiser.url=...
     * hirs.client.appraisers.test_appraiser.truststore=...
     *
     * hirs.client.appraisers.another_test_appraiser.url=...
     * hirs.client.appraisers.another_test_appraiser.truststore=...
     * </code>
     *
     * The resulting map will contains two keys (test_appraiser and
     * another_test_appraiser), and their values will be maps of their url and
     * truststore property/value pairs.
     *
     * @return Map with structure {appraiser name, {property, value}}
     */
    public Map<String, Map<String, String>> getAppraiserConfigs() {
        return getItems(APPRAISER_LIST);
    }

    /**
     * This method will return the main appraiser in the configuration file.  For single appraiser
     * configurations, this will return the one appraiser.  For multiple appraiser configurations,
     * this will return the first appraiser in the list.
     * <p>
     * For example, assume a .properties file contains:
     *
     * <code>
     * hirs.client.appraisers=test_appraiser,test_appraiser2
     *
     * hirs.client.appraisers.test_appraiser.url=...
     * hirs.client.appraisers.test_appraiser.truststore=...
     * </code>
     *
     * The resulting map will have keys url and truststore with their respective
     * values for the first appraiser in the list, test_appraiser.
     *
     * @return Map with structure {property, value} or an empty map if no
     *         appraiser was found.
     */
    public final Map<String, String> getMainAppraiserConfig() {
        try {
            return this.getAppraiserConfigurationList().get(0);
        } catch (IndexOutOfBoundsException e) {
            return new HashMap<>();
        }
    }

    /**
     * Returns the config map for a particular appraiser given the appraiser hostname.
     *
     * @param appraiserHostname
     *            hostname
     * @return Map with structure {property, value} or an empty map if no appraiser was found.
     */
    public final Map<String, String> getAppraiserConfig(final String appraiserHostname) {
        return getItemMap(String.format("%s.%s", APPRAISER_LIST, appraiserHostname));
    }

    /**
     * Gets a list of the appraiser configurations in the order in which the FQDNs
     * are listed in the client.
     * @return the list of ordered appraiser configuration settings maps
     */
    public List<Map<String, String>> getAppraiserConfigurationList() {
        ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();

        for (String itemName : getList(APPRAISER_LIST)) {
            list.add(getItemMap(String.format("%s.%s", APPRAISER_LIST, itemName)));
        }

        return Collections.unmodifiableList(list);
    }

}
