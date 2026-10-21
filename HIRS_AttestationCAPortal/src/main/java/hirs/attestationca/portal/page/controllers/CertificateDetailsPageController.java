package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentEvidenceResultRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.manager.ComponentAttributeRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentEvidenceResult;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.persist.dto.PageMessages;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controller for the Certificate Details page.
 */
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-details")
@Log4j2
public class CertificateDetailsPageController extends PageController<CertificateDetailsPageParams> {

    /**
     * Model attribute name used by initPage for the initial data passed to the page.
     */
    static final String INITIAL_DATA = "initialData";
    private final CertificateRepository certificateRepository;
    private final CACredentialRepository caCredentialRepository;
    private final ComponentResultRepository componentResultRepository;
    private final ComponentAttributeRepository componentAttributeRepository;
    private final ReferenceManifestRepository referenceManifestRepository;
    private final ComponentEvidenceResultRepository componentEvidenceResultRepository;

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param certificateRepository     the certificate repository
     * @param componentResultRepository the component result repository
     * @param componentAttributeRepository the component attribute repository
     * @param caCredentialRepository    the ca credential manager
     * @param referenceManifestRepository the rim repository
     * @param componentEvidenceResultRepository the component evidence result repository
     */
    @Autowired
    public CertificateDetailsPageController(final CertificateRepository certificateRepository,
                                            final ComponentResultRepository componentResultRepository,
                                            final ComponentAttributeRepository componentAttributeRepository,
                                            final CACredentialRepository caCredentialRepository,
                                            final ReferenceManifestRepository referenceManifestRepository,
                                            final ComponentEvidenceResultRepository
                                                    componentEvidenceResultRepository) {
        super(Page.CERTIFICATE_DETAILS);
        this.certificateRepository = certificateRepository;
        this.componentResultRepository = componentResultRepository;
        this.componentAttributeRepository = componentAttributeRepository;
        this.caCredentialRepository = caCredentialRepository;
        this.referenceManifestRepository = referenceManifestRepository;
        this.componentEvidenceResultRepository = componentEvidenceResultRepository;
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from redirect.
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
            messages.addErrorMessage(typeError);
            log.error(typeError);
            mav.addObject("initialDataExists", false);
        } else if (params.getType() == null) {
            String typeError = "Type was not provided";
            messages.addErrorMessage(typeError);
            log.error(typeError);
            mav.addObject("initialDataExists", false);
        } else {
            try {
                String type = params.getType().toLowerCase();
                UUID uuid = UUID.fromString(params.getId());
                switch (type) {
                    case "certificateauthority":
                        data.putAll(CertificateStringMapBuilder.getCertificateAuthorityInformation(
                                uuid, certificateRepository, caCredentialRepository, referenceManifestRepository));
                        break;
                    case "endorsement":
                        data.putAll(CertificateStringMapBuilder.getEndorsementInformation(uuid,
                                certificateRepository, caCredentialRepository));
                        break;
                    case "platform":
                        data.putAll(CertificateStringMapBuilder.getPlatformInformation(uuid,
                                certificateRepository, componentResultRepository, componentAttributeRepository,
                                caCredentialRepository, params.getProvisionSessionId()));
                        if (!data.isEmpty()) {
                            // Only for a certificate that was found, so a missing one reports "Unable to find"
                            data.put("componentEvidence", latestComponentEvidence(data.get("componentResults")));
                        }
                        break;
                    case "issued":
                        data.putAll(CertificateStringMapBuilder.getIssuedInformation(uuid,
                                certificateRepository, caCredentialRepository));
                        break;
                    case "idevid":
                        data.putAll(CertificateStringMapBuilder.getIdevidInformation(uuid,
                                certificateRepository, caCredentialRepository));
                        break;
                    default:
                        String typeError = "Invalid certificate type: " + params.getType();
                        messages.addErrorMessage(typeError);
                        log.error(typeError);
                        mav.addObject(MESSAGES_ATTRIBUTE, messages);
                        break;
                }
            } catch (IllegalArgumentException | IOException ex) {
                String uuidError = "Failed to parse ID from: " + params.getId();
                messages.addErrorMessage(uuidError);
                log.error(uuidError, ex);
            }

            if (data.isEmpty()) {
                String notFoundMessage = "Unable to find certificate with ID: " + params.getId();
                messages.addErrorMessage(notFoundMessage);
                log.warn(notFoundMessage);
                mav.addObject("initialDataExists", false);
            } else {
                mav.addObject("initialDataExists", true);
            }
        }
        // must add data even if it is empty; otherwise front end gives errors when trying to check it
        mav.addObject(MESSAGES_ATTRIBUTE, messages);
        mav.addObject(INITIAL_DATA, data);

        // return the model and view
        return mav;
    }

    /**
     * Finds the most recent evidence each component returned when it proved possession of its key.
     *
     * @param componentResults the platform certificate's {@link ComponentResult}s
     * @return component id to its latest evidence result
     */
    private Map<UUID, ComponentEvidenceResult> latestComponentEvidence(final Object componentResults) {
        if (!(componentResults instanceof List<?> components)) {
            return Map.of();
        }
        List<UUID> componentIds = components.stream()
                .filter(ComponentResult.class::isInstance)
                .map(component -> ((ComponentResult) component).getId())
                .toList();
        return componentEvidenceResultRepository.findByComponentIdInOrderByCreateTimeDesc(componentIds).stream()
                .collect(Collectors.toMap(ComponentEvidenceResult::getComponentId, Function.identity(),
                        (newer, older) -> newer));
    }
}
