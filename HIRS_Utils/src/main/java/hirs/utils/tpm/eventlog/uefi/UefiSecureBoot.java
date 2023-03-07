package hirs.utils.tpm.eventlog.uefi;

import lombok.Getter;

import java.math.BigInteger;

/**
 * Class that processes the UEFI defined SecureBoot Variable.
 * Currently this variable only specifies if SecureBoot is on/off.
 */
public class UefiSecureBoot {
    /**
     * Variable value.
     */
    @Getter
    private int secureBootVariable = 0;
    /**
     * Error flag.
     */
    private boolean berror = false;
    /**
     * Human readable description.
     */
    private String info = "";

    /**
     * Constructor to process the EFI Secure Boot Variable.
     *
     * @param data UEFI variable data.
     */
    public UefiSecureBoot(final byte[] data) {
        if (data.length == 0) {
            berror = true;
            info = "Unknown State: Empty Secure Boot variable\n";
        } else {
            secureBootVariable = new BigInteger(data).intValue();
        }
    }

    /**
     * Provides a human readable value for the Secure Boot variable.
     *
     * @return Human readable description.
     */
    public String toString() {
        if (!berror) {
            if (secureBootVariable == 1) {
                info += " Secure Boot is enabled   ";
            } else if (secureBootVariable == 0) {
                info += " Secure Boot is NOT enabled   ";
            } else {
                info += " Unkown State: Secure Variable is undefined   ";
            }
        }
        return info;
    }
}
