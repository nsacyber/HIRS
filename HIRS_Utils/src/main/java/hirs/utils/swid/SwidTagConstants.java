package hirs.utils.swid;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;


/**
 * This class contains the String constants that are referenced by the gateway
 * class. It is expected that member properties of this class will expand as
 * more functionality is added to SwidTagGateway.
 */
public class SwidTagConstants {

    public static final String DEFAULT_KEYSTORE_FILE = "keystore.jks";//"/opt/hirs/rimtool/keystore.jks";
    public static final String DEFAULT_KEYSTORE_PASSWORD = "password";
    public static final String DEFAULT_PRIVATE_KEY_ALIAS = "1";
    public static final String DEFAULT_ATTRIBUTES_FILE = "/opt/hirs/rimtool/rim_fields.json";
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
    public static final String REL = "rel";
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
    public static final String TCG_EVENTLOG_ASSERTION = "TCG_EventLog_Assertion";
    public static final String TPM_PCR_ASSERTION = "TPM_PCR_Assertion";
    public static final String SUPPORT_RIM_FORMAT_MISSING = "supportRIMFormat missing";
    public static final String SUPPORT_RIM_URI_GLOBAL = "supportRIMURIGlobal";
    public static final String DATETIME = "dateTime";

    public static final String NIST_NS = "http://csrc.nist.gov/ns/swid/2015-extensions/1.0";
    public static final String TCG_NS = "https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model";
    public static final String RFC3852_NS = "https://www.ietf.org/rfc/rfc3852.txt";
    public static final String RFC3339_NS = "https://www.ietf.org/rfc/rfc3339.txt";

    public static final String N8060_PFX = "n8060";
    public static final String RIM_PFX = "rim";
    public static final String FX_SEPARATOR = ":";
    public static final String RFC3852_PFX = "rcf3852";
    public static final String RFC3339_PFX = "rcf3339";

    public static final String _COLLOQUIAL_VERSION_STR = new String(N8060_PFX + FX_SEPARATOR +
            COLLOQUIAL_VERSION);
    public static final String _PRODUCT_STR = new String(N8060_PFX + FX_SEPARATOR +
            PRODUCT);
    public static final String _REVISION_STR = new String(N8060_PFX + FX_SEPARATOR +
            REVISION);
    public static final String _EDITION_STR = new String(N8060_PFX + FX_SEPARATOR +
            EDITION);

    public static final String _RIM_LINK_HASH_STR = new String(RIM_PFX + FX_SEPARATOR +
            RIM_LINK_HASH);
    public static final String _BINDING_SPEC_STR = new String(RIM_PFX + FX_SEPARATOR +
            BINDING_SPEC);
    public static final String _BINDING_SPEC_VERSION_STR = new String(RIM_PFX + FX_SEPARATOR +
            BINDING_SPEC_VERSION);
    public static final String _PLATFORM_MANUFACTURER_STR = new String(RIM_PFX + FX_SEPARATOR +
            PLATFORM_MANUFACTURER_STR);
    public static final String _PLATFORM_MANUFACTURER_ID_STR = new String(RIM_PFX + FX_SEPARATOR +
            PLATFORM_MANUFACTURER_ID);
    public static final String _PLATFORM_MODEL_STR = new String(RIM_PFX + FX_SEPARATOR +
            PLATFORM_MODEL);
    public static final String _PLATFORM_VERSION_STR = new String(RIM_PFX + FX_SEPARATOR +
            PLATFORM_VERSION);
    public static final String _PAYLOAD_TYPE_STR = new String(RIM_PFX + FX_SEPARATOR +
            PAYLOAD_TYPE);
    public static final String _PC_URI_LOCAL_STR = new String(RIM_PFX + FX_SEPARATOR +
            PC_URI_LOCAL);
    public static final String _PC_URI_GLOBAL_STR = new String(RIM_PFX + FX_SEPARATOR +
            PC_URI_GLOBAL);


    public static final QName _SHA256_HASH = new QName(
            "http://www.w3.org/2001/04/xmlenc#sha256", HASH, "SHA256");
    public static final QName _COLLOQUIAL_VERSION = new QName(
            NIST_NS, COLLOQUIAL_VERSION, N8060_PFX);
    public static final QName _EDITION = new QName(
            NIST_NS, EDITION, N8060_PFX);
    public static final QName _PRODUCT = new QName(
            NIST_NS, PRODUCT, N8060_PFX);
    public static final QName _REVISION = new QName(
            NIST_NS, REVISION, N8060_PFX);
    public static final QName _PAYLOAD_TYPE = new QName(
            TCG_NS, PAYLOAD_TYPE, RIM_PFX);
    public static final QName _PLATFORM_MANUFACTURER = new QName(
            TCG_NS, PLATFORM_MANUFACTURER_STR, RIM_PFX);
    public static final QName _PLATFORM_MANUFACTURER_ID = new QName(
            TCG_NS, PLATFORM_MANUFACTURER_ID, RIM_PFX);
    public static final QName _PLATFORM_MODEL = new QName(
            TCG_NS, PLATFORM_MODEL, RIM_PFX);
    public static final QName _PLATFORM_VERSION = new QName(
            TCG_NS, PLATFORM_VERSION, RIM_PFX);
    public static final QName _FIRMWARE_MANUFACTURER_STR = new QName(
            TCG_NS, FIRMWARE_MANUFACTURER_STR, RIM_PFX);
    public static final QName _FIRMWARE_MANUFACTURER_ID = new QName(
            TCG_NS, FIRMWARE_MANUFACTURER_ID, RIM_PFX);
    public static final QName _FIRMWARE_MODEL = new QName(
            TCG_NS, FIRMWARE_MODEL, RIM_PFX);
    public static final QName _FIRMWARE_VERSION = new QName(
            TCG_NS, FIRMWARE_VERSION, RIM_PFX);
    public static final QName _BINDING_SPEC = new QName(
            TCG_NS, BINDING_SPEC, RIM_PFX);
    public static final QName _BINDING_SPEC_VERSION = new QName(
            TCG_NS, BINDING_SPEC_VERSION, RIM_PFX);
    public static final QName _PC_URI_LOCAL = new QName(
            TCG_NS, PC_URI_LOCAL, RIM_PFX);
    public static final QName _PC_URI_GLOBAL = new QName(
            TCG_NS, PC_URI_GLOBAL, RIM_PFX);
    public static final QName _RIM_LINK_HASH = new QName(
            TCG_NS, RIM_LINK_HASH, RIM_PFX);
    public static final QName _SUPPORT_RIM_TYPE = new QName(
            TCG_NS, SUPPORT_RIM_TYPE, RIM_PFX);
    public static final QName _SUPPORT_RIM_FORMAT = new QName(
            TCG_NS, SUPPORT_RIM_FORMAT, RIM_PFX);
    public static final QName _SUPPORT_RIM_URI_GLOBAL = new QName(
            TCG_NS, SUPPORT_RIM_URI_GLOBAL, RIM_PFX);
    public static final QName _N8060_ENVVARPREFIX = new QName(
            NIST_NS, "envVarPrefix", N8060_PFX);
    public static final QName _N8060_ENVVARSUFFIX = new QName(
            NIST_NS, "envVarSuffix", N8060_PFX);
    public static final QName _N8060_PATHSEPARATOR = new QName(
            NIST_NS, "pathSeparator", N8060_PFX);

    public static final String CA_ISSUERS = "1.3.6.1.5.5.7.48.2";
}
