package hirs.attestationca.persist.validation;

import hirs.attestationca.persist.entity.ArchivableEntity;
import hirs.attestationca.persist.entity.manager.ComponentAttributeRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentAttributeResult;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import hirs.attestationca.persist.entity.userdefined.info.HardwareInfo;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.util.PciIds;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static hirs.attestationca.persist.enums.AppraisalStatus.Status.ERROR;
import static hirs.attestationca.persist.enums.AppraisalStatus.Status.FAIL;
import static hirs.attestationca.persist.enums.AppraisalStatus.Status.PASS;

@Log4j2
public class CertificateAttributeScvValidator extends SupplyChainCredentialValidator {

    private static final String LC_UNKNOWN = "unknown";

    /**
     * Checks if the delta credential's attributes are valid.
     * @param deltaPlatformCredential the delta credential to verify
     * @param deviceInfoReport The device info report containing
     *                         serial number of the platform to be validated.
     * @param basePlatformCredential the base credential from the same identity request
     *                              as the delta credential.
     * @param deltaMapping delta certificates associated with the
     *                          delta supply validation.
     * @return the result of the validation.
     */
    public static AppraisalStatus validateDeltaPlatformCredentialAttributes(
            final PlatformCredential deltaPlatformCredential,
            final DeviceInfoReport deviceInfoReport,
            final PlatformCredential basePlatformCredential,
            final Map<PlatformCredential, SupplyChainValidation> deltaMapping) {
        String message;

        // this needs to be a loop for all deltas, link to issue #110
        // check that they don't have the same serial number
        for (PlatformCredential pc : deltaMapping.keySet()) {
            if (!basePlatformCredential.getPlatformSerial()
                    .equals(pc.getPlatformSerial())) {
                message = String.format("Base and Delta platform serial "
                                + "numbers do not match (%s != %s)",
                        pc.getPlatformSerial(),
                        basePlatformCredential.getPlatformSerial());
                log.error(message);
                return new AppraisalStatus(FAIL, message);
            }
            // none of the deltas should have the serial number of the base
            if (!pc.isPlatformBase() && basePlatformCredential.getSerialNumber()
                    .equals(pc.getSerialNumber())) {
                message = String.format("Delta Certificate with same serial number as base. (%s)",
                        pc.getSerialNumber());
                log.error(message);
                return new AppraisalStatus(FAIL, message);
            }
        }

        // parse out the provided delta and its specific chain.
        List<ComponentIdentifier> origPcComponents
                = new LinkedList<>(basePlatformCredential.getComponentIdentifiers());

        return validateDeltaAttributesChainV2p0(
                deviceInfoReport, deltaMapping, origPcComponents);
    }

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
            log.info("Using device board serial number for validation: "
                    + deviceBaseboardSerialNumber);
        }

        if (StringUtils.isEmpty(deviceChassisSerialNumber)
                || DeviceInfoEnums.NOT_SPECIFIED.equalsIgnoreCase(deviceChassisSerialNumber)) {
            log.error("Failed to retrieve device chassis serial number");
        } else {
            deviceInfoSerialNumbers.put("chassis serial number", deviceChassisSerialNumber);
            log.info("Using device chassis serial number for validation: "
                    + deviceChassisSerialNumber);
        }
        if (StringUtils.isEmpty(deviceSystemSerialNumber)
                || DeviceInfoEnums.NOT_SPECIFIED.equalsIgnoreCase(deviceSystemSerialNumber)) {
            log.error("Failed to retrieve device system serial number");
        } else {
            deviceInfoSerialNumbers.put("system serial number", deviceSystemSerialNumber);
            log.info("Using device system serial number for validation: "
                    + deviceSystemSerialNumber);
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
     * @param platformCredential the Platform Credential
     * @param deviceInfoReport the Device Info Report
     * @param componentResultRepository db access to component result of mismatching
     * @param componentAttributeRepository  db access to component attribute match status
     * @param componentInfos list of device components
     * @param provisionSessionId UUID associated with the SCV Summary
     * @param ignoreRevisionAttribute policy flag to ignore the revision attribute
     * @return either PASS or FAIL
     */
    public static AppraisalStatus validatePlatformCredentialAttributesV2p0(
            final PlatformCredential platformCredential,
            final DeviceInfoReport deviceInfoReport,
            final ComponentResultRepository componentResultRepository,
            final ComponentAttributeRepository componentAttributeRepository,
            final List<ComponentInfo> componentInfos,
            final UUID provisionSessionId, final boolean ignoreRevisionAttribute) {
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
            log.warn("The Platform Certificate System version was {} and the reported Device System Information "
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

        // Retrieve the list of all components from the Platform Credential
        List<ComponentIdentifier> allPcComponents
                = new ArrayList<>(platformCredential.getComponentIdentifiers());

        // All components listed in the Platform Credential must have a manufacturer and model
        for (ComponentIdentifier pcComponent : allPcComponents) {
            fieldValidation = !hasEmptyValueForRequiredField("componentManufacturer",
                    pcComponent.getComponentManufacturer());

            if (!fieldValidation) {
                resultMessage.append("Component manufacturer is empty\n");
            }

            passesValidation &= fieldValidation;

            fieldValidation = !hasEmptyValueForRequiredField("componentModel",
                    pcComponent.getComponentModel());

            if (!fieldValidation) {
                resultMessage.append("Component model is empty\n");
            }

            passesValidation &= fieldValidation;
        }

        // There is no need to do comparisons with components that are invalid because
        // they did not have a manufacturer or model.
//        List<ComponentIdentifier> validPcComponents = allPcComponents.stream()
//                .filter(identifier -> identifier.getComponentManufacturer() != null
//                        && identifier.getComponentModel() != null)
//                .collect(Collectors.toList());

//        String paccorOutputString = deviceInfoReport.getPaccorOutputString();
//        String unmatchedComponents;

        // populate componentResults list
        List<ComponentResult> componentResults = componentResultRepository
                .findByCertificateSerialNumberAndBoardSerialNumber(
                        platformCredential.getSerialNumber().toString(),
                        platformCredential.getPlatformSerial());
        // first create hash map based on hashCode
        Map<Integer, List<ComponentInfo>> deviceHashMap = new HashMap<>();
        componentInfos.stream().forEach((componentInfo) -> {
            List<ComponentInfo> innerList;
            Integer compInfoHash = componentInfo.hashCommonElements();
            if (deviceHashMap.containsKey(compInfoHash)) {
                innerList = deviceHashMap.get(compInfoHash);
                innerList.add(componentInfo);
            } else {
                innerList = new ArrayList<>(0);
                innerList.add(componentInfo);
            }
            deviceHashMap.put(compInfoHash, innerList);
        });

        // Look for hash code in device mapping
        // if it exists, don't save the component
        List<ComponentResult> remainingComponentResults = new ArrayList<>();
        int numOfAttributes = 0;
        for (ComponentResult componentResult : componentResults) {
            if (!deviceHashMap.containsKey(componentResult.hashCommonElements())) {
                // didn't find the component result in the hashed mapping
                remainingComponentResults.add(componentResult);
            }
        }
        if (!remainingComponentResults.isEmpty()) {
            // continue down the options, move to a different method.
            // create component class mapping to component info
            Map<String, List<ComponentInfo>> componentDeviceMap = new HashMap<>();
            componentInfos.stream().forEach((componentInfo) -> {
                List<ComponentInfo> innerList;
                String componentClass = componentInfo.getComponentClass();
                if (componentDeviceMap.containsKey(componentClass)) {
                    innerList = componentDeviceMap.get(componentClass);
                    innerList.add(componentInfo);
                } else {
                    innerList = new ArrayList<>(0);
                    innerList.add(componentInfo);
                }
                componentDeviceMap.put(componentClass, innerList);
            });

            List<ComponentInfo> componentClassInfo;
            List<ComponentAttributeResult> attributeResults = new ArrayList<>();
            for (ComponentResult componentResult : remainingComponentResults) {
                componentClassInfo = componentDeviceMap.get(componentResult.getComponentClassValue());
                if (componentClassInfo.size() == 1) {
                    attributeResults.addAll(generateComponentResults(componentClassInfo.get(0), componentResult));
                } else {
                    attributeResults.addAll(findMismatchedValues(componentClassInfo, componentResult));
                }
            }

            for (ComponentAttributeResult componentAttributeResult : attributeResults) {
                if (componentAttributeResult.getAttribute().equalsIgnoreCase(ComponentResult.ATTRIBUTE_REVISION)
                        && !ignoreRevisionAttribute) {
                    componentAttributeResult.setProvisionSessionId(provisionSessionId);
                    componentAttributeRepository.save(componentAttributeResult);
                    fieldValidation &= componentAttributeResult.checkMatchedStatus();
                }
            }
            numOfAttributes = attributeResults.size();
        }

        StringBuilder additionalInfo = new StringBuilder();
        if (!remainingComponentResults.isEmpty()) {
            resultMessage.append(String.format("There are %d components not matched%n",
                    remainingComponentResults.size()));
            resultMessage.append(String.format("\twith %d total attributes mismatched.",
                    numOfAttributes));
        }

        passesValidation &= fieldValidation;

        if (passesValidation) {
            return new AppraisalStatus(PASS, PLATFORM_ATTRIBUTES_VALID);
        } else {
            return new AppraisalStatus(FAIL, resultMessage.toString(), additionalInfo.toString());
        }
    }

    /**
     * This method produces component attribute results for a single device that was found
     * by component class.
     * @param componentInfo the device object
     * @param componentResult the certificate expected object
     * @return a list of attribute match results
     */
    private static List<ComponentAttributeResult> generateComponentResults(
            final ComponentInfo componentInfo,
            final ComponentResult componentResult) {
        // there are instances of components with the same class (ie hard disks, memory)
        List<ComponentAttributeResult> attributeResults = new ArrayList<>();
        if (!componentInfo.getComponentManufacturer().equals(componentResult.getManufacturer())) {
            attributeResults.add(new ComponentAttributeResult(componentResult.getId(),
                    componentResult.getManufacturer(), componentInfo.getComponentManufacturer()));
        }

        if (!componentInfo.getComponentModel().equals(componentResult.getModel())) {
            attributeResults.add(new ComponentAttributeResult(componentResult.getId(),
                    componentResult.getModel(), componentInfo.getComponentModel()));
        }

        if (!componentInfo.getComponentSerial().equals(componentResult.getSerialNumber())) {
            attributeResults.add(new ComponentAttributeResult(componentResult.getId(),
                    componentResult.getSerialNumber(), componentInfo.getComponentSerial()));
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
     * @param componentClassInfo list of device components with the same class type
     * @param componentResult the certificate component that is mismatched
     * @return a list of attribute results, if all 4 attributes are never matched, it is not found
     */
    private static List<ComponentAttributeResult> findMismatchedValues(
            final List<ComponentInfo> componentClassInfo,
            final ComponentResult componentResult) {
        // this list only has those of the same class type
        Map<String, ComponentInfo> componentSerialMap = new HashMap<>();
        componentClassInfo.stream().forEach((componentInfo) -> {
            componentSerialMap.put(componentInfo.getComponentSerial(), componentInfo);
        });
        // see if the serial exists
        ComponentInfo componentInfo = componentSerialMap.get(componentResult.getSerialNumber());

        if (componentInfo != null && componentInfo.getComponentManufacturer()
                .equals(componentResult.getManufacturer())) {
            // the serial matched and the manufacturer, create attribute result and move on
            return generateComponentResults(componentInfo, componentResult);
        } else {
            // didn't find based on serial
            // look for highest match; otherwise ignore
            // I already know serial doesn't match
            for (ComponentInfo ci : componentClassInfo) {
                if (ci.getComponentManufacturer().equals(componentResult.getManufacturer())
                        && ci.getComponentModel().equals(componentResult.getModel())) {
                    return generateComponentResults(ci, componentResult);
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * The main purpose of this method, the in process of validation, is to
     * pick out the changes that lead to the delta cert and make sure the changes
     * are valid.
     *
     * @param deviceInfoReport The paccor profile of device being validated against.
     * @param deltaMapping map of delta certificates to their validated status
     * @param origPcComponents The component identifier list associated with the
     * base cert for this specific chain
     * @return Appraisal Status of delta being validated.
     */
    @SuppressWarnings("methodlength")
    static AppraisalStatus validateDeltaAttributesChainV2p0(
            final DeviceInfoReport deviceInfoReport,
            final Map<PlatformCredential, SupplyChainValidation> deltaMapping,
            final List<ComponentIdentifier> origPcComponents) {
        boolean fieldValidation = true;
        StringBuilder resultMessage = new StringBuilder();
        String tempStringMessage = "";
        List<ComponentIdentifier> validOrigPcComponents = origPcComponents.stream()
                .filter(identifier -> identifier.getComponentManufacturer() != null
                        && identifier.getComponentModel() != null)
                .collect(Collectors.toList());
        List<PlatformCredential> chainCertificates = new LinkedList<>(deltaMapping.keySet());

        // map the components throughout the chain
        List<ComponentIdentifier> baseCompList = new LinkedList<>(validOrigPcComponents);

        Collections.sort(chainCertificates, new Comparator<PlatformCredential>() {
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
        resultMessage.append("There are errors with Delta "
                + "Component Statuses:\n");
        List<ComponentIdentifier> leftOverDeltas = new ArrayList<>();
        List<ComponentIdentifier> absentSerialNum = new ArrayList<>();
        tempStringMessage = validateDeltaChain(deltaMapping, baseCompList,
                leftOverDeltas, absentSerialNum, chainCertificates);

        // check if there were any issues
        if (!tempStringMessage.isEmpty()) {
            resultMessage.append(tempStringMessage);
            fieldValidation = false;
        }

        // finished up
        List<ArchivableEntity> certificateList = null;
        SupplyChainValidation scv = null;
        StringBuilder deltaSb = new StringBuilder();

        // non-empty serial values
        for (ComponentIdentifier deltaCi : leftOverDeltas) {
            String classValue;
            ComponentIdentifierV2 ciV2 = (ComponentIdentifierV2) deltaCi;
            ComponentIdentifierV2 baseCiV2;
            boolean classFound;

            for (ComponentIdentifier ci : absentSerialNum) {
                classValue = ciV2.getComponentClass().getComponentIdentifier();
                baseCiV2 = (ComponentIdentifierV2) ci;
                classFound = classValue.equals(baseCiV2.getComponentClass()
                        .getComponentIdentifier());
                if (classFound) {
                    if (isMatch(ciV2, baseCiV2)) {
                        if (ciV2.isAdded() || ciV2.isModified()) {
                            // since the base list doesn't have this ci
                            // just add the delta
                            baseCompList.add(deltaCi);
                            break;
                        }
                        if (ciV2.isRemoved()) {
                            baseCompList.remove(ciV2);
                            break;
                        }
                        // if it is a remove
                        // we do nothing because baseCompList doesn't have it
                    } else {
                        // it is an add
                        if (ciV2.isAdded()) {
                            baseCompList.add(deltaCi);
                        }
                    }
                } else {
                    // delta change to a class not there
                    if (ciV2.isAdded()) {
                        baseCompList.add(deltaCi);
                    }

                    if (ciV2.isModified()) {
                        // error because you can't modify something
                        // that isn't here
                        resultMessage.append("MODIFIED attempted without prior instance\n");
                        deltaSb.append(String.format("%s;", ci.hashCode()));
                    }

                    if (ciV2.isRemoved()) {
                        // error because you can't remove something
                        // that isn't here
                        resultMessage.append("REMOVED attempted without prior instance\n");
                        deltaSb.append(String.format("%s;", ci.hashCode()));
                    }
                }
            }
        }

        if (!fieldValidation || !deltaSb.toString().isEmpty()) {
            deltaSb.insert(0, "COMPID=");
            return new AppraisalStatus(FAIL, resultMessage.toString(), deltaSb.toString());
        }

        String paccorOutputString = deviceInfoReport.getPaccorOutputString();
        String unmatchedComponents;
        try {
            // compare based on component class
            List<ComponentInfo> componentInfoList = getComponentInfoFromPaccorOutput(
                    deviceInfoReport.getNetworkInfo().getHostname(),
                    paccorOutputString);
            // this is what I want to rewrite
            unmatchedComponents = validateV2PlatformCredentialAttributes(
                    baseCompList,
                    componentInfoList);
            fieldValidation &= unmatchedComponents.isEmpty();
        } catch (IOException ioEx) {
            final String baseErrorMessage = "Error parsing JSON output from PACCOR: ";
            log.error(baseErrorMessage + ioEx.toString());
            log.error("PACCOR output string:\n" + paccorOutputString);
            return new AppraisalStatus(ERROR, baseErrorMessage + ioEx.getMessage());
        }
        StringBuilder additionalInfo = new StringBuilder();
        if (!fieldValidation) {
            resultMessage = new StringBuilder();
            resultMessage.append("There are unmatched components:\n");
            resultMessage.append(unmatchedComponents);

            // pass information of which ones failed in additionInfo
            int counter = 0;
            for (ComponentIdentifier ci : baseCompList) {
                counter++;
                additionalInfo.append(String.format("%d;", ci.hashCode()));
            }
            if (counter > 0) {
                additionalInfo.insert(0, "COMPID=");
                additionalInfo.append(counter);
            }
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
                if (cInfo.getComponentClass().contains(
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
                    ci = PciIds.translate((ComponentIdentifierV2) ci);
                }
                log.error("Unmatched component: " + ci);
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

    private static String validateDeltaChain(
            final Map<PlatformCredential, SupplyChainValidation> deltaMapping,
            final List<ComponentIdentifier> baseCompList,
            final List<ComponentIdentifier> leftOvers,
            final List<ComponentIdentifier> absentSerials,
            final List<PlatformCredential> chainCertificates) {
        StringBuilder resultMessage = new StringBuilder();
        List<String> noneSerialValues = new ArrayList<>();
        noneSerialValues.add("");
        noneSerialValues.add(null);
        noneSerialValues.add("Not Specified");
        noneSerialValues.add("To Be Filled By O.E.M.");

        // map the components throughout the chain
        Map<String, ComponentIdentifier> chainCiMapping = new HashMap<>();
        baseCompList.stream().forEach((ci) -> {
            if (!noneSerialValues.contains(ci.getComponentSerial().toString())) {
                chainCiMapping.put(ci.getComponentSerial().toString(), ci);
            } else {
                absentSerials.add(ci);
            }
        });

        String ciSerial;
        List<ArchivableEntity> certificateList = null;
        SupplyChainValidation scv = null;
        // go through the leaf and check the changes against the valid components
        // forget modifying validOrigPcComponents
        for (PlatformCredential delta : chainCertificates) {
            StringBuilder failureMsg = new StringBuilder();
            certificateList = new ArrayList<>();
            certificateList.add(delta);

            for (ComponentIdentifier ci : delta.getComponentIdentifiers()) {
                if (!noneSerialValues.contains(ci.getComponentSerial().toString())) {
                    if (ci.isVersion2()) {
                        ciSerial = ci.getComponentSerial().toString();
                        ComponentIdentifierV2 ciV2 = (ComponentIdentifierV2) ci;
                        if (ciV2.isModified()) {
                            // this won't match
                            // check it is there
                            if (chainCiMapping.containsKey(ciSerial)) {
                                chainCiMapping.put(ciSerial, ci);
                            } else {
                                failureMsg.append(String.format(
                                        "%s attempted MODIFIED with no prior instance.%n",
                                        ciSerial));
                                delta.setComponentFailures(String.format("%s,%d",
                                        delta.getComponentFailures(), ciV2.hashCode()));
                                scv = deltaMapping.get(delta);
                                if (scv != null
                                        && scv.getValidationResult() != PASS) {
                                    failureMsg.append(scv.getMessage());
                                }
                                deltaMapping.put(delta, new SupplyChainValidation(
                                        SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
                                        FAIL,
                                        certificateList,
                                        failureMsg.toString()));
                            }
                        } else if (ciV2.isRemoved()) {
                            if (!chainCiMapping.containsKey(ciSerial)) {
                                // error thrown, can't remove if it doesn't exist
                                failureMsg.append(String.format(
                                        "%s attempted REMOVED with no prior instance.%n",
                                        ciSerial));
                                delta.setComponentFailures(String.format("%s,%d",
                                        delta.getComponentFailures(), ciV2.hashCode()));
                                scv = deltaMapping.get(delta);
                                if (scv != null
                                        && scv.getValidationResult() != PASS) {
                                    failureMsg.append(scv.getMessage());
                                }
                                deltaMapping.put(delta, new SupplyChainValidation(
                                        SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
                                        FAIL,
                                        certificateList,
                                        failureMsg.toString()));
                            } else {
                                chainCiMapping.remove(ciSerial);
                            }
                        } else if (ciV2.isAdded()) {
                            // ADDED
                            if (chainCiMapping.containsKey(ciSerial)) {
                                // error, shouldn't exist
                                failureMsg.append(String.format(
                                        "%s was ADDED, the serial already exists.%n",
                                        ciSerial));
                                delta.setComponentFailures(String.format("%s,%d",
                                        delta.getComponentFailures(), ciV2.hashCode()));
                                scv = deltaMapping.get(delta);
                                if (scv != null
                                        && scv.getValidationResult() != PASS) {
                                    failureMsg.append(scv.getMessage());
                                }
                                deltaMapping.put(delta, new SupplyChainValidation(
                                        SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
                                        FAIL,
                                        certificateList,
                                        failureMsg.toString()));
                            } else {
                                // have to add in case later it is removed
                                chainCiMapping.put(ciSerial, ci);
                            }
                        }
                    }
                } else {
                    if (ci.isVersion2() && ((ComponentIdentifierV2) ci).isModified()) {
                        ComponentIdentifierV2 ciV2 = (ComponentIdentifierV2) ci;
                        // Look for singular component of same make/model/class
                        ComponentIdentifier candidate = null;
                        for (ComponentIdentifier search : absentSerials) {
                            if (!search.isVersion2()) {
                                continue;
                            }
                            ComponentIdentifierV2 noSerialV2 = (ComponentIdentifierV2) search;

                            if (noSerialV2.getComponentClass().getComponentIdentifier().equals(
                                    ciV2.getComponentClass().getComponentIdentifier())
                                    && isMatch(noSerialV2, ciV2)) {
                                if (candidate == null) {
                                    candidate = noSerialV2;
                                } else {
                                    // This only works if there is one matching component
                                    candidate = null;
                                    break;
                                }
                            }
                        }

                        if (candidate != null) {
                            absentSerials.remove(candidate);
                            absentSerials.add(ciV2);
                        } else {
                            leftOvers.add(ci);
                        }
                    } else {
                        // found a delta ci with no serial
                        // add to list
                        leftOvers.add(ci);
                    }
                }
            }

            resultMessage.append(failureMsg.toString());
        }
        baseCompList.clear();
        baseCompList.addAll(chainCiMapping.values());
        baseCompList.addAll(absentSerials);

        return resultMessage.toString();
    }

    /**
     * Compares the component information from the device info report against those of the
     * platform credential. All components in the platform credential should exactly match one
     * component in the device info report.  The device info report is allowed to have extra
     * components not represented in the platform credential.
     *
     * @param untrimmedPcComponents the platform credential components (may contain end whitespace)
     *                              **NEW** this is updated with just the unmatched components
     *                              if there are any failures, otherwise it remains unchanged.
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
        pcComponents.forEach(pcComp -> manufacturerSet.add(pcComp.getComponentManufacturer()));

        // Create a list for unmatched components across all manufacturers to display at the end.
        List<ComponentIdentifier> pcUnmatchedComponents = new ArrayList<>();

        for (ASN1UTF8String derUtf8Manufacturer : manufacturerSet) {
            List<ComponentIdentifier> pcComponentsFromManufacturer
                    = pcComponents.stream().filter(compIdentifier
                    -> compIdentifier.getComponentManufacturer().equals(derUtf8Manufacturer))
                    .collect(Collectors.toList());

            String pcManufacturer = derUtf8Manufacturer.getString();
            List<ComponentInfo> deviceInfoComponentsFromManufacturer
                    = allDeviceInfoComponents.stream().filter(componentInfo
                    -> componentInfo.getComponentManufacturer().equals(pcManufacturer))
                    .collect(Collectors.toList());
            // For each component listed in the platform credential from this manufacturer
            // find the ones that specify a serial number so we can match the most specific ones
            // first.
            List<ComponentIdentifier> pcComponentsFromManufacturerWithSerialNumber
                    = pcComponentsFromManufacturer.stream().filter(compIdentifier
                    -> compIdentifier.getComponentSerial() != null
                    && StringUtils.isNotEmpty(compIdentifier.getComponentSerial().getString()))
                    .collect(Collectors.toList());
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
                    .collect(Collectors.toList());
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
            log.error(String.format("Platform Credential contained %d unmatched components:",
                    pcUnmatchedComponents.size()));

            int unmatchedComponentCounter = 1;
            for (ComponentIdentifier unmatchedComponent : pcUnmatchedComponents) {
                if (unmatchedComponent.isVersion2() && PciIds.DB.isReady()) {
                    unmatchedComponent =
                            PciIds.translate((ComponentIdentifierV2) unmatchedComponent);
                }
                log.error("Unmatched component " + unmatchedComponentCounter++ + ": "
                        + unmatchedComponent);
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
     * @param pcComponent the platform credential component
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

    /**
     * Checks if the fields in the potentialMatch match the fields in the pcComponent,
     * or if the relevant field in the pcComponent is empty.
     * @param pcComponent the platform credential component
     * @param potentialMatch the component info from a device info report
     * @return true if the fields match exactly (null is considered the same as an empty string)
     */
    private static boolean isMatch(final ComponentIdentifierV2 pcComponent,
                           final ComponentIdentifierV2 potentialMatch) {
        boolean matchesSoFar = true;

        matchesSoFar &= isMatchOrEmptyInPlatformCert(
                potentialMatch.getComponentManufacturer(),
                pcComponent.getComponentManufacturer());

        matchesSoFar &= isMatchOrEmptyInPlatformCert(
                potentialMatch.getComponentModel(),
                pcComponent.getComponentModel());

        return matchesSoFar;
    }

    private static boolean isMatchOrEmptyInPlatformCert(
            final String evidenceFromDevice,
            final ASN1UTF8String valueInPlatformCert) {
        if (valueInPlatformCert == null || StringUtils.isEmpty(valueInPlatformCert.getString())) {
            return true;
        }
        return valueInPlatformCert.getString().equals(evidenceFromDevice);
    }

    private static boolean isMatchOrEmptyInPlatformCert(
            final ASN1UTF8String evidenceFromDevice,
            final ASN1UTF8String valueInPlatformCert) {
        return evidenceFromDevice.equals(valueInPlatformCert);
    }

    /**
     * Validates the platform credential's serial numbers with the device info's set of
     * serial numbers.
     * @param credentialBoardSerialNumber the PC board S/N
     * @param credentialChassisSerialNumber the PC chassis S/N
     * @param deviceInfoSerialNumbers the map of device info serial numbers with descriptions.
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
     * @param platformSerialNumber the platform serial number to compare
     * @param platformSerialNumberDescription description of the serial number for logging purposes.
     * @param deviceInfoSerialNumbers the map of device info serial numbers
     *                                (key = description, value = serial number)
     * @return true if the platform serial number was found (case-insensitive search),
     *          false otherwise
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
     * @param platformCredentialFieldName name of field to be compared
     * @param platformCredentialFieldValue first value to compare
     * @param otherValue second value to compare
     * @return true if values match
     */
    private static boolean requiredPlatformCredentialFieldIsNonEmptyAndMatches(
            final String platformCredentialFieldName,
            final String platformCredentialFieldValue,
            final String otherValue) {
        if (hasEmptyValueForRequiredField(platformCredentialFieldName,
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
     * @param platformCredentialFieldName name of field to be compared
     * @param platformCredentialFieldValue first value to compare
     * @param otherValue second value to compare
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
     * @param description description of the value
     * @param fieldValue value of the field
     * @return true if fieldValue is null or empty; false otherwise
     */
    private static boolean hasEmptyValueForRequiredField(final String description,
                                                         final String fieldValue) {
        if (StringUtils.isEmpty(fieldValue)) {
            log.error("Required field was empty or null in Platform Credential: "
                    + description);
            return true;
        }
        return false;
    }

    /**
     * Per update to the provisioning via Issue 723, Not Specified and Unknown values
     * are to be ignored.
     * @param versionNumber string value of the device/platform version number
     * @return true if they equal Not Specified or Unknown
     */
    public static boolean isNotSpecifiedOrUnknown(final String versionNumber) {
        if(versionNumber == null) {
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
            log.debug(String.format("%s field in Platform Credential (%s) does not match "
                            + "a related field in the DeviceInfoReport (%s)",
                    platformCredentialFieldName, trimmedFieldValue, trimmedOtherValue));
            return false;
        }

        log.debug(String.format("%s field in Platform Credential matches "
                        + "a related field in the DeviceInfoReport (%s)",
                platformCredentialFieldName, trimmedFieldValue)
        );

        return true;
    }

    /**
     * Returns true if fieldValue is null or empty.
     * @param description description of the value
     * @param fieldValue value of the field
     * @return true if fieldValue is null or empty; false otherwise
     */
    private static boolean hasEmptyValueForRequiredField(final String description,
                                                         final ASN1UTF8String fieldValue) {
        if (fieldValue == null || StringUtils.isEmpty(fieldValue.getString().trim())) {
            log.error("Required field was empty or null in Platform Credential: "
                    + description);
            return true;
        }
        return false;
    }
}
