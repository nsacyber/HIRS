package hirs.swid.utils;

public class CredentialArgumentValidator {
    private String truststoreFile;
    private String certificateFile;
    private String privateKeyFile;
    private String password;
    private String alias;
    private String format;
    private boolean isValidating;
    private String errorMessage;
    private static final String JKS = "JKS";
    private static final String PEM = "PEM";

    public CredentialArgumentValidator(String truststoreFile,
                                       String certificateFile,
                                       String privateKeyFile,
                                       String password,
                                       String alias,
                                       boolean isValidating) {
        this.truststoreFile = truststoreFile;
        this.certificateFile = certificateFile;
        this.privateKeyFile = privateKeyFile;
        this.password = password;
        this.alias = alias;
        this.isValidating = isValidating;
        errorMessage = "";
    }

    /**
     * Getter for format property
     *
     * @return string
     */
    public String getFormat() {
        return format;
    }

    /**
     * Getter for error message
     *
     * @return string
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * This method checks for the following valid configurations of input arguments:
     * 1. certificate only for validating (PEM format)
     * 2. truststore only for validating (PEM format)
     * 3. certificate + private key for signing (PEM format)
     * 4. truststore + private key for signing (PEM format)
     *
     * @return true if the above are found, false otherwise
     */
    public boolean isValid() {
        if (isValidating) {
            if (!truststoreFile.isEmpty() || !certificateFile.isEmpty()) {
                format = PEM;
                return true;
            } else {
                errorMessage = "Validation requires a public key certificate or truststore.";
                return false;
            }
        } else {
            if ((!truststoreFile.isEmpty() || !certificateFile.isEmpty())
                    && !privateKeyFile.isEmpty()) {
                format = PEM;
                return true;
            } else {
                errorMessage = "Either a truststore or public certificate, " +
                        "accompanied by a matching private key, is required for signing.";
                return false;
            }
        }
    }
}
