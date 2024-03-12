package hirs.attestationca.persist.validation;

import hirs.attestationca.persist.entity.manager.ComponentAttributeRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.cert.X509AttributeCertificateHolder;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static hirs.attestationca.persist.enums.AppraisalStatus.Status.ERROR;
import static hirs.attestationca.persist.enums.AppraisalStatus.Status.FAIL;
import static hirs.attestationca.persist.enums.AppraisalStatus.Status.PASS;

@Log4j2
public class CredentialValidator extends SupplyChainCredentialValidator {

    /**
     * Checks if the endorsement credential is valid.
     *
     * @param ec the endorsement credential to verify.
     * @param trustStore trust store holding trusted certificates.
     * @param acceptExpired whether or not to accept expired and not yet valid certificates
     *                      as valid.
     * @return the result of the validation.
     */
    public static AppraisalStatus validateEndorsementCredential(final EndorsementCredential ec,
                                                                final KeyStore trustStore,
                                                                final boolean acceptExpired) {
        final String baseErrorMessage = "Can't validate endorsement credential attributes without ";
        String message;
        if (ec == null) {
            message = baseErrorMessage + "an endorsement credential";
            return new AppraisalStatus(FAIL, message);
        }
        if (trustStore == null) {
            message = baseErrorMessage + "a trust store";
            return new AppraisalStatus(FAIL, message);
        }

        boolean keyInStore = false;
        try {
            keyInStore = trustStore.size() < 1;
        } catch (KeyStoreException ksEx) {
            log.error(ksEx.getMessage());
        }

        if (keyInStore) {
            message = baseErrorMessage + "keys in the trust store";
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
            return new AppraisalStatus(ERROR, message + " " + e.getMessage());
        } catch (SupplyChainValidatorException e) {
            message = "An error occurred indicating the credential is not valid";
            return new AppraisalStatus(ERROR, message + " " + e.getMessage());
        } catch (CertificateExpiredException e) {
            message = "The endorsement credential is expired";
            return new AppraisalStatus(FAIL, message + " " + e.getMessage());
        } catch (CertificateNotYetValidException e) {
            message = "The endorsement credential is not yet valid";
            return new AppraisalStatus(FAIL, message + " " + e.getMessage());
        }
    }

    /**
     * Checks if the platform credential is valid.
     *
     * @param pc The platform credential to verify.
     * @param trustStore trust store holding trusted certificates.
     * @param acceptExpired whether or not to accept expired certificates as valid.
     * @return The result of the validation.
     */
    public static AppraisalStatus validatePlatformCredential(final PlatformCredential pc,
                                                             final KeyStore trustStore,
                                                             final boolean acceptExpired) {
        final String baseErrorMessage = "Can't validate platform credential without ";
        String message;
        String certVerifyMsg;
        if (pc == null) {
            message = baseErrorMessage + "a platform credential";
            return new AppraisalStatus(FAIL, message);
        }
        try {
            if (trustStore == null || trustStore.size() == 0) {
                message = baseErrorMessage + "an Issuer Cert in the Trust Store";
                return new AppraisalStatus(FAIL, message);
            }
        } catch (KeyStoreException e) {
            message = baseErrorMessage + "an initialized trust store";
            return new AppraisalStatus(FAIL, message);
        }

        X509AttributeCertificateHolder attributeCert = null;
        try {
            attributeCert = pc.getX509AttributeCertificateHolder();
        } catch (IOException e) {
            message = "Could not retrieve X509 Attribute certificate";
            log.error(message, e);
            return new AppraisalStatus(FAIL, message + " " + e.getMessage());
        }

        // check validity period, currently acceptExpired will also accept not yet
        // valid certificates
        if (!acceptExpired && !pc.isValidOn(new Date())) {
            message = "Platform credential has expired";
            // if not valid at the current time
            log.debug(message);
            return new AppraisalStatus(FAIL, message);
        }

        // verify cert against truststore
        try {
            certVerifyMsg = verifyCertificate(attributeCert, trustStore);
            if (certVerifyMsg.isEmpty()) {
                message = PLATFORM_VALID;
                log.debug(message);
                return new AppraisalStatus(PASS, message);
            } else {
                message = String.format("Platform credential failed verification%n%s",
                        certVerifyMsg);
                log.debug(message);
                return new AppraisalStatus(FAIL, message);
            }
        } catch (SupplyChainValidatorException scvEx) {
            message = "An error occurred indicating the credential is not valid";
            log.warn(message, scvEx);
            return new AppraisalStatus(FAIL, message + " " + scvEx.getMessage());
        }
    }

    /**
     * Checks if the platform credential's attributes are valid.
     * @param platformCredential The platform credential to verify.
     * @param deviceInfoReport The device info report containing
     *                         serial number of the platform to be validated.
     * @param endorsementCredential The endorsement credential supplied from the same
     *          identity request as the platform credential.
     * @param componentResultRepository db access to component result of mismatching
     * @param componentAttributeRepository  db access to component attribute match status
     * @param componentInfos list of device components
     * @param provisionSessionId UUID associated with this run of the provision
     * @param ignoreRevisionAttribute policy flag to ignore the revision attribute
     * @return The result of the validation.
     */
    public static AppraisalStatus validatePlatformCredentialAttributes(
            final PlatformCredential platformCredential,
            final DeviceInfoReport deviceInfoReport,
            final EndorsementCredential endorsementCredential,
            final ComponentResultRepository componentResultRepository,
            final ComponentAttributeRepository componentAttributeRepository,
            final List<ComponentInfo> componentInfos,
            final UUID provisionSessionId, final boolean ignoreRevisionAttribute) {
        final String baseErrorMessage = "Can't validate platform credential attributes without ";
        String message;
        if (platformCredential == null) {
            message = baseErrorMessage + "a platform credential";
            return new AppraisalStatus(FAIL, message);
        }
        if (deviceInfoReport == null) {
            message = baseErrorMessage + "a device info report";
            return new AppraisalStatus(FAIL, message);
        }
        if (endorsementCredential == null) {
            message = baseErrorMessage + "an endorsement credential";
            return new AppraisalStatus(FAIL, message);
        }
        if (componentInfos.isEmpty()) {
            message = baseErrorMessage + "a list of device components";
            return new AppraisalStatus(FAIL, message);
        }

        // Quick, early check if the platform credential references the endorsement credential
        if (!endorsementCredential.getSerialNumber()
                .equals(platformCredential.getHolderSerialNumber())) {
            message = "Platform Credential holder serial number does not match "
                    + "the Endorsement Credential's serial number";
            return new AppraisalStatus(FAIL, message);
        }

        String credentialType = platformCredential.getCredentialType();
        if (PlatformCredential.CERTIFICATE_TYPE_2_0.equals(credentialType)) {
            return CertificateAttributeScvValidator.validatePlatformCredentialAttributesV2p0(
                    platformCredential, deviceInfoReport, componentResultRepository,
                    componentAttributeRepository, componentInfos, provisionSessionId,
                    ignoreRevisionAttribute);
        }
        return CertificateAttributeScvValidator.validatePlatformCredentialAttributesV1p2(
                platformCredential, deviceInfoReport);
    }

    /**
     * Checks if the delta credential's attributes are valid.
     * @param deviceInfoReport The device info report containing
     *                         serial number of the platform to be validated.
     * @param basePlatformCredential the base credential from the same identity request
     *      *                              as the delta credential.
     * @param deltaMapping delta certificates associated with the
     *      *                          delta supply validation.
     * @param componentInfos list of device components
     * @param componentResultRepository repository for component results
     * @param componentAttributeRepository repository for the attribute status
     * @param provisionSessionId the session id to share
     * @return the result of the validation.
     */
    public static AppraisalStatus validateDeltaPlatformCredentialAttributes(
            final DeviceInfoReport deviceInfoReport,
            final PlatformCredential basePlatformCredential,
            final Map<PlatformCredential, SupplyChainValidation> deltaMapping,
            final List<ComponentInfo> componentInfos,
            final ComponentResultRepository componentResultRepository,
            final ComponentAttributeRepository componentAttributeRepository,
            final UUID provisionSessionId) {
        final String baseErrorMessage = "Can't validate platform credential attributes without ";
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

        if (componentInfos.isEmpty()) {
            message = baseErrorMessage + "a list of device components";
            return new AppraisalStatus(FAIL, message);
        }

        // parse out the provided delta and its specific chain.
        List<ComponentResult> origPcComponents = componentResultRepository
                .findByCertificateSerialNumberAndBoardSerialNumber(
                        basePlatformCredential.getSerialNumber().toString(),
                        basePlatformCredential.getPlatformSerial());

        return CertificateAttributeScvValidator.validateDeltaAttributesChainV2p0(
                deviceInfoReport, deltaMapping, origPcComponents, componentInfos,
                componentResultRepository,
                componentAttributeRepository, provisionSessionId);
    }
}
