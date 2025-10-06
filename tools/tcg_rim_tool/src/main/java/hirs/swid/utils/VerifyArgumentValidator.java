package hirs.swid.utils;

import com.beust.jcommander.IParametersValidator;
import com.beust.jcommander.ParameterException;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * This class handles validating all arguments in the context of a verify function.
 * The input arguments are checked that --create is not also selected and that all
 * required inputs for --verify are present.
 */
@Log4j2
public class VerifyArgumentValidator implements IParametersValidator {
    private String[] requiredArgs = {"--rimel", "--truststore"};
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
        if (isValueNotNull(parameters, "--verify")) {
            if (isValueNotNull(parameters, "--create")) {
                throw new ParameterException("Create and verify cannot be called together.");
            } else {
                for (String arg : requiredArgs) {
                    if (!isValueNotNull(parameters, arg)) {
                        errorMessage += arg + " is required to verify a base RIM. ";
                    }
                }
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
        Object object = parameters.get(key);
        boolean retVal = true;
        if (object == null) {
            retVal = false;
        }
        return retVal;
    }
}
