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

    public DeviceSecurityEventDataSubHeader() {
    }

}
