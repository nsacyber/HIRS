package hirs.tcglp.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import hirs.tpm.eventlog.TCGEventLog;

/**
 * Command-line application for processing TCG Event Logs.
 * Input arg: path to *.tcglp file
 *
 * If an argument is given it will be validated against the schema at http://standards.iso.org/iso/19770/-2/2015/schema.xsd
 * If an argument is not given a SWID tag file will be generated.
 */
public class Main {
    private static Commander commander = null;
    static FileOutputStream outputStream = null;
    static byte[] eventLlog = null;
    static boolean bContentFlag, bEventFlag, bHexEvent, bOutFile = false;
    public static void main(String[] args) {
    commander = new Commander(args);
    String os = System.getProperty("os.name").toLowerCase();

        if (commander.hasArguments()) {
            // we have arguments to work with
            if (commander.getFileFlag()) {
                eventLlog = openLog(commander.getInFileName());
            } else {
                eventLlog = openLog("");
            }
            if (commander.getAllFlag()) {
                System.out.print("All option is not yet implemented");
                System.exit(1);
            }
            if (commander.getPCRFlag()) {
                try {
                    TCGEventLog tlp = new TCGEventLog(eventLlog, bEventFlag, bContentFlag, bHexEvent);
                    String[] pcrs = tlp.getExpectedPCRValues();
                    int i=0;
                    System.out.print("Platform Configuration Register (PCR) values: \n\n");
                    for (String pcr: pcrs) {
                        System.out.print(" pcr "+ i++ + " = " + pcr.toString() + "\n");
                    }
                } catch (Exception e) {
                    System.out.print("Error processing Event Log " + commander.getInFileName() 
                    + "\nError was "+ e.toString());
                    System.exit(1);
                } 
                System.out.print("\n----------------- End PCR Values ----------------- \n\n");
            }
            if (commander.getContentFlag()) {
                bContentFlag = true;
            }
            if (commander.getHexFlag()) {
                bHexEvent = true;
            }
            if (commander.getEventIdsFlag()) {
                bEventFlag = true;
            }
            if (commander.getOutputFile()) {
                try {
                    outputStream = new FileOutputStream(commander.getOutputFileName());
                } catch (FileNotFoundException e) {
                    System.out.print("Error opening output file" + commander.getOutputFileName() 
                                 + "\nError was "+ e.getMessage());
                     System.exit(1);
                    }
            }
            if (commander.getVerifyFile()) {
                System.out.print("Verify option is not yet implemented");
                System.exit(1);
            }

        } else {
            System.out.print("Nothing to do: No Parameters provided.");
            System.exit(1);
        }
        
    try {
        TCGEventLog tlp = new TCGEventLog(eventLlog, bEventFlag, bContentFlag, bHexEvent);
        writeOut(tlp.toString());                 
    } catch (Exception e) {
        System.out.print("Error processing Event Log " + commander.getInFileName() 
        + "\nError was "+ e.toString());
        System.exit(1);
    }
        
}


    /**
     * Opens a TCG Event log file
     * @param fileName  Name of the log file. Will use a OS specific default file if none is supplied.
     * @param os the name os of the current system
     * @return a byte array holding the entire log
     */
    public static byte[] openLog(String fileName) {
        String os = System.getProperty("os.name").toLowerCase();
        byte[] rawLog=null;
        boolean bDefault = false;
        try {
            
            if (fileName == "") {
                if (os.compareToIgnoreCase("linux")==0) { // need to find Windows path
                    fileName = "/sys/kernel/security/tpm0/binary_bios_measurements";
                    bDefault = true;
                    writeOut("Local Event Log being used: "+fileName +"\n");
                }
            }
            Path path = Paths.get(fileName);
            rawLog = Files.readAllBytes(path);
            writeOut("TPM Event Log parser using file:"+ path +"\n\n"); 
            
        } catch (Exception e) {
            String error = "Error reading event Log File: " +  e.toString();
            if (bDefault) {
                error += "\nTry using the -f option to specify an Event Log File";
            }
            writeOut(error);
            System.exit(1);
        }
        return rawLog;
    }
    
    /**
     * Write data out to the system and/or a file.
     * @param data
     */
    private static void writeOut(String data) {
        try {
            data = data.replaceAll("[^\\P{C}\t\r\n]", ""); // remove any null characters that seem to upset text editors
            if(commander.getOutputFile()) outputStream.write(data.getBytes());   // Write to an output file
            System.out.print(data);                // output to the console
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
