package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;

/**
 * Represents a symmetric key as specified in section 4.20 of the TCPA.
 */
@StructElements(elements = {"algorithmId", "encryptionScheme", "signatureScheme", "paramsSize",
        "params"})
public class SymmetricKeyParams implements Struct {

    /**
     * Algorithm ID for AES encryption.
     */
    public static final int ALGORITHM_AES = 6;

    /**
     * Scheme ID for CBC.
     */
    public static final short SCHEME_CBC_PKCS5PADDING = 0x1;

    private int algorithmId;

    private short encryptionScheme;

    private short signatureScheme;

    @StructElementLength(fieldName = "params")
    private int paramsSize;

    private SymmetricSubParams params;

    /**
     * @return the algorithm used.
     */
    public int getAlgorithmId() {
        return algorithmId;
    }

    /**
     * @return the encryption scheme used.
     */
    public short getEncryptionScheme() {
        return encryptionScheme;
    }

    /**
     * @return the algorithm used.
     */
    public short getSignatureScheme() {
        return signatureScheme;
    }

    /**
     * @return the size of the sub parameters block.
     */
    public int getParamsSize() {
        return paramsSize;
    }

    /**
     * @return the sub parameters block.
     */
    public SymmetricSubParams getParams() {
        return params;
    }
}
