package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.service.ValidationDetailsPageService;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.params.CertificateDetailsPageParams;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
/*

@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/report-details")
@Log4j2
public class ValidationDetailsPageController extends PageController<CertificateDetailsPageParams> {
    private final ValidationDetailsPageService validationDetailsPageService;

    public ValidationDetailsPageController(ValidationDetailsPageService validationDetailsPageService) {
        super(Page.VALIDATION_DETAILS);
        this.validationDetailsPageService = validationDetailsPageService;
    }

    @Override
    public ModelAndView initPage(CertificateDetailsPageParams params, Model model) {
        ModelAndView mav = getBaseModelAndView();
        return mav;
    }
}
*/
