package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.CertificateDetailsPageParams;
import hirs.attestationca.portal.util.CertificateStringMapBuilder;
import hirs.attestationca.service.CertificateService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import static hirs.attestationca.portal.page.Page.CERTIFICATE_DETAILS;

/**
 * Controller for the Certificate Details page.
 */
@RestController
@RequestMapping(path = "/certificate-details")
public class CertificateDetailsPageController extends PageController<CertificateDetailsPageParams> {

    /**
     * Model attribute name used by initPage for the initial data passed to the page.
     */
    static final String INITIAL_DATA = "initialData";

    @Autowired
    private final CertificateService certificateService;
    private static final Logger LOGGER =
            LogManager.getLogger(CertificateDetailsPageController.class);
    /**
     * Constructor providing the Page's display and routing specification.
     * @param certificateService the certificate service
     */
    @Autowired
    public CertificateDetailsPageController(final CertificateService certificateService) {
        super(CERTIFICATE_DETAILS);
        this.certificateService = certificateService;
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
            LOGGER.error(typeError);
            mav.addObject(MESSAGES_ATTRIBUTE, messages);
        } else if (params.getType() == null) {
            String typeError = "Type was not provided";
            messages.addError(typeError);
            LOGGER.error(typeError);
            mav.addObject(MESSAGES_ATTRIBUTE, messages);
        } else {
            try {
                String type = params.getType().toLowerCase();
                UUID uuid = UUID.fromString(params.getId());
                switch (type) {
                    case "certificateauthority":
                        data.putAll(CertificateStringMapBuilder.getCertificateAuthorityInformation(
                                uuid, certificateService));
                        break;
                    case "endorsement":
                        data.putAll(CertificateStringMapBuilder.getEndorsementInformation(uuid,
                                certificateService));
                        break;
                    case "platform":
                        data.putAll(CertificateStringMapBuilder.getPlatformInformation(uuid,
                                certificateService));
                        break;
                    case "issued":
                        data.putAll(CertificateStringMapBuilder.getIssuedInformation(uuid,
                                certificateService));
                        break;
                    default:
                        String typeError = "Invalid certificate type: " + params.getType();
                        messages.addError(typeError);
                        LOGGER.error(typeError);
                        mav.addObject(MESSAGES_ATTRIBUTE, messages);
                        break;
                }
            } catch (IllegalArgumentException | IOException ex) {
                String uuidError = "Failed to parse ID from: " + params.getId();
                messages.addError(uuidError);
                LOGGER.error(uuidError, ex);
            }

            if (data.isEmpty()) {
                String notFoundMessage = "Unable to find certificate with ID: " + params.getId();
                messages.addError(notFoundMessage);
                LOGGER.warn(notFoundMessage);
                mav.addObject(MESSAGES_ATTRIBUTE, messages);
            } else {
                mav.addObject(INITIAL_DATA, data);
            }
        }

        // return the model and view
        return mav;
    }
}
