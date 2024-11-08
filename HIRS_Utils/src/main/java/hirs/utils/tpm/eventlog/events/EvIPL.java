package hirs.utils.tpm.eventlog.events;

import java.nio.charset.StandardCharsets;

/**
 * Processes event type EV_IPL which is deprecated in the current spec,
 * but defined in older version of the specification(1.0.0) as contain
 * "informative information about the IPL code" (ascii strings).
 */
public class EvIPL {

    private String description = "";

    /**
     * IPL Event Constructor.
     *
     * @param event byte array holding the IPL Event data.
     */
    public EvIPL(final byte[] event) {
        event(event);
    }

    /**
     * Processes IPL event.
     *
     * @param event byte array holding the IPL Event data.
     * @return a description of the IPl event.
     */
    public String event(final byte[] event) {
        if (event == null) {
            description = "Invalid IPL event data";
        } else {
            description = "  \"" + new String(event, StandardCharsets.UTF_8) + "\"";
        }
        return description;
    }

    /**
     * Returns a human-readable description of the IPL Event.
     *
     * @return human readable description.
     */
    public String toString() {
        return description;
    }
}
