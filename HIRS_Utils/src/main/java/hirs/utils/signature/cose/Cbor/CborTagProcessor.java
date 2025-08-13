package hirs.utils.signature.cose.Cbor;

import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORTaggedItem;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.CoRim;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.ArmSwcompId;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.ComidSvn;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.RawValue;
import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.Coswid;
import hirs.utils.signature.cose.CoseType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

/**
 * Class to support processing of RIM-specific CBOR tags.
 * Support is limited to tags associated with supported RIM types.
 * <p>
 * Relevant registry:
 * <a href="https://www.iana.org/assignments/cbor-tags/cbor-tags.xhtml">
 * IANA CBOR Tag Registry</a>.
 * </p>
 */
@Getter @NoArgsConstructor
public class CborTagProcessor {
    private byte[] content = null;
    private boolean isTagged = false;
    private boolean isCoswid = false;
    private boolean isCose = false;
    private boolean isCorim = false;
    private boolean isComid = false;
    private boolean isCotl = false;
    private boolean isOid = false;
    private String oid = "";
    private String tagLabel = "";
    private int tagId = 0;
    private String tagName = "";
    /** Cbor tag value. */
    private static byte cBorTagByte = (byte) 0xC0;
    /** Coswid tag length. */
    private static int coswidTagLength = 4;
    /** Length of cbor type. */
    private static int cborTypeLength = 1;
    /** Offset of cbor type. */
    private static int cborTypeStart = 0;
    /** Offset for coswid tag. */
    private static int coswidStart = 0;
    /** String used to represent a Coswid Tag. */
    private static String cborCoswidTagStr = "CoSWID";
    private static int tagMask = 0xE0;
    private static int infoMask = 0x1F;
    private static int offset = 0x05;
    private static int tagType = 0x06;
    private static int berOidTag = 0x111;    //object identifier tag per [RFC9090]
    // Tags defines in the IETF CoRIM spec
    private static int corimSvnEq = 0x228; // (228 dec) security version number (SVN) with equivalence
    private static int corimSvnGt = 0x229; // (553 dec) SVN evaluated with greater than or equals semantics
    private static int corimTagByteType = 0x230; // (560 dec) Corim defined Tagged Bytes Type
    private static int corimMaskedRawValue = 0x233; // (563 dec) Corim defined tagged-masked-raw-value
    private static int corimOidType = 0x6f; // (111 dec) Corim defined tagged-oid-type
    private static int corimUuidTpe = 0x25; // (37 dec) tagged-uuid-type
    // Tags defines in  IETF draft-ydb-rats-cca-endorsements specification
    private static int armImpIdType = 0x258; // (dec 600) rats-caa Arm defined implementation-id-type
    private static int armSeCompId = 0x259; //(dec 601) rats-caa Arm defined arm-swcomp-id
    /** Tag define in rfc 8949 table 7. */
    private static int cborNegInt = 0x20; // (32 dec) Corim defined Regid URI
    /** rfc 8049 table 7 defined value. */
    public static final int CBOR_ONE_BYTE_UNSIGNED_INT = 0x18; // (24 dec) Cbor defined single byte int
    /** rfc 8049 table 7 defined value. */
    public static final int CBOR_FOUR_BYTE_UNSIGNED_INT = 0x1a; // (26 dec) Cbor defined two byte int
    /** rfc 8049 table 7 defined value. */
    public static final int CBOR_TWO_BYTE_UNSIGNED_INT = 0x19; //corim (25 dec) Cbor defined 4 byte int

    /**
     * This constructor checks for a Cbor Coswid tag and strips it off if found.
     * A Tag is defined as major type 6 which should be found in the highest 3 bits of the first byte.
     * The next 5 bytes determine how to process the tag.
     * Currently only the COSE, Coswid, and CoRim tags are being processed.
     * All other tags will result in an exception "1398229316" is a CBOR tag defined for coswid that
     * gets written to the start of the Coswid object.
     *
     * @param taggedData byte[]  Coswid data
     */
    public CborTagProcessor(final byte[] taggedData) {
        byte[] tagData = new byte[coswidTagLength];
        byte tagByte = taggedData[cborTypeStart];
        byte cborType = (byte) ((tagByte & tagMask) >> offset);
        byte tagInfo = (byte) (tagByte & infoMask);
        if (cborType != tagType) {
            isTagged = false;
            return;
        }
        // process per table 7 of rfc 8489 for integers only (gets to expansive for rarely used options)
        if (tagInfo < CBOR_ONE_BYTE_UNSIGNED_INT) {
            tagId = tagInfo; // values 0 to 0x17
        } else if (tagInfo == CBOR_ONE_BYTE_UNSIGNED_INT) {
            tagId = (int) taggedData[1];
        } else if (tagInfo == CBOR_TWO_BYTE_UNSIGNED_INT) {
            byte[] tmpArray = {0, 0, taggedData[1], taggedData[2]};
            ByteBuffer buf = ByteBuffer.wrap(tmpArray);
            tagId = buf.getInt();
        } else if (tagInfo == CBOR_FOUR_BYTE_UNSIGNED_INT) {
            byte[] tmpArray = {taggedData[1], taggedData[2], taggedData[3], taggedData[4]};
            ByteBuffer buf = ByteBuffer.wrap(tmpArray);
            tagId = buf.getInt();
        }
        // Check for a CBOR tagged, process tag if it is
        if (cborType == tagType) {
            isTagged = true;
            if (CoRim.isCoMidTag(tagId)) {
                int contentLength = taggedData.length - 3;
                content = new byte[contentLength];
                isComid = true;
                isCorim = true;
                tagLabel = "CoMid";
                System.arraycopy(taggedData, 3, content, 0, contentLength);
                return;
            } else if (CoRim.isCoSwidTag(tagId)) {
                int contentLength = taggedData.length - 3;
                content = new byte[contentLength];
                isCoswid = true;
                isCorim = true;
                tagLabel = "Coswid";
                System.arraycopy(taggedData, 3, content, 0, contentLength);
                return;
            } else if (CoRim.isTlTag(tagId)) {
                int contentLength = taggedData.length - 3;
                content = new byte[contentLength];
                isCotl = true;
                isCorim = true;
                tagLabel = "CoTL";
                System.arraycopy(taggedData, 3, content, 0, contentLength);
                return;
            } else if (tagId == berOidTag) {
                int contentLength = taggedData.length - 3;
                content = new byte[contentLength];
                isOid = true;
                System.arraycopy(taggedData, 3, content, 0, contentLength);
                CborBstr oidStr = new CborBstr(content);
                oid = oidStr.toString();
                tagLabel = "COSWID_BER_OID";
            } else if (CoRim.isCoRimTag(tagId)) {
                int contentLength = taggedData.length - 3;
                content = new byte[contentLength];
                isCorim = true;
                tagLabel = "Corim";
                System.arraycopy(taggedData, 3, content, 0, contentLength);
                return;
            } else if (CoseType.isCoseTag(tagId)) {
                int contentLength = taggedData.length - 1;
                content = new byte[contentLength];
                isCose = true;
                tagLabel = CoseType.getItemName(tagInfo);
                System.arraycopy(taggedData, 0, content, 0, contentLength);
                return;
            }
            System.arraycopy(taggedData, cborTypeLength, tagData, cborTypeStart, coswidTagLength);
            int dataTag = ByteBuffer.wrap(tagData).getInt();
            // Process tags
            if (dataTag == Coswid.coswidTag) {
                tagName = cborCoswidTagStr;
                tagId = Coswid.coswidTag;
                isCoswid = true;
                byte[] byteArrayData = new byte[taggedData.length - coswidTagLength - cborTypeLength];
                System.arraycopy(taggedData, coswidTagLength + cborTypeLength, byteArrayData, coswidStart,
                        taggedData.length - coswidTagLength - cborTypeLength);
                // Remove Cbor ByteString encoding to get Coswid Data
                CborBstr ctag = new CborBstr(byteArrayData);
                content = ctag.getContents();
            } else {
                content = taggedData.clone();
            }
        }  else {
            isTagged = false;
            tagName = "";
        }
    }
    /**
     * Helper method to convert an array of bytes to a {@link UUID} object.
     * This works by reconstructing the UUID from the most-significant bits (MSB)
     * and least-significant bits (LSB) contained in two 64-bit components, respectively.
     *
     * @param bytes An array of 16 bytes, corresponding to two 64-bit parts: MSB and LSB.
     * @return a UUID based upon the bytes provided
     */
    public static UUID bytesToUUID(final byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long mostSigBits = bb.getLong();
        long leastSigBits = bb.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }
    /**
     * Convenience method to process a {@link CBORTaggedItem} and associated tag number.
     * The output is contextual, and will require one to typically check the associated class,
     * such as in the following pattern:
     * <p>
     * {@code if (outputObject instanceof String) { ... } }
     *
     * @param taggedItem The tagged input item to be processed.
     * @return An {@link Optional} associated with the tagged input item, populated if matching tag found.
     */
    public static Optional<Object> process(final CBORTaggedItem taggedItem) {
        if (taggedItem.getTagNumber().equals(cborNegInt)) { // Reg id : URI
            String uriString = (String) taggedItem.getTagContent().parse();
            URI uri;
            try {
                uri = new URI(uriString);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return Optional.of(uri);
        } else if (taggedItem.getTagNumber().equals(corimUuidTpe)) { // UUID
            // Note: per section 7.4 in the IETF CoRIM specification, UUID is an array of 16 bytes
            UUID uuid = bytesToUUID((byte[]) taggedItem.getTagContent().parse());
            return Optional.of(uuid);
        } else if (taggedItem.getTagNumber().equals(corimOidType)) { // OID
            // Note: per section 7.6 in the IETF CoRIM specification, OID is a BER-encoded array of bytes
            try {
                var oid = new Oid((byte[]) taggedItem.getTagContent().parse());
                return Optional.of(oid);
            } catch (GSSException e) {
                throw new RuntimeException(e);
            }
        } else if (taggedItem.getTagNumber().equals(corimSvnEq)) { // tagged-svn
            // See section 5.1.4.1.4.4 in the IETF CoRIM specification
            var comidSvn = new ComidSvn((Integer) taggedItem.getTagContent().parse());
            return Optional.of(comidSvn);
        } else if (taggedItem.getTagNumber().equals(corimSvnGt)) { // tagged-min-svn
            // See section 5.1.4.1.4.4 in the IETF CoRIM specification
            var comidMinSvn = new ComidSvn((Integer) taggedItem.getTagContent().parse());
            comidMinSvn.setMinSvn(true);
            return Optional.of(comidMinSvn);
        } else if (taggedItem.getTagNumber().equals(corimTagByteType)) { // tagged-bytes
            // See section 7.8 in the IETF CoRIM specification
            var taggedBytes = (byte[]) taggedItem.getTagContent().parse();
            return Optional.ofNullable(taggedBytes);
        } else if (taggedItem.getTagNumber().equals(corimMaskedRawValue)) { // tagged-masked-raw-value
            // See section 5.1.4.1.4.6 in the IETF CoRIM specification
            var maskedRawValue = new RawValue();
            var maskedRVItems = ((CBORItemList) taggedItem.getTagContent()).getItems();
            var maskIter = maskedRVItems.iterator();
            maskedRawValue.setValue((byte[]) maskIter.next().parse()); // value
            maskedRawValue.setMask((byte[]) maskIter.next().parse()); // mask
            return Optional.of(maskedRawValue);
        } else if (taggedItem.getTagNumber().equals(armImpIdType)) { // tagged-implementation-id-type
            // See section 3.1.2 in the IETF draft-ydb-rats-cca-endorsements specification
            var implementationIdType = (byte[]) taggedItem.getTagContent().parse();
            return Optional.of(implementationIdType);
        } else if (taggedItem.getTagNumber().equals(armSeCompId)) { // arm-swcomp-id
            // See section 3.1.3.1 in the IETF draft-ydb-rats-cca-endorsements specification
            var armSwcompId = new ArmSwcompId(taggedItem);
            return Optional.of(armSwcompId);
        }
        return Optional.empty();
    }

    /**
     * Returns a defensive copy of the content byte array.
     *
     * @return a copy of the content array
     */
    public byte[] getContent() {
        return content.clone();
    }
}
