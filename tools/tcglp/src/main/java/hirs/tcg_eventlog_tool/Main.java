package hirs.tcg_eventlog_tool;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import hirs.tpm.eventlog.TCGEventLog;
import hirs.tpm.eventlog.TpmPcrEvent;
import hirs.utils.HexUtils;

/**
 * Command-line application for processing TCG Event Logs.
 * Input arg: path to *.tcglp file
 *
 */
public class Main {
    private static Commander commander = null;
    static FileOutputStream outputStream = null;
    static byte[] eventLog = null;
    static boolean bContentFlag, bEventFlag, bHexEvent, bHexFlag, bPcrFlag, bOutFile = false;
    public static void main(String[] args) {
    commander = new Commander(args);
        if (commander.hasArguments()) {
            if (commander.getOutputFlag()) {
                try {
                    outputStream = new FileOutputStream(commander.getOutputFileName());
                    } catch (FileNotFoundException e) {
                    System.out.print("Error opening output file" + commander.getOutputFileName() 
                                 + "\nError was "+ e.getMessage());
                     System.exit(1);
                    }
            }
            if (commander.getFileFlag()) {
                eventLog = openLog(commander.getInFileName());
            }
            if (commander.getAllFlag()) {
                System.out.print("All option is not yet implemented");
                System.exit(1);
            }
            if (commander.getContentFlag()) {
                bContentFlag = true;
            }
            if (commander.getDiffFlag()) {
                bEventFlag = true;
                if(commander.getHexFlag()) {
                    bHexFlag=bHexEvent = bContentFlag = true;
                }
                String results = compareLogs (commander.getInFileName(),commander.getInFile2Name());
                writeOut(results);
                System.exit(0);
            }
            if (commander.getEventIdsFlag()) {
                bEventFlag = true;
            }
            if (commander.getEventHexFlag()) {
                bHexEvent = true;
            }
            if (commander.getPCRFlag()) {
                bPcrFlag = true;
            }
            if (commander.getVerifyFile()) {
                System.out.print("Verify option is not yet implemented");
                System.exit(1);
            }
            if (commander.getHexFlag()) {
                bHexFlag = true;
            }
        } else {
            System.out.print("Nothing to do: No Parameters provided.");
            System.exit(1);
        }   // End commander processing

    try {
        if (eventLog == null)  {
            eventLog = openLog("");
        }
        // Main Event processing 
        TCGEventLog evLog = new TCGEventLog(eventLog, bEventFlag, bContentFlag, bHexEvent);
        
        // Check for pcr flag
        if (bPcrFlag) {
            String[] pcrs = evLog.getExpectedPCRValues();
            int count = 0;
            if(!commander.getHexFlag()) {
                  writeOut("Expected Platform Configuration Register (PCR) values"
                    + " derived from the Event Log: \n\n");
            }
            for (String pcr: pcrs) {
                if(count++ == commander.getPcrNumber() || (commander.getPcrNumber() == -1)) {
                    if(bHexFlag) {
                        writeOut(pcr.toString()+"\n");
                    } else {
                        writeOut(" pcr " + (count-1) + " = " + pcr.toString() + "\n");
                    }
                }
            }
            if(!bHexFlag) {
                  writeOut("\n----------------- End PCR Values ----------------- \n\n");
          }
        }

        // General event log output
        if (bEventFlag) {
                for (TpmPcrEvent event: evLog.getEventList()) {
                    if ((commander.getEventNumber() == event.getPcrIndex())|| commander.getEventNumber() == -1) {
                        if(bHexFlag) {
                             if(bEventFlag || bHexEvent) {
                                 writeOut(HexUtils.byteArrayToHexString(event.getEvent())+ "\n");
                             } 
                             if(bContentFlag) {
                                 writeOut(HexUtils.byteArrayToHexString(event.getEventContent())+ "\n");
                             }
                        }
                        else {
                             writeOut(event.toString(bEventFlag, bContentFlag, bHexEvent) + "\n");
                        }
                }
            } 
        }
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
            if(!commander.getHexFlag()) {
               writeOut("TPM Event Log parser opening file:"+ path +"\n\n"); 
            }
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
            if(commander.getOutputFlag()) {
                outputStream.write(data.getBytes());   // Write to an output file
            } else {
                System.out.print(data);                // output to the console
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Compares 2 Event Logs and returns a string based upon the results.
     * Uses the Events digest field for comparisons. 
     * @param LogFileName1 Log file to use as a reference.
     * @param LogFileName2 Log file to compare to the refernce.
     * @return A sting containing human readable results. 
     */
    public static String compareLogs (String LogFileName1, String LogFileName2) {
        TCGEventLog eventLog = null, eventLog2 = null;
        byte[] evLog = openLog(LogFileName1);
        byte[] evLog2 = openLog(LogFileName2);
        StringBuilder sb = new StringBuilder();
        try {
            eventLog = new TCGEventLog(evLog);
        } catch (Exception e) {
             sb.append("Error processing event log " + LogFileName1   + " : " + e.getMessage());
              return sb.toString();
        } try { 
            eventLog2 = new TCGEventLog(evLog2);
            ArrayList<TpmPcrEvent>  errors = diffEventLogs(eventLog.getEventList(),
                    eventLog2.getEventList(), commander.getPcrNumber() );
            if (errors.isEmpty() && !bHexFlag) {
                sb.append("Event Log " + LogFileName1 + " MATCHED EventLog "+ LogFileName2);
            } else {
                if (!errors.isEmpty() && !bHexFlag) {
                    sb.append("Event Log " + LogFileName1 
                            + " did NOT match EventLog " + LogFileName2 + "\n");
                    sb.append("There were " + errors.size()  + " event mismatches: \n\n");
                    }
                for (TpmPcrEvent error : errors ) {
                    if(bHexFlag) {
                        if(bEventFlag || bHexEvent) {
                            writeOut(HexUtils.byteArrayToHexString(error.getEvent())+ "\n");
                        } 
                        if(bContentFlag) {
                            writeOut(HexUtils.byteArrayToHexString(error.getEventContent())+ "\n");
                        }
                   }
                   else {
                        writeOut(error.toString(bEventFlag, bContentFlag, bHexEvent) + "\n");
                   }
                }
            }
        } catch (Exception e) {
            writeOut("Error processing event log " + LogFileName2   + " : " + e.getMessage());
        }
        return sb.toString();
    }
    /**
     * Compare this event log against a second event log.
     * Returns a String Array of event descriptions in which the digests from the first
     *  did no match the second. Return value is null if all events matched.  
     * @param eventList initial events 
     * @param eventList2 events to compare against
     * @param pcr used as a filter. Use -1 to check all pcrs.
     * @return array list of strings. Null of no events mismatched.
     */
    public static ArrayList<TpmPcrEvent> diffEventLogs(ArrayList<TpmPcrEvent> eventList,
                                              ArrayList<TpmPcrEvent> eventList2, int pcr) {
        ArrayList<TpmPcrEvent> results= new ArrayList<TpmPcrEvent>();
        for (TpmPcrEvent event2 : eventList2) {
            if(pcr >= 0) {
                if (event2.getPcrIndex() == pcr) {
                    if(!digestMatch(eventList,event2)) {
                        results.add(event2);
                        }
                } 
            } else {
                if(!digestMatch(eventList,event2)) {
                    results.add(event2);
                    }
            }
        }
        return results;
    }

    /**
     * Checks a digest from a single event against all digests with the same index in an Event Log.
     * @param eventLog The Reference Event log.
     * @param event single event to match.
     * @return
     */
    private static boolean digestMatch(final ArrayList<TpmPcrEvent> eventLog, final TpmPcrEvent event) {
        boolean matchFound = false;
        for (TpmPcrEvent event2 : eventLog) {
            if((event.getPcrIndex() == event2.getPcrIndex())
                    && (Arrays.equals(event.getEventDigest(), event2.getEventDigest()))) {
                matchFound = true;
            }
        }
        return matchFound;
    }

}
