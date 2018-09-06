package hirs.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Extends Properties to enable ease of working with structures inside of
 * .properties files.  Loads .properties files from classpath.
 */
public class ResourceProperties extends Properties {

    /**
     * Default Constructor.
     */
    public ResourceProperties() {
        super();
    }

    /**
     * Construct <code>ResourceProperties</code> from a resource name.
     * <p>
     * See {@link Class#getResourceAsStream(String)} for details on
     * how the file is loaded from the classpath.
     *
     * @param resourceName
     *          the name of the resource to load
     *
     * @throws IOException
     *          if an error is encountered while loading the resource
     */
    @SuppressFBWarnings(
            value = "UI_INHERITANCE_UNSAFE_GETRESOURCE",
            justification = "Not a problem since resourceName should be given"
                    + "relative to root of classpath")
    public ResourceProperties(final String resourceName) throws IOException {
        InputStream inStream = this.getClass().
                getResourceAsStream(resourceName);

        if (inStream == null) {
            throw new IOException("property file '" + resourceName
                    + "' not found");
        }
        try {
            load(inStream);
        } catch (IOException e) {
            throw new IOException("error reading '"
                    + resourceName + "'", e);
        } finally {
            try {
                inStream.close();
            } catch (IOException e) {
                throw new IOException("error closing '"
                        + resourceName + "'", e);
            }
        }
    }

    /**
     * Enables easy access to a list of items in the style of a .properties
     * file.  Given a property that holds comma-separated names, this method
     * constructs a mapping of items names to their properties and values.
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
     * If this method is called with the parameter "hirs.client.appraisers",
     * a Map with keys 'test_appraiser' and 'another_test_appraiser' will be
     * returned, with their values as maps holding their respective properties
     * and values (url, truststore.)
     *
     * @param listProperty
     *          the name of the property containing the list of item names
     *
     * @return
     *          a Map of form {item name, {property name, value}}
     */
    public final Map<String, Map<String, String>> getItems(
            final String listProperty) {
        Map<String, Map<String, String>> itemProperties = new HashMap<>();

        for (String itemName : getList(listProperty)) {
            itemProperties.put(
                    itemName,
                    getItemMap(String.format("%s.%s", listProperty, itemName))
            );
        }

        return itemProperties;
    }

    /**
     * Enables easy access to a list of items. Given a property that holds a
     * comma-separated list of values, this method constructs a list of those
     * values.
     *
     * <p>
     * For example, assume a .properties file contains:
     *
     * <code>
     * hirs.client.appraisers=test_appraiser,another_test_appraiser
     * </code>
     *
     * If this method is called with the parameter "hirs.client.appraisers", a
     * List with values 'test_appraiser' and 'another_test_appraiser' will
     * be returned.
     *
     * @param listProperty the name of the property containing the list of items
     * @return a list of values
     */
    public final List<String> getList(final String listProperty) {

        List<String> list = new ArrayList<>();

        if (listProperty == null) {
            return list;
        }

        String itemNames = getProperty(listProperty);
        if (itemNames == null) {
            return list;
        }

        itemNames = itemNames.trim();
        if (itemNames.length() == 0) {
            return list;
        }

        // Split & trim
        list.addAll(Arrays.asList(itemNames.split("\\s*,\\s*")));

        return list;
    }

    /**
     * Collects all properties/value pairs that begin with a base property name
     * into a single map.
     * <p>
     * For example, assume a .properties file contains:
     *
     * <code>
     * hirs.client.appraisers.test_appraiser.url=...
     * hirs.client.appraisers.test_appraiser.truststore=...
     * </code>
     *
     * If this method is called with "hirs.client.appraisers.test_appraiser" as
     * its parameter, a Map with keys 'url' and 'truststore' and their
     * respective values will be returned.
     *
     * @param basePropertyName
     *          the name of the base property to look for
     *
     * @return
     *          a Map of form {property name, value}
     */
    public final Map<String, String> getItemMap(final String basePropertyName) {
        Map<String, String> values = new HashMap<>();

        if (basePropertyName == null) {
            return values;
        }

        for (String propertyName : stringPropertyNames()) {
            if (propertyName.startsWith(basePropertyName)) {
                values.put(
                        propertyName.replaceAll(basePropertyName + ".", ""),
                        getProperty(propertyName)
                );
            }
        }
        return values;
    }

    /**
     * Quickly check if a property is set to "true".
     *
     * @param property to check
     * @return true if the property is set to case insensitive "true"
     */
    public final boolean propertyEnabled(final String property) {
        return "true".equalsIgnoreCase((String) get(property));
    }
}
