package hirs.attestationca.persist.validation;

import hirs.attestationca.persist.entity.manager.ComponentAttributeRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentAttributeResult;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentClass;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.AttributeStatus;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import hirs.attestationca.persist.entity.userdefined.info.HardwareInfo;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.util.AcaPciIds;
import hirs.utils.PciIds;
import hirs.utils.enums.DeviceInfoEnums;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.bouncycastle.asn1.ASN1UTF8String;
import org.bouncycastle.asn1.DERUTF8String;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static hirs.attestationca.persist.enums.AppraisalStatus.Status.FAIL;
import static hirs.attestationca.persist.enums.AppraisalStatus.Status.PASS;

@Log4j2
public class CertificateAttributeScvValidator extends SupplyChainCredentialValidator {

    private static final String LC_UNKNOWN = "unknown";

    /**
     * Validates platform credential attributes v1 p2.
     *
     * @param platformCredential platform credential
     * @param deviceInfoReport   device information report
     * @return an appraisal status
     */
    public static AppraisalStatus validatePlatformCredentialAttributesV1p2(
            final PlatformCredential platformCredential,
            final DeviceInfoReport deviceInfoReport) {

        // check the device's board serial number, and compare against this
        // platform credential's board serial number.
        // Retrieve the various device serial numbers.
        String credentialBoardSerialNumber = platformCredential.getPlatformSerial();
        String credentialChassisSerialNumber = platformCredential.getChassisSerialNumber();

        HardwareInfo hardwareInfo = deviceInfoReport.getHardwareInfo();
        String deviceBaseboardSerialNumber = hardwareInfo.getBaseboardSerialNumber();
        String deviceChassisSerialNumber = hardwareInfo.getChassisSerialNumber();
        String deviceSystemSerialNumber = hardwareInfo.getSystemSerialNumber();

        // log serial numbers that weren't collected. Force "not specified" serial numbers
        // to be ignored in below case checks
        Map<String, String> deviceInfoSerialNumbers = new HashMap<>();

        if (StringUtils.isEmpty(deviceBaseboardSerialNumber)
                || DeviceInfoEnums.NOT_SPECIFIED.equalsIgnoreCase(deviceBaseboardSerialNumber)) {
            log.error("Failed to retrieve device baseboard serial number");
            deviceBaseboardSerialNumber = null;
        } else {
            deviceInfoSerialNumbers.put("board serial number", deviceBaseboardSerialNumber);
            log.info("Using device board serial number for validation: {}", deviceBaseboardSerialNumber);
        }

        if (StringUtils.isEmpty(deviceChassisSerialNumber)
                || DeviceInfoEnums.NOT_SPECIFIED.equalsIgnoreCase(deviceChassisSerialNumber)) {
            log.error("Failed to retrieve device chassis serial number");
        } else {
            deviceInfoSerialNumbers.put("chassis serial number", deviceChassisSerialNumber);
            log.info("Using device chassis serial number for validation: {}", deviceChassisSerialNumber);
        }

        if (StringUtils.isEmpty(deviceSystemSerialNumber)
                || DeviceInfoEnums.NOT_SPECIFIED.equalsIgnoreCase(deviceSystemSerialNumber)) {
            log.error("Failed to retrieve device system serial number");
        } else {
            deviceInfoSerialNumbers.put("system serial number", deviceSystemSerialNumber);
            log.info("Using device system serial number for validation: {}", deviceSystemSerialNumber);
        }

        AppraisalStatus status;

        // Test 1: If the board serial number or chassis is set on the PC,
        // compare with each of the device serial numbers for any match
        if (StringUtils.isNotEmpty(credentialBoardSerialNumber)
                || StringUtils.isNotEmpty(credentialChassisSerialNumber)) {
            status = validatePlatformSerialsWithDeviceSerials(credentialBoardSerialNumber,
                    credentialChassisSerialNumber, deviceInfoSerialNumbers);
            // Test 2: If the board and chassis serial numbers are not set on the PC,
            // compare the SHA1 hash of the device baseboard serial number to
            // the certificate serial number
        } else {
            String message;
            log.debug("Credential Serial Number was null");
            if (StringUtils.isEmpty(deviceBaseboardSerialNumber)) {
                message = "Device Serial Number was null";
                log.error(message);
                status = new AppraisalStatus(FAIL, message);
            } else {
                // Calculate the SHA1 hash of the UTF8 encoded baseboard serial number
                BigInteger baseboardSha1 = new BigInteger(1,
                        DigestUtils.sha1(deviceBaseboardSerialNumber.getBytes(StandardCharsets.UTF_8)));
                BigInteger certificateSerialNumber = platformCredential.getSerialNumber();

                // compare the SHA1 hash of the baseboard serial number to the certificate SN
                if (certificateSerialNumber != null
                        && certificateSerialNumber.equals(baseboardSha1)) {
                    log.info("Device Baseboard Serial Number matches "
                            + "the Certificate Serial Number");
                    status = new AppraisalStatus(PASS, PLATFORM_ATTRIBUTES_VALID);
                } else if (certificateSerialNumber != null
                        && certificateSerialNumber.equals(
                        baseboardSha1.clearBit(NUC_VARIABLE_BIT))) {
                    log.info("Warning! The Certificate serial number had the most significant "
                            + "bit truncated.  159 bits of it matched the device baseboard "
                            + "serial number.");
                    status = new AppraisalStatus(PASS, PLATFORM_ATTRIBUTES_VALID);
                } else {
                    message = "The SHA1 hash of the Device Baseboard Serial Number "
                            + deviceBaseboardSerialNumber
                            + " did not match the Certificate's Serial Number";
                    log.error(message);
                    status = new AppraisalStatus(FAIL, message);
                }
            }
        }

        return status;
    }

    /**
     * Validates device info report against the new platform credential.
     *
     * @param platformCredential           the Platform Credential
     * @param deviceInfoReport             the Device Info Report
     * @param componentResultRepository    db access to component result of mismatching
     * @param componentAttributeRepository db access to component attribute match status
     * @param componentInfos               list of device components
     * @param provisionSessionId           UUID associated with the SCV Summary
     * @param ignoreRevisionAttribute      policy flag to ignore the revision attribute
     * @return either PASS or FAIL
     */
    public static AppraisalStatus validatePlatformCredentialAttributesV2p0(
            final PlatformCredential platformCredential,
            final DeviceInfoReport deviceInfoReport,
            final ComponentResultRepository componentResultRepository,
            final ComponentAttributeRepository componentAttributeRepository,
            final List<ComponentInfo> componentInfos,
            final UUID provisionSessionId, final boolean ignoreRevisionAttribute) throws IOException {
        boolean passesValidation = true;
        StringBuilder resultMessage = new StringBuilder();
        HardwareInfo hardwareInfo = deviceInfoReport.getHardwareInfo();

        boolean fieldValidation;
        fieldValidation = requiredPlatformCredentialFieldIsNonEmptyAndMatches(
                "PlatformManufacturerStr",
                platformCredential.getManufacturer(),
                hardwareInfo.getManufacturer());

        if (!fieldValidation) {
            resultMessage.append("Platform manufacturer did not match\n");
        }

        passesValidation &= fieldValidation;

        fieldValidation = requiredPlatformCredentialFieldIsNonEmptyAndMatches(
                "PlatformModel",
                platformCredential.getModel(),
                hardwareInfo.getProductName());

        if (!fieldValidation) {
            resultMessage.append("Platform model did not match\n");
        }

        passesValidation &= fieldValidation;

        if (!isNotSpecifiedOrUnknown(platformCredential.getVersion())
                && !isNotSpecifiedOrUnknown(hardwareInfo.getVersion())) {
            fieldValidation = requiredPlatformCredentialFieldIsNonEmptyAndMatches(
                    "PlatformVersion",
                    platformCredential.getVersion(),
                    hardwareInfo.getVersion());

            if (!fieldValidation) {
                resultMessage.append("Platform version did not match\n");
            }

            passesValidation &= fieldValidation;
        } else {
            log.warn("The Platform Certificate System version was {} and "
                            + "the reported Device System Information "
                            + "version was {}, therefore this check is skipped...",
                    platformCredential.getVersion(), hardwareInfo.getVersion());
        }

        // check PlatformSerial against both system-serial-number and baseboard-serial-number
        fieldValidation = (
                (optionalPlatformCredentialFieldNullOrMatches(
                        "PlatformSerial",
                        platformCredential.getPlatformSerial(),
                        hardwareInfo.getSystemSerialNumber()))
                        || (optionalPlatformCredentialFieldNullOrMatches(
                        "PlatformSerial",
                        platformCredential.getPlatformSerial(),
                        hardwareInfo.getBaseboardSerialNumber())));

        if (!fieldValidation) {
            resultMessage.append("Platform serial did not match\n");
        }

        passesValidation &= fieldValidation;

        if (platformCredential.getPlatformConfigurationV1() != null) {

            // Retrieve the list of all version 1 component identifiers from the Platform Credential
            List<ComponentIdentifier> allPcComponents
                    = new ArrayList<>(platformCredential.getComponentIdentifiers());

            // All V1 components listed in the Platform Credential must have a manufacturer and model
            for (ComponentIdentifier pcComponent : allPcComponents) {

                fieldValidation = !isRequiredASN1StringFieldBlank("componentManufacturer",
                        pcComponent.getComponentManufacturer());

                if (!fieldValidation) {
                    resultMessage.append("Component manufacturer is empty\n");
                }

                passesValidation &= fieldValidation;

                fieldValidation = !isRequiredASN1StringFieldBlank("componentModel",
                        pcComponent.getComponentModel());

                if (!fieldValidation) {
                    resultMessage.append("Component model is empty\n");
                }

                passesValidation &= fieldValidation;
            }

        } else if (platformCredential.getPlatformConfigurationV2() != null) {
            // Retrieve the list of all version 2 component identifiers from the Platform Credential
            List<ComponentIdentifierV2> allV2PcComponents
                    = new ArrayList<>(platformCredential.getComponentIdentifiersV2());


            // All V2 components listed in the Platform Credential must have a manufacturer and model
            for (ComponentIdentifierV2 pcComponent : allV2PcComponents) {
                fieldValidation = !isRequiredASN1StringFieldBlank("componentManufacturer",
                        pcComponent.getComponentManufacturer());

                if (!fieldValidation) {
                    resultMessage.append("Component manufacturer is empty\n");
                }

                passesValidation &= fieldValidation;

                fieldValidation = !isRequiredASN1StringFieldBlank("componentModel",
                        pcComponent.getComponentModel());

                if (!fieldValidation) {
                    resultMessage.append("Component model is empty\n");
                }

                passesValidation &= fieldValidation;

                if (pcComponent.getComponentClass() == null) {
                    passesValidation = false;
                } else {
                    ComponentClass pcComponentClass = pcComponent.getComponentClass();

                    // Component Class Registry Type field

                    fieldValidation = !isRequiredStringFieldBlank("registryType",
                            pcComponentClass.getRegistryType());

                    if (!fieldValidation) {
                        resultMessage.append("Component class registry type is empty or null\n");
                    }

                    passesValidation &= fieldValidation;

                    // Component Class Component Identifier field

                    fieldValidation = !isRequiredStringFieldBlank("componentIdentifier",
                            pcComponentClass.getComponentIdentifier());

                    if (!fieldValidation) {
                        resultMessage.append("Component class component identifier is empty or null\n");
                    }

                    passesValidation &= fieldValidation;

                    // Component Class category field

                    fieldValidation = !isRequiredStringFieldBlank("category",
                            pcComponentClass.getCategory());

                    if (!fieldValidation) {
                        resultMessage.append("Component class category is empty or null\n");
                    }

                    passesValidation &= fieldValidation;

                    // Component Class Category String field

                    fieldValidation = !isRequiredStringFieldBlank("categoryStr",
                            pcComponentClass.getCategoryStr());

                    if (!fieldValidation) {
                        resultMessage.append("Component class category string is empty or null\n");
                    }

                    passesValidation &= fieldValidation;

                    // Component Class Component String field

                    fieldValidation = !isRequiredStringFieldBlank("componentStr",
                            pcComponentClass.getComponentStr());

                    if (!fieldValidation) {
                        resultMessage.append("Component class string is empty or null\n");
                    }

                    passesValidation &= fieldValidation;

                    // Component Class Component field

                    fieldValidation = !isRequiredStringFieldBlank("component",
                            pcComponentClass.getComponent());

                    if (!fieldValidation) {
                        resultMessage.append("Component class component is empty or null\n");
                    }

                    passesValidation &= fieldValidation;
                }
            }
        }

        // populate componentResults list
        List<ComponentResult> componentResults = componentResultRepository
                .findByCertificateSerialNumberAndBoardSerialNumber(
                        platformCredential.getSerialNumber().toString(),
                        platformCredential.getPlatformSerial());

        // first create hash map based on hashCode
        List<ComponentResult> remainingComponentResults = checkDeviceHashMap(
                componentInfos, componentResults);

        //this is used to get a unique count
        List<UUID> componentIdList = new ArrayList<>();

        int numOfAttributes = 0;

        if (!remainingComponentResults.isEmpty()) {
            List<ComponentAttributeResult> attributeResults = checkComponentClassMap(
                    componentInfos, remainingComponentResults);

            numOfAttributes = attributeResults.size();

            if (numOfAttributes == 0) {
                passesValidation = false;

                resultMessage.append(String.format("There are %d component(s) not matched%n.",
                        remainingComponentResults.size()));

                for (ComponentResult componentResult : remainingComponentResults) {
                    resultMessage.append("Component not found: ")
                                    .append(componentResult.toString());
                }
            }

            boolean saveAttributeResult;

            for (ComponentAttributeResult componentAttributeResult : attributeResults) {
                saveAttributeResult = true;
                if (ignoreRevisionAttribute) {
                    saveAttributeResult = !componentAttributeResult.getAttribute()
                            .equalsIgnoreCase(ComponentResult.ATTRIBUTE_REVISION);
                }
                if (saveAttributeResult) {
                    componentAttributeResult.setProvisionSessionId(provisionSessionId);
                    componentAttributeRepository.save(componentAttributeResult);
                    fieldValidation &= componentAttributeResult.checkMatchedStatus();
                    componentIdList.add(componentAttributeResult.getComponentId());
                } else {
                    numOfAttributes--;
                }
            }
        }

        StringBuilder additionalInfo = new StringBuilder();

        if (numOfAttributes > 0) {
            resultMessage.append(String.format("There are %d component(s) not matched%n "
                            + "with %d total attributes mismatched.",
                    componentIdList.stream().distinct().count(), numOfAttributes));
        }

        passesValidation &= fieldValidation;

        if (passesValidation) {
            return new AppraisalStatus(PASS, PLATFORM_ATTRIBUTES_VALID);
        } else {
            return new AppraisalStatus(FAIL, resultMessage.toString(), additionalInfo.toString());
        }
    }

    /**
     * The main purpose of this method, the in process of validation, is to
     * pick out the changes that lead to the delta cert and make sure the changes
     * are valid.
     *
     * @param deviceInfoReport             The paccor profile of device being validated against.
     * @param deltaMapping                 map of delta certificates to their validated status
     * @param origPcComponents             The component identifier list associated with the
     *                                     base cert for this specific chain
     * @param componentInfos               list of component information
     * @param componentResultRepository    component result repository
     * @param componentAttributeRepository component attribute repository
     * @param provisionSessionId           uuid representation of the provision session id
     * @param ignoreRevisionAttribute      whether to ignore the revision attribute
     * @return Appraisal Status of delta being validated.
     */

    static AppraisalStatus validateDeltaAttributesChainV2p0(
            final DeviceInfoReport deviceInfoReport,
            final Map<PlatformCredential, SupplyChainValidation> deltaMapping,
            final List<ComponentResult> origPcComponents,
            final List<ComponentInfo> componentInfos,
            final ComponentResultRepository componentResultRepository,
            final ComponentAttributeRepository componentAttributeRepository,
            final UUID provisionSessionId, final boolean ignoreRevisionAttribute) {
        boolean fieldValidation = true;
        StringBuilder resultMessage = new StringBuilder();
        List<PlatformCredential> deltaCertificates = new LinkedList<>(deltaMapping.keySet());

        // sort the list so that it is in order by date
        Collections.sort(deltaCertificates, new Comparator<PlatformCredential>() {
            @Override
            public int compare(final PlatformCredential obj1,
                               final PlatformCredential obj2) {
                if (obj1 == null) {
                    return 0;
                }
                if (obj2 == null) {
                    return 0;
                }
                if (obj1.getBeginValidity() == null || obj2.getBeginValidity() == null) {
                    return 0;
                }
                return obj1.getBeginValidity().compareTo(obj2.getBeginValidity());
            }
        });
        // start of some changes
        List<ComponentResult> compiledComponentList = compileDeltaComponentResults(deltaCertificates,
                componentResultRepository, componentAttributeRepository, provisionSessionId);

        // Check if there were issues with compiling the delta list
        List<ComponentResult> deltaComponentList = new ArrayList<>();
        List<ComponentAttributeResult> componentAttributeResults = new ArrayList<>();
        for (PlatformCredential delta : deltaMapping.keySet()) {
            deltaComponentList.addAll(componentResultRepository
                    .findByBoardSerialNumberAndDelta(delta.getPlatformSerial(), true));
            for (ComponentResult componentResult : deltaComponentList) {
                componentAttributeResults.addAll(componentAttributeRepository
                        .findByComponentId(componentResult.getId()));
            }
        }

        if (!componentAttributeResults.isEmpty()) {
            resultMessage.append(String.format("There are %d errors with Delta "
                            + "Components associated with: %s%n",
                    componentAttributeResults.size(),
                    deltaCertificates.get(0).getPlatformSerial()));
            fieldValidation = false;
        }

        // now pass in new list
        // first create hash map based on hashCode
        List<ComponentResult> remainingComponentResults = checkDeviceHashMap(
                componentInfos, compiledComponentList);

        List<UUID> componentIdList = new ArrayList<>();
        int numOfAttributes = 0;
        if (!remainingComponentResults.isEmpty()) {
            List<ComponentAttributeResult> attributeResults = checkComponentClassMap(
                    componentInfos, remainingComponentResults);
            numOfAttributes = attributeResults.size();

            if (numOfAttributes == 0) {
                fieldValidation = false;

                resultMessage.append(String.format("There are %d component(s) not matched%n.",
                        remainingComponentResults.size()));

                for (ComponentResult componentResult : remainingComponentResults) {
                    resultMessage.append("Component not found: ")
                            .append(componentResult.toString());
                }
            }


            boolean saveAttributeResult;
            for (ComponentAttributeResult componentAttributeResult : attributeResults) {
                saveAttributeResult = true;
                if (ignoreRevisionAttribute) {
                    saveAttributeResult = !componentAttributeResult.getAttribute()
                            .equalsIgnoreCase(ComponentResult.ATTRIBUTE_REVISION);
                }
                if (saveAttributeResult) {
                    componentAttributeResult.setProvisionSessionId(provisionSessionId);
                    componentAttributeRepository.save(componentAttributeResult);
                    fieldValidation &= componentAttributeResult.checkMatchedStatus();
                    componentIdList.add(componentAttributeResult.getComponentId());
                } else {
                    numOfAttributes--;
                }
            }
        }

        StringBuilder additionalInfo = new StringBuilder();
        if (!remainingComponentResults.isEmpty()) {
            resultMessage.append(String.format("There are %d component(s) not matched%n "
                            + "with %d total attributes mismatched.",
                    componentIdList.stream().distinct().count(), numOfAttributes));
        }

        if (fieldValidation) {
            return new AppraisalStatus(PASS, PLATFORM_ATTRIBUTES_VALID);
        } else {
            return new AppraisalStatus(FAIL, resultMessage.toString(), additionalInfo.toString());
        }
    }

    private static String validateV2PlatformCredentialAttributes(
            final List<ComponentIdentifier> fullDeltaChainComponents,
            final List<ComponentInfo> allDeviceInfoComponents) {
        ComponentIdentifierV2 ciV2;
        StringBuilder invalidPcIds = new StringBuilder();
        List<ComponentIdentifier> subCompIdList = fullDeltaChainComponents
                .stream().collect(Collectors.toList());
        List<ComponentInfo> subCompInfoList = allDeviceInfoComponents
                .stream().collect(Collectors.toList());

        // Delta is the baseline
        for (ComponentInfo cInfo : allDeviceInfoComponents) {
            for (ComponentIdentifier cId : fullDeltaChainComponents) {
                ciV2 = (ComponentIdentifierV2) cId;
                if (cInfo.getComponentClassValue().contains(
                        ciV2.getComponentClass().getComponentIdentifier())
                        && isMatch(cId, cInfo)) {
                    subCompIdList.remove(cId);
                    subCompInfoList.remove(cInfo);
                }
            }
        }

        if (subCompIdList.isEmpty()) {
            return Strings.EMPTY;
        } else {
            // now we return everything that was unmatched
            // what is in the component info/device reported components
            // is to be displayed as the failure
            fullDeltaChainComponents.clear();
            for (ComponentIdentifier ci : subCompIdList) {
                if (ci.isVersion2() && PciIds.DB.isReady()) {
                    ci = AcaPciIds.translate((ComponentIdentifierV2) ci);
                }
                log.error("Unmatched component: {}", ci);
                fullDeltaChainComponents.add(ci);
                invalidPcIds.append(String.format(
                        "Manufacturer=%s, Model=%s, Serial=%s, Revision=%s;%n",
                        ci.getComponentManufacturer(),
                        ci.getComponentModel(),
                        ci.getComponentSerial(),
                        ci.getComponentRevision()));
            }
        }

        return invalidPcIds.toString();
    }

    /**
     * Compares the component information from the device info report against those of the
     * platform credential. All components in the platform credential should exactly match one
     * component in the device info report.  The device info report is allowed to have extra
     * components not represented in the platform credential.
     *
     * @param untrimmedPcComponents   the platform credential components (may contain end whitespace)
     *                                **NEW** this is updated with just the unmatched components
     *                                if there are any failures, otherwise it remains unchanged.
     * @param allDeviceInfoComponents the device info report components
     * @return passes if the returned value is empty, otherwise the components that are unmatched
     * populate the string
     */
    private static String validateV2p0PlatformCredentialComponentsExpectingExactMatch(
            final List<ComponentIdentifier> untrimmedPcComponents,
            final List<ComponentInfo> allDeviceInfoComponents) {
        // For each manufacturer listed in the platform credential, create two lists:
        // 1. a list of components listed in the platform credential for the manufacturer, and
        // 2. a list of components listed in the device info for the same manufacturer
        // Then eliminate matches from both lists. Finally, decide if the validation passes based
        // on the leftovers in the lists and the policy in place.
        final List<ComponentIdentifier> pcComponents = new ArrayList<>();
        for (ComponentIdentifier component : untrimmedPcComponents) {
            if (component.getComponentManufacturer() != null) {
                component.setComponentManufacturer(new DERUTF8String(
                        component.getComponentManufacturer().getString().trim()));
            }
            if (component.getComponentModel() != null) {
                component.setComponentModel(new DERUTF8String(
                        component.getComponentModel().getString().trim()));
            }
            if (component.getComponentSerial() != null) {
                component.setComponentSerial(new DERUTF8String(
                        component.getComponentSerial().getString().trim()));
            }
            if (component.getComponentRevision() != null) {
                component.setComponentRevision(new DERUTF8String(
                        component.getComponentRevision().getString().trim()));
            }
            pcComponents.add(component);
        }

        log.info("Validating the following Platform Cert components...");
        pcComponents.forEach(component -> log.info(component.toString()));

        log.info("...against the the following DeviceInfoReport components:");
        allDeviceInfoComponents.forEach(component -> log.info(component.toString()));

        Set<ASN1UTF8String> manufacturerSet = new HashSet<>();
        // create a set of component manufacturers
        pcComponents.forEach(pcComp -> manufacturerSet.add(pcComp.getComponentManufacturer()));

        // Create a list for unmatched components across all manufacturers to display at the end.
        List<ComponentIdentifier> pcUnmatchedComponents = new ArrayList<>();

        for (ASN1UTF8String derUtf8Manufacturer : manufacturerSet) {

            // look for all the component identifiers whose manufacturer matches that of the current
            // manufacturer
            List<ComponentIdentifier> pcComponentsFromManufacturer
                    = pcComponents.stream().filter(compIdentifier
                            -> compIdentifier.getComponentManufacturer().equals(derUtf8Manufacturer))
                    .collect(Collectors.toList());

            // look for all the component infos whose manufacturer matches that of the current
            // manufacturer
            String currentPCManufacturer = derUtf8Manufacturer.getString();
            List<ComponentInfo> deviceInfoComponentsFromManufacturer
                    = allDeviceInfoComponents.stream().filter(componentInfo
                            -> componentInfo.getComponentManufacturer().equals(currentPCManufacturer))
                    .collect(Collectors.toList());

            // For each component listed in the platform credential from this manufacturer
            // find the ones that specify a serial number so we can match the most specific ones
            // first.
            List<ComponentIdentifier> pcComponentsFromManufacturerWithSerialNumber
                    = pcComponentsFromManufacturer.stream().filter(compIdentifier
                            -> compIdentifier.getComponentSerial() != null
                            && StringUtils.isNotEmpty(compIdentifier.getComponentSerial().getString()))
                    .toList();

            // Now match up the components from the device info that are from the same
            // manufacturer and have a serial number. As matches are found, remove them from
            // both lists.
            for (ComponentIdentifier pcComponent
                    : pcComponentsFromManufacturerWithSerialNumber) {
                Optional<ComponentInfo> first
                        = deviceInfoComponentsFromManufacturer.stream()
                        .filter(componentInfo
                                -> StringUtils.isNotEmpty(componentInfo.getComponentSerial()))
                        .filter(componentInfo -> componentInfo.getComponentSerial()
                                .equals(pcComponent.getComponentSerial().getString())).findFirst();

                if (first.isPresent()) {
                    ComponentInfo potentialMatch = first.get();
                    if (isMatch(pcComponent, potentialMatch)) {
                        pcComponentsFromManufacturer.remove(pcComponent);
                        deviceInfoComponentsFromManufacturer.remove(potentialMatch);
                    }
                }
            }

            // For each component listed in the platform credential from this manufacturer
            // find the ones that specify value for the revision field so we can match the most
            // specific ones first.
            List<ComponentIdentifier> pcComponentsFromManufacturerWithRevision
                    = pcComponentsFromManufacturer.stream().filter(compIdentifier
                            -> compIdentifier.getComponentRevision() != null
                            && StringUtils.isNotEmpty(compIdentifier.getComponentRevision().getString()))
                    .toList();

            // Now match up the components from the device info that are from the same
            // manufacturer and specify a value for the revision field. As matches are found,
            // remove them from both lists.
            for (ComponentIdentifier pcComponent
                    : pcComponentsFromManufacturerWithRevision) {
                Optional<ComponentInfo> first
                        = deviceInfoComponentsFromManufacturer.stream()
                        .filter(info -> StringUtils.isNotEmpty(info.getComponentRevision()))
                        .filter(info -> info.getComponentRevision()
                                .equals(pcComponent.getComponentRevision().getString()))
                        .findFirst();

                if (first.isPresent()) {
                    ComponentInfo potentialMatch = first.get();
                    if (isMatch(pcComponent, potentialMatch)) {
                        pcComponentsFromManufacturer.remove(pcComponent);
                        deviceInfoComponentsFromManufacturer.remove(potentialMatch);
                    }
                }
            }
            // The remaining components from the manufacturer have only the 2 required fields so
            // just match them.
            List<ComponentIdentifier> templist = new ArrayList<>(pcComponentsFromManufacturer);
            for (ComponentIdentifier ci : templist) {
                Iterator<ComponentInfo> diComponentIter
                        = deviceInfoComponentsFromManufacturer.iterator();
                while (diComponentIter.hasNext()) {
                    ComponentInfo potentialMatch = diComponentIter.next();
                    if (isMatch(ci, potentialMatch)) {
                        pcComponentsFromManufacturer.remove(ci);
                        diComponentIter.remove();
                    }
                }
            }
            pcUnmatchedComponents.addAll(pcComponentsFromManufacturer);
        }

        if (!pcUnmatchedComponents.isEmpty()) {
            untrimmedPcComponents.clear();
            StringBuilder sb = new StringBuilder();
            log.error("Platform Credential contained {} unmatched components:", pcUnmatchedComponents.size());

            int unmatchedComponentCounter = 1;
            for (ComponentIdentifier unmatchedComponent : pcUnmatchedComponents) {
                if (unmatchedComponent.isVersion2() && PciIds.DB.isReady()) {
                    unmatchedComponent =
                            AcaPciIds.translate((ComponentIdentifierV2) unmatchedComponent);
                }
                log.error("Unmatched component {}: {}", unmatchedComponentCounter++, unmatchedComponent);
                sb.append(String.format("Manufacturer=%s, Model=%s, Serial=%s, Revision=%s;%n",
                        unmatchedComponent.getComponentManufacturer(),
                        unmatchedComponent.getComponentModel(),
                        unmatchedComponent.getComponentSerial(),
                        unmatchedComponent.getComponentRevision()));
                unmatchedComponent.setValidationResult(false);
                untrimmedPcComponents.add(unmatchedComponent);
            }
            return sb.toString();
        }
        return Strings.EMPTY;
    }

    /**
     * Checks if the fields in the potentialMatch match the fields in the pcComponent,
     * or if the relevant field in the pcComponent is empty.
     *
     * @param pcComponent    the platform credential component
     * @param potentialMatch the component info from a device info report
     * @return true if the fields match exactly (null is considered the same as an empty string)
     */
    public static boolean isMatch(final ComponentIdentifier pcComponent,
                                  final ComponentInfo potentialMatch) {
        boolean matchesSoFar = true;

        matchesSoFar &= isMatchOrEmptyInPlatformCert(
                potentialMatch.getComponentManufacturer(),
                pcComponent.getComponentManufacturer()
        );

        matchesSoFar &= isMatchOrEmptyInPlatformCert(
                potentialMatch.getComponentModel(),
                pcComponent.getComponentModel()
        );

        matchesSoFar &= isMatchOrEmptyInPlatformCert(
                potentialMatch.getComponentSerial(),
                pcComponent.getComponentSerial()
        );

        matchesSoFar &= isMatchOrEmptyInPlatformCert(
                potentialMatch.getComponentRevision(),
                pcComponent.getComponentRevision()
        );

        return matchesSoFar;
    }

    private static boolean isMatchOrEmptyInPlatformCert(
            final String evidenceFromDevice,
            final ASN1UTF8String valueInPlatformCert) {
        if (StringUtils.isBlank(valueInPlatformCert.getString())) {
            return true;
        }
        return valueInPlatformCert.getString().equals(evidenceFromDevice);
    }

    /**
     * Validates the platform credential's serial numbers with the device info's set of
     * serial numbers.
     *
     * @param credentialBoardSerialNumber   the PC board S/N
     * @param credentialChassisSerialNumber the PC chassis S/N
     * @param deviceInfoSerialNumbers       the map of device info serial numbers with descriptions.
     * @return the changed validation status
     */
    private static AppraisalStatus validatePlatformSerialsWithDeviceSerials(
            final String credentialBoardSerialNumber, final String credentialChassisSerialNumber,
            final Map<String, String> deviceInfoSerialNumbers) {
        boolean boardSerialNumberFound = false;
        boolean chassisSerialNumberFound = false;

        if (StringUtils.isNotEmpty(credentialBoardSerialNumber)) {
            boardSerialNumberFound = deviceInfoContainsPlatformSerialNumber(
                    credentialBoardSerialNumber, "board serial number", deviceInfoSerialNumbers);
        }
        if (StringUtils.isNotEmpty(credentialChassisSerialNumber)) {
            chassisSerialNumberFound = deviceInfoContainsPlatformSerialNumber(
                    credentialChassisSerialNumber,
                    "chassis serial number", deviceInfoSerialNumbers);
        }

        if (boardSerialNumberFound || chassisSerialNumberFound) {
            log.info("The platform credential's board or chassis serial number matched"
                    + " with a serial number from the client's device information");
            return new AppraisalStatus(PASS, PLATFORM_ATTRIBUTES_VALID);
        }
        log.error("The platform credential's board and chassis serial numbers did"
                + " not match with any device info's serial numbers");

        return new AppraisalStatus(FAIL, "Platform serial did not match device info");
    }

    /**
     * Checks if a platform credential's serial number matches ANY of the device information's
     * set of serial numbers.
     *
     * @param platformSerialNumber            the platform serial number to compare
     * @param platformSerialNumberDescription description of the serial number for logging purposes.
     * @param deviceInfoSerialNumbers         the map of device info serial numbers
     *                                        (key = description, value = serial number)
     * @return true if the platform serial number was found (case-insensitive search),
     * false otherwise
     */
    private static boolean deviceInfoContainsPlatformSerialNumber(
            final String platformSerialNumber, final String platformSerialNumberDescription,
            final Map<String, String> deviceInfoSerialNumbers) {
        // check to see if the platform serial number is contained in the map of device info's
        // serial numbers
        for (Map.Entry<String, String> entry : deviceInfoSerialNumbers.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(platformSerialNumber)) {
                log.info("Device info contained platform {} {}"
                                + " in the device info's {}", platformSerialNumberDescription,
                        platformSerialNumber, entry.getKey());
                return true;
            }
        }

        log.warn("Platform {}, {}, did not match any device info serial numbers",
                platformSerialNumberDescription, platformSerialNumber);
        return false;
    }

    /**
     * Validates the information supplied for the Platform Credential.  This
     * method checks if the field is required and therefore if the value is
     * present then verifies that the values match.
     *
     * @param platformCredentialFieldName  name of field to be compared
     * @param platformCredentialFieldValue first value to compare
     * @param otherValue                   second value to compare
     * @return true if values match
     */
    private static boolean requiredPlatformCredentialFieldIsNonEmptyAndMatches(
            final String platformCredentialFieldName,
            final String platformCredentialFieldValue,
            final String otherValue) {
        if (isRequiredStringFieldBlank(platformCredentialFieldName,
                platformCredentialFieldValue)) {
            return false;
        }

        return platformCredentialFieldMatches(platformCredentialFieldName,
                platformCredentialFieldValue, otherValue);
    }

    /**
     * Validates the information supplied for the Platform Credential.  This
     * method checks if the value is present then verifies that the values match.
     * If not present, then returns true.
     *
     * @param platformCredentialFieldName  name of field to be compared
     * @param platformCredentialFieldValue first value to compare
     * @param otherValue                   second value to compare
     * @return true if values match or null
     */
    private static boolean optionalPlatformCredentialFieldNullOrMatches(
            final String platformCredentialFieldName,
            final String platformCredentialFieldValue,
            final String otherValue) {
        if (platformCredentialFieldValue == null) {
            return true;
        }

        return platformCredentialFieldMatches(platformCredentialFieldName,
                platformCredentialFieldValue, otherValue);
    }

    /**
     * Returns true if fieldValue is null or empty.
     *
     * @param description description of the value
     * @param fieldValue  value of the field
     * @return true if fieldValue is null or empty; false otherwise
     */
    private static boolean isRequiredStringFieldBlank(final String description,
                                                      final String fieldValue) {
        if (StringUtils.isBlank(fieldValue)) {
            log.error("Required string field was empty or null in Platform Credential: {}", description);
            return true;
        }
        return false;
    }

    /**
     * Per update to the provisioning via Issue 723, Not Specified and Unknown values
     * are to be ignored.
     *
     * @param versionNumber string value of the device/platform version number
     * @return true if they equal Not Specified or Unknown
     */
    public static boolean isNotSpecifiedOrUnknown(final String versionNumber) {
        if (versionNumber == null) {
            return true;
        }
        String fieldValue = versionNumber.toLowerCase();

        return fieldValue.equals(DeviceInfoEnums.NOT_SPECIFIED.toLowerCase())
                || fieldValue.equals(LC_UNKNOWN);
    }

    private static boolean platformCredentialFieldMatches(
            final String platformCredentialFieldName,
            final String platformCredentialFieldValue,
            final String otherValue) {
        String trimmedFieldValue = platformCredentialFieldValue.trim();
        String trimmedOtherValue = otherValue.trim();

        if (!trimmedFieldValue.equals(trimmedOtherValue)) {
            log.debug("{} field in Platform Credential ({}) does not match "
                            + "a related field in the DeviceInfoReport ({})",
                    platformCredentialFieldName, trimmedFieldValue, trimmedOtherValue);
            return false;
        }

        log.debug("{} field in Platform Credential matches "
                        + "a related field in the DeviceInfoReport {}",
                platformCredentialFieldName, trimmedFieldValue
        );

        return true;
    }

    /**
     * Returns true if fieldValue is null or empty.
     *
     * @param description description of the value
     * @param fieldValue  value of the field
     * @return true if fieldValue is null or empty; false otherwise
     */
    private static boolean isRequiredASN1StringFieldBlank(final String description,
                                                          final ASN1UTF8String fieldValue) {
        if (fieldValue == null || StringUtils.isBlank(fieldValue.getString().trim())) {
            log.error("Required ASN1 string field was empty or null in Platform Credential: {}", description);
            return true;
        }
        return false;
    }

    /**
     * This method uses a specific hash to match device components with certificate components.
     *
     * @param componentInfos        list of device component infos
     * @param compiledComponentList list of the remaining unmatched component results
     * @return remaining component results not matched
     */
    private static List<ComponentResult> checkDeviceHashMap(
            final List<ComponentInfo> componentInfos,
            final List<ComponentResult> compiledComponentList) {
        Map<Integer, List<ComponentInfo>> deviceHashMap = new HashMap<>();

        componentInfos.forEach((componentInfo) -> {
            List<ComponentInfo> innerList = new ArrayList<>();
            Integer compInfoHash = componentInfo.hashCommonElements();

            if (deviceHashMap.containsKey(compInfoHash)) {
                innerList = deviceHashMap.get(compInfoHash);
            }

            innerList.add(componentInfo);

            deviceHashMap.put(compInfoHash, innerList);
        });

        // Look for hash code in device mapping
        // if it exists, don't save the component
        List<ComponentResult> remainingComponentResults = new ArrayList<>();
        for (ComponentResult componentResult : compiledComponentList) {
            if (!deviceHashMap.containsKey(componentResult.hashCommonElements())) {
                // didn't find the component result in the hashed mapping
                remainingComponentResults.add(componentResult);
            }
        }

        return remainingComponentResults;
    }

    /**
     * This method is used to find matches based on the component class value.
     *
     * @param componentInfos            list of device component infos
     * @param remainingComponentResults list of the remaining unmatched component results
     * @return a generated list of component attributes results
     */
    private static List<ComponentAttributeResult> checkComponentClassMap(
            final List<ComponentInfo> componentInfos,
            final List<ComponentResult> remainingComponentResults) {
        // continue down the options, move to a different method.
        // create component class mapping to component info
        Map<String, List<ComponentInfo>> componentDeviceMap = new HashMap<>();

        componentInfos.forEach((componentInfo) -> {
            List<ComponentInfo> innerList = new ArrayList<>();
            String componentClass = componentInfo.getComponentClassValue();

            if (componentDeviceMap.containsKey(componentClass)) {
                innerList = componentDeviceMap.get(componentClass);
            }

            innerList.add(componentInfo);

            componentDeviceMap.put(componentClass, innerList);
        });

        List<ComponentInfo> componentClassInfo;
        List<ComponentAttributeResult> attributeResults = new ArrayList<>();

        for (ComponentResult componentResult : remainingComponentResults) {

            componentClassInfo = componentDeviceMap.get(componentResult.getComponentClassValue());

            if (componentClassInfo == null) {
                log.error("The retrieved list of component class info is null. The null list"
                                + "is associated with the component result's component class value of {}",
                        componentResult.getComponentClassValue());

                //move on to the next iteration since there is nothing we can do with the null
                // component class info
                continue;
            }

            if (componentClassInfo.size() == 1) {
                attributeResults.addAll(generateComponentAttributeResults(
                        componentClassInfo.get(0), componentResult));
            } else {
                attributeResults.addAll(findMismatchedValues(componentClassInfo, componentResult));
            }
        }

        return attributeResults;
    }

    /**
     * This method produces component attribute results for a single device that was found
     * by component class.
     *
     * @param componentInfo   the device object
     * @param componentResult the certificate expected object
     * @return a list of attribute match results
     */
    private static List<ComponentAttributeResult> generateComponentAttributeResults(
            final ComponentInfo componentInfo,
            final ComponentResult componentResult) {
        // there are instances of components with the same class (ie hard disks, memory)
        List<ComponentAttributeResult> attributeResults = new ArrayList<>();
        if (!componentInfo.getComponentManufacturer().equals(componentResult.getManufacturer())) {
            ComponentAttributeResult manufacturerAttribute = new ComponentAttributeResult(
                    componentResult.getId(), componentResult.getManufacturer(),
                    componentInfo.getComponentManufacturer());
            manufacturerAttribute.setAttribute(ComponentResult.ATTRIBUTE_MANUFACTURER);
            attributeResults.add(manufacturerAttribute);
        }

        if (!componentInfo.getComponentModel().equals(componentResult.getModel())) {
            ComponentAttributeResult modelAttribute = new ComponentAttributeResult(
                    componentResult.getId(), componentResult.getModel(),
                    componentInfo.getComponentModel());
            modelAttribute.setAttribute(ComponentResult.ATTRIBUTE_MODEL);
            attributeResults.add(modelAttribute);
        }

        if (!componentInfo.getComponentSerial().equals(componentResult.getSerialNumber())) {
            ComponentAttributeResult serialAttribute = new ComponentAttributeResult(
                    componentResult.getId(), componentResult.getSerialNumber(),
                    componentInfo.getComponentSerial());
            serialAttribute.setAttribute(ComponentResult.ATTRIBUTE_SERIAL);
            attributeResults.add(serialAttribute);
        }

        if (!componentInfo.getComponentRevision().equals(componentResult.getRevisionNumber())) {
            ComponentAttributeResult revisionAttribute = new ComponentAttributeResult(
                    componentResult.getId(), componentResult.getRevisionNumber(),
                    componentInfo.getComponentRevision());
            // this could be a boolean, but then it is too specific to revision, this leaves it open
            // for future changes
            revisionAttribute.setAttribute(ComponentResult.ATTRIBUTE_REVISION);
            attributeResults.add(revisionAttribute);
        }

        return attributeResults;
    }

    /**
     * This method is called when there are multiple components on the device that match
     * the certificate component's component class type and there is either a mismatch or
     * a status of not found to be assigned.
     *
     * @param componentClassInfo list of device components with the same class type
     * @param componentResult    the certificate component that is mismatched
     * @return a list of attribute results, if all 4 attributes are never matched, it is not found
     */
    private static List<ComponentAttributeResult> findMismatchedValues(
            final List<ComponentInfo> componentClassInfo,
            final ComponentResult componentResult) {

        // this list only has those of the same class type
        Map<String, ComponentInfo> componentSerialMap = new HashMap<>();
        componentClassInfo.forEach((componentInfo) -> {
            componentSerialMap.put(componentInfo.getComponentSerial(), componentInfo);
        });

        // see if the serial exists
        ComponentInfo componentInfo = componentSerialMap.get(componentResult.getSerialNumber());

        if (componentInfo != null && componentInfo.getComponentManufacturer()
                .equals(componentResult.getManufacturer())) {
            // the serial matched and the manufacturer, create attribute result and move on
            return generateComponentAttributeResults(componentInfo, componentResult);
        } else {
            // didn't find based on serial
            // look for highest match; otherwise ignore
            // I already know serial doesn't match
            for (ComponentInfo ci : componentClassInfo) {
                if (ci.getComponentManufacturer().equals(componentResult.getManufacturer())
                        && ci.getComponentModel().equals(componentResult.getModel())) {
                    return generateComponentAttributeResults(ci, componentResult);
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Compiles a list of delta component results.
     *
     * @param deltaCertificates            delta certificates
     * @param componentResultRepository    component result repository
     * @param componentAttributeRepository component attribute repository
     * @param provisionSessionId           uuid representation of the provision session id
     * @return a list of delta component results
     */
    private static List<ComponentResult> compileDeltaComponentResults(
            final List<PlatformCredential> deltaCertificates,
            final ComponentResultRepository componentResultRepository,
            final ComponentAttributeRepository componentAttributeRepository,
            final UUID provisionSessionId) {
        Map<String, ComponentResult> componentSerialMap = new HashMap<>();
        Map<Integer, ComponentResult> componentNonUniqueSerialMap = new HashMap<>();
        List<String> nonSerialValues = new ArrayList<>();
        nonSerialValues.add("");
        nonSerialValues.add(null);
        nonSerialValues.add("Not Specified");
        nonSerialValues.add("Unknown");
        nonSerialValues.add("To Be Filled By O.E.M.");
        // pull all component results that are not delta
        List<ComponentResult> dbBaseComponents = componentResultRepository
                .findByBoardSerialNumberAndDelta(deltaCertificates.get(0).getPlatformSerial(), false);
        dbBaseComponents.forEach((componentResult) -> {
            // ignore values that are not unique
            if (nonSerialValues.contains(componentResult.getSerialNumber())) {
                componentNonUniqueSerialMap.put(componentResult.hashCommonElements(), componentResult);
            } else {
                componentSerialMap.put(componentResult.getSerialNumber(), componentResult);
            }
        });
        List<ComponentResult> deltaComponents;
        String componentSerialNumber;
        ComponentResult componentEntry;
        // delta certificates are in order
        for (PlatformCredential delta : deltaCertificates) {
            deltaComponents = componentResultRepository.findByCertificateSerialNumberAndBoardSerialNumber(
                    delta.getSerialNumber().toString(), delta.getPlatformSerial());
            for (ComponentResult deltaComponentResult : deltaComponents) {
                if (deltaComponentResult.getAttributeStatus() == AttributeStatus.EMPTY_STATUS) {
                    // create attribute and move on
                    componentAttributeRepository.save(new ComponentAttributeResult(
                            deltaComponentResult.getId(),
                            provisionSessionId,
                            deltaComponentResult.getSerialNumber(), "Delta Component with no Status"));
                } else {
                    componentSerialNumber = deltaComponentResult.getSerialNumber();
                    componentEntry = componentSerialMap.get(componentSerialNumber);
                    if (componentEntry != null) {
                        switch (deltaComponentResult.getAttributeStatus()) {
                            case ADDED:
                                componentAttributeRepository.save(new ComponentAttributeResult(
                                        deltaComponentResult.getId(),
                                        provisionSessionId,
                                        componentSerialNumber, "Delta Component Addition while"
                                        + " component is already present."));
                                break;
                            case REMOVED:
                                dbBaseComponents.remove(componentEntry);
                                break;
                            case MODIFIED:
                                dbBaseComponents.remove(componentEntry);
                                // serial number is not set because that couldn't have been modified
                                // and found this way
                                componentEntry.setManufacturer(deltaComponentResult.getManufacturer());
                                componentEntry.setModel(deltaComponentResult.getModel());
                                componentEntry.setRevisionNumber(deltaComponentResult.getRevisionNumber());
                                componentSerialMap.put(componentSerialNumber, componentEntry);
                                dbBaseComponents.add(componentEntry);
                                break;
                            default:
                                log.info("Default case that is already handled above");
                        }
                    } else {
                        if (nonSerialValues.contains(componentSerialNumber)) {
                            // if it is one of the non-unique values and save
                            // for now this does not handle modification WIP
                            ComponentResult componentResult = componentNonUniqueSerialMap.get(
                                    deltaComponentResult.hashCommonElements());
                            if (deltaComponentResult.getAttributeStatus() == AttributeStatus.ADDED) {
                                if (componentResult == null) {
                                    // valid case
                                    dbBaseComponents.add(deltaComponentResult);
                                } else {
                                    // can't add when it already exists
                                    componentAttributeRepository.save(new ComponentAttributeResult(
                                            deltaComponentResult.getId(),
                                            provisionSessionId,
                                            deltaComponentResult.getComponentClassStr(),
                                            "Delta Component Addition while"
                                                    + " component is already present."));
                                }
                            } else if (deltaComponentResult.getAttributeStatus()
                                    == AttributeStatus.REMOVED) {
                                if (componentResult == null) {
                                    // can't remove what doesn't exist
                                    componentAttributeRepository.save(new ComponentAttributeResult(
                                            deltaComponentResult.getId(),
                                            provisionSessionId,
                                            deltaComponentResult.getComponentClassStr(),
                                            "Delta Component Removal on non-existent component."));
                                } else {
                                    // valid case
                                    dbBaseComponents.remove(componentResult);
                                }
                            } else {
                                // this is the modified case.
                                // generate hash for delta that uses manufacturer, model, component class
                                // serial number because this is a special case that should only apply
                                // to the system bios
                                List<ComponentResult> cloneDbComponents = dbBaseComponents.stream().toList();
                                int deltaHash = Objects.hash(deltaComponentResult.getManufacturer(),
                                        deltaComponentResult.getModel(), componentSerialNumber,
                                        deltaComponentResult.getComponentClassValue());
                                int dbComponentHash;
                                for (ComponentResult result : cloneDbComponents) {
                                    // we have to manually search for this
                                    dbComponentHash = Objects.hash(result.getManufacturer(),
                                            result.getModel(), result.getSerialNumber(),
                                            result.getComponentClassValue());
                                    if (deltaHash == dbComponentHash) {
                                        dbBaseComponents.remove(result);
                                        // serial number is not set because that couldn't have been modified
                                        // and found this way
                                        result.setManufacturer(deltaComponentResult.getManufacturer());
                                        result.setModel(deltaComponentResult.getModel());
                                        result.setRevisionNumber(deltaComponentResult.getRevisionNumber());
                                        componentSerialMap.put(componentSerialNumber, result);
                                        dbBaseComponents.add(result);
                                    }
                                }
                            }
                        } else {
                            // if the is null and the status is added
                            if (deltaComponentResult.getAttributeStatus() == AttributeStatus.ADDED) {
                                dbBaseComponents.add(deltaComponentResult);
                                // add to the hash map as well in case a later delta removes/modifies it
                                componentSerialMap.put(componentSerialNumber, deltaComponentResult);
                            } else {
                                if (deltaComponentResult.getAttributeStatus() == AttributeStatus.REMOVED) {
                                    // problem, if the entry doesn't exist then it can't be removed
                                    componentAttributeRepository.save(new ComponentAttributeResult(
                                            deltaComponentResult.getId(),
                                            provisionSessionId,
                                            componentSerialNumber, "Delta Component Removal on"
                                            + " non-existent component."));
                                } else if (deltaComponentResult.getAttributeStatus()
                                        == AttributeStatus.MODIFIED) {
                                    // problem, can't modify what isn't there
                                    componentAttributeRepository.save(new ComponentAttributeResult(
                                            deltaComponentResult.getId(),
                                            provisionSessionId,
                                            componentSerialNumber, "Delta Component Modification "
                                            + "on non-existent component."));
                                }
                            }
                        }
                    }
                }
            }
        }

        return dbBaseComponents;
    }
}
