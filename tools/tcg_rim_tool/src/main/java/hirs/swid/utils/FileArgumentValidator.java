package hirs.swid.utils;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.io.File;
import lombok.extern.log4j.Log4j2;

/**
 * This class validates arguments that take a String path to a file.
 * The file path is checked for null, and if the file is found it is checked
 * for validity, emptiness, and read permissions.
 */
@Log4j2
public class FileArgumentValidator implements IParameterValidator {
    /**
     * Validates file arguments.
     * @param name
     * @param value
     * @throws ParameterException
     */
    public void validate(final String name, final String value) throws ParameterException {
        try {
            File file = new File(value);
            if (!file.isFile()) {
                throw new ParameterException("Invalid file path: " + value
                        + ". Please verify file path.");
            }
            if (file.length() == 0) {
                throw new ParameterException("File " + value + " is empty.");
            }
        } catch (NullPointerException e) {
            throw new ParameterException("File path cannot be null: " + e.getMessage());
        } catch (SecurityException e) {
            throw new ParameterException("Read access denied for " + value
                    + ", please verify permissions.");
        }
    }
}
