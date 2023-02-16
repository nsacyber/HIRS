package hirs.attestationca.portal.utils.tpm.eventlog.events;

import hirs.attestationca.portal.utils.HexUtils;
import hirs.attestationca.portal.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

/**
 * Class for processing the EV_EVENT_TAG.
 * The structure for the Event Data is defined as:
 * structure tdTCG_PCClientTaggedEvent{
 * UINT32 taggedEventID;
 * UINT32 taggedEventDataSize;
 * BYTE taggedEventData[taggedEventDataSize];
 * } TCG_PCClientTaggedEvent;
 * ToDo: Find lookup of taggedEventID and figure out how to process.
 */
public class EvEventTag {
    /**
     * Event Tag Information.
     */
    private String eventTagInfo = "";
    /**
     * Event Tag ID.
     */
    @Getter
    private int tagEventID = 0;
    /**
     * Event ID.
     */
    private int eventID = 0;
    /**
     * Data size.
     */
    @Getter
    private int dataSize = 0;

    /**
     * Processes event tag.
     *
     * @param eventTag byte array holding the eventTag data.
     */
    public EvEventTag(final byte[] eventTag) {
        if (eventTag.length < UefiConstants.SIZE_8) {
            eventTagInfo = "Invalid EV Event Tag data";
        } else {
            byte[] tagEventIdBytes = new byte[UefiConstants.SIZE_4];
            System.arraycopy(eventTag, 0, tagEventIdBytes, 0, UefiConstants.SIZE_4);
            eventID = HexUtils.leReverseInt(tagEventIdBytes);
            byte[] tagEventDataSize = new byte[UefiConstants.SIZE_4];
            System.arraycopy(eventTag, UefiConstants.OFFSET_4, tagEventDataSize, 0,
                    UefiConstants.SIZE_4);
            dataSize = HexUtils.leReverseInt(tagEventDataSize);
        }
    }

    /**
     * Returns a human readable string of the Event Tag.
     *
     * @return human readable string.
     */
    public String toString() {
        if (eventTagInfo.isEmpty()) {
            eventTagInfo = "  Tagged Event ID = " + eventID;
            eventTagInfo += " Data Size = " + dataSize;
        }
        return eventTagInfo;
    }
}
