package hirs.attestationca.configuration;

import hirs.attestationca.AttestationCertificateAuthorityConfiguration;
import hirs.attestationca.persist.DBCertificateManager;
import hirs.attestationca.persist.DBDeviceGroupManager;
import hirs.attestationca.persist.DBDeviceManager;
import hirs.attestationca.persist.DBManager;
import hirs.attestationca.persist.DBPolicyManager;
import hirs.attestationca.persist.DBPortalInfoManager;
import hirs.attestationca.persist.DBReferenceEventManager;
import hirs.attestationca.persist.DBReferenceManifestManager;
import hirs.attestationca.persist.DBReportManager;
import hirs.attestationca.persist.DBReportRequestStateManager;
import hirs.attestationca.persist.DBReportSummaryManager;
import hirs.data.persist.SupplyChainValidationSummary;
import hirs.persist.CertificateManager;
import hirs.persist.CrudManager;
import hirs.persist.DeviceGroupManager;
import hirs.persist.DeviceManager;
import hirs.persist.PolicyManager;
import hirs.persist.PortalInfoManager;
import hirs.persist.ReferenceEventManager;
import hirs.persist.ReferenceManifestManager;
import hirs.persist.ReportManager;
import hirs.persist.ReportRequestStateManager;
import hirs.persist.ReportSummaryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;

/**
 * Persistence Configuration for Spring enabled applications. Constructs a Hibernate SessionFactory
 * backed powered by a HikariCP connection pooled data source. Module-specific settings
 * need to be set in the persistence-extended.properties file on the classpath. If another module
 * such as the HIRS_Portal uses this class and doesn't have a persistence-extended.properties
 * file, the default persistence file will be used instead.
 */
@Configuration
@Import(AttestationCertificateAuthorityConfiguration.class)
public class PersistenceConfiguration {

    /**
     * The bean name to retrieve the default/general implementation of {@link }.
     */
    public static final String DEVICE_STATE_MANAGER_BEAN_NAME = "general_db_man_bean";

    @Autowired
    EntityManagerFactory entityManagerFactory;
    @PersistenceContext
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    @Autowired
    private long retryWaitTimeMilliseconds;

    @Autowired
    private int maxTransactionRetryAttempts;

    /**
     * Creates a {@link hirs.persist.PolicyManager} ready to use.
     *
     * @return {@link hirs.persist.PolicyManager}
     */
    @Bean
    public PolicyManager policyManager() {
        DBPolicyManager manager = new DBPolicyManager(entityManager);
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link hirs.persist.ReportManager} ready to use.
     *
     * @return {@link hirs.persist.ReportManager}
     */
    @Bean
    public ReportManager reportManager() {
        DBReportManager manager = new DBReportManager(entityManager);
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link hirs.persist.DeviceManager} ready to use.
     *
     * @return {@link hirs.persist.DeviceManager}
     */
    @Bean
    public DeviceManager deviceManager() {
        DBDeviceManager manager = new DBDeviceManager(entityManager);
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link hirs.persist.ReportSummaryManager} ready to use.
     *
     * @return {@link hirs.persist.ReportSummaryManager}
     */
    @Bean
    public ReportSummaryManager reportSummaryManager() {
        DBReportSummaryManager manager = new DBReportSummaryManager(entityManager);
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link hirs.persist.DeviceGroupManager} ready to use.
     *
     * @return {@link hirs.persist.DeviceGroupManager}
     */
    @Bean
    public DeviceGroupManager deviceGroupManager() {
        DBDeviceGroupManager manager = new DBDeviceGroupManager(entityManager);
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link hirs.persist.CertificateManager} ready to use.
     *
     * @return {@link hirs.persist.CertificateManager}
     */
    @Bean
    public CertificateManager certificateManager() {
        DBCertificateManager manager = new DBCertificateManager(entityManager);
        manager.setRetryTemplate(maxTransactionRetryAttempts, retryWaitTimeMilliseconds);
        return manager;
    }

    /**
     * Creates a {@link hirs.persist.ReferenceManifestManager} ready to use.
     *
     * @return {@link hirs.persist.ReferenceManifestManager}
     */
    @Bean
    public ReferenceManifestManager referenceManifestManager() {
        DBReferenceManifestManager manager
                = new DBReferenceManifestManager(entityManager);
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link hirs.persist.ReferenceEventManager} ready to use.
     *
     * @return {@link hirs.persist.ReferenceEventManager}
     */
    @Bean
    public ReferenceEventManager referenceEventManager() {
        DBReferenceEventManager manager
                = new DBReferenceEventManager(entityManager);
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link hirs.persist.ReportRequestStateManager} ready to use.
     *
     * @return {@link hirs.persist.ReportRequestStateManager}
     */
    @Bean
    public ReportRequestStateManager reportRequestStateManager() {
        DBReportRequestStateManager manager
                = new DBReportRequestStateManager(entityManager);
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link hirs.persist.PortalInfoManager} ready to use.
     *
     * @return {@link hirs.persist.PortalInfoManager}
     */
    @Bean
    public PortalInfoManager portalInfoManager() {
        DBPortalInfoManager manager = new DBPortalInfoManager(entityManager);
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link hirs.attestationca.persist.DBManager} for SupplyChainValidationSummary persistence, ready for use.
     *
     * @return {@link hirs.attestationca.persist.DBManager}
     */
    @Bean
    public CrudManager<SupplyChainValidationSummary> supplyChainValidationSummaryManager() {
        DBManager<SupplyChainValidationSummary> manager
                = new DBManager<SupplyChainValidationSummary>(
                SupplyChainValidationSummary.class,
                entityManager
        );
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Apply the spring-wired retry template settings to the db manager.
     * @param dbManager the manager to apply the retry settings to
     */
    private void setDbManagerRetrySettings(final DBManager dbManager) {
        dbManager.setRetryTemplate(maxTransactionRetryAttempts, retryWaitTimeMilliseconds);
    }
}
