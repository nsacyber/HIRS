package hirs.attestationca.portal.persistence;

import hirs.data.persist.SupplyChainValidationSummary;
import hirs.persist.AppraiserManager;
import hirs.persist.CrudManager;
import hirs.persist.DBAppraiserManager;
import hirs.persist.DBCertificateManager;
import hirs.persist.DBReferenceManifestManager;
import hirs.persist.DBDeviceGroupManager;
import hirs.persist.DBDeviceManager;
import hirs.persist.DBManager;
import hirs.persist.DBPolicyManager;
import hirs.persist.DeviceGroupManager;
import hirs.persist.DeviceManager;
import hirs.persist.HibernateConfiguration;
import hirs.persist.PolicyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;

/**
 * Spring configuration class for persistence beans used by the Attestation CA Portal.
 */
@Configuration
@Import({ HibernateConfiguration.class })
public class PersistenceConfiguration {

    @Autowired
    private LocalSessionFactoryBean sessionFactory;


    /**
     * Creates a {@link PolicyManager} ready to use.
     *
     * @return {@link PolicyManager}
     */
    @Bean
    public PolicyManager policyManager() {
        return new DBPolicyManager(sessionFactory.getObject());
    }

    /**
     * Creates a {@link DeviceManager} ready to use.
     *
     * @return {@link DeviceManager}
     */
    @Bean
    public DeviceManager deviceManager() {
        return new DBDeviceManager(sessionFactory.getObject());
    }

    /**
     * Creates a {@link DBCertificateManager} ready to use.
     *
     * @return {@link DBCertificateManager}
     */
    @Bean
    public DBCertificateManager certificateManager() {
        return new DBCertificateManager(sessionFactory.getObject());
    }

    /**
     * Creates a {@link DBReferenceManifestManager} ready to use.
     *
     * @return {@link DBReferenceManifestManager}
     */
    @Bean
    public DBReferenceManifestManager referenceManifestManager() {
        return new DBReferenceManifestManager(sessionFactory.getObject());
    }

    /**
     * Creates a {@link AppraiserManager} ready to use.
     *
     * @return {@link AppraiserManager}
     */
    @Bean
    public AppraiserManager appraiserManager() {
        return new DBAppraiserManager(sessionFactory.getObject());
    }

    /**
     * Creates a {@link DeviceGroupManager} ready to use.
     *
     * @return {@link DeviceGroupManager}
     */
    @Bean
    public DeviceGroupManager deviceGroupManager() {
        return new DBDeviceGroupManager(sessionFactory.getObject());
    }

    /**
     * Creates a {@link DBManager} for SupplyChainValidationSummary persistence, ready for use.
     *
     * @return {@link DBManager}
     */
    @Bean
    public CrudManager<SupplyChainValidationSummary> supplyChainValidationSummaryManager() {
        return new DBManager<>(SupplyChainValidationSummary.class, sessionFactory.getObject());
    }
}
