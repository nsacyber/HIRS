package hirs.tpm.eventlog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import hirs.utils.HexUtils;

/**
 * Class to process a TCG_PCR_EVENT2 which is used
 * when the Event log uses the Crypto Agile (SHA256) format as described in the
 * TCG Platform Firmware Profile specification.
 * This class will only process SHA-256 digests.
 * typedef struct {
 *    UINT32                 PCRIndex;  //PCR Index value that either
 *                                      //matches the PCRIndex of a
 *                                      //previous extend operation or
 *                                      //indicates that this Event Log
 *                                      //entry is not associated with
 *                                      //an extend operation
 *    UINT32                EventType; //See Log event types
 *    TPML_DIGEST_VALUES    digest;    //The hash of the event data
 *    UINT32                EventSize; //Size of the event data
 *    BYTE                  Event[1];  //The event data
 * } TCG_PCR_EVENT2;                    //The event data structure to be added
 * typedef struct {
 *     UINT32  count;
 *     TPMT_HA digests[HASH_COUNT];
 *  } TPML_DIGEST_VALUES;
 * typedef struct {
 *   TPMI_ALG_HASH hashAlg;
 *   TPMU_HA       digest;
 * } TPMT_HA;
 * typedef union {
 *       BYTE sha1[SHA1_DIGEST_SIZE];
 *       BYTE sha256[SHA256_DIGEST_SIZE];
 *       BYTE sha384[SHA384_DIGEST_SIZE];
 *       BYTE sha512[SHA512_DIGEST_SIZE];
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
  /** algorithms found. */
  private int algCount = 0;

  /** list of digests. */
  private ArrayList<TcgTpmtHa> hashlist = new ArrayList<TcgTpmtHa>();

  /**
  * Constructor.
  * @param is ByteArrayInputStream holding the TCG Log event
  * @throws IOException if an error occurs in parsing the event
  */
  public TpmPcrEvent2(final ByteArrayInputStream is) throws IOException {
    super(is);
    setDigestLength(TpmPcrEvent.SHA256_LENGTH);
    //TCG_PCR_EVENT2
    byte[] rawInt = new byte[TpmPcrEvent.INT_LENGTH];
    if (is.available() > TpmPcrEvent.MIN_SIZE) {
      is.read(rawInt);
      setPcrIndex(rawInt);
      is.read(rawInt);
      setEventType(rawInt);
      // TPML_DIGEST_VALUES
      is.read(rawInt);
      algCount = HexUtils.leReverseInt(rawInt);
      // Process TPMT_HA,
      for (int i = 0; i < algCount; i++) {
        TcgTpmtHa hashAlg = new TcgTpmtHa(is);
        hashlist.add(hashAlg);
        if (hashAlg.getHashName().compareToIgnoreCase("TPM_ALG_SHA256") == 0) {
          setDigest(hashAlg.getDigest());
        }
      }
      is.read(rawInt);
      int eventSize = HexUtils.leReverseInt(rawInt);
      byte[] eventContent = new byte[eventSize];
      is.read(eventContent);
      setEventContent(eventContent);
    }
  }
}
