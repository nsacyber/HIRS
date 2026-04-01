package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for parsing and analyzing DICE (Device Identifier Composition Engine) certificate attributes.
 * Provides methods to extract DICE-specific information from X.509 certificates and classify them according
 * to TCG DICE certificate profiles.
 * @see <a href="https://trustedcomputinggroup.org/resource/dice-certificate-profiles/">TCG DICE Certificate
 *      Profiles specification</a>
 */
@Log4j2
public final class DiceCertificateParser {
    /** Private constructor to prevent instantiation of utility class. */
    private DiceCertificateParser() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    /** Key usage bit position for keyCertSign (bit 5). */
    private static final int KEY_CERT_SIGN_BIT = 5;
    /** Key usage bit position for cRLSign (bit 6). */
    private static final int CRL_SIGN_BIT = 6;

    /**
     * Parses a DICE certificate and extracts relevant attributes.
     * @param cert the X.509 certificate to parse
     * @return a {@link DiceCertificateInfo} object containing the extracted attributes, or null if invalid
     * @throws IOException if certificate parsing fails
     */
    public static DiceCertificateInfo parse(final X509Certificate cert) throws IOException {
        if (cert == null) {
            throw new IOException("Certificate must be an X.509 certificate");
        }

        DiceProfileType profileType;
        Set<DiceKeyPurpose> diceKeyPurposes;
        boolean isCa;
        boolean hasKeyCertSign;
        boolean hasCrlSign;

        // Extended Key Usage: map DICE OIDs.
        List<String> ekuOids;

        try {
            ekuOids = cert.getExtendedKeyUsage();
        } catch (CertificateParsingException e) {
            log.warn("DICE certificate contains invalid OIDs");
            return null;
        }

        if (ekuOids != null) {
            diceKeyPurposes = extractKeyPurposes(ekuOids);
        } else {
            return null; // Not a DICE certificate
        }

        // Basic constraints and key usage.
        int bc = cert.getBasicConstraints();
        isCa = (bc >= 0);

        boolean[] ku = cert.getKeyUsage();

        if (ku != null && ku.length > 0) {
            // keyCertSign is bit 5, cRLSign is bit 6 (0‑based index).
            hasKeyCertSign = ku.length > KEY_CERT_SIGN_BIT && ku[KEY_CERT_SIGN_BIT];
            hasCrlSign = ku.length > CRL_SIGN_BIT && ku[CRL_SIGN_BIT];
        } else  {
            hasKeyCertSign = false;
            hasCrlSign = false;
        }

        // Rough classification based on key purposes (tables 1–4).
        profileType = classifyProfile(diceKeyPurposes, isCa, hasKeyCertSign);

        return new DiceCertificateInfo(profileType, diceKeyPurposes, isCa, hasKeyCertSign, hasCrlSign);
    }

    /**
     * Classifies a DICE certificate profile based on key purposes and related attributes.
     * @param keyPurposes the key purposes to classify
     * @param isCa true if a CA certificate
     * @param hasKeyCertSign true if cert contains keyCertSign
     * @return an output DICE profile type
     */
    private static DiceProfileType classifyProfile(final Set<DiceKeyPurpose> keyPurposes,
                                        final boolean isCa, final boolean hasKeyCertSign) {
        boolean hasIdentityInit = keyPurposes.contains(DiceKeyPurpose.IDENTITY_INIT);
        boolean hasIdentityLoc = keyPurposes.contains(DiceKeyPurpose.IDENTITY_LOC);
        boolean hasAttestInit = keyPurposes.contains(DiceKeyPurpose.ATTEST_INIT);
        boolean hasAttestLoc = keyPurposes.contains(DiceKeyPurpose.ATTEST_LOC);
        boolean hasEca = keyPurposes.contains(DiceKeyPurpose.ECA);

        DiceProfileType profileType;

        // ECA certificate profile.
        if (hasEca && isCa && hasKeyCertSign) {
            profileType = DiceProfileType.ECA;
        // Attestation certificate profile (5.1.6.4).
        } else if (hasAttestInit || hasAttestLoc) {
            profileType = DiceProfileType.ATTESTATION;
        // Profiles per 5.1.6.
        } else if (hasIdentityInit) {
            profileType = DiceProfileType.IDevID;
        } else if (hasIdentityLoc) {
            profileType = DiceProfileType.LDevID;
        } else {
            profileType = DiceProfileType.UNKNOWN;
        }

        return profileType;
    }

    /**
     * Helper method to extract DICE key purposes from a given list of OIDs.
     * @param ekuOids the input list of OIDs
     * @return a {@link Set} containing the corresponding key purposes
     */
    private static Set<DiceKeyPurpose> extractKeyPurposes(final List<String> ekuOids) {
        Set<DiceKeyPurpose> diceKeyPurposes = EnumSet.noneOf(DiceKeyPurpose.class);

        for (String oid : ekuOids) {
            DiceKeyPurpose kp = DiceKeyPurpose.fromOid(oid);

            if (kp != DiceKeyPurpose.OTHER) {
                diceKeyPurposes.add(kp);
            }
        }

        return diceKeyPurposes;
    }
}
