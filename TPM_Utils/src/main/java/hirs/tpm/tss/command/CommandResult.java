package hirs.tpm.tss.command;

/**
 * Encapsulates the results of executing a command.
 */
public class CommandResult {

    /**
     * The exit status when attempting to take ownership of an already owned TPM.
     */
    public static final int TPM_PREVIOUSLY_OWNED_ERROR = 20;

    private String output;

    private int exitStatus;

    /**
     * Creates the command result with the specified output and exist status.
     *
     * @param output     of the command
     * @param exitStatus of the command
     */
    public CommandResult(final String output, final int exitStatus) {
        this.output = output;
        this.exitStatus = exitStatus;
    }

    /**
     * @return command output.
     */
    public String getOutput() {
        return output;
    }

    /**
     * @return command exit status.
     */
    public int getExitStatus() {
        return exitStatus;
    }

}
