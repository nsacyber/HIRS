package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElements;

/**
 * As specified in TCPA Main Specification section 4.27.3. This structure contains the public
 * portion of an asymmetric key pair. It contains all the information necessary for it's unambiguous
 * usage.
 */
@StructElements(elements = { "asymmetricKeyParams", "storePubKey" })
public class PublicKey implements Struct {

    private AsymmetricKeyParams asymmetricKeyParams;

    private StorePubKey storePubKey;

    /**
     * @return information regarding this key
     */
    public AsymmetricKeyParams getAsymmetricKeyParams() {
        return asymmetricKeyParams;
    }

    /**
     * @return the public as described by the key parameters.
     */
    public StorePubKey getStorePubKey() {
        return storePubKey;
    }
}
