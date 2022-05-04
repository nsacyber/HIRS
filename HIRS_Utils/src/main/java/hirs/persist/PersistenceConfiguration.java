package hirs.persist;

import hirs.data.persist.SupplyChainValidationSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;


/**
 * Persistence Configuration for Spring enabled applications. Constructs a Hibernate SessionFactory
 * backed powered by a HikariCP connection pooled data source. Module-specific settings
 * need to be set in the persistence-extended.properties file on the classpath. If another module
 * such as the HIRS_Portal uses this class and doesn't have a persistence-extended.properties
 * file, the default persistence file will be used instead.
 */
@Configuration
@Import({ HibernateConfiguration.class })
public class PersistenceConfiguration {

    /**
     * The bean name to retrieve the default/general implementation of {@link }.
     */
    public static final String DEVICE_STATE_MANAGER_BEAN_NAME = "general_db_man_bean";

    @Autowired
    private LocalSessionFactoryBean sessionFactory;

    @Autowired
    private long retryWaitTimeMilliseconds;

    @Autowired
    private int maxTransactionRetryAttempts;

    /**
     * Creates a {@link AppraiserManager} ready to use.
     *
     * @return {@link AppraiserManager}
     */
    @Bean
    public AppraiserManager appraiserManager() {
        DBAppraiserManager manager = new DBAppraiserManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link PolicyManager} ready to use.
     *
     * @return {@link PolicyManager}
     */
    @Bean
    public PolicyManager policyManager() {
        DBPolicyManager manager = new DBPolicyManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link ReportManager} ready to use.
     *
     * @return {@link ReportManager}
     */
    @Bean
    public ReportManager reportManager() {
        DBReportManager manager = new DBReportManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link DeviceManager} ready to use.
     *
     * @return {@link DeviceManager}
     */
    @Bean
    public DeviceManager deviceManager() {
        DBDeviceManager manager = new DBDeviceManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link ReportSummaryManager} ready to use.
     *
     * @return {@link ReportSummaryManager}
     */
    @Bean
    public ReportSummaryManager reportSummaryManager() {
        DBReportSummaryManager manager = new DBReportSummaryManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link DeviceGroupManager} ready to use.
     *
     * @return {@link DeviceGroupManager}
     */
    @Bean
    public DeviceGroupManager deviceGroupManager() {
        DBDeviceGroupManager manager = new DBDeviceGroupManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link CertificateManager} ready to use.
     *
     * @return {@link CertificateManager}
     */
    @Bean
    public CertificateManager certificateManager() {
        DBCertificateManager manager = new DBCertificateManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link ReferenceManifestManager} ready to use.
     *
     * @return {@link ReferenceManifestManager}
     */
    @Bean
    public ReferenceManifestManager referenceManifestManager() {
        DBReferenceManifestManager manager
                = new DBReferenceManifestManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link ReferenceDigestManager} ready to use.
     *
     * @return {@link ReferenceDigestManager}
     */
    @Bean
    public ReferenceDigestManager referenceDigestManager() {
        DBReferenceDigestManager manager
                = new DBReferenceDigestManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link ReferenceEventManager} ready to use.
     *
     * @return {@link ReferenceEventManager}
     */
    @Bean
    public ReferenceEventManager referenceEventManager() {
        DBReferenceEventManager manager
                = new DBReferenceEventManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link ReportRequestStateManager} ready to use.
     *
     * @return {@link ReportRequestStateManager}
     */
    @Bean
    public ReportRequestStateManager reportRequestStateManager() {
        DBReportRequestStateManager manager
                = new DBReportRequestStateManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link RepositoryManager} ready to use.
     *
     * @return {@link RepositoryManager}
     */
    @Bean
    public RepositoryManager repositoryManager() {
        DBRepositoryManager manager = new DBRepositoryManager(sessionFactory.getObject());
        manager.setRetryTemplate(maxTransactionRetryAttempts, retryWaitTimeMilliseconds);
        return manager;
    }

    /**
     * Creates a {@link PortalInfoManager} ready to use.
     *
     * @return {@link PortalInfoManager}
     */
    @Bean
    public PortalInfoManager portalInfoManager() {
        DBPortalInfoManager manager = new DBPortalInfoManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link DBManager} for SupplyChainValidationSummary persistence, ready for use.
     *
     * @return {@link DBManager}
     */
    @Bean
    public CrudManager<SupplyChainValidationSummary> supplyChainValidationSummaryManager() {
        DBManager<SupplyChainValidationSummary> manager = new DBManager<SupplyChainValidationSummary>(
                SupplyChainValidationSummary.class,
                sessionFactory.getObject()
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
