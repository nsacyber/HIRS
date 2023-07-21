package hirs.swid.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimestampArgumentValidator {
    List<String> args;

    /**
     * This class handles validation of the --timestamp commandline parameter.
     * Currently RFC3339, RFC3852, and RFC2315 (PKCS7) formats are supported.
     *
     * @param args list of arguments from command line
     */
    public TimestampArgumentValidator(List<String> args) {
        this.args = args;
    }

    /**
     * This is the public access method through which all other methods are called.
     *
     * @return true if all arguments are valid, false otherwise
     */
    public boolean isValid() {
        if (isExactlyOneFormat(args)) {
            if (args.get(0).equalsIgnoreCase("RFC3852") ||
                    args.get(0).equalsIgnoreCase("RFC2315")) {
                if (args.size() > 1) {
                    if (isCountersignatureFileValid(args.get(1))) {
                        return true;
                    } else {
                        return false;
                    }
                } else if (args.size() == 1) {
                    System.out.println("Countersignature file is required for " +
                            "RFC3852 and RFC2315 (PKCS7) timestamps");
                    return false;
                }
            } else if (args.get(0).equalsIgnoreCase("RFC3339")) {
                if (args.size() > 1) {
                    if (isRfc3339Format(args.get(1))) {
                        return true;
                    } else {
                        return false;
                    }
                } else if (args.size() == 1) {
                    return true;
                }
            } else {
                System.out.println("Unsupported timestamp format specified");
                return false;
            }
        }
        return false;
    }

    /**
     * This method ensures that exactly one format is specified.
     *
     * @param args list of command line arguments
     * @return true if exactly one format is specified, false otherwise
     */
    private boolean isExactlyOneFormat(List<String> args) {
        Pattern pattern = Pattern.compile("(R|r)(F|f)(C|c)(3339|3852|2315)");
        String format = args.get(0);
        Matcher formatMatcher = pattern.matcher(format);

        if (!formatMatcher.matches()) {
            System.out.println("Invalid timestamp format specified. " +
                    "Please choose from RFC3339, RFC3852, or RFC2315.");
            return false;
        }
        if (args.size() == 2) {
            String argument = args.get(1);
            Matcher argumentMatcher = pattern.matcher(argument);
            if (argumentMatcher.matches()) {
                System.out.println("Only one timestamp format may be specified at a time.");
                return false;
            }
        }

        return true;
    }

    /**
     * This method verifies a user-given RFC3339 timestamp
     *
     * @param timestamp the timestamp string
     * @return true if valid RFC3339 format, false otherwise
     */
    private boolean isRfc3339Format(String timestamp) {
        try {
            Instant instant = Instant.parse(timestamp);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid RFC3339 timestamp given, " +
                    "expected yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            return false;
        }
        return true;
    }

    /**
     * This method verifies the counter signature file
     *
     * @param file the counter signature
     * @return true if file exists and is valid, false otherwise
     */
    private boolean isCountersignatureFileValid(String file) {
        if (file != null && !file.isEmpty()) {
            try {
                Files.readAllBytes(Paths.get(file));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            System.out.println("RFC3852 and RFC2315 (PKCS7) formats require " +
                    "a filename input of the countersignature file.");
            return false;
        }
        return true;
    }
}
