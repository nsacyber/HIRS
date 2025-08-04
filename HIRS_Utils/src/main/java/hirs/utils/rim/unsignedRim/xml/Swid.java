package hirs.utils.rim.unsignedRim.xml;

import lombok.Getter;
import lombok.Setter;

/**
 * Class that contains definitions found in the SWID specification  ISO/IEC 19770-2:2015.
 * Used by Coswid (Rfc 9393) which is based upon SWID specification.
 */
@SuppressWarnings({"VisibilityModifier", "JavadocVariable"})
public class Swid {
    // Order of variables follows the order listed in the table in section 8.5.1 of the SWID spec

    // Attributes are listed after the elements
    // SWID Elements are listed first
    public static final String SWID_SOFTWARE_IDENTITY_STR = "SoftwareIdentity";
    // Software Identity Element Attributes
    /** Flag set to true if tag is a patch tag which indicates this tag applies to pre-installation data. */
    @Setter
    @Getter
    protected boolean corpus = false;
    public static final String SWID_CORPUS_STR = "corpus";
    /** Flag set to true if tag is a patch tag that implies modification to the software. */
    @Setter
    @Getter
    protected boolean patch = false;
    public static final String SWID_PATCH_STR = "patch";
    /** String that desribes the "Platform" this software applies to. */
    @Setter
    @Getter
    protected String swidMedia = null;
    public static final String SWID_MEDIA_STR = "media";
    /** String that provides the software component name. */
    @Setter
    @Getter
    protected String softwareName = null;
    public static final String SWID_NAME_STR = "name";
    /** Flag set to true if tag is a Supplemental tag which is generally provided by different entities. */
    @Setter
    @Getter
    protected boolean supplemental = false;
    public static final String SWID_SUPPLEMENTAL_STR = "supplemental";
    /** Tag Identifier - usually a UUID. */
    @Setter
    @Getter
    protected String swidTagId = null;
    public static final String SWID_TAG_ID_STR = "tagId";
    /** Version of the tag. */
    @Setter
    @Getter
    protected String tagVersion = null;
    public static final String SWID_TAG_VERSION_STR = "tagVersion";
    /** Swid spec version. */
    @Setter
    @Getter
    protected String softwareVersion = null;
    public static final String SWID_VERSION_STR = "version";
    /** Swid spec version. */
    @Setter
    @Getter
    protected String swidVersionScheme = null;
    public static final String SWID_VERSION_SCHEME_STR = "versionScheme";

    /** Entity Element. */
    public static final String SWID_ENTITY_STR = "Entity";
    /** Name of the Entity that created this tag. */
    @Setter
    @Getter
    protected String swidEntityName = null;
    public static final String SWID_ENTITY_NAME_STR = "name";
    /** IANA ID of the Entity that created this tag. */
    @Setter
    @Getter
    protected String regId = null;
    public static final String SWID_ENTITY_REGID_STR = "regid";
    /** Role of the entity had in creating this tag. */
    @Setter
    @Getter
    protected String role = null;
    public static final String SWID_ENTITY_ROL_STR = "role";
    /** hash of the cert used to sign this tag. */
    @Setter
    @Getter
    protected String thumbprint = null;
    public static final String SWID_ENTITY_THUMBPRINT_STR = "thumbprint";

    // Evidence Element
    /** Evidence is a scan of the system where software which does not have a swid tag is discovered. */
    public static final String SWID_EVIDENCE_STR = "Evidence";
    /** Date and time the evidence was collected. */
    @Setter
    @Getter
    protected String swidEvidenceDate = null;
    public static final String SWID_EVIDENCE_DATE_STR = "date";
    /** Identifier of the device the evidence was collected from. */
    @Setter
    @Getter
    protected String swidEvidenceDeviceId = null;
    public static final String SWID_EVIDENCE_DEVICE_ID_STR = "deviceId";

    // Link Element
    /** Link is a reference to any other item. */
    public static final String SWID_LINK_STR = "Link";
    /** Canonical name for the item being referenced. */
    @Setter
    @Getter
    protected String swidLinkArtifact = null;
    public static final String SWID_LINK_ARTIFACT_STR = "artifact";
    /** Link to the item being referenced. */
    @Setter
    @Getter
    protected String href = null;
    public static final String SWID_LINK_HREF_STR = "href";
    /** String that describes the "Platform" this software applies to. */
    @Setter
    @Getter
    protected String swidLinkMedia = null;
    public static final String SWID_LINK_MEDIA_STR = "media";
    /** String that describes the "Strength of ownership" of the target piece of software. */
    @Setter
    @Getter
    protected String swidLinkOwnership = null;
    public static final String SWID_LINK_OWNERSHIP_STR = "ownership";
    /** String that describes the "relationship" betwen the tag abd the target software. */
    @Setter
    @Getter
    protected String rel = null;
    public static final String SWID_LINK_REL_STR = "rel";
    /** String type of media the device the link refers to. */
    @Setter
    @Getter
    protected String swidLinkType = null;
    public static final String SWID_LINK_TYPE_STR = "type";
    /** Determines if the target is a hard requirement.  */
    @Setter
    @Getter
    protected String swidLinkUse = null;
    public static final String SWID_LINK_USE_STR = "use";

    // Meta Element
    public static final String SWID_META_STR = "Meta";

    // Payload Element
    public static final String SWID_PAYLOAD_STR = "Payload";
    /** Directory where the payload is located. */
    @Setter
    @Getter
    protected String swidPayloadDirectory = null;
    public static final String SWID_PAYLOAD_DIR_STR = "directory";
    /** Flag to denote the importance of the directory. */
    @Setter
    @Getter
    private boolean swidPayloadDirectoryKey = false;
    public static final String SWID_PAYLOAD_DIR_KEY_STR = "payloadDirKey";
    /** location of the directory. */
    @Setter
    @Getter
    protected String swidPayloadDirectoryLocation = null;
    public static final String SWID_PAYLOAD_DIR_LOC_STR = "location";
    /** name of the directory. */
    @Setter
    @Getter
    protected String swidPayloadDirectoryName = null;
    public static final String SWID_PAYLOAD_DIR_NAME_STR = "directoryName";
    /** Root directory the directory os relative to. */
    @Setter
    @Getter
    protected String swidPayloadDirectoryRoot = null;
    public static final String SWID_PAYLOAD_DIR_ROOT_STR = "rootDirectory";
    /** file the payload refers to. */
    @Setter
    @Getter
    protected String swidPayloadFile = null;
    public static final String SWID_PAYLOAD_FILE_STR = "file";
    /** name of the file the payload refers to. */
    @Setter
    @Getter
    protected String swidPayloadFileName = null;
    public static final String SWID_PAYLOAD_FILE_NAME = "fileName";
    /** size the payload refers to. */
    @Setter
    @Getter
    protected int swidPayloadFileSize = 0;
    public static final String SWID_PAYLOAD_FILE_SIZE = "size";
    /** version of the file the payload refers to. */
    @Setter
    @Getter
    protected String swidPayloadFileVersion = null;
    public static final String SWID_PAYLOAD_FILE_VER_STR = "file-version";

    // Process
    /** OS Process information. */
    public static final String SWID_PROCESS_STR = "process";
    /** Process id string (name). */
    @Setter
    @Getter
    protected String swidProcessName = null;
    public static final String SWID_PROCESS_NAME_STR = "process";
    /** Process id int (name). */
    @Setter
    @Getter
    protected int swidProcessPid = 0;
    public static final String SWID_PROCESS_PID_STR = "processId";

    // Resource
    /** OS Process information. */
    public static final String SWID_RESOURCE_STR = "resource";
    /** Genric description of the resource (name). */
    @Setter
    @Getter
    protected String swidResourceType = null;
    public static final String SWID_RESOURCE_TYPE_STR = "resourceType";

    // NIST IR 8060 defined Meta fields used by Coswid and TCG PC Client RIM
    /** Version defined by NIST IR 8060. */
    @Setter
    @Getter
    protected String colloquialVersion = null;
    public static final String SWID_META_COL_VER_STR = "colloquialVersion";
    /** Product defined by NIST IR 8060. */
    @Setter
    @Getter
    protected String product = null;
    public static final String SWID_META_COL_PRODUCT_STR = "product";
    /** Revision defined by NIST IR 8060. */
    @Setter
    @Getter
    protected String revision = null;
    public static final String SWID_META_REV_STR = "revision";
    /** Edition defined by NIST IR 8060. */
    @Setter
    @Getter
    protected String edition = null;
    public static final String SWID_META_EDITION_STR = "edition";
}
