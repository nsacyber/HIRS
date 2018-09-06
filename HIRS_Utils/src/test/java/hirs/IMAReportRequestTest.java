package hirs;

import hirs.data.persist.IMAReport;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * <code>IMAReportRequestTest</code> is a unit test class for the
 * <code>IMAReportRequest</code> class.
 */
public class IMAReportRequestTest {

    private static final int FULL = 0;
    private static final int INDEX = 9999;
    private static final int INVALID_INDEX = -9999;
    private static final String BOOT_ID = "Mon Apr 20 09:32";

    /**
     * Tests that default constructor sets the correct properties.
     */
    @Test
    public final void defaultImaReportRequest() {
        final IMAReportRequest imaReportRequest = new IMAReportRequest();
        Assert.assertNull(imaReportRequest.getBootcycleId());
        Assert.assertEquals(imaReportRequest.getIMAIndex(), 0);
    }
    /**
     * Tests instantiation of IMAReportRequest object with index of 0.
     */
    @Test
    public final void imaReportRequestFullReport() {
        IMAReportRequest imaReportRequest = new IMAReportRequest(null, FULL);
        Assert.assertNotNull(imaReportRequest);
    }

    /**
     * Tests instantiation of IMAReportRequest object with index of 0 and a
     * boot-cycle ID. This tests that full report can be requested even if state
     * has been saved.
     */
    @Test
    public final void imaReportRequestFullReportWithBootcycleId() {
        IMAReportRequest imaReportRequest = new IMAReportRequest(BOOT_ID, FULL);
        Assert.assertNotNull(imaReportRequest);
    }

    /**
     * Tests instantiation of IMAReportRequest object with index greater
     * than 0.
     */
    @Test
    public final void imaReportRequestIndex() {
        final IMAReportRequest imaReportRequest =
                new IMAReportRequest(BOOT_ID, INDEX);
        Assert.assertNotNull(imaReportRequest);
    }

    /**
     * Tests that index must be zero if boot-cycle ID is null, which indicates
     * to send a full report.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void imaReportRequestWithNullBootcycleIdAndInvalidIndex() {
        final IMAReportRequest imaReportRequest = new IMAReportRequest(null,
                INDEX);
        Assert.assertNull(imaReportRequest);
    }

    /**
     * Tests that <code>IMAReportRequest</code> constructor handles an attempt
     * to set an invalid IMA index.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void imaReportRequestInvalid() {
        final IMAReportRequest imaReportRequest = new IMAReportRequest(BOOT_ID,
                INVALID_INDEX);
        Assert.assertNull(imaReportRequest);
    }

    /**
     * Tests that <code>IMAReportRequest</code> returns a valid boot-cycle ID.
     */
    @Test
    public final void getBootcycleId() {
        final IMAReportRequest imaReportRequest =
                new IMAReportRequest(BOOT_ID, INDEX);
        Assert.assertEquals(imaReportRequest.getBootcycleId(), BOOT_ID);
    }

    /**
     * Tests that <code>IMAReportRequest</code> returns a valid IMA index.
     */
    @Test
    public final void getIMAIndex() {
        final IMAReportRequest imaReportRequest =
                new IMAReportRequest(BOOT_ID, INDEX);
        Assert.assertEquals(imaReportRequest.getIMAIndex(), INDEX);
    }

    /**
     * Tests that <code>IMAReportRequest</code> returns a valid report type.
     */
    @Test
    public final void getReportType() {
        IMAReportRequest imaReportRequest = new IMAReportRequest(BOOT_ID, FULL);
        Assert.assertEquals(imaReportRequest.getReportType(),
                IMAReport.class);
    }

}
