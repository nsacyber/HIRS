package hirs.utils.rim.unsignedRim.xml;

import lombok.Getter;
import lombok.Setter;

/**
 * Class that contains definitions found in the SWID specification  ISO/IEC 19770-2:2015.
 * Used by Coswid (Rfc 9393) which is based upon SWID specification.
 */
@Setter
@Getter
public class Swid {
    // Order of variables follows the order listed in the table in section 8.5.1 of the SWID spec

    // Attributes are listed after the elements
    // SWID Elements are listed first
    public static final String SWID_SOFTWARE_IDENTITY_STR = "SoftwareIdentity";
    // Software Identity Element Attributes
    public static final String SWID_CORPUS_STR = "corpus";
    public static final String SWID_PATCH_STR = "patch";
    public static final String SWID_MEDIA_STR = "media";
    public static final String SWID_NAME_STR = "name";
    public static final String SWID_SUPPLEMENTAL_STR = "supplemental";
    public static final String SWID_TAG_ID_STR = "tagId";
    public static final String SWID_TAG_VERSION_STR = "tagVersion";
    public static final String SWID_VERSION_STR = "version";
    public static final String SWID_VERSION_SCHEME_STR = "versionScheme";
    /**
     * Entity Element.
     */
    public static final String SWID_ENTITY_STR = "Entity";
    public static final String SWID_ENTITY_NAME_STR = "name";
    public static final String SWID_ENTITY_REGID_STR = "regid";
    public static final String SWID_ENTITY_ROL_STR = "role";
    public static final String SWID_ENTITY_THUMBPRINT_STR = "thumbprint";
    /**
     * Evidence is a scan of the system where software which does not have a swid tag is discovered.
     */
    public static final String SWID_EVIDENCE_STR = "Evidence";
    public static final String SWID_EVIDENCE_DATE_STR = "date";
    public static final String SWID_EVIDENCE_DEVICE_ID_STR = "deviceId";
    /**
     * Link is a reference to any other item.
     */
    public static final String SWID_LINK_STR = "Link";
    public static final String SWID_LINK_ARTIFACT_STR = "artifact";
    public static final String SWID_LINK_HREF_STR = "href";
    public static final String SWID_LINK_MEDIA_STR = "media";
    public static final String SWID_LINK_OWNERSHIP_STR = "ownership";
    public static final String SWID_LINK_REL_STR = "rel";
    public static final String SWID_LINK_TYPE_STR = "type";
    public static final String SWID_LINK_USE_STR = "use";
    // Meta Element
    public static final String SWID_META_STR = "Meta";
    // Payload Element
    public static final String SWID_PAYLOAD_STR = "Payload";

    // Evidence Element
    public static final String SWID_PAYLOAD_DIR_STR = "directory";
    public static final String SWID_PAYLOAD_DIR_KEY_STR = "payloadDirKey";
    public static final String SWID_PAYLOAD_DIR_LOC_STR = "location";
    public static final String SWID_PAYLOAD_DIR_NAME_STR = "directoryName";
    public static final String SWID_PAYLOAD_DIR_ROOT_STR = "rootDirectory";

    // Link Element
    public static final String SWID_PAYLOAD_FILE_STR = "file";
    public static final String SWID_PAYLOAD_FILE_NAME = "fileName";
    public static final String SWID_PAYLOAD_FILE_SIZE = "size";
    public static final String SWID_PAYLOAD_FILE_VER_STR = "file-version";
    /**
     * OS Process information.
     */
    public static final String SWID_PROCESS_STR = "process";
    public static final String SWID_PROCESS_NAME_STR = "process";
    public static final String SWID_PROCESS_PID_STR = "processId";
    /**
     * OS Process information.
     */
    public static final String SWID_RESOURCE_STR = "resource";
    public static final String SWID_RESOURCE_TYPE_STR = "resourceType";
    public static final String SWID_META_COL_VER_STR = "colloquialVersion";
    public static final String SWID_META_COL_PRODUCT_STR = "product";
    public static final String SWID_META_REV_STR = "revision";
    public static final String SWID_META_EDITION_STR = "edition";
    /**
     * Flag set to true if tag is a patch tag which indicates this tag applies to pre-installation data.
     */
    protected boolean corpus = false;
    /**
     * Flag set to true if tag is a patch tag that implies modification to the software.
     */
    protected boolean patch = false;
    /**
     * String that desribes the "Platform" this software applies to.
     */
    protected String swidMedia = null;
    /**
     * String that provides the software component name.
     */
    protected String softwareName = null;
    /**
     * Flag set to true if tag is a Supplemental tag which is generally provided by different entities.
     */
    protected boolean supplemental = false;
    /**
     * Tag Identifier - usually a UUID.
     */
    protected String swidTagId = null;
    /**
     * Version of the tag.
     */
    protected String tagVersion = null;
    /**
     * Swid spec version.
     */
    protected String softwareVersion = null;
    /**
     * Swid spec version.
     */
    protected String swidVersionScheme = null;
    /**
     * Name of the Entity that created this tag.
     */
    protected String swidEntityName = null;
    /**
     * IANA ID of the Entity that created this tag.
     */
    protected String regId = null;
    /**
     * Role of the entity had in creating this tag.
     */
    protected String role = null;
    /**
     * hash of the cert used to sign this tag.
     */
    protected String thumbprint = null;
    /**
     * Date and time the evidence was collected.
     */
    protected String swidEvidenceDate = null;
    /**
     * Identifier of the device the evidence was collected from.
     */
    protected String swidEvidenceDeviceId = null;
    /**
     * Canonical name for the item being referenced.
     */
    protected String swidLinkArtifact = null;
    /**
     * Link to the item being referenced.
     */
    protected String href = null;
    /**
     * String that describes the "Platform" this software applies to.
     */
    protected String swidLinkMedia = null;
    /**
     * String that describes the "Strength of ownership" of the target piece of software.
     */
    protected String swidLinkOwnership = null;
    /**
     * String that describes the "relationship" betwen the tag abd the target software.
     */
    protected String rel = null;
    /**
     * String type of media the device the link refers to.
     */
    protected String swidLinkType = null;
    /**
     * Determines if the target is a hard requirement.
     */
    protected String swidLinkUse = null;

    // Process
    /**
     * Directory where the payload is located.
     */
    protected String swidPayloadDirectory = null;
    /**
     * location of the directory.
     */
    protected String swidPayloadDirectoryLocation = null;
    /**
     * name of the directory.
     */
    protected String swidPayloadDirectoryName = null;
    /**
     * Root directory the directory os relative to.
     */
    protected String swidPayloadDirectoryRoot = null;
    /**
     * file the payload refers to.
     */
    protected String swidPayloadFile = null;

    // Resource
    /**
     * name of the file the payload refers to.
     */
    protected String swidPayloadFileName = null;
    /**
     * size the payload refers to.
     */
    protected int swidPayloadFileSize = 0;
    /**
     * version of the file the payload refers to.
     */
    protected String swidPayloadFileVersion = null;
    // NIST IR 8060 defined Meta fields used by Coswid and TCG PC Client RIM
    /**
     * Process id string (name).
     */
    protected String swidProcessName = null;
    /**
     * Process id int (name).
     */
    protected int swidProcessPid = 0;
    /**
     * Genric description of the resource (name).
     */
    protected String swidResourceType = null;
    /**
     * Version defined by NIST IR 8060.
     */
    protected String colloquialVersion = null;
    /**
     * Product defined by NIST IR 8060.
     */
    protected String product = null;
    /**
     * Revision defined by NIST IR 8060.
     */
    protected String revision = null;
    /**
     * Edition defined by NIST IR 8060.
     */
    protected String edition = null;
    /**
     * Flag to denote the importance of the directory.
     */
    private boolean swidPayloadDirectoryKey = false;
}
