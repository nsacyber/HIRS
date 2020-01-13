package hirs.swid;

import javax.xml.XMLConstants;

/**
 * This class contains the String constants that are referenced by the gateway
 * class. It is expected that member properties of this class will expand as
 * more functionality is added to SwidTagGateway.
 *
 */
public class SwidTagConstants {

    public static final String SCHEMA_STATEMENT = "ISO/IEC 19770-2:2015 Schema (XSD 1.0) "
            + "- September 2015, see http://standards.iso.org/iso/19770/-2/2015/schema.xsd";
    public static final String SCHEMA_PACKAGE = "hirs.swid.xjc";
    public static final String SCHEMA_LANGUAGE = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    public static final String SCHEMA_URL = "swid_schema.xsd";

    public static final String HIRS_SWIDTAG_HEADERS = "hirsSwidTagHeader.properties";
    public static final String EXAMPLE_PROPERTIES = "swidExample.properties";

    public static final String SOFTWARE_IDENTITY_NAME = "softwareIdentity.name";
    public static final String SOFTWARE_IDENTITY_TAGID = "softwareIdentity.tagId";
    public static final String SOFTWARE_IDENTITY_VERSION = "softwareIdentity.version";
    public static final String SOFTWARE_IDENTITY_CORPUS = "softwareIdentity.corpus";
    public static final String SOFTWARE_IDENTITY_PATCH = "softwareIdentity.patch";
    public static final String SOFTWARE_IDENTITY_SUPPLEMENTAL = "softwareIdentity.supplemental";

    public static final String ENTITY_NAME = "entity.name";
    public static final String ENTITY_REGID = "entity.regid";
    public static final String ENTITY_ROLE = "entity.role";
    public static final String ENTITY_THUMBPRINT = "entity.thumbprint";

    public static final String LINK_HREF = "link.href";
    public static final String LINK_REL = "link.rel";

    public static final String META_PCURILOCAL = "softwareMeta.pcUriLocal";
    public static final String META_BINDINGSPEC = "softwareMeta.bindingSpec";
    public static final String META_BINDINGSPECVERSION = "softwareMeta.bindingSpecVersion";
    public static final String META_PLATFORMMANUFACTURERID = "softwareMeta.platformManufacturerId";
    public static final String META_PLATFORMMANUFACTURERSTR = "softwareMeta.platformManufacturerStr";
    public static final String META_PLATFORMMODEL = "softwareMeta.platformModel";
    public static final String META_COMPONENTCLASS = "softwareMeta.componentClass";
    public static final String META_COMPONENTMANUFACTURER = "softwareMeta.componentManufacturer";
    public static final String META_COMPONENTMANUFACTURERID = "softwareMeta.componentManufacturerId";
    public static final String META_RIMLINKHASH = "softwareMeta.rimLinkHash";

    public static final String PAYLOAD_ENVVARPREFIX = "n8060.envvarprefix";
    public static final String PAYLOAD_ENVVARSUFFIX = "n8060.envvarsuffix";
    public static final String PAYLOAD_PATHSEPARATOR = "n8060.pathseparator";

    public static final String DIRECTORY_KEY = "directory.key";
    public static final String DIRECTORY_LOCATION = "directory.location";
    public static final String DIRECTORY_NAME = "directory.name";
    public static final String DIRECTORY_ROOT = "directory.root";
    public static final String FILE_KEY = "file.key";
    public static final String FILE_LOCATION = "file.location";
    public static final String FILE_NAME = "file.name";
    public static final String FILE_ROOT = "file.root";
    public static final String FILE_SIZE = "file.size";
    public static final String FILE_VERSION = "file.version";

    public static final int PCR_NUMBER = 0;
    public static final int PCR_VALUE = 1;
}
