package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.TcgTpmtHa;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * Class to process the DeviceSecurityEventData event
 * DEVICE_SECURITY_EVENT_DATA has 2 structures:
 *    1) DEVICE_SECURITY_EVENT_DATA_HEADER
 *    2) DEVICE_SECURITY_EVENT_DATA_PCI_CONTEXT
 * DEVICE_SECURITY_EVENT_DATA_HEADER
 *    The first 16 bytes of the event data header MUST be a String based identifier (Signature),
 *    NUL-terminated. The only currently defined Signature is "SPDM Device Sec" which implies
 *    the event data is a DEVICE_SECURITY_EVENT_DATA. DEVICE_SECURITY_EVENT_DATA_HEADER contains
 *    the measurement(s) and hash algorithm (SpdmHashAlg) identifier returned by the SPDM
 *    "GET_MEASUREMENTS" function.
 * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT
 *    DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT is a common SPDM structure which includes the
 *       identification of the device, device vendor, subsystem, etc.
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA {
 * DEVICE_SECURITY_EVENT_DATA_HEADER            EventDataHeader;
 * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT    DeviceContext;
 * } DEVICE_SECURITY_EVENT_DATA;
 * <p>
 * Notes: Parses event data for an DEVICE_SECURITY_EVENT_DATA per PFP Spec.
 * 1. Has an EventType of EV_EFI_SPDM_FIRMWARE_BLOB (0x800000E1)
 * 2. Digest of 48 bytes
 * 3. Event content defined as DEVICE_SECURITY_EVENT_DATA Struct.
 * 4. First 16 bytes of the structure is an ASCII "SPDM Device Sec"
 */
public class DeviceSecurityEventData {

}
