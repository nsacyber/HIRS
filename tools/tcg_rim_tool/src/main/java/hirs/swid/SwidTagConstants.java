package hirs.swid;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;


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

    public static final String SOFTWARE_IDENTITY = "SoftwareIdentity";
    public static final String ENTITY = "Entity";
    public static final String LINK = "Link";
    public static final String META = "Meta";
    public static final String PAYLOAD = "Payload";
    public static final String DIRECTORY = "Directory";
    public static final String FILE = "File";
    public static final String NAME = "name";
    public static final String VERSION = "version";
    public static final String TAGID = "tagId";
    public static final String TAGVERSION = "tagVersion";
    public static final String CORPUS = "corpus";
    public static final String PATCH = "patch";
    public static final String SUPPLEMENTAL = "supplemental";
    public static final String ROLE = "role";
    public static final String THUMBPRINT = "thumbprint";
    public static final String HREF = "href";
    public static final String REL  = "rel";
    public static final String COLLOQUIAL_VERSION = "colloquialVersion";
    public static final String EDITION = "edition";
    public static final String PRODUCT = "product";
    public static final String REVISION = "revision";
    public static final String PLATFORM_MANUFACTURER_STR = "platformManufacturerStr";
    public static final String PLATFORM_MANUFACTURER_ID = "platformManufacturerId";
    public static final String PLATFORM_MODEL = "platformModel";
    public static final String BINDING_SPEC = "bindingSpec";
    public static final String BINDING_SPEC_VERSION = "bindingSpecVersion";
    public static final String RIM_LINK_HASH = "rimLinkHash";
    public static final String SIZE = "size";
    public static final String HASH = "hash";

    public static final QName _RIM_PCURILOCAL = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "pcURILocal", "rim");
    public static final QName _RIM_BINDINGSPEC = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "bindingSpec", "rim");
    public static final QName _RIM_BINDINGSPECVERSION = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "bindingSpecVersion", "rim");
    public static final QName _RIM_PLATFORMMANUFACTURERID = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "platformManufacturerId", "rim");
    public static final QName _RIM_PLATFORMMANUFACTURERSTR = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "platformManufacturerStr", "rim");
    public static final QName _RIM_PLATFORMMODEL = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "platformModel", "rim");
    public static final QName _RIM_COMPONENTCLASS = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "componentClass", "rim");
    public static final QName _RIM_COMPONENTMANUFACTURER = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "componentManufacturer", "rim");
    public static final QName _RIM_COMPONENTMANUFACTURERID = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "componentManufacturerId", "rim");
    public static final QName _RIM_RIMLINKHASH  = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "rimLinkHash", "rim");
    public static final QName _N8060_ENVVARPREFIX = new QName(
            "http://csrc.nist.gov/ns/swid/2015-extensions/1.0",
            "envVarPrefix", "n8060");
    public static final QName _N8060_ENVVARSUFFIX = new QName(
            "http://csrc.nist.gov/ns/swid/2015-extensions/1.0",
            "envVarSuffix", "n8060");
    public static final QName _N8060_PATHSEPARATOR = new QName(
            "http://csrc.nist.gov/ns/swid/2015-extensions/1.0",
            "pathSeparator", "n8060");

//Below properties can probably be deleted
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
