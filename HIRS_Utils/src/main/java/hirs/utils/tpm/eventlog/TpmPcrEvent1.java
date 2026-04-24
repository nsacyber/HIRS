package hirs.utils.tpm.eventlog;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.events.EvConstants;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Class to process a TCG_PCR_EVENT.
 * TCG_PCR_EVENT is used when the Event log uses the SHA1 Format as described in the
 * TCG Platform Firmware Profile specification.
 * typedef struct {
 * UINT32                   pcrIndex;        //PCR Index value that either
 * .                                        //matches the PCR Index of a
 * .                                        //previous extend operation or
 * .                                        //indicates that this Event Log
 * .                                        //entry is not associated with
 * .                                        //an extend operation
 * UINT32                   eventType;       //See Log event types
 * BYTE                     digest[20];      //digest
 * UINT32                   eventSize;       //Size of the event data
 * UINT8                    event[1];        //The event content
 * } TCG_PCR_EVENT;                          //The event data structure to be added
 *
 * In this code:
 * .   Event header = pcrIndex + eventType + digest + eventSize
 * .   Event content = event[1]
 */
@Log4j2
public class TpmPcrEvent1 extends TpmPcrEvent {

    /**
     * Constructor.
     *
     * @param is                        ByteArrayInputStream holding the TCG Log event
     * @param logFileBytesRemaining     number of bytes remaining in log file (for error checking)
     * @param eventNumber               event position within the event log
     * @throws java.io.IOException      if an error occurs in parsing the event
     */
    public TpmPcrEvent1(final ByteArrayInputStream is, final long logFileBytesRemaining, final int eventNumber)
            throws IOException {
        super(is);

        setLogFormat(1);

        // Event data.
        byte[] eventHeader;
        byte[] rawIndex = new byte[UefiConstants.SIZE_4];
        byte[] rawType = new byte[UefiConstants.SIZE_4];
        byte[] rawEventSize = new byte[UefiConstants.SIZE_4];
        byte[] eventDigest = new byte[EvConstants.SHA1_LENGTH];
        byte[] eventContent;

        int eventSize = 0;
        if (is.available() > UefiConstants.SIZE_32) {

            // read PCR index
            is.read(rawIndex);
            if (!setPcrIndex(rawIndex)) {
                throw new IOException("PCR Index out of range; possibly corrupt byte file.");
            }
            String pcrIndexStr = "Index PCR[" + getPcrIndex() + "]";

            // read event type
            is.read(rawType);
            setEventType(rawType);
            String eventTypeStr = "Event Type: 0x" + Long.toHexString(getEventType()) + " "
                    + eventString((int) getEventType());

            // read event digest
            is.read(eventDigest);
            hashListFromEvent.add(new EventDigest("TPM_ALG_SHA1", eventDigest));
            setEventStrongestDigest(eventDigest);

            // track event header length
            int eventHeaderLength = rawIndex.length + rawType.length + eventDigest.length + rawEventSize.length;

            // read event size (size of event content)
            is.read(rawEventSize);
            eventSize = HexUtils.leReverseInt(rawEventSize);
            if ((eventSize < 0) || (eventSize > (logFileBytesRemaining - eventHeaderLength))) {
                throw new IOException("Event size is not valid; possibly corrupt byte file.");
            }

            // copy entire event header into a byte array for processing
            int offset = 0;
            eventHeader = new byte[eventHeaderLength];
            System.arraycopy(rawIndex, 0, eventHeader, offset, rawIndex.length);
            offset += rawIndex.length;
            System.arraycopy(rawType, 0, eventHeader, offset, rawType.length);
            offset += rawType.length;
            System.arraycopy(eventDigest, 0, eventHeader, offset, eventDigest.length);
            offset += eventDigest.length;
            System.arraycopy(rawEventSize, 0, eventHeader, offset, rawEventSize.length);
            //offset += rawEventSize.length;
            setEventHeader(eventHeader);
            //System.arraycopy(eventContent, 0, event, offset, eventContent.length);

            // if event content cannot be processed, this is not fatal; log error and continue with attestation
            try {
                // read event content
                eventContent = new byte[eventSize];
                is.read(eventContent);
                setEventContent(eventContent);

                this.processEvent(eventContent, eventNumber);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                String error = "Error parsing event #" + eventNumber + ", " + eventTypeStr + ", " + pcrIndexStr;
                log.error(error, e);
            }

            description += "\ndigest (SHA-1): " + Hex.encodeHexString(eventDigest);
        }
    }
}
