package hirs.swid.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

/**
 * Commander is a class that handles the command line arguments for the SWID
 * Tags gateway.
 */
public class Commander {

    private static final String COMMAND_PREFIX = "-";
    private static final String FULL_COMMAND_PREFIX = "--";
    private static final String CREATE_STRING = "create";
    private static final String VERIFY_STRING = "verify";
    private static final String HELP_STRING = "help";
    private static final String EXAMPLE_STRING = "example";
    private static final String PARSE_STRING = "parse";
    private static final String IN_STRING = "in";
    private static final String OUT_STRING = "out";
    private static final String HASH_STRING = "hash";

    private boolean hasArguments = false;
    private boolean validate = false;
    private boolean create = false;
    private boolean parse = false;

    private String validateFile;
    private String createInFile;
    private String createOutFile;
    private String parseFile;
    private String hashAlg = null;

    /**
     * The main constructor for the Commander class
     *
     * @param args
     */
    public Commander(final String[] args) {
        hasArguments = args.length > 0;

        if (hasArguments) {
            parseArguments(args);
        } else {
            //printHelp();
        }

        if (create) {
            if (!Files.exists(Paths.get(getCreateInFile()))) {
                create = false;
                printHelp("Input file doesn't exist...");
            }

            if (hashAlg == null) {
                hashAlg = "256";
            }
            
            if (!isValidPath(getCreateOutFile())) {
                printHelp(String.format("Invalid file path on creation file...(%s)",
                        getCreateOutFile()));
            }
        }

        if (validate && create) {
            // there maybe a time in which you could validate what you created
            // but not right now
            // print the help information
            printHelp();
        }
    }

    /**
     * The default blank constructor
     */
    public Commander() {

    }

    /**
     * This method is called if an empty Commander was created, and later gets
     * args. Will be used by the main constructor.
     *
     * @param args
     */
    public final void parseArguments(final String[] args) {
        String tempValue;

        for (int i = 0; i < args.length; i++) {
            tempValue = args[i];

            switch (tempValue) {
                case FULL_COMMAND_PREFIX + CREATE_STRING:
                case COMMAND_PREFIX + "c":
                    create = true;
                    break;
                case COMMAND_PREFIX + IN_STRING:
                    if (create) {
                        createInFile = args[++i];
                    }
                    break;
                case COMMAND_PREFIX + OUT_STRING:
                    if (create) {
                        createOutFile = args[++i];
                    }
                    break;
                case FULL_COMMAND_PREFIX + VERIFY_STRING:
                case COMMAND_PREFIX + "v":
                    validate = true;
                    validateFile = args[++i];
                    break;
                case FULL_COMMAND_PREFIX + EXAMPLE_STRING:
                case COMMAND_PREFIX + "e":
                    hasArguments = false;
                    return; // default is generate
                case COMMAND_PREFIX + HASH_STRING:
                    hashAlg = args[++i];
                    break;
                case FULL_COMMAND_PREFIX + PARSE_STRING:
                case COMMAND_PREFIX + "p":
                	parse = true;
                	parseFile = args[++i];
                	break;
                case FULL_COMMAND_PREFIX + HELP_STRING:
                case COMMAND_PREFIX + "h":
                default:
                    if (Files.exists(Paths.get(args[i]))) {
                        validate = true;
                        validateFile = args[i];
                        break;
                    }

                    printHelp();
            }
        }
    }

    /**
     * Getter for the input validate file associated with the validate flag
     *
     * @return
     */
    public final String getValidateFile() {
        return validateFile;
    }

    /**
     * Getter for the input create file associated with the create flag
     *
     * @return
     */
    public final String getCreateInFile() {
        return createInFile;
    }

    /**
     * Getter for the output file for the create flag
     *
     * @return
     */
    public final String getCreateOutFile() {
        return createOutFile;
    }

    /**
     * Getter for the property that indicates if something was given at the
     * commandline.
     *
     * @return
     */
    public final boolean hasArguments() {
        return hasArguments;
    }

    /**
     * Getter for the validate command flag.
     *
     * @return
     */
    public final boolean validate() {
        return validate;
    }

    /**
     * Getter for the create command flag.
     *
     * @return
     */
    public final boolean create() {
        return create;
    }

    /**
     * Getter for the hash algorithm to be used for hash functions.
     * 
     * @return 
     */
    public final String getHashAlg() {
        return hashAlg;
    }
    
    /**
     * Getter for the parse command flag
     * 
     * @return
     */
    public final boolean parse() {
    	return parse;
    }
    
    /**
     * Getter for the file to be parsed by the parse command flag
     * 
     * @return
     */
    public final String getParseFile() {
    	return parseFile;
    }
    
    /**
     * Default no parameter help method.
     */
    private void printHelp() {
        printHelp(null);
    }

    /**
     * This method is used to inform the user of the allowed functionality of
     * the program.
     */
    private void printHelp(String message) {
        StringBuilder sb = new StringBuilder();

        if (message != null && !message.isEmpty()) {
            sb.append(String.format("ERROR: %s\n\n", message));
        }
        sb.append("Usage: HIRS_SwidTag\n");
        sb.append("   -c, --create \tTakes given input in the csv format\n"
                + "   \t-in <file>\tand produces swidtag payloads.\n"
                + "   \t-out <file>\tThe -hash argument is optional.\n"
                + "   \t-hash <algorithm>\n");
        sb.append("   -v, --verify\t\tTakes the provided input file and\n"
                + "   \t\t\tvalidates it against the schema at\n"
                + "   \t\t\thttp://standards.iso.org/iso/19770/-2/2015/schema.xsd\n");
        sb.append("   -e, --example\tCreate example swid tag file (generated_swidTag.swidtag)\n");
        sb.append("   -h, --help\t\tPrints the command help information\n");
        sb.append("   <no file>\t\tListing no command with no argument will\n"
                + "   \t\t\tcreate an example tag file\n");
        sb.append("   <file>\t\tValidate the given file argument\n");
        sb.append("   -p, --parse\t\tParse a swidtag's payload\n"
        		+ "   <file>\t\tInput swidtag");

        System.out.println(sb.toString());
        System.exit(1);
    }
    
    /**
     * Checks that the file given to create a new swidtag is a valid path.
     * @param filepath
     * @return 
     */
    public static boolean isValidPath(String filepath) {
        try {
            System.out.println("Checking for a valid creation path...");
            File file = new File(filepath);
            file.createNewFile();            
        } catch (IOException | InvalidPathException | NullPointerException ex) {
            return false;
        }
        return true;
    }
}
