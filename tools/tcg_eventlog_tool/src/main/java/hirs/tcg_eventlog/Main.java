package hirs.tcg_eventlog;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import hirs.utils.HexUtils;

/**
 * Command-line application for processing TCG Event Logs.
 * Input arg: path to *.tcglp file
 */
final class Main {
    private static Commander commander = null;
    private static FileOutputStream outputStream = null;
    private static byte[] eventLog = null;
    private static boolean bContentFlag, bEventFlag, bHexEvent, bHexFlag, bPcrFlag = false;

    /**
     * Main Constructor.
     *
     * @param args command line parameters.
     */
    public static void main(final String[] args) {
        commander = new Commander(args);
        if (!commander.getValidityFlag()) {
            System.out.print("\nProgram exiting without processs due to issues with"
                    + " parameters provided.\n");
            commander.printHelp("");
            System.exit(0);
        }
        if (commander.hasArguments()) {
            if (commander.getDoneFlag()) {
                System.exit(0);
            }
            if (commander.getHelpFlag()) {
                commander.printHelp("");
                System.exit(0);
            }
            if (commander.getOutputFlag()) {
                try {
                    outputStream = new FileOutputStream(commander.getOutputFileName());
                    System.out.print("Writing to output file: " + commander.getOutputFileName()
                     + "\n");
                } catch (Exception e) {
                    System.out.print("Error opening output file" + commander.getOutputFileName()
                            + "\nError was " + e.getMessage());
                    System.exit(1);
                }
            }
            if (commander.getContentFlag()) {
                bContentFlag = true;
            }
            if (commander.getDiffFlag()) {
                bEventFlag = true;
                String results = compareLogs(commander.getInFileName(),
                        commander.getInFile2Name());
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
           eventLog = openLog(commander.getInFileName());
           // Main Event processing
            TCGEventLog evLog = new TCGEventLog(eventLog, bEventFlag, bContentFlag, bHexEvent);
            if (bPcrFlag) {
                String[] pcrs = evLog.getExpectedPCRValues();
                int count = 0;
                if (!bHexFlag) {
                    writeOut("Expected Platform Configuration Register (PCR) values"
                            + " derived from the Event Log: \n\n");
                }
                for (String pcr : pcrs) {
                    if (count++ == commander.getPcrNumber() || (commander.getPcrNumber() == -1)) {
                        if (bHexFlag) {
                            writeOut(pcr.toString() + "\n");
                        } else {
                            writeOut(" pcr " + (count - 1) + " = " + pcr.toString() + "\n");
                        }
                    }
                }
                if (!bHexFlag) {
                    writeOut("\n----------------- End PCR Values ----------------- \n\n");
                }
            }
            // General event log output
            if ((bEventFlag || bHexFlag) && !bPcrFlag) {
                if (!bHexFlag) {
                    if (evLog.isCryptoAgile()) {
                        writeOut("\nEvent Log follows the \"Crypto Agile\" format and has "
                                + evLog.getEventList().size() + " events:\n\n");
                    } else {
                        writeOut("\nEvent Log follows the \"SHA1\" format and has "
                                + evLog.getEventList().size() + " events:\n\n");
                    }
                }
                int eventCount = 0;
                for (TpmPcrEvent event : evLog.getEventList()) {
                    if ((commander.getEventNumber() == eventCount++)
                            || commander.getEventNumber() == -1) {
                        if ((commander.getPcrNumber() == event.getPcrIndex())
                                || commander.getPcrNumber() == -1) {
                            if (bHexFlag) {
                                if (bHexFlag || bHexEvent) {
                                    writeOut(HexUtils.byteArrayToHexString(event.getEvent())
                                            + "\n");
                                }
                                if (bContentFlag) {
                                    writeOut(HexUtils.byteArrayToHexString(event.getEventContent())
                                            + "\n");
                                }
                            } else {
                                writeOut(event.toString(bEventFlag, bContentFlag, bHexEvent)
                                        + "\n");
                            }
                        }
                    }
                }
            }
        } catch (IOException i) {
            System.out.print("IO error processing Event Log " + commander.getInFileName()
                    + "\nError was " + i.toString());
            System.exit(1);
        } catch (CertificateException c) {
            System.out.print("Certificate error processing Event Log " + commander.getInFileName()
                    + "\nError was " + c.toString());
            System.exit(1);
        } catch (NoSuchAlgorithmException a) {
            System.out.print("Algorithm error processing Event Log " + commander.getInFileName()
                    + "\nError was " + a.toString());
            System.exit(1);
        }
    }

    /**
     * Opens a TCG Event log file.
     *
     * @param fileName Name of the log file. Will use a OS specific default.
     * @return a byte array holding the entire log
     */
    public static byte[] openLog(final String fileName) {
        String os = System.getProperty("os.name").toLowerCase(), fName = fileName;
        byte[] rawLog = null;
        boolean bDefault = false;
        bHexFlag = commander.getHexFlag();
        try {
            if (fileName.isEmpty()) {
                if (os.compareToIgnoreCase("linux") == 0) { // need to find Windows path
                    fName = "/sys/kernel/security/tpm0/binary_bios_measurements";
                    bDefault = true;
                    if (!bHexFlag) {
                        writeOut("Local Event Log being used: " + fileName + "\n");
                    }
                }
            }
            Path path = Paths.get(fName);
            rawLog = Files.readAllBytes(path);
            if (!bHexFlag) {
                writeOut("tcg_eventlog_tool is opening file:" + path + "\n");
            }
        } catch (Exception e) {
            String error = "Error reading event Log File: " + e.toString();
            if (bDefault) {
                error += "\nTry using the -f option to specify an Event Log File\n";
            }
            System.out.print(error);
            System.exit(1);
        }
        return rawLog;
    }

    /**
     * Write data out to the system and/or a file.
     *
     * @param data
     */
    private static void writeOut(final String data) {
        try {
            String dataNoNull = data.replaceAll("[^\\P{C}\t\r\n]", ""); // remove null characters
            if (commander.getOutputFlag()) {
                outputStream.write(dataNoNull.getBytes(Charset.forName("UTF-8")));
            } else {
                System.out.print(dataNoNull);                // output to the console
            }
        } catch (IOException e) {
            System.out.print("Error writing to output file: " + commander.getOutputFileName()
           + "\n  error was: " + e.toString() + "\n");
            e.printStackTrace();
        }
    }

    /**
     * Compares 2 Event Logs and returns a string based upon the results.
     * Uses the Events digest field for comparisons.
     *
     * @param logFileName1 Log file to use as a reference.
     * @param logFileName2 Log file to compare to the reference.
     * @return A sting containing human readable results.
     */
    public static String compareLogs(final String logFileName1, final String logFileName2) {
        TCGEventLog eventLog1 = null, eventLog2 = null;
        byte[] evLog = openLog(logFileName1);
        byte[] evLog2 = openLog(logFileName2);
        StringBuilder sb = new StringBuilder();
        bHexFlag = commander.getHexFlag();
        try {
            eventLog1 = new TCGEventLog(evLog);
        } catch (Exception e) {
            sb.append("\nError processing event log " + logFileName1 + " : " + e.getMessage());
            return sb.toString();
        }
        try {
            eventLog2 = new TCGEventLog(evLog2);
            ArrayList<TpmPcrEvent> errors = diffEventLogs(eventLog1.getEventList(),
                    eventLog2.getEventList(), commander.getPcrNumber());
            if (errors.isEmpty() && !bHexFlag) {
                sb.append("\nEvent Log " + logFileName1 + " MATCHED EventLog " + logFileName2
                    + "\n");
            } else {
                if (!errors.isEmpty() && !bHexFlag) {
                    sb.append("\nEvent Log " + logFileName1
                            + " did NOT match EventLog " + logFileName2 + "\n");
                    sb.append("There were " + errors.size() + " event mismatches: \n\n");
                }
                for (TpmPcrEvent error : errors) {
                    if (bHexFlag) {
                        if (bEventFlag || bHexEvent) {
                            sb.append(HexUtils.byteArrayToHexString(error.getEvent()) + "\n");
                        }
                        if (bContentFlag) {
                            sb.append(HexUtils.byteArrayToHexString(error.getEventContent())
                                    + "\n");
                        }
                    } else {
                        sb.append(error.toString(bEventFlag, bContentFlag, bHexEvent) + "\n");
                    }
                }
            }
        } catch (IOException i) {
            System.out.print("IO error processing Event Log " + commander.getInFileName()
                    + "\nError was " + i.toString());
            System.exit(1);
        } catch (CertificateException c) {
            System.out.print("Certificate error processing Event Log " + commander.getInFileName()
                    + "\nError was " + c.toString());
            System.exit(1);
        } catch (NoSuchAlgorithmException a) {
            System.out.print("Algorithm error processing Event Log " + commander.getInFileName()
                    + "\nError was " + a.toString());
            System.exit(1);
        }
        return sb.toString();
    }

    /**
     * Compare this event log against a second event log.
     * Returns a String Array of event descriptions in which the digests from the first
     * did no match the second. Return value is null if all events matched.
     *
     * @param eventList  initial events.
     * @param eventList2 events to compare against.
     * @param pcr        used as a filter. Use -1 to check all pcrs.
     * @return array list of strings. Null of no events mismatched.
     */
    public static ArrayList<TpmPcrEvent> diffEventLogs(final Collection<TpmPcrEvent> eventList,
                                                       final Collection<TpmPcrEvent> eventList2,
                                                       final int pcr) {
        ArrayList<TpmPcrEvent> results = new ArrayList<TpmPcrEvent>();
        for (TpmPcrEvent event2 : eventList2) {
            if (pcr >= 0) {
                if (event2.getPcrIndex() == pcr) {
                    if (!digestMatch(eventList, event2)) {
                        results.add(event2);
                    }
                }
            } else {
                if (!digestMatch(eventList, event2)) {
                    results.add(event2);
                }
            }
        }
        return results;
    }

    /**
     * Checks a digest from a single event against all digests with the same index in an Event Log.
     *
     * @param eventLog The Reference Event log.
     * @param event    single event to match.
     * @return
     */
    private static boolean digestMatch(final Collection<TpmPcrEvent> eventLog,
                                       final TpmPcrEvent event) {
        boolean matchFound = false;
        for (TpmPcrEvent event2 : eventLog) {
            if ((event.getPcrIndex() == event2.getPcrIndex())
                    && (Arrays.equals(event.getEventDigest(), event2.getEventDigest()))) {
                matchFound = true;
            }
        }
        return matchFound;
    }
    /**
     * Diagnostic method for detecting flag settings.
     */
    public static void dumpFlags() {
        System.out.print("Event Flag is " + commander.getEventIdsFlag() + "\n");
        System.out.print("Hex Flag is " + commander.getEventHexFlag() + "\n");
        System.out.print("Context Flag is " + commander.getContentFlag() + "\n");
        System.out.print("PCR Flag is " + commander.getPCRFlag() + "\n");
        System.out.print("Output File Flag is " + commander.getFileFlag() + "\n");
        System.out.print("Output Flag is " + commander.getOutputFlag() + "\n");
    }
}
