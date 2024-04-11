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
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.PlatformConfiguration;
import hirs.attestationca.persist.util.PciIds;
import hirs.utils.BouncyCastleUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for mapping certificate information in to string maps. These are used to display
 * information on a web page, as X509 cert classes do not serialize to JSON
 */
@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CertificateStringMapBuilder {

    /**
     * Returns the general information.
     *
     * @param certificate certificate to get the general information.
     * @param certificateRepository the certificate repository for retrieving certs.
     * @return a hash map with the general certificate information.
     */
    public static HashMap<String, String> getGeneralCertificateInfo(
            final Certificate certificate, final CertificateRepository certificateRepository,
            final CACredentialRepository caCertificateRepository) {
        HashMap<String, String> data = new HashMap<>();

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
            data.put("signatureSize", Integer.toString(certificate.getSignature().length
                    * Certificate.MIN_ATTR_CERT_LENGTH));

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
            }

            if (certificate.getPublicKeyModulusHexValue() != null) {
                data.put("publicKeyValue", certificate.getPublicKeyModulusHexValue());
                data.put("publicKeySize", String.valueOf(certificate.getPublicKeySize()));
            }

            if (certificate.getKeyUsage() != null) {
                data.put("keyUsage", certificate.getKeyUsage());
            }

            if (certificate.getExtendedKeyUsage() != null
                    && !certificate.getExtendedKeyUsage().isEmpty()) {
                data.put("extendedKeyUsage", certificate.getExtendedKeyUsage());
            }

            //Get issuer ID if not self signed
            if (data.get("isSelfSigned").equals("false")) {
                //Get the missing certificate chain for not self sign
                Certificate missingCert = containsAllChain(certificate, certificateRepository, caCertificateRepository);
                String issuerResult;

                if (missingCert != null) {
                    data.put("missingChainIssuer", String.format("Missing %s from the chain.",
                            missingCert.getIssuer()));
                }
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
     * @param certificate certificate to get the issuer
     * @param certificateRepository the certificate repository for retrieving certs.
     * @return a boolean indicating if it has the full chain or not.
     */
    public static Certificate containsAllChain(
            final Certificate certificate,
            final CertificateRepository certificateRepository,
            final CACredentialRepository caCredentialRepository) {
        List<CertificateAuthorityCredential> issuerCertificates = new ArrayList<>();
        CertificateAuthorityCredential skiCA = null;
        String issuerResult;

        //Check if there is a subject organization
        if (certificate.getAuthorityKeyIdentifier() != null
                && !certificate.getAuthorityKeyIdentifier().isEmpty()) {
            skiCA = caCredentialRepository.findBySubjectKeyIdStringAndArchiveFlag(certificate.getAuthorityKeyIdentifier(), false);
        } else {
            log.error(String.format("Certificate (%s) for %s has no authority key identifier.",
                    certificate.getClass().toString(), certificate.getSubject()));
        }

        if (skiCA == null) {
            if (certificate.getIssuerSorted() == null
                    || certificate.getIssuerSorted().isEmpty()) {
                //Get certificates by subject
                issuerCertificates = caCredentialRepository.findBySubjectAndArchiveFlag(certificate.getIssuer(), false);
            } else {
                //Get certificates by subject organization
                issuerCertificates = caCredentialRepository.findBySubjectSortedAndArchiveFlag(certificate.getIssuerSorted(), false);
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
                    return containsAllChain(issuerCert, certificateRepository, caCredentialRepository);
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
     * @param uuid ID for the certificate.
     * @param caCertificateRepository the certificate manager for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getCertificateAuthorityInformation(final UUID uuid,
                                                                             final CertificateRepository certificateRepository,
                                                                             final CACredentialRepository caCertificateRepository) {
        if (!caCertificateRepository.existsById(uuid)) {
            return new HashMap<>();
        }
        CertificateAuthorityCredential certificate = caCertificateRepository.getReferenceById(uuid);

        String notFoundMessage = "Unable to find Certificate Authority "
                + "Credential with ID: " + uuid;

        return getCertificateAuthorityInfoHelper(certificateRepository, caCertificateRepository, certificate, notFoundMessage);
    }

    /**
     * Returns the Trust Chain credential information.
     *
     * @param certificate the certificate
     * @param certificateRepository the certificate repository for retrieving certs.
     * @param caCertificateRepository the certificate repository for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getCertificateAuthorityInformation(
            final CertificateAuthorityCredential certificate,
            final CertificateRepository certificateRepository,
            final CACredentialRepository caCertificateRepository) {
        return getCertificateAuthorityInfoHelper(certificateRepository, caCertificateRepository, certificate,
                "No cert provided for mapping");
    }

    private static HashMap<String, String> getCertificateAuthorityInfoHelper(
            final CertificateRepository certificateRepository,
            final CACredentialRepository caCertificateRepository,
            final CertificateAuthorityCredential certificate, final String notFoundMessage) {
        HashMap<String, String> data = new HashMap<>();

        if (certificate != null) {
            data.putAll(getGeneralCertificateInfo(certificate, certificateRepository, caCertificateRepository));
            data.put("subjectKeyIdentifier",
                    Arrays.toString(certificate.getSubjectKeyIdentifier()));
            //x509 credential version
            data.put("x509Version", Integer.toString(certificate
                    .getX509CredentialVersion()));
            data.put("credentialType", certificate.getCredentialType());
        } else {
            log.error(notFoundMessage);
        }
        return data;
    }

    /**
     * Returns the endorsement credential information.
     *
     * @param uuid ID for the certificate.
     * @param certificateRepository the certificate repository for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getEndorsementInformation(final UUID uuid,
                                                                    final CertificateRepository certificateRepository,
                                                                    final CACredentialRepository caCertificateRepository) {
        HashMap<String, String> data = new HashMap<>();
        EndorsementCredential certificate = (EndorsementCredential) certificateRepository.getCertificate(uuid);

        if (certificate != null) {
            data.putAll(getGeneralCertificateInfo(certificate, certificateRepository, caCertificateRepository));
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
            // Add hashmap with TPM information if available
            if (certificate.getTpmSpecification() != null) {
                data.putAll(
                        convertStringToHash(certificate.getTpmSpecification().toString()));
            }
            if (certificate.getTpmSecurityAssertions() != null) {
                data.putAll(
                        convertStringToHash(certificate.getTpmSecurityAssertions().toString()));
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
     * @param uuid ID for the certificate.
     * @param certificateRepository the certificate manager for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     * @throws IOException when parsing the certificate
     * @throws IllegalArgumentException invalid argument on parsing the certificate
     */
    public static HashMap<String, Object> getPlatformInformation(final UUID uuid,
                                                                 final CertificateRepository certificateRepository,
                                                                 final ComponentResultRepository componentResultRepository,
                                                                 final CACredentialRepository caCertificateRepository)
            throws IllegalArgumentException, IOException {
        HashMap<String, Object> data = new HashMap<>();
        PlatformCredential certificate = (PlatformCredential) certificateRepository.getCertificate(uuid);

        if (certificate != null) {
            data.putAll(getGeneralCertificateInfo(certificate, certificateRepository, caCertificateRepository));
            data.put("credentialType", certificate.getCredentialType());
            data.put("platformType", certificate.getPlatformChainType());
            data.put("manufacturer", certificate.getManufacturer());
            data.put("model", certificate.getModel());
            data.put("version", certificate.getVersion());
            data.put("platformSerial", certificate.getPlatformSerial());
            data.put("chassisSerialNumber", certificate.getChassisSerialNumber());
            data.put("platformClass", certificate.getPlatformClass());
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
                if (certificate.getPlatformChainType()!= null
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
                compResults = PciIds.translateResults(compResults);
            }
            data.put("componentResults", compResults);

            //Get platform Configuration values and set map with it
            PlatformConfiguration platformConfiguration = certificate.getPlatformConfiguration();
            if (platformConfiguration != null) {
                //Component Identifier - attempt to translate hardware IDs
                List<ComponentIdentifier> comps = platformConfiguration.getComponentIdentifier();
                if (PciIds.DB.isReady()) {
                    comps = PciIds.translate(comps);
                }
                data.put("componentsIdentifier", comps);
                //Component Identifier URI
                data.put("componentsIdentifierURI", platformConfiguration
                        .getComponentIdentifierUri());
                //Platform Properties
                data.put("platformProperties", platformConfiguration.getPlatformProperties());
                //Platform Properties URI
                data.put("platformPropertiesURI", platformConfiguration.getPlatformPropertiesUri());
            }
            //TBB Security Assertion
            data.put("tbbSecurityAssertion", certificate.getTBBSecurityAssertion());

            if (certificate.getPlatformSerial() != null) {
                // link certificate chain
                List<PlatformCredential> chainCertificates = certificateRepository.byBoardSerialNumber(certificate.getPlatformSerial());
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
     *     output   map[TPMSpecificationFamily] = 'abc'
     *              map[TPMSpecificationLevel] = 0
     *              map[TPMSpecificationRevision] = 0
     *
     * @param str HashMap string to be converted.
     * @return a hash map with key-value pairs from the string
     */
    private static HashMap<String, String> convertStringToHash(final String str) {
        HashMap<String, String> map = new HashMap<>();
        String name = str.substring(0, str.indexOf('{')).trim();
        String data = str.trim().substring(str.trim().indexOf('{') + 1,
                str.trim().length() - 1);
        // Separate key and value and parse the key
        for (String pair : data.split(",")) {
            String[] keyValue = pair.split("=");
            // Remove white space and change first character in the key to uppercase
            keyValue[0] = Character.toUpperCase(
                    keyValue[0].trim().charAt(0)) + keyValue[0].trim().substring(1);

            map.put(name + keyValue[0], keyValue[1].trim());
        }
        return map;
    }

    /**
     * Returns the Issued Attestation Certificate information.
     *
     * @param uuid ID for the certificate.
     * @param certificateRepository the certificate manager for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getIssuedInformation(final UUID uuid,
                                                               final CertificateRepository certificateRepository,
                                                               final CACredentialRepository caCredentialRepository) {
        HashMap<String, String> data = new HashMap<>();
        IssuedAttestationCertificate certificate = (IssuedAttestationCertificate) certificateRepository.getCertificate(uuid);

        if (certificate != null) {
            data.putAll(getGeneralCertificateInfo(certificate, certificateRepository, caCredentialRepository));

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
     * @param uuid ID for the certificate.
     * @param certificateRepository the certificate manager for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getIdevidInformation(final UUID uuid,
                                                               final CertificateRepository certificateRepository,
                                                               final CACredentialRepository caCredentialRepository) {

        HashMap<String, String> data = new HashMap<>();
        IDevIDCertificate certificate = (IDevIDCertificate) certificateRepository.getCertificate(uuid);

        if (certificate != null) {
            data.putAll(getGeneralCertificateInfo(certificate, certificateRepository, caCredentialRepository));

            if (certificate.getHwSerialNum() != null) {
                String hwSerialStr = new String(certificate.getHwSerialNum(), StandardCharsets.US_ASCII);

                // Obtain colon-delimited fields from hwSerialNum field, if present
                if (hwSerialStr.contains(":")) {
                    String[] hwSerialArray = hwSerialStr.split(":");
                    if (hwSerialArray.length >= 3) {
                        data.put("tcgTpmManufacturer", hwSerialArray[0]);
                        data.put("ekAuthorityKeyIdentifier", hwSerialArray[1]);
                        data.put("ekCertificateSerialNumber", hwSerialArray[2]);
                    }
                } else {
                    String hwSerialToAdd = hwSerialStr;

                    // Check if hwSerialNum is a printable ASCII string; default to hex otherwise
                    if (hwSerialStr.chars().allMatch(c -> c > 0x20 && c <= 0x7F)) {
                        data.put("hwSerialNum", hwSerialStr);
                    }
                    else {
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
                data.put("extendedKeyUsage", certificate.getExtendedKeyUsage());
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
