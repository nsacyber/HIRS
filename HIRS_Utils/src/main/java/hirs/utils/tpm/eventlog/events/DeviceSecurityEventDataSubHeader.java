package hirs.utils.tpm.eventlog.events;


/**
 * Class to process the DEVICE_SECURITY_EVENT_DATA_SUB_HEADER event per PFP.
 *
 * <p>
 * typedef union tdDEVICE_SECURITY_EVENT_DATA_SUB_HEADER {
 *      DEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_MEASUREMENT_BLOCK  SpdmMeasurementBlock;
 *      DEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_CERT_CHAIN         SpdmCertChain;
 *      DEVICE_SECURITY_EVENT_DATA_SUB_HEADER_OEM_MEASUREMENT         OemMeasurement;
 * } DEVICE_SECURITY_EVENT_DATA_SUB_HEADER;
 * <p>
 */
public abstract class DeviceSecurityEventDataSubHeader {

    /**
     * Sub header type - SPDM measurement block
     */
    public static final int SUBHEADERTYPE_MEAS_BLOCK = 0;
    /**
     * Sub header type - SPDM cert chain
     */
    public static final int SUBHEADERTYPE_CERT_CHAIN = 1;

    public DeviceSecurityEventDataSubHeader() {
    }

    /**
     * Returns the device type via a lookup.
     * Lookup based upon section 10.2.7.2, Table 19, in the PFP 1.06 v52 spec.
     *
     * @param subheaderTypeInt int to convert to string
     * @return name of the device type
     */
    public static String subheaderTypeToString(final int subheaderTypeInt) {
        switch (subheaderTypeInt) {
            case SUBHEADERTYPE_MEAS_BLOCK:
                return "SPDM Measurement Block";
            case SUBHEADERTYPE_CERT_CHAIN:
                return "SPDM Cert Chain";
            default:
                return "Unknown or invalid Subheader Type of value " + subheaderTypeInt;
        }
    }
}
