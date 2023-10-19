package hirs.attestationca.persist.validation;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.CertException;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Log4j2
@NoArgsConstructor
public class SupplyChainCredentialValidator  {

    public static final int NUC_VARIABLE_BIT = 159;
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

    /**
     * AppraisalStatus message for a valid firmware appraisal.
     */
    public static final String FIRMWARE_VALID = "Firmware validated";

    /**
     * Ensure that BouncyCastle is configured as a javax.security.Security provider, as this
     * class expects it to be available.
     */
    static {
        Security.addProvider(new BouncyCastleProvider());
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
    public static String verifyCertificate(final X509AttributeCertificateHolder cert,
                                           final KeyStore trustStore) throws SupplyChainValidatorException {
        try {
            if (cert == null || trustStore == null) {
                throw new SupplyChainValidatorException("Certificate or trust store is null");
            } else if (trustStore.size() == 0) {
                throw new SupplyChainValidatorException("Truststore is empty");
            }
        } catch (KeyStoreException e) {
            log.error("Error accessing trust store: " + e.getMessage());
        }

        try {
            Set<X509Certificate> trustedCerts = new HashSet<>();

            Enumeration<String> alias = trustStore.aliases();

            while (alias.hasMoreElements()) {
                trustedCerts.add((X509Certificate) trustStore.getCertificate(alias.nextElement()));
            }

            String certChainValidated = validateCertChain(cert, trustedCerts);
            if (!certChainValidated.isEmpty()) {
                log.error("Cert chain could not be validated");
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
        try {
            if (cert == null || trustStore == null) {
                throw new SupplyChainValidatorException("Certificate or trust store is null");
            } else if (trustStore.size() == 0) {
                throw new SupplyChainValidatorException("Truststore is empty");
            }
        } catch (KeyStoreException e) {
            log.error("Error accessing trust store: " + e.getMessage());
        }

        try {
            Set<X509Certificate> trustedCerts = new HashSet<>();
            Enumeration<String> alias = trustStore.aliases();

            while (alias.hasMoreElements()) {
                trustedCerts.add((X509Certificate) trustStore.getCertificate(alias.nextElement()));
            }

            return validateCertChain(cert, trustedCerts).isEmpty();
        } catch (KeyStoreException e) {
            log.error("Error accessing keystore", e);
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
     * @return String status of the cert chain validation -
     *  blank if successful, error message otherwise
     * @throws SupplyChainValidatorException tried to validate using null certificates
     */
    public static String validateCertChain(final X509AttributeCertificateHolder cert,
                                           final Set<X509Certificate> additionalCerts)
            throws SupplyChainValidatorException {
        if (cert == null || additionalCerts == null) {
            throw new SupplyChainValidatorException(
                    "Certificate or validation certificates are null");
        }
        final String intCAError = "Intermediate signing cert found, check for CA cert";
        String foundRootOfCertChain = "";
        X509Certificate nextInChain = null;

        do {
            for (X509Certificate trustedCert : additionalCerts) {
                boolean issuerMatchesSubject = false;
                boolean signatureMatchesPublicKey = false;
                if (nextInChain != null) {
                    issuerMatchesSubject = issuerMatchesSubjectDN(nextInChain, trustedCert);
                    signatureMatchesPublicKey = signatureMatchesPublicKey(nextInChain,
                            trustedCert);
                } else {
                    issuerMatchesSubject = issuerMatchesSubjectDN(cert, trustedCert);
                    signatureMatchesPublicKey = signatureMatchesPublicKey(cert, trustedCert);
                }

                if (issuerMatchesSubject && signatureMatchesPublicKey) {
                    if (isSelfSigned(trustedCert)) {
                        log.info("CA Root found.");
                        return "";
                    } else {
                        foundRootOfCertChain = intCAError;
                        nextInChain = trustedCert;
                        break;
                    }
                } else {
                    if (!issuerMatchesSubject) {
                        foundRootOfCertChain = "Issuer DN does not match Subject DN";
                    }
                    if (!signatureMatchesPublicKey) {
                        foundRootOfCertChain = "Certificate signature failed to verify";
                    }
                }
            }
        } while (foundRootOfCertChain.equals(intCAError));

        log.error(foundRootOfCertChain);
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
     * @return String status of the cert chain validation -
     *  blank if successful, error message otherwise
     * @throws SupplyChainValidatorException tried to validate using null certificates
     */
    public static String validateCertChain(final X509Certificate cert,
                                           final Set<X509Certificate> additionalCerts) throws SupplyChainValidatorException {
        if (cert == null || additionalCerts == null) {
            throw new SupplyChainValidatorException(
                    "Certificate or validation certificates are null");
        }
        final String intCAError = "Intermediate signing cert found, check for CA cert";
        String foundRootOfCertChain = "";
        X509Certificate startOfChain = cert;

        do {
            for (X509Certificate trustedCert : additionalCerts) {
                boolean issuerMatchesSubject = issuerMatchesSubjectDN(startOfChain, trustedCert);
                boolean signatureMatchesPublicKey = signatureMatchesPublicKey(startOfChain,
                        trustedCert);
                if (issuerMatchesSubject && signatureMatchesPublicKey) {
                    if (isSelfSigned(trustedCert)) {
                        log.info("CA Root found.");
                        return "";
                    } else {
                        foundRootOfCertChain = intCAError;
                        startOfChain = trustedCert;
                        break;
                    }
                } else {
                    if (!issuerMatchesSubject) {
                        foundRootOfCertChain = "Issuer DN does not match Subject DN";
                    }
                    if (!signatureMatchesPublicKey) {
                        foundRootOfCertChain = "Certificate signature failed to verify";
                    }
                }
            }
        } while (foundRootOfCertChain.equals(intCAError));

        log.warn(foundRootOfCertChain);
        return foundRootOfCertChain;
    }

    /**
     * Parses the output from PACCOR's allcomponents.sh script into ComponentInfo objects.
     * @param paccorOutput the output from PACCOR's allcomoponents.sh
     * @return a list of ComponentInfo objects built from paccorOutput
     * @throws java.io.IOException if something goes wrong parsing the JSON
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

    /**
     * Parses the output from PACCOR's allcomponents.sh script into ComponentInfo objects.
     * @param paccorOutput the output from PACCOR's allcomoponents.sh
     * @return a list of ComponentInfo objects built from paccorOutput
     * @throws IOException if something goes wrong parsing the JSON
     */
    public static List<ComponentInfo> getV2PaccorOutput(
            final String paccorOutput) throws IOException {
        List<ComponentInfo> ciList = new LinkedList<>();
        String manufacturer, model, serial, revision;
        String componentClass = Strings.EMPTY;

        if (StringUtils.isNotEmpty(paccorOutput)) {
            ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
            JsonNode rootNode = objectMapper.readTree(paccorOutput);
            Iterator<JsonNode> jsonComponentNodes
                    = rootNode.findValue("COMPONENTS").elements();
            while (jsonComponentNodes.hasNext()) {
                JsonNode next = jsonComponentNodes.next();
                manufacturer = getJSONNodeValueAsText(next, "MANUFACTURER");
                model = getJSONNodeValueAsText(next, "MODEL");
                serial = getJSONNodeValueAsText(next, "SERIAL");
                revision = getJSONNodeValueAsText(next, "REVISION");
                List<JsonNode> compClassNodes = next.findValues("COMPONENTCLASS");

                for (JsonNode subNode : compClassNodes) {
                    componentClass = getJSONNodeValueAsText(subNode,
                            "COMPONENTCLASSVALUE");
                }
                ciList.add(new ComponentInfo(manufacturer, model,
                        serial, revision, componentClass));
            }
        }

        return ciList;
    }

    private static String getJSONNodeValueAsText(final JsonNode node, final String fieldName) {
        if (node.hasNonNull(fieldName)) {
            return node.findValue(fieldName).asText();
        }
        return null;
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

        String certIssuerDN = cert.getIssuerX500Principal().getName();
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
        } catch (InvalidKeyException e) {
            log.info("Incorrect key given to validate this cert's signature");
        } catch (CertificateException e) {
            log.info("Encoding error while validating this cert's signature");
        } catch (NoSuchAlgorithmException e) {
            log.info("Unsupported signature algorithm found during validation");
        } catch (NoSuchProviderException e) {
            log.info("Incorrect provider for cert signature validation");
        } catch (SignatureException e) {
            log.info(String.format("%s.verify(%s)", cert.getSubjectX500Principal(),
                    signingCert.getSubjectX500Principal()));
        }
        return false;

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
        if (signingCert == null) {
            throw new SupplyChainValidatorException("Signing certificate is null");
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
            log.info("Exception thrown while verifying certificate", e);
            log.info(String.format("%s.isSignatureValid(%s)", cert.getSerialNumber(),
                    signingKey.getFormat()));
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
            log.error("Exception occurred while checking if cert is self-signed", e);
            return false;
        }
    }
}
