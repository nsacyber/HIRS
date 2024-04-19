package hirs.utils.tpm.eventlog.events;

import lombok.Getter;

public class DeviceSecurityEventData2 extends DeviceSecurityEventDataBase {

    /**
     * DeviceSecurityEventDataHeader2 Object.
     */
    @Getter
    private DeviceSecurityEventDataHeader2 dsedHeader2 = null;
//    /**
//     * DeviceSecurityEventDataSubHeader Object.
//     */
//    @Getter
//    private DeviceSecurityEventDataSubHeader dsedSubHeader = null;

    /**
     * DeviceSecurityEventData2 Constructor.
     *
     * @param dSEDbytes byte array holding the DeviceSecurityEventData.
     */
    public DeviceSecurityEventData2(final byte[] dSEDbytes) {

    }

    public String toString() {
        String dsedInfo = "";
//        dsedInfo += dsedHeader2.toString();
//                dsedInfo += dsedSubHeader.toString();
//      dsedInfo += dsedDeviceContext.toString();
        return dsedInfo;
    }
}
