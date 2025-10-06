package hirs.swid.utils;

import com.beust.jcommander.IParametersValidator;
import com.beust.jcommander.ParameterException;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * This class handles validating all arguments in the context of a create+sign function.
 * The input arguments are checked that --verify is not also selected and that all
 * required inputs for --create are present.
 */
@Log4j2
public class CreateArgumentValidator implements IParametersValidator {
    private String[] requiredArgs = {"--attributes", "--rimel"};
    private String errorMessage = "";

    /**
     * This method validates the input parameter map.
     * @param parameters
     *            Name-value-pairs of all parameters (e.g. "-host":"localhost").
     *
     * @throws ParameterException
     */
    @Override
    public void validate(final Map<String, Object> parameters) throws ParameterException {
        if (isValueNotNull(parameters, "--create")) {
            if (isValueNotNull(parameters, "--verify")) {
                throw new ParameterException("Create and verify cannot be called together.");
            } else {
                for (String arg : requiredArgs) {
                    if (!isValueNotNull(parameters, arg)) {
                        errorMessage += arg + " is required to create and sign a base RIM. ";
                    }
                }
                validateSigningCredentials(parameters);
            }
        }
        if (!errorMessage.isEmpty()) {
            throw new ParameterException(errorMessage);
        }
    }

    /**
     * This method checks the given key for a null value.
     * @param parameters map
     * @param key the key to check
     * @return true if not null, else false
     */
    private boolean isValueNotNull(final Map<String, Object> parameters, final String key) {
        boolean retVal = true;
        Object object = parameters.get(key);
        if (object == null) {
            retVal = false;
        }
        return retVal;
    }

    /**
     * Validates X.509 certificates passed in as arguments.
     * @param parameters map
     */
    private void validateSigningCredentials(final Map<String, Object>  parameters) {
        if (isValueNotNull(parameters, "--default-key")
                && (isValueNotNull(parameters, "--privateKeyFile")
                        || isValueNotNull(parameters, "--publicCertificate"))) {
            errorMessage += "Too many signing credentials given, either choose --default-key OR "
                    + "provide --privateKeyFile and --publicCertificate";
        } else if (!isValueNotNull(parameters, "--default-key")
                && !isValueNotNull(parameters, "--privateKeyFile")
                && !isValueNotNull(parameters, "--publicCertificate")) {
            errorMessage += "No signing credentials given, either choose --default-key OR "
                    + "provide --privateKeyFile and --publicCertificate";
        } else {
            if (!(isValueNotNull(parameters, "--privateKeyFile")
                    && isValueNotNull(parameters, "--publicCertificate"))) {
                if (isValueNotNull(parameters, "--privateKeyFile")) {
                    errorMessage += "A signing certificate is missing. ";
                } else {
                    errorMessage += "A private key is missing. ";
                }
            }
        }
    }
}
