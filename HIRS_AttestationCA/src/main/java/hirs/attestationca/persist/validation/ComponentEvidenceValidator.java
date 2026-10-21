package hirs.attestationca.persist.validation;

import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentEvidenceResult;
import hirs.attestationca.persist.provision.helper.EpscBootlogEvidence;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Verifies HP EpSC bootlog evidence against the platform certificate and the nonce the ACA issued.
 * <p>
 * The trust chain: the OEM signs the platform certificate, which identifies the EpSC Device ID certificate. ECR1 in
 * the signed evidence pins the exact Device ID certificate. The Device ID key issues the Attestation certificate,
 * whose key signs the evidence over the ACA's nonce and the platform UUID.
 */
public final class ComponentEvidenceValidator {
    private static final String SHA384 = "SHA-384";
    private static final String SIGNATURE_ALGORITHM = "SHA384withECDSA";
    private static final int ATTESTATION_INDEX = 0;
    private static final int DEVICE_ID_INDEX = 1;

    private ComponentEvidenceValidator() {
    }

    /**
     * Runs every check and records it in the result.
     *
     * @param result                 receives the evidence and the checks
     * @param platformCertIdentifier hash over the Device ID certificate's signature value, from componentPlatformCert
     * @param platformUuid           platform UUID reported by the provisioner, SMBIOS byte order, or empty
     * @param answer                 the provisioner's answer to the challenge
     */
    public static void validateEpscBootlog(final ComponentEvidenceResult result, final byte[] platformCertIdentifier,
                                           final byte[] platformUuid,
                                           final ProvisionerTpm2.ExternalEvidenceResult answer) {
        List<X509Certificate> chain = parseChain(answer);
        byte[] rawEvidence = answer.getEvidence().toByteArray();
        result.setEvidence(answer.getEvidenceVersion(), rawEvidence, encoded(chain, DEVICE_ID_INDEX),
                encoded(chain, ATTESTATION_INDEX), platformUuid);

        Optional<EpscBootlogEvidence> parsed = EpscBootlogEvidence.parse(rawEvidence);
        boolean formatOk = result.addCheck("Evidence format", parsed.isPresent()
                        && answer.getEvidenceFormat() == ProvisionerTpm2.EvidenceFormat.HP_EPSC_BOOTLOG,
                answer.getEvidenceFormat() + ", " + rawEvidence.length + " bytes");
        boolean chainOk = result.addCheck("Certificate chain [Attestation, Device ID]", chain.size() == 2,
                chain.size() + " certificate(s) returned");
        if (!formatOk || !chainOk) {
            return;
        }
        checkEvidence(result, parsed.get(), chain.get(DEVICE_ID_INDEX), chain.get(ATTESTATION_INDEX),
                platformCertIdentifier, platformUuid);
    }

    private static void checkEvidence(final ComponentEvidenceResult result, final EpscBootlogEvidence evidence,
                                      final X509Certificate deviceId, final X509Certificate attestation,
                                      final byte[] platformCertIdentifier, final byte[] platformUuid) {
        byte[] deviceIdSignatureHash = sha384(deviceId.getSignature());
        result.addCheck("Platform certificate identifies the Device ID certificate",
                Arrays.equals(platformCertIdentifier, deviceIdSignatureHash),
                "SHA-384 over the Device ID signature value " + Hex.toHexString(deviceIdSignatureHash)
                        + ", platform certificate " + Hex.toHexString(platformCertIdentifier));
        result.addCheck("Evidence pins the Device ID certificate (ECR1)",
                Arrays.equals(evidence.deviceIdCertHash(), sha384(encoded(deviceId))),
                "ECR1 " + Hex.toHexString(evidence.deviceIdCertHash()) + ", SHA-384 over the certificate "
                        + Hex.toHexString(sha384(encoded(deviceId))));
        result.addCheck("Device ID key issued the Attestation certificate", isIssuedBy(attestation, deviceId),
                "Attestation issuer " + attestation.getIssuerX500Principal());
        result.addCheck("Evidence names the Attestation certificate",
                Arrays.equals(evidence.attestationCertDigest(), sha384(encoded(attestation))),
                "Evidence " + Hex.toHexString(evidence.attestationCertDigest()) + ", SHA-384 over the certificate "
                        + Hex.toHexString(sha384(encoded(attestation))));
        result.addCheck("Attestation key signed the evidence",
                verifyRawSignature(attestation.getPublicKey(), evidence.signedData(), evidence.signature()),
                "ECDSA P-384 over " + evidence.signedData().length + " bytes");
        result.addCheck("Evidence contains the ACA nonce", Arrays.equals(evidence.nonce(), result.getNonce()),
                "Evidence " + Hex.toHexString(evidence.nonce()) + ", issued " + Hex.toHexString(result.getNonce()));
        result.addCheck("Evidence UUID matches the platform UUID", Arrays.equals(evidence.uuid(), platformUuid),
                "Evidence " + EpscBootlogEvidence.formatSmbiosUuid(evidence.uuid()) + ", platform "
                        + (platformUuid.length == 0 ? "not reported"
                        : EpscBootlogEvidence.formatSmbiosUuid(platformUuid)));
    }

    private static List<X509Certificate> parseChain(final ProvisionerTpm2.ExternalEvidenceResult answer) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return answer.getCertificateChainList().stream()
                    .map(der -> parseCertificate(factory, der.toByteArray()))
                    .flatMap(Optional::stream)
                    .toList();
        } catch (CertificateException e) {
            return List.of();
        }
    }

    private static Optional<X509Certificate> parseCertificate(final CertificateFactory factory, final byte[] der) {
        try {
            return Optional.of((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(der)));
        } catch (CertificateException e) {
            return Optional.empty();
        }
    }

    private static boolean isIssuedBy(final X509Certificate certificate, final X509Certificate issuer) {
        try {
            certificate.verify(issuer.getPublicKey());
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /**
     * Verifies an ECDSA signature given as r||s, the form the EpSC uses.
     *
     * @param key          signer's public key
     * @param data         signed data
     * @param rawSignature r||s
     * @return whether the signature verifies
     */
    private static boolean verifyRawSignature(final PublicKey key, final byte[] data, final byte[] rawSignature) {
        int half = rawSignature.length / 2;
        try {
            byte[] der = new DERSequence(new ASN1Integer[] {
                    new ASN1Integer(new BigInteger(1, Arrays.copyOfRange(rawSignature, 0, half))),
                    new ASN1Integer(new BigInteger(1, Arrays.copyOfRange(rawSignature, half, rawSignature.length)))
            }).getEncoded();
            Signature verifier = Signature.getInstance(SIGNATURE_ALGORITHM);
            verifier.initVerify(key);
            verifier.update(data);
            return verifier.verify(der);
        } catch (GeneralSecurityException | IOException e) {
            return false;
        }
    }

    private static byte[] encoded(final List<X509Certificate> chain, final int index) {
        return chain.size() > index ? encoded(chain.get(index)) : new byte[0];
    }

    private static byte[] encoded(final X509Certificate certificate) {
        try {
            return certificate.getEncoded();
        } catch (CertificateException e) {
            return new byte[0];
        }
    }

    /**
     * Hashes data with SHA-384.
     *
     * @param data the data
     * @return SHA-384 over the data
     */
    public static byte[] sha384(final byte[] data) {
        try {
            return MessageDigest.getInstance(SHA384).digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
