package hirs.utils.tpm.eventlog;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Class to process a TCG_PCR_EVENT2 which is used
 * when the Event log uses the Crypto Agile (SHA256) format as described in the
 * TCG Platform Firmware Profile specification.
 * This class will only process SHA-256 digests.
 * typedef struct {
 * .    UINT32                 pcrIndex;        //PCR Index value that either
 * .                                            //matches the PCR Index of a
 * .                                            //previous extend operation or
 * .                                            //indicates that this Event Log
 * .                                            //entry is not associated with
 * .                                            //an extend operation
 * .    UINT32                eventType;        //See Log event types
 * .    TPML_DIGEST_VALUES    digest;           //Number & list of tagged digests
 * .    UINT32                eventSize;        //Size of the event content
 * .    BYTE                  event[EventSize]; //The event content
 * } TCG_PCR_EVENT2;                            //The event data structure to be added
 * In this code:
 * .   Event header = pcrIndex + eventType + digests + eventSize
 * .   Event content = event[EventSize]
 * typedef struct {
 * .    UINT32  count;
 * .    TPMT_HA digests[count];
 * } TPML_DIGEST_VALUES;
 * typedef struct {
 * .    TPMI_ALG_HASH hashAlg;
 * .    TPMU_HA       digest;
 * } TPMT_HA;
 * typedef union {
 * .    BYTE sha1[SHA1_DIGEST_SIZE];
 * .    BYTE sha256[SHA256_DIGEST_SIZE];
 * .    BYTE sha384[SHA384_DIGEST_SIZE];
 * .    BYTE sha512[SHA512_DIGEST_SIZE];
 * } TPMU_HA;
 * define SHA1_DIGEST_SIZE   20
 * define SHA256_DIGEST_SIZE 32
 * define SHA384_DIGEST_SIZE 48
 * define SHA512_DIGEST_SIZE 64
 * typedef TPM_ALG_ID TPMI_ALG_HASH;
 * typedef UINT16 TPM_ALG_ID;
 * define TPM_ALG_SHA1           (TPM_ALG_ID)(0x0004)
 * define TPM_ALG_SHA256         (TPM_ALG_ID)(0x000B)
 * define TPM_ALG_SHA384         (TPM_ALG_ID)(0x000C)
 * define TPM_ALG_SHA512         (TPM_ALG_ID)(0x000D)
 */
@Log4j2
public class TpmPcrEvent2 extends TpmPcrEvent {

    /**
     * Constructor.
     *
     * @param is                        ByteArrayInputStream holding the TCG Log event
     * @param logFileBytesRemaining     number of bytes remaining in log file (for error checking)
     * @param eventNumber               event position within the event log
     * @param strongestAlg              name of strongest hash algorithm used in the log
     * @throws java.io.IOException                     if an error occurs in parsing the event
     */
    public TpmPcrEvent2(final ByteArrayInputStream is, final long logFileBytesRemaining,
                        final int eventNumber, final String strongestAlg)
            throws IOException {

        super(is);
        setLogFormat(2);

        // Event data.
        String hashName = "";
        byte[] eventHeader;
        byte[] rawIndex = new byte[UefiConstants.SIZE_4];
        byte[] algCountBytes = new byte[UefiConstants.SIZE_4];
        byte[] rawType = new byte[UefiConstants.SIZE_4];
        byte[] rawEventSize = new byte[UefiConstants.SIZE_4];
        byte[] eventDigest = null;
        byte[] eventContent = null;
        TcgTpmtHa hashAlg = null;
        int eventSize = 0;

        //TCG_PCR_EVENT2
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

            // read TPML_DIGEST_VALUES (algCount should match 'numberOfAlgorithms' in Spec ID event)
            is.read(algCountBytes);
            int algCount = HexUtils.leReverseInt(algCountBytes);
            if (algCount < 0) {
                throw new IOException("Number of digests is a negative value; possibly corrupt byte file.");
            }

            // Process TPMT_HA
            ArrayList<TcgTpmtHa> hashList = new ArrayList<>();
            for (int i = 0; i < algCount; i++) {
                try {
                    hashAlg = new TcgTpmtHa(is);
                } catch (IOException io) {
                    throw new IOException("Issue reading hash algorithm and digests; possibly corrupt byte file.", io);
                }
                hashName = hashAlg.getHashName();
                eventDigest = new byte[hashAlg.getHashLength()];
                hashList.add(hashAlg);
                hashListFromEvent.add(new EventDigest(hashName, eventDigest));
                if (hashName.compareTo(strongestAlg) == 0) {
                    setEventStrongestDigest(hashAlg.getDigest());
                }
            }

            // track event header length
            int eventHeaderLength = rawIndex.length + rawType.length + rawEventSize.length;
            for (TcgTpmtHa hash : hashList) {
                eventHeaderLength += hash.getBuffer().length;
            }

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
            System.arraycopy(rawEventSize, 0, eventHeader, offset, rawEventSize.length);
            offset += rawEventSize.length;
            for (TcgTpmtHa hash : hashList) {
                System.arraycopy(hash.getBuffer(), 0, eventHeader, offset, hash.getBuffer().length);
                offset += hash.getBuffer().length;
            }
            setEventHeader(eventHeader);

            // if event content cannot be processed, this is not fatal; log error and continue with attestation
            try {
                // read event content
                eventContent = new byte[eventSize];
                is.read(eventContent);
                setEventContent(eventContent);

                this.processEvent(eventContent, eventNumber);
            } catch (RuntimeException r) {
                throw r;
            } catch (Exception e) {
                String error = "Error parsing event #" + eventNumber + ", " + eventTypeStr + ", " + pcrIndexStr;
                log.error(error, e);
            }

            for (int i = 0; i < algCount; i++) {
                description += "\ndigest (" + hashList.get(i).getHashName() + "): "
                        + Hex.encodeHexString(hashList.get(i).getDigest());
            }
        }
    }
}
