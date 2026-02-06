package hirs.utils.rim.unsignedRim.cbor.ietfCoswid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import hirs.utils.rim.unsignedRim.common.IanaHashAlg;
import hirs.utils.rim.unsignedRim.common.measurement.Measurement;
import hirs.utils.rim.unsignedRim.common.measurement.MeasurementType;
import hirs.utils.signature.cose.Cbor.CborTagProcessor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static hirs.utils.rim.unsignedRim.cbor.ietfCoswid.CoswidItems.FS_NAME_INT;

/**
 * Class that is used to parse a CoSWID file based upon values previously retrieved from a config file.
 * Uses the indexes to convert the integer key into the text values published in rfc 9393
 * Section 2.3 "The concise-swid-tag Map".
 * <p>
 * Based on: https://datatracker.ietf.org/doc/rfc9393/
 * </p>
 */
@NoArgsConstructor
public class CoswidParser {
    private static final Logger LOGGER = LogManager.getLogger(CoswidParser.class);
    private static final int COSWID_MAP = 0;
    private static final int ENTITY_MAP = CoswidItems.ENTITY_INT;
    private final List<String> path = new ArrayList<>();
    /**
     * Coswid object.
     */
    @Getter
    public Coswid coswid = new Coswid();
    /**
     * Tag found on the coswid data.
     */
    @Setter
    @Getter
    public int coswidTag = 0;
    protected Map<String, Object> parsedData = null;
    protected JsonNode rootNode = null;
    private String nonpayloadParsedDataOneline = "";
    private String nonpayloadParsedDataPretty = "";
    private String payloadParsedDataOneline = "";
    private String payloadParsedDataPretty = "";

    /**
     * Constructor for a CoSWID parser that takes a byte array of CBOR-encoded CoSWID object
     * and populates associated member variables.
     *
     * @param cborData CBOR-encoded byte array
     * @throws IOException for decoding errors
     */
    public CoswidParser(final byte[] cborData) throws IOException {
        CborTagProcessor tparse = new CborTagProcessor(cborData);
        byte[] untaggedCbor = null;
        // Check for a Coswid tag and remove if found
        if (tparse.isTagged()) {
            untaggedCbor = tparse.getContent();
        } else {
            untaggedCbor = cborData;
        }
        ObjectMapper mapper = new ObjectMapper(new CBORFactory());
        parsedData = mapper.readValue(new ByteArrayInputStream(untaggedCbor), Map.class);
        LOGGER.info("CoswidParser's parsed data: {}", parsedData);
        rootNode = mapper.readTree(untaggedCbor);
        initParser(untaggedCbor);
    }

    /**
     * Parses a byte array of a CBOR-encoded CoSWID object and populates associated member variables.
     * While parsing, populates a String variable with items from the CoSWID.
     *
     * @param cborData the cbor byte string to process
     */
    protected void initParser(final byte[] cborData) {

        try {
            // concise-swid-tag map
            nonpayloadParsedDataOneline += " concise-swid-tag: => ";
            nonpayloadParsedDataPretty += "concise-swid-tag:\n";
            if (rootNode.path(Integer.toString(CoswidItems.TAG_ID_INT)).binaryValue() == null) {
                coswid.setSwidTagId("unspecified");
            } else {
                byte[] tagBytes = rootNode.path(Integer.toString(CoswidItems.TAG_ID_INT)).binaryValue();
                UUID tagUuid = CborTagProcessor.bytesToUUID(tagBytes);
                String tag = tagUuid.toString();
                coswid.setTagId(tag);
            }
            buildString(CoswidItems.TAG_ID_INT, coswid.getTagId());
            coswid.setSoftwareName(parseString(COSWID_MAP, CoswidItems.SOFTWARE_NAME_INT));
            coswid.setCorpus(parseBoolean(COSWID_MAP, CoswidItems.CORPUS_INT));
            coswid.setPatch(parseBoolean(COSWID_MAP, CoswidItems.PATCH_INT));
            coswid.setSupplemental(parseBoolean(COSWID_MAP, CoswidItems.SUPPLEMENTAL_INT));
            coswid.setTagVersion(parseString(COSWID_MAP, CoswidItems.TAG_VERSION_INT));
            coswid.setSoftwareVersion(parseString(COSWID_MAP, CoswidItems.SOFTWARE_VERSION_INT));

            // global-attributes group
            String lang = parseString(COSWID_MAP, CoswidItems.LANG_INT);
            if (!lang.isEmpty()) {
                nonpayloadParsedDataOneline += " global-attributes => ";
                nonpayloadParsedDataPretty += "global-attributes :\n";
                coswid.setLang(lang);
            }

            // entity-entry map
            nonpayloadParsedDataOneline += " entity => ";
            nonpayloadParsedDataPretty += "entity :\n";
            coswid.setEntityName(parseString(ENTITY_MAP, CoswidItems.ENTITY_NAME_INT));
            coswid.setRegId(parseString(ENTITY_MAP, CoswidItems.REG_ID_INT));
            JsonNode roleNode = rootNode.path(Integer.toString(CoswidItems.ENTITY_INT))
                    .path(Integer.toString(CoswidItems.ROLE_INT));
            processRole(roleNode);
            coswid.setThumbprint(rootNode.path(Integer.toString(CoswidItems.ENTITY_INT))
                    .path(Integer.toString(CoswidItems.THUMBPRINT_INT)).asText());

            // link-entry map
            nonpayloadParsedDataOneline += " link => ";
            nonpayloadParsedDataPretty += "link :\n";

            coswid.setHref(parseString(CoswidItems.LINK_INT, CoswidItems.HREF_INT));
            coswid.setRel(parseString(CoswidItems.LINK_INT, CoswidItems.REL_INT));

            // software-meta-entry map
            nonpayloadParsedDataOneline += " software-meta => ";
            nonpayloadParsedDataPretty += "software-meta :\n";

            // software-meta-entry map: NIST 8060 defined <SoftwareIdentity>/<Meta> items
            coswid.setActivationStatus(parseString(CoswidItems.SOFTWARE_META_INT,
                    CoswidItems.ACTIVATION_STATUS_INT));
            coswid.setColloquialVersion(parseString(CoswidItems.SOFTWARE_META_INT,
                    CoswidItems.COLLOQUIAL_VERSION_INT));
            coswid.setEdition(parseString(CoswidItems.SOFTWARE_META_INT, CoswidItems.EDITION_INT));
            coswid.setProduct(parseString(CoswidItems.SOFTWARE_META_INT, CoswidItems.PRODUCT_INT));
            coswid.setRevision(parseString(CoswidItems.SOFTWARE_META_INT, CoswidItems.REVISION_INT));
            // software-meta-entry map: other CoSWID Meta attributes currently processed by this application
            coswid.setDescription(parseString(CoswidItems.SOFTWARE_META_INT, CoswidItems.DESCRIPTION_INT));
            coswid.setPersistentId(parseString(CoswidItems.SOFTWARE_META_INT, CoswidItems.PERSISTENT_ID_INT));
            coswid.setProductFamily(parseString(CoswidItems.SOFTWARE_META_INT,
                    CoswidItems.PRODUCT_FAMILY_INT));
            coswid.setSummary(parseString(CoswidItems.SOFTWARE_META_INT, CoswidItems.SUMMARY_INT));
            coswid.setNonpayloadPrintOneline(nonpayloadParsedDataOneline);
            coswid.setNonpayloadPrintPretty(nonpayloadParsedDataPretty);

            // payload map
            // payload is a complex structure with a variable number of entries
            payloadParsedDataOneline += " payload => ";
            payloadParsedDataPretty += "payload :\n";
            coswid.setPayloadNode(rootNode.path(Integer.toString(CoswidItems.PAYLOAD_INT)));
            processPayload(coswid.getPayloadNode());
            coswid.setPayloadPrintOneline(payloadParsedDataOneline);
            coswid.setPayloadPrintPretty(payloadParsedDataPretty);

        } catch (IOException e) {
            throw new RuntimeException("Error parsing CoSWID data: " + e.getMessage());
        }
    }

    /**
     * Processes a role as published in rfc 9393 Section 2.6. "The entity-entry Map".
     * This lookup is specific for roles defined in section 2.6.
     *
     * @param node JsonNode that holds the array value
     */
    private void processRole(final JsonNode node) {

        if (node != null) {
            nonpayloadParsedDataOneline += " role(s) => ";
            nonpayloadParsedDataPretty += "         role(s):\n";
            String value = "";
            int key = 0;
            int i = 0;

            if (node.isArray()) {
                for (i = 0; i < node.size(); i++) {
                    key = node.get(i).asInt();
                    value = roleLookup(key);
                    coswid.roleCoswid.add(value);
                    nonpayloadParsedDataOneline += " " + value;
                    nonpayloadParsedDataPretty += "            " + value + "\n";
                }
            } else {
                key = node.asInt();
                value = roleLookup(key);
                coswid.roleCoswid.add(value);
                // THIS SYNTAX
                nonpayloadParsedDataOneline += " " + value;
                nonpayloadParsedDataPretty += "            " + value + "\n";
            }
        }
    }

    /**
     * Uses the indexes to convert the integer key into the text values published in
     * rfc 9393 Section 2.6. "The entity-entry Map".
     * This lookup is specific for roles defined in section 2.6.
     *
     * @param index The index value defined in RFC-9393 for roles
     * @return String holding the human-readable role
     */
    private String roleLookup(final int index) {
        return switch (index) {
            case 1 -> "tag-creator";
            case 2 -> "software-creator";
            case 3 -> "aggregator";
            case 4 -> "distributor";
            case 5 -> "licensor";
            case 6 -> "maintainer";
            default -> "unknown role";
        };
    }

    /**
     * Process the payload map.
     *
     * @param node JsonNode that holds the payload node
     */
    private void processPayload(final JsonNode node) {

        String index = "";
        int key = 0;
        int i = 0;
        Set<Map.Entry<String, JsonNode>> fields = node.properties();

        for (Map.Entry<String, JsonNode> field : fields) {
            key = Integer.parseInt(field.getKey());
            index = CoswidItems.getItemName(key);

            if (index.compareTo("directory") == 0) {
                // if there is an array of directory objects, iterate through each directory
                // else if there is only 1 directory object, iterate through that one directory
                if (field.getValue().isArray()) {
                    for (i = 0; i < node.size(); i++) {
                        processPayloadDirectory(field.getValue().get(i));
                    }
                } else {
                    processPayloadDirectory(field.getValue());
                }
            } else if (index.compareTo("file") == 0) {
                // if there is an array of file objects, iterate through each file
                // else if there is only 1 file object, iterate through the fields of that file object
                if (field.getValue().isArray()) {
                    for (i = 0; i < field.getValue().size(); i++) {
                        processPayloadFile(field.getValue().get(i));
                    }
                } else {
                    processPayloadFile(field.getValue());
                }
            } else if (index.compareTo("size") == 0) {
                payloadParsedDataOneline += " " + index + " => " + field.getValue().asText();
                payloadParsedDataPretty += "         " + index + " : " + field.getValue().asText() + "\n";
            } else {
                payloadParsedDataOneline += " " + index + " => " + field.getValue();
                payloadParsedDataPretty += "         " + index + " : " + field.getValue() + "\n";
            }
        }
    }

    /**
     * Process a file from a Json array.
     *
     * @param directoryNode JsonNode that holds the file node
     */
    private void processPayloadDirectory(final JsonNode directoryNode) {

        payloadParsedDataOneline += " directory" + " => ";
        payloadParsedDataPretty += "     directory" + " : \n";

        // temporarily peek ahead to grab the directory name and add it to the current path
        Set<Map.Entry<String, JsonNode>> properties = directoryNode.properties();
        if (!properties.isEmpty()) {
            Map.Entry<String, JsonNode> firstEntry = properties.iterator().next();
            if (Integer.parseInt(firstEntry.getKey()) == FS_NAME_INT) {
                path.add(firstEntry.getValue().asText());
            } else {
                path.add("Unknown_directory_name");
            }
        } else {
            path.add("Unknown_directory_name");
        }

        // recursively process everything under this directory
        processPayload(directoryNode);

        // when leaving the directory, remove its name from the current path
        path.removeLast();
    }

    /**
     * Process a file from a Json array.
     *
     * @param fileNode JsonNode that holds the file node
     */
    private void processPayloadFile(final JsonNode fileNode) {

        payloadParsedDataOneline += " file => ";
        payloadParsedDataPretty += "     file : \n";
        String index = "";
        String filepath = "";
        int key = 0;
        Set<Map.Entry<String, JsonNode>> fields = fileNode.properties();
        Measurement measurement = new Measurement();

        // process the measurement type
        measurement.setMeasurementType(MeasurementType.UNKNOWN);

        // iterate through each item belonging to this file (name, size, hash, etc)

        for (Map.Entry<String, JsonNode> field : fields) {
            key = Integer.parseInt(field.getKey());
            index = CoswidItems.getItemName(key);
            measurement.setRevision(coswid.getRevision());

            if (index.compareTo(CoswidItems.HASH_STR) == 0) {
                Object[] hashInfo = processPayloadHash(field.getValue());
                measurement.setMeasurementBytes((byte[]) hashInfo[1]);
                measurement.setAlg(IanaHashAlg.getAlgFromId((Integer) hashInfo[0]));
            } else {
                switch (index) {
                    case CoswidItems.FS_NAME_STR:
                        for (String eachDirectory : path) {
                            filepath += eachDirectory + "\\";
                        }
                        filepath += field.getValue().asText();
                        measurement.setAdditionalMetadata("    File path: " + filepath + "\n");
                        break;
                    case CoswidItems.SIZE_STR, CoswidItems.FILE_VERSION_STR:
                        break;
                    default:
                        throw new RuntimeException("Error processing Coswid data: "
                                + "Unknown Payload item (" + index + ")");
                }
                payloadParsedDataOneline += " " + index + " => " + field.getValue();
                payloadParsedDataPretty += "         " + index + " : " + field.getValue() + "\n";
            }
        }
        coswid.measurements.add(measurement);
    }

    /**
     * Processes a hash-entry as published in rfc 9393 Section 2.9.1 "The hash-entry array".
     *
     * @param node JsonNode that holds the array value
     * @return returns a byte array with the measurement hash
     */
    private Object[] processPayloadHash(final JsonNode node) {

        HexFormat hexTool = HexFormat.of();

        String hash = "";
        String algStr = "";
        int algInt = 0;
        byte[] hashBytes = null;

        if (node.isArray()) {
            algStr = node.get(0).asText();
            try {
                hashBytes = node.get(1).binaryValue();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        hash = hexTool.formatHex(hashBytes);
        algInt = Integer.parseInt(algStr);
        algStr = IanaHashAlg.getAlgFromId(algInt).getAlgName();

        payloadParsedDataOneline += "Hash algorithm => " + algStr + " Hash => " + hash;
        payloadParsedDataPretty += "         Hash algorithm : " + algStr
                + "\n         Hash => " + hash + "\n";

        return new Object[] {algInt, hashBytes};
    }

    /**
     * During initial parsing of the CoSWID, builds a string that contains the processed CoSWID data.
     *
     * @param key  integer value that corresponds to the key in the key/value pair of the CoSWID map
     * @param item item that corresponds to the value in the key/value pair of the CoSWID map
     */
    private void buildString(final int key, final String item) {
        String index = CoswidItems.getItemName(key);
        if (!item.isEmpty()) {
            nonpayloadParsedDataOneline += " " + index + " => " + item;
            nonpayloadParsedDataPretty += "         " + index + " : " + item + "\n";
        }
    }

    /**
     * Checks the payload for a valid tag.
     * by parsing the first byte of the payload as a tag
     * and checking for one of the supported tags by this application.
     * If a supported tag is found the payload and coswidTag references are adjusted.
     *
     * @param coswidData byte array holding the raw coswid data to process
     * @return true if a valid tag is found
     */
    private byte[] checkForTag(final byte[] coswidData) {
        CborTagProcessor tmpTag = new CborTagProcessor(coswidData);
        if (tmpTag.isTagged()) {
            return (tmpTag.getContent());
        } else {
            return (coswidData);
        }
    }

    /**
     * Parses an item from the coswid data referenced by the Coswid item number.
     * Also updates the toString() data.
     *
     * @param map        Cowid defined item number
     * @param coswidItem coswid item defined by rfc 9393
     * @return String obtained from the coswid data
     */
    private String parseString(final int map, final int coswidItem) {
        String itemString = Integer.toString(coswidItem);
        String mapString = Integer.toString(map);
        String referenceVal = "";
        if (map == COSWID_MAP) {
            referenceVal = rootNode.path(itemString).asText();
        } else {
            referenceVal = rootNode.path(mapString).path(itemString).asText();
        }
        buildString(coswidItem, referenceVal);
        return referenceVal;
    }

    /**
     * Parses a boolean from the coswid data referenced by the Coswid item number.
     * Also updates the toString() data.
     *
     * @param map        Cowid defined item number
     * @param coswidItem coswid item defined by rfc 9393
     * @return boolean obtained from the coswid data
     */
    private boolean parseBoolean(final int map, final int coswidItem) {
        String itemString = Integer.toString(coswidItem);
        String mapString = Integer.toString(map);
        boolean referenceVal = false;
        if (map == COSWID_MAP) {
            referenceVal = rootNode.path(itemString).asBoolean(false);
        } else {
            referenceVal = rootNode.path(mapString).path(itemString).asBoolean(false);
        }
        buildString(coswidItem, Boolean.toString(referenceVal));
        return referenceVal;
    }

    /**
     * Default toString that contains all key/value pairs in the CoSWID data with no line breaks.
     *
     * @return the CoSWID data in a string with no line breaks
     */
    public String toString() {
        return toString("none");
    }

    /**
     * Prints the processed CoSWID data that was stored when initially parsed.
     *
     * @param format options: "pretty" (default is anything else)
     * @return the CoSWID data in a string
     */
    public String toString(final String format) {
        return coswid.toString(format);
    }
}
