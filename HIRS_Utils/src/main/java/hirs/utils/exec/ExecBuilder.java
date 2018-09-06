package hirs.utils.exec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A 'builder' class to aid in the configuration and execution of external commands.
 */
public class ExecBuilder {
    private static final long DEFAULT_TIMEOUT = 5 * 60 * 1000;

    private static final Logger LOGGER = LogManager.getLogger(ExecBuilder.class);

    private final String command;
    private final boolean handleQuoting;

    private String[] args;
    private long timeout = DEFAULT_TIMEOUT;
    private InputStream stdIn;
    private OutputStream stdOut = new ByteArrayOutputStream();
    private OutputStream stdErr = new ByteArrayOutputStream();
    private Path workingDirectory;
    private Map<String, String> env;
    private int[] exitValues = new int[]{0};

    /**
     * Construct a new ExecBuilder.  Quoting arguments
     * will be automatically handled.
     *
     * @param command the command this execution will run
     */
    public ExecBuilder(final String command) {
        this.command = command;
        this.handleQuoting = true;
    }

    /**
     * Construct a new ExecBuilder.
     *
     * @param command the command this execution will run
     * @param handleQuoting whether the underlying library will insert quotes where necessary
     */
    public ExecBuilder(final String command, final boolean handleQuoting) {
        this.command = command;
        this.handleQuoting = handleQuoting;
    }

    /**
     * Set the execution's args.
     *
     * @param args the desired args
     * @return the current ExecBuilder
     */
    public final ExecBuilder args(final String... args) {
        this.args = args.clone();
        return this;
    }

    /**
     * Set the execution's timeout.
     *
     * @param timeout the desired timeout
     * @return the current ExecBuilder
     */
    public final ExecBuilder timeout(final long timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Set the execution's standard input stream.
     *
     * @param stdIn the desired std in stream
     * @return the current ExecBuilder
     */
    public final ExecBuilder stdIn(final InputStream stdIn) {
        this.stdIn = stdIn;
        return this;
    }

    /**
     * Set the execution's standard out and error streams.
     *
     * @param stdOut the desired std out stream
     * @param stdErr the desired std err stream
     * @return the current ExecBuilder
     */
    public final ExecBuilder stdOutAndErr(final OutputStream stdOut, final OutputStream stdErr) {
        this.stdOut = stdOut;
        this.stdErr = stdErr;
        return this;
    }

    /**
     * Set the execution's working directory.
     *
     * @param workingDirectory the desired working directory
     * @return the current ExecBuilder
     */
    public final ExecBuilder workingDirectory(final Path workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    /**
     * Set the execution's environment.
     *
     * @param env the desired environment
     * @return the current ExecBuilder
     */
    public final ExecBuilder environment(final Map<String, String> env) {
        this.env = env;
        return this;
    }

    /**
     * Set the execution's valid exit values.  If this is not called, the expected exit value
     * defaults to 0.
     *
     * @param exitValues the desired exit values
     * @return the current ExecBuilder
     */
    public final ExecBuilder exitValues(final int[] exitValues) {
        this.exitValues = exitValues.clone();
        return this;
    }

    /**
     * Synchronously execute the configured execution.  Throws an IOException if there is an error
     * during execution, including if the desired execution target is not found, or if the process
     * exits with an unexpected exit value.
     *
     * @return a SynchronousExecResult which contains the exit status of the execution as well as
     *                the stdout of the process
     * @throws IOException if one of the above error conditions is met
     */
    public final SynchronousExecResult exec() throws IOException {
        try {
            int exitStatus = getExecutor().execute(getCommandLine(), env);
            return new SynchronousExecResult(stdOut, exitStatus);
        } catch (ExecuteException e) {
            throw logFailureAndGetIOException(toString(), e, stdOut, stdErr);
        }
    }

    /**
     * Asynchronously execute the configured execution.
     *
     * @return an AsynchronousExecResult which contains the execution's status as well as
     *                the stdout of the process
     * @throws IOException if there is an error during execution initialization
     */
    public final AsynchronousExecResult asyncExec() throws IOException {
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        getExecutor().execute(getCommandLine(), env, resultHandler);
        return new AsynchronousExecResult(resultHandler, stdOut, stdErr, toString());
    }

    private Executor getExecutor() {
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWatchdog(new ExecuteWatchdog(timeout));
        executor.setStreamHandler(new PumpStreamHandler(stdOut, stdErr, stdIn));
        executor.setExitValues(exitValues);
        if (workingDirectory != null) {
            executor.setWorkingDirectory(workingDirectory.toFile());
        }
        return executor;
    }

    private CommandLine getCommandLine() {
        CommandLine cmd = new CommandLine(command);
        if (args != null) {
            cmd.addArguments(args, handleQuoting);
        }
        return cmd;
    }

    /**
     * Utility method to assist with failed executions.
     * This method logs information about the failed
     * execution, including the command which failed,
     * as well as its stdout and stderr.
     *
     * @param exec a String representing the failed command
     * @param e the ExecuteException containing information about the failure
     * @param stdOut the stdOut of the failed execution
     * @param stdErr the stdErr of the failed execution
     * @return an IOException representing the failure
     * @throws UnsupportedEncodingException if there is a problem using the UTF-8 charset
     */
    static IOException logFailureAndGetIOException(
            final String exec,
            final ExecuteException e,
            final OutputStream stdOut,
            final OutputStream stdErr) throws UnsupportedEncodingException {
        String errString = "Execution Failed: ";
        LOGGER.error(errString);
        LOGGER.error(exec);

        if (stdOut != null && stdOut instanceof ByteArrayOutputStream) {
            String stdOutStr = ((ByteArrayOutputStream) stdOut).toString("UTF-8");
            LOGGER.error("stdout: " + stdOutStr);
        }

        if (stdErr != null && stdErr instanceof ByteArrayOutputStream) {
            String stdErrStr = ((ByteArrayOutputStream) stdErr).toString("UTF-8");
            LOGGER.error("stderr: " + stdErrStr);
            errString += stdErrStr;
        }

        return new IOException(errString, e);
    }

    @Override
    public final String toString() {
        return "ExecBuilder{"
                + "command='" + command + '\''
                + ", args=" + Arrays.toString(args)
                + ", timeout=" + timeout
                + ", workingDirectory=" + workingDirectory
                + ", env=" + env
                + ", exitValues=" + Arrays.toString(exitValues)
                + '}';
    }
}
