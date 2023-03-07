package hirs.utils.tpm.eventlog.events;

import java.nio.charset.StandardCharsets;

/**
 * Class to process the PC Client Firmware profile defined EV_S_CRTM_CONTENTS event.
 */
public class EvSCrtmContents {

    private String description = "";

   /**
     * Constructor that takes in the event data and waits to be called.
     * @param event byte array holding the event content data.
     */
    public EvSCrtmContents(final byte[] event) {
        scrtmContents(event);
    }

   /**
    * Checks if event data is null and if not it converts to a String.
    * @param event byte array holding the event data.
    * @return String contents contained within the event.
    */
    public String scrtmContents(final byte[] event) {
        if (event == null) {
            description = "invalid content event data";
            } else {
                description = new String(event, StandardCharsets.UTF_8);
            }
        return description;
     }

   /**
    * Human readable string contained within the CRTM Contents event.
    * @return Human readable string.
    */
    public String toString() {
        return description;
    }
}
