package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.service.ReferenceManifestDetailsService;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.ReferenceManifestDetailsPageParams;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Controller for the Reference Manifest Details page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/rim-details")
public class ReferenceManifestDetailsPageController
        extends PageController<ReferenceManifestDetailsPageParams> {
    private final ReferenceManifestDetailsService referenceManifestService;

    /**
     * Constructor providing the Page's display and routing specification.
     */
    @Autowired
    public ReferenceManifestDetailsPageController(
            final ReferenceManifestDetailsService referenceManifestService) {
        super(Page.RIM_DETAILS);
        this.referenceManifestService = referenceManifestService;
    }

    /**
     * Returns the filePath for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    public ModelAndView initPage(final ReferenceManifestDetailsPageParams params,
                                 final Model model) {
        // get the basic information to render the page
        ModelAndView mav = getBaseModelAndView();
        PageMessages messages = new PageMessages();

        // Map with the rim information
        HashMap<String, Object> data = new HashMap<>();

        // Check if parameters were set
        if (params.getId() == null) {
            String typeError = "ID was not provided";
            messages.addErrorMessage(typeError);
            log.debug(typeError);
            mav.addObject(MESSAGES_ATTRIBUTE, messages);
        } else {
            try {
                UUID uuid = UUID.fromString(params.getId());
                data.putAll(this.referenceManifestService.getRimDetailInfo(uuid));
            } catch (IllegalArgumentException iaEx) {
                String uuidError = "Failed to parse ID from: " + params.getId();
                messages.addErrorMessage(uuidError);
                log.error(uuidError, iaEx);
            } catch (CertificateException cEx) {
                log.error(cEx);
            } catch (NoSuchAlgorithmException nsEx) {
                log.error(nsEx);
            } catch (IOException ioEx) {
                log.error(ioEx);
            } catch (Exception ex) {
                log.error(ex);
            }

            if (data.isEmpty()) {
                String notFoundMessage = "Unable to find RIM with ID: " + params.getId();
                messages.addErrorMessage(notFoundMessage);
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
