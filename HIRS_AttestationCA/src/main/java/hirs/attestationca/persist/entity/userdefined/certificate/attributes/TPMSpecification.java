package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * A class to represent the TPM Specification in an Endorsement Credential as
 * defined by the TCG spec for TPM 1.2.
 * <p>
 * https://www.trustedcomputinggroup.org/wp-content/uploads/IWG-Credential_Profiles_V1_R0.pdf
 * <p>
 * Future iterations of this code may want to reference
 * www.trustedcomputinggroup.org/wp-content/uploads/Credential_Profile_EK_V2.0_R14_published.pdf
 * for specifications for TPM 2.0.
 */
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Embeddable
public class TPMSpecification implements Serializable {

    @Column
    private String family;

    @Column
    private BigInteger level;

    @Column
    private BigInteger revision;

    /**
     * Standard constructor.
     *
     * @param family   the specification family.
     * @param level    the specification level.
     * @param revision the specification revision.
     */
    public TPMSpecification(final String family, final BigInteger level,
                            final BigInteger revision) {
        this.family = family;
        this.level = level;
        this.revision = revision;
    }

    @Override
    public String toString() {
        return "TPMSpecification{"
                + "family='" + family + '\''
                + ", level=" + level
                + ", revision=" + revision
                + '}';
    }
}
