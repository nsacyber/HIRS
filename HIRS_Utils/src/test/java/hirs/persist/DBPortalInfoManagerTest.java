package hirs.persist;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import hirs.data.persist.info.PortalInfo;
import hirs.data.persist.enums.PortalScheme;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for the DBPortalInfoManager.
 */
public class DBPortalInfoManagerTest extends SpringPersistenceTest {
    private static final Logger LOGGER = LogManager.getLogger(DBPortalInfoManagerTest.class);

    /**
    * Initializes a <code>SessionFactory</code>. The factory is used for an in-memory database that
    * is used for testing. Sets up an initial Alert Service equivalent to the HIRS SystemConfig
    */
    @BeforeClass
    public final void beforeClass() {
        LOGGER.debug("retrieving session factory");
    }

    /**
    * Closes the <code>SessionFactory</code> from setup.
    */
    @AfterClass
    public final void afterClass() {
        LOGGER.debug("cleaning up AlertServiceConfigs closing session factory");
    }

    /**
    * Cleans up the DB after each Test.
    */
    @AfterMethod
    public final void afterMethod() {
        DBUtility.removeAllInstances(sessionFactory, PortalInfo.class);
    }

    /**
    * Test for the deletePortalInfo method.
    */
    @Test
    public final void deletePortalInfo() {
        final PortalScheme scheme = PortalScheme.HTTPS;

        LOGGER.debug("creating DBPortalInfoManager");
        PortalInfoManager dbpim = new DBPortalInfoManager(sessionFactory);

        LOGGER.debug("creating a Portal Info");
        PortalInfo info = new PortalInfo();
        info.setSchemeName(scheme);
        dbpim.savePortalInfo(info);

        LOGGER.debug("saving a Portal Info");
        PortalInfo info2 = dbpim.getPortalInfo(scheme);
        Assert.assertEquals(info2.getSchemeName(), scheme.name());

        LOGGER.debug("deleting a Portal Info");
        dbpim.deletePortalInfo(scheme);

        PortalInfo info3 = dbpim.getPortalInfo(scheme);
        Assert.assertNull(info3);
    }

    /**
    * Test for the getPortalInfo method.
    */
    @Test
    public final void getPortalInfo() {
        final PortalScheme scheme = PortalScheme.HTTPS;

        PortalInfoManager dbpim = new DBPortalInfoManager(sessionFactory);
        PortalInfo info = new PortalInfo();
        info.setSchemeName(scheme);

        LOGGER.debug("saving a Portal Info");
        dbpim.savePortalInfo(info);

        LOGGER.debug("retrieving a Portal Info");
        PortalInfo info2 = dbpim.getPortalInfo(scheme);
        Assert.assertEquals(info2.getSchemeName(), scheme.name());
    }

    /**
    * Test for the savePortalInfo method.
    */
    @Test
    public final void savePortalInfo() {
        final PortalScheme scheme = PortalScheme.HTTPS;

        PortalInfoManager dbpim = new DBPortalInfoManager(sessionFactory);
        PortalInfo info = new PortalInfo();
        info.setSchemeName(scheme);
        dbpim.savePortalInfo(info);


        PortalInfo info2 = dbpim.getPortalInfo(scheme);

        Assert.assertEquals(info2.getSchemeName(), scheme.name());
    }

    /**
    * Test for the updatePortalInfo method.
    */
    @Test
    public final void updatePortalInfo() {
        final PortalScheme scheme = PortalScheme.HTTPS;
        final int port = 127;

        PortalInfoManager dbpim = new DBPortalInfoManager(sessionFactory);
        PortalInfo info = new PortalInfo();
        info.setSchemeName(scheme);
        dbpim.savePortalInfo(info);

        LOGGER.debug("Updating a Portal Info");
        PortalInfo info2 = dbpim.getPortalInfo(scheme);
        info2.setPort(port);
        dbpim.updatePortalInfo(info2);

        LOGGER.debug("Verifying changes to the updated Portal Info");
        PortalInfo info3 = dbpim.getPortalInfo(scheme);
        Assert.assertEquals(info3.getPort(), port);
    }

    /**
     * Test for the getPortalUrlBase static method.
     * @throws Exception To report problems.
     */
    @Test
    public final void testGetPortalUrl() throws Exception {
        final PortalScheme scheme = PortalScheme.HTTPS;
        final int port = 127;
        final String contextName = "HIRS_Portal";
        final String address = "localhost";

        try {
            HashMap<String, String> envMap = new HashMap<>(System.getenv());
            setEnv(envMap);

            PortalInfoManager dbpim = new DBPortalInfoManager(sessionFactory);
            PortalInfo info = new PortalInfo();
            info.setSchemeName(scheme);
            info.setPort(port);
            info.setContextName(contextName);
            info.setIpAddress(address);
            dbpim.savePortalInfo(info);

            String url = dbpim.getPortalUrlBase();
            Assert.assertEquals(url, "https://localhost:127/HIRS_Portal/");
            Assert.assertEquals(url, URI.create(url).toString());

            String urlExtension = "jsp/alerts.jsp?UUID=1342-ABCD";
            Assert.assertEquals(url + urlExtension, URI.create(url + urlExtension).toString());
        } finally {
            // Unset the process environment variable for other tests.
            HashMap<String, String> envMap = new HashMap<>(System.getenv());
            envMap.remove("HIRS_HIBERNATE_CONFIG");
            setEnv(envMap);
        }
    }

    /**
     * Test getPortalUrl works as expected when there is no PortalInfo object.
     * @throws Exception To report problems.
     */
    @Test
    public final void testGetPortalUrlNoPortalInfoObject() throws Exception {
        PortalInfoManager dbpim = new DBPortalInfoManager(sessionFactory);
        dbpim.getPortalInfo(PortalScheme.HTTPS);

        String url = dbpim.getPortalUrlBase();
        Assert.assertEquals(url, "Your_HIRS_Portal/");
        Assert.assertEquals(url, URI.create(url).toString());
    }

    /**
     * Set an environment variable for the process.
     * @param newenv envMap to use
     */
    @SuppressWarnings("unchecked")
    public static void setEnv(final Map<String, String> newenv) {
      try {
        Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
        Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
        theEnvironmentField.setAccessible(true);
        Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
        env.putAll(newenv);
        Field theCaseInsensitiveEnvironmentField =
                processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
        theCaseInsensitiveEnvironmentField.setAccessible(true);
        Map<String, String> cienv =
                (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
        cienv.putAll(newenv);
        } catch (ReflectiveOperationException e) {
          try {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
          } catch (ReflectiveOperationException e2) {
              LOGGER.error(e2.getMessage());
          }
        }
    }
}
