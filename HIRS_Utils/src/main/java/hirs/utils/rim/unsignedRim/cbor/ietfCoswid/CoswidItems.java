package hirs.utils.rim.unsignedRim.cbor.ietfCoswid;

/**
 * Class that provides support for table 9 of rfc 9393 (CoSWID Items Initial Registrations).
 * The static fields are intended to be referenced the CoSWID parser and builder.
 */
public class CoswidItems {
    // Constant Index values defined in RFC 9393 Table 9
    // concise-swid-tag map
    /** A 16-byte binary string, or a textual identifier, uniquely referencing a software component. */
    public static final int TAG_ID_INT = 0;
    /** Textual item that provides the software component's namee. */
    public static final int SOFTWARE_NAME_INT = 1;
    /** Provides information about one or more organizations responsible for producing the CoSWID tag. */
    public static final int ENTITY_INT = 2;
    /** Records the results of a software discovery process used to identify untagged software. */
    public static final int EVIDENCE_INT = 3;
    /** Provides a means to establish relationship arcs between the tag and another item. */
    public static final int LINK_INT = 4;
    /** An open-ended map of key/value data pairs. */
    public static final int SOFTWARE_META_INT = 5;
    /** Represents a collection of software artifacts that compose the target software. */
    public static final int PAYLOAD_INT = 6;
    /**  Value that provides a hash of a file. */
    public static final int HASH_INT = 7;           // note: belongs to resource-collection group
    /** A boolean value that indicates if software component is an installable software component. */
    public static final int CORPUS_INT = 8;
    /** Indicates that the software component is an incremental change installed on an endpoint. */
    public static final int PATCH_INT = 9;
    /** Media. */
    public static final int MEDIA_INT = 10;
    /** A boolean value that indicates if the tag is associated with another referenced SWID or CoSWID tag. */
    public static final int SUPPLEMENTAL_INT = 11;
    /** An integer value that indicates the specific release revision of the tag. */
    public static final int TAG_VERSION_INT = 12;
    /** A textual value representing the specific release or development version of the software component. */
    public static final int SOFTWARE_VERSION_INT = 13;
    /**  An integer or textual value representing the versioning scheme used for the software-version item. */
    public static final int VERSION_SCHEME_INT = 14;
    // global-attributes group
    /** Language. */
    public static final int LANG_INT = 15;
    // resource-collection group
    /** Item that allows child directory and file items to be defined within a directory hierarchy. */
    public static final int DIRECTORY_INT = 16;
    /** Item that allows details about a file to be provided for the software component. */
    public static final int FILE_INT = 17;
    /** Item that allows details to be provided about the runtime behavior of the software component. */
    public static final int PROCESS_INT = 18;
    /** Item that can be used to provide details about an artifact or capability. */
    public static final int RESOURCE_INT = 19;
    /** The file's size in bytes. */
    public static final int SIZE_INT = 20;
    /** The file's version as reported by querying information on the file from the operating system. */
    public static final int FILE_VERSION_INT = 21;
    /** A boolean value indicating if a file or directory is significant or required. */
    public static final int KEY_INT = 22;
    /** The filesystem path of the location of the CoSWID tag generated as evidence. */
    public static final int LOCATION_INT = 23;
    /** The name of the directory or file without any path informatio. */
    public static final int FS_NAME_INT = 24;
    /** A host-specific name for the root of the filesystem. */
    public static final int ROOT_STR_INT = 25;
    /** Group that allows a hierarchy of directory and file items to be defined in payload. */
    public static final int PATH_ELEMENTS_INT = 26;
    /** The software component's process name as it will appear in an endpoint's process list. */
    public static final int PROCESS_NAME_INT = 27;
    /** The process ID for a running instance of the software component in the endpoint's process list. */
    public static final int PID_INT = 28;
    /** A human-readable string indicating the type of resource. */
    public static final int TYPE_INT = 29;
    /** Name of the organizational entity claiming the roles for the CoSWID tag. */
    public static final int UNASSIGNED_INT = 30;
    /** entity-entry map. */
    public static final int ENTITY_NAME_INT = 31;
    /** Registration ID. */
    public static final int REG_ID_INT = 32;
    /** A textual value representing the relationship(s) between the entity and the software component. */
    public static final int ROLE_INT = 33;
    /** Value that provides a hash (i.e., the thumbprint) of the signing entity's public key certificate. */
    public static final int THUMBPRINT_INT = 34;
    // evidence-entry map
    /** The date and time the information was collected. */
    public static final int DATE_INT = 35;
    /** The endpoint's string identifier from which the evidence was collected. */
    public static final int DEVICE_INT = 36;
    // link-entry map
    /** Provides the absolute filesystem path to the installer executable. */
    public static final int ARTIFACT_INT = 37;
    /** A URI-reference for the referenced resource. */
    public static final int HREF_INT = 38;
    /** Indicates the degree of ownership between the software component and the referenced link. */
    public static final int OWNERSHIP_INT = 39;
    /** Relationship between this CoSWID and the target resource identified by the href item.. */
    public static final int REL_INT = 40;
    /** Supplies the resource consumer with a hint regarding what type of resource to expect. */
    public static final int MEDIA_TYPE_INT = 41;
    /** An integer or textual value used to determine if prerequisite software is required to be installed. */
    public static final int USE_INT = 42;
    // software-meta-entry map
    /** A textual value that identifies how the software component has been activated. */
    public static final int ACTIVATION_STATUS_INT = 43;
    /** A textual value that identifies a sales, licensing, or marketing channel. */
    public static final int CHANNEL_TYPE_INT = 44;
    /** A textual value for the software component's informal or colloquial version. */
    public static final int COLLOQUIAL_VERSION_INT = 45;
    /** A textual value that provides a detailed description of the software componen. */
    public static final int DESCRIPTION_INT = 46;
    /** ext indicating a functional variation of the code base. */
    public static final int EDITION_INT = 47;
    /** A boolean value that can be used to determine if accompanying proof of entitlement is needed. */
    public static final int ENTITLEMENT_DATA_REQUIRED_INT = 48;
    /** vendor-specific key that can be used to identify an entitlement. */
    public static final int ENTITLEMENT_KEY_INT = 49;
    /** The name (or tag-id) of the software component that created the CoSWID tag. */
    public static final int GENERATOR_INT = 50;
    /** A globally unique identifier used to identify a set of software components that are related. */
    public static final int PERSISTENT_ID_INT = 51;
    /**  A basic name for the software component. */
    public static final int PRODUCT_INT = 52;
    /** A textual value indicating the software components' overall product family. */
    public static final int PRODUCT_FAMILY_INT = 53;
    /** A string value indicating an informal or colloquial release version of the software. */
    public static final int REVISION_INT = 54;
    /** A short description of the software component. */
    public static final int SUMMARY_INT = 55;
    /** United Nations Standard Products and Services Code. */
    public static final int UNSPSC_CODE_INT = 56;
    /** United Nations Standard Products and Services Code version. */
    public static final int UNSPSC_VERSION_INT = 57;
    // other
    /** Unknown item id. */
    public static final int UNKNOWN_INT = 99;

    // Constant Item Names defined in RFC 9393 Table 9
    // concise-swid-tag map
    /** A 16-byte binary string, or a textual identifier, uniquely referencing a software component. */
    public static final String TAG_ID_STR = "tag-id";
    /** Textual item that provides the software component's name. */
    public static final String SOFTWARE_NAME_STR = "software-name";
    /** Provides information about one or more organizations responsible for producing the CoSWID tag. */
    public static final String ENTITY_STR = "entity";
    /** Records the results of a software discovery process used to identify untagged software. */
    public static final String EVIDENCE_STR = "evidence";
    /** Provides a means to establish relationship arcs between the tag and another item. */
    public static final String LINK_STR = "link";
    /** An open-ended map of key/value data pairs. */
    public static final String SOFTWARE_META_STR = "software-meta";
    /** Represents a collection of software artifacts that compose the target software. */
    public static final String PAYLOAD_STR = "payload";
    /** Value that provides a hash of a file. */
    public static final String HASH_STR = "hash";           // belongs to resource-collection group
    /** A boolean value that indicates if software component is an installable software component. */
    public static final String CORPUS_STR = "corpus";
    /** Indicates that the software component is an incremental change installed on an endpoint. */
    public static final String PATCH_STR = "patch";
    /** A boolean value that indicates if the tag is associated with another referenced SWID or CoSWID tag.*/
    public static final String SUPPLEMENTAL_STR = "supplemental";
    /** An integer value that indicates the specific release revision of the tag. */
    public static final String TAG_VERSION_STR = "tag-version";
    /** A textual value representing the specific release or development version of the software component. */
    public static final String SOFTWARE_VERSION_STR = "software-version";
    /** An integer or textual value representing the versioning scheme used for the software-version item. */
    public static final String VERSION_SCHEME_STR = "version-scheme";
    // global-attributes group
    /** Language. */
    public static final String LANG_STR = "lang";
    // resource-collection group
    /** Item that allows child directory and file items to be defined within a directory hierarchy. */
    public static final String DIRECTORY_STR = "directory";
    /** Item that allows details about a file to be provided for the software component. */
    public static final String FILE_STR = "file";
    /** Item that allows details to be provided about the runtime behavior of the software component. */
    public static final String PROCESS_STR = "process";
    /** Item that can be used to provide details about an artifact or capability. */
    public static final String RESOURCE_STR = "resource";
    /** The file's size in bytes. */
    public static final String SIZE_STR = "size";
    /** The file's version as reported by querying information on the file from the operating system. */
    public static final String FILE_VERSION_STR = "file-version";
    /** A boolean value indicating if a file or directory is significant or required. */
    public static final String KEY_STR = "key";
    /** The filesystem path of the location of the CoSWID tag generated as evidence. */
    public static final String LOCATION_STR = "location";
    /** The name of the directory or file without any path information. */
    public static final String FS_NAME_STR = "fs-name";
    /** A host-specific name for the root of the filesystem. */
    public static final String ROOT_STR = "root";
    /** Group that allows a hierarchy of directory and file items to be defined in payload. */
    public static final String PATH_ELEMENTS_STR = "path-elements";
    /** The software component's process name as it will appear in an endpoint's process list. */
    public static final String PROCESS_NAME_STR = "process-name";
    /** The process ID for a running instance of the software component in the endpoint's process list. */
    public static final String PID_STR = "pid";
    /** A human-readable string indicating the type of resource. */
    public static final String TYPE_STR = "type";
    // other
    /** Not currently a valid rfc 9393 item. */
    public static final String UNASSIGNED_STR = "Unassigned";
    // entity-entry map
    /** Name of the organizational entity claiming the roles for the CoSWID tag. */
    public static final String ENTITY_NAME_STR = "entity-name";
    /** Registration ID. */
    public static final String REG_ID_STR = "reg-id";
    /** A textual value representing the relationship(s) between the entity and the software component. */
    public static final String ROLE_STR = "role";
    /** Value that provides a hash (i.e., the thumbprint) of the signing entity's public key certificate. */
    public static final String THUMBPRINT_STR = "thumbprint";
    // evidence-entry map
    /** The date and time the information was collected. */
    public static final String DATE_STR = "date";
    /** The endpoint's string identifier from which the evidence was collected. */
    public static final String DEVICE_STR = "device";
    // link-entry map
    /** Item represents a query as defined by the W3C "Media Queries Level 3" Recommendation.  */
    public static final String MEDIA_STR = "media";
    /** Provides the absolute filesystem path to the installer executable. */
    public static final String ARTIFACT_STR = "artifact";
    /** A URI-reference for the referenced resource. */
    public static final String HREF_STR = "href";
    /** Indicates the degree of ownership between the software component and the referenced link. */
    public static final String OWNERSHIP_STR = "ownership";
    /** Relationship between this CoSWID and the target resource identified by the href item. */
    public static final String REL_STR = "rel";
    /** Supplies the resource consumer with a hint regarding what type of resource to expect. */
    public static final String MEDIA_TYPE_STR = "media-type";
    /** An integer or textual value used to determine if prerequisite software is required to be installed.*/
    public static final String USE_STR = "use";
    // software-meta-entry map
    /** A textual value that identifies how the software component has been activated. */
    public static final String ACTIVATION_STATUS_STR = "activation-status";
    /** A textual value that identifies a sales, licensing, or marketing channel. */
    public static final String CHANNEL_TYPE_STR = "channel-type";
    /** A textual value for the software component's informal or colloquial version. */
    public static final String COLLOQUIAL_VERSION_STR = "colloquial-version";
    /**A textual value that provides a detailed description of the software component. */
    public static final String DESCRIPTION_STR = "description";
    /** Text indicating a functional variation of the code base. */
    public static final String EDITION_STR = "edition";
    /**A boolean value that can be used to determine if accompanying proof of entitlement is needed. */
    public static final String ENTITLEMENT_DATA_REQUIRED_STR = "entitlement-data-required";
    /** A vendor-specific key that can be used to identify an entitlement. */
    public static final String ENTITLEMENT_KEY_STR = "entitlement-key";
    /** The name (or tag-id) of the software component that created the CoSWID tag. */
    public static final String GENERATOR_STR = "generator";
    /** A globally unique identifier used to identify a set of software components that are related. */
    public static final String PERSISTENT_ID_STR = "persistent-id";
    /**A basic name for the software component. */
    public static final String PRODUCT_STR = "product";
    /** A textual value indicating the software components' overall product family. */
    public static final String PRODUCT_FAMILY_STR = "product-family";
    /** A string value indicating an informal or colloquial release version of the software. */
    public static final String REVISION_STR = "revision";
    /** A short description of the software component. */
    public static final String SUMMARY_STR = "summary";
    /** United Nations Standard Products and Services Code. */
    public static final String UNSPSC_CODE_STR = "unspsc-code";
    /** United Nations Standard Products and Services Code version. */
    public static final String UNSPSC_VERSION_STR = "unspsc-version";
    /** Unknown item name. */
    public static final String UNKNOWN_STR = "unknown";
    /** Array of valid Coswid index names. */
    private static final String[][] INDEX_NAMES = {
            {"0", "tag-id" },
            {"1", "software-name" },
            {"2", "entity" },
            {"3", "evidence" },
            {"4", "link" },
            {"5", "software-meta" },
            {"6", "payload" },
            {"7", "hash" },
            {"8", "corpus" },
            {"9", "patch" },
            {"10", "media" },
            {"11", "supplemental" },
            {"12", "tag-version" },
            {"13", "software-version" },
            {"14", "version-scheme" },
            {"15", "lang" },
            {"16", "directory" },
            {"17", "file" },
            {"18", "process" },
            {"19", "resource" },
            {"20", "size" },
            {"21", "file-version" },
            {"22", "key" },
            {"23", "location" },
            {"24", "fs-name" },
            {"25", "root" },
            {"26", "path-elements" },
            {"27", "process-name" },
            {"28", "pid" },
            {"29", "type" },
            {"30", "Unassigned" },
            {"31", "entity-name" },
            {"32", "reg-id" },
            {"33", "role" },
            {"34", "thumbprint" },
            {"35", "date" },
            {"36", "device-id" },
            {"37", "artifact" },
            {"38", "href" },
            {"39", "ownership" },
            {"40", "rel" },
            {"41", "media-type" },
            {"42", "use" },
            {"43", "activation-status" },
            {"44", "channel-type" },
            {"45", "colloquial-version" },
            {"46", "description" },
            {"47", "edition" },
            {"48", "entitlement-data-required" },
            {"49", "entitlement-key" },
            {"50", "generator" },
            {"51", "persistent-id" },
            {"52", "product" },
            {"53", "product-family" },
            {"54", "revision" },
            {"55", "summary" },
            {"56", "unspsc-code" },
            {"57", "unspsc-version" }
    };
    /**
     * Searches Rfc 9393 Items Names for match to a specified item name and returns the index.
     * @param itemName  Iem Name specified in section 6.1 of rfc 9393
     * @return int id of algorithm
     */
    public static int getIndex(final String itemName) {
        for (int i = 0; i < INDEX_NAMES.length; i++) {
            if (itemName.compareToIgnoreCase(INDEX_NAMES[i][1]) == 0) {
                return i;
            }
        }
        return UNKNOWN_INT;
    }
    /**
     * Searches for an rfc 9393 specified index and returns the item name associated with the index.
     * @param index int rfc 939 sepcified index value
     * @return String item name associated with the index
     */
    public static String getItemName(final int index) {
        for (int i = 0; i < INDEX_NAMES.length; i++) {
            if (index == Integer.parseInt(INDEX_NAMES[i][0])) {
                return INDEX_NAMES[i][1];
            }
        }
        return UNKNOWN_STR;
    }

    /**
     * Protected constructor.
     */
    protected CoswidItems() {

    }
}
