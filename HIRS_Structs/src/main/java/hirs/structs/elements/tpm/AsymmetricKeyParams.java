package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;
import lombok.Getter;

/**
 * As defined in TCPA 4.20, the key parameters data structure describes the parameters used to
 * generate a key pair and to store the parts of a key.
 */
@Getter
@StructElements(elements = {"algorithmId", "encryptionScheme", "signatureScheme", "paramsSize",
        "params"})
public class AsymmetricKeyParams implements Struct {

    /**
     * the key algorithm.
     */
    private int algorithmId;

    /**
     * the encryption scheme that the key uses.
     */
    private short encryptionScheme;

    /**
     * the signature scheme that the key uses to perform digital signatures.
     */
    private short signatureScheme;

    /**
     * the size of the params field.
     */
    @StructElementLength(fieldName = "params")
    private int paramsSize;

    /**
     * parameter information dependant upon the key algorithm.
     */
    private RsaSubParams params;

}
