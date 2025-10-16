package hirs.swid.utils;

public class CredentialArgumentValidator {
    private String truststoreFile;
    private String certificateFile;
    private String privateKeyFile;
    private String format;
    private boolean isValidating;
    private String errorMessage;
    private static final String PEM = "PEM";

    /**
     * Validates Certificate based arguments.
     * @param truststoreFile
     * @param certificateFile
     * @param privateKeyFile
     * @param isValidating
     */
    public CredentialArgumentValidator(final String truststoreFile,
                                       final String certificateFile,
                                       final String privateKeyFile,
                                       final boolean isValidating) {
        this.truststoreFile = truststoreFile;
        this.certificateFile = certificateFile;
        this.privateKeyFile = privateKeyFile;
        this.isValidating = isValidating;
        errorMessage = "";
    }

    /**
     * Getter for format property.
     *
     * @return string
     */
    public String getFormat() {
        return format;
    }

    /**
     * Getter for error message.
     *
     * @return string
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * This method checks for the following valid configurations of input arguments.
     * 1. truststore only for validating (PEM format)
     * 2. certificate + private key for signing (PEM format)
     *
     * @return true if the above are found, false otherwise
     */
    public boolean isValid() {
        if (isValidating) {
            if (!truststoreFile.isEmpty()) {
                format = PEM;
                return true;
            } else {
                errorMessage = "Validation requires a valid truststore file.";
                return false;
            }
        } else {
            if (!certificateFile.isEmpty() && !privateKeyFile.isEmpty()) {
                format = PEM;
                return true;
            } else {
                if (certificateFile.isEmpty()) {
                    errorMessage = "A public certificate must be specified by \'-p\' "
                            + "for signing operations.";
                }
                if (privateKeyFile.isEmpty()) {
                    errorMessage = "A private key must be specified by \'-k\' "
                            + "for signing operations.";
                }
                return false;
            }
        }
    }
}
