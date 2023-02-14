package hirs.attestationca.portal.enums;

import hirs.attestationca.portal.utils.VersionHelper;

/**
 * Contains attributes required to display a portal page and its menu link.
 */
public enum Page {

    /**
     * Site landing page.
     */
    INDEX("HIRS Attestation CA", "Version: " + VersionHelper.getVersion(),
            null, false, false, null, null),
    /**
     * Page to display registered devices.
     */
    DEVICES("Devices", "ic_devices", "first"),
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

    private final boolean hasMenu;
    private final String menuLinkClass;
    private final boolean inMenu;

    private final String prefixPath;
    private final String viewName;

    /**
     * Constructor for Page.
     *
     * @param title title of the page
     * @param subtitle subtitle of the page
     * @param icon icon for the page
     * @param hasMenu the page has its own menu
     * @param inMenu the page appears in a menu
     * @param menuLinkClass the category to which this page belongs
     * @param prefixPath prefix path that appears in the URL for this page
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
     * @param title title of the page
     * @param icon icon for the page
     * @param menuLinkClass the category to which this page belongs
     * @param prefixPath prefix path that appears in the URL for this page
     */
    Page(final String title,
         final String icon,
         final String menuLinkClass,
         final String prefixPath) {
        this(title, null, icon, true, true, menuLinkClass, prefixPath);
    }

    /**
     * Constructor for Page.
     *
     * @param title title of the page
     * @param icon icon for the page
     * @param menuLinkClass the category to which this page belongs
     */
    Page(final String title,
         final String icon,
         final String menuLinkClass) {
        this(title, null, icon, true, true, menuLinkClass, null);
    }

    /**
     * Constructor for Page.
     *
     * @param title title of the page
     * @param icon icon for the page
     */
    Page(final String title,
         final String icon) {
        this(title, null, icon, true, true, null, null);
    }

    /**
     * Returns the title of the page.
     *
     * @return the title of the page.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the subtitle of the page.
     *
     * @return the subtitle of the page.
     */
    public String getSubtitle() {
        return subtitle;
    }

    /**
     * Returns the base filename of the icon for page. E.g. "ic_my_icon", which will be appended
     * with appropriate size string (_24dp/_48dp) and file extension (.png) when used.
     *
     * @return the base filename of the icon for page.
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Returns true if the page should be displayed in the navigation menu.
     *
     * @return true if the page should be displayed in the navigation menu.
     */
    public boolean getInMenu() {
        return inMenu;
    }

    /**
     * Returns the css class to add to the menu link to display it appropriately. E.g. "first" if
     * the link is the first in a group to separate it visually from the previous group.
     *
     * @return he class to add to the menu link to display it appropriately.
     */
    public String getMenuLinkClass() {
        return menuLinkClass;
    }

    /**
     * Returns true if the page should display the navigation menu.
     *
     * @return true if the page should display the navigation menu.
     */
    public boolean getHasMenu() {
        return hasMenu;
    }

    /**
     * Return the page's view name.
     *
     * @return the page's view name
     */
    public String getViewName() {
        return viewName;
    }

    /**
     * Return the page's view name.
     *
     * @return the page's view name
     */
    public String getPrefixPath() {
        return prefixPath;
    }

}

