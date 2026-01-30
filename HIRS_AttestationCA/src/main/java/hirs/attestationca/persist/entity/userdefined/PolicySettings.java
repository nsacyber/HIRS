package hirs.attestationca.persist.entity.userdefined;

import hirs.attestationca.persist.entity.UserDefinedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Class represents Supply Chain policy. Supply Chain Policy identifies the methods in
 * SupplyChainValidator that should be used in order to validate a supply chain.
 * By default, the policy does not enable any validations.
 */
@Table(name = "PolicySettings")
@Getter
@Setter
@Entity
@ToString(callSuper = true)
public class PolicySettings extends UserDefinedEntity {
    /**
     * Name of the default Supply Chain Policy.
     */
    public static final String DEFAULT_POLICY = "Default Supply Chain Policy";
    /**
     * Number of days in 10 years.
     */
    public static final int TEN_YEARS_IN_DAYS = 3651;

    /**
     * Number of days in 1 year.
     */
    public static final int A_YEAR_IN_DAYS = 365;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean ecValidationEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean pcValidationEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean pcAttributeValidationEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean ignoreRevisionEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean firmwareValidationEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean expiredCertificateValidationEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean issueAttestationCertificateEnabled = true;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean issueDevIdCertificateEnabled = true;

    @Column(nullable = false)
    private int validityDays = TEN_YEARS_IN_DAYS;

    @Column(nullable = false)
    private int devIdValidityDays = TEN_YEARS_IN_DAYS;

    @Column(nullable = false)
    private int reissueThreshold = A_YEAR_IN_DAYS;

    @Column(nullable = false)
    private int devIdReissueThreshold = A_YEAR_IN_DAYS;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean generateAttestationCertificateOnExpiration = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean generateDevIdCertificateOnExpiration = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean ignoreImaEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean ignoretBootEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean ignorePcieVpdEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean ignoreGptEnabled = true;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean ignoreOsEvtEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean saveProtobufToLogOnFailedValEnabled = true;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean saveProtobufToLogAlwaysEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean saveProtobufToLogNeverEnabled = false;

    /**
     * Default constructor necessary for Hibernate.
     */
    protected PolicySettings() {
        super();
    }

    /**
     * Constructor used to initialize PolicySettings object.
     *
     * @param name A name used to uniquely identify and reference the Supply Chain policy.
     */
    public PolicySettings(final String name) {
        super(name);
    }

    /**
     * Constructor used to initialize PolicySettings object.
     *
     * @param name        A name used to uniquely identify and reference the supply chain policy.
     * @param description Optional description of the policy that can be added by the user
     */
    public PolicySettings(final String name, final String description) {
        super(name, description);
    }
}
