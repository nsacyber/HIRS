package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

/**
 * Contains information about a DICE certificate.
 * @see <a href="https://trustedcomputinggroup.org/resource/dice-certificate-profiles/">TCG DICE Certificate
 *      Profiles specification</a>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class DiceCertificateInfo {
    /** The DICE profile type of the certificate. */
    private DiceProfileType profileType;
    /** The DICE key purposes of the certificate. */
    private Set<DiceKeyPurpose> diceKeyPurposes;
    /** The CA flag of the certificate. */
    private boolean isCa;
    /** The keyCertSign flag of the certificate. */
    @Getter(AccessLevel.NONE)
    private boolean hasKeyCertSign;
    /** The cRLSign flag of the certificate. */
    @Getter(AccessLevel.NONE)
    private boolean hasCrlSign;

    /**
     * Returns the keyCertSign flag of this certificate.
     * @return the keyCertSign boolean value
     */
    public boolean hasKeyCertSign() {
        return hasKeyCertSign;
    }
    /**
     * Returns the cRLSign flag of this certificate.
     * @return the cRLSign boolean value
     */
    public boolean hasCrlSign() {
        return hasCrlSign;
    }
}
