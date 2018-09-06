package hirs.persist;

import hirs.alert.JsonAlertService;
import hirs.data.persist.SpringPersistenceTest;
import hirs.data.persist.alert.AlertServiceConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Tests for the DBAlertServiceManager.
 */
public class DBAlertServiceConfigManagerTest extends SpringPersistenceTest {
    private static final Logger LOGGER =
            LogManager.getLogger(DBAlertServiceConfigManagerTest.class);

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an in-memory database that
     * is used for testing. Sets up an initial Alert Service equivalent to the HIRS SystemConfig
     */
    @BeforeClass
    public final void beforeClass() {
        LOGGER.debug("retrieving session factory");
        LOGGER.debug("creating temporary alert service config");
        DBManager<AlertServiceConfig> alertSrvConf = new DBManager<>(
                AlertServiceConfig.class, sessionFactory
        );
        AlertServiceConfig jsonConfig = new AlertServiceConfig(JsonAlertService.NAME);
        jsonConfig.disable();
        alertSrvConf.save(jsonConfig);
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public final void afterClass() {
    }

    /**
     * Cleans up the DB after each Test.
     */
    @AfterMethod
    public final void afterMethod() {
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final List<?> services = session.createCriteria(AlertServiceConfig.class).list();
        for (Object o : services) {
            //LOGGER.debug("deleting report: {}", o);
            session.delete(o);
        }
        LOGGER.debug("all Services removed");
        session.getTransaction().commit();

    }

    /**
     * Test for the getAlertService method.
     */
    @Test
    public final void getAlertService() {
        DBAlertServiceManager dbamm = new DBAlertServiceManager(sessionFactory);
        AlertServiceConfig service = new AlertServiceConfig(JsonAlertService.NAME);
        LOGGER.debug("saving an alert service");
        dbamm.saveAlertServiceConfig(service);
        LOGGER.debug("retrieving an Alert Service");
        AlertServiceConfig service2 = dbamm.getAlertServiceConfig(JsonAlertService.NAME);
        Assert.assertEquals(service2.getType(), JsonAlertService.NAME);
    }

    /**
     * Test for the getAlertServiceList method.
     */
    @Test
    public final void getAlertServiceList() {
        DBAlertServiceManager dbamm = new DBAlertServiceManager(sessionFactory);
        LOGGER.debug("Adding multiple alert services");
        AlertServiceConfig service = new AlertServiceConfig(JsonAlertService.NAME);
        List<AlertServiceConfig> monList;
        dbamm.saveAlertServiceConfig(service);

        LOGGER.debug("Reterieving  an Alert Service List");
        monList = dbamm.getAlertServiceConfigList(AlertServiceConfig.class);

        Assert.assertEquals(monList.get(0).getType(), JsonAlertService.NAME);

    }

    /**
     * Test for the saveAlertService method.
     */
    @Test
    public final void saveAlertService() {
        DBAlertServiceManager dbamm = new DBAlertServiceManager(sessionFactory);
        AlertServiceConfig service = new AlertServiceConfig(JsonAlertService.NAME);
        dbamm.saveAlertServiceConfig(service);

        AlertServiceConfig getAm = dbamm.getAlertServiceConfig(JsonAlertService.NAME);

        Assert.assertEquals(getAm.getType(), JsonAlertService.NAME);
    }

    /**
     * Test for the updateAlertService method.
     */
    @Test
    public final void updateAlertService() {
        DBAlertServiceManager dbamm = new DBAlertServiceManager(sessionFactory);
        AlertServiceConfig service = new AlertServiceConfig(JsonAlertService.NAME);
        service.disable();
        AlertServiceConfig saved = dbamm.saveAlertServiceConfig(service);
        Assert.assertFalse(saved.isEnabled());
        LOGGER.debug("Updating an Alert Service");
        saved.enable();
        dbamm.updateAlertServiceConfig(saved);
        LOGGER.debug("Verifying changes to the updated Alert Service");
        AlertServiceConfig updatedConfig = dbamm.getAlertServiceConfig(JsonAlertService.NAME);

        Assert.assertTrue(updatedConfig.isEnabled());
    }

    /**
     * Test for the deleteAlertService method.
     */
    @Test
    public final void deleteAlertService() {
        LOGGER.debug("creating DBAlertServiceManager");
        DBAlertServiceManager dbamm = new DBAlertServiceManager(sessionFactory);
        dbamm.deleteAlertServiceConfig(JsonAlertService.NAME);
        AlertServiceConfig serviceConfig2 = dbamm.getAlertServiceConfig(JsonAlertService.NAME);
        Assert.assertNull(serviceConfig2);
        AlertServiceConfig serviceConfig = new AlertServiceConfig(JsonAlertService.NAME);
        dbamm.saveAlertServiceConfig(serviceConfig);
    }
}
