package hirs.attestationca.persist.entity.userdefined.certificate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One check performed on component evidence, such as "the evidence contains the ACA's nonce".
 */
@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComponentEvidenceCheck {
    private static final int MAX_DETAIL_LENGTH = 2048;

    private String name;

    private boolean passed;

    @Column(length = MAX_DETAIL_LENGTH)
    private String detail;
}
