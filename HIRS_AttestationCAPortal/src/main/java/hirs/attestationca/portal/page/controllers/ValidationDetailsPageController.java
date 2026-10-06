package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.dto.PageMessages;
import hirs.attestationca.persist.service.ValidationDetailsPageService;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.params.ValidationDetailsPageParams;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.UUID;


@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/validation-details")
@Log4j2
public class ValidationDetailsPageController extends PageController<ValidationDetailsPageParams> {
    private final ValidationDetailsPageService validationDetailsPageService;

    /**
     * Constructor.
     *
     * @param validationDetailsPageService service class
    **/

    public ValidationDetailsPageController(
            final ValidationDetailsPageService validationDetailsPageService) {
        super(Page.VALIDATION_DETAILS);
        this.validationDetailsPageService = validationDetailsPageService;
    }


    /**
     * Returns the filePath for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the page.
    **/

    @Override
    public ModelAndView initPage(final ValidationDetailsPageParams params, final Model model) {
        ModelAndView mav = getBaseModelAndView();
        PageMessages messages = new PageMessages();
        HashMap<String, Object> data = new HashMap<>();

        if (params.getId() == null) {
            String typeError = "ID was not provided";
            messages.addErrorMessage(typeError);
            log.debug(typeError);
            mav.addObject(MESSAGES_ATTRIBUTE, messages);
        } else {
            UUID validationSummaryId = UUID.fromString(params.getId());
            data.putAll(validationDetailsPageService.getAssociatedIds(validationSummaryId));
        }

        if (data.isEmpty()) {
            String notFoundMessage = "Unable to find supply chain validation summary with id: "
                    + params.getId();
            messages.addErrorMessage(notFoundMessage);
            log.warn(notFoundMessage);
        } else {
            mav.addObject(INITIAL_DATA, data);
        }


        return mav;
    }
}
