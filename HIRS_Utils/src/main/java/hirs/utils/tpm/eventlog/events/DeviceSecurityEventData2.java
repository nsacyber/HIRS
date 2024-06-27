package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import lombok.Getter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static hirs.utils.tpm.eventlog.events.DeviceSecurityEventDataHeader2.SUBHEADERTYPE_CERT_CHAIN;
import static hirs.utils.tpm.eventlog.events.DeviceSecurityEventDataHeader2.SUBHEADERTYPE_MEAS_BLOCK;

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
public class DeviceSecurityEventData2 extends DeviceSecurityEvent {

    /**
     * DeviceSecurityEventDataHeader Object.
     */
    @Getter
    private DeviceSecurityEventDataHeader2 dsedHeader2 = null;

    /**
     * DeviceSecurityEventDataSubHeader Object.
     */
    @Getter
    private DeviceSecurityEventDataSubHeader dsedSubHeader = null;

    /**
     * Human readable description of the data within the
     * DEVICE_SECURITY_EVENT_DATA_SUB_HEADER. SUB_HEADER can be either
     * DEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_MEASUREMENT_BLOCK or
     * DEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_CERT_CHAIN
     */
    @Getter
    String subHeaderInfo = "";

    /**
     * DeviceSecurityEventData2 Constructor.
     *
     * @param dSEDbytes byte array holding the DeviceSecurityEventData2.
     */
    public DeviceSecurityEventData2(final byte[] dSEDbytes) throws IOException {

        dsedHeader2 = new DeviceSecurityEventDataHeader2(dSEDbytes);
        int dSEDheaderByteSize = dsedHeader2.getDSEDheaderByteSize();
        int subHeaderType = dsedHeader2.getSubHeaderType();
        int subHeaderLength = dsedHeader2.getSubHeaderLength();

        subHeaderInfo = "\nSub header type: " + subHeaderType;

        byte[] dSEDsubHeaderBytes = new byte[subHeaderLength];
        System.arraycopy(dSEDbytes, dSEDheaderByteSize, dSEDsubHeaderBytes, 0, subHeaderLength);

        if (subHeaderType == SUBHEADERTYPE_MEAS_BLOCK) {
            dsedSubHeader = new DeviceSecurityEventDataSubHeaderSpdmMeasurementBlock(dSEDsubHeaderBytes);
            subHeaderInfo += dsedSubHeader.toString();
        }
        else if (subHeaderType == SUBHEADERTYPE_CERT_CHAIN) {
            // TBD:
            // dsedSubHeader = new DeviceSecurityEventDataSubHeaderCertChain();
        }
        else {
            subHeaderInfo += "Subheader type unknown";
        }

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
        dsedInfo += dsedHeader2.toString();
        dsedInfo += dsedSubHeader.toString();
        dsedInfo += getDeviceContextInfo();
        return dsedInfo;
    }
}
