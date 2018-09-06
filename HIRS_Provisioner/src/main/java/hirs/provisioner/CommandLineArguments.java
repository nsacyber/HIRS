package hirs.provisioner;

/**
 * Class to hold the command line argument values passed to the provisioner.
 */
public final class CommandLineArguments {

    private static String hostName;

    private CommandLineArguments() {

    }

    /**
     *
     * @return the host name
     */
    public static String getHostName() {
        return hostName;
    }

    /**
     *
     * @param hostName the host name
     */
    public static void setHostName(final String hostName) {
        CommandLineArguments.hostName = hostName;
    }
}
