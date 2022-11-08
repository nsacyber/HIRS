package hirs.attestationca.persist;

import hirs.attestationca.servicemanager.DBManager;
import hirs.data.persist.LazyTestItemChild;
import hirs.persist.DBManagerException;
import hirs.persist.DBUtility;
import org.hibernate.LazyInitializationException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;

/**
 * Contains unit tests for the {@link DBManager} class.  Many other tests also implicitly
 * test functionality in DBManager.
 */
public class DBManagerTest extends SpringPersistenceTest {
    private int errorCount;
    private int closeCount;

    private static final int RETRY_ATTEMPTS = 5;
    private static final long RETRY_SLEEP_MS = 200;

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
     */
    @BeforeClass
    public final void setup() {
    }

    /**
     * Resets counters used by TestRetryListener.
     */
    @BeforeMethod
    public final void reset() {
        errorCount = 0;
        closeCount = 0;
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public final void tearDown() {

    }

    /**
     * Resets the test state to a known good state. This currently only resets
     * the database by removing all <code>Repository</code> and <code>RepoPackage</code> objects.
     */
    @AfterMethod
    public final void resetTestState() {
        DBUtility.removeAllInstances(sessionFactory, LazyTestItemChild.class);
    }

    /**
     * Tests that attempting to retrieve lazily-loaded fields without using
     * <code>getAndLoadLazyFields(Serializable)</code> results in a
     * LazyInitializationException.
     */
    @Test(expectedExceptions = LazyInitializationException.class)

    public final void testGet() {
        LazyTestItemChild child = new LazyTestItemChild("Test Child");
        DBManager<LazyTestItemChild> childMan =
                new DBManager<>(LazyTestItemChild.class, sessionFactory);
        LazyTestItemChild savedChild = childMan.save(child);
        LazyTestItemChild retrievedChild = childMan.get(savedChild.getId());
        retrievedChild.getItems().size();
        Assert.fail("LazyLoadingException was not thrown");
    }

    /**
     * Tests that attempting to retrieve lazily-loaded fields using
     * <code>getAndLoadLazyFields(Serializable)</code> gives the proper data.
     */
    @Test
    public final void testGetLazyFields() {
        LazyTestItemChild child = new LazyTestItemChild("Test Child");
        DBManager<LazyTestItemChild> childMan =
                new DBManager<>(LazyTestItemChild.class, sessionFactory);
        LazyTestItemChild savedChild = childMan.save(child);
        LazyTestItemChild retrievedChild = childMan.getAndLoadLazyFields(savedChild.getId(), false);
        Assert.assertEquals(retrievedChild.getItems().size(), 1);
    }

    /**
     * Test to exercise the retry logic and verify the retry listener is called.
     */
    @Test
    public final void testRetryStaleObjectState() {
        LazyTestItemChild child = new LazyTestItemChild("Test Child");
        DBManager<LazyTestItemChild> childMan =
                new DBManager<LazyTestItemChild>(LazyTestItemChild.class, sessionFactory) {
                    @Override
                    protected void doUpdate(final LazyTestItemChild object)
                            throws DBManagerException {
                        Exception staleObjState = new StaleObjectStateException(
                                "Test name",
                                "Test identifier"
                        );
                        throw new DBManagerException(staleObjState);
                    }
                };

        childMan.setRetryTemplate(RETRY_ATTEMPTS, RETRY_SLEEP_MS);
        childMan.addRetryListener(new TestRetryListener());

        try {
            childMan.update(child);
            Assert.fail("update should have failed and been retried");
        } catch (DBManagerException dbe) {
            Assert.assertEquals(dbe.getCause().getClass(), StaleObjectStateException.class);
        } catch (Exception e) {
            Assert.fail("Should not have thrown anything other than a DBManagerException");
        }

        Assert.assertEquals(errorCount, RETRY_ATTEMPTS);
        Assert.assertEquals(closeCount, 1);
    }

    /**
     * Test to exercise the retry logic and verify the retry listener is called.
     */
    @Test
    public final void testRetryLockAcquisitionException() {
        LazyTestItemChild child = new LazyTestItemChild("Test Child");
        DBManager<LazyTestItemChild> childMan =
                new DBManager<LazyTestItemChild>(LazyTestItemChild.class, sessionFactory) {
                    @Override
                    protected void doUpdate(final LazyTestItemChild object)
                            throws DBManagerException {
                        Exception staleObjState = new LockAcquisitionException(
                                "Test",
                                new SQLException("Test")
                        );
                        throw new DBManagerException(staleObjState);
                    }
                };

        childMan.setRetryTemplate(RETRY_ATTEMPTS, RETRY_SLEEP_MS);
        childMan.addRetryListener(new TestRetryListener());

        try {
            childMan.update(child);
            Assert.fail("update should have failed and been retried");
        } catch (DBManagerException dbe) {
            Assert.assertEquals(dbe.getCause().getClass(), LockAcquisitionException.class);
        } catch (Exception e) {
            Assert.fail("Should not have thrown anything other than a DBManagerException");
        }

        Assert.assertEquals(errorCount, RETRY_ATTEMPTS);
        Assert.assertEquals(closeCount, 1);
    }

    /**
     * Test to exercise the retry logic and verify the retry listener is called.
     */
    @Test
    public final void testShouldNotRetryDBManagerException() {
        LazyTestItemChild child = new LazyTestItemChild("Test Child");
        DBManager<LazyTestItemChild> childMan =
                new DBManager<LazyTestItemChild>(LazyTestItemChild.class, sessionFactory) {
                    @Override
                    protected void doUpdate(final LazyTestItemChild object)
                            throws DBManagerException {
                        throw new DBManagerException("test message");
                    }
                };

        childMan.setRetryTemplate(RETRY_ATTEMPTS, RETRY_SLEEP_MS);
        childMan.addRetryListener(new TestRetryListener());

        try {
            childMan.update(child);
            Assert.fail("update should have failed");
        } catch (DBManagerException dbe) {
            Assert.assertEquals(dbe.getCause(), null);
        } catch (Exception e) {
            Assert.fail("Should not have thrown anything other than a DBManagerException");
        }

        Assert.assertEquals(errorCount, 1);
        Assert.assertEquals(closeCount, 1);
    }

    /**
     * Test to exercise the retry logic and verify the retry listener is called.
     */
    @Test
    public final void testShouldNotRetryNullPointerException() {
        LazyTestItemChild child = new LazyTestItemChild("Test Child");
        DBManager<LazyTestItemChild> childMan =
                new DBManager<LazyTestItemChild>(LazyTestItemChild.class, sessionFactory) {
                    @Override
                    protected void doUpdate(final LazyTestItemChild object)
                            throws DBManagerException {
                        throw new NullPointerException("test message");
                    }
                };

        childMan.setRetryTemplate(RETRY_ATTEMPTS, RETRY_SLEEP_MS);
        childMan.addRetryListener(new TestRetryListener());

        try {
            childMan.update(child);
            Assert.fail("update should have failed");
        } catch (NullPointerException npe) {
            Assert.assertEquals(npe.getCause(), null);
        } catch (Exception e) {
            Assert.fail("Should not have thrown anything other than a NullPointerException");
        }

        Assert.assertEquals(errorCount, 1);
        Assert.assertEquals(closeCount, 1);
    }

    private class TestRetryListener implements RetryListener {
        @Override
        public <T, E extends Throwable> boolean open(
                final RetryContext context, final RetryCallback<T, E> callback) {
            return true;
        }

        @Override
        public <T, E extends Throwable> void close(
                final RetryContext context, final RetryCallback<T, E> callback,
                final Throwable throwable) {
            closeCount++;
        }

        @Override
        public <T, E extends Throwable> void onError(final RetryContext context,
                                                     final RetryCallback<T, E> callback,
                                                     final Throwable throwable) {
            errorCount++;
        }
    }
}
