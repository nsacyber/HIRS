package hirs.attestationca.persist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The <code>Appraiser</code> class represents an appraiser that can appraise a <code>Report</code>.
 * <code>Appraiser</code>s are invoked to validate the integrity of client's platform. An
 * <code>Appraiser</code> does this by examining a <code>Report</code> sent from the client's
 * machine.
 * <p>
 * Supported <code>Report</code> types are kept track of in three ways: <ul> <li>The type of report
 * received for appraisal is getAppraiseReportType() (e.g. the <code>DeviceInfoAppraiser</code>
 * takes in a <code>DeviceInfoReport</code> and the <code>TPMAppraiser</code> takes in an
 * <code>IntegrityReport</code>)</li> <li>The type requested in getReportRequest is
 * getRequestReportType(). This tends to be the specific report type for that type of appraiser
 * (e.g. the <code>IMAAppraiser</code> requests an <code>IMAReport</code> and the
 * <code>TPMAppraiser</code> requests a <code>TPMReport</code>)</li> <li>The set of types this
 * appraiser relies on extracting from the top-level report is getRequiredReportTypes() (e.g. if the
 * top-level report is <code>IntegrityReport</code> then the <code>IMAAppraiser</code> needs to
 * extract both a <code>DeviceInfoReport</code> and a <code>IMAReport</code> from the
 * <code>IntegrityReport</code>)</li> </ul>
 */
@Entity
@Table(name = "Appraiser")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@EqualsAndHashCode(callSuper = false)
public class Appraiser {
    /**
     * Name set for every instance of <code>TPMAppraiser</code>.
     */
    public static final String TPM_NAME = "TPM Appraiser";
    /**
     * Name set for every instance of <code>SupplyChainAppraiser</code>.
     */
    public static final String SC_NAME = "Supply Chain Appraiser";
    /**
     * Name set for every instance of <code>IMAAppraiser</code>.
     */
    public static final String IMA_NAME = "IMA Appraiser";
    /**
     * Name set for every instance of <code>HIRSAppraiser</code>.
     */
    public static final String HIRS_NAME = "HIRS Appraiser";
    /**
     * Name set for every instance of <code>DeviceInfoAppraiser</code>.
     */
    public static final String DI_NAME = "Device Info Appraiser";

    @Getter
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Id
    @Column(name = "Appraiser_ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Getter
    @Setter
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Creates a new <code>Appraiser</code> with the specified name. The name should be universally
     * unique as this is how other components will identify <code>Appraiser</code>s. Web portals,
     * for instance, could display a list of <code>Appraiser</code> names to display which
     * <code>Appraiser</code>s are available.
     * <p>
     * The name will be tested for uniqueness when it is added to a repository. It is not tested for
     * uniqueness in the class.
     *
     * @param name unique name
     */
    public Appraiser(final String name) {
        this.name = name;
    }
}
