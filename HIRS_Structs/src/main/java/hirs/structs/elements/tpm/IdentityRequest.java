package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

/**
 * As specified in TCPA 4.30.2 specification. This structure is sent to the Attestation Certificate
 * Authority to create an Identity Credential.
 */
@StructElements(elements = {"asymmetricBlobSize", "symmetricBlobSize", "asymmetricAlgorithm",
        "symmetricAlgorithm", "asymmetricBlob", "symmetricBlob"})
public class IdentityRequest implements Struct {

    /**
     * the size of the asymmetric encrypted area.
     */
    @Getter
    @StructElementLength(fieldName = "asymmetricBlob")
    private int asymmetricBlobSize;

    /**
     * the size of the symmetric encrypted area.
     */
    @Getter
    @StructElementLength(fieldName = "symmetricBlob")
    private int symmetricBlobSize;

    /**
     * the parameters for the asymmetric algorithm used to create the asymmetricBlob.
     */
    @Getter
    private AsymmetricKeyParams asymmetricAlgorithm;

    /**
     * the parameters for the symmetric algorithm used to create the asymmetricBlob.
     */
    @Getter
    private SymmetricKeyParams symmetricAlgorithm;

    private byte[] asymmetricBlob;

    /**
     * the value of the encrypted symmetric blob.
     */
    @Setter
    private byte[] symmetricBlob;

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

}
