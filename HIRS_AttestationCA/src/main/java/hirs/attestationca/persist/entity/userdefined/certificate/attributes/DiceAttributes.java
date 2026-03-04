package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import java.io.IOException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for parsing and analyzing DICE (Device Identifier Composition Engine) certificate attributes.
 * Provides methods to extract DICE-specific information from X.509 certificates and classify them according to
 * TCG DICE certificate profiles.
 */
public final class DiceAttributes {
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private DiceAttributes() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    /**
     * Key usage bit position for keyCertSign (bit 5).
     */
    private static final int KEY_CERT_SIGN_BIT = 5;
    /**
     * Key usage bit position for cRLSign (bit 6).
     */
    private static final int CRL_SIGN_BIT = 6;

    /**
     * OID for Initial Identity key purpose.
     */
    public static final String OID_TCG_DICE_KP_IDENTITY_INIT = "2.23.133.5.4.100.6";
    /**
     * OID for Local Identity key purpose.
     */
    public static final String OID_TCG_DICE_KP_IDENTITY_LOC = "2.23.133.5.4.100.7";
    /**
     * OID for Initial Attestation key purpose.
     */
    public static final String OID_TCG_DICE_KP_ATTEST_INIT = "2.23.133.5.4.100.8";
    /**
     * OID for Local Attestation key purpose.
     */
    public static final String OID_TCG_DICE_KP_ATTEST_LOC = "2.23.133.5.4.100.9";
    /**
     * OID for Initial Assertion key purpose.
     */
    public static final String OID_TCG_DICE_KP_ASSERT_INIT = "2.23.133.5.4.100.10";
    /**
     * OID for Local Assertion key purpose.
     */
    public static final String OID_TCG_DICE_KP_ASSERT_LOC = "2.23.133.5.4.100.11";
    /**
     * OID for ECA (Endorsement Certificate Authority) key purpose.
     */
    public static final String OID_TCG_DICE_KP_ECA = "2.23.133.5.4.100.12";

    /**
     * Create a mapping of DICE EKU OIDs to their corresponding key purposes.
     *
     * @return an unmodifiable map of DICE EKU OIDs to human-readable key purpose descriptions
     */
    public static Map<String, String> getExtendedKeyUsageMap() {
        Map<String, String> ekuMap = new HashMap<>();
        ekuMap.put(OID_TCG_DICE_KP_IDENTITY_INIT, "DICE Initial Identity");
        ekuMap.put(OID_TCG_DICE_KP_IDENTITY_LOC, "DICE Local Identity");
        ekuMap.put(OID_TCG_DICE_KP_ATTEST_INIT, "DICE Initial Attestation");
        ekuMap.put(OID_TCG_DICE_KP_ATTEST_LOC, "DICE Local Attestation");
        ekuMap.put(OID_TCG_DICE_KP_ASSERT_INIT, "DICE Initial Assertion");
        ekuMap.put(OID_TCG_DICE_KP_ASSERT_LOC, "DICE Local Assertion");
        ekuMap.put(OID_TCG_DICE_KP_ECA, "DICE Embedded Certificate Authority");
        return Collections.unmodifiableMap(ekuMap);
    }

    /**
     * Enumeration of DICE key purposes as defined in the TCG specification "DICE Certificate Profiles".
     */
    public enum DiceKeyPurpose {
        /** Initial Identity key purpose. */
        IDENTITY_INIT,
        /** Local Identity key purpose. */
        IDENTITY_LOC,
        /** Initial Attestation key purpose. */
        ATTEST_INIT,
        /** Local Attestation key purpose. */
        ATTEST_LOC,
        /** Initial Assertion key purpose. */
        ASSERT_INIT,
        /** Local Assertion key purpose. */
        ASSERT_LOC,
        /** ECA (Embedded Certificate Authority) key purpose. */
        ECA,
        /** Other key purposes not specifically defined. */
        OTHER
    }

    /**
     * Enumeration of DICE certificate profiles as defined in the TCG specification "DICE Certificate Profiles".
     */
    public enum DiceProfileType {
        /** IDevID (Initial Device Identifier) profile. */
        IDevID,
        /** LDevID (Locally Significant Device Identifier) profile. */
        LDevID,
        /** ECA (Embedded Certificate Authority) profile. */
        ECA,
        /** Attestation certificate profile. */
        ATTESTATION,
        /** Unknown or unclassified profile. */
        UNKNOWN
    }

    /**
     * Information about a DICE certificate.
     */
    public static class DiceCertInfo {
        private DiceProfileType profileType = DiceProfileType.UNKNOWN;
        private Set<DiceKeyPurpose> diceKeyPurposes = EnumSet.noneOf(DiceKeyPurpose.class);
        private boolean isCa;
        private boolean hasKeyCertSign;
        private boolean hasCrlSign;

        /** Gets the DICE profile type of the certificate.
         * @return the DICE profile type, or UNKNOWN if it cannot be classified
         */
        public DiceProfileType getProfileType() {
            return profileType;
        }

        /** Sets the DICE profile type of the certificate.
         * @param profileType the DICE profile type to set for the certificate
         */
        public void setProfileType(final DiceProfileType profileType) {
            this.profileType = profileType;
        }

        /** Gets the DICE key purposes of the certificate.
         * @return a set of DICE key purposes associated with the certificate, or an empty set if none are present
         */
        public Set<DiceKeyPurpose> getDiceKeyPurposes() {
            return diceKeyPurposes;
        }

        /** Sets the DICE key purposes of the certificate.
         * @param diceKeyPurposes the set of DICE key purposes to associate with the certificate
         */
        public void setDiceKeyPurposes(final Set<DiceKeyPurpose> diceKeyPurposes) {
            this.diceKeyPurposes = diceKeyPurposes;
        }

        /** Gets the CA flag of the certificate.
         * @return true if the certificate is a CA certificate (basic conssints CA: true), false otherwise
         */
        public boolean isCa() {
            return isCa;
        }

        /** Sets the CA flag of the certificate.
         * @param ca the value to set for the CA flag (true if the certificate is a CA certificate, false otherwise)
         */
        public void setCa(final boolean ca) {
            isCa = ca;
        }

        /** Gets the keyCertSign flag of the certificate.
         * @return true if the certificate has the keyCertSign key usage, false otherwise
         */
        public boolean isHasKeyCertSign() {
            return hasKeyCertSign;
        }

        /** Sets the keyCertSign flag of the certificate.
         * @param hasKeyCertSign the value to set for the keyCertSign flag
         */
        public void setHasKeyCertSign(final boolean hasKeyCertSign) {
            this.hasKeyCertSign = hasKeyCertSign;
        }

        /** Gets the CRL Sign flag of the certificate.
         * @return true if the certificate has the CRL Sign key usage, false otherwise
         */
        public boolean isHasCrlSign() {
            return hasCrlSign;
        }

        /** Sets the CRL Sign flag of the certificate.
         * @param hasCrlSign the value to set for the CRL Sign flag
         */
        public void setHasCrlSign(final boolean hasCrlSign) {
            this.hasCrlSign = hasCrlSign;
        }
    }

    /**
     * Parses a DICE certificate and extracts relevant attributes.
     *
     * @param cert the X.509 certificate to parse
     * @return a DiceCertInfo object containing the extracted attributes
     * @throws IOException if certificate parsing fails
     */
    public static DiceCertInfo parseDiceCertificate(final X509Certificate cert) throws IOException {
        DiceCertInfo info = new DiceCertInfo();

        if (cert == null) {
            throw new IOException("Certificate must be an X.509 certificate");
        }

        // Basic constraints and key usage.
        int bc = cert.getBasicConstraints();
        info.isCa = (bc >= 0);

        boolean[] ku = cert.getKeyUsage();

        if (ku != null && ku.length > 0) {
            // keyCertSign is bit 5, cRLSign is bit 6 (0‑based index).
            info.hasKeyCertSign = ku.length > KEY_CERT_SIGN_BIT && Boolean.TRUE.equals(ku[KEY_CERT_SIGN_BIT]);
            info.hasCrlSign = ku.length > CRL_SIGN_BIT && Boolean.TRUE.equals(ku[CRL_SIGN_BIT]);
        }

        // Extended Key Usage: map DICE OIDs.
        List<String> ekuOids;

        try {
            ekuOids = cert.getExtendedKeyUsage();

        } catch (CertificateParsingException e) {
            ekuOids = Collections.emptyList();
        }

        if (ekuOids != null) {
            for (String oid : ekuOids) {
                DiceKeyPurpose kp = toDiceKeyPurpose(oid);

                if (kp != DiceKeyPurpose.OTHER) {
                    info.diceKeyPurposes.add(kp);
                }
            }
        } else {
            ekuOids = Collections.emptyList();
        }

        // Rough classification based on EKU content (tables 1–4).
        classifyProfile(info, ekuOids);

        return info;
    }

    /**
     * Maps a DICE OID to its corresponding key purpose.
     *
     * @param oid the OID to map
     * @return the corresponding DiceKeyPurpose, or OTHER if not recognized
     */
    private static DiceKeyPurpose toDiceKeyPurpose(final String oid) {
        if (OID_TCG_DICE_KP_IDENTITY_INIT.equals(oid)) {
            return DiceKeyPurpose.IDENTITY_INIT;
        }

        if (OID_TCG_DICE_KP_IDENTITY_LOC.equals(oid)) {
            return DiceKeyPurpose.IDENTITY_LOC;
        }

        if (OID_TCG_DICE_KP_ATTEST_INIT.equals(oid)) {
            return DiceKeyPurpose.ATTEST_INIT;
        }

        if (OID_TCG_DICE_KP_ATTEST_LOC.equals(oid)) {
            return DiceKeyPurpose.ATTEST_LOC;
        }

        if (OID_TCG_DICE_KP_ASSERT_INIT.equals(oid)) {
            return DiceKeyPurpose.ASSERT_INIT;
        }

        if (OID_TCG_DICE_KP_ASSERT_LOC.equals(oid)) {
            return DiceKeyPurpose.ASSERT_LOC;
        }

        if (OID_TCG_DICE_KP_ECA.equals(oid)) {
            return DiceKeyPurpose.ECA;
        }

        return DiceKeyPurpose.OTHER;
    }

    /**
     * Classifies a certificate profile based on its EKU content.
     *
     * @param info the DiceCertInfo object to classify
     * @param ekuOids the list of EKU OIDs
     */
    private static void classifyProfile(final DiceCertInfo info, final List<String> ekuOids) {
        boolean hasIdentityInit = ekuOids.contains(OID_TCG_DICE_KP_IDENTITY_INIT);
        boolean hasIdentityLoc = ekuOids.contains(OID_TCG_DICE_KP_IDENTITY_LOC);
        boolean hasAttestInit = ekuOids.contains(OID_TCG_DICE_KP_ATTEST_INIT);
        boolean hasAttestLoc = ekuOids.contains(OID_TCG_DICE_KP_ATTEST_LOC);
        boolean hasEca = ekuOids.contains(OID_TCG_DICE_KP_ECA);

        // Profiles per 5.1.6.
        if (hasIdentityInit) {
            info.profileType = DiceProfileType.IDevID;
        } else if (hasIdentityLoc) {
            info.profileType = DiceProfileType.LDevID;
        }

        if (hasEca && info.isCa) {
            // ECA certificate profile.
            info.profileType = DiceProfileType.ECA;
        }

        if (hasAttestInit || hasAttestLoc) {
            // Attestation certificate profile (5.1.6.4).
            info.profileType = DiceProfileType.ATTESTATION;
        }
    }
}
