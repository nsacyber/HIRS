
package hirs.attestationca.portal.page.controllers;

import hirs.data.persist.ReferenceManifest;
import hirs.persist.ReferenceManifestManager;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.ReferenceManifestDetailsPageParams;

import java.util.HashMap;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for the Reference Manifest Details page.
 */
@Controller
@RequestMapping("/rim-details")
public class ReferenceManifestDetailsPageController
extends PageController<ReferenceManifestDetailsPageParams> {

    private final ReferenceManifestManager referenceManifestManager;
    private static final Logger LOGGER =
            LogManager.getLogger(ReferenceManifestDetailsPageController.class);

    /**
     * Constructor providing the Page's display and routing specification.
     * @param referenceManifestManager the reference manifest manager
     */
    @Autowired
    public ReferenceManifestDetailsPageController(
            final ReferenceManifestManager referenceManifestManager) {
        super(Page.RIM_DETAILS);
        this.referenceManifestManager = referenceManifestManager;
    }

    /**
     *
     * @param params The object to map url parameters into.
     * @param model The data model for the request. Can contain data from
     * redirect.
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
            messages.addError(typeError);
            LOGGER.error(typeError);
            mav.addObject(MESSAGES_ATTRIBUTE, messages);
        } else {
            try {
                UUID uuid = UUID.fromString(params.getId());
                data.putAll(getRimDetailInfo(uuid, referenceManifestManager));
            } catch (IllegalArgumentException iaEx) {
                String uuidError = "Failed to parse ID from: " + params.getId();
                messages.addError(uuidError);
                LOGGER.error(uuidError, iaEx);
            } catch (Exception ioEx) {
                LOGGER.error(ioEx);
            }
            if (data.isEmpty()) {
                String notFoundMessage = "Unable to find RIM with ID: " + params.getId();
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

    /**
     * This method takes the place of an entire class for a string builder.
     * Gathers all information and returns it for displays.
     * @param uuid database reference for the requested RIM.
     * @param referenceManifestManager the reference manifest manager.
     * @return mapping of the RIM information from the database.
     */
    public static HashMap<String, Object> getRimDetailInfo(final UUID uuid,
            final ReferenceManifestManager referenceManifestManager) {
        HashMap<String, Object> data = new HashMap<>();

        ReferenceManifest rim = ReferenceManifest
                .select(referenceManifestManager)
                .byEntityId(uuid).getRIM();

        if (rim != null) {
            // Software Identity
            data.put("swidName", rim.getSwidName());
            data.put("swidVersion", rim.getSwidVersion());
            data.put("swidCorpus", Boolean.toString(rim.isSwidCorpus()));
            data.put("swidPatch", Boolean.toString(rim.isSwidPatch()));
            data.put("swidSupplemental", Boolean.toString(
                    rim.isSwidSupplemental()));
            data.put("swidTagId", rim.getTagId());
            // Entity
            data.put("entityName", rim.getEntityName());
            data.put("entityRegId", rim.getEntityRegId());
            data.put("entityRole", rim.getEntityRole());
            data.put("entityThumbprint", rim.getEntityThumbprint());
            // Link
            data.put("linkHref", rim.getLinkHref());
            data.put("linkRel", rim.getLinkRel());

            data.put("platformManufacturer", rim.getPlatformManufacturer());
            data.put("platformManufacturerId", rim.getPlatformManufacturerId());
            data.put("platformModel", rim.getPlatformModel());
            data.put("platformVersion", rim.getPlatformVersion());
            data.put("firmwareVersion", rim.getFirmwareVersion());
            data.put("payloadType", rim.getPayloadType());
            data.put("colloquialVersion", rim.getColloquialVersion());
            data.put("edition", rim.getEdition());
            data.put("product", rim.getProduct());
            data.put("revision", rim.getRevision());
            data.put("bindingSpec", rim.getBindingSpec());
            data.put("bindingSpecVersion", rim.getBindingSpecVersion());
            data.put("pcUriGlobal", rim.getPcURIGlobal());
            data.put("pcUriLocal", rim.getPcURILocal());
            data.put("rimLinkHash", rim.getRimLinkHash());

            // checkout later
            data.put("rimType", rim.getRimType());
            data.put("swidFiles", rim.parseResource());
        } else {
            LOGGER.error(String.format("Unable to find Reference Integrity "
                    + "Manifest with ID: %s", uuid));
        }

        return data;
    }
}
