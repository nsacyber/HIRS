package hirs.attestationca;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.GeneralNamesBuilder;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.asn1.x509.AttributeCertificateInfo;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import hirs.attestationca.entity.certificate.EndorsementCredential;
import hirs.attestationca.entity.certificate.PlatformCredential;

/**
 * Builds extensions based on Platform and Endorsement credentials to provide in an issued
 * certificate.
 */
public final class IssuedCertificateAttributeHelper {

    private static final String TPM_ID_LABEL_OID = "2.23.133.2.15";

    /**
     * Object Identifier TCPA at TPM ID Label.
     */
    public static final ASN1ObjectIdentifier TCPA_AT_TPM_ID_LABEL =
            new ASN1ObjectIdentifier(TPM_ID_LABEL_OID);
    /**
     * The extended key usage extension.
     */
    public static final Extension EXTENDED_KEY_USAGE_EXTENSION;
    private static final Logger LOG = LogManager.getLogger(IssuedCertificateAttributeHelper.class);
    private static final ASN1ObjectIdentifier TCG_KP_AIK_CERTIFICATE_ATTRIBUTE =
            new ASN1ObjectIdentifier("2.23.133.8.3");

    static {
        // Generates an extension that identifies a cert as an AIK cert
        Extension extension = null;
        try {
            extension = new Extension(Extension.extendedKeyUsage, true,
                new ExtendedKeyUsage(new KeyPurposeId[] {
                KeyPurposeId.getInstance(TCG_KP_AIK_CERTIFICATE_ATTRIBUTE)}).getEncoded());
        } catch (IOException e) {
            LOG.error("Error generating extended key usage extension");
        }
        EXTENDED_KEY_USAGE_EXTENSION = extension;
    }

    private IssuedCertificateAttributeHelper() {
        // do not construct publicly
    }

    /**
     * This method builds the AKI extension that will be stored in the generated
     * Attestation Issued Certificate.
     * @param endorsementCredential EK object to pull AKI from.
     * @return the AKI extension.
     * @throws IOException on bad get instance for AKI.
     */
    public static Extension buildAuthorityKeyIdentifier(
            final EndorsementCredential endorsementCredential) throws IOException {
        if (endorsementCredential == null || endorsementCredential.getX509Certificate() == null) {
            return null;
        }
        byte[] extValue = endorsementCredential.getX509Certificate()
                    .getExtensionValue(Extension.authorityKeyIdentifier.getId());

        if (extValue == null) {
            return null;
        }

        byte[] authExtension = ASN1OctetString.getInstance(extValue).getOctets();
        AuthorityKeyIdentifier aki = AuthorityKeyIdentifier.getInstance(authExtension);

        return new Extension(Extension.authorityKeyIdentifier, true, aki.getEncoded());
    }

    /**
     * Builds the subject alternative name based on the supplied certificates.
     * @param endorsementCredential the endorsement credential
     * @param platformCredentials the platform credentials
     * @param hostName the host name
     * @return the subject alternative name extension
     * @throws IOException an IO exception occurs building the extension
     * @throws IllegalArgumentException if the host name is null
     */
    public static Extension buildSubjectAlternativeNameFromCerts(
            final EndorsementCredential endorsementCredential,
            final Collection<PlatformCredential> platformCredentials, final String hostName)
            throws IOException, IllegalArgumentException {

        if (StringUtils.isEmpty(hostName)) {
            LOG.error("null host name");
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
            LOG.debug("Applying platform credential attributes to SAN");
            AttributeCertificateInfo platformCredentialAttributeHolders =
                    platformCredential.getAttributeCertificate().getAcinfo();
            rdns = ((X500Name) GeneralNames.fromExtensions(
                    platformCredentialAttributeHolders.getExtensions(),
                    Extension.subjectAlternativeName).getNames()[0].getName()).getRDNs();
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to extract attributes from platform credential", e);
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
            LOG.debug("Applying endorsement credential attributes to SAN");
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
                LOG.error("No RDNs in endorsement credential attributes");
                return;
            }
        } catch (CertificateEncodingException e) {
            LOG.error("Certificate encoding exception", e);
            return;
        } catch (IOException e) {
            LOG.error("Error creating x509 cert from endorsement credential", e);
            return;
        }

    }

    private static void populateRdnAttributesInNameBuilder(final X500NameBuilder nameBuilder,
                                                    final RDN[] rdns) {
        for (final RDN rdn : rdns) {
            nameBuilder.addRDN(rdn.getTypesAndValues()[0]);
        }
    }
}
