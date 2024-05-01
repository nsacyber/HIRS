package hirs.utils.tpm.eventlog.events;

import lombok.Getter;
import java.io.UnsupportedEncodingException;

/**
 * Class to process DEVICE_SECURITY_EVENT_DATA.
 * Parses event data per PFP v1.06 Rev52 Table 20.
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA {
 * DEVICE_SECURITY_EVENT_DATA_HEADER            EventDataHeader;
 * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT    DeviceContext;
 * } DEVICE_SECURITY_EVENT_DATA;
 * <p>
 */
public class DeviceSecurityEventData extends DeviceSecurityEvent {

    /**
     * DeviceSecurityEventDataHeader Object.
     */
    @Getter
    private DeviceSecurityEventDataHeader dsedHeader = null;

    /**
     * DeviceSecurityEventData Constructor.
     *
     * @param dSEDbytes byte array holding the DeviceSecurityEventData.
     */
    public DeviceSecurityEventData(final byte[] dSEDbytes) throws UnsupportedEncodingException {
        dsedHeader = new DeviceSecurityEventDataHeader(dSEDbytes);
        parseDeviceContext(dSEDbytes, dsedHeader.getDSEDheaderByteSize(), dsedHeader.getDeviceType());
    }

    /**
     * Returns a human readable description of the data within this structure.
     *
     * @return a description of this structure.
     */
    public String toString() {
        String dsedInfo = "";
        dsedInfo += dsedHeader.toString();
        dsedInfo += getDeviceContextInfo();
        return dsedInfo;
    }
}
