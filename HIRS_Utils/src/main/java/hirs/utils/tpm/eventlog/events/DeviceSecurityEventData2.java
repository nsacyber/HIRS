package hirs.utils.tpm.eventlog.events;

import lombok.Getter;

// TODO Placeholder class to be implemented upon getting test pattern
/**
 * Class to process DEVICE_SECURITY_EVENT_DATA2.
 * Parses event data per PFP v1.06 Rev52 Table 26.
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA2 {
 * DEVICE_SECURITY_EVENT_DATA_HEADER2           EventDataHeader;
 * DEVICE_SECURITY_EVENT_DATA_SUB_HEADER        EventDataSubHeader;
 * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT    DeviceContext;
 * } DEVICE_SECURITY_EVENT_DATA2;
 * <p>
 */
public class DeviceSecurityEventData2 extends DeviceSecurityEventDataBase {

    /**
     * DeviceSecurityEventDataHeader Object.
     */
    @Getter
    private DeviceSecurityEventDataHeader2 dsedHeader2 = null;

    /**
     * DeviceSecurityEventData2 Constructor.
     *
     * @param dSEDbytes byte array holding the DeviceSecurityEventData2.
     */
    public DeviceSecurityEventData2(final byte[] dSEDbytes) {

        dsedHeader2 = new DeviceSecurityEventDataHeader2(dSEDbytes);
        // get subheader
        parseDeviceContext(dSEDbytes, dsedHeader2.getDSEDheaderByteSize(), dsedHeader2.getDeviceType());
    }

    /**
     * Returns a human readable description of the data within this structure.
     *
     * @return a description of this structure.
     */
    public String toString() {
        String dsedInfo = "";
        return dsedInfo;
    }
}
