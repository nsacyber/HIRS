package hirs.tpm.eventlog;

import hirs.utils.HexUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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
     * @param is ByteArrayInputStream holding the TCG Log event
     * @throws IOException if an error occurs in parsing the event
     */
    public TpmPcrEvent1(final ByteArrayInputStream is) throws IOException {
        super(is);
        setDigestLength(SHA1_LENGTH);
        byte[] unit32Data = new byte[INT_LENGTH];
        if (is.available() > MIN_SIZE) {
            is.read(unit32Data);
            setPcrIndex(unit32Data);
            is.read(unit32Data);
            setEventType(unit32Data);
            byte[] eventDigest = new byte[SHA1_LENGTH];
            is.read(eventDigest);
            setDigest(eventDigest);
            is.read(unit32Data);
            int eventSize = HexUtils.leReverseInt(unit32Data);
            byte[] eventContent = new byte[eventSize];
            is.read(eventContent);
        }
    }
}
