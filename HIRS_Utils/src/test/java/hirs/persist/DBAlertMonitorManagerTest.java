package hirs.persist;

import hirs.alert.JsonAlertService;
import hirs.data.persist.SpringPersistenceTest;
import hirs.data.persist.alert.AlertMonitor;
import hirs.data.persist.alert.AlertServiceConfig;
import hirs.data.persist.alert.JsonAlertMonitor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Tests for the DBAlertMonitorManager.
 */
public class DBAlertMonitorManagerTest extends SpringPersistenceTest {

    private static final Logger LOGGER = LogManager.getLogger(DBAlertMonitorManagerTest.class);

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an in-memory database that
     * is used for testing. Sets up an initial Alert Service equivalent to the HIRS SystemConfig
     */
    @BeforeClass
    public final void beforeClass() {
        LOGGER.debug("retrieving session factory");
        // Add JSON Alert Service default, should be set in SystemInit
        LOGGER.debug("creating temporary Alert Service Config");
        DBManager<AlertServiceConfig> alertSrvConf;
        alertSrvConf = new DBManager<>(AlertServiceConfig.class, sessionFactory);
        AlertServiceConfig snmpConfig = new AlertServiceConfig(JsonAlertService.NAME);
        snmpConfig.disable();
        alertSrvConf.save(snmpConfig);
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
        DBUtility.removeAllInstances(sessionFactory, AlertMonitor.class);
    }

    /**
     * Test for the deleteAlertMonitor method.
     */
    @Test
    public final void deleteAlertMonitor() {
        LOGGER.debug("creating DBAlertMonitorManager");
        DBAlertMonitorManager dbamm = new DBAlertMonitorManager(sessionFactory);
        LOGGER.debug("creating an Alert Monitor");
        JsonAlertMonitor monitor = new JsonAlertMonitor("test");
        monitor.setName("TestAM1");
        dbamm.saveAlertMonitor(monitor);
        LOGGER.debug("saving an Alert Monitor");
        AlertMonitor monitor2 = dbamm.get("TestAM1");
        Assert.assertEquals(monitor2.getName(), "TestAM1");
        LOGGER.debug("deleting an Alert Monitor");
        dbamm.deleteAlertMonitor("TestAM1");
        AlertMonitor monitor3 = dbamm.get("TestAM1");
        Assert.assertNull(monitor3);
    }

    /**
     * Test for the getAlertMonitor method.
     */
    @Test
    public final void getAlertMonitor() {
        DBAlertMonitorManager dbamm = new DBAlertMonitorManager(sessionFactory);
        JsonAlertMonitor monitor = new JsonAlertMonitor("test");
        monitor.setName("TestAM1");
        LOGGER.debug("saving an Alert Monitor");
        dbamm.saveAlertMonitor(monitor);
        LOGGER.debug("retrieving an Alert Monitor");
        AlertMonitor monitor2 = dbamm.get("TestAM1");
        Assert.assertEquals(monitor2.getName(), "TestAM1");
    }

    /**
     * Test for the getAlertMonitorList method.
     */
    @Test
    public final void getAlertMonitorList() {
        DBAlertMonitorManager dbamm = new DBAlertMonitorManager(sessionFactory);
        LOGGER.debug("Adding multiple Alert Monitors");
        JsonAlertMonitor monitor = new JsonAlertMonitor("test");
        List<AlertMonitor> monList;
        monitor.setName("TestAM1");
        dbamm.saveAlertMonitor(monitor);
        JsonAlertMonitor monitor2 = new JsonAlertMonitor("test");
        monitor2.setName("TestAM2");
        dbamm.saveAlertMonitor(monitor2);
        LOGGER.debug("Reterieving  an Alert Monitor List");
        monList = dbamm.getAlertMonitorList(AlertMonitor.class);

        Assert.assertEquals(monList.get(0).getName(), "TestAM1");
        Assert.assertEquals(monList.get(1).getName(), "TestAM2");

    }

    /**
     * Test for the saveAlertMonitor method.
     */
    @Test
    public final void saveAlertMonitor() {
        DBAlertMonitorManager dbamm = new DBAlertMonitorManager(sessionFactory);
        JsonAlertMonitor monitor = new JsonAlertMonitor("test");
        monitor.setName("TestAM1");
        dbamm.saveAlertMonitor(monitor);

        AlertMonitor getAm = dbamm.getAlertMonitor("TestAM1");
        String getName = getAm.getName();

        Assert.assertEquals(getName, "TestAM1");
    }

    /**
     * Test for the updateAlertMonitor method.
     */
    @Test
    public final void updateAlertMonitor() {
        DBAlertMonitorManager dbamm = new DBAlertMonitorManager(sessionFactory);
        JsonAlertMonitor monitor = new JsonAlertMonitor("test");
        monitor.setName("TestAM1");
        Assert.assertEquals("TestAM1", monitor.getName());
        dbamm.saveAlertMonitor(monitor);
        LOGGER.debug("Updating an Alert Monitor");
        monitor.setName("TestAM2");
        dbamm.updateAlertMonitor(monitor);

        LOGGER.debug("Verifying changes to the updated Alert Monitor");
        AlertMonitor getAm = dbamm.getAlertMonitor("TestAM2");
        String getName = getAm.getName();
        Assert.assertNotEquals("SNMP", getName);
        Assert.assertEquals(getName, "TestAM2");
    }
}
