package hirs.attestationca.portal.util;

import hirs.persist.ComponentResultManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.UUID;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.CertificateAuthorityCredential;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.data.persist.certificate.IssuedAttestationCertificate;
import hirs.data.persist.certificate.attributes.ComponentIdentifier;
import hirs.data.persist.certificate.attributes.PlatformConfiguration;
import hirs.persist.CertificateManager;
import hirs.utils.BouncyCastleUtils;
import org.bouncycastle.util.encoders.Hex;

import java.util.Collections;

/**
 * Utility class for mapping certificate information in to string maps. These are used to display
 * information on a web page, as X509 cert classes do not serialize to JSON
 */
public final class CertificateStringMapBuilder {

    private static final Logger LOGGER =
            LogManager.getLogger(CertificateStringMapBuilder.class);

    private CertificateStringMapBuilder() {

    }

    /**
     * Returns the general information.
     *
     * @param certificate certificate to get the general information.
     * @param certificateManager the certificate manager for retrieving certs.
     * @return a hash map with the general certificate information.
     */
    public static HashMap<String, String> getGeneralCertificateInfo(
            final Certificate certificate, final CertificateManager certificateManager) {
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
            data.put("authInfoAccess", certificate.getAuthInfoAccess());
            data.put("beginValidity", certificate.getBeginValidity().toString());
            data.put("endValidity", certificate.getEndValidity().toString());
            data.put("signature", Arrays.toString(certificate.getSignature()));
            data.put("signatureSize", Integer.toString(certificate.getSignature().length
                    * Certificate.MIN_ATTR_CERT_LENGTH));

            if (certificate.getSubject() != null) {
                data.put("subject", certificate.getSubject());
                data.put("isSelfSigned",
                    String.valueOf(certificate.getIssuer().equals(certificate.getSubject())));
            } else {
                data.put("isSelfSigned", "false");
            }

            data.put("authKeyId", certificate.getAuthKeyId());
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
                Certificate missingCert = containsAllChain(certificate, certificateManager);
                String issuerResult;

                if (missingCert != null) {
                    data.put("missingChainIssuer", String.format("Missing %s from the chain.",
                            missingCert.getIssuer()));
                }
                //Find all certificates that could be the issuer certificate based on subject name
                for (Certificate issuerCert : CertificateAuthorityCredential
                        .select(certificateManager)
                        .bySubjectSorted(certificate.getIssuerSorted())
                        .getCertificates()) {

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
                        LOGGER.error(e);
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
     * @param certificateManager the certificate manager for retrieving certs.
     * @return a boolean indicating if it has the full chain or not.
     */
    public static Certificate containsAllChain(
            final Certificate certificate,
            final CertificateManager certificateManager) {
        Set<CertificateAuthorityCredential> issuerCertificates = new HashSet<>();
        CertificateAuthorityCredential skiCA = null;
        String issuerResult;
        //Check if there is a subject organization
        if (certificate.getAuthKeyId() != null
                && !certificate.getAuthKeyId().isEmpty()) {
            byte[] bytes = Hex.decode(certificate.getAuthKeyId());
            skiCA = CertificateAuthorityCredential
                    .select(certificateManager)
                    .bySubjectKeyIdentifier(bytes).getCertificate();
        } else {
            LOGGER.error(String.format("Certificate (%s) for %s has no authority key identifier.",
                    certificate.getClass().toString(), certificate.getSubject()));
        }

        if (skiCA == null) {
            if (certificate.getIssuerSorted() == null
                    || certificate.getIssuerSorted().isEmpty()) {
                //Get certificates by subject
                issuerCertificates = CertificateAuthorityCredential.select(certificateManager)
                        .bySubject(certificate.getIssuer())
                        .getCertificates();
            } else {
                //Get certificates by subject organization
                issuerCertificates = CertificateAuthorityCredential.select(certificateManager)
                        .bySubjectSorted(certificate.getIssuerSorted())
                        .getCertificates();
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
                    if (BouncyCastleUtils.x500NameCompare(issuerCert.getIssuer(),
                            issuerCert.getSubject())) {
                        return null;
                    }
                    return containsAllChain(issuerCert, certificateManager);
                }
            } catch (IOException e) {
                LOGGER.error(e);
                return certificate;
            }
        }

        return certificate;
    }

    /**
     * Returns the Certificate Authority information.
     *
     * @param uuid ID for the certificate.
     * @param certificateManager the certificate manager for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getCertificateAuthorityInformation(final UUID uuid,
            final CertificateManager certificateManager) {
        CertificateAuthorityCredential certificate =
                CertificateAuthorityCredential
                        .select(certificateManager)
                        .byEntityId(uuid)
                        .getCertificate();

        String notFoundMessage = "Unable to find Certificate Authority "
                + "Credential with ID: " + uuid;

        return getCertificateAuthorityInfoHelper(certificateManager, certificate, notFoundMessage);
    }


    /**
     * Returns the Trust Chain credential information.
     *
     * @param certificate the certificate
     * @param certificateManager the certificate manager for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getCertificateAuthorityInformation(
            final CertificateAuthorityCredential certificate,
            final CertificateManager certificateManager) {
        return getCertificateAuthorityInfoHelper(certificateManager, certificate,
                "No cert provided for mapping");
    }

    private static HashMap<String, String> getCertificateAuthorityInfoHelper(
            final CertificateManager certificateManager,
            final CertificateAuthorityCredential certificate, final String notFoundMessage) {
        HashMap<String, String> data = new HashMap<>();

        if (certificate != null) {
            data.putAll(getGeneralCertificateInfo(certificate, certificateManager));
            data.put("subjectKeyIdentifier",
                    Arrays.toString(certificate.getSubjectKeyIdentifier()));
            //x509 credential version
            data.put("x509Version", Integer.toString(certificate
                    .getX509CredentialVersion()));
            data.put("credentialType", certificate.getCredentialType());
        } else {
            LOGGER.error(notFoundMessage);
        }
        return data;
    }

    /**
     * Returns the endorsement credential information.
     *
     * @param uuid ID for the certificate.
     * @param certificateManager the certificate manager for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getEndorsementInformation(final UUID uuid,
            final CertificateManager certificateManager) {
        HashMap<String, String> data = new HashMap<>();
        EndorsementCredential certificate = EndorsementCredential
                .select(certificateManager)
                .byEntityId(uuid)
                .getCertificate();
        if (certificate != null) {
            data.putAll(getGeneralCertificateInfo(certificate, certificateManager));
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
            LOGGER.error(notFoundMessage);
        }
        return data;
    }

    /**
     * Returns the Platform credential information.
     *
     * @param uuid ID for the certificate.
     * @param certificateManager the certificate manager for retrieving certs.
     * @param componentResultManager the component result manager for component mismatches.
     * @return a hash map with the endorsement certificate information.
     * @throws IOException when parsing the certificate
     * @throws IllegalArgumentException invalid argument on parsing the certificate
     */
    public static HashMap<String, Object> getPlatformInformation(final UUID uuid,
            final CertificateManager certificateManager,
            final ComponentResultManager componentResultManager)
            throws IllegalArgumentException, IOException {
        HashMap<String, Object> data = new HashMap<>();
        PlatformCredential certificate = PlatformCredential
                .select(certificateManager)
                .byEntityId(uuid)
                .getCertificate();

        if (certificate != null) {
            data.putAll(getGeneralCertificateInfo(certificate, certificateManager));
            data.put("credentialType", certificate.getCredentialType());
            data.put("platformType", certificate.getPlatformType());
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
            if (certificate.isBase()) {
                EndorsementCredential ekCertificate = EndorsementCredential
                        .select(certificateManager)
                        .bySerialNumber(certificate.getHolderSerialNumber())
                        .getCertificate();
                if (ekCertificate != null) {
                    data.put("holderId", ekCertificate.getId().toString());
                }
            } else {
                if (certificate.getPlatformType() != null
                        && certificate.getPlatformType().equals("Delta")) {
                    PlatformCredential holderCertificate = PlatformCredential
                            .select(certificateManager)
                            .bySerialNumber(certificate.getHolderSerialNumber())
                            .getCertificate();
                    if (holderCertificate != null) {
                        data.put("holderId", holderCertificate.getId().toString());
                    }
                }
            }

            PlatformCredential prevCertificate = PlatformCredential
                    .select(certificateManager)
                    .byHolderSerialNumber(certificate.getSerialNumber())
                    .getCertificate();

            if (prevCertificate != null) {
                data.put("prevCertId", prevCertificate.getId().toString());
            }

            //x509 credential version
            data.put("x509Version", certificate.getX509CredentialVersion());
            //CPSuri
            data.put("CPSuri", certificate.getCPSuri());

            if (!certificate.getComponentFailures().isEmpty()) {
                data.put("failures", certificate.getComponentFailures());
                data.put("failureMessages", certificate.getComponentFailureMessage());
            }

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
                List<PlatformCredential> chainCertificates = PlatformCredential
                        .select(certificateManager)
                        .byBoardSerialNumber(certificate.getPlatformSerial())
                        .getCertificates().stream().collect(Collectors.toList());

                data.put("numInChain", chainCertificates.size());
                Collections.sort(chainCertificates, new Comparator<PlatformCredential>() {
                    @Override
                    public int compare(final PlatformCredential obj1,
                            final PlatformCredential obj2) {
                        return obj1.getBeginValidity().compareTo(obj2.getBeginValidity());
                    }
                });

                data.put("chainCertificates", chainCertificates);

                if (!certificate.isBase()) {
                    for (PlatformCredential pc : chainCertificates) {
                        if (pc.isBase()) {
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
            LOGGER.error(notFoundMessage);
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
     * @param certificateManager the certificate manager for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getIssuedInformation(final UUID uuid,
            final CertificateManager certificateManager) {
        HashMap<String, String> data = new HashMap<>();
        IssuedAttestationCertificate certificate = IssuedAttestationCertificate
                .select(certificateManager)
                .byEntityId(uuid)
                .getCertificate();
        if (certificate != null) {
            data.putAll(getGeneralCertificateInfo(certificate, certificateManager));

            // add endorsement credential ID if not null
            if (certificate.getEndorsementCredential() != null) {
                EndorsementCredential ek = certificate.getEndorsementCredential();
                data.put("endorsementID", ek.getId().toString());
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
            LOGGER.error(notFoundMessage);
        }
        return data;
    }
}
