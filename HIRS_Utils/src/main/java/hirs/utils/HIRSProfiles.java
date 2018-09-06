package hirs.utils;

/**
 * Simple class to provide values for Spring profiles in use across HIRS components.
 */
public final class HIRSProfiles {
    private HIRSProfiles() {
    }

    /**
     * String representing the server (appraiser, Portal) profile.
     */
    public static final String SERVER  = "server";

    /**
     * String representing the client profile, used by the client in both its interactive and
     * service forms.
     */
    public static final String CLIENT  = "client";
}
