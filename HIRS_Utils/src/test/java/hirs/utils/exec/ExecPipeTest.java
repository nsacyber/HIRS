package hirs.utils.exec;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Tests for the ExecPipe class.
 */
public class ExecPipeTest {
    /**
     * Tests that a pipeline of a single command works as expected.
     *
     * @throws IOException if there is a problem executing the command
     * @throws InterruptedException if execution is interrupted
     */
    @Test
    public void testOneStagePipe() throws IOException, InterruptedException {
        ExecPipe pipe = new ExecPipe(
                new ExecBuilder("echo").args("test")
        );

        AsynchronousExecResult result = pipe.exec();
        result.waitFor();
        Assert.assertTrue(result.isSuccessful());
        Assert.assertEquals(result.getStdOutResult(), "test\n");
    }

    /**
     * Tests that a simple three-command pipeline functions as expected.
     *
     * @throws IOException if there is a problem executing the command
     * @throws InterruptedException if execution is interrupted
     */
    @Test
    public void testSimplePipe() throws IOException, InterruptedException {
        ExecPipe pipe = new ExecPipe(
                new ExecBuilder("echo").args("test"),
                new ExecBuilder("grep").args("te"),
                new ExecBuilder("wc").args("-l")
        );

        AsynchronousExecResult result = pipe.exec();
        result.waitFor();
        Assert.assertTrue(result.isSuccessful());
        Assert.assertEquals(result.getStdOutResult(), "1\n");
    }

    /**
     * Tests that a simple three-command pipeline with a specified standard
     * output stream functions as expected.
     *
     * @throws IOException if there is a problem executing the command
     * @throws InterruptedException if execution is interrupted
     */
    @Test
    public void testSimplePipeWithAltStdOut() throws IOException, InterruptedException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ExecPipe pipe = new ExecPipe(
                new ExecBuilder("echo").args("test"),
                new ExecBuilder("grep").args("te"),
                new ExecBuilder("wc").args("-l").stdOutAndErr(baos, null)
        );

        AsynchronousExecResult result = pipe.exec();
        result.waitFor();
        Assert.assertTrue(result.isSuccessful());
        Assert.assertEquals(baos.toString("UTF-8"), "1\n");
    }

    /**
     * Tests that the pipeOf utility method functions as expected.
     *
     * @throws IOException if there is a problem executing the command
     * @throws InterruptedException if execution is interrupted
     */
    @Test
    public void testPipeOf() throws IOException, InterruptedException {
        AsynchronousExecResult result = ExecPipe.pipeOf(false, new String[][]{
                {"echo", "test"},
                {"grep", "te"},
                {"wc", "-l"}
        }).exec();

        result.waitFor();
        Assert.assertTrue(result.isSuccessful());
        Assert.assertEquals(result.getStdOutResult(), "1\n");
    }
}
