package hirs.tpm.eventlog.uefi;

import java.math.BigInteger;

/**
 * Class that processes the UEFI defined SecureBoot Variable.
 * Currently this variable only specifies if SecureBoot is on/off.
 */
public class UefiSecureBoot {
  /** Variable value. */
  private int secureBootVar = 0;
  /** Error flag.*/
  private boolean berror = false;
  /** Human readable description. */
  private String info = "";

/**
 * Constructor to process the EFI Secure Boot Variable.
 * @param data  UEFI variable data.
 */
public UefiSecureBoot(final byte[] data) {
  if (data.length == 0) {
    berror = true;
    info = "Unkown State: Empty Secure Boot variable\n";
  } else {
    secureBootVar = new BigInteger(data).intValue();
  }
 }

/**
 * Return the value of the Secure Boot Variable.
 * Current defined values are 1 = On and 0=off.
 * @return Integer value of the Secure Boot Variable.
 */
public int getSecurBootVariable() {
  return secureBootVar;
}

/**
 * Provides a human readable value for the Secure Boot variable.
 * @return Human readable description.
 */
public String toString() {
  if (!berror) {
  if (secureBootVar == 1) {
      info += "    Secure Boot is enabled \n";
   } else if (secureBootVar == 0) {
      info += "    Secure Boot is NOT enabled \n";
   } else {
      info += " Unkown State: Secure Variable is undefined \n";
   }
  }
 return info;
 }
}
