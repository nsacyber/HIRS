package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageControllerTest;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class IDevIdCertificatePageControllerTest extends PageControllerTest {
    // Base path for the page
    private final String pagePath;

    /**
     * Constructor providing the IDevId Certificate Page's display and routing specification.
     */
    public IDevIdCertificatePageControllerTest() {
        super(Page.IDEVID_CERTIFICATES);
        pagePath = getPagePath();
    }
    
    /**
     * Tests the list REST endpoints on the IDevID Certificate page controller.
     */
    @Test
    public void testGetAllIDevIdCertificates() {

    }

    /**
     * Tests the delete REST endpoint on the IDevID Certificate page controller.
     */
    @Test
    public void testDeleteIDevIDCertificate() {

    }

    /**
     * Tests the bulk-delete REST endpoint on the IDevID Certificate page controller.
     */
    @Test
    public void testDeleteMultipleIDevIDCertificates() {

    }
}
