package hirs.utils.exec;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Unit tests for the {@link ExecResult} class.
 */
public class SynchronousExecResultTest {
    private static final int EXIT_STATUS = 123;

    /**
     * Tests that ExecResult can be constructed.
     */
    @Test
    @SuppressFBWarnings(
            value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
            justification = "This test checks for errorless construction"
    )
    public final void testConstruction() {
        new SynchronousExecResult(getTestOutputStream(), 0);
    }

    /**
     * Tests that ExecResult can be constructed with a null standard output.
     */
    @Test
    @SuppressFBWarnings(
            value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
            justification = "This test checks for errorless construction"
    )
    public final void testConstructionWithNullOutput() {
        new SynchronousExecResult(null, 0);
    }

    /**
     * Tests that ExecResult returns the correct standard output.
     */
    @Test
    public final void testGetOutput() {
        Assert.assertEquals(
                new SynchronousExecResult(getTestOutputStream(), 0).getStdOutResult(),
                "Success"
        );
    }

    /**
     * Tests that ExecResult can return a null value for the standard output.
     */
    @Test
    public final void testGetNullOutput() {
        Assert.assertNull(new SynchronousExecResult(null, 0).getStdOutResult());
    }

    /**
     * Tests that ExecResult returns the correct exit status.
     */
    @Test
    public final void testGetExitStatus() {
        Assert.assertEquals(
                new SynchronousExecResult(getTestOutputStream(), EXIT_STATUS).getExitStatus(),
                EXIT_STATUS
        );
    }

    private static OutputStream getTestOutputStream() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write("Success".getBytes("UTF-8"));
            return baos;
        } catch (IOException e) {
            throw new RuntimeException("Could not create OutputStream", e);
        }
    }
}
