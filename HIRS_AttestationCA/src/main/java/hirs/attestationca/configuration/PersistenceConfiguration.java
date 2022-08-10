package hirs.attestationca.configuration;

import hirs.attestationca.service.CertificateServiceImpl;
import hirs.attestationca.service.DbServiceImpl;
import hirs.attestationca.service.DeviceServiceImpl;
import hirs.attestationca.service.PolicyServiceImpl;
import hirs.attestationca.service.ReferenceDigestValueServiceImpl;
import hirs.attestationca.service.ReferenceManifestServiceImpl;
import hirs.persist.service.CertificateService;
import hirs.persist.service.DeviceService;
import hirs.persist.service.PolicyService;
import hirs.persist.service.ReferenceDigestValueService;
import hirs.persist.service.ReferenceManifestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

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
@EnableJpaRepositories("hirs.attestationca.service")
public class PersistenceConfiguration {

    /**
     * The bean name to retrieve the default/general implementation of {@link }.
     */
    public static final String DEVICE_STATE_MANAGER_BEAN_NAME = "general_db_man_bean";

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @PersistenceContext
    private EntityManager entityManager = entityManagerFactory.createEntityManager();

    @Autowired
    private long retryWaitTimeMilliseconds;

    @Autowired
    private int maxTransactionRetryAttempts;

    /**
     * Creates a {@link hirs.persist.service.PolicyService} ready to use.
     *
     * @return {@link hirs.persist.service.PolicyService}
     */
    @Bean
    public PolicyService policyService() {
        PolicyServiceImpl serviceImpl = new PolicyServiceImpl(entityManager);
        setDbServiceRetrySettings(serviceImpl);
        return serviceImpl;
    }

    /**
     * Creates a {@link hirs.persist.service.DeviceService} ready to use.
     *
     * @return {@link hirs.persist.service.DeviceService}
     */
    @Bean
    public DeviceService deviceService() {
        DeviceServiceImpl serviceImpl = new DeviceServiceImpl(entityManager);
        setDbServiceRetrySettings(serviceImpl);
        return serviceImpl;
    }

    /**
     * Creates a {@link hirs.persist.service.CertificateService} ready to use.
     *
     * @return {@link hirs.persist.service.CertificateService}
     */
    @Bean
    public CertificateService certificateService() {
        CertificateServiceImpl serviceImpl = new CertificateServiceImpl(entityManager);
        setDbServiceRetrySettings(serviceImpl);
        return serviceImpl;
    }

    /**
     * Creates a {@link hirs.persist.service.ReferenceManifestService} ready to use.
     *
     * @return {@link hirs.persist.service.ReferenceManifestService}
     */
    @Bean
    public ReferenceManifestService referenceManifestService() {
        ReferenceManifestServiceImpl serviceImpl
                = new ReferenceManifestServiceImpl(entityManager);
        setDbServiceRetrySettings(serviceImpl);
        return serviceImpl;
    }

    /**
     * Creates a {@link hirs.persist.service.ReferenceDigestValueService} ready to use.
     *
     * @return {@link hirs.persist.service.ReferenceDigestValueService}
     */
    @Bean
    public ReferenceDigestValueService referenceEventService() {
        ReferenceDigestValueServiceImpl serviceImpl
                = new ReferenceDigestValueServiceImpl(entityManager);
        setDbServiceRetrySettings(serviceImpl);
        return serviceImpl;
    }
//
//    /**
//     * Creates a {@link hirs.attestationca.servicemanager.DBManager}
//     * for SupplyChainValidationSummary persistence, ready for use.
//     *
//     * @return {@link hirs.attestationca.servicemanager.DBManager}
//     */
//    @Bean
//    public DbServiceImpl<SupplyChainValidationSummary> supplyChainValidationSummaryManager() {
//        DbServiceImpl<SupplyChainValidationSummary> serviceImpl
//                = new DbServiceImpl<SupplyChainValidationSummary>(entityManager);
//        setDbServiceRetrySettings(serviceImpl);
//        return serviceImpl;
//    }

    /**
     * Apply the spring-wired retry template settings to the db manager.
     * @param dbServiceImpl the service to apply the retry settings to
     */
    private void setDbServiceRetrySettings(final DbServiceImpl dbServiceImpl) {
        dbServiceImpl.setRetryTemplate(maxTransactionRetryAttempts, retryWaitTimeMilliseconds);
    }
}
