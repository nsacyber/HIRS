package hirs.attestationca.portal.page.utils;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.service.CertificateServiceImpl;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
     * @param certificateServiceImpl the certificate manager for retrieving certs.
     * @return a hash map with the general certificate information.
     */
    public static HashMap<String, String> getGeneralCertificateInfo(
            final Certificate certificate, final CertificateServiceImpl certificateServiceImpl) {
        HashMap<String, String> data = new HashMap<>();

        return data;
    }

    /**
     * Recursive function that check if all the certificate chain is present.
     *
     * @param certificate certificate to get the issuer
     * @param certificateServiceImpl the certificate manager for retrieving certs.
     * @return a boolean indicating if it has the full chain or not.
     */
    public static Certificate containsAllChain(
            final Certificate certificate,
            final CertificateServiceImpl certificateServiceImpl) {
        Set<CertificateAuthorityCredential> issuerCertificates = new HashSet<>();
        CertificateAuthorityCredential skiCA = null;
        String issuerResult;

        return null;
    }

    /**
     * Returns the Certificate Authority information.
     *
     * @param uuid ID for the certificate.
     * @param certificateServiceImpl the certificate manager for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getCertificateAuthorityInformation(final UUID uuid,
                                                                             final CertificateServiceImpl certificateServiceImpl) {
//        CertificateAuthorityCredential certificate =
//                CertificateAuthorityCredential
//                        .select(certificateManager)
//                        .byEntityId(uuid)
//                        .getCertificate();
        String notFoundMessage = "Unable to find Certificate Authority "
                + "Credential with ID: " + uuid;

//        return getCertificateAuthorityInfoHelper(certificateServiceImpl, certificate, notFoundMessage);
        return null;
    }

    /**
     * Returns the Trust Chain credential information.
     *
     * @param certificate the certificate
     * @param certificateServiceImpl the certificate manager for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getCertificateAuthorityInformation(
            final CertificateAuthorityCredential certificate,
            final CertificateServiceImpl certificateServiceImpl) {
//        return getCertificateAuthorityInfoHelper(certificateManager, certificate,
//                "No cert provided for mapping");
        return null;
    }

    private static HashMap<String, String> getCertificateAuthorityInfoHelper(
            final CertificateServiceImpl certificateServiceImpl,
            final CertificateAuthorityCredential certificate, final String notFoundMessage) {
        HashMap<String, String> data = new HashMap<>();

        return data;
    }

    /**
     * Returns the endorsement credential information.
     *
     * @param uuid ID for the certificate.
     * @param certificateServiceImpl the certificate manager for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getEndorsementInformation(final UUID uuid,
                                                                    final CertificateServiceImpl certificateServiceImpl) {
        HashMap<String, String> data = new HashMap<>();

        return data;
    }

    /**
     * Returns the Platform credential information.
     *
     * @param uuid ID for the certificate.
     * @param certificateServiceImpl the certificate manager for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     * @throws IOException when parsing the certificate
     * @throws IllegalArgumentException invalid argument on parsing the certificate
     */
    public static HashMap<String, Object> getPlatformInformation(final UUID uuid,
                                                                 final CertificateServiceImpl certificateServiceImpl)
            throws IllegalArgumentException, IOException {
        HashMap<String, Object> data = new HashMap<>();

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
     * @param certificateServiceImpl the certificate manager for retrieving certs.
     * @return a hash map with the endorsement certificate information.
     */
    public static HashMap<String, String> getIssuedInformation(final UUID uuid,
                                                               final CertificateServiceImpl certificateServiceImpl) {
        HashMap<String, String> data = new HashMap<>();

        return data;
    }
}
