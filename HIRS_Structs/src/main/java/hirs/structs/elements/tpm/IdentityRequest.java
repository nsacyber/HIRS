package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;

import java.util.Arrays;

/**
 * As specified in TCPA 4.30.2 specification. This structure is sent to the Attestation Certificate
 * Authority to create an Identity Credential.
 */
@StructElements(elements = {"asymmetricBlobSize", "symmetricBlobSize", "asymmetricAlgorithm",
        "symmetricAlgorithm", "asymmetricBlob", "symmetricBlob"})
public class IdentityRequest implements Struct {

    @StructElementLength(fieldName = "asymmetricBlob")
    private int asymmetricBlobSize;

    @StructElementLength(fieldName = "symmetricBlob")
    private int symmetricBlobSize;

    private AsymmetricKeyParams asymmetricAlgorithm;

    private SymmetricKeyParams symmetricAlgorithm;

    private byte[] asymmetricBlob;

    private byte[] symmetricBlob;

    /**
     * @return the size of the asymmetric encrypted area
     */
    public int getAsymmetricBlobSize() {
        return asymmetricBlobSize;
    }

    /**
     * @return the size of the symmetric encrypted area
     */
    public int getSymmetricBlobSize() {
        return symmetricBlobSize;
    }

    /**
     * @return the parameters for the asymmetric algorithm used to create the asymmetricBlob
     */
    public AsymmetricKeyParams getAsymmetricAlgorithm() {
        return asymmetricAlgorithm;
    }

    /**
     * @return the parameters for the symmetric algorithm used to create the asymmetricBlob
     */
    public SymmetricKeyParams getSymmetricAlgorithm() {
        return symmetricAlgorithm;
    }

    /**
     * @return encrypted asymmetric area
     */
    public byte[] getAsymmetricBlob() {
        return Arrays.copyOf(asymmetricBlob, asymmetricBlob.length);
    }

    /**
     * @return encrypted symmetric area
     */
    public byte[] getSymmetricBlob() {
        return Arrays.copyOf(symmetricBlob, symmetricBlob.length);
    }

    /**
     * Sets the value of the encrypted symmetric blob.
     *
     * @param symmetricBlob new value
     */
    public void setSymmetricBlob(final byte[] symmetricBlob) {
        this.symmetricBlob = symmetricBlob;
    }
}
