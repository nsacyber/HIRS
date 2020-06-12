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
    private static final String VERSION_NUMBER = "1.0";

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
            String[] defualtArgs=new String[1];
            defualtArgs[0] = "-e";
            hasArguments = true;
            parseArguments(defualtArgs);
 //           printHelp("");
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
                            if(eventFilter.chars().allMatch( Character::isDigit )) {
                                eventNumber = Integer.parseInt(eventFilter);
                            } else {
                              System.out.println("invalid parameter following -e: " + eventFilter);
                              System.exit(1);
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
                        System.out.print("tcg_eventlog_tool command line error:"
                                        +  " 2 or 3 parameters needed for -diff.\n");
                        System.out.print("usage: elt -d logFile1 logFile2 pcr#");
                        bValidArgs = false;
                    } else {
                        inFile = args[i++  + 1];
                        inFile2 = args[i++ + 1];
                        bDiff = true;
                    }
                    break;
                case FULL_COMMAND_PREFIX + FILE_STRING:
                case COMMAND_PREFIX + "f":
                    bFile = true;
                    inFile = args[++i];
                    break;
                case FULL_COMMAND_PREFIX + OUTPUT_STRING:
                case COMMAND_PREFIX + "o":
                    if (i < args.length - 1) {  // Check for a filter following the -o
                        if (!args[i + 1].startsWith("-")) {
                            outFile = args[i++ + 1];
                        } else {
                            System.out.print("no output file specified with -o option");
                            bValidArgs = false;
                        }
                    }
                    bOutput = true;
                    break;
                case FULL_COMMAND_PREFIX + PCR_STRING:
                case COMMAND_PREFIX + "p":
                    if (i < args.length - 1) {  // Check for a filter following the -p
                        if (!args[i + 1].startsWith("-")) {
                            pcrFilter = args[i++ + 1 ];
                            if(pcrFilter.chars().allMatch( Character::isDigit )) {
                                pcrNumber = Integer.parseInt(pcrFilter);
                            } else {
                              System.out.println("invalid parameter following -p: " + pcrFilter);
                              System.exit(1);
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
                    printHelp("");
                    bValidArgs = false;
            }
          }
        }
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
     * This method is used to inform the user of the allowed functionality of the program.
     * @param message message caller specific message to print before listing the help.
     */
    public final void printHelp(final String message) {
        StringBuilder sb = new StringBuilder();
        String os = System.getProperty("os.name").toLowerCase();
        if ((message != null) && (!message.isEmpty())) {
            sb.append("\n\n" + message);
        }
        sb.append("\nTCG Log Parser ");
        if (os.compareToIgnoreCase("linux") == 0) {
            sb.append("Usage: sh elt.sh [OPTION]...-f [FILE]...\n");
        } else {
            sb.append("Usage: ./elt.ps1 [OPTION]...-f [FILE]...\n");
        }
        sb.append("Options:\n"
                + "  -f\t--file\t\t Use specific Event Log file. "
                + "\n\t\t\t Following parameter MUST be a path and file name."
                + "\n\t\t\t The local Event Log file will be used if this option is not present."
                + "\n\t\t\t Note: Access to the local Event Log may require admin privileges.\n"
                + "  -e\t--event\t Display event descriptions (including event content) in "
                + "human readable form."
                + "\n\t\t\t Following optional parameter is a single event number used to filter"
                + " the output."
                + "\n\t\t\t All events will be displayed if the optional parameter is not +"
                + "provided.\n"
                + "  -ec\t--contenthex\t Displays event content"
                + " in eventhex format when -event is used.\n"
                + "  -ex\t--eventhex\t Displays event in hex format when -event is used.\n"
                + "  -d\t--diff\t\t Compares two TCG Event Logs and outputs a list of events"
                + " of the second log that differred.\n"
                + "  -o\t--output\t Output to a file. "
                + "\n\t\t\t Following parameter MUST be a relative path and file name.\n"
                + "  -p\t--pcr\t\t Output expected PCR value calculated from the "
                + "TCG Log (for PCR Replay)."
                + "\n\t\t\t Following parameter MAY be a PCR number used to specify a single pcr."
                + "\n\t\t\t No following parameters will display all PCRs.\n"
                + "  -v\t--version\t Parser Version.\n"
//                + "  -V\t--Verify\t Attempts to verify the log file against values."
                + "  -x\t--hex\t\t Displays event in hex format. Use with -ec to get content."
                + "\n\t\t\t Use -e -ec and -ex options to filter output."
                + "\n\t\t\t All output will be human readble form if not present."
                + "\n\n");
        if (os.compareToIgnoreCase("linux") == 0) {
        sb.append("\nIf no FILE parameter is provided then the standard Linux TCGEventLog path "
                + "\n(/sys/kernel/security/tpm0/binary_bios_measurements) is used."
                + "\n Note admin privileges may be required (e.g. use sudo when running the "
                + " script).\n"
                + "All OPTIONS must be seperated by a space delimiter, no concatenation"
                + " of OPTIONS is currently supported.\n"
                + "\nExamples: (run from the script directory)\n"
                + "1. Display all events from the binary_bios_measurements.bin test pattern:\n"
                + "    sh elt.sh -f ../test/testdata/binary_bios_measurements_Dell_Fedora30.bin "
                + " -e\n"
                + "2. Display only the event with an index of 0 (e.g event that extend PCR 0):\n"
                + "    sh scripts/elt.sh -f "
                + "../test/testdata/binary_bios_measurements_Dell_Fedora30.bin -p 0\n"
                );
        } else { //windows
            sb.append("\nIf no FILE parameter is provided then the "
                    + "standard Windows TCGEventLog path (C:\\Windows\\Logs\\MeasuredBoot) is used"
                + "\n Note admin privileges may be required (e.g. run as Administrator).\n"
                + "All OPTIONS must be seperated by a space delimiter, "
                +  "no concatenation of OPTIONS is currently supported.\n"
                + "\nExamples:(run from the script directory)\n"
                + "1. Display all events from the binary_bios_measurements.bin test pattern:\n"
                + "    ./elt.ps1 -f "
                + "..\\test\\testdata\\binary_bios_measurements_Dell_Fedora30.bin -e\n"
                + "2. Display only the event with an index of 0 (e.g event that extend PCR 0):\n"
                + "    ./elt.ps1 -f "
                + "..\\test\\testdata\\binary_bios_measurements_Dell_Fedora30.bin -p 0\n"
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
