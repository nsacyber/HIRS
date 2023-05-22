package hirs.swid.utils;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.io.File;
import java.io.IOException;

public class FileArgumentValidator implements IParameterValidator {
    public void validate(String name, String value) throws ParameterException {
        try {
            File file = new File(value);
            if (!file.isFile()) {
                throw new ParameterException("Invalid file path: " + value +
                        ". Please verify file path.");
            }
        } catch (NullPointerException e) {
            throw new ParameterException("File path cannot be null: " + e.getMessage());
        } catch (SecurityException e) {
            throw new ParameterException("Read access denied for " + value +
                    ", please verify permissions.");
        }
    }
}
