package hirs.tpm.eventlog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import hirs.tpm.eventlog.events.EvConstants;
import hirs.tpm.eventlog.events.EvCompactHash;
import hirs.tpm.eventlog.events.EvEfiBootServicesApp;
import hirs.tpm.eventlog.events.EvEfiGptPartition;
import hirs.tpm.eventlog.events.EvEfiHandoffTable;
import hirs.tpm.eventlog.events.EvEfiSpecIdEvent;
import hirs.tpm.eventlog.events.EvEventTag;
import hirs.tpm.eventlog.events.EvIPL;
import hirs.tpm.eventlog.events.EvNoAction;
import hirs.tpm.eventlog.events.EvPostCode;
import hirs.tpm.eventlog.events.EvSCrtmContents;
import hirs.tpm.eventlog.events.EvSCrtmVersion;
import hirs.tpm.eventlog.uefi.UefiFirmware;
import hirs.tpm.eventlog.uefi.UefiVariable;
import hirs.utils.HexUtils;

/**
 * Class to process a TCG_PCR_EVENT.
 * TCG_PCR_EVENT is used when the Event log uses the SHA1 Format as described in the
 * TCG Platform Firmware Profile (PFP) specification.
 * typedef struct {
 *    TCG_PCRINDEX  PCRIndex;  //PCR Index value that either
 *    //matches the PCRIndex of a
 *    //previous extend operation or
 *    //indicates that this Event Log
 *    //entry is not associated with
 *    //an extend operation
 *    TCG_EVENTTYPE EventType; //See Log event types defined in toStrng()
 *    TCG_DIGEST    digest;    //The hash of the event data
 *    UINT32        EventSize; //Size of the event data
 *    UINT8         Event[EventSize];  //The event data
 * } TCG_PCR_EVENT;
 */
public class TpmPcrEvent {
    /** Log format. SHA1=1, Crytpo agile=2. */
    private int logFormat = -1;
    /** PCR index. */
    private int pcrIndex = -1;
    /**  Event Type (long). */
    private long eventType = 0;
    /**  Event digest. */
    private byte[] digest = null;
    /**  Even content data. */
    private byte[] eventContent;
    /** TCG Event Log spec version.  */
    private String version = "Unknown";
    /** TCG Event Log errata version. */
    private String errata = "Unknown";
    /**  Length (in bytes) of a pcr. */
    private int digestLength = 0;
    /**  Event Number. */
    private int eventNumber = 1;
    /**  Index. */
    private int index = -1;
    /**  Event Contents flag. */
    private boolean bEvContent = false;
    /**  Event hash for SHA1 event logs. */
    private byte[] eventDataSha1hash;
    /**  Event hash for Crypto Agile events. */
    private byte[] eventDataSha256hash;
    /** Signature extension mask.*/
    private static final long SIGN_MASK = 0x00000000FFFFFFFFL;
    /** Mask used to remove upper values from a long. */
    private static final long INT_MASK = 0x000000007FFFFFFFL;

    /**
     * Constructor.
     *
     * @param is ByteArrayInputStream holding the event
     * @throws IOException when event can't be parsed
     */
    public TpmPcrEvent(final ByteArrayInputStream is) throws IOException {

    }

    /**
     * Sets the digest from a  TCG_PCR_EVENT digest field.
     * This can be SHA1 for older event structures or any algorithm for newer structure.
     *
     * @param digestData cryptographic hash
     */
    protected void setEventDigest(final byte[] digestData) {
        digest = new byte[digestLength];
        System.arraycopy(digestData, 0, digest, 0, this.digestLength);
    }

    /**
     * Retrieves the digest from a TCG Event.
     * This can be SHA1 for older event structures or any algorithm for newer structure.
     *
     * @return the digest data for the event
     */
    public byte[] getEventDigest() {
        byte[] digestCopy = new byte[digestLength];
        System.arraycopy(digest, 0, digestCopy, 0, this.digestLength);
        return digestCopy;
    }

    /**
     * Sets the event PCR index value from a TCG Event.
     *
     * @param eventIndex TCG Event PCR Index as defined in the PFP
     */
    protected void setPcrIndex(final byte[] eventIndex) {
        pcrIndex = HexUtils.leReverseInt(eventIndex);
    }

    /**
     * Gets the event index value from a TCG Event.
     *
     * @return eventIndex TCG Event Index as defined in the PFP
     */
    public int getPcrIndex() {
        return pcrIndex;
    }

   /** Sets the Log Format for this TCG Event.
    *  1 = SHA1 Format, 2 = Crypto Agile format.
    * @param format indicates log format.
    */
   protected void setLogFormat(final int format) {
       logFormat = format;
   }

   /**
    * Gets the Log Format for this TCG Event.
    *  1 = SHA1 Format, 2 = Crypto Agile format.
    * @return number representing the format.
    */
   public int getLogFormat() {
       return logFormat;
   }

    /**
     * Sets the EventType.
     *
     * @param type byte array holding the PFP defined log event type
     */
    protected void setEventType(final byte[] type) {
        eventType = new BigInteger(HexUtils.leReverseByte(type)).longValue();
    }

    /**
     * Returns the EventType for the Event.
     *
     * @return event type
     */
    public long getEventType() {
        return eventType;
    }

    /**
     * Returns the version of the TCG Log Event specification pertaining to the log.
     * only updated if the event is a TCG_EfiSpecIdEvent.
     *
     * @return specification version
     */
    public String getSpecVersion() {
        return version;
    }

    /**
     * Returns the Errata version of the TCG Log Event specification pertaining to the log.
     * only updated if the event is a TCG_EfiSpecIdEvent).
     *
     * @return Errata version
     */
    public String getSpecErrataVersion() {
        return errata;
    }

    /**
     * Sets the event content after processing.
     *
     * @param eventData The PFP defined event content
     */
    protected void setEventContent(final byte[] eventData) {
        eventContent = new byte[eventData.length];
        System.arraycopy(eventContent, 0, eventData, 0, eventData.length);
    }

    /**
     * Gets the length of number of bytes in a PCR for the event.
     * event log format.
     *
     * @return byte array holding the events content field
     */
    public byte[] getEventContent() {
        return java.util.Arrays.copyOf(eventContent, eventContent.length);
    }

    /**
     * Sets the Digest Length.
     * Also the number of bytes expected within each PCR.
     *
     * @param length number of bytes in a PCR for the event.
     */
    public void setDigestLength(final int length) {
        digestLength = length;
    }

    /**
     * Gets the length of number of bytes in a PCR for the event.
     *
     * @return Byte Array containing the PFP defined event content
     */
    public int getDigestLength() {
        return digestLength;
    }

    /**
     * Parses the event content and creates a human readable description of each event.
     * @param event the byte array holding the event data.
     * @param eventContent the byte array holding the event content.
     * @return String description of the event.
     * @throws CertificateException if the event contains an event that cannot be processed.
     * @throws NoSuchAlgorithmException if an event contains an unsupported algorithm.
     * @throws IOException if the event cannot be parsed.
     */
 public String processEvent(final byte[] event, final byte[] eventContent)
                            throws CertificateException, NoSuchAlgorithmException, IOException {
        String description = "";
        int eventID = (int) eventType;
        description += "Event# " + eventNumber++ + ": ";
        description += "Index PCR[" + this.index + "]\n";
        description += "Event Type: 0x" + this.eventType + " " + eventString(eventID);
        description += "\n";
        if (logFormat == 1) {   // Digest
            description += "digest (SHA-1): " + HexUtils.byteArrayToHexString(this.digest) + "\n";
        } else {
            description += "digest (SHA256): " + HexUtils.byteArrayToHexString(this.digest) + "\n";
        }
        // Calculate both the SHA1 and SHA256 on the event since this will equal the digest
        // field of about half the log messages.
        MessageDigest md1 = MessageDigest.getInstance("SHA-1");
        md1.update(event);
        eventDataSha1hash = md1.digest();
        MessageDigest md2 = MessageDigest.getInstance("SHA-256");
        md2.update(event);
        eventDataSha256hash = md2.digest();

        switch (eventID) {
            case EvConstants.EV_PREBOOT_CERT:
                description += " EV_PREBOOT_CERT" + "\n";
            break;
            case EvConstants.EV_POST_CODE:
                EvPostCode postCode = new EvPostCode(eventContent);
                   description += "Event Content:\n" + postCode.toString() + "\n";
            break;
            case EvConstants.EV_UNUSED:
                break;
            case EvConstants.EV_NO_ACTION:
                EvNoAction noAction = new EvNoAction(eventContent);
                description += "Event Content:\n" + noAction.toString();
                if (noAction.isSpecIDEvent()) {
                    EvEfiSpecIdEvent specID = noAction.getEvEfiSpecIdEvent();
                    version = specID.getVersionMajor() + "." + specID.getVersionMinor();
                    errata = specID.getErrata();
                }
                break;
            case EvConstants.EV_SEPARATOR:
                if (EvPostCode.isAscii(eventContent)) {
                    String seperatorEventData = new String(eventContent, StandardCharsets.UTF_8);
                    if (!this.isEmpty(eventContent)) {
                        description += "Seperator event content = " + seperatorEventData + "\n";
                    }
                   }
                description += eventHashCheck();
                break;
            case EvConstants.EV_ACTION:
                description += "Event Content:\n"
                                      + new String(eventContent, StandardCharsets.UTF_8) + "\n";
                description += eventHashCheck();
                break;
            case EvConstants.EV_EVENT_TAG:
                EvEventTag eventTag = new EvEventTag(eventContent);
                description += eventTag.toString() + "\n";
                description += eventHashCheck();
                break;
            case EvConstants.EV_S_CRTM_CONTENTS:
                EvSCrtmContents sCrtmContents = new EvSCrtmContents(eventContent);
                description += "Event Content:\n   " + sCrtmContents.toString() + "\n";
                break;
            case EvConstants.EV_S_CRTM_VERSION:
                EvSCrtmVersion sCrtmVersion = new EvSCrtmVersion(eventContent);
                description += "Event Content:\n" + sCrtmVersion.toString() + "\n";
                description += eventHashCheck();
                break;
            case EvConstants.EV_CPU_MICROCODE:
                break;
            case EvConstants.EV_PLATFORM_CONFIG_FLAGS:
                description += eventHashCheck();
                break;
            case EvConstants.EV_TABLE_OF_DEVICES:
                break;
            case EvConstants.EV_COMPACT_HASH:
                EvCompactHash compactHash =  new EvCompactHash(eventContent);
                description += "Event Content:\n" + compactHash.toString() + "\n";
                description += eventHashCheck();
                break;
            case EvConstants.EV_IPL:
                EvIPL ipl = new EvIPL(eventContent);
                description += "Event Content:\n" + ipl.toString() + "\n";
                break;
            case EvConstants.EV_IPL_PARTITION_DATA:
                break;
            case EvConstants.EV_NONHOST_CODE:
                break;
            case EvConstants.EV_NONHOST_CONFIG:
                break;
            case EvConstants.EV_NONHOST_INFO:
                break;
            case EvConstants.EV_EV_OMIT_BOOT_DEVICES_EVENTS:
                break;
            case EvConstants.EV_EFI_EVENT_BASE:
                break;
            case EvConstants.EV_EFI_VARIABLE_DRIVER_CONFIG:
                UefiVariable efiVar = new UefiVariable(eventContent);
                description += "Event Content:\n" + efiVar.toString();
                description += eventHashCheck();
                break;
            case EvConstants.EV_EFI_VARIABLE_BOOT:
                description += "Event Content:\n" + new UefiVariable(eventContent).toString();
                description += eventHashCheck();
                break;
            case EvConstants.EV_EFI_BOOT_SERVICES_APPLICATION:
                EvEfiBootServicesApp bootServices = new EvEfiBootServicesApp(eventContent);
                description += "Event Content:\n" + bootServices.toString();
                break;
            case EvConstants.EV_EFI_BOOT_SERVICES_DRIVER: // same as EV_EFI_BOOT_SERVICES_APP
                EvEfiBootServicesApp bootDriver = new EvEfiBootServicesApp(eventContent);
                description += "Event Content:\n" + bootDriver.toString();
                break;
            case EvConstants.EV_EFI_RUNTIME_SERVICES_DRIVER:
                break;
            case EvConstants.EV_EFI_GPT_EVENT:
                description += "Event Content:\n" + new EvEfiGptPartition(eventContent).toString();
                description += eventHashCheck();
                break;
            case EvConstants.EV_EFI_ACTION:
                description += new String(eventContent, StandardCharsets.UTF_8) + "\n";
                description += eventHashCheck();
                break;
            case EvConstants.EV_EFI_PLATFORM_FIRMWARE_BLOB:
                description += "Event Content:\n"
                                    + new UefiFirmware(eventContent).toString() + "\n";
                break;
            case EvConstants.EV_EFI_HANDOFF_TABLES:
                EvEfiHandoffTable efiTable = new EvEfiHandoffTable(eventContent);
                description += "Event Content:\n" + efiTable.toString();
                break;
            case EvConstants.EV_EFI_HCRTM_EVENT:
                break;
            case EvConstants.EV_EFI_VARIABLE_AUTHORITY:
                description += "Event Content:\n" + new UefiVariable(eventContent).toString();
                break;
            default: description += " Unknown Event found" + "\n";
        }

        if (bEvContent) {
            description += "Event content (Hex) (" + event.length + "): "
                                            + HexUtils.byteArrayToHexString(eventContent) + "\n\n";
        }
        return description;
    }

    /**
     * Converts the Event ID into a String As defined in the TCG PC Client FW Profile.
     * Event IDs have values larger than an integer,so a Long is used hold the value.
     * @param event the event id.
     * @return TCG defined String that represents the event id
     */
     private static String eventString(final long event) {
         String evString = "";
         long tmpEvent = event;
         Long longEvent = Long.valueOf(tmpEvent & SIGN_MASK); // Remove signed extension
         Long intEvent = Long.valueOf(tmpEvent & INT_MASK); // truncate to an int value
         // Check to see if value is larger than an int, if it is then truncate the value
         if (longEvent.longValue() > (long) Integer.MAX_VALUE) {
             switch (intEvent.intValue()) {
             case  EvConstants.EV_EFI_EVENT_BASE: evString = "EV_EFI_EVENT_BASE"; break;
             case  EvConstants.EV_EFI_VARIABLE_DRIVER_CONFIG:
                                       evString = "EV_EFI_VARIABLE_DRIVER_CONFIG"; break;
             case  EvConstants.EV_EFI_VARIABLE_BOOT:
                                                evString = "EV_EFI_VARIABLE_BOOT"; break;
             case  EvConstants.EV_EFI_BOOT_SERVICES_APPLICATION:
                                     evString = "EV_EFI_BOOT_SERVICES_APPLICATION"; break;
             case  EvConstants.EV_EFI_BOOT_SERVICES_DRIVER:
                                           evString = "EV_EFI_BOOT_SERVICES_DRIVER"; break;
             case  EvConstants.EV_EFI_RUNTIME_SERVICES_DRIVER:
                                        evString = "EV_EFI_RUNTIME_SERVICES_DRIVER"; break;
             case  EvConstants.EV_EFI_GPT_EVENT: evString = "EV_EFI_GPT_EVENT"; break;
             case  EvConstants.EV_EFI_ACTION: evString = "EV_EFI_ACTION"; break;
             case  EvConstants.EV_EFI_PLATFORM_FIRMWARE_BLOB:
                                           evString = "EV_EFI_PLATFORM_FIRMWARE_BLOB"; break;
             case  EvConstants.EV_EFI_HANDOFF_TABLES: evString = "EV_EFI_HANDOFF_TABLES"; break;
             case  EvConstants.EV_EFI_HCRTM_EVENT: evString = "EV_EFI_HCRTM_EVENT"; break;
             case  EvConstants.EV_EFI_VARIABLE_AUTHORITY:
                                               evString = "EV_EFI_VARIABLE_AUTHORITY"; break;
             default: evString = "Unknown Event ID " + event + " encountered";
             }
         } else {
           switch (intEvent.intValue()) {
               case  EvConstants.EV_PREBOOT_CERT: evString = "EV_PREBOOT_CERT"; break;
               case  EvConstants.EV_POST_CODE: evString = "EV_POST_CODE"; break;
               case  EvConstants.EV_UNUSED: evString = "EV_Unused"; break;
               case  EvConstants.EV_NO_ACTION: evString = "EV_NO_ACTION"; break;
               case  EvConstants.EV_SEPARATOR: evString = "EV_SEPARATOR"; break;
               case  EvConstants.EV_ACTION: evString = "EV_ACTION"; break;
               case  EvConstants.EV_EVENT_TAG: evString = "EV_EVENT_TAG"; break;
               case  EvConstants.EV_S_CRTM_CONTENTS: evString = "EV_S_CRTM_CONTENTS"; break;
               case  EvConstants.EV_S_CRTM_VERSION: evString = "EV_S_CRTM_VERSION"; break;
               case  EvConstants.EV_CPU_MICROCODE: evString = "EV_CPU_MICROCODE"; break;
               case  EvConstants.EV_PLATFORM_CONFIG_FLAGS: evString = "EV_PLATFORM_CONFIG_FLAGS ";
                                                                                  break;
               case  EvConstants.EV_TABLE_OF_DEVICES: evString = "EV_TABLE_OF_DEVICES"; break;
               case  EvConstants.EV_COMPACT_HASH: evString = "EV_COMPACT_HASH"; break;
               case  EvConstants.EV_IPL: evString = "EV_IPL"; break;
               case  EvConstants.EV_IPL_PARTITION_DATA: evString = "EV_IPL_PARTITION_DATA"; break;
               case  EvConstants.EV_NONHOST_CODE: evString = "EV_NONHOST_CODE"; break;
               case  EvConstants.EV_NONHOST_CONFIG: evString = "EV_NONHOST_CONFIG"; break;
               case  EvConstants.EV_NONHOST_INFO: evString = "EV_NONHOST_INFO"; break;
               case  EvConstants.EV_EV_OMIT_BOOT_DEVICES_EVENTS:
                                                 evString = "EV_EV_OMIT_BOOT_DEVICES_EVENTS"; break;
               default: evString = "Unknown Event ID " + event + " encountered";
         }
         }
         return evString;
     }

     /**
      * Human readable output of a check of input against the current event hash.
      * @return human readable string.
      */
     private String eventHashCheck() {
         String result = "";
         if (logFormat == 1) {
             if (Arrays.equals(this.digest, eventDataSha1hash)) { result
                                       += "Event digest matched hash of the event data " + "\n";
             } else {
                 result += "Event digest DID NOT match the hash of the event data :"
                                       + HexUtils.byteArrayToHexString(getEventDigest()) + "\n";
                 }
            } else {
             if (Arrays.equals(this.digest, eventDataSha256hash)) {
                 result += "Event digest matched hash of the event data " + "\n";
             } else {
                 result += "Event digest DID NOT match the hash of the event data :"
                         + HexUtils.byteArrayToHexString(getEventDigest()) + "\n";
                }
           }
         return result;
     }

     /**
      * Checks a byte array for all zeros.
      * @param array holds data to check.
      * @return true of all zeros are found.
      */
     public boolean isEmpty(final byte[] array) {
         for (int i = 0; i < array.length; i++) {
             if (array[i] != 0) {
                 return false;
             }
         }
         return true;
     }
}
