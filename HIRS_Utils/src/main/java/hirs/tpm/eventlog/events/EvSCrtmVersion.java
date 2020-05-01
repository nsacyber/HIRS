package hirs.tpm.eventlog.events;

import java.io.UnsupportedEncodingException;

import hirs.tpm.eventlog.uefi.UefiConstants;
import hirs.tpm.eventlog.uefi.UefiGuid;
import hirs.utils.HexUtils;

/**
 * Class to process the PC Client Firmware profile defined EV_S_CRTM_VERSION event.
 */
public class EvSCrtmVersion {

   private String description = "";

   /**
    * Constructor that takes in the event data and waits to be called.
    * @param event byte array holding the event content data.
    * @throws UnsupportedEncodingException  if parsing issues exist.
    */
    public EvSCrtmVersion(final byte[] event) throws UnsupportedEncodingException {
      sCrtmVersion(event);
    }

   /**
    * Checks if event data is null and if not it converts to a String.
    * @param data byte array holding the vent content.
    * @throws UnsupportedEncodingException if parsing issues exist.
    * @return String representation of the version.
    */
   public String sCrtmVersion(final byte[] data) throws UnsupportedEncodingException {
       UefiGuid guid = null;
       if (data == null) {
           description = "invalid content event data";
           } else {
               if (data.length == UefiConstants.SIZE_16) {
                   if (UefiGuid.isValidUUID(data)) {
                       guid = new UefiGuid(data);
                       String guidInfo = guid.toStringNoLookup();
                       description =  " SCRM Version = " + guidInfo;
                       }
                   } else if (data.length < UefiConstants.SIZE_4) {
                       description = HexUtils.byteArrayToHexString(data);
                   } else if (EvPostCode.isAscii(data)) {
                       description = new String(data, "UTF-8");
                   } else {
                       description = "Unknown Version format";
                   }
       }
      return (description);
    }

   /**
    * Return function to send data to the toString.
    * @return String representation of the version.
    */
    public String toString() {
        return description;
    }
}
