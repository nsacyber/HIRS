package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;
import lombok.Getter;

/**
 * Represents a symmetric key as specified in section 4.20 of the TCPA.
 */
@Getter
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

    /**
     * the algorithm used.
     */
    private int algorithmId;

    /**
     * the encryption scheme used.
     */
    private short encryptionScheme;

    /**
     * the algorithm used.
     */
    private short signatureScheme;

    /**
     * the size of the sub parameters block.
     */
    @StructElementLength(fieldName = "params")
    private int paramsSize;

    /**
     * the sub parameters block.
     */
    private SymmetricSubParams params;

}
