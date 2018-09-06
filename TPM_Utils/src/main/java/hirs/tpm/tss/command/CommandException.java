package hirs.tpm.tss.command;

/**
 * Exception that is thrown by the {@link CommandTpm} when executing operations.
 */
public class CommandException extends RuntimeException {

    private CommandResult commandResult;

    /**
     * Constructs this exception with the specified message and the command result.
     *
     * @param message       as to why this exception occurred
     * @param commandResult the result of the command that caused this exception
     */
    public CommandException(final String message, final CommandResult commandResult) {
        super(message);

        this.commandResult = commandResult;
    }

    /**
     * @return {@link CommandResult} of the command that caused this exception
     */
    public CommandResult getCommandResult() {
        return this.commandResult;
    }
}
