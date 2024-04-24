package hirs.utils.tpm.eventlog.events;


import lombok.Getter;
import java.io.UnsupportedEncodingException;

public class DeviceSecurityEventData extends DeviceSecurityEventDataBase {

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
