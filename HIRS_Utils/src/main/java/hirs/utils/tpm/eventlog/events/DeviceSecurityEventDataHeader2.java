package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import lombok.Getter;

/**
 * Class to process the DEVICE_SECURITY_EVENT_DATA_HEADER2.
 * DEVICE_SECURITY_EVENT_DATA_HEADER2 contains the measurement(s) and hash algorithm identifier
 * returned by the SPDM "GET_MEASUREMENTS" function.
 *
 * HEADERS defined by PFP v1.06 Rev 52:
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_HEADER2 {
 *      UINT8                           Signature[16];
 *      UINT16                          Version;
 *      UINT8                           AuthState;
 *      UINT8                           Reserved
 *      UINT32                          Length;
 *      UINT32                          DeviceType;
 *      UINT32                          SubHeaderType;
 *      UINT32                          SubHeaderLength;
 *      UINT64                          SubHeaderUID;
 *      UINT64                          DevicePathLength;
 *      UNIT8                           DevicePath[DevicePathLength]
 * } DEVICE_SECURITY_EVENT_DATA_HEADER2;
 * <p>
 */
public class DeviceSecurityEventDataHeader2 extends DeviceSecurityEventHeader {

    /**
     * Event auth state
     */
    @Getter
    private int authState = 0;
    /**
     * Event data length.
     */
    @Getter
    private int length = 0;
    /**
     * Event sub headerType
     * SUBHEADERTYPE_MEAS_BLOCK = 0
     * SUBHEADERTYPE_CERT_CHAIN = 1
     */
    @Getter
    private int subHeaderType = -1;
    /**
     * Event sub header length.
     */
    @Getter
    private int subHeaderLength = 0;
    /**
     * Event sub header UID.
     */
    @Getter
    private String subHeaderUid = "";

    /**
     * Auth state - success
     */
    public static final int AUTH_SUCCESS = 0;
    /**
     * Auth state - digital signature of the data is valid, but the public key certificate chain is not
     *              validated with the entry in in the UEFI device signature variable
     */
    public static final int AUTH_NO_AUTHORITY = 1;
    /**
     * Auth state - digital signature of the measurement data is valid, but the reported device capabilities,
     *              negotiated parameters or certificate chains were not validated by a transcript.
     */
    public static final int AUTH_NO_BINDING = 2;
    /**
     * Auth state - data has no digital signature
     */
    public static final int AUTH_FAIL_NO_SIG = 3;
    /**
     * Auth state - data is invalid
     */
    public static final int AUTH_FAIL_INVALID = 4;
    /**
     * Auth state - device is not an SPDM-capable device
     */
    public static final int AUTH_NO_SPDM = 0xFF;

    /**
     * DeviceSecurityEventDataHeader2 Constructor.
     *
     * @param dsedBytes byte array holding the DeviceSecurityEventData2.
     */
    public DeviceSecurityEventDataHeader2(final byte[] dsedBytes) {

        super(dsedBytes);

        byte[] authStateBytes = new byte[1];
        System.arraycopy(dsedBytes, 18, authStateBytes, 0, 1);
        authState = HexUtils.leReverseInt(authStateBytes);

        // byte[] reserved[Bytes]: 1 byte

        byte[] lengthBytes = new byte[4];
        System.arraycopy(dsedBytes, 20, lengthBytes, 0, 4);
        length = HexUtils.leReverseInt(lengthBytes);

        extractDeviceType(dsedBytes, 24);

        byte[] subHeaderTypeBytes = new byte[4];
        System.arraycopy(dsedBytes, 28, subHeaderTypeBytes, 0, 4);
        subHeaderType = HexUtils.leReverseInt(subHeaderTypeBytes);

        byte[] subHeaderLengthBytes = new byte[4];
        System.arraycopy(dsedBytes, 32, subHeaderLengthBytes, 0, 4);
        subHeaderLength = HexUtils.leReverseInt(subHeaderLengthBytes);

        byte[] subHeaderUidBytes = new byte[8];
        System.arraycopy(dsedBytes, 36, subHeaderUidBytes, 0, 8);
        subHeaderUidBytes = HexUtils.leReverseByte(subHeaderUidBytes);
        subHeaderUid = HexUtils.byteArrayToHexString(subHeaderUidBytes);

        int devPathLenStartByte = 44;
        extractDevicePathAndFinalSize(dsedBytes, devPathLenStartByte);
    }

    /**
     * Returns a human-readable description of the data within this structure.
     *
     * @return a description of this structure.
     */
    public String toString() {
        String dsedHeader2Info = super.toString();
        dsedHeader2Info += "   AuthState: " + getAuthStateString() + "\n";
        dsedHeader2Info += "   Sub header UID: " + subHeaderUid + "\n";

        return dsedHeader2Info;
    }

    /**
     * Returns a human-readable description of auth state based on numeric representation lookup.
     *
     * @return a description of the auth state.
     */
    public String getAuthStateString() {

        switch (authState) {
            case AUTH_SUCCESS:
                return ("AUTH_SUCCESS");
            case AUTH_NO_AUTHORITY:
                return ("AUTH_NO_AUTHORITY");
            case AUTH_NO_BINDING:
                return ("AUTH_NO_BINDING");
            case AUTH_FAIL_NO_SIG:
                return ("AUTH_FAIL_NO_SIG");
            case AUTH_FAIL_INVALID:
                return ("AUTH_FAIL_INVALID");
            case AUTH_NO_SPDM:
                return ("AUTH_NO_SPDM");
            default:
                return ("Auth State unknown");
        }
    }
}
