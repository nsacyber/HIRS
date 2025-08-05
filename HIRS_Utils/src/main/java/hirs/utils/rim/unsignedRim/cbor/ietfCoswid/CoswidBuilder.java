package hirs.utils.rim.unsignedRim.cbor.ietfCoswid;

import com.authlete.cbor.CBORByteArray;
import com.authlete.cbor.CBORTaggedItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import hirs.utils.rim.unsignedRim.common.IanaHashAlg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.HexFormat;

/**
 * Class that is used to build a Coswid file based upon values previously retrieved from a config file.
 * Uses the indexes to convert the integer key into the text values published in rfc 9393 Section 2.3
 * "The concise-swid-tag Map".
 */
public class CoswidBuilder extends Coswid {
    protected CBORGenerator cborGen = null;
    protected CoswidConfig config = null;
    protected IanaHashAlg algInfo = null;
    protected CoswidItems coswidItems = new CoswidItems();
    /**
     * Constructor for the Coswid Builder.
     * Configuration file is a json formatted file consisting of Coswid defined variables
     * to be encoded as Cbor items.
     * @param conf  Coswid Configuration file
     */
    public CoswidBuilder(final CoswidConfig conf) {
        config = conf;
        // Software
        setLang(config.getLang());
        setSoftwareName(config.getSoftwareName());
        setSoftwareVersion(config.getSoftwareVersion());
        setTagId(config.getTagId());
        setTagVersion(config.getTagVersion());
        setPatch(config.isPatch());
        setCorpus(config.isCorpus());
        setSupplemental(config.isSupplemental());
        setTagVersion(config.getTagVersion());
        // Entity
        setEntityName(config.getEntityName());
        setRegId(config.getRegId());
        setRole(config.getRole());
        setThumbprint(config.getThumbprint());
        // Link
        setHref(config.getHref());
        setRel(config.getRel());
        // Meta - NIST 8060 defined attributes
        setColloquialVersion(config.getColloquialVersion());
        setEdition(config.getEdition());
        setRevision(config.getRevision());
        setProduct(config.getProduct());
        // Meta - Coswid defined attributes supported by this application
        setDescription(config.getDescription());
        setPersistentId(config.getPersistentId());
        setProductFamily(config.getProductFamily());
        setSummary(config.getSummary());
    }
    /**
     * Method for creating a Coswid Object.
     * @param out Byte array to write Coswid data to
     * @return updated Byte array.
     */
    public ByteArrayOutputStream createCoswidData(final ByteArrayOutputStream out) throws IOException {
        initCoswid(out);
        createPayload(config.getPayloadNode(), out);
        completeCoswid(out);
        out.flush();
        return out;
    }
    /**
     * Method for creating a Coswid Object.
     * Note 1398229316 is the IANA CBOR Tag for coswid
     * @param fileName File name to place the encoded Coswid data
     */
    public void createCoswidData(final String fileName) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        //createCoswidData(out);
        initCoswid(out);
        createPayload(config.getPayloadNode(), out);
        completeCoswid(out);
        ByteArrayOutputStream taggedCoswid = addCborTag(out);
        try {
            Files.write(new File(fileName).toPath(), taggedCoswid.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Encodes the Coswid variables within this class but does not finish the encoding.
     * This allows for addition of other objects by other classes.
     * Use completeCoswid to close the ByteArrayOutputStream for writing to a file.
     * @param out ByteArrayOutputStream to hold the encoded Coswid data
     * @return ByteArrayOutputStream that contains the encoded Coswid data
     */
    public ByteArrayOutputStream initCoswid(final ByteArrayOutputStream out) {
        try {
            cborGen = new CBORFactory().createGenerator(out);  // ioContext.getEncoding()
            cborGen.writeStartObject();
            // Software Entity - Note CoSwid places SWID software element items in top level map
            addStringAttribute(getLang(), CoswidItems.LANG_INT, out);
            addStringAttribute(getSoftwareName(), CoswidItems.SOFTWARE_NAME_INT, out);
            addStringAttribute(getSoftwareVersion(), CoswidItems.SOFTWARE_VERSION_INT, out);
            addTagId(getTagId(), out);
            addStringAttribute(getTagVersion(), CoswidItems.TAG_VERSION_INT, out);
            addBooleanAttribute(isPatch(), CoswidItems.PATCH_INT, out);
            addBooleanAttribute(isSupplemental(), CoswidItems.PATCH_INT, out);
            addBooleanAttribute(isCorpus(), CoswidItems.CORPUS_INT, out);
            // Entity - Create array and add Entity defined attributes
            cborGen.writeFieldId(CoswidItems.ENTITY_INT);
            cborGen.writeStartObject();
            addStringAttribute(getSwidEntityName(), CoswidItems.ENTITY_NAME_INT, out);
            addStringAttribute(getRegId(), CoswidItems.REG_ID_INT, out);
            addRoles(getRole(), CoswidItems.ROLE_INT, out);
            addStringAttribute(getThumbprint(), CoswidItems.THUMBPRINT_INT, out);
            cborGen.writeEndObject();
            // Link - Create array and add link attributes
            cborGen.writeFieldId(CoswidItems.LINK_INT);
            cborGen.writeStartObject();
            addStringAttribute(getHref(), CoswidItems.HREF_INT, out);
            addStringAttribute(getRel(), CoswidItems.REL_INT, out);
            cborGen.writeEndObject();
            // Meta -  NIST IR 8060, TCG PC Client RIM, and TCG Component RIM
            cborGen.writeFieldId(CoswidItems.SOFTWARE_META_INT);
            cborGen.writeStartObject();
            // Meta - NIST IR 8060 specific fields
            addStringAttribute(getColloquialVersion(), CoswidItems.COLLOQUIAL_VERSION_INT, out);
            addStringAttribute(getEdition(), CoswidItems.EDITION_INT, out);
            addStringAttribute(getRevision(), CoswidItems.REVISION_INT, out);
            addStringAttribute(getProduct(), CoswidItems.PRODUCT_INT, out);
            // Meta - Coswid defined Meta Values
            addStringAttribute(getPersistentId(), CoswidItems.PERSISTENT_ID_INT, out);
            addStringAttribute(getDescription(), CoswidItems.PERSISTENT_ID_INT, out);
            addStringAttribute(getProductFamily(), CoswidItems.PRODUCT_FAMILY_INT, out);
            addStringAttribute(getSummary(), CoswidItems.SUMMARY_INT, out);
            cborGen.writeEndObject();
            cborGen.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error building CoSwid Object: " + e.getMessage());
        }
        return out;
    }

    /**
     * Completes the encoding of the coswid data contained in a ByteArrayOutputStream object.
     * @param out ByteArrayOutputStream to hold the encoded Coswid data
     * @return ByteArrayOutputStream that contains the encoded Coswid data
     */
    protected ByteArrayOutputStream completeCoswid(final ByteArrayOutputStream out) {
        try {
            //cborGen.writeEndObject();  // For Meta
            cborGen.writeEndObject(); // For the Entire RIM
            cborGen.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out;
    }
    /**
     * Adds a string attribute to the ByteArrayOutputStream.
     * @param attribute String to hold the field value
     * @param fieldItem int : spec defined Coswid "index" for the item
     * @param out ByteArrayOutputStream that holds the encoded Coswid object
     */
    protected void addStringAttribute(final String attribute, final int fieldItem,
                                      final ByteArrayOutputStream out) {
        // byte[] value = attribute.getBytes(StandardCharsets.UTF_8);
        if (attribute == null) { // Skip if variable was null
            return;
        }
        if (attribute.isEmpty()) { // Skip if variable was empty
            return;
        }
        try {
            cborGen.writeFieldId(fieldItem); // index
            cborGen.writeString(attribute);  // value
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    /**
     * Adds a string attribute to the ByteArrayOutputStream.
     * @param attribute Int to hold the field value
     * @param fieldItem int : spec defined Coswid "index" for the item
     * @param out ByteArrayOutputStream that holds the encoded Coswid object
     */
    protected void addIntAttribute(final int attribute, final int fieldItem,
                                   final ByteArrayOutputStream out) {
        try {
            cborGen.writeFieldId(fieldItem); // index
            cborGen.writeNumber(attribute);  // value
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    /**
     * Adds a boolean attribute to the ByteArrayOutputStream.
     * @param attribute Boolean to hold the field value
     * @param fieldItem int : spec defined Coswid "index" for the item
     * @param out       ByteArrayOutputStream that holds the encoded Coswid object
     */
    protected void addBooleanAttribute(final boolean attribute, final int fieldItem,
                                       final ByteArrayOutputStream out) {
        try {
            cborGen.writeFieldId(fieldItem); // index
            cborGen.writeBoolean(attribute);  // value
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Adds the tagid to the encoded Coswid Object.
     * @param tagId String GUID
     * @param out   ByteArrayOutputStream to add the tagid into.
     */
    protected void addTagId(final String tagId, final ByteArrayOutputStream out) {
        byte[] bytes = convertUUIDtoBytes(tagId);
        try {
            cborGen.writeFieldId(CoswidItems.TAG_ID_INT);
            cborGen.writeBinary(bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Converts a comma separated string from the configuration file, converts each sting item to an int index
     * and encodes it as an array using the provided index as a array identifier.
     * @param roles     comma separated string , each item must exactly match the role name in rfc9393
     * @param roleIndex the dex value to use for the role array
     * @param out       ByteArrayOutputStream to add the role array into
     */
    protected void addRoles(final String roles, final int roleIndex, final ByteArrayOutputStream out) {
        String[] roleList = roles.split(",");
        int[] roleArray = new int[roleList.length];

        for (int i = 0; i < roleList.length; i++) {
            roleArray[i] = roleLookup(roleList[i]);
        }
        try {
            cborGen.writeFieldId(roleIndex);
            cborGen.writeArray(roleArray, 0, roleList.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts a tagid string (UUID) into a "16-byte binary string" per rfc 9393.
     * @param guid Global Unique Identifier
     * @return byte array holding the UUID
     */
    protected byte[] convertUUIDtoBytes(final String guid) {
        byte[] bytes = new byte[16];
        UUID uuid = UUID.fromString(guid);
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();

        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (mostSigBits >>> (8 * (7 - i)));
            bytes[i + 8] = (byte) (leastSigBits >>> (8 * (7 - i)));
        }
        return bytes;
    }
    /**
     * Uses the role string field name defined in rfc9393 to convert the index value.
     * This lookup is specific for roles defined in section 2.6
     * @param role The index value defined in RFC-9393 for roles
     * @return role index
     */
    protected int roleLookup(final String role) {
        int roleValue = 0;
        switch (role) {
            case "tag-creator":  roleValue = 1; break;
            case "software-creator": roleValue = 2; break;
            case "aggregator": roleValue = 3; break;
            case "distributor": roleValue = 4; break;
            case "licensor": roleValue = 5; break;
            case "maintainer": roleValue = 6; break;
            default: roleValue = 0xff;
        }
        return roleValue;
    }
    /**
     * Builds a Coswid payload based upon the Json based config file.
     * @param payloadNode A JSonNode pointing to the Payload of Json Config File
     * @param out ByteArrayOutputStream that holds the encoded Coswid object
     */
    protected void createPayload(final JsonNode payloadNode, final ByteArrayOutputStream out) {

        try {
            cborGen.writeFieldId(CoswidItems.PAYLOAD_INT); // Add payload map
            cborGen.writeStartObject();
            Iterator<Map.Entry<String, JsonNode>> fields = payloadNode.fields();
            int i = 0;
            while (fields.hasNext()) {
                ArrayList<String> payloadItem = new ArrayList<>();
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                if (key.compareToIgnoreCase("Directory") == 0) {
                    createDirectory(field.getValue(), out);
                } else if (key.compareToIgnoreCase("File") == 0) {
                    prepFile(field.getValue(), out);
                } else {
                    JsonNode item = field.getValue();
                    String value = field.getValue().textValue();
                    addStringAttribute(key, coswidItems.getIndex(value), out);
                }
                cborGen.writeEndObject();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Adds a Cbor directory based upon input from the Json Configuration file.
     * @param node  JsonNode to place the directory
     * @param out ByteArrayOutputStream that holds the encoded Coswid object
     * @throws IOException if an issue occur when updating cbor data
     */
    protected void createDirectory(final JsonNode node, final ByteArrayOutputStream out) throws IOException {
        cborGen.writeFieldId(CoswidItems.DIRECTORY_INT); // Add Directory map
        cborGen.writeStartObject();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            if (key.compareToIgnoreCase("File") == 0) {
                cborGen.writeFieldId(CoswidItems.FILE_INT); // Add payload map
                prepFile(field.getValue(), out);
            } else {
                String value = field.getValue().textValue();
                addStringAttribute(value, coswidItems.getIndex(key), out);
            }
        }
        cborGen.writeEndObject();
    }
    /**
     * Saves the current Coswid Data to A Byte array.
     * @param node cbor encoded data
     * @param out ByteArrayOutputStream that holds the encoded Coswid object
     * @throws IOException if an issue occurs when creating or writing a file
     */
    protected void prepFile(final JsonNode node, final ByteArrayOutputStream out) throws IOException {
        int i = 0;
        // If multiple files are provided then process one at a time
        if (node.isArray()) {
            cborGen.writeStartArray();
            for (i = 0; i < node.size(); i++) {
                prepFile(node.get(i), out);
            }
            cborGen.writeEndArray();
        } else {
            cborGen.writeStartObject();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                if (key.compareToIgnoreCase(CoswidItems.FILE_STR) == 0) {
                    String value = field.getValue().textValue();
                    addStringAttribute(value, coswidItems.getIndex(key), out);
                } else if (key.compareToIgnoreCase(CoswidItems.SIZE_STR) == 0) {
                    int value = Integer.parseInt(field.getValue().textValue());
                    addIntAttribute(value, coswidItems.getIndex(key), out);
                } else if (key.compareToIgnoreCase(CoswidItems.HASH_STR) == 0) {
                    String value = field.getValue().textValue();
                    createFileHash(out, value, IanaHashAlg.SHA_256);
                } else {
                    String value = field.getValue().textValue();
                    addStringAttribute(value, coswidItems.getIndex(key), out);
                }
            }
            cborGen.writeEndObject();
        }
    }
    /**
     * Create a hash-entry array as specified i rfc 9393.
     * hash-entry = [
     *   hash-alg-id: int,
     *   hash-value: bytes,
     * ]
     * where hash-alg-id value is defined by Iana :
     * https://www.iana.org/assignments/named-information/named-information.xhtml
     * @param out  ByteArrayOutputStream to add the tagid into.
     * @param hash  String holding the text representation of the hash value
     * @param alg Iana registered algorithm ID
     * @throws IOException if an issue occur when updating cbor data
     */
    protected void createFileHash(final ByteArrayOutputStream out, final String hash, final IanaHashAlg alg)
            throws IOException {
        HexFormat hexTool =  HexFormat.of();
        int size = hash.length() / 2;

        byte[] hashByteArray = hexTool.parseHex(hash);

        try {
            cborGen.writeFieldId(CoswidItems.HASH_INT);
            cborGen.writeStartArray();
            cborGen.writeNumber(alg.getAlgId());
            cborGen.writeBinary(hashByteArray);
            cborGen.writeEndArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * "1398229316" is a CBOR tag defined for coswid that gets written to the start of the Coswid object.
     * Cbor Tags are defined in https://www.iana.org/assignments/cbor-tags/cbor-tags.xhtml
     * Note in section 8 of RFC 9393 states that Coswid tags should be tagged but redundant tagging
     * should be avoided.
     * @param untaggedCoswid  Coswid Byte array to add the tag to
     * @throws IOException if an issue occur when updating cbor data
     * @return Coswid Byte array with a Coswid CBOR tag added
     */
    public ByteArrayOutputStream addCborTag(final ByteArrayOutputStream untaggedCoswid) throws IOException {
        ByteArrayOutputStream taggedCoswid = new ByteArrayOutputStream();
        CBORByteArray coswidData = new CBORByteArray(untaggedCoswid.toByteArray());
        CBORTaggedItem taggedCbor = new CBORTaggedItem(Coswid.coswidTag, coswidData);
        taggedCbor.encode(taggedCoswid);
        //System.out.println(taggedCoswid.prettify());
        return taggedCoswid;
    }
}
