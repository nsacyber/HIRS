package hirs.provisioner.client;

/**
 * Describes exceptions encountered while provisioning a client.
 */
public class ProvisioningException extends RuntimeException {

    /**
     * Provisioning exception that includes a root exception, the throwable, as the reason why the
     * provisioning process failed. The message provides additional detail as to why the exception
     * was encountered.
     *
     * @param message   additional explanation of the failure
     * @param throwable root cause for the exception
     */
    public ProvisioningException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
