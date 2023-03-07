package hirs.utils.tpm.eventlog;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.events.EvConstants;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Class to process a TCG_PCR_EVENT.
 * TCG_PCR_EVENT is used when the Event log uses the SHA1 Format as described in the
 * TCG Platform Firmware Profile specification.
 * typedef struct {
 * UINT32                   PCRIndex;        //PCR Index value that either
 * //matches the PCRIndex of a
 * //previous extend operation or
 * //indicates that this Event Log
 * //entry is not associated with
 * //an extend operation
 * UINT32                   EventType;       //See Log event types
 * BYTE                     digest[20];      //The SHA1 hash of the event data
 * UINT32                   EventSize;       //Size of the event data
 * UINT8                    Event[1];        //
 * } TCG_PCR_EVENT;                             //The event data structure to be added
 */
public class TpmPcrEvent1 extends TpmPcrEvent {

    /**
     * Constructor.
     *
     * @param is          ByteArrayInputStream holding the TCG Log event.
     * @param eventNumber event position within the event log.
     * @throws java.io.IOException              if an error occurs in parsing the event.
     * @throws java.security.NoSuchAlgorithmException if an undefined algorithm is encountered.
     * @throws java.security.cert.CertificateException     If a certificate within an event can't be processed.
     */
    public TpmPcrEvent1(final ByteArrayInputStream is, final int eventNumber)
            throws IOException, CertificateException, NoSuchAlgorithmException {
        super(is);
        setDigestLength(EvConstants.SHA1_LENGTH);
        setLogFormat(1);
        /**  Event data. */
        byte[] event = null;
        byte[] rawIndex = new byte[UefiConstants.SIZE_4];
        byte[] rawType = new byte[UefiConstants.SIZE_4];
        byte[] rawEventSize = new byte[UefiConstants.SIZE_4];
        byte[] eventDigest = new byte[EvConstants.SHA1_LENGTH];
        byte[] eventContent = null;
        int digestSize = EvConstants.SHA1_LENGTH;
        int eventSize = 0;
        String hashName = "TPM_ALG_SHA1";
        if (is.available() > UefiConstants.SIZE_32) {
            is.read(rawIndex);
            setPcrIndex(rawIndex);
            is.read(rawType);
            setEventType(rawType);
            is.read(eventDigest);
            setEventDigest(eventDigest, digestSize);
            is.read(rawEventSize);
            eventSize = HexUtils.leReverseInt(rawEventSize);
            eventContent = new byte[eventSize];
            is.read(eventContent);
            setEventContent(eventContent);
            // copy entire event into a byte array for processing
            int eventLength = rawIndex.length + rawType.length + eventDigest.length
                    + rawEventSize.length;
            int offset = 0;
            event = new byte[eventLength];
            System.arraycopy(rawIndex, 0, event, offset, rawIndex.length);
            offset += rawIndex.length;
            System.arraycopy(rawType, 0, event, offset, rawType.length);
            offset += rawType.length;
            System.arraycopy(eventDigest, 0, event, offset, eventDigest.length);
            offset += eventDigest.length;
            System.arraycopy(rawEventSize, 0, event, offset, rawEventSize.length);
            offset += rawEventSize.length;
            setEventData(event);
            //System.arraycopy(eventContent, 0, event, offset, eventContent.length);
            this.processEvent(event, eventContent, eventNumber, hashName);
        }
    }
}
