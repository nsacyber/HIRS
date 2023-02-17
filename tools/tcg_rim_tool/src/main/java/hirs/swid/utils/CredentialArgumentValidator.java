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
     * @return string
     */
    public String getFormat() { return format; }

    /**
     * Getter for error message
     * @return string
     */
    public String getErrorMessage() { return errorMessage; }

    /**
     * This method checks for the following valid configurations of input arguments:
     * 1. truststore + password + alias (JKS format)
     * 2. truststore + private key (PEM format)
     * 3. truststore only for validating (PEM format)
     * 4. certificate + private key (PEM format)
     * 5. certificate only for validating (PEM format)
     *
     * @return true if the above are found, false otherwise
     */
    public boolean isValid() {
        if (!truststoreFile.isEmpty()) {
            if (!password.isEmpty() && !alias.isEmpty()) {
                format = JKS;
                return true;
            } else if (!privateKeyFile.isEmpty() || isValidating) {
                format = PEM;
                return true;
            } else {
                errorMessage = "A JKS truststore needs a password and alias; " +
                        "a PEM truststore needs a private key file.";
                return false;
            }
        } else if (!certificateFile.isEmpty() && !privateKeyFile.isEmpty()) {
            format = PEM;
            return true;
        } else {
            errorMessage = "A public certificate must be accompanied by a private key file.";
            return false;
        }
    }
}
