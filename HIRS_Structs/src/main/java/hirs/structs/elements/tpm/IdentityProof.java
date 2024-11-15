package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;

import java.util.Arrays;

/**
 * As defined in the TCPA 4.30.3 specification. A structure that is generated during the collate
 * identity process. This structure contains information that is required by the Attestation
 * Certificate Authority to attest an identity request.
 */
@StructElements(elements = {"version", "labelSize", "identityBindingSize", "endorsementSize",
        "platformSize", "conformanceSize", "identityKey", "label", "identityBinding",
        "endorsementCredential", "platformCredential", "conformanceCredential"})
public class IdentityProof implements Struct {

    private Version version;

    @StructElementLength(fieldName = "label")
    private int labelSize;

    @StructElementLength(fieldName = "identityBinding")
    private int identityBindingSize;

    @StructElementLength(fieldName = "endorsementCredential")
    private int endorsementSize;

    @StructElementLength(fieldName = "platformCredential")
    private int platformSize;

    @StructElementLength(fieldName = "conformanceCredential")
    private int conformanceSize;

    private AsymmetricPublicKey identityKey;

    private byte[] label;

    private byte[] identityBinding;

    private byte[] endorsementCredential;

    private byte[] platformCredential;

    private byte[] conformanceCredential;

    /**
     * @return version of the TPM that created this data structure
     */
    public Version getVersion() {
        return version;
    }

    /**
     * @return the size of the label area
     */
    public int getLabelSize() {
        return labelSize;
    }

    /**
     * @return the size of the identity binding area
     */
    public int getIdentityBindingSize() {
        return identityBindingSize;
    }

    /**
     * @return the size of the endorsement credential
     */
    public int getEndorsementSize() {
        return endorsementSize;
    }

    /**
     * @return the size of the endorsement credential
     */
    public int getPlatformSize() {
        return platformSize;
    }

    /**
     * @return the size of the conformance credential
     */
    public int getConformanceSize() {
        return conformanceSize;
    }

    /**
     * @return public key of the new identity
     */
    public AsymmetricPublicKey getIdentityKey() {
        return identityKey;
    }

    /**
     * @return label of the identity
     */
    public byte[] getLabel() {
        return Arrays.copyOf(label, label.length);
    }

    /**
     * @return the signature value of the identity contents that is generated using the 'Make
     * Identity' command
     */
    public byte[] getIdentityBinding() {
        return Arrays.copyOf(identityBinding, identityBinding.length);
    }

    /**
     * @return the TPM endorsement credential
     */
    public byte[] getEndorsementCredential() {
        return Arrays.copyOf(endorsementCredential, endorsementCredential.length);
    }

    /**
     * @return the TPM platform credential
     */
    public byte[] getPlatformCredential() {
        return Arrays.copyOf(platformCredential, platformCredential.length);
    }

    /**
     * @return the TPM conformance credential
     */
    public byte[] getConformanceCredential() {
        return Arrays.copyOf(conformanceCredential, conformanceCredential.length);
    }
}
