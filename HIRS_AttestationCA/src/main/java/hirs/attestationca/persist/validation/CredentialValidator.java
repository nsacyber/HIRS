package hirs.attestationca.persist.validation;

import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
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

import static hirs.attestationca.persist.enums.AppraisalStatus.Status.ERROR;
import static hirs.attestationca.persist.enums.AppraisalStatus.Status.FAIL;
import static hirs.attestationca.persist.enums.AppraisalStatus.Status.PASS;

@Log4j2
public class CredentialValidator extends SupplyChainCredentialValidator {

    /**
     * Checks if the endorsement credential is valid.
     *
     * @param ec the endorsement credential to verify.
     * @param trustStore trust store holding trusted trusted certificates.
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
     * @return The result of the validation.
     */
    public static AppraisalStatus validatePlatformCredentialAttributes(
            final PlatformCredential platformCredential,
            final DeviceInfoReport deviceInfoReport,
            final EndorsementCredential endorsementCredential) {
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
                    platformCredential, deviceInfoReport);
        }
        return CertificateAttributeScvValidator.validatePlatformCredentialAttributesV1p2(
                platformCredential, deviceInfoReport);
    }
}