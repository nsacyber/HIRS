package hirs.utils.tpm.eventlog.events;


/**
 * Class to process the NV_INDEX_INSTANCE_EVENT_LOG_DATA per PFP.
 * The first 16 bytes of the event data header are a String based identifier (Signature),
 * NUL-terminated, per PFP. The only currently defined Signature is "SPDM Device Sec",
 * which implies the data is a DEVICE_SECURITY_EVENT_DATA or ..DATA2.
 *
 * HEADERS defined by PFP v1.06 Rev 52.
 * Certain fields are common to both ..HEADER and ..HEADER2, and are noted below the structures.
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_HEADER {
 *      UINT8                           Signature[16];
 *      UINT16                          Version;
 *      UINT16                          Length;
 *      UINT32                          SpdmHashAlg;
 *      UINT32                          DeviceType;
 *      SPDM_MEASUREMENT_BLOCK          SpdmMeasurementBlock;
 *      UINT64                          DevicePathLength;
 *      UNIT8                           DevicePath[DevicePathLength]
 * } DEVICE_SECURITY_EVENT_DATA_HEADER;
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_HEADER2 {        - NOT IMPLEMENTED YET
 *      UINT8                           Signature[16];
 *      UINT16                          Version;
 *      UINT8                           AuthState;
 *      UINT8                           Reserved;
 *      UINT32                          Length;
 *      UINT32                          DeviceType;
 *      UINT32                          SubHeaderType;
 *      UINT32                          SubHeaderLength;
 *      UINT32                          SubHeaderUID;
 *      UINT64                          DevicePathLength;
 *      UNIT8                           DevicePath[DevicePathLength]
 * } DEVICE_SECURITY_EVENT_DATA_HEADER2;
 * <p>
 * Fields common to both ..HEADER and ..HEADER2:
 *    Signature
 *    Version
 *    DeviceType
 *    DevicePathLength
 *    DevicePath
 * <p>
 */
public class NvIndexInstanceEventLogData {
}
