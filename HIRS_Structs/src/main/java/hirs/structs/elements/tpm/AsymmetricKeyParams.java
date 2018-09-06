package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;

/**
 * As defined in TCPA 4.20, the key parameters data structure describes the parameters used to
 * generate a key pair and to store the parts of a key.
 */
@StructElements(elements = { "algorithmId", "encryptionScheme", "signatureScheme", "paramsSize",
        "params" })
public class AsymmetricKeyParams implements Struct {

    private int algorithmId;

    private short encryptionScheme;

    private short signatureScheme;

    @StructElementLength(fieldName = "params")
    private int paramsSize;

    private RsaSubParams params;

    /**
     * @return the key algorithm
     */
    public int getAlgorithmId() {
        return algorithmId;
    }

    /**
     * @return the size of the params field
     */
    public int getParamsSize() {
        return paramsSize;
    }

    /**
     * @return the encryption scheme that the key uses
     */
    public short getEncryptionScheme() {
        return encryptionScheme;
    }

    /**
     * @return the signature scheme that the key uses to perform digital signatures
     */
    public short getSignatureScheme() {
        return signatureScheme;
    }

    /**
     * @return parameter information dependant upon the key algorithm.
     */
    public RsaSubParams getParams() {
        return params;
    }
}
