package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.ComponentAttributeRepository;
import hirs.attestationca.persist.entity.manager.ComponentInfoRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.manager.PlatformCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentAttributeResult;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentClass;
import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import hirs.attestationca.persist.util.PciIds;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.CertificateDetailsPageParams;
import hirs.utils.xjc.Link;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/component-comparison")
public class ComponentComparisonPageController extends PageController<CertificateDetailsPageParams> {

    private final PlatformCertificateRepository platformCertificateRepository;
    private final ComponentResultRepository componentResultRepository;
    private final ComponentInfoRepository componentInfoRepository;
    private final ComponentAttributeRepository componentAttributeRepository;
    @Autowired
    public ComponentComparisonPageController(final PlatformCertificateRepository platformCertificateRepository,
                                             final ComponentResultRepository componentResultRepository,
                                             final ComponentInfoRepository componentInfoRepository,
                                             final ComponentAttributeRepository componentAttributeRepository) {
        super(Page.COMPONENT_COMPARISON);
        this.platformCertificateRepository = platformCertificateRepository;
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
        // Check if parameters were set
        if (params.getSessionId() == null) {
            String typeError = "ID was not provided";
            messages.addError(typeError);
            log.debug(typeError);
            mav.addObject(MESSAGES_ATTRIBUTE, messages);
        } else {
            try {
                String uuid = params.getSessionId();
                data.putAll(getPlatformComponentInformation(uuid, params.getDeviceName(),
                        platformCertificateRepository, componentResultRepository,
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
                String notFoundMessage = "Unable to find session with ID: " + params.getId();
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
            final String sessionId, final String deviceName,
            final PlatformCertificateRepository platformCertificateRepository,
            final ComponentResultRepository componentResultRepository,
            final ComponentInfoRepository componentInfoRepository,
            final ComponentAttributeRepository componentAttributeRepository)
            throws IllegalArgumentException, IOException {
        HashMap<String, Object> data = new HashMap<>();
        List<ComponentResult> componentResults = new ArrayList<>();
        PlatformCredential platformCredential = null;
        List<ComponentAttributeResult> attributeResults = componentAttributeRepository
                .findByProvisionSessionId(sessionId);

        data.put("deviceName", deviceName);
        if (!attributeResults.isEmpty()) {
            ComponentResult componentResult = componentResultRepository.findById(attributeResults.get(0).getComponentId()).get();
             platformCredential = platformCertificateRepository
                    .findByPlatformSerialAndSerialNumber(componentResult.getBoardSerialNumber(),
                            BigInteger.valueOf(Long.parseLong(
                                    componentResult.getCertificateSerialNumber())));

            if (platformCredential != null) {
                data.put("certificateId", platformCredential.getId());
                data.put("boardNumber", platformCredential.getPlatformSerial());
                data.put("certificateSerialNumber", platformCredential.getSerialNumber());
                data.put("platformManufacturer", platformCredential.getManufacturer());
                data.put("platformModel", platformCredential.getModel());
            } else {
                log.error("Can't find platform certificate "
                        + componentResults.get(0).getBoardSerialNumber());
                return data;
            }
            List<ComponentInfo> componentInfos = componentInfoRepository
                    .findByDeviceNameOrderByComponentClassAsc(deviceName);
            Map<ComponentResult, ComponentInfo> componentInfoHashMap = findMatchedComponents(componentResults, componentInfos);
            List<ComponentResult> matchedResults = new LinkedList<>(componentInfoHashMap.keySet());
            List<ComponentInfo> matchedDeviceComps = new LinkedList<>(componentInfoHashMap.values());
            List<ComponentResult> mismatchedResults = new LinkedList<>();
            List<ComponentInfo> mismatchedDeviceComps = new LinkedList<>();
            for(ComponentAttributeResult dbObject : attributeResults) {
                    mismatchedResults.add(componentResultRepository.getReferenceById(dbObject.getComponentId()));
                    mismatchedDeviceComps.add(componentInfoRepository.getReferenceById(dbObject.getDeviceComponentId()));

            }

//            componentResults.clear();
//            List<ComponentInfo> componentInfos = componentInfoRepository
//                    .findByDeviceNameOrderByComponentClassAsc(deviceName);
//            // find the ones that aren't matched or unmatched
//            for (ComponentResult dbResult : componentResultRepository
//                    .findByBoardSerialNumberOrderByComponentClassValueAsc(
//                            platformCredential.getPlatformSerial())) {
//                for (ComponentResult matched : matchedResults) {
//                    if (dbResult.getId().equals(matched.getId())) {
//
//                    }
//                }
//            }
            if (PciIds.DB.isReady()) {
//                componentResults = PciIds.translateResults(componentResults);
//                componentInfos = PciIds.translateDeviceComponentInfo(componentInfos);
                matchedResults = PciIds.translateResults(matchedResults);
                matchedDeviceComps = PciIds.translateDeviceComponentInfo(matchedDeviceComps);
                mismatchedResults = PciIds.translateResults(mismatchedResults);
                mismatchedDeviceComps = PciIds.translateDeviceComponentInfo(mismatchedDeviceComps);
            }

            matchedDeviceComps = translateComponentClass(matchedDeviceComps);
            mismatchedDeviceComps = translateComponentClass(mismatchedDeviceComps);

            data.put("componentResults", matchedResults);
            data.put("componentInfos", matchedDeviceComps);
            data.put("misMatchedComponentResults", mismatchedResults);
            data.put("misMatchedComponentInfos", mismatchedDeviceComps);
//            data.put("notFoundResults", );
//            data.put("notFoundComponentInfs", );
        } else {
            String notFoundMessage = "No components attribute comparison found "
                    + "with ID: " + sessionId;
            log.error(notFoundMessage);
        }
        return data;
    }

    private static List<ComponentInfo> translateComponentClass(final List<ComponentInfo> componentInfos) {
        List<ComponentInfo> tempList = new ArrayList<>();
        ComponentInfo componentInfo;
        ComponentClass componentClass;
        for (ComponentInfo info : componentInfos) {
            componentInfo = info;
            componentClass = new ComponentClass("TCG", info.getComponentClass());
            componentInfo.setComponentClassStr(componentClass.toString());
            tempList.add(componentInfo);
        }

        return tempList;
    }

    private static Map<ComponentResult, ComponentInfo> findMatchedComponents(
            final List<ComponentResult> componentResults, final List<ComponentInfo> componentInfos) {
        // first create hash map based on hashCode
        Map<ComponentResult, ComponentInfo> resultComponentInfoMap = new HashMap<>();
        Map<Integer, ComponentInfo> deviceHashMap = new HashMap<>();
        componentInfos.stream().forEach((componentInfo) -> {
            deviceHashMap.put(componentInfo.hashCommonElements(), componentInfo);
        });

        // Look for hash code in device mapping
        // if it exists, don't save the component
        List<ComponentResult> remainingComponentResults = new ArrayList<>();
        int numOfAttributes = 0;
        for (ComponentResult componentResult : componentResults) {
            if (!deviceHashMap.containsKey(componentResult.hashCommonElements())) {
                // didn't find the component result in the hashed mapping
                remainingComponentResults.add(componentResult);
            } else {
                resultComponentInfoMap.put(componentResult, deviceHashMap.get(componentResult.hashCommonElements()));
            }
        }

        return resultComponentInfoMap;
    }
}


