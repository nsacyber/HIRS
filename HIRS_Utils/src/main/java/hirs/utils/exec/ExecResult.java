package hirs.utils.exec;

import java.io.IOException;

/**
 * A small class that holds the results of an execution run through {@link ExecBuilder}.
 * This will always contain the exit status and will contain the contents of the execution's
 * standard output if an alternate OutputStream for standard out was not specified.  If it
 * was, the standard out contents here will be null.
 */
public interface ExecResult {
    /**
     * Retrieve whether the execution in question has completed,
     * successfully or not.
     *
     * @return whether the execution has completed
     */
    boolean isFinished();

    /**
     * Retrieve whether the execution was successful or not.
     *
     * @return true if the execution completely successfully, false otherwise
     */
    boolean isSuccessful();

    /**
     * Get the execution's exit status.
     *
     * @return the exit status of the execution
     */
    int getExitStatus();

    /**
     * Get the execution's standard output.
     *
     * @return the contents of standard output collected from the execution, or null if an
     * alternate OutputStream was specified
     *
     * @throws IOException if there is a problem serializing the stdout as UTF-8
     */
    String getStdOutResult() throws IOException;
}
