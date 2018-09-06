package hirs.persist;

import hirs.data.persist.SupplyChainValidationSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;


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
     * Creates a {@link AlertManager} ready to use.
     *
     * @return {@link AlertManager}
     */
    @Bean
    public AlertManager alertManager() {
        DBAlertManager manager = new DBAlertManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link BaselineManager} ready to use.
     *
     * @return {@link BaselineManager}
     */
    @Bean
    public BaselineManager baselineManager() {
        DBBaselineManager manager = new DBBaselineManager(sessionFactory.getObject());
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
     * Creates a {@link DeviceStateManager} ready to use.
     *
     * @return {@link DeviceStateManager}
     */
    @Bean(name = "general_db_man_bean")
    public DeviceStateManager generalDeviceStateManager() {
        DBDeviceStateManager manager = new DBDeviceStateManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }
    /**
     * Creates a {@link IMADeviceStateManager} ready to use.
     *
     * @return {@link IMADeviceStateManager}
     */
    @Bean
    public IMADeviceStateManager imaDeviceStateManager() {
        DBIMADeviceStateManager manager = new DBIMADeviceStateManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link TPMDeviceStateManager} ready to use.
     *
     * @return {@link TPMDeviceStateManager}
     */
    @Bean
    public TPMDeviceStateManager tpmDeviceStateManager() {
        DBTPMDeviceStateManager manager = new DBTPMDeviceStateManager(sessionFactory.getObject());
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
     * Creates a {@link ImaBaselineRecordManager} ready to use.
     *
     * @return {@link ImaBaselineRecordManager}
     */
    @Bean
    public ImaBaselineRecordManager imaBaselineRecordManager() {
        DbImaBaselineRecordManager manager =
                new DbImaBaselineRecordManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link ImaBlacklistBaselineRecordManager} ready to use.
     *
     * @return {@link ImaBlacklistBaselineRecordManager}
     */
    @Bean
    public ImaBlacklistBaselineRecordManager imaBlacklistBaselineRecordManager() {
        DbImaBlacklistBaselineRecordManager manager =
                new DbImaBlacklistBaselineRecordManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link AlertMonitorManager} ready to use.
     *
     * @return {@link AlertMonitorManager}
     */
    @Bean
    public AlertMonitorManager alertMonitorManager() {
        DBAlertMonitorManager manager = new DBAlertMonitorManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link AlertServiceConfigManager} ready to use.
     *
     * @return {@link AlertServiceConfigManager}
     */
    @Bean
    public AlertServiceConfigManager alertServiceConfigManager() {
        DBAlertServiceManager manager = new DBAlertServiceManager(sessionFactory.getObject());
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link DeviceHealthManager} ready to use.
     * @return {@link DeviceHealthManager}
     */
    @Bean
    public DeviceHealthManager deviceHealthManager() {
        return new DeviceHealthManagerImpl();
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
        DBManager<SupplyChainValidationSummary> manager = new DBManager<>(
                SupplyChainValidationSummary.class,
                sessionFactory.getObject()
        );
        setDbManagerRetrySettings(manager);
        return manager;
    }

    /**
     * Creates a {@link DBManager} for TPM2ProvisionerState persistence, ready for use.
     *
     * @return {@link DBManager} for TPM2ProvisionerState
     */
    @Bean
    public DBManager<TPM2ProvisionerState> tpm2ProvisionerStateDBManager() {
        DBManager<TPM2ProvisionerState> manager = new DBManager<>(
                TPM2ProvisionerState.class,
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
