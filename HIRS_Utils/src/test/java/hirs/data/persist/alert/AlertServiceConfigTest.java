package hirs.data.persist.alert;

import hirs.alert.JsonAlertService;
import hirs.data.persist.SpringPersistenceTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.hibernate.Session;

import hirs.persist.DBManager;

import java.util.List;

/**
 * Test class for AlertServiceConfig.
 */
public class AlertServiceConfigTest extends SpringPersistenceTest {
    private static final Logger LOGGER = LogManager.getLogger(AlertServiceConfig.class);

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
     */
    @BeforeClass
    public final void beforeClass() {
        LOGGER.debug("retrieving session factory");

        // Add SNMP Alert Service default, should be set in SystemInit
        LOGGER.debug("creating temporary Alert Service Config");
        DBManager<AlertServiceConfig> alertSrvConf;
        alertSrvConf = new DBManager<>(AlertServiceConfig.class, sessionFactory);
        AlertServiceConfig jsonConfig = new AlertServiceConfig(JsonAlertService.NAME);
        jsonConfig.enable();
        alertSrvConf.save(jsonConfig);
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public final void afterClass() {
        LOGGER.debug("cleaning objects and closing session factory");
    }

    /**
     * Cleans up the DB.
     */
    @AfterMethod
    public final void afterMethod() {
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final List<?> alertMonitors = session
                .createCriteria(AlertMonitor.class).list();
        for (Object o : alertMonitors) {
             LOGGER.debug("deleting alertMonitors: {}", o);
            session.delete(o);
        }

         LOGGER.debug("all alert Monitors removed");
        session.getTransaction().commit();

    }

    /**
     * Test of enable() and getStatus().
     */
    @Test
    public final void testEnable() {
        boolean enable = false;
        AlertServiceConfig asc = new AlertServiceConfig(JsonAlertService.NAME);
        asc.disable();

        boolean testEnable = asc.isEnabled();

        Assert.assertEquals(enable, testEnable);

        enable = true;
        asc.enable();
        testEnable = asc.isEnabled();

        Assert.assertEquals(enable, testEnable);

    }

    /**
     * Test of getName method, of class AlertServiceConfig.
     */
    @Test
    public final void testGetName() {
        AlertServiceConfig asc = new AlertServiceConfig(JsonAlertService.NAME);
        Assert.assertEquals(asc.getType(), JsonAlertService.NAME);
    }

    /**
     * Test of getId method, of class AlertServiceConfig.
     */
    @Test
    public final void testGetId() {
        AlertServiceConfig asc = new AlertServiceConfig(JsonAlertService.NAME);
        String id = "1234";
        asc.setServiceIdentifier(id);

        String getId = asc.getServiceIdentifier();

        Assert.assertEquals(id, getId);
    }

    /**
     * Test of getAlertMonitorList method, of class AlertServiceConfig.
     */
    @Test
    public final void testGetAlertMonitorList() {
        //Will get enabled when SnmpAlertService is added.
        /*      SnmpAlertService sas = getSNMPService();
        AlertMonitor am = new AlertMonitor();
        am.setName("TestAM1");
        sas.addMonitor(am);
        AlertMonitor am2 = new AlertMonitor();
        am2.setName("TestAM2");
        sas.addMonitor(am2);
        AlertMonitor am3 = new AlertMonitor();
        am3.setName("TestAM3");
        sas.addMonitor(am3);

        sas.reloadProperties();
        List<AlertMonitor> monitors = sas.getMonitors();

        AlertMonitor getAm = monitors.get(0);
        String getName = getAm.getName();
        Assert.assertEquals(getName, "TestAM1");

        getAm = monitors.get(1);
        getName = getAm.getName();
        Assert.assertEquals(getName, "TestAM2");

        getAm = monitors.get(2);
        getName = getAm.getName();
        Assert.assertEquals(getName, "TestAM3");
 */
    }
/*
    private SnmpAlertService getSNMPService() {
        final HibernateDao dao = Mockito.mock(HibernateDao.class);
        final HibernateAlertService unmangedService = new HibernateAlertService(
                  dao);
        CompositeAlertService cas =
                new CompositeAlertService(unmangedService, factory);
        SnmpAlertService sas = null;
        try {
          sas = (SnmpAlertService) cas.getAlertService("SNMP");
          } catch (ServiceNotFoundException e) {
          e.printStackTrace();
        }
      return (sas);
    }
*/

}
