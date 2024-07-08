package hirs.utils.tpm.eventlog.events;

import lombok.Getter;

import java.io.IOException;
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
     * Human-readable description of the data within the
     * DEVICE_SECURITY_EVENT_DATA_HEADER.
     */
    @Getter
    String headerInfo = "";

    /**
     * DeviceSecurityEventData Constructor.
     *
     * @param dsedBytes byte array holding the DeviceSecurityEventData.
     */
    public DeviceSecurityEventData(final byte[] dsedBytes) {

        try {
            dsedHeader = new DeviceSecurityEventDataHeader(dsedBytes);
            headerInfo = dsedHeader.toString();

            setDeviceType(dsedHeader.getDeviceType());
            int dsedHeaderLength = dsedHeader.getDsedHeaderLength();

            int dsedDevContextLength = dsedBytes.length - dsedHeaderLength;
            byte[] dsedDevContextBytes = new byte[dsedDevContextLength];
            System.arraycopy(dsedBytes, dsedHeaderLength, dsedDevContextBytes, 0,
                    dsedDevContextLength);

            instantiateDeviceContext(dsedDevContextBytes);
        }
        catch(NullPointerException e) {
            headerInfo = "   Could not interpret Header info";
        }
    }

    /**
     * Returns a human-readable description of the data within this structure.
     *
     * @return a description of this structure.
     */
    public String toString() {
        String dsedInfo = "";
        dsedInfo += headerInfo;
        dsedInfo += getDeviceContextInfo();
        return dsedInfo;
    }
}
