package hirs.utils.tpm.eventlog;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.events.EvCompactHash;
import hirs.utils.tpm.eventlog.events.EvConstants;
import hirs.utils.tpm.eventlog.events.EvEfiBootServicesApp;
import hirs.utils.tpm.eventlog.events.EvEfiGptPartition;
import hirs.utils.tpm.eventlog.events.EvEfiHandoffTable;
import hirs.utils.tpm.eventlog.events.EvEfiSpdmDeviceSecurityEvent;
import hirs.utils.tpm.eventlog.events.EvEventTag;
import hirs.utils.tpm.eventlog.events.EvIPL;
import hirs.utils.tpm.eventlog.events.EvNoAction;
import hirs.utils.tpm.eventlog.events.EvPostCode;
import hirs.utils.tpm.eventlog.events.EvSCrtmContents;
import hirs.utils.tpm.eventlog.events.EvSCrtmVersion;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import hirs.utils.tpm.eventlog.uefi.UefiFirmware;
import hirs.utils.tpm.eventlog.uefi.UefiVariable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;

import static hirs.utils.tpm.eventlog.uefi.UefiConstants.FILESTATUS_FROM_FILESYSTEM;

/**
 * Class to process a TCG_PCR_EVENT.
 * TCG_PCR_EVENT is used when the Event log uses the SHA1 Format as described in the
 * TCG Platform Firmware Profile (PFP) specification.
 * typedef struct {
 * .     TCG_PCRINDEX  PCRIndex;  //PCR Index value that either
 * .                              //matches the PCRIndex of a
 * .                              //previous extend operation or
 * .                              //indicates that this Event Log
 * .                              //entry is not associated with
 * .                              //an extend operation
 * .     TCG_EVENTTYPE EventType; //See Log event types defined in toStrng()
 * .     TCG_DIGEST    digest;    //The hash of the event data
 * .     UINT32        EventSize; //Size of the event data
 * .     UINT8         Event[EventSize];  //The event data
 * } TCG_PCR_EVENT;
 */
@Log4j2
public class TpmPcrEvent {

    /**
     * Log format. SHA1=1, Crytpo agile=2.
     * this can be refactored out
     */
    @Getter
    @Setter(value = AccessLevel.PROTECTED)
    private int logFormat = -1;

    /**
     * PCR index.
     */
    @Getter
    private int pcrIndex = -1;

    /**
     * Event Type (long).
     */
    @Getter
    private long eventType = 0;

    /**
     * Event digest. If more than one digest in the Event, use the strongest one.
     */
    private byte[] strongestDigest = null;

    /**
     * list of digests from the event log.
     */
    protected final ArrayList<EventDigest> hashListFromEvent = new ArrayList<>();

    /**
     * list of digests by calculating the hash of the event.
     */
    private final ArrayList<EventDigest> hashListCalculated = new ArrayList<>();

    /**
     * Event hash for SHA1 event logs.
     */
    private byte[] eventDataSha1hash;

    /**
     * Event hash for Crypto Agile events.
     */
    private byte[] eventDataSha256hash;

    /**
     * Event data (no content).
     */
    private byte[] event;

    /**
     * Event content data.
     */
    private byte[] eventContent;

    /**
     * TCG Event Log spec version.
     */
    @Getter
    private String specVersion = "Unknown";

    /**
     * TCG Event Log errata version.
     */
    @Getter
    private String specErrataVersion = "Unknown";

    /**
     * True of the event is a EV_NO_ACTION SpecID event.
     */
    @Getter
    private boolean isNoActionSpecIdEvent = false;

    /**
     * True of the event is a EV_NO_ACTION StartupLocality event.
     */
    @Getter
    private boolean isStartupLocalityEvent = false;

    /**
     * Description for toString support.
     */
    protected String description = "";

    @Getter
    @Setter
    private int eventNumber;

    @Getter
    @Setter
    private boolean error = false;

    /**
     * Track status of vendor-table.json file.
     * See TCGEventLog for more detail.
     */
    @Getter
    private String guidTableFileStatus = FILESTATUS_FROM_FILESYSTEM;

    /**
     * Track status of pci.ids file.
     * See TCGEventLog for more detail.
     */
    @Getter
    private String pciidsFileStatus = FILESTATUS_FROM_FILESYSTEM;

    /**
     * Constructor.
     *
     * @param baIs ByteArrayInputStream holding the event
     * @throws java.io.IOException when event can't be parsed
     */
    public TpmPcrEvent(final ByteArrayInputStream baIs) throws IOException {

    }

    /**
     * Converts the Event ID into a String As defined in the TCG PC Client FW Profile.
     * Event IDs have values larger than an integer,so a Long is used hold the value.
     *
     * @param event the event id.
     * @return TCG defined String that represents the event id
     */
    private static String eventString(final long event) {

        if (event == EvConstants.EV_PREBOOT_CERT) {
            return "EV_PREBOOT_CERT";
        } else if (event == EvConstants.EV_POST_CODE) {
            return "EV_POST_CODE";
        } else if (event == EvConstants.EV_UNUSED) {
            return "EV_Unused";
        } else if (event == EvConstants.EV_NO_ACTION) {
            return "EV_NO_ACTION";
        } else if (event == EvConstants.EV_SEPARATOR) {
            return "EV_SEPARATOR";
        } else if (event == EvConstants.EV_ACTION) {
            return "EV_ACTION";
        } else if (event == EvConstants.EV_EVENT_TAG) {
            return "EV_EVENT_TAG";
        } else if (event == EvConstants.EV_S_CRTM_CONTENTS) {
            return "EV_S_CRTM_CONTENTS";
        } else if (event == EvConstants.EV_S_CRTM_VERSION) {
            return "EV_S_CRTM_VERSION";
        } else if (event == EvConstants.EV_CPU_MICROCODE) {
            return "EV_CPU_MICROCODE";
        } else if (event == EvConstants.EV_PLATFORM_CONFIG_FLAGS) {
            return "EV_PLATFORM_CONFIG_FLAGS ";
        } else if (event == EvConstants.EV_TABLE_OF_DEVICES) {
            return "EV_TABLE_OF_DEVICES";
        } else if (event == EvConstants.EV_COMPACT_HASH) {
            return "EV_COMPACT_HASH";
        } else if (event == EvConstants.EV_IPL) {
            return "EV_IPL";
        } else if (event == EvConstants.EV_IPL_PARTITION_DATA) {
            return "EV_IPL_PARTITION_DATA";
        } else if (event == EvConstants.EV_NONHOST_CODE) {
            return "EV_NONHOST_CODE";
        } else if (event == EvConstants.EV_NONHOST_CONFIG) {
            return "EV_NONHOST_CONFIG";
        } else if (event == EvConstants.EV_NONHOST_INFO) {
            return "EV_NONHOST_INFO";
        } else if (event == EvConstants.EV_EV_OMIT_BOOT_DEVICES_EVENTS) {
            return "EV_EV_OMIT_BOOT_DEVICES_EVENTS";
        } else if (event == EvConstants.EV_EFI_EVENT_BASE) {
            return "EV_EFI_EVENT_BASE";
        } else if (event == EvConstants.EV_EFI_VARIABLE_DRIVER_CONFIG) {
            return "EV_EFI_VARIABLE_DRIVER_CONFIG";
        } else if (event == EvConstants.EV_EFI_VARIABLE_BOOT) {
            return "EV_EFI_VARIABLE_BOOT";
        } else if (event == EvConstants.EV_EFI_BOOT_SERVICES_APPLICATION) {
            return "EV_EFI_BOOT_SERVICES_APPLICATION";
        } else if (event == EvConstants.EV_EFI_BOOT_SERVICES_DRIVER) {
            return "EV_EFI_BOOT_SERVICES_DRIVER";
        } else if (event == EvConstants.EV_EFI_RUNTIME_SERVICES_DRIVER) {
            return "EV_EFI_RUNTIME_SERVICES_DRIVER";
        } else if (event == EvConstants.EV_EFI_GPT_EVENT) {
            return "EV_EFI_GPT_EVENT";
        } else if (event == EvConstants.EV_EFI_ACTION) {
            return "EV_EFI_ACTION";
        } else if (event == EvConstants.EV_EFI_PLATFORM_FIRMWARE_BLOB) {
            return "EV_EFI_PLATFORM_FIRMWARE_BLOB";
        } else if (event == EvConstants.EV_EFI_HANDOFF_TABLES) {
            return "EV_EFI_HANDOFF_TABLES";
        } else if (event == EvConstants.EV_EFI_HCRTM_EVENT) {
            return "EV_EFI_HCRTM_EVENT";
        } else if (event == EvConstants.EV_EFI_VARIABLE_AUTHORITY) {
            return "EV_EFI_VARIABLE_AUTHORITY";
        } else if (event == EvConstants.EV_EFI_SPDM_FIRMWARE_BLOB) {
            return "EV_EFI_SPDM_FIRMWARE_BLOB";
        } else if (event == EvConstants.EV_EFI_SPDM_FIRMWARE_CONFIG) {
            return "EV_EFI_SPDM_FIRMWARE_CONFIG";
        } else if (event == EvConstants.EV_EFI_SPDM_DEVICE_POLICY) {
            return "EV_EFI_SPDM_DEVICE_POLICY";
        } else if (event == EvConstants.EV_EFI_SPDM_DEVICE_AUTHORITY) {
            return "EV_EFI_SPDM_DEVICE_AUTHORITY";
        } else {
            return "Unknown Event ID " + event + " encountered";
        }
    }

    /**
     * Sets the digest from a TCG_PCR_EVENT digest field.
     * In the case of multiple digests, set the strongest one.
     * This can be SHA1 for older event structures or any algorithm for newer structure.
     *
     * @param data   cryptographic hash
     */
    protected void setEventStrongestDigest(final byte[] data) {
        strongestDigest = new byte[data.length];
        System.arraycopy(data, 0, strongestDigest, 0, data.length);
    }

    /**
     * Retrieves the digest from a TCG Event.
     * This can be SHA1 for older event structures or any algorithm for newer structure.
     *
     * @return the digest data for the event
     */
    public byte[] getEventStrongestDigest() {
        byte[] digestCopy = new byte[strongestDigest.length];
        System.arraycopy(strongestDigest, 0, digestCopy, 0, strongestDigest.length);
        return digestCopy;
    }

    /**
     * Returns a hex representation of the event digest.
     *
     * @return hex string
     */
    public String getEventDigestStr() {
        return Hex.encodeHexString(strongestDigest);
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
     * Sets the EventType.
     *
     * @param type byte array holding the PFP defined log event type
     */
    protected void setEventType(final byte[] type) {
        eventType = new BigInteger(1, HexUtils.leReverseByte(type)).longValue();
    }

    /**
     * Returns a formatted string of the type for the event.
     *
     * @return a string formatted to be human-readable
     */
    public String getEventTypeStr() {
        return String.format("0x%s %s", Long.toHexString(eventType), eventString((int) eventType));
    }

    /**
     * Returns a formatted string of the type for the event minus the byte code.
     *
     * @return a string formatted to be human-readable
     */
    public String getEventTypeString() {
        return eventString((int) eventType);
    }

    /**
     * Sets the event data after processing.
     *
     * @param eventData The PFP defined event content
     */
    protected void setEventData(final byte[] eventData) {
        event = new byte[eventData.length];
        System.arraycopy(eventData, 0, event, 0, eventData.length);
    }

    /**
     * Gets the Event Data (no event content) for the event.
     * event log format.
     *
     * @return byte array holding the event structure.
     */
    public byte[] getEvent() {
        return Arrays.copyOf(event, event.length);
    }

    /**
     * Gets the event Content Data (not the entire event structure).
     *
     * @return byte array holding the events content field
     */
    public byte[] getEventContent() {
        return Arrays.copyOf(eventContent, eventContent.length);
    }

    /**
     * Sets the event content after processing.
     *
     * @param eventData The PFP defined event content
     */
    protected void setEventContent(final byte[] eventData) {
        eventContent = new byte[eventData.length];
        //EvPostCode evPostCode = new EvPostCode(eventContent);
        System.arraycopy(eventData, 0, eventContent, 0, eventData.length);
    }

    /**
     * A getter that parses the content based on the type and returns the proper string
     * value for the content.
     *
     * @return an appended string of human-readable data
     */
    public String getEventContentStr() {
        StringBuilder sb = new StringBuilder();

        switch ((int) this.eventType) {
            case EvConstants.EV_PREBOOT_CERT:
                sb.append(" EV_PREBOOT_CERT");
                break;
            case EvConstants.EV_POST_CODE:
                sb.append(new EvPostCode(eventContent));
                break;
            case EvConstants.EV_UNUSED, EvConstants.EV_IPL_PARTITION_DATA, EvConstants.EV_NONHOST_CODE,
                 EvConstants.EV_NONHOST_CONFIG, EvConstants.EV_NONHOST_INFO,
                 EvConstants.EV_EV_OMIT_BOOT_DEVICES_EVENTS, EvConstants.EV_EFI_EVENT_BASE,
                 EvConstants.EV_EFI_RUNTIME_SERVICES_DRIVER, EvConstants.EV_CPU_MICROCODE,
                 EvConstants.EV_PLATFORM_CONFIG_FLAGS, EvConstants.EV_TABLE_OF_DEVICES,
                 EvConstants.EV_EFI_HCRTM_EVENT:
                break;
            case EvConstants.EV_NO_ACTION:
                EvNoAction noAction = null;
                try {
                    noAction = new EvNoAction(eventContent);
                    sb.append(noAction);
                    if (noAction.isSpecIdEvent()) {
                        specVersion = noAction.getSpecVersion();
                        specErrataVersion = noAction.getSpecErrataVersion();
                    }
                } catch (UnsupportedEncodingException ueEx) {
                    log.error(ueEx);
                    sb.append(ueEx);
                }
                break;
            case EvConstants.EV_SEPARATOR:
                if (EvPostCode.isAscii(eventContent)
                        && !this.isBlank(eventContent)) {
                    sb.append(String.format("Separator event content = %s",
                            new String(eventContent, StandardCharsets.UTF_8)));
                }
                break;
            case EvConstants.EV_EVENT_TAG:
                sb.append(new EvEventTag(eventContent));
                break;
            case EvConstants.EV_S_CRTM_CONTENTS:
                sb.append(new EvSCrtmContents(eventContent));
                break;
            case EvConstants.EV_S_CRTM_VERSION:
                try {
                    sb.append(new EvSCrtmVersion(eventContent));
                } catch (UnsupportedEncodingException ueEx) {
                    log.error(ueEx);
                    sb.append(ueEx);
                }
                break;
            case EvConstants.EV_COMPACT_HASH:
                try {
                    sb.append(new EvCompactHash(eventContent));
                } catch (UnsupportedEncodingException ueEx) {
                    log.error(ueEx);
                    sb.append(ueEx);
                }
                break;
            case EvConstants.EV_IPL:
                sb.append(new EvIPL(eventContent));
                break;
            case EvConstants.EV_EFI_VARIABLE_DRIVER_CONFIG:
            case EvConstants.EV_EFI_VARIABLE_BOOT:
            case EvConstants.EV_EFI_VARIABLE_AUTHORITY:
            case EvConstants.EV_EFI_SPDM_DEVICE_POLICY:
            case EvConstants.EV_EFI_SPDM_DEVICE_AUTHORITY:
                try {
                    sb.append(new UefiVariable(eventContent));
                } catch (CertificateException | NoSuchAlgorithmException | IOException exception) {
                    log.error(exception);
                    sb.append(exception);
                }
                break;
            case EvConstants.EV_EFI_BOOT_SERVICES_APPLICATION:
            case EvConstants.EV_EFI_BOOT_SERVICES_DRIVER: // same as EV_EFI_BOOT_SERVICES_APP
                try {
                    sb.append(new EvEfiBootServicesApp(eventContent));
                } catch (UnsupportedEncodingException ueEx) {
                    log.error(ueEx);
                    sb.append(ueEx);
                }
                break;
            case EvConstants.EV_EFI_GPT_EVENT:
                try {
                    sb.append(new EvEfiGptPartition(eventContent));
                } catch (UnsupportedEncodingException ueEx) {
                    log.error(ueEx);
                    sb.append(ueEx);
                }
                break;
            case EvConstants.EV_EFI_ACTION:
            case EvConstants.EV_ACTION:
                sb.append(new String(eventContent, StandardCharsets.UTF_8));
                break;
            case EvConstants.EV_EFI_PLATFORM_FIRMWARE_BLOB:
                sb.append(new UefiFirmware(eventContent));
                break;
            case EvConstants.EV_EFI_HANDOFF_TABLES:
                sb.append(new EvEfiHandoffTable(eventContent));
                break;
            case EvConstants.EV_EFI_SPDM_FIRMWARE_BLOB:
            case EvConstants.EV_EFI_SPDM_FIRMWARE_CONFIG:
                sb.append(new EvEfiSpdmDeviceSecurityEvent(eventContent));
                break;
            default:
                sb.append("Unknown Event found\n");
        }

        return cleanTextContent(sb.toString());
    }

    /**
     * Parses the event content and creates a human-readable description of each event.
     *
     * @param eventData     the byte array holding the event data.
     * @param content       the byte array holding the event content.
     * @param eventPosition event position within the event log.
     * @return String description of the event.
     * @throws CertificateException     if the event contains an event that cannot be processed.
     * @throws NoSuchAlgorithmException if an event contains an unsupported algorithm.
     * @throws java.io.IOException      if the event cannot be parsed.
     */
    public String processEvent(final byte[] eventData, final byte[] content,
                               final int eventPosition)
            throws CertificateException, NoSuchAlgorithmException, IOException {
        int eventID = (int) eventType;
        this.eventNumber = eventPosition;
        description += "Event# " + eventPosition + ": ";
        description += "Index PCR[" + getPcrIndex() + "]\n";
        description += "Event Type: 0x" + Long.toHexString(eventType) + " " + eventString(eventID);

        if (eventID != UefiConstants.SIZE_4) {
            description += "\n";
        }
        // Calculate both the SHA1 and SHA256 on the event since this will equal the digest
        // field of about half the log messages.
        MessageDigest md1 = MessageDigest.getInstance("SHA-1");
        md1.update(eventData);
        eventDataSha1hash = md1.digest();
        MessageDigest md2 = MessageDigest.getInstance("SHA-256");
        md2.update(eventData);
        eventDataSha256hash = md2.digest();

        switch (eventID) {
            case EvConstants.EV_PREBOOT_CERT:
                description += " EV_PREBOOT_CERT" + "\n";
                break;
            case EvConstants.EV_POST_CODE:
                EvPostCode postCode = new EvPostCode(content);
                description += "Event Content:\n" + postCode;
                break;
            case EvConstants.EV_UNUSED, EvConstants.EV_EFI_RUNTIME_SERVICES_DRIVER,
                 EvConstants.EV_EFI_HCRTM_EVENT, EvConstants.EV_EFI_EVENT_BASE,
                 EvConstants.EV_EV_OMIT_BOOT_DEVICES_EVENTS, EvConstants.EV_NONHOST_INFO,
                 EvConstants.EV_NONHOST_CONFIG, EvConstants.EV_NONHOST_CODE,
                 EvConstants.EV_IPL_PARTITION_DATA, EvConstants.EV_PLATFORM_CONFIG_FLAGS,
                 EvConstants.EV_CPU_MICROCODE, EvConstants.EV_TABLE_OF_DEVICES:
                break;
            case EvConstants.EV_NO_ACTION:
                EvNoAction noAction = new EvNoAction(content);
                description += "Event Content:\n" + noAction;
                if (noAction.isSpecIdEvent()) {
                    specVersion = noAction.getSpecVersion();
                    specErrataVersion = noAction.getSpecErrataVersion();
                    isNoActionSpecIdEvent = true;
                } else if (noAction.isStartupLocality()) {
                    isStartupLocalityEvent = true;
                }
                pciidsFileStatus = noAction.getPciidsFileStatus();
                break;
            case EvConstants.EV_SEPARATOR:
                if (EvPostCode.isAscii(content)) {
                    String separatorEventData = new String(content, StandardCharsets.UTF_8);
                    if (!this.isBlank(content)) {
                        description += "Separator event content = " + separatorEventData;
                    }
                }
                break;
            case EvConstants.EV_ACTION:
                description += "Event Content:\n"
                        + new String(content, StandardCharsets.UTF_8);
                break;
            case EvConstants.EV_EVENT_TAG:
                EvEventTag eventTag = new EvEventTag(content);
                description += eventTag.toString();
                break;
            case EvConstants.EV_S_CRTM_CONTENTS:
                EvSCrtmContents sCrtmContents = new EvSCrtmContents(content);
                description += "Event Content:\n   " + sCrtmContents;
                break;
            case EvConstants.EV_S_CRTM_VERSION:
                EvSCrtmVersion sCrtmVersion = new EvSCrtmVersion(content);
                description += "Event Content:\n" + sCrtmVersion;
                break;
            case EvConstants.EV_COMPACT_HASH:
                EvCompactHash compactHash = new EvCompactHash(content);
                description += "Event Content:\n" + compactHash;
                break;
            case EvConstants.EV_IPL:
                EvIPL ipl = new EvIPL(content);
                description += "Event Content:\n" + ipl;
                break;
            case EvConstants.EV_EFI_VARIABLE_DRIVER_CONFIG:
            case EvConstants.EV_EFI_VARIABLE_BOOT:
            case EvConstants.EV_EFI_VARIABLE_AUTHORITY:
            case EvConstants.EV_EFI_SPDM_DEVICE_POLICY:
            case EvConstants.EV_EFI_SPDM_DEVICE_AUTHORITY:
                UefiVariable efiVar = new UefiVariable(content);
                description += "Event Content:\n" + efiVar;
                guidTableFileStatus = efiVar.getGuidTableFileStatus();
                break;
            case EvConstants.EV_EFI_BOOT_SERVICES_APPLICATION:
            case EvConstants.EV_EFI_BOOT_SERVICES_DRIVER:
                EvEfiBootServicesApp bootServices = new EvEfiBootServicesApp(content);
                description += "Event Content:\n" + bootServices;
                break;
            case EvConstants.EV_EFI_GPT_EVENT:
                description += "Event Content:\n" + new EvEfiGptPartition(content);
                break;
            case EvConstants.EV_EFI_ACTION:
                description += new String(content, StandardCharsets.UTF_8);
                break;
            case EvConstants.EV_EFI_PLATFORM_FIRMWARE_BLOB:
                description += "Event Content:\n"
                        + new UefiFirmware(content);
                break;
            case EvConstants.EV_EFI_HANDOFF_TABLES:
                EvEfiHandoffTable efiTable = new EvEfiHandoffTable(content);
                description += "Event Content:\n" + efiTable;
                break;
            case EvConstants.EV_EFI_SPDM_FIRMWARE_BLOB:
            case EvConstants.EV_EFI_SPDM_FIRMWARE_CONFIG:
                EvEfiSpdmDeviceSecurityEvent efiSpdmDse = new EvEfiSpdmDeviceSecurityEvent(content);
                description += "Event Content:\n" + efiSpdmDse;
                pciidsFileStatus = efiSpdmDse.getPciidsFileStatus();
                break;
            default:
                description += " Unknown Event found" + "\n";
        }
        return description;
    }

    /**
     * Human-readable output of a check of input against the current event hash.
     *
     * @return human-readable string.
     */
    private String eventHashCheck() {
        String result = "";
        if (logFormat == 1) {
            if (Arrays.equals(strongestDigest, eventDataSha1hash)) {
                result
                        += "Event digest matched hash of the event data " + "\n";
            } else {
                result += "Event digest DID NOT match the hash of the event data :"
                        + Hex.encodeHexString(getEventStrongestDigest()) + "\n";
            }
        } else {
            if (Arrays.equals(strongestDigest, eventDataSha256hash)) {
                result += "Event digest matched hash of the event data " + "\n";
            } else {
                result += "Event digest DID NOT match the hash of the event data :"
                        + Hex.encodeHexString(getEventStrongestDigest()) + "\n";
            }
        }
        return result;
    }

    /**
     * This method takes in an event and compares the hashes to verify that they match.
     *
     * @param tpmPcrEvent an event to match.
     * @return true if the event # matches and the hash is correct.
     */
    public boolean eventCompare(final TpmPcrEvent tpmPcrEvent) {
        if (tpmPcrEvent.getPcrIndex() != getPcrIndex()) {
            return false;
        }

        return Arrays.equals(strongestDigest, tpmPcrEvent.getEventStrongestDigest());
    }

    /**
     * Checks a byte array for all zeros.
     *
     * @param array holds data to check.
     * @return true of all zeros are found.
     */
    public boolean isBlank(final byte[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Human-readable string representing the contents of the Event Log.
     *
     * @return Description of the log.
     */
    public String toString() {
        return description;
    }

    /**
     * Human-readable string representing the contents of the Event Log.
     *
     * @param bEvent    event Flag.
     * @param bContent  content flag.
     * @param bHexEvent hex event flag.
     * @return Description of the log.
     */
    public String toString(final boolean bEvent, final boolean bContent, final boolean bHexEvent) {
        StringBuilder sb = new StringBuilder();
        if (bEvent) {
            sb.append(description);
        }
        if (bHexEvent) {
            if (bEvent || bContent) {
                sb.append("\n");
            }
            byte[] eventData = getEvent();
            sb.append("Event (Hex no Content) (" + eventData.length + " bytes): "
                    + Hex.encodeHexString(eventData));
        }
        if (bContent) {
            byte[] evContent = getEventContent();
            sb.append("Event content (Hex) (" + evContent.length + " bytes): "
                    + Hex.encodeHexString(evContent));
        }
        return sb + "\n";
    }

    /**
     * Remove bad visual value text.
     *
     * @param text content to operate over.
     * @return cleared string
     */
    public String cleanTextContent(final String text) {
        String result;
        // strips off all non-ASCII characters
        result = text.replaceAll("[^\\x00-\\x7F]", "");

        // erases all the ASCII control characters
        result = result.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        // removes non-printable characters from Unicode
        result = result.replaceAll("\\p{C}", "");

        return result.trim();
    }
}
