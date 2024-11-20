package hirs.attestationca.persist.provision.helper;

import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x509.AttributeCertificateInfo;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.GeneralNamesBuilder;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.TBSCertificate;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * Builds extensions based on Platform and Endorsement credentials to provide in an issued
 * certificate.
 */
@Log4j2
public final class IssuedCertificateAttributeHelper {

    /**
     * The extended key usage extension.
     */
    public static final Extension EXTENDED_KEY_USAGE_EXTENSION;
    private static final String TPM_ID_LABEL_OID = "2.23.133.2.15";
    /**
     * Object Identifier TCPA at TPM ID Label.
     */
    public static final ASN1ObjectIdentifier TCPA_AT_TPM_ID_LABEL =
            new ASN1ObjectIdentifier(TPM_ID_LABEL_OID);
    private static final ASN1ObjectIdentifier TCG_KP_AIK_CERTIFICATE_ATTRIBUTE =
            new ASN1ObjectIdentifier("2.23.133.8.3");

    static {
        // Generates an extension that identifies a cert as an AIK cert
        Extension extension = null;
        try {
            extension = new Extension(Extension.extendedKeyUsage, true,
                    new ExtendedKeyUsage(new KeyPurposeId[] {
                            KeyPurposeId.getInstance(TCG_KP_AIK_CERTIFICATE_ATTRIBUTE),
                            KeyPurposeId.getInstance(KeyPurposeId.id_kp_clientAuth)
                            }).getEncoded());
        } catch (IOException e) {
            log.error("Error generating extended key usage extension");
        }
        EXTENDED_KEY_USAGE_EXTENSION = extension;
    }

    private IssuedCertificateAttributeHelper() {
        // do not construct publicly
    }

    /**
     * This method builds the AKI extension that will be stored in the generated
     * Attestation Issued Certificate.
     *
     * @param acaCertificate ACA certificate to pull SKI from, that will be used to build matching AKI.
     * @return the AKI extension.
     * @throws IOException on bad get instance for SKI.
     */
    public static Extension buildAuthorityKeyIdentifier(
            final X509Certificate acaCertificate) throws IOException {
        if (acaCertificate == null) {
            return null;
        }
        byte[] extValue = acaCertificate
                .getExtensionValue(Extension.subjectKeyIdentifier.getId());

        if (extValue == null) {
            return null;
        }

        byte[] authExtension = ASN1OctetString.getInstance(extValue).getOctets();
        SubjectKeyIdentifier ski = SubjectKeyIdentifier.getInstance(authExtension);

        AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier(ski.getKeyIdentifier());

        return new Extension(Extension.authorityKeyIdentifier, false, aki.getEncoded());
    }

    /**
     * Builds the subject alternative name based on the supplied certificates.
     *
     * @param endorsementCredential the endorsement credential
     * @param platformCredentials   the platform credentials
     * @param hostName              the host name
     * @return the subject alternative name extension
     * @throws IOException              an IO exception occurs building the extension
     * @throws IllegalArgumentException if the host name is null
     */
    public static Extension buildSubjectAlternativeNameFromCerts(
            final EndorsementCredential endorsementCredential,
            final Collection<PlatformCredential> platformCredentials, final String hostName)
            throws IOException, IllegalArgumentException {

        if (StringUtils.isEmpty(hostName)) {
            log.error("null host name");
            throw new IllegalArgumentException("must provide host name");
        }

        // assemble AIK cert SAN, using info from EC and PC
        X500NameBuilder nameBuilder = new X500NameBuilder();
        populateEndorsementCredentialAttributes(endorsementCredential, nameBuilder);
        if (platformCredentials != null) {
            for (PlatformCredential platformCredential : platformCredentials) {
                populatePlatformCredentialAttributes(platformCredential, nameBuilder);
            }
        }

        // add the OID for the TCG-required TPM ID label
        DERUTF8String idLabel = new DERUTF8String(hostName);
        nameBuilder.addRDN(new AttributeTypeAndValue(TCPA_AT_TPM_ID_LABEL, idLabel));

        // put everything into the SAN, usable by the certificate builder
        GeneralNamesBuilder genNamesBuilder = new GeneralNamesBuilder();
        genNamesBuilder.addName(new GeneralName(nameBuilder.build()));
        DEROctetString sanContent =
                new DEROctetString(genNamesBuilder.build().getEncoded());
        Extension subjectAlternativeName = new Extension(Extension.subjectAlternativeName,
                true, sanContent);

        return subjectAlternativeName;
    }

    private static void populatePlatformCredentialAttributes(
            final PlatformCredential platformCredential,
            final X500NameBuilder nameBuilder) throws IOException {
        if (platformCredential == null) {
            return;
        }

        final RDN[] rdns;
        try {
            log.debug("Applying platform credential attributes to SAN");
            AttributeCertificateInfo platformCredentialAttributeHolders =
                    platformCredential.getAttributeCertificate().getAcinfo();
            rdns = ((X500Name) GeneralNames.fromExtensions(
                    platformCredentialAttributeHolders.getExtensions(),
                    Extension.subjectAlternativeName).getNames()[0].getName()).getRDNs();
        } catch (IllegalArgumentException iaEx) {
            log.error("Unable to extract attributes from platform credential", iaEx);
            return;
        }

        populateRdnAttributesInNameBuilder(nameBuilder, rdns);
    }

    private static void populateEndorsementCredentialAttributes(
            final EndorsementCredential endorsementCredential, final X500NameBuilder nameBuilder) {
        if (endorsementCredential == null) {
            return;
        }

        final RDN[] rdns;
        try {
            log.debug("Applying endorsement credential attributes to SAN");
            X509Certificate endorsementX509 = endorsementCredential.getX509Certificate();
            TBSCertificate tbsCertificate = TBSCertificate.getInstance(
                    endorsementX509.getTBSCertificate());
            Extensions extensions = tbsCertificate.getExtensions();
            GeneralNames names = GeneralNames.fromExtensions(extensions,
                    Extension.subjectAlternativeName);
            if (names != null) {
                X500Name x500 = (X500Name) names.getNames()[0].getName();
                rdns = x500.getRDNs();
                populateRdnAttributesInNameBuilder(nameBuilder, rdns);
            } else {
                log.error("No RDNs in endorsement credential attributes");
            }
        } catch (CertificateEncodingException e) {
            log.error("Certificate encoding exception", e);
        } catch (IOException e) {
            log.error("Error creating x509 cert from endorsement credential", e);
        }

    }

    private static void populateRdnAttributesInNameBuilder(final X500NameBuilder nameBuilder,
                                                           final RDN[] rdns) {
        for (final RDN rdn : rdns) {
            nameBuilder.addRDN(rdn.getTypesAndValues()[0]);
        }
    }
}
