package hirs.utils.tpm.eventlog.events;

import lombok.Getter;

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
     * Human-readable description of the data within the
     * DEVICE_SECURITY_EVENT_DATA_HEADER2.
     */
    @Getter
    String headerInfo = "";

    /**
     * Human-readable description of the data within the
     * DEVICE_SECURITY_EVENT_DATA_SUB_HEADER. SUB_HEADER can be either
     * DEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_MEASUREMENT_BLOCK or
     * DEVICE_SECURITY_EVENT_DATA_SUB_HEADER_SPDM_CERT_CHAIN
     */
    @Getter
    String subHeaderInfo = "";

    /**
     * DeviceSecurityEventData2 Constructor.
     *
     * @param dsedBytes byte array holding the DeviceSecurityEventData2.
     */
    public DeviceSecurityEventData2(final byte[] dsedBytes) {

        try {
            dsedHeader2 = new DeviceSecurityEventDataHeader2(dsedBytes);
            headerInfo = dsedHeader2.toString();

            setDeviceType(dsedHeader2.getDeviceType());
            int dsedHeaderLength = dsedHeader2.getDsedHeaderLength();
            int subHeaderType = dsedHeader2.getSubHeaderType();
            int subHeaderLength = dsedHeader2.getSubHeaderLength();

            subHeaderInfo = "\n   Sub header type: "
                    + DeviceSecurityEventDataSubHeader.subheaderTypeToString(subHeaderType);

            byte[] dsedSubHeaderBytes = new byte[subHeaderLength];
            System.arraycopy(dsedBytes, dsedHeaderLength, dsedSubHeaderBytes, 0, subHeaderLength);

            if (subHeaderType == DeviceSecurityEventDataSubHeader.SUBHEADERTYPE_MEAS_BLOCK) {
                try {
                    dsedSubHeader =
                            new DeviceSecurityEventDataSubHeaderSpdmMeasurementBlock(dsedSubHeaderBytes);
                    subHeaderInfo += dsedSubHeader.toString();
                }
                catch(NullPointerException e) {
                    subHeaderInfo = "\n    Could not interpret Sub header info for SPDM measurment block";
                }
            }
            else if (subHeaderType == DeviceSecurityEventDataSubHeader.SUBHEADERTYPE_CERT_CHAIN) {
                subHeaderInfo += "\n    Cert chain to be implemented ";
                try {
                    dsedSubHeader =
                            new DeviceSecurityEventDataSubHeaderCertChain(dsedSubHeaderBytes);
                    subHeaderInfo += dsedSubHeader.toString();
                }
                catch(NullPointerException e) {
                    subHeaderInfo = "\n    Could not interpret Sub header info for SPDM measurement block";
                }
            }
            else {
                subHeaderInfo += "\n     Sub header type unknown";
            }

            int dsedDevContextStartByte = dsedHeaderLength + subHeaderLength;
            int dsedDevContextLength = dsedBytes.length - dsedDevContextStartByte;
            byte[] dsedDevContextBytes = new byte[dsedDevContextLength];
            System.arraycopy(dsedBytes, dsedDevContextStartByte, dsedDevContextBytes, 0,
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
        dsedInfo += subHeaderInfo;
        dsedInfo += getDeviceContextInfo();
        return dsedInfo;
    }
}
