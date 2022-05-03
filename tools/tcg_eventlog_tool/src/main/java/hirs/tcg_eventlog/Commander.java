package hirs.tcg_eventlog;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;

/**
* Commander is a class that handles the command line arguments for the
* TCG Log Parser (tcg_eventlotool).
*/
public class Commander {

    private static final String COMMAND_PREFIX = "-";
    private static final String FULL_COMMAND_PREFIX = "--";
    private static final String CONTENT_STRING = "contenthex";
    private static final String DIFF_STRING = "diff";
    private static final String EVENTIDS_STRING = "event";
    private static final String FILE_STRING = "file";
    private static final String HELP_STRING = "help";
    private static final String EVENTHEX_STRING = "eventhex";
    private static final String HEX_STRING = "hex";
    private static final String OUTPUT_STRING = "output";
    private static final String PCR_STRING = "pcr";
    private static final String VERIFY_STRING = "Verify";
    private static final String VERSION_STRING = "version";
    private static final String VERSION_NUMBER = "2.1";
    private static final String REGEX = "[0-9]+";

    private boolean hasArguments = false;
    private boolean bValidArgs = true;
    private boolean bContentHex = false;
    private boolean bDiff = false;
    private boolean bEventIds = false;
    private boolean bFile = false;
    private boolean bEventHex = false;
    private boolean bHex = false;
    private boolean bOutput = false;
    private boolean bPCRs = false;
    private boolean bVerify = false;
    private boolean bHelp = false;
    private boolean bDone = false;

    private String inFile = "";
    private String inFile2  = "";
    private String outFile = "";
    private String eventFilter = "";
    private String pcrFilter = "";
    private int pcrNumber = -1;
    private int eventNumber = -1;

    /**
     * The main constructor for the Commander class.
     *
     * @param args inout parameters
     */
    public Commander(final String[] args) {
        hasArguments = args.length > 0;

        if (hasArguments) {
            parseArguments(args);
        } else {
            String[] defualtArgs = new String[1];
            defualtArgs[0] = "-e";
            hasArguments = true;
            parseArguments(defualtArgs);
        }
    }

    /**
     * This method is called if an empty Commander was created, and later gets args.
     * Will be used by the main constructor.
     *
     * @param args input parameters
     */
    public final void parseArguments(final String[] args) {
        String tempValue;

        for (int i = 0; i < args.length; i++) {
            tempValue = args[i];

            if (bDone) {
                  break;
            }
        if (args.length == 0) {    // Process default params if none were given
            bEventIds = true;
        } else {
            switch (tempValue) {
                case FULL_COMMAND_PREFIX + CONTENT_STRING:
                case FULL_COMMAND_PREFIX + EVENTIDS_STRING:
                case COMMAND_PREFIX + "e":
                    if (i < args.length - 1) {  // Check for a filter following the -e
                        if (!args[i + 1].startsWith("-")) {
                            eventFilter = args[i++ + 1];
                            if (eventFilter.matches(REGEX)) {
                                eventNumber = Integer.parseInt(eventFilter);
                            } else {
                              printHelp("Invalid parameter following -e: " + eventFilter
                                 + "\n");
                              bValidArgs = false;
                              bDone = true;
                            }
                        }
                    }
                    bEventIds = true;
                    break;
                case COMMAND_PREFIX + "ec":
                    bContentHex = true;
                    break;
                case FULL_COMMAND_PREFIX + EVENTHEX_STRING:
                case COMMAND_PREFIX + "ex":
                    bEventHex = true;
                    break;
                case FULL_COMMAND_PREFIX + DIFF_STRING:
                case COMMAND_PREFIX + "d":
                    if ((args.length < i + 2 + 1) || (args[i + 1].charAt(0) == '-')
                                              || (args[i + 2].charAt(0) == '-')) {
                        printHelp("tcg_eventlog_tool command line error:"
                                        +  " 2 parameters needed for -diff." + "\n");
                        bValidArgs = false;
                        bDone = true;
                    } else {
                        inFile = args[i++  + 1];
                        inFile2 = args[i++ + 1];
                        bDiff = true;
                    }
                    break;
                case FULL_COMMAND_PREFIX + FILE_STRING:
                case COMMAND_PREFIX + "f":
                   if (i == args.length - 1) {
                        printHelp("No output file specified with the " + tempValue
                        + " option" + "\n");
                        bValidArgs = false;
                        bDone = true;
                    } else if (args[i + 1].charAt(0) == '-') {
                        printHelp("No output file specified with the " + tempValue
                        + "option" + "\n");
                        bValidArgs = false;
                        bDone = true;
                    } else {
                        bFile = true;
                        inFile = args[++i];
                    }
                    break;
                case FULL_COMMAND_PREFIX + OUTPUT_STRING:
                case COMMAND_PREFIX + "o":
                    if (i == args.length - 1) {
                        printHelp("No output file specified with the " + tempValue
                        + " option" + "\n");
                        bValidArgs = false;
                        bDone = true;
                    } else {
                       outFile = args[i++ + 1];
                       if (outFile.isEmpty()) {
                           printHelp("No output file specified with the " + tempValue
                            + "option" + "\n");
                           bValidArgs = false;
                           bDone = true;
                         }
                    }
                    bOutput = true;
                    break;
                case FULL_COMMAND_PREFIX + PCR_STRING:
                case COMMAND_PREFIX + "p":
                    if (i < args.length - 1) {  // Check for a filter following the -p
                        if (!args[i + 1].startsWith("-")) {
                            pcrFilter = args[i++ + 1 ];
                            if (pcrFilter.matches(REGEX)) {
                                pcrNumber = Integer.parseInt(pcrFilter);
                            } else {
                              printHelp("Invalid parameter following -p: "
                              + pcrFilter + "\n");
                              bValidArgs = false;
                              bDone = true;
                            }
                        }
                    }
                    bPCRs = true;
                    break;
                case FULL_COMMAND_PREFIX + VERSION_STRING:
                case COMMAND_PREFIX + "v":
                    System.out.print("TCG Event Log Parser version " + VERSION_NUMBER + "\n");
                    bDone = true;
                    break;
                case FULL_COMMAND_PREFIX + VERIFY_STRING:
                case COMMAND_PREFIX + "V":
                    bVerify = true;
                    break;
                case FULL_COMMAND_PREFIX + HEX_STRING:
                case COMMAND_PREFIX + "x":
                    bHex = true;
                    break;
                case FULL_COMMAND_PREFIX + HELP_STRING:
                case COMMAND_PREFIX + "h":
                    bHelp = true;
                    break;
                default:
                    //System.out.print("Unknown option: " + tempValue + "\n");
                    bValidArgs = false;
                    bDone = true;
                    printHelp("Unknown option: " + tempValue + "\n");
            }
          }
        }
        checkForInvalidOptions();
        checkDefaults();
    }

    /**
     * Getter for the property that indicates if something was given at the command line.
     *
     * @return true if any arguments were passed in.
     */
    public final boolean hasArguments() {
        return hasArguments;
    }
    /**
     * Getter for the validity of the commands.
     * @return true if the All flag was set.
     */
    public final boolean getValidityFlag() {
        return bValidArgs;
    }
    /**
     * Getter for the Done flag.
     * @return true if the Done flag was set.
     */
    public final boolean getDoneFlag() {
        return bDone;
    }
    /**
     * Getter for the help flag.
     * @return true if the Help flag was set.
     */
    public final boolean getHelpFlag() {
        return bHelp;
    }
    /**
     * Getter for the input associated with the PCR flag.
     * @return true if the PCR Flag was set.
     */
    public final boolean getPCRFlag() {
        return bPCRs;
    }
    /**
     * Getter for the input associated with the Event flag.
     * @return true if the Event Flag was set.
     */
    public final boolean getContentFlag() {
        return bContentHex;
    }
    /**
     * Getter for the input associated with the Event Hex flag.
     * @return true if the Hex Flag was set.
     */
    public final boolean getEventHexFlag() {
        return bEventHex;
    }
    /**
     * Getter for the input associated with the Hex flag.
     * @return true if the Hex Flag was set.
     */
    public final boolean getHexFlag() {
        return bHex;
    }
    /**
     * Getter for the input associated with the EventIds flag.
     * @return true of EventIds Flag was set.
     */
    public final boolean getEventIdsFlag() {
        return bEventIds;
    }
    /**
     * Getter for the input associated with the File flag.
     * @return true if File Flag was set.
     */
    public final boolean getFileFlag() {
        return bFile;
    }
    /**
     * Getter for the input associated with the diff flag.
     * @return true if the diff flag was set
     */
    public final boolean getDiffFlag() {
        return bDiff;
    }
    /**
     * Getter for the input associated with the Verify flag.
     * @return true if the verify flag was set
     */
    public final boolean getVerifyFile() {
        return bVerify;
    }
    /**
     * Returns the name of the input file, if provided.
     * @return name of the input file.
     */
    public final String getInFileName() {
        return inFile;
    }
    /**
     * Returns the name of the 2nd input file, if provided.
     * @return name of the 2nd input file.
     */
    public final String getInFile2Name() {
        return inFile2;
    }
    /**
     * Returns the name of the 2nd input file, if provided.
     * @return name of the 2nd input file.
     */
    public final String getEventFilter() {
        return eventFilter;
    }
    /**
     * Returns the name of the 2nd input file, if provided.
     * @return name of the 2nd input file.
     */
    public final int getEventNumber() {
        return eventNumber;
    }
    /**
     * Getter for the input associated with the Output flag.
     * @return true if the Output flag was set.
     */
    public final boolean getOutputFlag() {
        return bOutput;
    }
    /**
     * Returns the name of the output file, if provided.
     * @return name of the output file.
     */
    public final String getOutputFileName() {
        return outFile;
    }
    /**
     * Returns the name of the 2nd input file, if provided.
     * @return name of the 2nd input file.
     */
    public final String getPcrFilter() {
        return pcrFilter;
    }
    /**
     * Returns the name of the 2nd input file, if provided.
     * @return name of the 2nd input file.
     */
    public final int getPcrNumber() {
        return pcrNumber;
    }
    /**
     * Setter for the input associated with the EventIds flag.
     */
    public final void setEventIdsFlag() {
        bEventIds = true;
    }
    /**
     * Check for invalid option combinations.
     * @return false is an invalid combination was found
     */
    public final boolean checkForInvalidOptions() {
        bValidArgs = false;
        if (!bEventIds && (bEventHex || bContentHex)) {
            return false;
        }
        if (bHex && (bEventHex || bContentHex)) {
            return false;
        }
        bValidArgs = true;
        return true;
    }
    /**
     * Check for situations where default values need to be set.
     */
    public final void checkDefaults() {
      if (bFile) {
             if (!bHex && !bEventIds && !bContentHex && !bPCRs) {
                 bEventIds = true;
             }
         }
      if (bOutput) {
          if (!bHex && !bEventIds && !bContentHex && !bPCRs) {
              bEventIds = true;
          }
      }
    }
    /**
     * This method is used to inform the user of the allowed functionality of the program.
     * @param message message caller specific message to print before listing the help.
     */
    public final void printHelp(final String message) {
        StringBuilder sb = new StringBuilder();
        String os = System.getProperty("os.name").toLowerCase();
        if ((message != null) && (!message.isEmpty())) {
            sb.append("\n" + message);
        }
        sb.append("\nTCG Log Parser ");
        if (os.compareToIgnoreCase("linux") == 0) {
            sb.append("Usage: elt [OPTION]... [OPTION]...\n");
        } else {
            sb.append("Usage: ./elt.ps1 [OPTION]... [OPTION]...\n");
        }
        sb.append("\nOptions:\n"
                + "  -f\t--file\t\t Use specific Event Log file. "
                + "\n\t\t\t example: elt [-f|--file] /path/to/eventlogfile\n"
                + "  -e\t--event\t\t Display all event detials for a specific event"
                + "\n\t\t\t example: elt [-e|--event] 30"
                + "\n\t\t\t no event specified will default to all events"
                + "\n\t\t\t example: elt [-e|--event]\n"
                + "  -ec\t--contenthex\t Include event content in hex format."
                + " Only valid with -e option.\n"
                + "  -ex\t--eventhex\t Include event only (no content) in hex format."
                + " Only valid with -e option.\n"
                + "  -d\t--diff\t\t Compares two TCG Event Logs and displays events from second"
                + " file that do not match."
                + "\n\t\t\t example: elt [-d|--diff] /path/to/eventlogfile1 "
                + "/path/to/eventlogfile2\n"
                + "  -o\t--output\t Redirect to a file in the current working directory unless a"
                + " path is specified. "
                + "\n\t\t\t example: elt [-o|--output] /path/to/outputfile\n"
                + "  -p\t--pcr\t\t Display all expected PCR values calculated from the TCG Log"
                + "(for PCR Replay)."
                + "\n\t\t\t Specify a PCR number to filter on a single PCR."
                + "\n\t\t\t example: elt [-p|--pcr] 5\n"
                + "  -v\t--version\t Version info.\n"
                + "  -x\t--hex\t\t Event only (no content) in hex format."
                + "\n\n");
        if (os.compareToIgnoreCase("linux") == 0) {
        sb.append("\nIf no file parameter is provided then the standard Linux TCGEventLog path "
                + "\n(/sys/kernel/security/tpm0/binary_bios_measurements) is used."
                + "\nIf no parameter is given then the -e option will be used as default."
                + "\n Note admin privileges may be required (e.g. use sudo when running the "
                + " script).\n"
                + "All OPTIONS must be seperated by a space delimiter, no concatenation"
                + " of OPTIONS is currently supported.\n"
                );
        } else { //windows
            sb.append("\nIf no file parameter is provided then the "
                + "standard Windows TCGEventLog path (C:\\Windows\\Logs\\MeasuredBoot) is used"
                + "\nIf no parameter is given then the -e option will be used as default."
                + "\n Note admin privileges may be required (e.g. run as Administrator).\n"
                + "All OPTIONS must be seperated by a space delimiter, "
                +  "no concatenation of OPTIONS is currently supported.\n"
                );
        }
        System.out.println(sb.toString());
    }

    /**
     * Checks that the file path is a valid.
     * @param filepath file path of file to check
     * @return true if path is valid
     */
    public static boolean isValidPath(final String filepath) {
        try {
            System.out.println("Checking for a valid creation path...");
            File file = new File(filepath);
            boolean test = file.createNewFile();
            if (!test) {
                return false;
            }
        } catch (IOException | InvalidPathException | NullPointerException ex) {
            return false;
        }
        return true;
    }
}
