package hirs.tpm.eventlog.uefi;

/**
 * Class to process a UEFI BootOrder variable.
 * UEFI spec version 2.8 section 3.3 on page 83 defines the Boot Order as:
 *     an array of UINT16s that make up an ordered list of the Boot#### options.
 */
public class UefiBootOrder {
  /** list of UINT16 Boot#### numbers.*/
  private char[] bootOrder = null;

  /**
   * Process the BootOrder UEFI variable.
   * @param order byte array holding the UEFI boot order variable.
   */
  UefiBootOrder(final byte[] order) {
    bootOrder = new char[order.length / UefiConstants.SIZE_2];
    for (int i = 0; i < order.length; i = i + UefiConstants.SIZE_2) {
        bootOrder[i / UefiConstants.SIZE_2] =
                (char) (order[i + 1] * UefiConstants.SIZE_256 + order[i]);
      }
  }

/**
 * Provides a human readable Boot Order list on single line.
 * @return A human readable Boot Order
 */
public String toString() {
 StringBuffer orderList = new StringBuffer();
 orderList.append("BootOrder = ");
 for (int i = 0; i < bootOrder.length; i++) {
    int order = bootOrder[i];
    orderList.append(" Boot" + String.format("%04d", order));
 }
 return orderList.toString();
 }
}
