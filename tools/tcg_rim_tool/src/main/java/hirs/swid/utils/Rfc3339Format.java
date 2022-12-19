package hirs.swid.utils;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public class Rfc3339Format implements IParameterValidator {
    public void validate(String name, String value) throws ParameterException {
        if (value != null) {
            try {
                Instant instant = Instant.parse(value);
            } catch (DateTimeParseException e) {
                e.printStackTrace();
                throw new ParameterException("Parameter " + name + "=" + value +
                        " is not in valid RFC3339 format; " +
                        "expected format is yyyy-MM-dd'T'hh:mm:ss'Z'");
            }
        } else {
            return;
        }
    }
}
