package hirs.validation;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hirs.data.persist.AppraisalStatus;
import hirs.data.persist.ComponentInfo;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.HardwareInfo;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.data.persist.certificate.attributes.ComponentIdentifier;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.CertException;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.springframework.stereotype.Service;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static hirs.data.persist.AppraisalStatus.Status.ERROR;
import static hirs.data.persist.AppraisalStatus.Status.FAIL;
import static hirs.data.persist.AppraisalStatus.Status.PASS;


/**
 * Validates elements of the supply chain.
 */
@Service
public final class SupplyChainCredentialValidator implements CredentialValidator {
    private static final int NUC_VARIABLE_BIT = 159;

    private static final Logger LOGGER = LogManager.getLogger(
            SupplyChainCredentialValidator.class);

    /**
     * AppraisalStatus message for a valid endorsement credential appraisal.
     */
    public static final String ENDORSEMENT_VALID = "Endorsement credential validated";

    /**
     * AppraisalStatus message for a valid platform credential appraisal.
     */
    public static final String PLATFORM_VALID = "Platform credential validated";

    /**
     * AppraisalStatus message for a valid platform credential attributes appraisal.
     */
    public static final String PLATFORM_ATTRIBUTES_VALID =
            "Platform credential attributes validated";

    /*
     * Ensure that BouncyCastle is configured as a javax.security.Security provider, as this
     * class expects it to be available.
     */
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Default constructor, should only be instantiated for testing.
     */
    public SupplyChainCredentialValidator() {

    }

    /**
     * Parses the output from PACCOR's allcomponents.sh script into ComponentInfo objects.
     * @param paccorOutput the output from PACCOR's allcomoponents.sh
     * @return a list of ComponentInfo objects built from paccorOutput
     * @throws IOException if something goes wrong parsing the JSON
     */
    public static List<ComponentInfo> getComponentInfoFromPaccorOutput(final String paccorOutput)
            throws IOException {
        List<ComponentInfo> componentInfoList = new ArrayList<>();

        if (StringUtils.isNotEmpty(paccorOutput)) {
            ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
            JsonNode rootNode = objectMapper.readTree(paccorOutput);
            Iterator<JsonNode> jsonComponentNodes
                    = rootNode.findValue("COMPONENTS").elements();
            while (jsonComponentNodes.hasNext()) {
                JsonNode next = jsonComponentNodes.next();
                componentInfoList.add(new ComponentInfo(
                        getJSONNodeValueAsText(next, "MANUFACTURER"),
                        getJSONNodeValueAsText(next, "MODEL"),
                        getJSONNodeValueAsText(next, "SERIAL"),
                        getJSONNodeValueAsText(next, "REVISION")));
            }
        }

        return componentInfoList;
    }

    private static String getJSONNodeValueAsText(final JsonNode node, final String fieldName) {
        if (node.hasNonNull(fieldName)) {
            return node.findValue(fieldName).asText();
        }
        return null;

    }

    /**
     * Checks if the platform credential is valid.
     *
     * @param pc The platform credential to verify.
     * @param trustStore trust store holding trusted certificates.
     * @param acceptExpired whether or not to accept expired certificates as valid.
     * @return The result of the validation.
     */
    @Override
    public AppraisalStatus validatePlatformCredential(final PlatformCredential pc,
                                                     final KeyStore trustStore,
                                                     final boolean acceptExpired) {
        final String baseErrorMessage = "Can't validate platform credential without ";
        String message;
        if (pc == null) {
            message = baseErrorMessage + "a platform credential";
            LOGGER.error(message);
            return new AppraisalStatus(FAIL, message);
        }
        try {
            if (trustStore == null || trustStore.size() == 0) {
                message = baseErrorMessage + "a trust store";
                LOGGER.error(message);
                return new AppraisalStatus(FAIL, message);

            }
        } catch (KeyStoreException e) {
            message = baseErrorMessage + "an intitialized trust store";
            LOGGER.error(message);
            return new AppraisalStatus(FAIL, message);
        }

        X509AttributeCertificateHolder attributeCert = null;
        try {
            attributeCert = pc.getX509AttributeCertificateHolder();
        } catch (IOException e) {
            message = "Could not retrieve X509 Attribute certificate";
            LOGGER.error(message, e);
            return new AppraisalStatus(FAIL, message + " " + e.getMessage());
        }

        // check validity period, currently acceptExpired will also accept not yet
        // valid certificates
        if (!acceptExpired && !pc.isValidOn(new Date())) {
            message = "Platform credential has expired";
            // if not valid at the current time
            LOGGER.warn(message);
            return new AppraisalStatus(FAIL, message);
        }

        // verify cert against truststore
        try {
            if (verifyCertificate(attributeCert, trustStore)) {
                message = PLATFORM_VALID;
                LOGGER.info(message);
                return new AppraisalStatus(PASS, message);
            } else {
                message = "Platform credential failed verification";
                LOGGER.error(message);
                return new AppraisalStatus(FAIL, message);
            }
        } catch (SupplyChainValidatorException e) {
            message = "An error occurred indicating the credential is not valid";
            LOGGER.warn(message, e);
            return new AppraisalStatus(FAIL, message + " " + e.getMessage());
        }
    }

    /**
     * Checks if the platform credential's attributes are valid.
     * @param platformCredential The platform credential to verify.
     * @param deviceInfoReport The device info report containing
     *                         serial number of the platform to be validated.
     * @param endorsementCredential The endorsement credential supplied from the same
     *          identity request as the platform credential.
     * @return The result of the validation.
     */
    @Override
    public AppraisalStatus validatePlatformCredentialAttributes(
            final PlatformCredential platformCredential,
            final DeviceInfoReport deviceInfoReport,
            final EndorsementCredential endorsementCredential) {
        final String baseErrorMessage = "Can't validate platform credential attributes without ";
        String message;
        if (platformCredential == null) {
            message = baseErrorMessage + "a platform credential";
            LOGGER.error(message);
            return new AppraisalStatus(FAIL, message);
        }
        if (deviceInfoReport == null) {
            message = baseErrorMessage + "a device info report";
            LOGGER.error(message);
            return new AppraisalStatus(FAIL, message);
        }
        if (endorsementCredential == null) {
            message = baseErrorMessage + "an endorsement credential";
            LOGGER.error(message);
            return new AppraisalStatus(FAIL, message);
        }

        // Quick, early check if the platform credential references the endorsement credential
        if (!endorsementCredential.getSerialNumber()
                .equals(platformCredential.getHolderSerialNumber())) {
            message = "Platform Credential holder serial number does not match "
                    + "the Endorsement Credential's serial number";
            LOGGER.error(message);
            return new AppraisalStatus(FAIL, message);
        }

        String credentialType = platformCredential.getCredentialType();
        if (PlatformCredential.CERTIFICATE_TYPE_2_0.equals(credentialType)) {
            return validatePlatformCredentialAttributesV2p0(platformCredential, deviceInfoReport);
        }
        return validatePlatformCredentialAttributesV1p2(platformCredential, deviceInfoReport);
    }

    private static AppraisalStatus validatePlatformCredentialAttributesV1p2(
            final PlatformCredential platformCredential,
            final DeviceInfoReport deviceInfoReport) {

        // check the device's board serial number, and compare against this platform credential's
        // board serial number.
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
                || DeviceInfoReport.NOT_SPECIFIED.equalsIgnoreCase(deviceBaseboardSerialNumber)) {
            LOGGER.error("Failed to retrieve device baseboard serial number");
            deviceBaseboardSerialNumber = null;
        } else {
            deviceInfoSerialNumbers.put("board serial number", deviceBaseboardSerialNumber);
            LOGGER.info("Using device board serial number for validation: "
                    + deviceBaseboardSerialNumber);
        }

        if (StringUtils.isEmpty(deviceChassisSerialNumber)
                || DeviceInfoReport.NOT_SPECIFIED.equalsIgnoreCase(deviceChassisSerialNumber)) {
            LOGGER.error("Failed to retrieve device chassis serial number");
        } else {
            deviceInfoSerialNumbers.put("chassis serial number", deviceChassisSerialNumber);
            LOGGER.info("Using device chassis serial number for validation: "
                    + deviceChassisSerialNumber);
        }
        if (StringUtils.isEmpty(deviceSystemSerialNumber)
                || DeviceInfoReport.NOT_SPECIFIED.equalsIgnoreCase(deviceSystemSerialNumber)) {
            LOGGER.error("Failed to retrieve device system serial number");
        } else {
            deviceInfoSerialNumbers.put("system serial number", deviceSystemSerialNumber);
            LOGGER.info("Using device system serial number for validation: "
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
            LOGGER.debug("Credential Serial Number was null");
            if (StringUtils.isEmpty(deviceBaseboardSerialNumber)) {
                message = "Device Serial Number was null";
                LOGGER.error(message);
                status = new AppraisalStatus(FAIL, message);
            } else {
                // Calculate the SHA1 hash of the UTF8 encoded baseboard serial number
                BigInteger baseboardSha1 = new BigInteger(1,
                        DigestUtils.sha1(deviceBaseboardSerialNumber.getBytes(Charsets.UTF_8)));
                BigInteger certificateSerialNumber = platformCredential.getSerialNumber();

                // compare the SHA1 hash of the baseboard serial number to the certificate SN
                if (certificateSerialNumber != null
                        && certificateSerialNumber.equals(baseboardSha1)) {
                    LOGGER.info("Device Baseboard Serial Number matches "
                            + "the Certificate Serial Number");
                    status = new AppraisalStatus(PASS, PLATFORM_ATTRIBUTES_VALID);
                } else if (certificateSerialNumber != null
                        && certificateSerialNumber.equals(
                                baseboardSha1.clearBit(NUC_VARIABLE_BIT))) {
                    LOGGER.info("Warning! The Certificate serial number had the most significant "
                            + "bit truncated.  159 bits of it matched the device baseboard "
                            + "serial number.");
                    status = new AppraisalStatus(PASS, PLATFORM_ATTRIBUTES_VALID);
                } else {
                    message = "The SHA1 hash of the Device Baseboard Serial Number "
                            + deviceBaseboardSerialNumber
                            + " did not match the Certificate's Serial Number";
                    LOGGER.error(message);
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
     * @return either PASS or FAIL
     */
    static AppraisalStatus validatePlatformCredentialAttributesV2p0(
            final PlatformCredential platformCredential,
            final DeviceInfoReport deviceInfoReport) {
        boolean passesValidation = true;

        StringBuffer resultMessage = new StringBuffer();

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

        fieldValidation = requiredPlatformCredentialFieldIsNonEmptyAndMatches(
                "PlatformVersion",
                platformCredential.getVersion(),
                hardwareInfo.getVersion());

        if (!fieldValidation) {
            resultMessage.append("Platform version did not match\n");
        }

        passesValidation &= fieldValidation;

        // check PlatformSerial against both system-serial-number and baseboard-serial-number
        fieldValidation = (
                (
                optionalPlatformCredentialFieldNullOrMatches(
                    "PlatformSerial",
                    platformCredential.getPlatformSerial(),
                    hardwareInfo.getSystemSerialNumber())
                ) || (
                optionalPlatformCredentialFieldNullOrMatches(
                        "PlatformSerial",
                        platformCredential.getPlatformSerial(),
                        hardwareInfo.getBaseboardSerialNumber())
                )
        );

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
        List<ComponentIdentifier> validPcComponents = allPcComponents.stream()
                .filter(identifier -> identifier.getComponentManufacturer() != null
                        && identifier.getComponentModel() != null)
                .collect(Collectors.toList());

        String paccorOutputString = deviceInfoReport.getPaccorOutputString();
        try {
            List<ComponentInfo> componentInfoList
                    = getComponentInfoFromPaccorOutput(paccorOutputString);
            fieldValidation &= validateV2p0PlatformCredentialComponentsExpectingExactMatch(
                    validPcComponents, componentInfoList);
        } catch (IOException e) {
            final String baseErrorMessage = "Error parsing JSON output from PACCOR: ";
            LOGGER.error(baseErrorMessage + e.toString());
            LOGGER.error("PACCOR output string:\n" + paccorOutputString);
            return new AppraisalStatus(ERROR, baseErrorMessage + e.getMessage());
        }

        if (!fieldValidation) {
            resultMessage.append("There are unmatched components\n");
        }

        passesValidation &= fieldValidation;

        if (passesValidation) {
            return new AppraisalStatus(PASS, PLATFORM_ATTRIBUTES_VALID);
        } else {
            return new AppraisalStatus(FAIL, resultMessage.toString());
        }
    }

    /**
     * Compares the component information from the device info report against those of the
     * platform credential. All components in the platform credential should exactly match one
     * component in the device info report.  The device info report is allowed to have extra
     * components not represented in the platform credential.
     *
     * @param untrimmedPcComponents the platform credential components (may contain end whitespace)
     * @param allDeviceInfoComponents the device info report components
     * @return true if validation passes
     */
    private static boolean validateV2p0PlatformCredentialComponentsExpectingExactMatch(
            final List<ComponentIdentifier> untrimmedPcComponents,
            final List<ComponentInfo> allDeviceInfoComponents) {
        // For each manufacturer listed in the platform credential, create two lists:
        // 1. a list of components listed in the platform credential for the manufacturer, and
        // 2. a list of components listed in the device info for the same manufacturer
        // Then eliminate matches from both lists. Finally, decide if the validation passes based
        // on the leftovers in the lists and the policy in place.
        final List<ComponentIdentifier> pcComponents = new ArrayList<>();
        for (ComponentIdentifier component : untrimmedPcComponents) {
            DERUTF8String componentSerial = new DERUTF8String("");
            DERUTF8String componentRevision = new DERUTF8String("");
            if (component.getComponentSerial() != null) {
                componentSerial = new DERUTF8String(
                        component.getComponentSerial().getString().trim());
            }
            if (component.getComponentRevision() != null) {
                componentRevision = new DERUTF8String(
                        component.getComponentRevision().getString().trim());
            }
            pcComponents.add(
                new ComponentIdentifier(
                        new DERUTF8String(component.getComponentManufacturer().getString().trim()),
                        new DERUTF8String(component.getComponentModel().getString().trim()),
                        componentSerial,
                        componentRevision,
                        component.getComponentManufacturerId(),
                        component.getFieldReplaceable(),
                        component.getComponentAddress()
                ));
        }

        LOGGER.info("Validating the following Platform Cert components...");
        pcComponents.forEach(component -> LOGGER.info(component.toString()));
        LOGGER.info("...against the the following DeviceInfoReport components:");
        allDeviceInfoComponents.forEach(component -> LOGGER.info(component.toString()));

        Set<DERUTF8String> manufacturerSet = new HashSet<>();
        pcComponents.forEach(component -> manufacturerSet.add(
                component.getComponentManufacturer()));

        // Create a list for unmatched components across all manufacturers to display at the end.
        List<ComponentIdentifier> pcUnmatchedComponents = new ArrayList<>();

        for (DERUTF8String derUtf8Manufacturer : manufacturerSet) {
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
                        .filter(componentInfo
                                -> componentInfo.getComponentSerial()
                                .equals(pcComponent.getComponentSerial().getString()))
                        .findFirst();

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
                        .filter(info
                                -> StringUtils.isNotEmpty(info.getComponentRevision()))
                        .filter(info
                                -> info.getComponentRevision()
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
                ComponentIdentifier pcComponent = ci;
                Iterator<ComponentInfo> diComponentIter
                        = deviceInfoComponentsFromManufacturer.iterator();
                while (diComponentIter.hasNext()) {
                    ComponentInfo potentialMatch = diComponentIter.next();
                    if (isMatch(pcComponent, potentialMatch)) {
                        pcComponentsFromManufacturer.remove(ci);
                        diComponentIter.remove();
                    }
                }
            }
            pcUnmatchedComponents.addAll(pcComponentsFromManufacturer);
        }

        if (!pcUnmatchedComponents.isEmpty()) {
            LOGGER.error(String.format(
                    "Platform Credential contained %d unmatched components:",
                    pcUnmatchedComponents.size()));

            int umatchedComponentCounter = 1;
            for (ComponentIdentifier unmatchedComponent : pcUnmatchedComponents) {
                LOGGER.error("Unmatched component " + umatchedComponentCounter++ + ": "
                        + unmatchedComponent);
            }
            return false;
        }
        return true;
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
            LOGGER.error("Required field was empty or null in Platform Credential: "
                    + description);
            return true;
        }
        return false;
    }

    /**
     * Returns true if fieldValue is null or empty.
     * @param description description of the value
     * @param fieldValue value of the field
     * @return true if fieldValue is null or empty; false otherwise
     */
    private static boolean hasEmptyValueForRequiredField(final String description,
                                                  final DERUTF8String fieldValue) {
        if (fieldValue == null || StringUtils.isEmpty(fieldValue.getString().trim())) {
            LOGGER.error("Required field was empty or null in Platform Credential: "
                    + description);
            return true;
        }
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

    private static boolean platformCredentialFieldMatches(
            final String platformCredentialFieldName,
            final String platformCredentialFieldValue,
            final String otherValue) {
        String trimmedFieldValue = platformCredentialFieldValue.trim();
        String trimmedOtherValue = otherValue.trim();

        if (!trimmedFieldValue.equals(trimmedOtherValue)) {
            LOGGER.debug(String.format("%s field in Platform Credential (%s) does not match "
                            + "a related field in the DeviceInfoReport (%s)",
                    platformCredentialFieldName, trimmedFieldValue, trimmedOtherValue));
            return false;
        }

        LOGGER.debug(String.format("%s field in Platform Credential matches "
                + "a related field in the DeviceInfoReport (%s)",
                platformCredentialFieldName, trimmedFieldValue)
        );

        return true;
    }

    /**
     * Checks if the fields in the potentialMatch match the fields in the pcComponent,
     * or if the relevant field in the pcComponent is empty.
     * @param pcComponent the platform credential component
     * @param potentialMatch the component info from a device info report
     * @return true if the fields match exactly (null is considered the same as an empty string)
     */
    static boolean isMatch(final ComponentIdentifier pcComponent,
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
            final DERUTF8String valueInPlatformCert) {
        if (valueInPlatformCert == null || StringUtils.isEmpty(valueInPlatformCert.getString())) {
            return true;
        }
        return valueInPlatformCert.getString().equals(evidenceFromDevice);
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
            LOGGER.info("The platform credential's board or chassis serial number matched"
                    + " with a serial number from the client's device information");
            return new AppraisalStatus(PASS, PLATFORM_ATTRIBUTES_VALID);
        }
        LOGGER.error("The platform credential's board and chassis serial numbers did"
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
     * @return true if the platform serial number was found (case insensitive search),
     *          false otherwise
     */
    private static boolean deviceInfoContainsPlatformSerialNumber(
            final String platformSerialNumber, final String platformSerialNumberDescription,
            final Map<String, String> deviceInfoSerialNumbers) {
        // check to see if the platform serial number is contained in the map of device info's
        // serial numbers
        for (Map.Entry<String, String> entry : deviceInfoSerialNumbers.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(platformSerialNumber)) {
                LOGGER.info("Device info contained platform {} {}"
                        + " in the device info's {}", platformSerialNumberDescription,
                        platformSerialNumber, entry.getKey());
                return true;
            }
        }

        LOGGER.warn("Platform {}, {}, did not match any device info serial numbers",
                platformSerialNumberDescription, platformSerialNumber);
        return false;
    }

    /**
     * Checks if the endorsement credential is valid.
     *
     * @param ec the endorsement credential to verify.
     * @param trustStore trust store holding trusted trusted certificates.
     * @param acceptExpired whether or not to accept expired and not yet valid certificates
     *                      as valid.
     * @return the result of the validation.
     */
    @Override
    public AppraisalStatus validateEndorsementCredential(final EndorsementCredential ec,
                                                       final KeyStore trustStore,
                                                       final boolean acceptExpired) {
        final String baseErrorMessage = "Can't validate endorsement credential attributes without ";
        String message;
        if (ec == null) {
            message = baseErrorMessage + "an endorsement credential";
            LOGGER.error(message);
            return new AppraisalStatus(FAIL, message);
        }
        if (trustStore == null) {
            message = baseErrorMessage + "a trust store";
            LOGGER.error(message);
            return new AppraisalStatus(FAIL, message);
        }

        try {
            X509Certificate verifiableCert = ec.getX509Certificate();

            // check validity period, currently acceptExpired will also accept not yet
            // valid certificates
            if (!acceptExpired) {
                verifiableCert.checkValidity();
            }

            if (verifyCertificate(verifiableCert, trustStore)) {
                return new AppraisalStatus(PASS, ENDORSEMENT_VALID);
            } else {
                return new AppraisalStatus(FAIL, "Endorsement credential does not have a valid "
                        + "signature chain in the trust store");
            }
        } catch (IOException e) {
            message = "Couldn't retrieve X509 certificate from endorsement credential";
            LOGGER.error(message, e);
            return new AppraisalStatus(ERROR, message + " " + e.getMessage());
        } catch (SupplyChainValidatorException e) {
            message = "An error occurred indicating the credential is not valid";
            LOGGER.warn(message, e);
            return new AppraisalStatus(ERROR, message + " " + e.getMessage());
        } catch (CertificateExpiredException e) {
            message = "The endorsement credential is expired";
            LOGGER.warn(message, e);
            return new AppraisalStatus(FAIL, message + " " + e.getMessage());
        } catch (CertificateNotYetValidException e) {
            message = "The endorsement credential is not yet valid";
            LOGGER.warn(message, e);
            return new AppraisalStatus(FAIL, message + " " + e.getMessage());
        }
    }

    /**
     * Attempts to check if the certificate is validated by certificates in a cert chain. The cert
     * chain is expected to be stored in a non-ordered KeyStore (trust store). If the signing
     * certificate for the target cert is found, but it is an intermediate cert, the validation will
     * continue to try to find the signing cert of the intermediate cert. It will continue searching
     * until it follows the chain up to a root (self-signed) cert.
     *
     * @param cert
     *            certificate to validate
     * @param trustStore
     *            trust store holding trusted root certificates and intermediate certificates
     * @return the certificate chain if validation is successful
     * @throws SupplyChainValidatorException
     *             if the verification is not successful
     */
    public static boolean verifyCertificate(final X509AttributeCertificateHolder cert,
            final KeyStore trustStore) throws SupplyChainValidatorException {
        if (cert == null || trustStore == null) {
            throw new SupplyChainValidatorException("Certificate or trust store is null");
        }

        try {
            Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();

            Enumeration<String> alias = trustStore.aliases();

            while (alias.hasMoreElements()) {
                trustedCerts.add((X509Certificate) trustStore.getCertificate(alias.nextElement()));
            }

            boolean certChainValidated = validateCertChain(cert, trustedCerts);
            if (!certChainValidated) {
                LOGGER.error("Cert chain could not be validated");
            }
            return certChainValidated;
        } catch (KeyStoreException e) {
            throw new SupplyChainValidatorException("Error with the trust store", e);
        }

    }

    /**
     * Attempts to check if the certificate is validated by certificates in a cert chain. The cert
     * chain is expected to be stored in a non-ordered KeyStore (trust store). If the signing
     * certificate for the target cert is found, but it is an intermediate cert, the validation will
     * continue to try to find the signing cert of the intermediate cert. It will continue searching
     * until it follows the chain up to a root (self-signed) cert.
     *
     * @param cert
     *            certificate to validate
     * @param trustStore
     *            trust store holding trusted root certificates and intermediate certificates
     * @return the certificate chain if validation is successful
     * @throws SupplyChainValidatorException
     *             if the verification is not successful
     */
    public static boolean verifyCertificate(final X509Certificate cert,
            final KeyStore trustStore) throws SupplyChainValidatorException {
        if (cert == null || trustStore == null) {
            throw new SupplyChainValidatorException("Certificate or trust store is null");
        }
        try {
            Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();

            Enumeration<String> alias = trustStore.aliases();

            while (alias.hasMoreElements()) {
                trustedCerts.add((X509Certificate) trustStore.getCertificate(alias.nextElement()));
            }

            return validateCertChain(cert, trustedCerts);
        } catch (KeyStoreException e) {
            LOGGER.error("Error accessing keystore", e);
            throw new SupplyChainValidatorException("Error with the trust store", e);
        }

    }

    /**
     * Attempts to check if an attribute certificate is validated by certificates in a cert chain.
     * The cert chain is represented as a Set of X509Certificates. If the signing certificate for
     * the target cert is found, but it is an intermediate cert, the validation will continue to try
     * to find the signing cert of the intermediate cert. It will continue searching until it
     * follows the chain up to a root (self-signed) cert.
     *
     * @param cert
     *            certificate to validate
     * @param additionalCerts
     *            Set of certs to validate against
     * @return boolean indicating if the validation was successful
     * @throws SupplyChainValidatorException tried to validate using null certificates
     */
    public static boolean validateCertChain(final X509AttributeCertificateHolder cert,
            final Set<X509Certificate> additionalCerts) throws SupplyChainValidatorException {
        if (cert == null || additionalCerts == null) {
            throw new SupplyChainValidatorException(
                    "Certificate or validation certificates are null");
        }
        boolean foundRootOfCertChain = false;
        Iterator<X509Certificate> certIterator = additionalCerts.iterator();
        X509Certificate trustedCert;

        while (!foundRootOfCertChain && certIterator.hasNext()) {
            trustedCert = certIterator.next();
            if (issuerMatchesSubjectDN(cert, trustedCert)
                    && signatureMatchesPublicKey(cert, trustedCert)) {
                if (isSelfSigned(trustedCert)) {
                    LOGGER.info("CA Root found.");
                    foundRootOfCertChain = true;
                } else {
                    foundRootOfCertChain = validateCertChain(trustedCert, additionalCerts);

                    if (!foundRootOfCertChain) {
                        LOGGER.error("Root of certificate chain not found. Check for CA Cert: "
                                + cert.getIssuer().getNames()[0]);
                    }
                }
            }
        }

        return foundRootOfCertChain;
    }

    /**
     * Attempts to check if a public-key certificate is validated by certificates in a cert chain.
     * The cert chain is represented as a Set of X509Certificates. If the signing certificate for
     * the target cert is found, but it is an intermediate cert, the validation will continue to try
     * to find the signing cert of the intermediate cert. It will continue searching until it
     * follows the chain up to a root (self-signed) cert.
     *
     * @param cert
     *            certificate to validate
     * @param additionalCerts
     *            Set of certs to validate against
     * @return boolean indicating if the validation was successful
     * @throws SupplyChainValidatorException tried to validate using null certificates
     */
    public static boolean validateCertChain(final X509Certificate cert,
            final Set<X509Certificate> additionalCerts) throws SupplyChainValidatorException {
        if (cert == null || additionalCerts == null) {
            throw new SupplyChainValidatorException(
                    "Certificate or validation certificates are null");
        }
        boolean foundRootOfCertChain = false;
        Iterator<X509Certificate> certIterator = additionalCerts.iterator();
        X509Certificate trustedCert;

        while (!foundRootOfCertChain && certIterator.hasNext()) {
            trustedCert = certIterator.next();
            if (issuerMatchesSubjectDN(cert, trustedCert)
                    && signatureMatchesPublicKey(cert, trustedCert)) {
                if (isSelfSigned(trustedCert)) {
                    LOGGER.info("CA Root found.");
                    foundRootOfCertChain = true;
                } else if (!cert.equals(trustedCert)) {
                    foundRootOfCertChain = validateCertChain(trustedCert, additionalCerts);

                    if (!foundRootOfCertChain) {
                        LOGGER.error("Root of certificate chain not found. Check for CA Cert: "
                                + cert.getIssuerDN().getName());
                    }
                }
            }
        }

        return foundRootOfCertChain;
    }

    /**
     * Checks if the issuer info of an attribute cert matches the supposed signing cert's
     * distinguished name.
     *
     * @param cert
     *            the attribute certificate with the signature to validate
     * @param signingCert
     *            the certificate with the public key to validate
     * @return boolean indicating if the names
     * @throws SupplyChainValidatorException tried to validate using null certificates
     */
    public static boolean issuerMatchesSubjectDN(final X509AttributeCertificateHolder cert,
            final X509Certificate signingCert) throws SupplyChainValidatorException {
        if (cert == null || signingCert == null) {
            throw new SupplyChainValidatorException("Certificate or signing certificate is null");
        }
        String signingCertSubjectDN = signingCert.getSubjectX500Principal().getName();
        X500Name namedSubjectDN = new X500Name(signingCertSubjectDN);

        X500Name issuerDN = cert.getIssuer().getNames()[0];

        // equality check ignore DN component ordering
        return issuerDN.equals(namedSubjectDN);
    }

    /**
     * Checks if the issuer info of a public-key cert matches the supposed signing cert's
     * distinguished name.
     *
     * @param cert
     *            the public-key certificate with the signature to validate
     * @param signingCert
     *            the certificate with the public key to validate
     * @return boolean indicating if the names
     * @throws SupplyChainValidatorException tried to validate using null certificates
     */
    public static boolean issuerMatchesSubjectDN(final X509Certificate cert,
            final X509Certificate signingCert) throws SupplyChainValidatorException {
        if (cert == null || signingCert == null) {
            throw new SupplyChainValidatorException("Certificate or signing certificate is null");
        }
        String signingCertSubjectDN = signingCert.getSubjectX500Principal().
                                                       getName(X500Principal.RFC1779);
        X500Name namedSubjectDN = new X500Name(signingCertSubjectDN);

        String certIssuerDN = cert.getIssuerDN().getName();
        X500Name namedIssuerDN = new X500Name(certIssuerDN);

        // equality check ignore DN component ordering
        return namedIssuerDN.equals(namedSubjectDN);
    }

    /**
     * Checks if the signature of an attribute cert is validated against the signing cert's public
     * key.
     *
     * @param cert
     *            the public-key certificate with the signature to validate
     * @param signingCert
     *            the certificate with the public key to validate
     * @return boolean indicating if the validation passed
     * @throws SupplyChainValidatorException tried to validate using null certificates
     */
    public static boolean signatureMatchesPublicKey(final X509Certificate cert,
            final X509Certificate signingCert) throws SupplyChainValidatorException {
        if (cert == null || signingCert == null) {
            throw new SupplyChainValidatorException("Certificate or signing certificate is null");
        }
        try {
            cert.verify(signingCert.getPublicKey(), BouncyCastleProvider.PROVIDER_NAME);
            return true;
        } catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException
                | NoSuchProviderException | SignatureException e) {
            LOGGER.error("Exception thrown while verifying certificate", e);
            return false;
        }

    }

    /**
     * Checks if the signature of a public-key cert is validated against the signing cert's public
     * key.
     *
     * @param cert
     *            the attribute certificate with the signature to validate
     * @param signingCert
     *            the certificate with the public key to validate
     * @return boolean indicating if the validation passed
     * @throws SupplyChainValidatorException tried to validate using null certificates
     */
    public static boolean signatureMatchesPublicKey(final X509AttributeCertificateHolder cert,
            final X509Certificate signingCert) throws SupplyChainValidatorException {
        if (cert == null || signingCert == null) {
            throw new SupplyChainValidatorException("Certificate or signing certificate is null");
        }
        return signatureMatchesPublicKey(cert, signingCert.getPublicKey());
    }

    /**
     * Checks if an X509 Attribute Certificate is valid directly against a public key.
     *
     * @param cert
     *            the attribute certificate with the signature to validate
     * @param signingKey
     *            the key to use to check the attribute cert
     * @return boolean indicating if the validation passed
     * @throws SupplyChainValidatorException tried to validate using null certificates
     */
    public static boolean signatureMatchesPublicKey(final X509AttributeCertificateHolder cert,
        final PublicKey signingKey) throws SupplyChainValidatorException {
        if (cert == null || signingKey == null) {
            throw new SupplyChainValidatorException("Certificate or signing certificate is null");
        }
        ContentVerifierProvider contentVerifierProvider;
        try {
            contentVerifierProvider =
                    new JcaContentVerifierProviderBuilder().setProvider("BC").build(signingKey);
            return cert.isSignatureValid(contentVerifierProvider);
        } catch (OperatorCreationException | CertException e) {
            LOGGER.error("Exception thrown while verifying certificate", e);
            return false;
        }
    }

    /**
     * Checks whether given X.509 public-key certificate is self-signed. If the cert can be
     * verified using its own public key, that means it was self-signed.
     *
     * @param cert
     *            X.509 Certificate
     * @return boolean indicating if the cert was self-signed
     */
    private static boolean isSelfSigned(final X509Certificate cert)
            throws SupplyChainValidatorException {
        if (cert == null) {
            throw new SupplyChainValidatorException("Certificate is null");
        }
        try {
            PublicKey key = cert.getPublicKey();
            cert.verify(key);
            return true;
        } catch (SignatureException | InvalidKeyException e) {
            return false;
        } catch (CertificateException | NoSuchAlgorithmException | NoSuchProviderException e) {
            LOGGER.error("Exception occurred while checking if cert is self-signed", e);
            return false;
        }
    }
}
