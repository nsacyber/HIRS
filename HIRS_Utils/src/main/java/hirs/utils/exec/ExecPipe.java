package hirs.utils.exec;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A class that helps organize piped commands, using the ExecBuilder
 * class.  Given a list of ExecBuilders or strings representing commands,
 * the external processes will have their stdout and stdin connected
 * such that the overall execution operates the same as a bash pipe.
 *
 * For usage examples, see ExecPipeTest.
 */
public class ExecPipe {
    private final List<ExecBuilder> builders;

    /**
     * Construct a new ExecPipe from the given ExecBuilders.  There must be at least one
     * ExecBuilder.
     *
     * @param execBuilders ExecBuilders representing the executions to pipe together
     */
    public ExecPipe(final ExecBuilder... execBuilders) {
        this(Arrays.asList(execBuilders));
    }

    /**
     * Construct a new ExecPipe from the given ExecBuilders.  There must be at least one
     * ExecBuilder.
     *
     * @param execBuilders ExecBuilders representing the executions to pipe together
     */
    public ExecPipe(final List<ExecBuilder> execBuilders) {
        if (execBuilders.size() == 0) {
            throw new IllegalArgumentException("Must have at least one ExecBuilder.");
        }
        builders = Collections.unmodifiableList(execBuilders);
    }

    /**
     * Execute the commands that this ExecPipe instance was constructed with,
     * and redirect each command's stdin to the output of the previous command (except for the
     * first) and each command's stdout to the input of the next command (except for the last).
     *
     * @return an AsynchronousExecResult representing the execution of the last command in the pipe
     * @throws IOException if there is an error during execution initialization
     */
    public AsynchronousExecResult exec() throws IOException {
        PipedInputStream inputStream = null;
        PipedOutputStream outputStream = null;

        for (int i = 0; i < builders.size(); i++) {
            ExecBuilder builder = builders.get(i);

            // if this is not the first process, set the stdin
            if (i != 0) {
                builder.stdIn(inputStream);
            }

            // if this is not the last process, set the stdout
            if (i != builders.size() - 1) {
                outputStream = new PipedOutputStream();
                builder.stdOutAndErr(outputStream, null);
                inputStream = new PipedInputStream(outputStream);
            }
        }

        AsynchronousExecResult lastAsyncResult = null;
        for (ExecBuilder builder : builders) {
            lastAsyncResult = builder.asyncExec();
        }

        return lastAsyncResult;
    }

    /**
     * Utility method to help create an ExecPipe.  Given a list of commands,
     * create an ExecPipe with all these commands piped together.
     *
     * The method takes a variable number of commands, but there must be at least
     * one.  Each command is represented as a String array.  The first element
     * is the command to run, and all following elements are its arguments.
     * See ExecPipeTest for example usage.
     *
     * @param quoteArgs whether to handle quoating the arguments to the given commands
     * @param commands any number of commands of the above form
     * @return an ExecPipe whose execution will execute the given commands
     */
    public static ExecPipe pipeOf(final boolean quoteArgs, final String[]... commands) {
        List<ExecBuilder> builders = new ArrayList<>();
        for (String[] command : commands) {
            List<String> commandParts = Arrays.asList(command);
            ExecBuilder builder = new ExecBuilder(commandParts.get(0), quoteArgs);
            List<String> args = commandParts.subList(1, commandParts.size());
            String[] argsArr = new String[args.size()];
            builder.args(args.toArray(argsArr));
            builders.add(builder);
        }
        return new ExecPipe(builders);
    }

    @Override
    public String toString() {
        return "ExecPipe{"
                + "builders=" + builders
                + '}';
    }
}
