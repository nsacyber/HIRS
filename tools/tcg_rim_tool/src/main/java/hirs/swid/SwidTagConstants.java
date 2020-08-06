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

    public static final String DEFAULT_KEYSTORE_PATH = "keystore.jks";
    public static final String DEFAULT_KEYSTORE_PASSWORD = "password";
    public static final String DEFAULT_PRIVATE_KEY_ALIAS = "selfsigned";
    public static final String DEFAULT_ATTRIBUTES_FILE = "rim_fields.json";
    public static final String DEFAULT_ENGLISH = "en";

    public static final String SIGNATURE_ALGORITHM_RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    public static final String SCHEMA_PACKAGE = "hirs.swid.xjc";
    public static final String SCHEMA_LANGUAGE = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    public static final String SCHEMA_URL = "swid_schema.xsd";

    public static final String SOFTWARE_IDENTITY = "SoftwareIdentity";
    public static final String ENTITY = "Entity";
    public static final String LINK = "Link";
    public static final String META = "Meta";
    public static final String PAYLOAD = "Payload";
    public static final String DIRECTORY = "Directory";
    public static final String FILE = "File";
    public static final String NAME = "name";
    public static final String VERSION = "version";
    public static final String VERSION_SCHEME = "versionScheme";
    public static final String TAGID = "tagId";
    public static final String TAGVERSION = "tagVersion";
    public static final String CORPUS = "corpus";
    public static final String PATCH = "patch";
    public static final String SUPPLEMENTAL = "supplemental";
    public static final String REGID = "regid";
    public static final String ROLE = "role";
    public static final String THUMBPRINT = "thumbprint";
    public static final String HREF = "href";
    public static final String REL  = "rel";
    public static final String COLLOQUIAL_VERSION = "colloquialVersion";
    public static final String EDITION = "edition";
    public static final String PRODUCT = "product";
    public static final String REVISION = "revision";
    public static final String PAYLOAD_TYPE = "PayloadType";
    public static final String HYBRID = "hybrid";
    public static final String PLATFORM_MANUFACTURER_STR = "platformManufacturerStr";
    public static final String PLATFORM_MANUFACTURER_ID = "platformManufacturerId";
    public static final String PLATFORM_MODEL = "platformModel";
    public static final String PLATFORM_VERSION = "platformVersion";
    public static final String FIRMWARE_MANUFACTURER_STR = "firmwareManufacturerStr";
    public static final String FIRMWARE_MANUFACTURER_ID = "firmwareManufacturerId";
    public static final String FIRMWARE_MODEL = "firmwareModel";
    public static final String FIRMWARE_VERSION = "firmwareVersion";
    public static final String BINDING_SPEC = "bindingSpec";
    public static final String BINDING_SPEC_VERSION = "bindingSpecVersion";
    public static final String PC_URI_LOCAL = "pcURIlocal";
    public static final String PC_URI_GLOBAL = "pcURIGlobal";
    public static final String RIM_LINK_HASH = "rimLinkHash";
    public static final String SIZE = "size";
    public static final String HASH = "hash";
    public static final String SUPPORT_RIM_TYPE = "supportRIMType";
    public static final String SUPPORT_RIM_FORMAT = "supportRIMFormat";
    public static final String SUPPORT_RIM_URI_GLOBAL = "supportRIMURIGlobal";

    public static final QName _SHA256_HASH = new QName(
            "http://www.w3.org/2001/04/xmlenc#sha256",
            "hash", "SHA256");
    public static final QName _COLLOQUIAL_VERSION = new QName(
            "http://csrc.nist.gov/ns/swid/2015-extensions/1.0",
            "colloquialVersion", "n8060");
    public static final QName _EDITION = new QName(
            "http://csrc.nist.gov/ns/swid/2015-extensions/1.0",
            "edition", "n8060");
    public static final QName _PRODUCT = new QName(
            "http://csrc.nist.gov/ns/swid/2015-extensions/1.0",
            "product", "n8060");
    public static final QName _REVISION = new QName(
            "http://csrc.nist.gov/ns/swid/2015-extensions/1.0",
            "revision", "n8060");
    public static final QName _PAYLOAD_TYPE = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "PayloadType", "rim");
    public static final QName _PLATFORM_MANUFACTURER_STR = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "platformManufacturerStr", "rim");
    public static final QName _PLATFORM_MANUFACTURER_ID = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "platformManufacturerId", "rim");
    public static final QName _PLATFORM_MODEL = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "platformModel", "rim");
    public static final QName _PLATFORM_VERSION = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "platformVersion", "rim");
    public static final QName _FIRMWARE_MANUFACTURER_STR = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "firmwareManufacturerStr", "rim");
    public static final QName _FIRMWARE_MANUFACTURER_ID = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "firmwareManufacturerId", "rim");
    public static final QName _FIRMWARE_MODEL = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "firmwareModel", "rim");
    public static final QName _FIRMWARE_VERSION = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "firmwareVersion", "rim");
    public static final QName _BINDING_SPEC = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "BindingSpec", "rim");
    public static final QName _BINDING_SPEC_VERSION = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "BindingSpecVersion", "rim");
    public static final QName _PC_URI_LOCAL = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "pcURILocal", "rim");
    public static final QName _PC_URI_GLOBAL = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "pcURIGlobal", "rim");
    public static final QName _RIM_LINK_HASH  = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "rimLinkHash", "rim");
    public static final QName _SUPPORT_RIM_TYPE = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "supportRIMType", "rim");
    public static final QName _SUPPORT_RIM_FORMAT = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "supportRIMFormat", "rim");
    public static final QName _SUPPORT_RIM_URI_GLOBAL = new QName(
            "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
            "supportRIMURIGlobal", "rim");
    public static final QName _N8060_ENVVARPREFIX = new QName(
            "http://csrc.nist.gov/ns/swid/2015-extensions/1.0",
            "envVarPrefix", "n8060");
    public static final QName _N8060_ENVVARSUFFIX = new QName(
            "http://csrc.nist.gov/ns/swid/2015-extensions/1.0",
            "envVarSuffix", "n8060");
    public static final QName _N8060_PATHSEPARATOR = new QName(
            "http://csrc.nist.gov/ns/swid/2015-extensions/1.0",
            "pathSeparator", "n8060");

    public static final String CA_ISSUERS = "1.3.6.1.5.5.7.48.2";
}
