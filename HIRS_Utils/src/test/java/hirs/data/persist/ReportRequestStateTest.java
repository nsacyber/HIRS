package hirs.data.persist;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;

/**
 * Unit tests for ReportRequestState.
 */
public class ReportRequestStateTest {

    /**
     * Tests getNonce and setNonce with a null nonce.
     */
    @Test
    public final void nullNonce() {
        ReportRequestState reportRequestState = new ReportRequestState();
        reportRequestState.setNonce(null);
        Assert.assertNull(reportRequestState.getNonce());

    }

    /**
     * Tests that the setDueDate method sets the due date to a later date
     * than the current time.
     */
    @Test
    public final void setDueDate() {
        ReportRequestState state = new ReportRequestState();
        Date currentTime = new Date();
        state.setDueDate(ReportRequestState.MINUTE_MS_INTERVAL);
        Date setDate = state.getDueDate();
        Assert.assertTrue(currentTime.before(setDate));
    }
}
