package hirs.persist;

import hirs.data.persist.SpringPersistenceTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import hirs.FilteredRecordsList;
import hirs.data.persist.Alert;

/**
 * Tests the DBAlertManager's ability to retrieve an ordered list of alerts
 * based on start and end dates.
 */
public class DBAlertManagerDateFilterTest extends SpringPersistenceTest {
    private static final Logger LOGGER = LogManager.getLogger(DBAlertManagerTrustAlertsTest.class);
    private static final String ORDER_COL = "id";

    private AlertManager mgr;
    private List<Alert> sequentialAlerts;

    /**
     * Creates a new <code>DBAlertManagerDateFilterTest</code>.
     */
    public DBAlertManagerDateFilterTest() {
        /* do nothing */
    }

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
     *
     * @throws InterruptedException if the sleep operation is interrupted
     */
    @BeforeClass
    public void setup() throws InterruptedException {
        mgr = new DBAlertManager(sessionFactory);

        sequentialAlerts = new ArrayList<>();

        // sleep between each creation to ensure that each alert has a distinct create time

        sequentialAlerts.add(createAlert(mgr, "1"));
        Thread.sleep(2000);
        sequentialAlerts.add(createAlert(mgr, "2"));
        Thread.sleep(2000);
        sequentialAlerts.add(createAlert(mgr, "3"));
        Thread.sleep(2000);
        sequentialAlerts.add(createAlert(mgr, "4"));
        Thread.sleep(2000);
        sequentialAlerts.add(createAlert(mgr, "5"));

        List<Alert> allAlerts = mgr.getAlertList();
        Assert.assertEquals(allAlerts.size(), 5);

        Date previousAlertDate = sequentialAlerts.get(0).getCreateTime();
        // verify alerts have sequential creation times
        for (Alert alert : sequentialAlerts.subList(1, sequentialAlerts.size() - 1)) {

            Assert.assertTrue(alert.getCreateTime().getTime() > previousAlertDate.getTime());
            previousAlertDate = alert.getCreateTime();
        }
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public void tearDown() {

    }

    /**
     * Tests that specifying no date filtering returns the full set of alerts.
     */
    @Test
    public void noDateFilteringReturnsFullList() {

        FilteredRecordsList<Alert> allAlerts = mgr.getOrderedAlertList(null, ORDER_COL,
                true, 0, 0, null, AlertManager.AlertListType.UNRESOLVED_ALERTS, null, null, null);

        Assert.assertEquals(allAlerts.size(), sequentialAlerts.size());
    }

    /**
     * Tests that specifying only a begin date will only exclude older alerts.
     */
    @Test
    public void beginDateOnlyFilteringExcludesOlderAlerts() {
        DateTime beginDate = new DateTime(sequentialAlerts.get(2).getCreateTime()).plusSeconds(1);

        FilteredRecordsList<Alert> returnAlerts = mgr.getOrderedAlertList(null, ORDER_COL,
                true, 0, 0, null, AlertManager.AlertListType.UNRESOLVED_ALERTS, null,
                beginDate.toDate(), null);
        // nothing before alert index 2
        Assert.assertEquals(returnAlerts.size(), 2);


        Assert.assertTrue(returnAlerts.contains(sequentialAlerts.get(3)));
        Assert.assertTrue(returnAlerts.contains(sequentialAlerts.get(4)));
    }

    /**
     * Tests that specifying only an end date will only exclude newer alerts.
     */
    @Test
    public void endDateOnlyFilteringExcludesNewerAlerts() {
        DateTime endDate = new DateTime(sequentialAlerts.get(2).getCreateTime()).plusSeconds(1);

        FilteredRecordsList<Alert> returnAlerts = mgr.getOrderedAlertList(null, ORDER_COL,
                true, 0, 0, null, AlertManager.AlertListType.UNRESOLVED_ALERTS, null,
                null, endDate.toDate());
        // nothing after alert index 2
        Assert.assertEquals(returnAlerts.size(), 3);

        Assert.assertTrue(returnAlerts.contains(sequentialAlerts.get(0)));
        Assert.assertTrue(returnAlerts.contains(sequentialAlerts.get(1)));
        Assert.assertTrue(returnAlerts.contains(sequentialAlerts.get(2)));
    }

    /**
     * Tests that specifying a begin and end date (date range) will exclude alerts outside
     * the range.
     */
    @Test
    public void beginAndEndDateFilteringExcludesCorrectAlerts() {
        DateTime beginDate = new DateTime(sequentialAlerts.get(1).getCreateTime()).minusSeconds(1);
        DateTime endDate = new DateTime(sequentialAlerts.get(3).getCreateTime()).plusSeconds(1);

        FilteredRecordsList<Alert> returnAlerts = mgr.getOrderedAlertList(null, ORDER_COL,
                true, 0, 0, null, AlertManager.AlertListType.UNRESOLVED_ALERTS, null,
                beginDate.toDate(), endDate.toDate());

        // only alerts 1 through 3
        Assert.assertEquals(returnAlerts.size(), 3);

        Assert.assertTrue(returnAlerts.contains(sequentialAlerts.get(1)));
        Assert.assertTrue(returnAlerts.contains(sequentialAlerts.get(2)));
        Assert.assertTrue(returnAlerts.contains(sequentialAlerts.get(3)));
    }

    private Alert createAlert(final AlertManager mgr, final String details)
            throws AlertManagerException {
        String alertDetails = details;
        if (details == null) {
            alertDetails = "default";
        }
        final Alert alert = new Alert(alertDetails);

        return mgr.saveAlert(alert);
    }
}
