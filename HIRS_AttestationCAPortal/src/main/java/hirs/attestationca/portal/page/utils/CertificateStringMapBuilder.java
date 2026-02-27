package hirs.attestationca.portal.page.utils;

import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.IDevIDCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.PlatformConfigurationV1;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.PlatformConfigurationV2;
import hirs.attestationca.persist.exceptions.NonUniqueSKIException;
import hirs.attestationca.persist.util.AcaPciIds;
import hirs.utils.BouncyCastleUtils;
import hirs.utils.PciIds;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.encoders.Hex;

import jakarta.persistence.NonUniqueResultException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static hirs.utils.specificationLookups.PlatformClass.getPlatClassFromId;

/**
 * Utility class for mapping certificate information in to string maps. These are used to display
 * information on a web page, as X509 cert classes do not serialize to JSON
 */
@Log4j2
public final class CertificateStringMapBuilder {

    // Extended Key Usage (TCG KP) OIDs
    private static final String TCG_KP_EK_CERTIFICATE = "2.23.133.8.1";
    private static final String TCG_KP_PLATFORM_ATTRIBUTE_CERTIFICATE = "2.23.133.8.2";
    private static final String TCG_KP_AIK_CERTIFICATE = "2.23.133.8.3";
    private static final String TCG_KP_PLATFORM_KEY_CERTIFICATE = "2.23.133.8.4";
    private static final String TCG_KP_DELTA_PLATFORM_ATTRIBUTE_CERTIFICATE = "2.23.133.8.5";

    /**
     * This private constructor was created to silence checkstyle error.
     */
    private CertificateStringMapBuilder() {
    }

    private static Map<String, String> getExtendedKeyUsageMap() {
        Map<String, String> ekuMap = new HashMap<>();
        ekuMap.put(TCG_KP_EK_CERTIFICATE, "tcg-kp-EKCertificate");
        ekuMap.put(TCG_KP_PLATFORM_ATTRIBUTE_CERTIFICATE, "tcg-kp-PlatformAttributeCertificate");
        ekuMap.put(TCG_KP_AIK_CERTIFICATE, "tcg-kp-AIKCertificate");
        ekuMap.put(TCG_KP_PLATFORM_KEY_CERTIFICATE, "tcg-kp-PlatformKeyCertificate");
        ekuMap.put(TCG_KP_DELTA_PLATFORM_ATTRIBUTE_CERTIFICATE, "tcg-kp-DeltaPlatformAttributeCertificate");
        return ekuMap;
    }

    /**
     * Returns the general information.
     *
     * @param certificate             certificate to get the general information.
     * @param caCertificateRepository CA Certificate repository
     * @param certificateRepository   the certificate repository for retrieving certs.
     * @return a hash map with the general certificate information.
     */
    public static HashMap<String, String> getGeneralCertificateInfo(
            final Certificate certificate,
            final CertificateRepository certificateRepository,
            final CACredentialRepository caCertificateRepository) {
        HashMap<String, String> data = new HashMap<>();
        Map<String, String> ekuMap = getExtendedKeyUsageMap();

        if (certificate != null) {
            data.put("issuer", certificate.getIssuer());
            //Serial number in hex value
            data.put("serialNumber", Hex.toHexString(certificate.getSerialNumber().toByteArray()));
            if (!certificate.getAuthoritySerialNumber().equals(BigInteger.ZERO)) {
                data.put("authSerialNumber", Hex.toHexString(certificate
                        .getAuthoritySerialNumber().toByteArray()));
            }
            if (certificate.getId() != null) {
                data.put("certificateId", certificate.getId().toString());
            }
            data.put("authInfoAccess", certificate.getAuthorityInfoAccess());
            data.put("beginValidity", Long.toString(certificate.getBeginValidity().getTime()));
            data.put("endValidity", Long.toString(certificate.getEndValidity().getTime()));
            data.put("signature", Arrays.toString(certificate.getSignature()));

            if (certificate.getSubject() != null) {
                data.put("subject", certificate.getSubject());
                if (certificate.getIssuer() != null) {
                    data.put("isSelfSigned",
                            String.valueOf(certificate.getIssuer().equals(certificate.getSubject())));
                } else {
                    data.put("isSelfSigned", "false");
                }
            } else {
                data.put("isSelfSigned", "false");
            }

            data.put("authKeyId", certificate.getAuthorityKeyIdentifier());
            data.put("crlPoints", certificate.getCrlPoints());
            data.put("signatureAlgorithm", certificate.getSignatureAlgorithm());
            if (certificate.getEncodedPublicKey() != null) {
                data.put("encodedPublicKey",
                        Arrays.toString(certificate.getEncodedPublicKey()));
                data.put("publicKeyAlgorithm", certificate.getPublicKeyAlgorithm());
                byte[] encodedPublicKey = certificate.getEncodedPublicKey();
                try {
                    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedPublicKey);
                    PublicKey publicKey = null;
                    // Attempt EC
                    try {
                        KeyFactory ecFactory = KeyFactory.getInstance("EC");
                        publicKey = ecFactory.generatePublic(keySpec);
                    } catch (Exception ignore) { }
                    // If no EC then RSA
                    if (publicKey == null) {
                        KeyFactory rsaFactory = KeyFactory.getInstance("RSA");
                        publicKey = rsaFactory.generatePublic(keySpec);
                    }
                    // Get public key size
                    if (publicKey != null) {
                        String keySizeStr;
                        if (publicKey instanceof ECPublicKey ecKey) {
                            keySizeStr = Integer.toString(ecKey.getParams().getCurve().getField().getFieldSize());
                        } else {
                            keySizeStr = String.valueOf(certificate.getPublicKeySize());
                        }
                        data.put("publicKeySize", keySizeStr);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse public key from certificate", e);
                }
            }

            if (certificate.getPublicKeyModulusHexValue() != null) {
                data.put("publicKeyValue", certificate.getPublicKeyModulusHexValue());
            }

            if (certificate.getKeyUsage() != null) {
                data.put("keyUsage", certificate.getKeyUsage());
            }

            if (certificate.getExtendedKeyUsage() != null
                    && !certificate.getExtendedKeyUsage().isEmpty()) {
                String eku = certificate.getExtendedKeyUsage().replaceAll("\\n$", "");
                if (ekuMap.containsKey(eku)) {
                    data.put("extendedKeyUsage", eku + " (" + ekuMap.get(eku) + ")");
                } else {
                    data.put("extendedKeyUsage", eku + " (Warning: Unexpected OID)");
                }
            }

            //Get issuer ID if not self signed
            if (data.get("isSelfSigned").equals("false")) {
                //Get the missing certificate chain for not self sign
                Certificate missingCert = null;
                try {
                    missingCert = containsAllChain(certificate, caCertificateRepository);
                } catch (NonUniqueSKIException e) {
                    data.put("missingChainIssuer",
                            "Chain contains Root CA whose SKI is non-unique within CA repository.");
                }
                String issuerResult;

                if (missingCert != null) {
                    data.put("missingChainIssuer", String.format("Missing %s from the chain.",
                            missingCert.getIssuer()));
                }

                // Match AKI against SKI
                if (certificate.getAuthorityKeyIdentifier() != null
                        && !certificate.getAuthorityKeyIdentifier().isEmpty()) {
                    try {
                        CertificateAuthorityCredential keyIdMatch = caCertificateRepository
                                .findBySubjectKeyIdStringAndArchiveFlag(
                                        certificate.getAuthorityKeyIdentifier(), false);
                        if (keyIdMatch != null) {
                            data.put("issuerID", keyIdMatch.getId().toString());
                            return data;
                        }
                    } catch (IncorrectResultSizeDataAccessException | NonUniqueResultException e) {
                        log.error("Duplicate Root CA SKI detected while matching AKI to SKI: {}",
                                certificate.getAuthorityKeyIdentifier(), e);
                    }
                }

                // If no AKI SKI match found, fall back on DN match
                List<Certificate> certificates = certificateRepository.findBySubjectSorted(
                        certificate.getIssuerSorted(), "CertificateAuthorityCredential");
                //Find all certificates that could be the issuer certificate based on subject name
                for (Certificate issuerCert : certificates) {
                    try {
                        //Find the certificate that actually signed this cert
                        issuerResult = certificate.isIssuer(issuerCert);
                        if (issuerResult.isEmpty()) {
                            data.put("issuerID", issuerCert.getId().toString());
                            break;
                        } else {
                            data.put("issuerID", issuerCert.getId().toString());
                            issuerResult = String.format("%s: %s", issuerResult,
                                    issuerCert.getSubject());
                            data.put("missingChainIssuer", issuerResult);
                            break;
                        }
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
        }

        return data;
    }

    /**
     * Recursive function that check if all the certificate chain is present.
     *
     * @param certificate            certificate to get the issuer
     * @param caCredentialRepository CA Certificate repository
     * @return a boolean indicating if it has the full chain or not.
     */
    public static Certificate containsAllChain(
            final Certificate certificate,
            final CACredentialRepository caCredentialRepository) {
        List<CertificateAuthorityCredential> issuerCertificates = new ArrayList<>();
        CertificateAuthorityCredential skiCA = null;
        String issuerResult;

        //Check if there is a subject organization
        if (certificate.getAuthorityKeyIdentifier() != null
                && !certificate.getAuthorityKeyIdentifier().isEmpty()) {
            try {
                skiCA = caCredentialRepository.findBySubjectKeyIdStringAndArchiveFlag(
                        certificate.getAuthorityKeyIdentifier(), false);
            } catch (IncorrectResultSizeDataAccessException | NonUniqueResultException e) {
                log.error("Duplicate Root CA SKI detected while resolving chain: {}",
                        certificate.getAuthorityKeyIdentifier(), e);
                throw new NonUniqueSKIException(
                        "Duplicate Root CA SKI detected in CA Credential Repository: "
                                + certificate.getAuthorityKeyIdentifier(), e);
            }
        } else {
            log.error("Certificate ({}) for {} has no authority key identifier.",
                    certificate.getClass(), certificate.getSubject());
        }

        if (skiCA == null) {
            if (certificate.getIssuerSorted() == null
                    || certificate.getIssuerSorted().isEmpty()) {
                //Get certificates by subject
                issuerCertificates =
                        caCredentialRepository.findBySubjectAndArchiveFlag(certificate.getIssuer(), false);
            } else {
                //Get certificates by subject organization
                issuerCertificates = caCredentialRepository.findBySubjectSortedAndArchiveFlag(
                        certificate.getIssuerSorted(), false);
            }
        } else {
            issuerCertificates.add(skiCA);
        }

        for (Certificate issuerCert : issuerCertificates) {
            try {
                // Find the certificate that actually signed this cert
                issuerResult = certificate.isIssuer(issuerCert);
                if (issuerResult.isEmpty()) {
                    //Check if it's root certificate
                    if (BouncyCastleUtils.x500NameCompare(issuerCert.getIssuerSorted(),
                            issuerCert.getSubjectSorted())) {
                        return null;
                    }
                    return containsAllChain(issuerCert, caCredentialRepository);
                }
            } catch (IOException ioEx) {
                log.error(ioEx);
                return certificate;
            }
        }

        return certificate;
    }

    /**
     * Returns the Certificate Authority information.
     *
     * @param uuid                    ID for the certificate.
     * @param certificateRepository   the certificate manager for retrieving certs.
     * @param caCertificateRepository CA Certificate repository
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String>
    getCertificateAuthorityInformation(final UUID uuid,
                                       final CertificateRepository certificateRepository,
                                       final CACredentialRepository caCertificateRepository) {

        if (!caCertificateRepository.existsById(uuid)) {
            return new HashMap<>();
        }
        CertificateAuthorityCredential certificate = caCertificateRepository.getReferenceById(uuid);

        String notFoundMessage = "Unable to find Certificate Authority "
                + "Credential with ID: " + uuid;

        return getCertificateAuthorityInfoHelper(certificateRepository, caCertificateRepository,
                List.of(certificate),
                notFoundMessage);
    }

    /**
     * Returns the Trust Chain credential information.
     *
     * @param certificates            the certificates
     * @param certificateRepository   the certificate repository for retrieving certs.
     * @param caCertificateRepository the certificate repository for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getCertificateAuthorityInformation(
            final List<CertificateAuthorityCredential> certificates,
            final CertificateRepository certificateRepository,
            final CACredentialRepository caCertificateRepository) {
        return getCertificateAuthorityInfoHelper(certificateRepository, caCertificateRepository, certificates,
                "No cert provided for mapping");
    }

    private static HashMap<String, String> getCertificateAuthorityInfoHelper(
            final CertificateRepository certificateRepository,
            final CACredentialRepository caCertificateRepository,
            final List<CertificateAuthorityCredential> certificates, final String notFoundMessage) {
        HashMap<String, String> data = new HashMap<>();

        if (certificates != null) {
            for (CertificateAuthorityCredential certificate : certificates) {
                data.putAll(
                        getGeneralCertificateInfo(certificate, certificateRepository,
                                caCertificateRepository));
                data.put("subjectKeyIdentifier",
                        Arrays.toString(certificate.getSubjectKeyIdentifier()));
                //x509 credential version
                data.put("x509Version", Integer.toString(certificate
                        .getX509CredentialVersion()));
                data.put("credentialType", certificate.getCredentialType());
            }

        } else {
            log.error(notFoundMessage);
        }
        return data;
    }

    /**
     * Returns the endorsement credential information.
     *
     * @param uuid                    ID for the certificate.
     * @param certificateRepository   the certificate repository for retrieving certs.
     * @param caCertificateRepository CA Certificate repository
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getEndorsementInformation(
            final UUID uuid,
            final CertificateRepository certificateRepository,
            final CACredentialRepository caCertificateRepository) {
        HashMap<String, String> data = new HashMap<>();
        EndorsementCredential certificate =
                (EndorsementCredential) certificateRepository.getCertificate(uuid);

        if (certificate != null) {
            data.putAll(
                    getGeneralCertificateInfo(certificate, certificateRepository, caCertificateRepository));
            // Set extra fields
            data.put("manufacturer", certificate.getManufacturer());
            data.put("model", certificate.getModel());
            data.put("version", certificate.getVersion());
            data.put("policyReference", certificate.getPolicyReference());
            data.put("crlPoints", certificate.getCrlPoints());
            data.put("credentialType", certificate.getCredentialType());
            //x509 credential version
            data.put("x509Version", Integer.toString(certificate
                    .getX509CredentialVersion()));
            // Reparse certificate to fetch additional details for display
            try {
                certificate.parseCertificate();
            } catch (IOException e) {
                throw new RuntimeException("Failed to re-parse Endorsement Credential for details display", e);
            }
            // Add hashmap with TPM information if available
            if (certificate.getTpmSpecification() != null) {
                data.putAll(
                        convertStringToHash(certificate.getTpmSpecification().toString()));
            }
            if (certificate.getTpmSecurityAssertions() != null) {
                data.putAll(
                        convertStringToHash(certificate.getTpmSecurityAssertions().toString()));
                if (certificate.getCommonCriteriaMeasures() != null) {
                    data.putAll(convertStringToHash(certificate.getCommonCriteriaMeasures().toString()));
                }
                if (certificate.getFipsLevel() != null) {
                    data.putAll(convertStringToHash(certificate.getFipsLevel().toString()));
                }
                data.put("iso9000Certified", String.valueOf(certificate.isIso9000Certified()));
                if (certificate.getIso9000Uri() != null) {
                    data.put("iso9000Uri", certificate.getIso9000Uri());
                }
            }
        } else {
            String notFoundMessage = "Unable to find Endorsement Credential "
                    + "with ID: " + uuid;
            log.error(notFoundMessage);
        }
        return data;
    }

    /**
     * Returns the Platform credential information.
     *
     * @param uuid                      ID for the certificate.
     * @param certificateRepository     the certificate manager for retrieving certs.
     * @param componentResultRepository component result repository.
     * @param caCertificateRepository   CA credential repository.
     * @return a hash map with the endorsement certificate information.
     * @throws IOException              when parsing the certificate
     * @throws IllegalArgumentException invalid argument on parsing the certificate
     */
    public static HashMap<String, Object> getPlatformInformation(final UUID uuid,
                                                                 final CertificateRepository
                                                                         certificateRepository,
                                                                 final ComponentResultRepository
                                                                         componentResultRepository,
                                                                 final CACredentialRepository
                                                                         caCertificateRepository)
            throws IllegalArgumentException, IOException {
        HashMap<String, Object> data = new HashMap<>();
        PlatformCredential certificate = (PlatformCredential) certificateRepository.getCertificate(uuid);

        if (certificate != null) {
            data.putAll(
                    getGeneralCertificateInfo(certificate, certificateRepository, caCertificateRepository));
            data.put("credentialType", certificate.getCredentialType());
            data.put("platformType", certificate.getPlatformChainType());
            data.put("manufacturer", certificate.getManufacturer());
            data.put("model", certificate.getModel());
            data.put("version", certificate.getVersion());
            data.put("platformSerial", certificate.getPlatformSerial());
            data.put("chassisSerialNumber", certificate.getChassisSerialNumber());
            try {
                String platformClassStr = certificate.getPlatformClass().replaceAll("[^0-9]", "");
                data.put("platformClass", getPlatClassFromId(Integer.parseInt(platformClassStr)));
            } catch (Exception e) {
                if ((certificate.getPlatformClass() == null) || (certificate.getPlatformClass().isEmpty())) {
                    data.put("platformClass", "Not Specified");
                } else {
                    data.put("platformClass", certificate.getPlatformClass() + " (unable to perform lookup");
                }
            }
            data.put("majorVersion",
                    Integer.toString(certificate.getMajorVersion()));
            data.put("minorVersion",
                    Integer.toString(certificate.getMinorVersion()));
            data.put("revisionLevel",
                    Integer.toString(certificate.getRevisionLevel()));
            data.put("holderSerialNumber", certificate.getHolderSerialNumber()
                    .toString(Certificate.HEX_BASE)
                    .replaceAll("(?<=..)(..)", ":$1"));
            data.put("holderIssuer", certificate.getHolderIssuer());
            if (certificate.isPlatformBase()) {
                EndorsementCredential ekCertificate = (EndorsementCredential) certificateRepository
                        .findBySerialNumber(certificate.getHolderSerialNumber(),
                                "EndorsementCredential");

                if (ekCertificate != null) {
                    data.put("holderId", ekCertificate.getId().toString());
                }
            } else {
                if (certificate.getPlatformChainType() != null
                        && certificate.getPlatformChainType().equals("Delta")) {
                    PlatformCredential holderCertificate = (PlatformCredential) certificateRepository
                            .findBySerialNumber(certificate.getHolderSerialNumber(),
                                    "PlatformCredential");

                    if (holderCertificate != null) {
                        data.put("holderId", holderCertificate.getId().toString());
                    }
                }
            }

            PlatformCredential prevCertificate = certificateRepository
                    .getPcByHolderSerialNumber(certificate.getSerialNumber());

            if (prevCertificate != null) {
                data.put("prevCertId", prevCertificate.getId().toString());
            }

            //x509 credential version
            data.put("x509Version", certificate.getX509CredentialVersion());
            //CPSuri
            data.put("CPSuri", certificate.getCPSuri());
            //Component Identifier - attempt to translate hardware IDs
            List<ComponentResult> compResults = componentResultRepository
                    .findByCertificateSerialNumberAndBoardSerialNumber(
                            certificate.getSerialNumber().toString(),
                            certificate.getPlatformSerial());
            if (PciIds.DB.isReady()) {
                compResults = AcaPciIds.translateResults(compResults);
            }
            data.put("componentResults", compResults);

            //Get platform Configuration values and set map with it
            if (certificate.getPlatformConfigurationV1() != null) {
                PlatformConfigurationV1 platformConfigurationV1 = certificate.getPlatformConfigurationV1();

                List<ComponentIdentifier> componentIdentifiersV1 =
                        platformConfigurationV1.getComponentIdentifiers();

                if (PciIds.DB.isReady()) {
                    componentIdentifiersV1 = AcaPciIds.translate(componentIdentifiersV1);
                }
                //Component Identifiers
                data.put("componentsIdentifier", componentIdentifiersV1);

                //Platform Properties
                data.put("platformProperties", platformConfigurationV1.getPlatformProperties());
                //Platform Properties URI
                data.put("platformPropertiesURI", platformConfigurationV1.getPlatformPropertiesUri());

            } else if (certificate.getPlatformConfigurationV2() != null) {
                PlatformConfigurationV2 platformConfigurationV2 = certificate.getPlatformConfigurationV2();
                //Component Identifiers
                List<ComponentIdentifierV2> componentIdentifiersV2 =
                        platformConfigurationV2.getComponentIdentifiers();

                data.put("componentsIdentifier", componentIdentifiersV2);
                //Component Identifier URI
                data.put("componentsIdentifierURI", platformConfigurationV2
                        .getComponentIdentifiersUri());
                //Platform Properties
                data.put("platformProperties", platformConfigurationV2.getPlatformProperties());
                //Platform Properties URI
                data.put("platformPropertiesURI", platformConfigurationV2.getPlatformPropertiesUri());
            }

            //TBB Security Assertion
            data.put("tbbSecurityAssertion", certificate.getTBBSecurityAssertion());

            if (certificate.getPlatformSerial() != null) {
                // link certificate chain
                List<PlatformCredential> chainCertificates =
                        certificateRepository.byBoardSerialNumber(certificate.getPlatformSerial());
                data.put("numInChain", chainCertificates.size());
                Collections.sort(chainCertificates, new Comparator<PlatformCredential>() {
                    @Override
                    public int compare(final PlatformCredential obj1,
                                       final PlatformCredential obj2) {
                        return obj1.getBeginValidity().compareTo(obj2.getBeginValidity());
                    }
                });

                data.put("chainCertificates", chainCertificates);

                if (!certificate.isPlatformBase()) {
                    for (PlatformCredential pc : chainCertificates) {
                        if (pc.isPlatformBase()) {
                            if (!pc.getComponentFailures().isEmpty()) {
                                data.put("failures", pc.getComponentFailures());
                            }
                            break;
                        }
                    }
                }
            }
        } else {
            String notFoundMessage = "Unable to find Platform Certificate "
                    + "with ID: " + uuid;
            log.error(notFoundMessage);
        }
        return data;
    }

    /**
     * Returns a HasHMap of a string.
     * Ex: input "TPMSpecification{family='abc',level=0, revision=0}"
     * output   map[TPMSpecificationFamily] = 'abc'
     * map[TPMSpecificationLevel] = 0
     * map[TPMSpecificationRevision] = 0
     *
     * @param str HashMap string to be converted.
     * @return a hash map with key-value pairs from the string
     */
    private static HashMap<String, String> convertStringToHash(final String str) {
        HashMap<String, String> map = new HashMap<>();
        if (str == null || str.isEmpty()) {
            return map;
        }

        // Determine delimiter type
        int startIdx = str.indexOf('(');
        char openDelim = '(';
        char closeDelim = ')';

        if (startIdx < 0) {
            startIdx = str.indexOf('{');
            openDelim = '{';
            closeDelim = '}';
        }

        // If no delimiters, cannot parse
        if (startIdx < 0) {
            return map;
        }

        String name = str.substring(0, startIdx).trim();
        String data = str.substring(startIdx + 1, str.lastIndexOf(closeDelim)).trim();

        int braceDepth = 0;
        StringBuilder current = new StringBuilder();
        List<String> pairs = new ArrayList<>();

        // Split top-level key=value pairs, ignoring commas inside braces
        for (char c : data.toCharArray()) {
            if (c == '{' || c == '(') {
                braceDepth++;
            } else if (c == '}' || c == ')') {
                braceDepth--;
            }
            if (c == ',' && braceDepth == 0) {
                pairs.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            pairs.add(current.toString());
        }

        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length < 2) {
                continue;
            }
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            // Capitalize first letter of key
            key = Character.toUpperCase(key.charAt(0)) + key.substring(1);

            // Handle nested object recursively if it contains braces or parentheses
            if ((value.contains("{") && value.contains("}")) || (value.contains("(") && value.contains(")"))) {
                HashMap<String, String> nestedMap = convertStringToHash(value);
                // Prefix nested keys with parent key
                map.putAll(nestedMap);
            } else {
                map.put(name + key, value);
            }
        }
        return map;
    }

    /**
     * Returns the Issued Attestation Certificate information.
     *
     * @param uuid                   ID for the certificate.
     * @param certificateRepository  the certificate manager for retrieving certs.
     * @param caCredentialRepository CA Credential repository.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getIssuedInformation(final UUID uuid,
                                                               final CertificateRepository
                                                                       certificateRepository,
                                                               final CACredentialRepository
                                                                       caCredentialRepository) {
        HashMap<String, String> data = new HashMap<>();
        IssuedAttestationCertificate certificate =
                (IssuedAttestationCertificate) certificateRepository.getCertificate(uuid);

        if (certificate != null) {
            data.putAll(
                    getGeneralCertificateInfo(certificate, certificateRepository, caCredentialRepository));

            // add endorsement credential ID if not null
            if (certificate.getEndorsementCredential() != null) {
                EndorsementCredential ek = certificate.getEndorsementCredential();
                if (ek.getId() != null) {
                    data.put("endorsementID", ek.getId().toString());
                } else {
                    data.put("endorsementID", "0");
                }
                // Add hashmap with TPM information if available
                if (ek.getTpmSpecification() != null) {
                    data.putAll(
                            convertStringToHash(ek.getTpmSpecification().toString()));
                }
                if (ek.getTpmSecurityAssertions() != null) {
                    data.putAll(
                            convertStringToHash(ek.getTpmSecurityAssertions().toString()));
                }

                data.put("policyReference", ek.getPolicyReference());
                data.put("crlPoints", ek.getCrlPoints());
                data.put("credentialType", IssuedAttestationCertificate.AIC_TYPE_LABEL);
            }
            // add platform credential IDs if not empty
            if (!certificate.getPlatformCredentials().isEmpty()) {
                StringBuilder buf = new StringBuilder();
                for (PlatformCredential pc : certificate.getPlatformCredentials()) {
                    buf.append(pc.getId().toString());
                    buf.append(',');
                    data.put("manufacturer", pc.getManufacturer());
                    data.put("model", pc.getModel());
                    data.put("version", pc.getVersion());
                    data.put("majorVersion",
                            Integer.toString(pc.getMajorVersion()));
                    data.put("minorVersion",
                            Integer.toString(pc.getMinorVersion()));
                    data.put("revisionLevel",
                            Integer.toString(pc.getRevisionLevel()));
                    data.put("tcgMajorVersion",
                            Integer.toString(pc.getTcgCredentialMajorVersion()));
                    data.put("tcgMinorVersion",
                            Integer.toString(pc.getTcgCredentialMinorVersion()));
                    data.put("tcgRevisionLevel",
                            Integer.toString(pc.getTcgCredentialRevisionLevel()));
                }
                // remove last comma character
                buf.deleteCharAt(buf.lastIndexOf(","));
                data.put("platformID", buf.toString());
            }
        } else {
            String notFoundMessage = "Unable to find Issued Attestation Certificate "
                    + "with ID: " + uuid;
            log.error(notFoundMessage);
        }
        return data;
    }

    /**
     * Returns the IDevID Certificate information.
     *
     * @param uuid                   ID for the certificate.
     * @param certificateRepository  the certificate manager for retrieving certs.
     * @param caCredentialRepository CA Credential repository.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getIdevidInformation(final UUID uuid,
                                                               final CertificateRepository
                                                                       certificateRepository,
                                                               final CACredentialRepository
                                                                       caCredentialRepository) {

        HashMap<String, String> data = new HashMap<>();
        Map<String, String> ekuMap = getExtendedKeyUsageMap();
        IDevIDCertificate certificate = (IDevIDCertificate) certificateRepository.getCertificate(uuid);

        if (certificate != null) {
            data.putAll(
                    getGeneralCertificateInfo(certificate, certificateRepository, caCredentialRepository));

            if (certificate.getHwType() != null) {
                data.put("hwType", certificate.getHwType());
                String hwTypeReadable;
                if (certificate.hasTCGOIDs()) {
                    hwTypeReadable = "TPM-Bound IDevID";
                } else {
                    hwTypeReadable = "Manufacturer Specific";
                }
                data.put("hwTypeReadable", hwTypeReadable);
            }

            if (certificate.getHwSerialNum() != null) {
                String hwSerialStr = new String(certificate.getHwSerialNum(), StandardCharsets.US_ASCII);

                // Obtain colon-delimited fields from hwSerialNum field, if present
                if (certificate.hasTCGOIDs()) {
                    if (hwSerialStr.contains(":")) {
                        String[] hwSerialArray = hwSerialStr.split(":");
                        final int minArrayLength = 3;
                        if (hwSerialArray.length >= minArrayLength) {
                            data.put("tcgTpmManufacturer", hwSerialArray[0]);
                            data.put("ekAuthorityKeyIdentifier", hwSerialArray[1]);
                            data.put("ekCertificateSerialNumber", hwSerialArray[2]);
                        }
                    } else {
                        // Corresponds to digest of EK certificate
                        data.put("ekCertificateDigest", Boolean.valueOf(true).toString());
                        String hwSerialToAdd = Hex.toHexString(certificate.getHwSerialNum());
                        data.put("hwSerialNumHex", Boolean.valueOf(true).toString());
                        data.put("hwSerialNum", hwSerialToAdd);
                    }
                } else {
                    String hwSerialToAdd = hwSerialStr;

                    // Check if hwSerialNum is a printable ASCII string; default to hex otherwise
                    final int minMatchedNum = 0x20;
                    final int maxMatchedNum = 0x7F;
                    if (hwSerialStr.chars().allMatch(c -> c > minMatchedNum && c <= maxMatchedNum)) {
                        data.put("hwSerialNum", hwSerialStr);
                    } else {
                        hwSerialToAdd = Hex.toHexString(certificate.getHwSerialNum());
                        data.put("hwSerialNumHex", Boolean.valueOf(true).toString());
                    }
                    data.put("hwSerialNum", hwSerialToAdd);
                }
            }

            if (certificate.getKeyUsage() != null) {
                data.put("keyUsage", certificate.getKeyUsage());
            }

            if (certificate.getExtendedKeyUsage() != null
                    && !certificate.getExtendedKeyUsage().isEmpty()) {
                String eku = certificate.getExtendedKeyUsage().replaceAll("\\n$", "");
                if (ekuMap.containsKey(eku)) {
                    data.put("extendedKeyUsage", eku + " (" + ekuMap.get(eku) + ")");
                } else {
                    data.put("extendedKeyUsage", eku + " (Warning: Unexpected OID)");
                }
            }

            if (certificate.getTpmPolicies() != null) {
                data.put("tpmPolicies", certificate.getTpmPolicies());
            }

            data.put("x509Version", Integer.toString(certificate
                    .getX509CredentialVersion()));
        } else {
            String notFoundMessage = "Unable to find IDevIDCertificate with ID: " + uuid;
            log.error(notFoundMessage);
        }
        return data;
    }
}
