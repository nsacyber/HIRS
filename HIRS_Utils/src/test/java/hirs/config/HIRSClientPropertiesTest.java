package hirs.config;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit test class for <code>HIRSClientProperties</code>.
 */
public class HIRSClientPropertiesTest {
    private static final String APPRAISER_NAME = "test_appraiser";

    /**
     * Tests getting the 'first' appraiser when none are defined.
     *
     * @throws IOException
     *              if there is an error while loading the properties
     */
    @Test
    public final void testGetSingleAppraiserEmptyFile() throws IOException {
        HIRSClientProperties prop = getEmptyHIRSClientProps();

        Assert.assertEquals(
                prop.getMainAppraiserConfig(),
                new HashMap<String, String>()
        );
    }

    /**
     * Tests getting a property of the 'first' appraiser.
     *
     * @throws IOException
     *              if there is an error while loading the properties
     */
    @Test
    public final void testGetSingleAppraiserConfig() throws IOException {
        HIRSClientProperties prop = getHIRSClientProps();

        Assert.assertEquals(
                prop.getMainAppraiserConfig().get(
                        HIRSClientProperties.APPRAISER_TRUSTSTORE_PASSWORD
                ),
                "password"
        );
    }

    /**
     * Tests getting a mapping of all appraisers from an empty file.
     *
     * @throws IOException
     *              if there is an error while loading the properties
     */
    @Test
    public final void testGetAppraiserConfigsEmpty() throws IOException {
        HIRSClientProperties prop = getEmptyHIRSClientProps();

        Assert.assertEquals(
                prop.getAppraiserConfigs(),
                new HashMap<String, Map<String, String>>()
        );
    }

    /**
     * Tests whether the appraiser returns the correct mapping.
     *
     * @throws IOException
     *              if there is an error while loading the properties
     */
    @Test
    public final void testGetAppraiserConfigs() throws IOException {
        HIRSClientProperties prop = getHIRSClientProps();

        Map<String, Map<String, String>> appraiserConfigs =
                prop.getAppraiserConfigs();

        Assert.assertEquals(appraiserConfigs.size(), 1);
        Assert.assertNotNull(appraiserConfigs.get(APPRAISER_NAME));
        Assert.assertEquals(
                appraiserConfigs.get(APPRAISER_NAME).get(
                        HIRSClientProperties.APPRAISER_TRUSTSTORE_PASSWORD
                ),
                "password"
        );
    }

    /**
     * Tests that the configuration options for a specific appraiser in a multiple appraiser config
     * file are parsed correctly.
     *
     * @throws IOException
     *             if there is an error while loading the properties
     */
    @Test
    public final void testMultipleAppraisersGetAppraiser1Values() throws IOException {
        HIRSClientProperties prop = getMultipleAppraiserProps();

        String appraiser1Name = "hirs-appraiser";
        String appraiser1URL =
                "https://hirs-appraiser:8443/HIRS_Appraiser/SOAPMessageProcessor?wsdl";
        String appraiser1Truststore = "/etc/hirs/provisioner/setup/TrustStore.jks";
        String appraiser1Password = "password";
        String appraiser1UUID = "d5606e40-366f-4693-a21f-12916cb1e733";
        String appraiser1IdentityCert =
                "/etc/hirs/provisioner/certs/d5607e40-366f-4693-a21f-12916cb1e733.cer";

        Map<String, String> appraiser1Config = prop.getAppraiserConfig(appraiser1Name);
        Assert.assertEquals(appraiser1Config.get(HIRSClientProperties.APPRAISER_URL),
                appraiser1URL);
        Assert.assertEquals(appraiser1Config.get(HIRSClientProperties.APPRAISER_TRUSTSTORE),
                appraiser1Truststore);
        Assert.assertEquals(
                appraiser1Config.get(HIRSClientProperties.APPRAISER_TRUSTSTORE_PASSWORD),
                appraiser1Password);
        Assert.assertEquals(appraiser1Config.get(HIRSClientProperties.APPRAISER_IDENTITY_UUID),
                appraiser1UUID);
        Assert.assertEquals(appraiser1Config.get(HIRSClientProperties.APPRAISER_IDENTITY_CERT),
                appraiser1IdentityCert);
    }

    /**
     * Tests that the configuration options for the second appraiser in a multiple appraiser config
     * file are parsed correctly.
     *
     * @throws IOException
     *             if there is an error while loading properties
     */
    @Test
    public final void testMultipleAppraisersGetAppraiser2Values() throws IOException {
        HIRSClientProperties prop = getMultipleAppraiserProps();

        String appraiser2Name = "test-appraiser";
        String appraiser2URL =
                "https://test-appraiser:8443/HIRS_Appraiser/SOAPMessageProcessor?wsdl";
        String appraiser2Truststore = "/etc/hirs/provisioner/setup/TrustStore.jks";
        String appraiser2Password = "password";
        String appraiser2UUID = "d5606e40-366f-4693-a21f-12916cb1e732";
        String appraiser2IdentityCert =
                "/etc/hirs/provisioner/certs/d5607e40-366f-4693-a21f-12916cb1e732.cer";

        Map<String, String> appraiser2Config = prop.getAppraiserConfig(appraiser2Name);
        Assert.assertEquals(appraiser2Config.get(HIRSClientProperties.APPRAISER_URL),
                appraiser2URL);
        Assert.assertEquals(appraiser2Config.get(HIRSClientProperties.APPRAISER_TRUSTSTORE),
                appraiser2Truststore);
        Assert.assertEquals(
                appraiser2Config.get(HIRSClientProperties.APPRAISER_TRUSTSTORE_PASSWORD),
                appraiser2Password);
        Assert.assertEquals(appraiser2Config.get(HIRSClientProperties.APPRAISER_IDENTITY_UUID),
                appraiser2UUID);
        Assert.assertEquals(appraiser2Config.get(HIRSClientProperties.APPRAISER_IDENTITY_CERT),
                appraiser2IdentityCert);
    }

    /**
     * Tests that the appraisers in a config file are retrieved in the order they appear in the list
     * in the config file.
     *
     * @throws IOException
     *             if an error occurs while parsing the file.
     */
    @Test
    public final void testMultipleAppraisersOrder() throws IOException {
        HIRSClientProperties prop = getMultipleAppraiserProps();
        String expectedAppraiserList = "hirs-appraiser,test-appraiser";
        String appraiserURL1 =
                "https://hirs-appraiser:8443/HIRS_Appraiser/SOAPMessageProcessor?wsdl";
        String appraiserURL2 =
                "https://test-appraiser:8443/HIRS_Appraiser/SOAPMessageProcessor?wsdl";

        List<Map<String, String>> list = prop.getAppraiserConfigurationList();

        String appraiserList = prop.getProperty(HIRSClientProperties.APPRAISER_LIST);

        Assert.assertEquals(appraiserList, expectedAppraiserList);
        Assert.assertEquals(list.get(0).get(HIRSClientProperties.APPRAISER_URL), appraiserURL1);
        Assert.assertEquals(list.get(1).get(HIRSClientProperties.APPRAISER_URL), appraiserURL2);

    }

    private HIRSClientProperties getHIRSClientProps() throws IOException {
        return new HIRSClientProperties("/config/TestHIRSClient.properties");
    }

    private HIRSClientProperties getEmptyHIRSClientProps() throws IOException {
        return new HIRSClientProperties("/config/Empty.properties");
    }

    private HIRSClientProperties getMultipleAppraiserProps() throws IOException {
        return new HIRSClientProperties("/config/TestMultipleAppraisers.properties");
    }
}
