package hirs.utils.exec;

import org.apache.commons.exec.DefaultExecuteResultHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 *  An implementation of ExecResult that facilitates working with
 *  asynchronous executions of external commands.
 */
public class AsynchronousExecResult implements ExecResult {
    private final DefaultExecuteResultHandler handler;
    private final OutputStream stdOut;
    private final OutputStream stdErr;
    private final String exec;

    /**
     * Construct a new AsyncExecResult.
     *
     * @param handler the execution result handler
     * @param stdOut the std out stream
     * @param stdErr the std err stream
     * @param exec a String representing the original execution
     */
    public AsynchronousExecResult(
            final DefaultExecuteResultHandler handler,
            final OutputStream stdOut,
            final OutputStream stdErr,
            final String exec) {
        this.handler = handler;
        this.stdOut = stdOut;
        this.stdErr = stdErr;
        this.exec = exec;
    }

    /**
     * Retrieve whether the execution in question has completed,
     * successfully or not.
     *
     * @return whether the execution has completed
     */
    @Override
    public boolean isFinished() {
        return handler.hasResult();
    }

    /**
     * Retrieve whether the execution was successful or not.
     * Throws an IllegalStateException if the execution has not yet finished.
     *
     * @return true if the execution completely successfully, false otherwise
     */
    @Override
    public boolean isSuccessful() {
        checkFinished();
        return handler.getException() == null;
    }

    /**
     * Get the execution's exit status.
     * Throws an IllegalStateException if the execution has not yet finished.
     *
     * @return the exit status of the execution
     */
    @Override
    public int getExitStatus() {
        checkFinished();
        return handler.getExitValue();
    }

    /**
     * Get the execution's standard output.
     * Throws an IllegalStateException if the execution has not yet finished.
     *
     * @return the contents of standard output collected from the execution, or null if an
     * alternate OutputStream was specified
     *
     * @throws IOException if there is a problem formatting the output to UTF-8
     */
    @Override
    public String getStdOutResult() throws IOException {
        checkFinished();

        if (stdOut != null) {
            return ((ByteArrayOutputStream) stdOut).toString(StandardCharsets.UTF_8.toString());
        } else {
            return null;
        }
    }

    /**
     * Synchronously wait for the execution to complete.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void waitFor() throws InterruptedException {
        handler.waitFor();
    }

    /**
     * If the execution failed, throw the exception containing
     * the cause of failure.
     * Throws an IllegalStateException if the execution has not yet finished.
     *
     * @throws IOException the exception that occurred during execution
     */
    public void throwExceptionIfFailed() throws IOException {
        checkFinished();
        if (!isSuccessful()) {
            throw ExecBuilder.logFailureAndGetIOException(
                    exec,
                    handler.getException(),
                    stdOut,
                    stdErr
            );
        }
    }

    private void checkFinished() {
        if (!isFinished()) {
            throw new IllegalStateException("Execution has not yet finished.");
        }
    }
}
