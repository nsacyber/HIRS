package hirs.swid.utils;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import lombok.extern.log4j.Log4j2;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class validates arguments that take a directory path as an argument
 * The directory path is checked for existence and permissions.
 */
@Log4j2
public class DirectoryArgumentValidator implements IParameterValidator {
    /**
     * This implementation checks that a directory path exists and can be accessed.
     *
     * @param name "-l | --rimel"
     * @param value directory path
     * @throws ParameterException if errors are encountered
     */
    public void validate(final String name, final String value) throws ParameterException {
        try {
            Path directory = Paths.get(value);
            if (Files.isDirectory(directory)) {
                if (!Files.isExecutable(directory)) {
                    throw new ParameterException(String.format("%s is not traversable/searchable, "
                            + "check permissions.", directory));
                }
            }
        } catch (InvalidPathException e) {
            throw new ParameterException("Unable to convert " + value + " to a file path: "
                    + e.getMessage());
        }
    }
}
