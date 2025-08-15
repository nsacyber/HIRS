package hirs.utils.tpm.eventlog;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.events.EvConstants;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

/**
 * Class to process a TCG_PCR_EVENT2 which is used
 * when the Event log uses the Crypto Agile (SHA256) format as described in the
 * TCG Platform Firmware Profile specification.
 * This class will only process SHA-256 digests.
 * typedef struct {
 * .    UINT32                 PCRIndex;  //PCR Index value that either
 * .                                      //matches the PCRIndex of a
 * .                                      //previous extend operation or
 * .                                      //indicates that this Event Log
 * .                                      //entry is not associated with
 * .                                      //an extend operation
 * .    UINT32                EventType; //See Log event types
 * .    TPML_DIGEST_VALUES    digest;    //The hash of the event data
 * .    UINT32                EventSize; //Size of the event data
 * .    BYTE                  Event[1];  //The event data
 * } TCG_PCR_EVENT2;                     //The event data structure to be added
 * typedef struct {
 * .    UINT32  count;
 * .    TPMT_HA digests[HASH_COUNT];
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
public class TpmPcrEvent2 extends TpmPcrEvent {
    /**
     * algorithms found.
     */
    private int algCount = 0;

    /**
     * list of digests.
     */
    private final ArrayList<TcgTpmtHa> hashList = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param is          ByteArrayInputStream holding the TCG Log event
     * @param eventNumber event position within the event log.
     * @throws java.io.IOException                     if an error occurs in parsing the event
     * @throws java.security.NoSuchAlgorithmException  if an undefined algorithm is encountered.
     * @throws java.security.cert.CertificateException If a certificate within an event can't be processed.
     */
    public TpmPcrEvent2(final ByteArrayInputStream is, final int eventNumber, String strongestAlg)
            throws IOException, CertificateException, NoSuchAlgorithmException {
        super(is);
//        setDigestLength(EvConstants.SHA256_LENGTH);
        setLogFormat(2);
        // Event data.
        // int eventDigestLength = 0;
        String hashName = "";
        byte[] event2;
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
            is.read(rawIndex);
            setPcrIndex(rawIndex);
            is.read(rawType);
            setEventType(rawType);
            // TPML_DIGEST_VALUES (algCount should match 'numberOfAlgorithms' in Spec ID event)
            is.read(algCountBytes);
            algCount = HexUtils.leReverseInt(algCountBytes);
            // Process TPMT_HA,
            for (int i = 0; i < algCount; i++) {
                hashAlg = new TcgTpmtHa(is);
                hashName = hashAlg.getHashName();
                eventDigest = new byte[hashAlg.getHashLength()];
                hashList.add(hashAlg);
                hashListFromEvent.add(new EventDigest(hashName, eventDigest));
//                setEventDigest(hashAlg.getDigest(), hashAlg.getHashLength());
                if (hashName.compareTo(strongestAlg) == 0) {
                    setEventStrongestDigest(hashAlg.getDigest());
                }
            }
            is.read(rawEventSize);
            eventSize = HexUtils.leReverseInt(rawEventSize);
            eventContent = new byte[eventSize];
            is.read(eventContent);
            setEventContent(eventContent);
//            int eventLength = rawIndex.length + rawType.length + eventDigest.length
//                    + rawEventSize.length;
            int eventLength = rawIndex.length + rawType.length
                    + rawEventSize.length;
            int offset = 0;
            for (TcgTpmtHa hash : hashList) {
                eventLength += hash.getBuffer().length;
            }
            event2 = new byte[eventLength];
            System.arraycopy(rawIndex, 0, event2, offset, rawIndex.length);
            offset += rawIndex.length;
            System.arraycopy(rawType, 0, event2, offset, rawType.length);
            offset += rawType.length;
//            System.arraycopy(eventDigest, 0, event, offset, eventDigest.length);
//            offset += eventDigest.length;



// CHECK HERE - may have to use the strongest digest here for ACA to work
// (plus will need to add the size back in above)
            System.arraycopy(rawEventSize, 0, event2, offset, rawEventSize.length);
            offset += rawEventSize.length;

            for (TcgTpmtHa hash : hashList) {
                System.arraycopy(hash.getBuffer(), 0, event2, offset, hash.getBuffer().length);
                offset += hash.getBuffer().length;
            }
            //System.arraycopy(eventContent, 0, event, offset, eventContent.length);
            setEventData(event2);
            //setDigestLength(eventDigestLength);

//            this.processEvent(event2, eventContent, eventNumber, hashName);
            this.processEvent(event2, eventContent, eventNumber);
            for (int i = 0; i < algCount; i++) {
                description +=  "\ndigest (" + hashList.get(i).getHashName() + "): "
                        + Hex.encodeHexString(hashList.get(i).getDigest());
//                if (hashList.get(i).getHashName().compareToIgnoreCase(TcgTpmtHa.TPM_ALG_SHA256_STR) == 0) {
//                    description +=  "\ndigest (SHA256): " + Hex.encodeHexString(hashList.get(i).getDigest());
//                }
//                if (hashList.get(i).getHashName().compareToIgnoreCase(TcgTpmtHa.TPM_ALG_SHA1_STR) == 0) {
//                    description +=  "\ndigest (SHA-1): " + Hex.encodeHexString(hashList.get(i).getDigest());
//                }
            }
        }
    }
}
