package hirs.utils.rim.unsignedRim;

import hirs.utils.rim.unsignedRim.common.measurement.Measurement;

import java.util.List;

/**
 * Interface for all RIMs.
 * <p>
 * RIM types used by the ACA as well as the rim-tool (rim-tool create, verify and print commands)</p>
 *
 * <p>Signature Types
 * <ul>
 * <li>SIGTYPE_COSE: IETF RFC 9052 defined CBOR Signatures
 *     (https://datatracker.ietf.org/doc/html/rfc9052)</li>
 * <li>SIGTYPE_DSIG: W3C Defined Signatures for XML (https://www.w3.org/TR/xmldsig-core1/)</li>
 * </ul>
 * <p>Unsigned RIM Types used for PC
 * <ul>
 *   <li>RIMTYPE_PCRIM: TCG Defined PC Client RIM which uses SWID</li>
 *   <li>RIMTYPE_COMP_SWID: TCG Component-RIM which uses SWID</li>
 * </ul>
 * <p>Unsigned RIM Types used for PC Components
 * <ul>
 *   <li>RIMTYPE_COSWID: IETF RFC 9393 defined CoSWID (Concise SWID) tags</li>
 *   <li>RIMTYPE_COMP_COSWID: TCG Component-RIM which uses CoSWID</li>
 *   <li>RIMTYPE_CORIM_COMID: IETF CoRIM (Concise RIM) which envelopes a comid</li>
 *   <li>RIMTYPE_CORIM_COSWID: IETF CoRIM which envelopes a CoSWID</li>
 * </ul>
 * </p>
 */
public interface GenericRim {

    // Signature types
    /**
     * Signature type COSE.
     */
    String SIGTYPE_COSE = "cose";
    /**
     * Signature type DSIG.
     */
    String SIGTYPE_DSIG = "dsig";

    // Unsigned RIM Types used for PC
    /**
     * RIM type PC RIM.
     */
    String RIMTYPE_PCRIM = "pcrim";
    /**
     * RIM type CoSWID.
     */
    String RIMTYPE_COMP_SWID = "comp_swid";

    // Unsigned RIM Types used for PC Components
    /**
     * RIM type TCG Comp RIM CoSWID.
     */
    String RIMTYPE_COSWID = "coswid";
    /**
     * RIM type TCG Comp RIM SWID.
     */
    String RIMTYPE_COMP_COSWID = "comp_coswid";
    /**
     * RIM type CORIM-COMID.
     */
    String RIMTYPE_CORIM_COMID = "corim_comid";
    /**
     * RIM type CORIM-CoSWID.
     */
    String RIMTYPE_CORIM_COSWID = "corim_coswid";

    /**
     * Human-readable string listing RIM types available.
     */
    String RIMTYPES_AVAILABLE = RIMTYPE_PCRIM + " " + RIMTYPE_COSWID + " " + RIMTYPE_COMP_SWID
            + " " + RIMTYPE_COMP_COSWID + " " + RIMTYPE_CORIM_COMID + " " + RIMTYPE_CORIM_COSWID + ".";

    /**
     * Returns the signature type options.
     *
     * @return the signature type options
     */
    static String getValidSigTypes() {
        return SIGTYPE_COSE + " " + SIGTYPE_DSIG + ".";
    }

    /**
     * Returns signature type of RIM.
     *
     * @param rimType the RIM type
     * @return the signature type
     */
    static String getSigType(final String rimType) {
        return switch (rimType) {
            case GenericRim.RIMTYPE_COSWID,
                 GenericRim.RIMTYPE_COMP_COSWID,
                 GenericRim.RIMTYPE_CORIM_COMID,
                 GenericRim.RIMTYPE_CORIM_COSWID -> GenericRim.SIGTYPE_COSE;
            case GenericRim.RIMTYPE_PCRIM,
                 GenericRim.RIMTYPE_COMP_SWID -> GenericRim.SIGTYPE_DSIG;
            default -> "";
        };
    }

    /**
     * Returns a unique identifier String describing the type of RIM.
     *
     * @return the RIM type
     */
    String getRimType();

    /**
     * Returns a unique identifier String (Manufacturer+Model in most cases)
     * or perhaps hash of a string to use as a DB lookup value for the RIMs Digests and the RIM itself.
     *
     * @return the Rim ID
     */
    String getRimID();

    /**
     * Retrieves the Signer info for the RIM.
     *
     * @return String representing the SKID of the RIM Signer
     */
    String getSignerId();

    /**
     * Runs checks on the RIM to check validity.
     * Should include signature checks, content checks, and formatting checks
     * Requires a cert chain to verify the RIMs signature
     * SignerId would provide the reference for the ACA to look up the certs
     *
     * @return true if valid, false if not
     */
    boolean isValid();

    /**
     * Returns a list of Measurement objects for given RIM identifier that were found in payload (if any).
     *
     * @return list of reference measurements
     */
    List<Measurement> getReferenceMeasurements();

    /**
     * ReferencedRims is a list of RimId references found in the payload (if any).
     *
     * @return the string of RIMId references
     */
    String getReferencedRims();

    /**
     * Produces an object specific string with info about the object.
     *
     * @return the human-readable string
     */
    String toString();
}
