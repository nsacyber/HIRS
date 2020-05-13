package hirs.tcglp.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;

/**
* Commander is a class that handles the command line arguments for the
* TCG Log Parser (tcglp).
*/
public class Commander {

    private static final String COMMAND_PREFIX = "-";
    private static final String FULL_COMMAND_PREFIX = "--";
    private static final String ALL_STRING = "all";
    private static final String CONTENT_STRING = "eventcontent";
    private static final String DIFF_STRING = "diff";
    private static final String EVENTIDS_STRING = "eventids";
    private static final String FILE_STRING = "file";
    private static final String HELP_STRING = "help";
    private static final String HEX_STRING = "hex";
    private static final String OUTPUT_STRING = "output";
    private static final String PCR_STRING = "tpmpcrs";
    private static final String VERIFY_STRING = "Verify";

    private boolean hasArguments = false;
    private boolean bAll = false;
    private boolean bContent = false;
    private boolean bDiff = false;
    private boolean bEventIds = false;
    private boolean bFile = false;
    private boolean bHex = false;
    private boolean bOutput = false;
    private boolean bPCRs = false;
    private boolean bVerify = false;

    private String inFile = "";
    private String outFile = "";
    
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
            printHelp("");
        }
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
                case FULL_COMMAND_PREFIX + ALL_STRING:
                case COMMAND_PREFIX + "a":
                    bAll = true;
                    break;
                case FULL_COMMAND_PREFIX + CONTENT_STRING:
                case COMMAND_PREFIX + "c":
                    bContent = true;
                    break;
                case FULL_COMMAND_PREFIX + DIFF_STRING:
                case COMMAND_PREFIX + "d":
                    bDiff = true;
                    break;
                case FULL_COMMAND_PREFIX + EVENTIDS_STRING:
                case COMMAND_PREFIX + "e":
                    bEventIds = true;
                    break;
                case FULL_COMMAND_PREFIX + FILE_STRING:
                case COMMAND_PREFIX + "f":
                    bFile = true;
                    inFile = args[++i];
                    break;
                case FULL_COMMAND_PREFIX + HEX_STRING:
                case COMMAND_PREFIX + "x":
                    bHex = true;
                    break;
                case FULL_COMMAND_PREFIX + OUTPUT_STRING:
                case COMMAND_PREFIX + "o":
                    bOutput = true;
                    outFile = args[++i];
                    break;
                case FULL_COMMAND_PREFIX + PCR_STRING:
                case COMMAND_PREFIX + "p":
                    bPCRs = true;
                    break;
                case FULL_COMMAND_PREFIX + VERIFY_STRING:
                case COMMAND_PREFIX + "V":
                    bVerify = true;
                    break;
                case FULL_COMMAND_PREFIX + HELP_STRING:
                case COMMAND_PREFIX + "h":
                default:
                    printHelp("");
            }
        }
    }

    /**
     * Getter for the property that indicates if something was given at the
     * commandline.
     * @return true if any arguments were passed in.
     */
    public final boolean hasArguments() {
        return hasArguments;
    }

    /**
     * Getter for the input All flag.
     * @return true if the All flag was set.
     */
    public final boolean getAllFlag() {
        return bAll;
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
        return bContent;
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
     * @return true of EventIds Falg was set.
     */
    public final boolean getEventIdsFlag() {
        return bEventIds;
    }

    /**
     * Getter for the input associated with the File flag.
     * @return true if File Flage was set.
     */
    public final boolean getFileFlag() {
        return bFile;
    }

    /**
     * Getter for the input associated with the Output flag.
     * @return true if the Output flag was set.
     */
    public final boolean getOutputFile() {
        return bOutput;
    }

    /**
     * Getter for the input associated with the diff flag.
     * @return
     */
    public final boolean getDiffFlag() {
        return bDiff;
    }

    /**
     * Getter for the input associated with the Verify flag.
     * @return
     */
    public final boolean getVerifyFile() {
        return bVerify;
    }

    /**
     * Returns the name of the output file, if provided.
     * @return name of the output file.
     */
    public final String getOutputFileName() {
        return outFile;
    }

    /**
     * Returns the name of the input file, if provided.
     * @return name of the input file.
     */
    public final String getInFileName() {
        return inFile;
    }
    /**
     * This method is used to inform the user of the allowed functionality of
     * the program.
     */
    private void printHelp(String message) {
        StringBuilder sb = new StringBuilder();
        String os = System.getProperty("os.name").toLowerCase();
        if (message != null && !message.isEmpty()) {
            sb.append(String.format("ERROR: %s\n\n", message));
        }
        sb.append("\nTCG Log Parser ");
        if (os.compareToIgnoreCase("linux")==0) {
            sb.append("Usage: sh tcglp.sh [OPTION]...-f [FILE]...\n");
        } else {
            sb.append("Usage: .tcglp.ps1 [OPTION]...-f [FILE]...\n");
        }
        sb.append("Options:\n  -a\t--all\t\t Displays everything; overrides other options.\n"
                + "  -c\t--eventcontent\t Displays event content (hex). \n\t\t\t Following paramter MAY be a event id or event id label. \n\t\t\t No following parameters will read All Events.\n" 
                + "  -d\t--diff\t\t Compares two TCG Event Logs and outputs a list of events that differred.\n"
                + "  -e\t--eventids\t Filters the output to only display events using ID's provided.\n\t\t\t ID is single work mask of the event ID's. \n\t\t\t No EventID will output all events.\n"
                + "  -f\t--file\t\t Use specific input file. \n\t\t\t Following parameter MUST be a relative path and file name.\n"
                + "  -o\t--output\t Output to a file. \n\t\t\t Following parameter MUST be a relative path and file name.\n"
                + "  -p\t--tpmpcrs\t Output PCR contents calculated from the TCG Log. \n\t\t\t Following parameter MAY be a PCR number or Text PCR[] string.\n\t\t\t No following parameters will display ALl PCRs.\n"
                + "  -v\t--Version\t Parser Version.\n" 
                + "  -V\t--Verify\t Attempts to verify the log file against values on the local device.\n"
                + "  -x\t--hex\t\t Displays event structure of each log event in hexdecimal.\n");
        if (os.compareToIgnoreCase("linux")==0) {
        sb.append("\nIf no FILE parameter is provided then the standard Linux TCGEventLog path (/sys/kernel/security/tpm0/binary_bios_measurements) is used."
                +"\n Note admin privileges may be required (e.g. use sudo when running the script).\n"
                +"All OPTIONS must be seperated by a space delimiter, no concatenation of OPTIONS is currently supported.\n"
                +"\nExamples: (run from the script directory)\n"
                +"1. Display all events from the binary_bios_measurements.bin test pattern:\n"
                +"    sh tcglp.sh -f ../test/testdata/binary_bios_measurements_Dell_Fedora30.bin -e\n"
                +"2. Display only the event with an index of 0 (e.g event that extend PCR 0):\n"
                +"    sh scripts/tcglp.sh -f ../test/testdata/binary_bios_measurements_Dell_Fedora30.bin -p 0\n"
                );
        } else { //windows
            sb.append("\nIf no FILE parameter is provided then the standard Windows TCGEventLog path (C:\\Windows\\Logs\\MeasuredBoot) is used" 
                +"\n Note admin privileges may be required (e.g. run as Administrator).\n"
                +"All OPTIONS must be seperated by a space delimiter, no concatenation of OPTIONS is currently supported.\n"
                +"\nExamples:(run from the script directory)\n"
                +"1. Display all events from the binary_bios_measurements.bin test pattern:\n"
                +"    ./tcglp.ps1 -f ..\\test\\testdata\\binary_bios_measurements_Dell_Fedora30.bin -e\n"
                +"2. Display only the event with an index of 0 (e.g event that extend PCR 0):\n"
                +"    ./tcglp.ps1 -f ..\\test\\testdata\\binary_bios_measurements_Dell_Fedora30.bin -p 0\n"
                );
        }
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
