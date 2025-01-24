package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElements;
import lombok.Getter;

/**
 * As specified in TCPA Main Specification section 4.27.3. This structure contains the public
 * portion of an asymmetric key pair. It contains all the information necessary for it's unambiguous
 * usage.
 */
@Getter
@StructElements(elements = {"asymmetricKeyParams", "storePubKey"})
public class PublicKey implements Struct {

    /**
     * information regarding this key.
     */
    private AsymmetricKeyParams asymmetricKeyParams;

    /**
     * the public as described by the key parameters.
     */
    private StorePubKey storePubKey;

}
