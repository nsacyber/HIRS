package hirs.attestationca.persist;

import org.springframework.context.annotation.Configuration;

/**
 * Persistence Configuration for Spring enabled applications. Constructs a Hibernate SessionFactory
 * backed powered by a HikariCP connection pooled data source. Module-specific settings
 * need to be set in the persistence-extended.properties file on the classpath. If another module
 * such as the HIRS_Portal uses this class and doesn't have a persistence-extended.properties
 * file, the default persistence file will be used instead.
 */
@Configuration
public class PersistenceConfiguration {

//    @Bean
//    public FilesStorageService filesStorageService() {
//        FilesStorageServiceImpl filesStorageService =  new FilesStorageServiceImpl(new StorageProperties());
//        filesStorageService.init();
//        return filesStorageService;
//    }

//    /**
//     * Creates a {@link CertificateServiceImpl} ready to use.
//     *
//     * @return {@link CertificateServiceImpl}
//     */
//    @Bean
//    public CertificateServiceImpl certificateServiceImpl() {
//        CertificateServiceImpl manager =  new CertificateServiceImpl();
//        setDbManagerRetrySettings(manager);
//        return manager;
//    }
//
//    /**
//     * Creates a {@link DeviceServiceImpl} ready to use.
//     *
//     * @return {@link DeviceServiceImpl}
//     */
//    @Bean
//    public DeviceServiceImpl deviceServiceImpl() {
//        DeviceServiceImpl manager = new DeviceServiceImpl();
//        setDbManagerRetrySettings(manager);
//        return manager;
//    }
//
//    /**
//     * Creates a {@link PolicyServiceImpl} ready to use.
//     *
//     * @return {@link PolicyServiceImpl}
//     */
//    @Bean
//    public PolicyServiceImpl policyServiceImpl() {
//        PolicyServiceImpl manager = new PolicyServiceImpl();
//        setDbManagerRetrySettings(manager);
//        return manager;
//    }
//
//    /**
//     * Creates a {@link ReferenceManifestServiceImpl} ready to use.
//     *
//     * @return {@link ReferenceManifestServiceImpl}
//     */
//    @Bean
//    public ReferenceManifestServiceImpl referenceManifestServiceImpl() {
//        ReferenceManifestServiceImpl manager = new ReferenceManifestServiceImpl();
//        setDbManagerRetrySettings(manager);
//        return manager;
//    }
//
//    /**
//     * Creates a {@link ReferenceDigestValueServiceImpl} ready to use.
//     *
//     * @return {@link ReferenceDigestValueServiceImpl}
//     */
//    @Bean
//    public ReferenceDigestValueServiceImpl referenceDigestValueServiceImpl() {
//        ReferenceDigestValueServiceImpl manager = new ReferenceDigestValueServiceImpl();
//        setDbManagerRetrySettings(manager);
//        return manager;
//    }
//
//    /**
//     * Apply the spring-wired retry template settings to the db manager.
//     * @param dbManager the manager to apply the retry settings to
//     */
//    private void setDbManagerRetrySettings(final DefaultDbService dbManager) {
//        dbManager.setRetryTemplate();
//    }
}
