package hirs.structs.elements.aca;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;

import java.util.Arrays;

/**
 * A container for an encoded {@link hirs.structs.elements.tpm.IdentityRequest},
 * its associated endorsement credential, and its device information.
 */
@StructElements(elements = { "requestLength", "request",
    "endorsementCredentialModulusLength", "endorsementCredentialModulus",
    "endorsementCredentialLength", "endorsementCredential",
    "deviceInfoReportLength", "deviceInfoReport" })
public class IdentityRequestEnvelope implements Struct {

    @StructElementLength(fieldName = "request")
    private int requestLength;

    private byte[] request;

    @StructElementLength(fieldName = "endorsementCredentialModulus")
    private int endorsementCredentialModulusLength;

    private byte[] endorsementCredentialModulus;

    @StructElementLength(fieldName = "endorsementCredential")
    private int endorsementCredentialLength;

    private byte[] endorsementCredential;

    @StructElementLength(fieldName = "deviceInfoReport")
    private int deviceInfoReportLength;

    private byte[] deviceInfoReport;

    /**
     * @return the length of the identity request blob.
     */
    public int getRequestLength() {
        return requestLength;
    }

    /**
     * @return the identity request.
     */
    public byte[] getRequest() {
        return Arrays.copyOf(request, request.length);
    }

    /**
     * @return the length of the endorsementCredentialModulus blob
     */
    public int getEndorsementCredentialModulusLength() {
        return endorsementCredentialModulusLength;
    }

    /**
     * @return the endorsementCredentialModulus blob.
     */
    public byte[] getEndorsementCredentialModulus() {
        return Arrays.copyOf(endorsementCredentialModulus, endorsementCredentialModulus.length);
    }

    /**
     * @return the length of the endorsementCredential blob
     */
    public int getEndorsementCredentialLength() {
        return endorsementCredentialLength;
    }

    /**
     *
     * @return the endorsementCredential
     */
    public byte[] getEndorsementCredential() {
        return Arrays.copyOf(endorsementCredential, endorsementCredential.length);
    }

    /**
     *
     * @return the length of the device info report
     */
    public int getDeviceInfoReportLength() {
        return deviceInfoReportLength;
    }

    /**
     *
     * @return the device info report
     */
    public byte[] getDeviceInfoReport() {
        return Arrays.copyOf(deviceInfoReport, deviceInfoReport.length);
    }
}
