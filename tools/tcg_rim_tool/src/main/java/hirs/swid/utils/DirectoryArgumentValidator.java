package hirs.swid.utils;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.io.File;

/**
 * This class validates a directory argument. If the directory is neither valid nor
 * read-accessible then an error is thrown.
 */

public class DirectoryArgumentValidator implements IParameterValidator {
    public void validate(String name, String value) throws ParameterException {
        try {
            File directory = new File(value);
            if (!directory.isDirectory()) {
                throw new ParameterException("Invalid directory given: " + value +
                        ".  Please provide a valid directory path.");
            }
        } catch (SecurityException e) {
            throw new ParameterException("Read access denied for " + value +
                    ", please verify permissions.");
        }
    }
}
