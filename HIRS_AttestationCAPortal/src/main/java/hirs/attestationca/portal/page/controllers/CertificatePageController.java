package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.service.CertificateServiceImpl;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Log4j2
@Controller
@RequestMapping("/certificate-request")
public class CertificatePageController extends PageController<NoPageParams> {

    private final CertificateServiceImpl certificateServiceImpl;
    private CertificateAuthorityCredential certificateAuthorityCredential;

    private static final String TRUSTCHAIN = "trust-chain";
    private static final String PLATFORMCREDENTIAL = "platform-credentials";
    private static final String ENDORSEMENTCREDENTIAL = "endorsement-key-credentials";
    private static final String ISSUEDCERTIFICATES = "issued-certificates";

    /**
     * Model attribute name used by initPage for the aca cert info.
     */
    static final String ACA_CERT_DATA = "acaCertData";

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param certificateServiceImpl the certificate manager
     * @param crudManager the CRUD manager for certificates
     * @param acaCertificate the ACA's X509 certificate
     */
    @Autowired
    public CertificatePageController(
            final CertificateServiceImpl certificateServiceImpl
//            final CrudManager<Certificate> crudManager,
//            final X509Certificate acaCertificate
    ) {
        super(Page.TRUST_CHAIN);
        this.certificateServiceImpl = certificateServiceImpl;
//        this.dataTableQuerier = crudManager;

//        try {
////            certificateAuthorityCredential
////                    = new CertificateAuthorityCredential(acaCertificate.getEncoded());
//        } catch (IOException e) {
//            log.error("Failed to read ACA certificate", e);
//        } catch (CertificateEncodingException e) {
//            log.error("Error getting encoded ACA certificate", e);
//        }
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model The data model for the request. Can contain data from
     * redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView();
    }
}
