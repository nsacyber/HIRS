package hirs.utils.rim.unsignedRim.cbor.tcgCompRimCoswid;

import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.CoswidConfigValidator;
import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.CoswidItems;

/**
 * Class that validates the CoSwid variation of the TCG Component RIM.
 */
public class TcgCompRimCoswidValidator extends CoswidConfigValidator {

    private final TcgCompRimCoswid tcgCompRef = new TcgCompRimCoswid();

    /**
     * Default constructor for TcgCompRimCoswidValidator.
     */
    public TcgCompRimCoswidValidator() {

    }

    /**
     * Checks a single entry against a set of rfc 9393 define item names.
     * @param key specific item to check
     * @return true if valid
     */
    @Override
    protected boolean isValidKey(final String key) {
        int index = tcgCompRef.lookupIndex(key);
        boolean validity = true;
        if (index == CoswidItems.UNKNOWN_INT) {
            validity = false;
            invalidFields += key + " ";
            invalidFieldCount++;
        }
        return validity;
    }
}
