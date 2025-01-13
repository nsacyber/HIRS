package hirs.structs.elements.aca;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;
import lombok.Getter;

import java.util.Arrays;

/**
 * A container for an encoded {@link hirs.structs.elements.tpm.IdentityRequest},
 * its associated endorsement credential, and its device information.
 */
@StructElements(elements = {"requestLength", "request",
        "endorsementCredentialModulusLength", "endorsementCredentialModulus",
        "endorsementCredentialLength", "endorsementCredential",
        "deviceInfoReportLength", "deviceInfoReport"})
public class IdentityRequestEnvelope implements Struct {

    /**
     * the length of the identity request blob.
     */
    @Getter
    @StructElementLength(fieldName = "request")
    private int requestLength;

    private byte[] request;

    /**
     * the length of the endorsementCredentialModulus blob.
     */
    @Getter
    @StructElementLength(fieldName = "endorsementCredentialModulus")
    private int endorsementCredentialModulusLength;

    private byte[] endorsementCredentialModulus;

    /**
     * the length of the endorsementCredential blob.
     */
    @Getter
    @StructElementLength(fieldName = "endorsementCredential")
    private int endorsementCredentialLength;

    private byte[] endorsementCredential;

    /**
     * the length of the device info report.
     */
    @Getter
    @StructElementLength(fieldName = "deviceInfoReport")
    private int deviceInfoReportLength;

    private byte[] deviceInfoReport;

    /**
     * @return the identity request.
     */
    public byte[] getRequest() {
        return Arrays.copyOf(request, request.length);
    }

    /**
     * @return the endorsementCredentialModulus blob.
     */
    public byte[] getEndorsementCredentialModulus() {
        return Arrays.copyOf(endorsementCredentialModulus, endorsementCredentialModulus.length);
    }

    /**
     * @return the endorsementCredential
     */
    public byte[] getEndorsementCredential() {
        return Arrays.copyOf(endorsementCredential, endorsementCredential.length);
    }

    /**
     * @return the device info report
     */
    public byte[] getDeviceInfoReport() {
        return Arrays.copyOf(deviceInfoReport, deviceInfoReport.length);
    }
}
