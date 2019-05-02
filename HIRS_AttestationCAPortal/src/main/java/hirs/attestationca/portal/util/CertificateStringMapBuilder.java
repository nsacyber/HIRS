package hirs.attestationca.portal.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
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
import hirs.data.persist.certificate.attributes.PlatformConfiguration;
import hirs.persist.CertificateManager;
import hirs.utils.BouncyCastleUtils;
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
            data.put("serialNumber", Long.toHexString(certificate.getSerialNumber().longValue()));
            if (!certificate.getAuthoritySerialNumber().equals(BigInteger.ZERO)) {
                data.put("authSerialNumber", Long.toHexString(certificate
                        .getAuthoritySerialNumber().longValue()));
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
                if (missingCert != null) {
                    data.put("missingChainIssuer", missingCert.getIssuer());
                }
                //Find all certificates that could be the issuer certificate based on subject name
                for (Certificate issuerCert : CertificateAuthorityCredential
                        .select(certificateManager)
                        .bySubject(certificate.getIssuer())
                        .getCertificates()) {

                    try {
                        //Find the certificate that actually signed this cert
                        if (certificate.isIssuer(issuerCert)) {
                            data.put("issuerID", issuerCert.getId().toString());
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

        Set<CertificateAuthorityCredential> issuerCertificates;
        //Check if there is a subject organization
        if (certificate.getIssuerOrganization() == null
                || certificate.getIssuerOrganization().isEmpty()) {
            //Get certificates by subject
            issuerCertificates = CertificateAuthorityCredential.select(certificateManager)
                                    .bySubject(certificate.getIssuer())
                                     .getCertificates();
        } else {
            //Get certificates by subject organization
            issuerCertificates = CertificateAuthorityCredential.select(certificateManager)
                                    .bySubjectOrganization(certificate.getIssuerOrganization())
                                     .getCertificates();
        }

        for (Certificate issuerCert : issuerCertificates) {
            try {
                // Find the certificate that actually signed this cert
                if (certificate.isIssuer(issuerCert)) {
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
     * @return a hash map with the endorsement certificate information.
     * @throws IOException when parsing the certificate
     * @throws IllegalArgumentException invalid argument on parsing the certificate
     */
    public static HashMap<String, Object> getPlatformInformation(final UUID uuid,
            final CertificateManager certificateManager)
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

            //Get platform Configuration values and set map with it
            PlatformConfiguration platformConfiguration = certificate.getPlatformConfiguration();
            if (platformConfiguration != null) {
                //Component Identifier
                data.put("componentsIdentifier", platformConfiguration.getComponentIdentifier());
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
        for (String pair: data.split(",")) {
            String[] keyValue = pair.split("=");
            // Remove white space and change firt charater in the key to uppsercase
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
                data.put("endorsementID",
                        certificate.getEndorsementCredential().getId().toString());
            }
            // add platform credential IDs if not empty
            if (!certificate.getPlatformCredentials().isEmpty()) {
                StringBuilder buf = new StringBuilder();
                for (PlatformCredential pc: certificate.getPlatformCredentials()) {
                    buf.append(pc.getId().toString());
                    buf.append(',');
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
