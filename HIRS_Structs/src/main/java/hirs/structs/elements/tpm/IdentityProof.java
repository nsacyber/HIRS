package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;
import lombok.Getter;

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

    /**
     * version of the TPM that created this data structure.
     */
    @Getter
    private Version version;

    /**
     * the size of the label area.
     */
    @Getter
    @StructElementLength(fieldName = "label")
    private int labelSize;

    /**
     * the size of the identity binding area.
     */
    @Getter
    @StructElementLength(fieldName = "identityBinding")
    private int identityBindingSize;

    /**
     * the size of the endorsement credential.
     */
    @Getter
    @StructElementLength(fieldName = "endorsementCredential")
    private int endorsementSize;

    /**
     * the size of the endorsement credential.
     */
    @Getter
    @StructElementLength(fieldName = "platformCredential")
    private int platformSize;

    /**
     * the size of the conformance credential.
     */
    @Getter
    @StructElementLength(fieldName = "conformanceCredential")
    private int conformanceSize;

    /**
     * public key of the new identity.
     */
    @Getter
    private AsymmetricPublicKey identityKey;

    private byte[] label;

    private byte[] identityBinding;

    private byte[] endorsementCredential;

    private byte[] platformCredential;

    private byte[] conformanceCredential;

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
