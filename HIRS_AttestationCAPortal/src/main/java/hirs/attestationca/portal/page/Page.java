package hirs.attestationca.portal.page;

import hirs.utils.VersionHelper;
import lombok.Getter;

/**
 * Contains attributes required to display a portal page and its menu link.
 */
@Getter
public enum Page {

    /**
     * Site landing page.
     */
    INDEX("HIRS Attestation CA", "Version: " + VersionHelper.getVersion(),
            null, false, false, null, ""),

    /**
     * Page to import and manage trust chains.
     */
    TRUST_CHAIN("Trust Chain Management", "ic_store",
            null, "certificate-request/"),

    /**
     * Page to display and manage endorsement key credentials.
     */
    ENDORSEMENT_KEY_CREDENTIALS("Endorsement Key Certificates", "ic_vpn_key",
            "first", "certificate-request/"),

    /**
     * Page to display and manage platform credentials.
     */
    PLATFORM_CREDENTIALS("Platform Certificates", "ic_important_devices",
            null, "certificate-request/"),

    /**
     * Page to display and manage IDevID certificates.
     */
    IDEVID_CERTIFICATES("IDevID Certificates", "ic_important_devices",
            null, "certificate-request/"),

    /**
     * Page to display issued certificates.
     */
    ISSUED_CERTIFICATES("Issued Certificates", "ic_library_books",
            null, "certificate-request/"),
    /**
     * Page to display certificate validation reports.
     */
    VALIDATION_REPORTS("Validation Reports", "ic_assignment", "first"),

    /**
     * Non-menu page to display certificate.  Reachable from all certificate pages.
     */
    CERTIFICATE_DETAILS("Certificate Details", "", null, true, false, null, ""),

    /**
     * Page to display registered devices.
     */
    DEVICES("Devices", "ic_devices", "first"),

    /**
     * Page to display RIMs.
     */
    REFERENCE_MANIFESTS("Reference Integrity Manifests",
            "ic_important_devices", "first"),

    /**
     * Non-menu page to display rims.
     */
    RIM_DETAILS("Reference Integrity Manifest Details",
            "", null, true, false, null, ""),

    /**
     * Page to display RIM event digest table.
     */
    RIM_DATABASE("RIM Database", "ic_important_devices", "first"),

    /**
     * Page that manages Attestation CA Policy.
     */
    POLICY("Policy", "ic_subtitles"),

    /**
     * Help page.
     */
    HELP("Help", "ic_live_help");


    private final String title;

    private final String subtitle;

    private final String icon;

    /**
     * Boolean representation of whether the page should display the navigation menu.
     */
    private final boolean hasMenu;

    private final String menuLinkClass;

    /**
     * Boolean representation of whether the page should be displayed in the navigation menu.
     */
    private final boolean inMenu;

    private final String prefixPath;

    private final String viewName;

    /**
     * Constructor for Page.
     *
     * @param title         title of the page
     * @param subtitle      subtitle of the page
     * @param icon          icon for the page
     * @param hasMenu       the page has its own menu
     * @param inMenu        the page appears in a menu
     * @param menuLinkClass the category to which this page belongs
     * @param prefixPath    prefix path that appears in the URL for this page
     */
    Page(final String title,
         final String subtitle,
         final String icon,
         final boolean hasMenu,
         final boolean inMenu,
         final String menuLinkClass,
         final String prefixPath) {
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;
        this.hasMenu = hasMenu;
        this.menuLinkClass = menuLinkClass;
        this.inMenu = inMenu;
        this.prefixPath = prefixPath;

        viewName = this.name().toLowerCase().replaceAll("_", "-");
    }

    /**
     * Constructor for Page.
     *
     * @param title         title of the page
     * @param icon          icon for the page
     * @param menuLinkClass the category to which this page belongs
     * @param prefixPath    prefix path that appears in the URL for this page
     */
    Page(final String title, final String icon, final String menuLinkClass, final String prefixPath) {
        this(title, "", icon, true, true, menuLinkClass, prefixPath);
    }

    /**
     * Constructor for Page.
     *
     * @param title         title of the page
     * @param icon          icon for the page
     * @param menuLinkClass the category to which this page belongs
     */
    Page(final String title, final String icon, final String menuLinkClass) {
        this(title, "", icon, true, true, menuLinkClass, "");
    }

    /**
     * Constructor for Page.
     *
     * @param title title of the page
     * @param icon  icon for the page
     */
    Page(final String title, final String icon) {
        this(title, "", icon, true, true, null, "");
    }
}
