package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.CertificateDetailsPageParams;
import hirs.attestationca.portal.page.utils.CertificateStringMapBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Controller for the Certificate Details page.
 */
@Log4j2
@Controller
@RequestMapping("/certificate-details")
public class CertificateDetailsPageController extends PageController<CertificateDetailsPageParams> {

    /**
     * Model attribute name used by initPage for the initial data passed to the page.
     */
    static final String INITIAL_DATA = "initialData";
    private final CertificateRepository certificateRepository;
    private final ComponentResultRepository componentResultRepository;

    /**
     * Constructor providing the Page's display and routing specification.
     * @param certificateRepository the certificate repository
     * @param componentResultRepository the component result repository
     */
    @Autowired
    public CertificateDetailsPageController(final CertificateRepository certificateRepository,
                                            final ComponentResultRepository componentResultRepository) {
        super(Page.CERTIFICATE_DETAILS);
        this.certificateRepository = certificateRepository;
        this.componentResultRepository = componentResultRepository;
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model The data model for the request. Can contain data from redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    @RequestMapping
    public ModelAndView initPage(final CertificateDetailsPageParams params, final Model model) {
        // get the basic information to render the page
        ModelAndView mav = getBaseModelAndView();
        PageMessages messages = new PageMessages();

        // Map with the certificate information
        HashMap<String, Object> data = new HashMap<>();

        // Check if parameters were set
        if (params.getId() == null) {
            String typeError = "ID was not provided";
            messages.addError(typeError);
            log.error(typeError);
            mav.addObject(MESSAGES_ATTRIBUTE, messages);
        } else if (params.getType() == null) {
            String typeError = "Type was not provided";
            messages.addError(typeError);
            log.error(typeError);
            mav.addObject(MESSAGES_ATTRIBUTE, messages);
        } else {
            try {
                String type = params.getType().toLowerCase();
                UUID uuid = UUID.fromString(params.getId());
                switch (type) {
                    case "certificateauthority":
                        data.putAll(CertificateStringMapBuilder.getCertificateAuthorityInformation(
                                uuid, certificateRepository));
                        break;
                    case "endorsement":
                        data.putAll(CertificateStringMapBuilder.getEndorsementInformation(uuid,
                                certificateRepository));
                        break;
                    case "platform":
                        data.putAll(CertificateStringMapBuilder.getPlatformInformation(uuid,
                                certificateRepository, componentResultRepository));
                        break;
                    case "issued":
                        data.putAll(CertificateStringMapBuilder.getIssuedInformation(uuid,
                                certificateRepository));
                        break;
                    default:
                        String typeError = "Invalid certificate type: " + params.getType();
                        messages.addError(typeError);
                        log.error(typeError);
                        mav.addObject(MESSAGES_ATTRIBUTE, messages);
                        break;
                }
            } catch (IllegalArgumentException | IOException ex) {
                String uuidError = "Failed to parse ID from: " + params.getId();
                messages.addError(uuidError);
                log.error(uuidError, ex);
            }

            if (data.isEmpty()) {
                String notFoundMessage = "Unable to find certificate with ID: " + params.getId();
                messages.addError(notFoundMessage);
                log.warn(notFoundMessage);
                mav.addObject(MESSAGES_ATTRIBUTE, messages);
            } else {
                mav.addObject(INITIAL_DATA, data);
            }
        }

        // return the model and view
        return mav;
    }
}
