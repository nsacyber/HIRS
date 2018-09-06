package hirs;

import hirs.data.persist.TPMReport;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * <code>TPMReportRequestTest</code> is a unit test class for the
 * <code>TPMReportRequest</code> class.
 */
public class TPMReportRequestTest {

    private static final byte[] TEST_NONCE = new byte[] {(byte) 0x14,
        (byte) 0x13, (byte) 0x12, (byte) 0x11, (byte) 0x10, (byte) 0x0F,
        (byte) 0x0E, (byte) 0x0D, (byte) 0x0C, (byte) 0x0B, (byte) 0x0A,
        (byte) 0x09, (byte) 0x08, (byte) 0x07, (byte) 0x06, (byte) 0x05,
        (byte) 0x04, (byte) 0x03, (byte) 0x02, (byte) 0x01 };

    private static final byte[] TEST_SHORT_NONCE = new byte[] {(byte) 0x0F,
        (byte) 0x0E, (byte) 0x0D, (byte) 0x0C, (byte) 0x0B, (byte) 0x0A,
        (byte) 0x09, (byte) 0x08, (byte) 0x07, (byte) 0x06, (byte) 0x05,
        (byte) 0x04, (byte) 0x03, (byte) 0x02, (byte) 0x01 };

    private static final int TEST_MASK = 0xF17;
    private static final int ZERO_MASK = 0x0;

    /**
     * Tests instantiation of TPMReportRequest with setup of specific PCIndex
     * and nonce.
     */
    @Test
    public final void tpmReportRequestByteArrayInt() {
        TPMReportRequest tpmRepReq = new TPMReportRequest(TEST_NONCE, TEST_MASK);
        Assert.assertNotNull(tpmRepReq);
        Assert.assertEquals(tpmRepReq.getNonce(), TEST_NONCE);
        Assert.assertEquals(tpmRepReq.getPcrMask(), TEST_MASK);
    }

    /**
     * Tests that <code>TPMReportRequest</code> constructor handles an attempt
     * to set Nonce to Null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void tpmReportRequestNullNonce() {
        new TPMReportRequest(null, TEST_MASK);
    }


    /**
     * Tests that <code>TPMReportRequest</code> constructor handles an attempt
     * to set nonce to value less than Minimum nonce length.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void tpmReportRequestInvalidNonce() {
        new TPMReportRequest(TEST_SHORT_NONCE, TEST_MASK);
    }

    /**
     * Tests that <code>TPMReportRequest</code> constructor handles an attempt
     * to set the PCR Mask to all zeros.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void tpmReportRequestInvalidPcrMask() {
        new TPMReportRequest(TEST_NONCE, ZERO_MASK);
    }

    /**
     * Tests Nonce getter method.
     */
    @Test
    public final void getNonce() {
        TPMReportRequest tpmRepReq = new TPMReportRequest(TEST_NONCE, TEST_MASK);
        Assert.assertEquals(tpmRepReq.getNonce(), TEST_NONCE);
    }

    /**
     * Tests PCR mask getter method.
     */
    @Test
    public final void getPcrMask() {
        TPMReportRequest tpmRepReq = new TPMReportRequest(TEST_NONCE, TEST_MASK);
        Assert.assertEquals(tpmRepReq.getPcrMask(), TEST_MASK);
    }

    /**
     * Tests ReportType getter method.
     */
    @Test
    public final void getReportType() {
        TPMReportRequest tpmRepReq = new TPMReportRequest(TEST_NONCE, TEST_MASK);
        Assert.assertEquals(tpmRepReq.getReportType(), TPMReport.class);
    }
}
