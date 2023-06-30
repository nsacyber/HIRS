package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.SupplyChainValidationRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.utils.BouncyCastleUtils;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Log4j2
//@Service
public class SupplyChainValidationServiceImpl extends DefaultDbService<SupplyChainValidation> {

    @Autowired
    SupplyChainValidationRepository repository;
    @Autowired
    private CertificateRepository certificateRepository;

    public SupplyChainValidationServiceImpl(final CertificateRepository certificateRepository) {
        super();
        this.certificateRepository = certificateRepository;
    }

    /**
     * This method is used to retrieve the entire CA chain (up to a trusted
     * self-signed certificate) for the given certificate. This method will look
     * up CA certificates that have a matching issuer organization as the given
     * certificate, and will perform that operation recursively until all
     * certificates for all relevant organizations have been retrieved. For that
     * reason, the returned set of certificates may be larger than the the
     * single trust chain for the queried certificate, but is guaranteed to
     * include the trust chain if it exists in this class' CertificateManager.
     * Returns the certificate authority credentials in a KeyStore.
     *
     * @param credential the credential whose CA chain should be retrieved
     * @return A keystore containing all relevant CA credentials to the given
     * certificate's organization or null if the keystore can't be assembled
     */
    public KeyStore getCaChain(final Certificate credential) {
        KeyStore caKeyStore = null;
        try {
            caKeyStore = caCertSetToKeystore(getCaChainRec(credential, Collections.emptySet()));
        } catch (KeyStoreException | IOException e) {
            log.error("Unable to assemble CA keystore", e);
        }
        return caKeyStore;
    }

    /**
     * This is a recursive method which is used to retrieve the entire CA chain
     * (up to a trusted self-signed certificate) for the given certificate. This
     * method will look up CA certificates that have a matching issuer
     * organization as the given certificate, and will perform that operation
     * recursively until all certificates for all relevant organizations have
     * been retrieved. For that reason, the returned set of certificates may be
     * larger than the the single trust chain for the queried certificate, but
     * is guaranteed to include the trust chain if it exists in this class'
     * CertificateManager.
     * <p>
     * Implementation notes: 1. Queries for CA certs with a subject org matching
     * the given (argument's) issuer org 2. Add that org to
     * queriedOrganizations, so we don't search for that organization again 3.
     * For each returned CA cert, add that cert to the result set, and recurse
     * with that as the argument (to go up the chain), if and only if we haven't
     * already queried for that organization (which prevents infinite loops on
     * certs with an identical subject and issuer org)
     *
     * @param credential                the credential whose CA chain should be retrieved
     * @param previouslyQueriedSubjects a list of organizations to refrain
     *                                  from querying
     * @return a Set containing all relevant CA credentials to the given
     * certificate's organization
     */
    private Set<CertificateAuthorityCredential> getCaChainRec(
            final Certificate credential,
            final Set<String> previouslyQueriedSubjects) {
        CertificateAuthorityCredential skiCA = null;
        List<CertificateAuthorityCredential> certAuthsWithMatchingIssuer = new LinkedList<>();
        if (credential.getAuthorityKeyIdentifier() != null
                && !credential.getAuthorityKeyIdentifier().isEmpty()) {
            byte[] bytes = Hex.decode(credential.getAuthorityKeyIdentifier());
            skiCA = (CertificateAuthorityCredential) certificateRepository.findBySubjectKeyIdentifier(bytes);
        }

        if (skiCA == null) {
            if (credential.getIssuerSorted() == null
                    || credential.getIssuerSorted().isEmpty()) {
                certAuthsWithMatchingIssuer = certificateRepository.findBySubject(credential.getHolderIssuer(),
                        "CertificateAuthorityCredential");
            } else {
                //Get certificates by subject organization
                certAuthsWithMatchingIssuer = certificateRepository.findBySubjectSorted(credential.getIssuerSorted(),
                        "CertificateAuthorityCredential");

            }
        } else {
            certAuthsWithMatchingIssuer.add(skiCA);
        }
        Set<String> queriedOrganizations = new HashSet<>(previouslyQueriedSubjects);
        queriedOrganizations.add(credential.getHolderIssuer());

        HashSet<CertificateAuthorityCredential> caCreds = new HashSet<>();
        for (CertificateAuthorityCredential cred : certAuthsWithMatchingIssuer) {
            caCreds.add(cred);
            if (!BouncyCastleUtils.x500NameCompare(cred.getHolderIssuer(),
                    cred.getSubject())) {
                caCreds.addAll(getCaChainRec(cred, queriedOrganizations));
            }
        }

        return caCreds;
    }

    private KeyStore caCertSetToKeystore(final Set<CertificateAuthorityCredential> certs)
            throws KeyStoreException, IOException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try {
            keyStore.load(null, "".toCharArray());
            for (Certificate cert : certs) {
                keyStore.setCertificateEntry(cert.getId().toString(), cert.getX509Certificate());
            }
        } catch (IOException | CertificateException | NoSuchAlgorithmException e) {
            throw new IOException("Could not create and populate keystore", e);
        }

        return keyStore;
    }

    private boolean checkForMultipleBaseCredentials(final String platformSerialNumber) {
        boolean multiple = false;
        PlatformCredential baseCredential = null;

        if (platformSerialNumber != null) {
            List<PlatformCredential> chainCertificates = certificateRepository
                    .byBoardSerialNumber(platformSerialNumber);

            for (PlatformCredential pc : chainCertificates) {
                if (baseCredential != null && pc.isPlatformBase()) {
                    multiple = true;
                } else if (pc.isPlatformBase()) {
                    baseCredential = pc;
                }
            }
        }

        return multiple;
    }
}
