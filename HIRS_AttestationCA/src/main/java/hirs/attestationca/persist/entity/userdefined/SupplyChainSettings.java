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
@Table(name = "SupplyChainSettings")
@Getter
@Setter
@Entity
@ToString(callSuper = true)
public class SupplyChainSettings extends UserDefinedEntity {
    /**
     * Name of the default Supply Chain Policy.
     */
    public static final String DEFAULT_POLICY = "Default Supply Chain Policy";
    /**
     * Number of days in 10 years.
     */
    public static final String TEN_YEARS = "3651";
    /**
     * Number of days in 1 year.
     */
    public static final String YEAR = "365";

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean ecValidationEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean pcValidationEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean pcAttributeValidationEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean firmwareValidationEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean utcValidationEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean expiredCertificateValidationEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean replaceEC = false;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean issueAttestationCertificate = true;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean issueDevIdCertificate = true;

    @Column(nullable = false)
    private String validityDays = TEN_YEARS;

    @Column(nullable = false)
    private String devIdValidityDays = TEN_YEARS;

    @Column(nullable = false)
    private String reissueThreshold = YEAR;

    @Column(nullable = false)
    private String devIdReissueThreshold = YEAR;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean generateOnExpiration = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean devIdExpirationFlag = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean ignoreImaEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean ignoretBootEnabled = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean linuxOs = false;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean ignoreGptEnabled = true;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean ignoreOsEvtEnabled = false;

    /**
     * Default constructor necessary for Hibernate.
     */
    protected SupplyChainSettings() {
        super();
    }

    /**
     * Constructor used to initialize SupplyChainSettings object.
     *
     * @param name
     *        A name used to uniquely identify and reference the Supply Chain policy.
     */
    public SupplyChainSettings(final String name) {
        super(name);
    }

    /**
     * Constructor used to initialize SupplyChainSettings object.
     *
     * @param name
     *        A name used to uniquely identify and reference the supply chain policy.
     * @param description
     *        Optional description of the policy that can be added by the user
     */
    public SupplyChainSettings(final String name, final String description) {
        super(name, description);
    }
}
