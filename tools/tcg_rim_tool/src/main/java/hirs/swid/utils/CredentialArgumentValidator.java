package hirs.swid.utils;

import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class CredentialArgumentValidator {
    private static final String PEM = "PEM";

    private final String truststoreFile;

    private final String certificateFile;

    private final String privateKeyFile;

    @Getter(AccessLevel.NONE)
    private final boolean isValidating;

    private String format;

    private String errorMessage;

    /**
     * Validates Certificate based arguments.
     *
     * @param truststoreFile  trust store file
     * @param certificateFile certificate file
     * @param privateKeyFile  private key file
     * @param isValidating    isValidating
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
                    errorMessage = "A public certificate must be specified by '-p' "
                            + "for signing operations.";
                }
                if (privateKeyFile.isEmpty()) {
                    errorMessage = "A private key must be specified by '-k' "
                            + "for signing operations.";
                }
                return false;
            }
        }
    }
}
