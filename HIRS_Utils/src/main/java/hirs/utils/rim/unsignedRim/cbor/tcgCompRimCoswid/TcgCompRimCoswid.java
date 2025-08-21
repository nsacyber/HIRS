package hirs.utils.rim.unsignedRim.cbor.tcgCompRimCoswid;

import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.Coswid;
import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.CoswidItems;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class that implements the CoSwid variation of the TCG Component RIM.
 *    TCG Component RIM Binding for SWID/CoSWID:<br>
 *     Spec is currently missing value for rimLinkHash, componentManufacturerID, firmwareVersion,
 *          so these values are made up<br>
 *     Missing componentManufacturerID in table 4<br>
 *     Missing bindingSpecVersion in table 4 but has value assigned in section<br>
 */
@NoArgsConstructor @Getter @Setter
public class TcgCompRimCoswid extends Coswid {
    /** Spec used to define this tag. */
    protected String crimBindingSpec = null;
    protected static final String CRIM_BINDING_SPEC_STR = "binding-spec";
    protected static final int CRIM_BINDING_SPEC_INT = 59;
    /** Version of the spec used to define this tag. */
    protected String crimBindingSpecVersion = null;
    protected static final String  CRIM_BINDING_SPEC_VERSION_STR = "binding-spec-version";
    protected static final int  CRIM_BINDING_SPEC_VERSION_INT = 60;
    /** crimPayloadType is cirect, indirect, or composite. */
    protected String crimPayloadType = null;
    protected static final String CRIM_PAYLOAD_TYPE_STR = "payload-type";
    protected static final int CRIM_PAYLOAD_TYPE_INT = 61;
    protected String crimComponentManufacturer = null;
    protected static final String CRIM_COMPONENT_MANUFACTURER_STR = "component-manufacturer-str";
    protected static final int CRIM_COMPONENT_MANUFACTURER_INT = 73;
    protected String crimComponentManufacturerID = null;
    protected static final String CRIM_COMPONENT_MANUFACTURER_ID_STR = "component-manufacturer-id";
    protected static final int CRIM_COMPONENT_MANUFACTURER_ID_INT = 74;

    // TCG-defined Payload definitions found in the TCG Component RIM SIWD/Cosiwd Binding spec
    protected static final String CRIM_TCG_COMPONENT_RIM_SW_META_ENTRY_STR = "metaExtEntry";
    protected static final int CRIM_TCG_COMPONENT_RIM_SW_META_INT = 58;
    protected static final String CRIM_TCG_COMPONENT_RIM_PAYLOAD_ENTRY_STR = "payloadExtEntry";
    protected static final int CRIM_TCG_COMPONENT_RIM_PAYLOAD_INT = 62;
    protected String crimSupportRimType = null;
    protected static final String CRIM_SUPPORT_RIM_TYPE_STR = "supportRimType";
    protected static final int CRIM_SUPPORT_RIM_TYPE_INT = 63;
    protected String crimSupportRimFormat = null;
    protected static final String CRIM_SUPPORT_RIM_FORMAT_STR = "supportRimFormat";
    protected static final int CRIM_SUPPORT_RIM_FORMAT_INT = 64;
    protected String crimSupportRimUriGlobal = null;
    protected static final String CRIM_SUPPORT_RIM_URI_GLOBAL_STR = "supportRimUriGlobal";
    protected static final int CRIM_SUPPORT_RIM_URI_GLOBAL_INT = 65;
    // SPDM Definitions within thc TCG Spec
    protected String crimSpdmMeasurementBlock = null;
    protected static final String CRIM_SPDM_MEASUREMENT_BLOCK_STR = "spdm-measurement-block";
    protected static final int CRIM_SPDM_MEASUREMENT_BLOCK_INT = 66;
    protected String crimSpdmVersion = null;
    protected static final String CRIM_SPDM_VERSION_STR = "spdm-version";
    protected static final int CRIM_SPDM_VERSION_INT = 67;
    protected String crimSpdmMeasurementBlockIndex = null;
    protected static final String CRIM_SPDM_MEASUREMENT_BLOCK_INDEX_STR = "spdm-measurement-block-index ";
    protected static final int CRIM_SPDM_MEASUREMENT_BLOCK_INDEX_INT = 68;
    protected String crimSpdmMeasurementSpec = null;
    protected static final String CRIM_SPDM_MEASUREMENT_STR = "spdm-measurement";
    protected static final int CRIM_SPDM_MEASUREMENT_SPEC_INT = 69;
    protected String crimSpdmMeasurementValueType = null;
    protected static final String CRIM_SPDM_MEASUREMENT_VALUE_TYPE_STR
            = "spdm-dmtf-spec-measurement-value-type";
    protected static final int SPDM_MEASUREMENT_VALUE_TYPE_INT = 70;
    protected String crimSpdmMeasurementHash = null;
    protected static final String CRIM_SPDM_MEASUREMENT_HASH_STR = "spdm-measurement-hash";
    protected static final int CRIM_SPDM_MEASUREMENT_HASH_INT = 71;
    protected String crimSpdmMeasurementRawData = null;
    protected static final String CRIM_SPDM_MEASUREMENT_RAW_DATA_STR = "spdm-measurement-raw-data";
    protected static final int CRIM_SPDM_MEASUREMENT_RAW_DATA_INT = 72;

    /**
     * Converts a TCG Component RIM or coswid defined index into its corresponding text name.
     * @param index CDDL extension points defined by the TCG Component Rim Binding specification
     * @return String text name of the index
     */
    public static String lookupFieldName(final int index) {
        return switch (index) {
            case CRIM_BINDING_SPEC_INT -> CRIM_BINDING_SPEC_STR;
            case CRIM_BINDING_SPEC_VERSION_INT -> CRIM_BINDING_SPEC_VERSION_STR;
            case CRIM_PAYLOAD_TYPE_INT -> CRIM_PAYLOAD_TYPE_STR;
            case CRIM_COMPONENT_MANUFACTURER_INT -> CRIM_COMPONENT_MANUFACTURER_STR;
            case CRIM_COMPONENT_MANUFACTURER_ID_INT -> CRIM_COMPONENT_MANUFACTURER_ID_STR;
            case CRIM_TCG_COMPONENT_RIM_SW_META_INT -> CRIM_TCG_COMPONENT_RIM_SW_META_ENTRY_STR;
            case CRIM_TCG_COMPONENT_RIM_PAYLOAD_INT -> CRIM_TCG_COMPONENT_RIM_PAYLOAD_ENTRY_STR;
            case CRIM_SUPPORT_RIM_TYPE_INT -> CRIM_SUPPORT_RIM_TYPE_STR;
            case CRIM_SUPPORT_RIM_FORMAT_INT -> CRIM_SUPPORT_RIM_FORMAT_STR;
            case CRIM_SUPPORT_RIM_URI_GLOBAL_INT -> CRIM_SUPPORT_RIM_FORMAT_STR;
            case CRIM_SPDM_MEASUREMENT_BLOCK_INT -> CRIM_SPDM_MEASUREMENT_BLOCK_STR;
            case CRIM_SPDM_VERSION_INT -> CRIM_SPDM_VERSION_STR;
            case CRIM_SPDM_MEASUREMENT_BLOCK_INDEX_INT -> CRIM_SPDM_MEASUREMENT_BLOCK_INDEX_STR;
            case CRIM_SPDM_MEASUREMENT_SPEC_INT -> CRIM_SPDM_MEASUREMENT_STR;
            case SPDM_MEASUREMENT_VALUE_TYPE_INT -> CRIM_SPDM_MEASUREMENT_VALUE_TYPE_STR;
            case CRIM_SPDM_MEASUREMENT_HASH_INT -> CRIM_SPDM_MEASUREMENT_HASH_STR;
            case CRIM_SPDM_MEASUREMENT_RAW_DATA_INT -> CRIM_SPDM_MEASUREMENT_RAW_DATA_STR;
            // if current index is not defined by TCG Component RIM then lookup coswid index
            default -> CoswidItems.getItemName(index);
        };
    }

    /**
     * Converts a TCG Component RIM or coswid defined index name (key) into its corresponding index value.
     * @param key label (name) given to the CDDL extension points defined by the
     *            TCG Component Rim Binding specification
     * @return int the corresponding int value
     */
    public int lookupIndex(final String key) {
        return switch (key) {
            case CRIM_BINDING_SPEC_STR -> CRIM_BINDING_SPEC_INT;
            case CRIM_BINDING_SPEC_VERSION_STR -> CRIM_BINDING_SPEC_VERSION_INT;
            case CRIM_PAYLOAD_TYPE_STR -> CRIM_PAYLOAD_TYPE_INT;
            case CRIM_COMPONENT_MANUFACTURER_STR -> CRIM_COMPONENT_MANUFACTURER_INT;
            case CRIM_COMPONENT_MANUFACTURER_ID_STR -> CRIM_COMPONENT_MANUFACTURER_ID_INT;
            case CRIM_TCG_COMPONENT_RIM_SW_META_ENTRY_STR -> CRIM_TCG_COMPONENT_RIM_SW_META_INT;
            case CRIM_TCG_COMPONENT_RIM_PAYLOAD_ENTRY_STR -> CRIM_TCG_COMPONENT_RIM_PAYLOAD_INT;
            case CRIM_SUPPORT_RIM_TYPE_STR -> CRIM_SUPPORT_RIM_TYPE_INT;
            case CRIM_SUPPORT_RIM_FORMAT_STR -> CRIM_SUPPORT_RIM_FORMAT_INT;
            case CRIM_SUPPORT_RIM_URI_GLOBAL_STR -> CRIM_SUPPORT_RIM_URI_GLOBAL_INT;
            case CRIM_SPDM_MEASUREMENT_BLOCK_STR -> CRIM_SPDM_MEASUREMENT_BLOCK_INT;
            case CRIM_SPDM_VERSION_STR -> CRIM_SPDM_VERSION_INT;
            case CRIM_SPDM_MEASUREMENT_BLOCK_INDEX_STR -> CRIM_SPDM_MEASUREMENT_BLOCK_INDEX_INT;
            case CRIM_SPDM_MEASUREMENT_STR -> CRIM_SPDM_MEASUREMENT_SPEC_INT;
            case CRIM_SPDM_MEASUREMENT_VALUE_TYPE_STR -> SPDM_MEASUREMENT_VALUE_TYPE_INT;
            case CRIM_SPDM_MEASUREMENT_HASH_STR -> CRIM_SPDM_MEASUREMENT_HASH_INT;
            case CRIM_SPDM_MEASUREMENT_RAW_DATA_STR -> CRIM_SPDM_MEASUREMENT_RAW_DATA_INT;
            default -> CoswidItems.getIndex(key);
        };
    }
}
