package hirs.utils.rim.unsignedRim.cbor.tcgCompRimCoswid;

import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.Coswid;
import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.CoswidItems;
import lombok.Getter;
import lombok.Setter;

/**
 * Class that implements the CoSwid variation of the TCG Component RIM.
 *    TCG Component RIM Binding for SWID/CoSWID:<br>
 *     Spec is currently missing value for rimLinkHash, componentManufacturerID, firmwareVersion,
 *          so these values are made up<br>
 *     Missing componentManufacturerID in table 4<br>
 *     Missing bindingSpecVersion in table 4 but has value assigned in section<br>
 */
public class TcgCompRimCoswid extends Coswid {
    /** Spec used to define this tag. */
    @Setter
    @Getter
    protected String crimBindingSpec = null;
    protected static final String CRIM_BINDING_SPEC_STR = "binding-spec";
    protected static final int CRIM_BINDING_SPEC_INT = 59;
    /** Version of the spec used to define this tag. */
    @Setter
    @Getter
    protected String crimBindingSpecVersion = null;
    protected static final String  CRIM_BINDING_SPEC_VERSION_STR = "binding-spec-version";
    protected static final int  CRIM_BINDING_SPEC_VERSION_INT = 60;
    /** crimPayloadType is cirect, indirect, or composite. */
    @Setter
    @Getter
    protected String crimPayloadType = null;
    protected static final String CRIM_PAYLOAD_TYPE_STR = "payload-type";
    protected static final int CRIM_PAYLOAD_TYPE_INT = 61;
    @Setter
    @Getter
    protected String crimComponentManufacturer = null;
    protected static final String CRIM_COMPONENT_MANUFACTURER_STR = "component-manufacturer-str";
    protected static final int CRIM_COMPONENT_MANUFACTURER_INT = 73;
    @Setter
    @Getter
    protected String crimComponentManufacturerID = null;
    protected static final String CRIM_COMPONENT_MANUFACTURER_ID_STR = "component-manufacturer-id";
    protected static final int CRIM_COMPONENT_MANUFACTURER_ID_INT = 74;

    // TCG-defined Payload definitions found in the TCG Component RIM SIWD/Cosiwd Binding spec
    protected static final String CRIM_TCG_COMPONENT_RIM_SW_META_ENTRY_STR = "metaExtEntry";
    protected static final int CRIM_TCG_COMPONENT_RIM_SW_META_INT = 58;
    protected static final String CRIM_TCG_COMPONENT_RIM_PAYLOAD_ENTRY_STR = "payloadExtEntry";
    protected static final int CRIM_TCG_COMPONENT_RIM_PAYLOAD_INT = 62;
    @Setter
    @Getter
    protected String crimSupportRimType = null;
    protected static final String CRIM_SUPPORT_RIM_TYPE_STR = "supportRimType";
    protected static final int CRIM_SUPPORT_RIM_TYPE_INT = 63;
    @Setter
    @Getter
    protected String crimSupportRimFormat = null;
    protected static final String CRIM_SUPPORT_RIM_FORMAT_STR = "supportRimFormat";
    protected static final int CRIM_SUPPORT_RIM_FORMAT_INT = 64;
    @Setter
    @Getter
    protected String crimSupportRimUriGlobal = null;
    protected static final String CRIM_SUPPORT_RIM_URI_GLOBAL_STR = "supportRimUriGlobal";
    protected static final int CRIM_SUPPORT_RIM_URI_GLOBAL_INT = 65;
    // SPDM Definitions within thc TCG Spec
    @Setter
    @Getter
    protected String crimSpdmMeasurementBlock = null;
    protected static final String CRIM_SPDM_MEASUREMENT_BLOCK_STR = "spdm-measurement-block";
    protected static final int CRIM_SPDM_MEASUREMENT_BLOCK_INT = 66;
    @Setter
    @Getter
    protected String crimSpdmVersion = null;
    protected static final String CRIM_SPDM_VERSION_STR = "spdm-version";
    protected static final int CRIM_SPDM_VERSION_INT = 67;
    @Setter
    @Getter
    protected String crimSpdmMeasurementBlockIndex = null;
    protected static final String CRIM_SPDM_MEASUREMENT_BLOCK_INDEX_STR = "spdm-measurement-block-index ";
    protected static final int CRIM_SPDM_MEASUREMENT_BLOCK_INDEX_INT = 68;
    @Setter
    @Getter
    protected String crimSpdmMeasurementSpec = null;
    protected static final String CRIM_SPDM_MEASUREMENT_STR = "spdm-measurement";
    protected static final int CRIM_SPDM_MEASUREMENT_SPEC_INT = 69;
    @Setter
    @Getter
    protected String crimSpdmMeasurementValueType = null;
    protected static final String CRIM_SPDM_MEASUREMENT_VALUE_TYPE_STR
            = "spdm-dmtf-spec-measurement-value-type";
    protected static final int SPDM_MEASUREMENT_VALUE_TYPE_INT = 70;
    @Setter
    @Getter
    protected String crimSpdmMeasurementHash = null;
    protected static final String CRIM_SPDM_MEASUREMENT_HASH_STR = "spdm-measurement-hash";
    protected static final int CRIM_SPDM_MEASUREMENT_HASH_INT = 71;
    @Setter
    @Getter
    protected String crimSpdmMeasurementRawData = null;
    protected static final String CRIM_SPDM_MEASUREMENT_RAW_DATA_STR = "spdm-measurement-raw-data";
    protected static final int CRIM_SPDM_MEASUREMENT_RAW_DATA_INT = 72;

    /**
     * Default constructor.
     */
    public TcgCompRimCoswid() {

    }

    /**
     * Converts a TCG Component RIM or coswid defined index into its corresponding text name.
     * @param index CDDL extension points defined by the TCG Component Rim Binding specification
     * @return String text name of the index
     */
    public static String lookupFieldName(final int index) {

        String value = "";

        switch (index) {
            case CRIM_BINDING_SPEC_INT: value = CRIM_BINDING_SPEC_STR; break;
            case CRIM_BINDING_SPEC_VERSION_INT: value = CRIM_BINDING_SPEC_VERSION_STR; break;
            case CRIM_PAYLOAD_TYPE_INT: value = CRIM_PAYLOAD_TYPE_STR; break;
            case CRIM_COMPONENT_MANUFACTURER_INT: value = CRIM_COMPONENT_MANUFACTURER_STR; break;
            case CRIM_COMPONENT_MANUFACTURER_ID_INT: value = CRIM_COMPONENT_MANUFACTURER_ID_STR; break;
            case CRIM_TCG_COMPONENT_RIM_SW_META_INT:
                value = CRIM_TCG_COMPONENT_RIM_SW_META_ENTRY_STR; break;
            case CRIM_TCG_COMPONENT_RIM_PAYLOAD_INT:
                value = CRIM_TCG_COMPONENT_RIM_PAYLOAD_ENTRY_STR; break;
            case CRIM_SUPPORT_RIM_TYPE_INT: value = CRIM_SUPPORT_RIM_TYPE_STR; break;
            case CRIM_SUPPORT_RIM_FORMAT_INT: value = CRIM_SUPPORT_RIM_FORMAT_STR; break;
            case CRIM_SUPPORT_RIM_URI_GLOBAL_INT: value = CRIM_SUPPORT_RIM_FORMAT_STR; break;
            case CRIM_SPDM_MEASUREMENT_BLOCK_INT: value = CRIM_SPDM_MEASUREMENT_BLOCK_STR; break;
            case CRIM_SPDM_VERSION_INT: value = CRIM_SPDM_VERSION_STR; break;
            case CRIM_SPDM_MEASUREMENT_BLOCK_INDEX_INT:
                value = CRIM_SPDM_MEASUREMENT_BLOCK_INDEX_STR; break;
            case CRIM_SPDM_MEASUREMENT_SPEC_INT: value = CRIM_SPDM_MEASUREMENT_STR; break;
            case SPDM_MEASUREMENT_VALUE_TYPE_INT: value = CRIM_SPDM_MEASUREMENT_VALUE_TYPE_STR; break;
            case CRIM_SPDM_MEASUREMENT_HASH_INT: value = CRIM_SPDM_MEASUREMENT_HASH_STR; break;
            case CRIM_SPDM_MEASUREMENT_RAW_DATA_INT: value = CRIM_SPDM_MEASUREMENT_RAW_DATA_STR; break;
            // if current index is not defined by TCg Component RIM then lookup coswid index
            default: value = CoswidItems.getItemName(index);
        }
        return value;
    }

    /**
     * Converts a TCG Component RIM or coswid defined index name (key) into its corresponding index value.
     * @param key label (name) given to the CDDL extension points defined by the
     *            TCG Component Rim Binding specification
     * @return int the corresponding int value
     */
    public int lookupIndex(final String key) {
        int value = 0;
        switch (key) {
            case CRIM_BINDING_SPEC_STR: value = CRIM_BINDING_SPEC_INT; break;
            case CRIM_BINDING_SPEC_VERSION_STR: value = CRIM_BINDING_SPEC_VERSION_INT; break;
            case CRIM_PAYLOAD_TYPE_STR: value = CRIM_PAYLOAD_TYPE_INT; break;
            case CRIM_COMPONENT_MANUFACTURER_STR : value = CRIM_COMPONENT_MANUFACTURER_INT; break;
            case CRIM_COMPONENT_MANUFACTURER_ID_STR : value = CRIM_COMPONENT_MANUFACTURER_ID_INT; break;
            case CRIM_TCG_COMPONENT_RIM_SW_META_ENTRY_STR : value = CRIM_TCG_COMPONENT_RIM_SW_META_INT; break;
            case CRIM_TCG_COMPONENT_RIM_PAYLOAD_ENTRY_STR : value = CRIM_TCG_COMPONENT_RIM_PAYLOAD_INT; break;
            case CRIM_SUPPORT_RIM_TYPE_STR : value = CRIM_SUPPORT_RIM_TYPE_INT; break;
            case CRIM_SUPPORT_RIM_FORMAT_STR : value = CRIM_SUPPORT_RIM_FORMAT_INT; break;
            case CRIM_SUPPORT_RIM_URI_GLOBAL_STR : value = CRIM_SUPPORT_RIM_URI_GLOBAL_INT; break;
            case CRIM_SPDM_MEASUREMENT_BLOCK_STR : value = CRIM_SPDM_MEASUREMENT_BLOCK_INT; break;
            case CRIM_SPDM_VERSION_STR : value = CRIM_SPDM_VERSION_INT; break;
            case CRIM_SPDM_MEASUREMENT_BLOCK_INDEX_STR : value = CRIM_SPDM_MEASUREMENT_BLOCK_INDEX_INT; break;
            case CRIM_SPDM_MEASUREMENT_STR : value = CRIM_SPDM_MEASUREMENT_SPEC_INT; break;
            case CRIM_SPDM_MEASUREMENT_VALUE_TYPE_STR : value = SPDM_MEASUREMENT_VALUE_TYPE_INT; break;
            case CRIM_SPDM_MEASUREMENT_HASH_STR : value = CRIM_SPDM_MEASUREMENT_HASH_INT; break;
            case CRIM_SPDM_MEASUREMENT_RAW_DATA_STR : value = CRIM_SPDM_MEASUREMENT_RAW_DATA_INT; break;
            // if current index is not defined by TCg Component RIM then lookup coswid index
            default: value = CoswidItems.getIndex(key);
        }
        return value;
    }
}
