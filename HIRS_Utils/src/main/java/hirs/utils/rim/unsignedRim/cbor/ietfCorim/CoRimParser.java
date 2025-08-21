package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import static java.lang.Long.valueOf;

import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import com.authlete.cbor.CBORDecoder;
import com.authlete.cbor.CBORItem;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import hirs.utils.signature.cose.Cbor.CborTagProcessor;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.Comid;
import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.Coswid;
import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.CoswidParser;
import hirs.utils.rim.unsignedRim.common.IanaHashAlg;
import hirs.utils.rim.unsignedRim.common.measurement.Measurement;
import hirs.utils.rim.unsignedRim.common.measurement.MeasurementType;

/**
 * Class that parses a Cbor encoded CoRim object. CoRims are defined by IETF. Current version of spec is:
 * https://datatracker.ietf.org/doc/draft-ietf-rats-corim/
 * <p>
 * Note that when document becomes an approved specification, a rfc number will be assigned
 * and this link will change.
 */
public class CoRimParser extends CoRim {
    protected List<Comid> comidList = new ArrayList<>();
    protected List<Coswid> coswidList = new ArrayList<Coswid>();
    List<Object[]> dependentRims = new ArrayList<>();
    /** Contains a list of measurements pertaining to various objects within the CoRIM (CoMID, CoSWID, etc.).
     * Populated after construction and parsing. */
    List<Measurement> measurements = new ArrayList<>();

    /**
     * Constructor used to parse Cbor Encoded Corim data.
     * @param corimData byte arra holding the cbor encoded corim data
     */
    public CoRimParser(final byte[] corimData) {
        byte[] uriDigest = null;
        String status = "";
        String uri = "";
        int uriDigestAlg = 0;
        final Format format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        try {
            final CBORDecoder cborDecoder = new CBORDecoder(corimData);
            final CBORItem corimObject = cborDecoder.next();
            final LinkedHashMap corimMap = (LinkedHashMap) corimObject.parse();
            setId((String) corimMap.get(CoRimItems.CORIM_ID_TYPE_CHOICE_INT));
            final ArrayList tagTypeChoice = (ArrayList) corimMap
                    .get(CoRimItems.CONCISE_TAG_TYPE_CHOICE_INT);
            // Parse list of concise tags contained within the corim-map
            for (int tagCount = 0; tagCount < tagTypeChoice.size(); tagCount++) {
                status = "Processing CoRim tag";
                final CborTagProcessor ctp = new CborTagProcessor((byte[]) tagTypeChoice.get(tagCount));
                final byte[] corimContent = ctp.getContent();
                if (ctp.isCorim()) {
                    setCorimTag(ctp.getTagId());
                    status = "Process CoRim tagged content";

                    if (ctp.isCoswid()) { // process content as CoSwid
                        final CoswidParser cospar = new CoswidParser(corimContent);
                        coswidList.add(cospar.getCoswid());
                    } else if (ctp.isComid()) { // process content as CoMid
                        comidList.add(new Comid(corimContent));
                    } else if (ctp.isCotl()) { // process content as CoTL, not
                        // supported for now so throw an
                        // exception ...
                        throw new RuntimeException(
                                "Error parsing CoRim data, CoTL data (CBor Tag 508) found within the"
                                        + " CoRIM data is not currently supported");
                    }
                }
            }

            // process corim defined data
            setId((String) corimMap.get(CoRimItems.CORIM_ID_TYPE_CHOICE_INT));
            status = "Process CoRim dependent-rims";
            final ArrayList dependantRims = (ArrayList) corimMap.get(CoRimItems.CORIM_LOCATOR_MAP_INT);
            if (dependantRims != null) {
                final Iterator corimLocators = dependantRims.iterator();
                if (corimLocators != null) {
                    while (corimLocators.hasNext()) {
                        status = "processing CoRim locators";
                        final LinkedHashMap locator = (LinkedHashMap) corimLocators.next();
                        if (locator.get(0) != null) {
                            uri = locator.get(0).toString();
                        }
                        if (locator.get(1) != null) { //
                            final ArrayList thumbprint = (ArrayList) locator.get(1); // sec-common-hash-entry
                            uriDigestAlg = thumbprint.get(0).hashCode();
                            final byte[] digest = (byte[]) thumbprint.get(1);
                            uriDigest = new byte[digest.length];
                            System.arraycopy(digest, 0, uriDigest, 0, digest.length);
                        }
                        if (uri != null) {
                            dependentRims.add(new Object[] {uri, uriDigestAlg, uriDigest});
                        }
                    }
                }
                status = "Processing CoRim profile";
                final Object profileList = corimMap.get(CoRimItems.PROFILE_TYPE_CHOICE_INT);
                if (profileList != null) {
                    if (profileList instanceof String) {
                        setProfile((String) corimMap.get(CoRimItems.PROFILE_TYPE_CHOICE_INT));
                    }
                }
                status = "Process CoRim validity-map";
                final LinkedHashMap validityMap = (LinkedHashMap) corimMap.get(CoRimItems.VALIDITY_MAP_INT);

                if (validityMap.get(0) != null) { // not before
                    final int before = (int) validityMap.get(0);
                    setNotBefore(valueOf(before));
                    final Date date = new Date(notBefore * 1000);
                    setNotBeforeStr(format.format(date));
                }
                if (validityMap.get(1) != null) { // not before
                    final int after = (int) validityMap.get(1);
                    setNotAfter(valueOf(after));
                    final Date date = new Date(notAfter * 1000);
                    setNotAfterStr(format.format(date));
                }
                status = " Processing CoRim entities";
                final ArrayList entities = (ArrayList) corimMap.get(CoRimItems.CORIM_ENTITY_MAP_INT);
                final LinkedHashMap corimEntityMap = (LinkedHashMap) entities.get(0);
                setEntityName(corimEntityMap.get(0).toString());
                if (corimEntityMap.get(1) != null) {
                    setEntityRegId(corimEntityMap.get(1).toString());
                }
                if (corimEntityMap.get(2) != null) {
                    final ArrayList role = (ArrayList) corimEntityMap.get(2);
                    if (role.get(0) != null) {
                        final int roleVal = (int) role.get(0);
                        switch (roleVal) {
                            case 0 -> setEntityRole("tag-creator");
                            case 1 -> setEntityRole("manifest-creator");
                            case 2 -> setEntityRole("manifest-signer");
                            default -> setEntityRole("unknown CoRim role");
                        }
                    } else {
                        setEntityRole("unspecified CoRim role");
                    }
                }
            }
        } catch (final RuntimeException e) {
            throw new RuntimeException("Error parsing CoRim data: " + status + ": " + e.getMessage());
        } catch (final IOException e) {
            throw new RuntimeException("Error parsing CoRim data: " + status + ": " + e.getMessage());
        }

        // Add measurement list
        extractMeasurements();
    }

    /**
     * Extracts a list of measurements from various objects belonging to the CoRIM,
     * including CoSWIDs or CoMIDs.
     */
    private void extractMeasurements() {
        // CoSWIDs
        for (final Coswid cswid : coswidList) {
            measurements.addAll(cswid.getMeasurements());
        }

        // CoMIDs
        for (final Comid cmid : comidList) {
            // Start with reference triples
            // Traverse from the current reference triple -> reference claims
            // list -> mval -> digests
            final var refTripleList = cmid.getTriples().getReferenceTriples();
            for (final var refTriple : refTripleList) {
                final var currRefClaims = refTriple.getRefClaims();
                final var manufacturer = refTriple.getRefEnv().getComidClass().getVendor();
                final var model = refTriple.getRefEnv().getComidClass().getModel();
                final var index = refTriple.getRefEnv().getComidClass().getIndex();

                for (final var refClaims : currRefClaims) {
                    // Get info about version for current measured environment
                    final var envVersion = refClaims.getMval().getVersion();
                    final var serialNum = refClaims.getMval().getSerialNumber();
                    final var currDigests = refClaims.getMval().getDigests();
                    for (final var digest : currDigests) {
                        final Measurement measurement = new Measurement();
                        measurement.setMeasurementType(MeasurementType.UNKNOWN);
                        measurement.setAlg(digest.getAlg());
                        measurement.setMeasurementBytes(digest.getVal());
                        if (manufacturer != null) {
                            measurement.setManufacturer(manufacturer);
                        }
                        if (model != null) {
                            measurement.setModel(model);
                        }
                        if (index != null) {
                            measurement.setIndex(index);
                        }
                        if (envVersion != null) {
                            measurement.setRevision(envVersion.getVersion());
                        }
                        if (serialNum != null) {
                            measurement.setSerialNumber(serialNum);
                        }
                        measurements.add(measurement);
                    }
                }
            }
        }
    }

    /**
     * Provides a human-readable representation of the CoRim object.
     * @return String representing the CoRIm object in human-readable form
     */
    public String toString() {
        final HexFormat hexTool = HexFormat.of();
        final String indent = "    ";
        String returnString = "";
        returnString += indent + "Corim id = " + getId() + "\n";
        returnString += indent + "Corim tag = " + getTagLabel(getCorimTag()) + " (" + getCorimTag() + ") "
                + "\n";
        // Iterate through CoMID list
        final ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        // Add serializer for hexadecimal byte[] printing
        final SimpleModule module = new SimpleModule();
        module.addSerializer(byte[].class, new HexByteArraySerializer());
        mapper.registerModule(module);
        // Enable print features
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        for (int currComid = 0; currComid < comidList.size(); currComid++) {
            returnString += indent + "CoMID at index " + currComid + ":" + "\n";
            try {
                returnString += mapper.writeValueAsString(comidList.get(currComid)).indent(4);
            } catch (final JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        if (!dependentRims.isEmpty()) {
            returnString += indent + "Dependent RIMs (manifests or related files):" + "\n";
            for (final Object[] row : dependentRims) {
                returnString += indent + indent + "Uri = " + row[0] + "\n";
                if (row[1] != null && !Objects.equals(row[1], 0)) {
                    final String hash = hexTool.formatHex((byte[]) row[2]);
                    returnString += indent + indent + "URI digest = " + hash + "\n";
                    final int alg = (int) row[1];
                    final IanaHashAlg algorithm = IanaHashAlg.getAlgFromId(alg);
                    returnString += indent + indent + indent + "URI digest Algorithm = "
                            + algorithm.getAlgName() + "\n";
                }
            }
        } // dependentRims
        // Profile
        if (!profile.isEmpty()) {
            returnString += indent + "Profile is: " + profile + "\n";
        }
        // Validity
        if ((!notBeforeStr.isEmpty()) || (!notAfterStr.isEmpty())) {
            returnString += indent + "Corim Validity:" + "\n";
        }
        if (!notBeforeStr.isEmpty()) {
            returnString += indent + indent + "notBefore: " + notBeforeStr + "\n";
        }
        if (!notAfterStr.isEmpty()) {
            returnString += indent + indent + "notAfter: " + notAfterStr + "\n";
        }
        // Process Entity Map
        if (!entityName.isEmpty()) {
            returnString += indent + "Entity Info: " + "\n";
            returnString += indent + indent + "Entity Name: " + entityName + "\n";
        }
        if (!entityRegId.isEmpty()) {
            returnString += indent + indent + "Entity Registration ID (URI): " + entityRegId + "\n";
        }
        if (!entityRole.isEmpty()) {
            returnString += indent + indent + "Entity Role: " + entityRole + "\n";
        }
        return returnString;
    }

    /**
     * Returns a copy of the measurements list.
     *
     * @return a defensive copy of the measurements list
     */
    public List<Measurement> getMeasurements() {
        return new ArrayList<>(measurements);
    }
}
