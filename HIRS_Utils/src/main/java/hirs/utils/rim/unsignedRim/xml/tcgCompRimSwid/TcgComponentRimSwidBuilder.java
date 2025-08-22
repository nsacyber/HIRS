package hirs.utils.rim.unsignedRim.xml.tcgCompRimSwid;

import hirs.utils.rim.unsignedRim.xml.pcclientrim.PcClientRimBuilder;
import hirs.utils.swid.SwidTagConstants;
import hirs.utils.xjc.SoftwareMeta;
import jakarta.json.JsonObject;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * Class that is used to build a TCG Component RIM SWID file based upon values previously
 * retrieved from a config file.
 */
public class TcgComponentRimSwidBuilder extends PcClientRimBuilder {

    /** Component Manufacturer Name key. */
    public static final String COMPONENT_MANUFACTURER_STR = "platformManufacturerStr";
    /** Component Manufacturer ID key. */
    public static final String COMPONENT_MANUFACTURER_ID = "platformManufacturerId";
    /** Component Manufacturer Name. */
    public static final QName COMPONENT_MANUFACTURER_STR_QNAME =
            new QName("https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
                    "componentManufacturerStr", "rim");
    /** Component Manufacturer ID. */
    public static final QName COMPONENT_MANUFACTURER_ID_QNAME =
            new QName("https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model",
                    "componentManufacturerId", "rim");

    /**
     * Writes a TCG Component RIM SWID object to a SoftwareMeta object.
     * @param jsonObject JSON object with the config data
     */
    @Override
    protected SoftwareMeta createSoftwareMeta(final JsonObject jsonObject) {
        SoftwareMeta softwareMeta = this.objectFactory.createSoftwareMeta();
        Map<QName, String> attributes = softwareMeta.getOtherAttributes();
        this.addNonNullAttribute(attributes, SwidTagConstants.QNAME_COLLOQUIAL_VERSION,
                jsonObject.getString("colloquialVersion", ""));
        this.addNonNullAttribute(attributes, SwidTagConstants.QNAME_EDITION,
                jsonObject.getString("edition", ""));
        this.addNonNullAttribute(attributes, SwidTagConstants.QNAME_PRODUCT,
                jsonObject.getString("product", ""), true);
        this.addNonNullAttribute(attributes, SwidTagConstants.QNAME_REVISION,
                jsonObject.getString("revision", ""));
        this.addNonNullAttribute(attributes, SwidTagConstants.QNAME_PAYLOAD_TYPE,
                jsonObject.getString("PayloadType", ""));
        this.addNonNullAttribute(attributes, COMPONENT_MANUFACTURER_STR_QNAME,
                jsonObject.getString("componentManufacturerStr", ""), true);
        this.addNonNullAttribute(attributes, COMPONENT_MANUFACTURER_ID_QNAME,
                jsonObject.getString("componentManufacturerID", ""));
        this.addNonNullAttribute(attributes, SwidTagConstants.QNAME_FIRMWARE_MANUFACTURER_STR,
                jsonObject.getString("firmwareManufacturerStr", ""));
        this.addNonNullAttribute(attributes, SwidTagConstants.QNAME_FIRMWARE_MANUFACTURER_ID,
                jsonObject.getString("firmwareManufacturerId", ""));
        this.addNonNullAttribute(attributes, SwidTagConstants.QNAME_FIRMWARE_MODEL,
                jsonObject.getString("firmwareModel", ""));
        this.addNonNullAttribute(attributes, SwidTagConstants.QNAME_FIRMWARE_VERSION,
                jsonObject.getString("firmwareVersion", ""));
        this.addNonNullAttribute(attributes, SwidTagConstants.QNAME_BINDING_SPEC,
                jsonObject.getString("bindingSpec", ""));
        this.addNonNullAttribute(attributes, SwidTagConstants.QNAME_BINDING_SPEC_VERSION,
                jsonObject.getString("bindingSpecVersion", ""));
        this.addNonNullAttribute(attributes, SwidTagConstants.QNAME_PC_URI_LOCAL,
                jsonObject.getString("pcURIlocal", ""));
        this.addNonNullAttribute(attributes, SwidTagConstants.QNAME_PC_URI_GLOBAL,
                jsonObject.getString("pcURIGlobal", ""));
        this.addNonNullAttribute(attributes, SwidTagConstants.QNAME_RIM_LINK_HASH,
                jsonObject.getString("rimLinkHash", ""));
        return softwareMeta;
    }
}
