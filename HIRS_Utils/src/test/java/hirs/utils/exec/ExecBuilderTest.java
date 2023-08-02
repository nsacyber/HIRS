package hirs.utils.exec;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for <code>ExecBuilder</code>.
 */
public class ExecBuilderTest {
    private static final int SHORT_TIMEOUT = 1000;
    private static final int LONG_TIMEOUT = 5000;

    /**
     * Tests the simplest case of synchronously executing a command.
     *
     * @throws Exception if an unexpected error is encountered while executing the test.
     */
    @Test
    public final void testSyncExecSimple() throws Exception {
        SynchronousExecResult result = new ExecBuilder("/bin/echo").args("test").exec();
        Assert.assertTrue(result.isFinished());
        Assert.assertTrue(result.isSuccessful());
        Assert.assertEquals(result.getStdOutResult(), "test\n");
    }

    /**
     * Tests the simplest case of asynchronously executing a command.
     *
     * @throws Exception if an unexpected error is encountered while executing the test.
     */
    @Test
    public final void testAsyncExecSimple() throws Exception {
        AsynchronousExecResult result = new ExecBuilder("/bin/echo").args("test").asyncExec();
        result.waitFor();
        Assert.assertTrue(result.isFinished());
        Assert.assertTrue(result.isSuccessful());
        Assert.assertEquals(result.getStdOutResult(), "test\n");
        result.throwExceptionIfFailed();
    }

    /**
     * Tests that an IOException is thrown if a command's synchronous execution runs longer
     * than the given timeout.
     *
     * @throws Exception if an unexpected error is encountered while executing the test.
     */
    @Test(expectedExceptions = IOException.class)
    public final void testSyncExecTimeout() throws Exception {
        new ExecBuilder("/bin/sleep").args("10").timeout(SHORT_TIMEOUT).exec();
    }

    /**
     * Tests that an IOException is thrown if a command's asynchronous execution runs longer
     * than the given timeout.
     *
     * @throws Exception if an unexpected error is encountered while executing the test.
     */
    @Test(expectedExceptions = IOException.class)
    public final void testAsyncExecTimeout() throws Exception {
        AsynchronousExecResult result = new ExecBuilder("/bin/sleep")
                .args("10").timeout(SHORT_TIMEOUT).asyncExec();

        result.waitFor();
        Assert.assertTrue(result.isFinished());
        Assert.assertFalse(result.isSuccessful());
        result.throwExceptionIfFailed();
    }

    /**
     * Tests that an IOException is thrown if the given command can't be found during a
     * synchronous execution.
     *
     * @throws Exception if an unexpected error is encountered while executing the test.
     */
    @Test(expectedExceptions = IOException.class)
    public final void testSyncExecNonexistentCommand() throws Exception {
        new ExecBuilder("/bin/veryfakeprogram").exec();
    }

    /**
     * Tests that an IOException is thrown if the given command can't be found during an
     * asynchronous execution.
     *
     * @throws Exception if an unexpected error is encountered while executing the test.
     */
    @Test(expectedExceptions = IOException.class)
    public final void testAsyncExecNonexistentCommand() throws Exception {
        AsynchronousExecResult result = new ExecBuilder("/bin/veryfakeprogram").asyncExec();
        result.waitFor();
        Assert.assertTrue(result.isFinished());
        Assert.assertFalse(result.isSuccessful());
        result.throwExceptionIfFailed();
    }

    /**
     * Tests whether a custom execution environment and working directory can be set for a given
     * synchronous execution.
     *
     * @throws Exception if an unexpected error is encountered while executing the test.
     */
    @Test
    public final void testSyncExecCustomEnvironmentAndWorkingDir() throws Exception {
        Map<String, String> env = new HashMap<>();
        File tempFile = File.createTempFile("test", "sh");
        tempFile.deleteOnExit();
        FileUtils.writeStringToFile(tempFile, "echo $TEST; pwd", StandardCharsets.UTF_8);
        tempFile.setExecutable(true);
        env.put("TEST", "SOMEVALUE");

        SynchronousExecResult result = new ExecBuilder("/bin/bash")
                .args("-c", tempFile.toPath().toString())
                .timeout(LONG_TIMEOUT)
                .workingDirectory(new File("/tmp").toPath())
                .environment(env)
                .exec();

        boolean successfulDelete = tempFile.delete();
        if (!successfulDelete) {
            System.out.println("Failed to delete temporary script file.");
        }

        Assert.assertTrue(result.isFinished());
        Assert.assertTrue(result.isSuccessful());

        // for systems that alias /tmp directories
        Assert.assertTrue(
                result.getStdOutResult().contains("SOMEVALUE")
                && result.getStdOutResult().contains("tmp")
        );
    }

    /**
     * Tests whether a custom execution environment and working directory can be set for a given
     * asynchronous execution.
     *
     * @throws Exception if an unexpected error is encountered while executing the test.
     */
    @Test
    public final void testAsyncExecCustomEnvironmentAndWorkingDir() throws Exception {
        Map<String, String> env = new HashMap<>();
        File tempFile = File.createTempFile("test", "sh");
        tempFile.deleteOnExit();
        FileUtils.writeStringToFile(tempFile, "echo $TEST; pwd", StandardCharsets.UTF_8);
        tempFile.setExecutable(true);
        env.put("TEST", "SOMEVALUE");

        AsynchronousExecResult result = new ExecBuilder("/bin/bash")
                .args("-c", tempFile.toPath().toString())
                .timeout(LONG_TIMEOUT)
                .workingDirectory(new File("/tmp").toPath())
                .environment(env)
                .asyncExec();

        result.waitFor();
        Assert.assertTrue(result.isFinished());
        Assert.assertTrue(result.isSuccessful());

        boolean successfulDelete = tempFile.delete();
        if (!successfulDelete) {
            System.out.println("Failed to delete temporary script file.");
        }

        // for systems that alias /tmp directories
        Assert.assertTrue(
                result.getStdOutResult().contains("SOMEVALUE")
                        && result.getStdOutResult().contains("tmp")
        );
    }

    /**
     * Tests whether output streams can be used to capture a process's standard output and standard
     * error streams during a sychronous execution.
     *
     * @throws Exception if an unexpected error is encountered while executing the test.
     */
    @Test
    public final void testSyncExecOutputStream() throws Exception {
        ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
        ByteArrayOutputStream stdErr = new ByteArrayOutputStream();

        SynchronousExecResult result = new ExecBuilder("/bin/echo")
                .args("test")
                .timeout(LONG_TIMEOUT)
                .stdOutAndErr(stdOut, stdErr)
                .exec();

        Assert.assertTrue(result.isFinished());
        Assert.assertTrue(result.isSuccessful());

        stdOut.close();
        stdErr.close();
        Assert.assertEquals(stdOut.toString("UTF-8"), "test\n");
        Assert.assertEquals(stdErr.toString("UTF-8"), "");
    }

    /**
     * Tests whether output streams can be used to capture a process's standard output and standard
     * error streams during an asychronous execution.
     *
     * @throws Exception if an unexpected error is encountered while executing the test.
     */
    @Test
    public final void testAsyncExecOutputStream() throws Exception {
        ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
        ByteArrayOutputStream stdErr = new ByteArrayOutputStream();

        AsynchronousExecResult result = new ExecBuilder("/bin/echo")
                .args("test")
                .timeout(LONG_TIMEOUT)
                .stdOutAndErr(stdOut, stdErr)
                .asyncExec();

        result.waitFor();
        Assert.assertTrue(result.isFinished());
        Assert.assertTrue(result.isSuccessful());

        stdOut.close();
        stdErr.close();
        Assert.assertEquals(stdOut.toString("UTF-8"), "test\n");
        Assert.assertEquals(stdErr.toString("UTF-8"), "");
    }

    /**
     * Tests whether ExecResult contains the correct exit code of the process during
     * a synchronous execution.
     *
     * @throws Exception if an unexpected error is encountered while executing the test.
     */
    @Test
    public final void testSyncExecExitStatus() throws Exception {
        SynchronousExecResult result = new ExecBuilder("/bin/false")
                .exitValues(new int[]{1}).exec();
        Assert.assertTrue(result.isFinished());
        Assert.assertTrue(result.isSuccessful());
        Assert.assertEquals(result.getExitStatus(), 1);
    }

    /**
     * Tests whether ExecResult contains the correct exit code of the process during
     * an asynchronous execution.
     *
     * @throws Exception if an unexpected error is encountered while executing the test.
     */
    @Test
    public final void testAsyncExecExitStatus() throws Exception {
        AsynchronousExecResult result = new ExecBuilder("/bin/false")
                .exitValues(new int[]{1}).asyncExec();

        result.waitFor();
        Assert.assertTrue(result.isFinished());
        Assert.assertTrue(result.isSuccessful());
        Assert.assertEquals(result.getExitStatus(), 1);
    }
}
