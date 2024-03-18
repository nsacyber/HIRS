package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentAttributeRepository;
import hirs.attestationca.persist.entity.manager.ComponentInfoRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentAttributeResult;
import hirs.attestationca.persist.util.PciIds;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.CertificateDetailsPageParams;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/component-validation")
public class ComponentComparisonPageController extends PageController<CertificateDetailsPageParams> {

    private final CertificateRepository certificateRepository;
    private final ComponentResultRepository componentResultRepository;
    private final ComponentInfoRepository componentInfoRepository;
    private final ComponentAttributeRepository componentAttributeRepository;
    @Autowired
    public ComponentComparisonPageController(final CertificateRepository certificateRepository,
                                             final ComponentResultRepository componentResultRepository,
                                             final ComponentInfoRepository componentInfoRepository,
                                             final ComponentAttributeRepository componentAttributeRepository) {
        super(Page.COMPONENT_COMPARISON);
        this.certificateRepository = certificateRepository;
        this.componentResultRepository = componentResultRepository;
        this.componentInfoRepository = componentInfoRepository;
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
    public ModelAndView initPage(final CertificateDetailsPageParams params, final Model model) {
        // get the basic information to render the page
        ModelAndView mav = getBaseModelAndView();
        PageMessages messages = new PageMessages();
        // Map with the certificate information
        HashMap<String, Object> data = new HashMap<>();

        mav.addObject(MESSAGES_ATTRIBUTE, messages);
        mav.addObject(INITIAL_DATA, data);
        // Check if parameters were set
        if (params.getId() == null) {
            String typeError = "ID was not provided";
            messages.addError(typeError);
            log.debug(typeError);
            mav.addObject(MESSAGES_ATTRIBUTE, messages);
        } else {
            try {
                UUID uuid = UUID.fromString(params.getId());
                data.putAll(getPlatformComponentInformation(uuid, params.getDeviceName(),
                        certificateRepository, componentResultRepository,
                        componentInfoRepository,
                        componentAttributeRepository));
            } catch (IllegalArgumentException iaEx) {
                String uuidError = "Failed to parse ID from: " + params.getId();
                messages.addError(uuidError);
                log.error(uuidError, iaEx);
            } catch (IOException ioEx) {
                log.error(ioEx);
            } catch (Exception ex) {
                log.error(ex);
            }

            if (data.isEmpty()) {
                String notFoundMessage = "Unable to find RIM with ID: " + params.getId();
                messages.addError(notFoundMessage);
                log.warn(notFoundMessage);
                mav.addObject(MESSAGES_ATTRIBUTE, messages);
            } else {
                mav.addObject(INITIAL_DATA, data);
            }
        }

        return mav;
    }

    /**
     * Compiles and returns Platform Certificate component information.
     *
     * @param uuid ID for the certificate.
     * @param certificateRepository the certificate manager for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     * @throws IOException when parsing the certificate
     * @throws IllegalArgumentException invalid argument on parsing the certificate
     */
    public static HashMap<String, Object> getPlatformComponentInformation(
            final UUID sessionId, final String deviceName,
            final CertificateRepository certificateRepository,
            final ComponentResultRepository componentResultRepository,
            final ComponentInfoRepository componentInfoRepository,
            final ComponentAttributeRepository componentAttributeRepository)
            throws IllegalArgumentException, IOException {
        HashMap<String, Object> data = new HashMap<>();
        List<ComponentResult> componentResults = new ArrayList<>();
        List<ComponentAttributeResult> attributeResults = componentAttributeRepository.findByProvisionSessionId(sessionId);
        if (!attributeResults.isEmpty()) {
            List<UUID> tempIdList = new ArrayList<>();
            attributeResults.stream().forEach((dbObject) -> {
                if (!tempIdList.contains(dbObject.getComponentId())) {
                    tempIdList.add(dbObject.getComponentId());
                }
            });
            componentResults.addAll(componentResultRepository.findAllById(tempIdList));
            PlatformCredential platformCredential = (PlatformCredential) certificateRepository
                    .findByPlatformSerialAndSerialNumber(componentResults.get(0).getBoardSerialNumber(),
                            BigInteger.valueOf(Long.parseLong(componentResults.get(0).getCertificateSerialNumber())));

            if (platformCredential != null) {
                data.put("certificateId", platformCredential.getId());
                data.put("certificateSerialNumber", platformCredential.getSerialNumber());
                data.put("platformManufacturer", platformCredential.getManufacturer());
                data.put("platformModel", platformCredential.getModel());
            }
            if (PciIds.DB.isReady()) {
                componentResults = PciIds.translateResults(componentResults);
            }
            data.put("componentResults", componentResults);
            data.put("componentInfos", componentInfoRepository.findByDeviceNameOrderByDeviceNameAsc(deviceName));
        } else {
            String notFoundMessage = "No components attribute comparison found "
                    + "with ID: " + sessionId;
            log.error(notFoundMessage);
        }
        return data;
    }
}


