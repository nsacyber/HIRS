package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import lombok.Getter;

/**
 * Class to process the DEVICE_SECURITY_EVENT_DATA_HEADER2.
 * DEVICE_SECURITY_EVENT_DATA_HEADER2 contains the measurement(s) and hash algorithm identifier
 * returned by the SPDM "GET_MEASUREMENTS" function.
 * <p>
 * HEADERS defined by PFP v1.06 Rev 52:
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_HEADER2 {
 * .    UINT8                           Signature[16];
 * .    UINT16                          Version;
 * .    UINT8                           AuthState;
 * .    UINT8                           Reserved
 * .    UINT32                          Length;
 * .    UINT32                          DeviceType;
 * .    UINT32                          SubHeaderType;
 * .    UINT32                          SubHeaderLength;
 * .    UINT64                          SubHeaderUID;
 * .    UINT64                          DevicePathLength;
 * .    UNIT8                           DevicePath[DevicePathLength]
 * } DEVICE_SECURITY_EVENT_DATA_HEADER2;
 * <p>
 */
public class DeviceSecurityEventDataHeader2 extends DeviceSecurityEventHeader {

    /**
     * Auth state - success.
     */
    public static final int AUTH_SUCCESS = 0;
    /**
     * Auth state - digital signature of the data is valid, but the public key certificate chain is not
     * validated with the entry in the UEFI device signature variable.
     */
    public static final int AUTH_NO_AUTHORITY = 1;
    /**
     * Auth state - digital signature of the measurement data is valid, but the reported device capabilities,
     * negotiated parameters or certificate chains were not validated by a transcript.
     */
    public static final int AUTH_NO_BINDING = 2;
    /**
     * Auth state - data has no digital signature.
     */
    public static final int AUTH_FAIL_NO_SIG = 3;
    /**
     * Auth state - data is invalid.
     */
    public static final int AUTH_FAIL_INVALID = 4;
    /**
     * Auth state - device is not an SPDM-capable device.
     */
    public static final int AUTH_NO_SPDM = 0xFF;
    /**
     * Event auth state.
     */
    @Getter
    private int authState = 0;
    /**
     * Event data length.
     */
    @Getter
    private int length = 0;
    /**
     * Event sub headerType.
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
     * DeviceSecurityEventDataHeader2 Constructor.
     *
     * @param dsedBytes byte array holding the DeviceSecurityEventData2.
     */
    public DeviceSecurityEventDataHeader2(final byte[] dsedBytes) {

        super(dsedBytes);

        final int dsedBytesSrcIndex = 18;
        byte[] authStateBytes = new byte[1];
        System.arraycopy(dsedBytes, dsedBytesSrcIndex, authStateBytes, 0, 1);
        authState = HexUtils.leReverseInt(authStateBytes);

        // byte[] reserved[Bytes]: 1 byte

        final int dsedBytesSrcIndex2 = 20;
        final int lengthBytesSize = 4;
        byte[] lengthBytes = new byte[lengthBytesSize];
        System.arraycopy(dsedBytes, dsedBytesSrcIndex2, lengthBytes, 0, lengthBytesSize);
        length = HexUtils.leReverseInt(lengthBytes);

        final int dsedBytesStartByte1 = 24;
        extractDeviceType(dsedBytes, dsedBytesStartByte1);

        final int dsedBytesSrcIndex3 = 28;
        final int subHeaderTypeBytesSize = 4;
        byte[] subHeaderTypeBytes = new byte[subHeaderTypeBytesSize];
        System.arraycopy(dsedBytes, dsedBytesSrcIndex3, subHeaderTypeBytes, 0, subHeaderTypeBytesSize);
        subHeaderType = HexUtils.leReverseInt(subHeaderTypeBytes);

        final int dsedBytesSrcIndex4 = 32;
        final int subHeaderLengthBytesSize = 4;
        byte[] subHeaderLengthBytes = new byte[subHeaderLengthBytesSize];
        System.arraycopy(dsedBytes, dsedBytesSrcIndex4, subHeaderLengthBytes, 0, subHeaderLengthBytesSize);
        subHeaderLength = HexUtils.leReverseInt(subHeaderLengthBytes);

        final int dsedBytesSrcIndex5 = 36;
        final int subHeaderUidBytesSize = 8;
        byte[] subHeaderUidBytes = new byte[subHeaderUidBytesSize];
        System.arraycopy(dsedBytes, dsedBytesSrcIndex5, subHeaderUidBytes, 0, subHeaderUidBytesSize);
        subHeaderUidBytes = HexUtils.leReverseByte(subHeaderUidBytes);
        subHeaderUid = HexUtils.byteArrayToHexString(subHeaderUidBytes);

        final int dsedBytesStartByte2 = 44;
        extractDevicePathAndFinalSize(dsedBytes, dsedBytesStartByte2);
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
        return switch (authState) {
            case AUTH_SUCCESS -> ("AUTH_SUCCESS");
            case AUTH_NO_AUTHORITY -> ("AUTH_NO_AUTHORITY");
            case AUTH_NO_BINDING -> ("AUTH_NO_BINDING");
            case AUTH_FAIL_NO_SIG -> ("AUTH_FAIL_NO_SIG");
            case AUTH_FAIL_INVALID -> ("AUTH_FAIL_INVALID");
            case AUTH_NO_SPDM -> ("AUTH_NO_SPDM");
            default -> ("Auth State unknown");
        };
    }
}
