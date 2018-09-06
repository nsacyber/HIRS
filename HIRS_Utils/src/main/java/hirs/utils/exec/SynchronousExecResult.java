package hirs.utils.exec;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * A small class that holds the results of an execution run through {@link ExecBuilder}.
 * This will always contain the exit status and will contain the contents of the execution's
 * standard output if an alternate OutputStream for standard out was not specified or if
 * a ByteArrayOutputStream was specified.  Otherwise, the standard out contents
 * will be reported as null and the given output stream will need to be used directly.
 */
public class SynchronousExecResult implements ExecResult {
    private String stdOutResult;
    private int exitStatus;

    /**
     * Construct a new SynchronousExecResult.
     *
     * @param stdOut the OutputStream of the execution's standard output, can be null
     * @param exitStatus the exit status of the execution
     */
    public SynchronousExecResult(final OutputStream stdOut, final int exitStatus) {
        this.exitStatus = exitStatus;

        if (stdOut != null && stdOut instanceof ByteArrayOutputStream) {
            ByteArrayOutputStream baos = (ByteArrayOutputStream) stdOut;
            try {
                this.stdOutResult = baos.toString("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public boolean isSuccessful() {
        return true;
    }

    /**
     * Get the execution's standard output.
     *
     * @return the contents of standard output collected from the execution, or null if an
     * alternate OutputStream was specified
     */
    @Override
    public String getStdOutResult() {
        return stdOutResult;
    }

    /**
     * Get the execution's exit status.
     *
     * @return the exit status of the execution
     */
    @Override
    public final int getExitStatus() {
        return exitStatus;
    }

    @Override
    public final String toString() {
        return String.format("SynchronousExecResult{exitStatus=%d}", exitStatus);
    }
}
