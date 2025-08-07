package hirs.utils.rim.unsignedRim.cbor.ietfCoswid;

import com.fasterxml.jackson.databind.JsonNode;
import hirs.utils.rim.unsignedRim.GenericRim;
import hirs.utils.rim.unsignedRim.common.measurement.Measurement;
import hirs.utils.rim.unsignedRim.common.measurement.MeasurementType;
import hirs.utils.rim.unsignedRim.xml.Swid;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that holds CoSWID (rfc 9393) Defined attributes, attribute names, and assigned indexes.
 * All variable names match those defined in rfc 9393.
 */
@Setter @Getter
public class Coswid extends Swid implements GenericRim {
    /** Reference to the primary CoSWID JsonNode object. */
    protected JsonNode rootNode = null;
    /** Reference to a Payload JsonNode object. */
    protected JsonNode payloadNode = null;

    // CoSWID defined attributes (not Sets or Arrays) found in rfc 9393
    // ------------------------------------

    // concise-swid-tag map
    protected String tagId = null;
    protected String softwareName = null;
    protected boolean corpus = false;
    protected boolean patch = false;
    protected String media = null;
    protected boolean supplemental = false;
    protected String tagVersion = null;
    protected String softwareVersion = null;
    protected String softwareScheme = null; // versionScheme

    // global-attributes group
    protected String lang = null;

    // resource-collection group
    // (reserved for future values)

    // entity-entry map
    protected String entityName = null;
    protected String regId = null;
    protected List<String> roleCoswid = new ArrayList<>();
    protected String thumbprint = null;

    // evidence-entry map
    protected String date = null;
    protected String deviceId = null;

    // link-entry map
    protected String ownership = null;
    protected String mediaType = null;
    protected String use = null;

    // software-meta-entry map
    protected String activationStatus = null;
    protected String channelType = null;
    protected String colloquialVersion = null;
    protected String description = null;
    protected String edition = null;
    protected String entitlementDataRequired = null;
    protected String entitlementKey = null;
    protected String generator = null;
    protected String persistentId = null;
    protected String productFamily = null;
    protected String revision = null;
    protected String summary = null;
    protected String unspscCode = null;
    protected String unspscVersion = null;

    // End CoSWID defined attributes
    // -----------------------------------------------------------------------

    // Payload (including measurement) data
    protected String nonpayloadPrintOneline = null;
    protected String nonpayloadPrintPretty = null;

    // Payload (including measurement) data
    protected String payloadPrintOneline = null;
    protected String payloadPrintPretty = null;
    protected MeasurementType measurementType = MeasurementType.UNKNOWN;
    // List of hash measurements in this CoSWID and their associated data
    protected List<Measurement> measurements = new ArrayList<>();

    // @Setter(AccessLevel.NONE)

    /** IANA CBOR registry define Coswid Tag.*/
    @Setter
    public static int coswidTag = 1398229316;

    /**
     * Returns a unique identifier String describing the type of RIM.
     * @return the RIM type
     */
    public String getRimType() {
        return GenericRim.RIMTYPE_COSWID;
    };

    /**
     * Returns a unique identifier String (Manufacturer+Model in most cases)
     * or perhaps hash of a string to use as a DB lookup value for the RIMs Digests and the RIM itself.
     * @return the Rim ID
     */
    public String getRimID() {
        return ""; // TBD
    };

    /**
     * Retrieves the Signer info for the RIM.
     * @return String representing the SKID of the RIM Signer
     */
    public String getSignerId() {
        // signer ID does not apply to unsigned CoSWID
        return "";
    };

    /**
     * Runs checks on the rim to check validity
     * Should include signature checks, content checks, and formatting checks.
     * Requires a cert chain to verify the RIMs signature.
     * SignerId would provide the reference for the ACA to look up the certs
     * @return true if valid, false if not
     */
    public boolean isValid() {
        return false; // TODO
    };

    /**
     * Returns a list of Measurement objects for the given rim identifier that were found in the payload.
     * @return List<Measurement> that holds the reference measurements
     */
    public List<Measurement> getReferenceMeasurements() {
        return measurements;
    };

    /**
     * ReferencedRims is a list of RimId's references found in the payload (if any).
     * @return  String contianing a list of reference RIMs.
     */
    public String getReferencedRims() {
        return ""; // TODO
    };

    /**
     * Default toString that contains all key/value pairs in the CoSWID data with no line breaks.
     * @return Human-readable form of the measurement
     */
    public String measurementsToString() {
        String measurementData = "Measurements:\n";

        int count = 0;
        for (Measurement measurement : measurements) {
            count++;
            measurementData += "Measurement #" + count + ":\n";
            measurementData += measurement;
        }
        return measurementData;
    }

    /**
     * Default toString that contains all key/value pairs in the CoSWID data with no line breaks.
     * @return Human-readable form of this coswid objec
     */
    public String toString() {
        return toString("none");
    }

    /**
     * Prints the processed CoSWID data that was stored when initially parsed.
     * @param format options: "pretty" (default is anything else)
     * @return Human-readable form of this coswid object
     */
    public String toString(final String format) {
        String coswidData = "";
        if (format.compareTo("pretty") == 0) {
            coswidData += nonpayloadPrintPretty;
            coswidData += payloadPrintPretty;
        } else {
            coswidData += nonpayloadPrintOneline;
            coswidData += payloadPrintOneline;
        }
        return coswidData;
    }
}
