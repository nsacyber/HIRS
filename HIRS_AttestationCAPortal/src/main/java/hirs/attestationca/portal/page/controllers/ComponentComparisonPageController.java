package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentAttributeRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/component-validation")
public class ComponentComparisonPageController extends PageController<NoPageParams> {

    private final CertificateRepository certificateRepository;
    private final ComponentResultRepository componentResultRepository;
    private final ComponentAttributeRepository componentAttributeRepository;
    @Autowired
    public ComponentComparisonPageController(final CertificateRepository certificateRepository, final ComponentResultRepository componentResultRepository, final ComponentAttributeRepository componentAttributeRepository) {
        super(Page.COMPONENT_COMPARISON);
        this.certificateRepository = certificateRepository;
        this.componentResultRepository = componentResultRepository;
        this.componentAttributeRepository = componentAttributeRepository;
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
        // get the basic information to render the page
        ModelAndView mav = getBaseModelAndView();
        PageMessages messages = new PageMessages();

        mav.addObject(MESSAGES_ATTRIBUTE, messages);
        mav.addObject(INITIAL_DATA, data);

        return mav;
    }
}


