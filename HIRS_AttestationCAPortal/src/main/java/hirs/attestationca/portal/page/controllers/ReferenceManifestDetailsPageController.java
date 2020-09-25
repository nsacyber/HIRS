package hirs.attestationca.portal.page.controllers;

import hirs.data.persist.BaseReferenceManifest;
import hirs.data.persist.ReferenceManifest;
import hirs.data.persist.SupportReferenceManifest;
import hirs.data.persist.SwidResource;
import hirs.persist.ReferenceManifestManager;
import hirs.tpm.eventlog.TCGEventLog;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.ReferenceManifestDetailsPageParams;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
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
    private static final Logger LOGGER
            = LogManager.getLogger(ReferenceManifestDetailsPageController.class);

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param referenceManifestManager the reference manifest manager
     */
    @Autowired
    public ReferenceManifestDetailsPageController(
            final ReferenceManifestManager referenceManifestManager) {
        super(Page.RIM_DETAILS);
        this.referenceManifestManager = referenceManifestManager;
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
     *
     * @param uuid                     database reference for the requested RIM.
     * @param referenceManifestManager the reference manifest manager.
     * @return mapping of the RIM information from the database.
     * @throws java.io.IOException      error for reading file bytes.
     * @throws NoSuchAlgorithmException If an unknown Algorithm is encountered.
     * @throws CertificateException     if a certificate doesn't parse.
     */
    public static HashMap<String, Object> getRimDetailInfo(final UUID uuid,
             final ReferenceManifestManager referenceManifestManager) throws IOException,
            CertificateException, NoSuchAlgorithmException {
        HashMap<String, Object> data = new HashMap<>();

        ReferenceManifest rim = ReferenceManifest
                .select(referenceManifestManager)
                .byEntityId(uuid).getRIM();

        if (rim instanceof BaseReferenceManifest) {
            BaseReferenceManifest bRim = (BaseReferenceManifest) rim;
            // Software Identity
            data.put("swidName", bRim.getSwidName());
            data.put("swidVersion", bRim.getSwidVersion());
            if (bRim.isSwidCorpus() == 1) {
                data.put("swidCorpus", "True");
            } else {
                data.put("swidCorpus", "False");
            }
            if (bRim.isSwidPatch() == 1) {
                data.put("swidPatch", "True");
            } else {
                data.put("swidPatch", "False");
            }
            if (bRim.isSwidSupplemental() == 1) {
                data.put("swidSupplemental", "True");
            } else {
                data.put("swidSupplemental", "False");
            }
            data.put("swidTagId", rim.getTagId());
            // Entity
            data.put("entityName", bRim.getEntityName());
            data.put("entityRegId", bRim.getEntityRegId());
            data.put("entityRole", bRim.getEntityRole());
            data.put("entityThumbprint", bRim.getEntityThumbprint());
            // Link
            data.put("linkHref", bRim.getLinkHref());
            data.put("linkRel", bRim.getLinkRel());
            data.put("supportBaseRimId", "");
            data.put("supportBaseRimTagId", "");
            data.put("platformManufacturer", bRim.getPlatformManufacturer());
            data.put("platformManufacturerId", bRim.getPlatformManufacturerId());
            data.put("platformModel", bRim.getPlatformModel());
            data.put("platformVersion", bRim.getPlatformVersion());
            data.put("firmwareVersion", bRim.getFirmwareVersion());
            data.put("payloadType", bRim.getPayloadType());
            data.put("colloquialVersion", bRim.getColloquialVersion());
            data.put("edition", bRim.getEdition());
            data.put("product", bRim.getProduct());
            data.put("revision", bRim.getRevision());
            data.put("bindingSpec", bRim.getBindingSpec());
            data.put("bindingSpecVersion", bRim.getBindingSpecVersion());
            data.put("pcUriGlobal", bRim.getPcURIGlobal());
            data.put("pcUriLocal", bRim.getPcURILocal());
            data.put("rimLinkHash", bRim.getRimLinkHash());
            data.put("rimType", bRim.getRimType());

            List<SwidResource> resources = bRim.parseResource();
            String resourceFilename = null;
            TCGEventLog logProcessor;

            // going to have to pull the filename and grab that from the DB
            // to get the id to make the link
            for (SwidResource swidRes : resources) {
                resourceFilename = swidRes.getName();
                ReferenceManifest dbRim = ReferenceManifest.select(
                        referenceManifestManager).byFileName(resourceFilename).getRIM();

                if (dbRim != null) {
                    logProcessor = new TCGEventLog(dbRim.getRimBytes());
                    swidRes.setPcrValues(Arrays.asList(
                            logProcessor.getExpectedPCRValues()));

                    if (bRim.getAssociatedRim() == null) {
                        bRim.setAssociatedRim(dbRim.getId());
                    }
                } else {
                    swidRes.setPcrValues(new ArrayList<>());
                }
            }

            data.put("associatedRim", bRim.getAssociatedRim());
            data.put("swidFiles", resources);
        } else if (rim instanceof SupportReferenceManifest) {
            SupportReferenceManifest sRim = (SupportReferenceManifest) rim;

            if (sRim.getAssociatedRim() == null) {
                Set<ReferenceManifest> rims = ReferenceManifest
                        .select(referenceManifestManager).getRIMs();
                for (ReferenceManifest dbRim : rims) {
                    if (dbRim instanceof BaseReferenceManifest
                            && dbRim.getTagId().equals(sRim.getTagId())) {
                        sRim.setAssociatedRim(dbRim.getId());
                        break;
                    }
                }
            }
            data.put("baseRim", sRim.getTagId());
            data.put("associatedRim", sRim.getAssociatedRim());
            data.put("rimType", sRim.getRimType());

            TCGEventLog logProcessor = new TCGEventLog(sRim.getRimBytes());
            data.put("events", logProcessor.getEventList());
        } else {
            LOGGER.error(String.format("Unable to find Reference Integrity "
                    + "Manifest with ID: %s", uuid));
        }

        return data;
    }
}
