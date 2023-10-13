package hirs.attestationca.portal.page;

import hirs.attestationca.portal.PageConfiguration;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * A configuration class for testing Attestation CA Portal classes that require a database.
 * This apparently is needed to appease spring tests in the TestNG runner.
 */
//@Profile("test")
@Import({ PageConfiguration.class })
@TestConfiguration
//@ComponentScan(excludeFilters  = {@ComponentScan.Filter(
//                type = FilterType.ASSIGNABLE_TYPE,
//                classes = {PersistenceJPAConfig.class})})
//@ComponentScan(basePackages = {"hirs"},
//        excludeFilters = {@ComponentScan.Filter(
//                type = FilterType.ASPECTJ,
//                classes = {PersistenceJPAConfig.class})})
//@EnableAutoConfiguration(exclude= hibernate.properties)
//@TestPropertySource(value = "classpath:application-test.properties")
//@Configuration
@EnableJpaRepositories(basePackages = "hirs.attestationca.persist.entity.manager")
//@EnableTransactionManagement
public class PageTestConfiguration {

    /**
     * Test ACA cert.
     */
    public static final String FAKE_ROOT_CA = "/certificates/fakeCA.pem";

    @Autowired
    private Environment environment;

    /**
     * Gets a test x509 cert as the ACA cert for ACA portal tests.
     *
     * @return the {@link X509Certificate} of the ACA
     * @throws URISyntaxException if there's a syntax error on the path to the cert
     * @throws IOException exception reading the file
     */
    @Bean
    //@Bean("test_acaCertificate")
    //@Primary
    public X509Certificate acaCertificate() throws URISyntaxException, IOException {

        CertificateAuthorityCredential credential = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(getClass().getResource(FAKE_ROOT_CA).toURI()))
        );
        return credential.getX509Certificate();
    }


    /**
     * Overrides the {@link DataSource} with one that is configured against an in-memory HSQL DB.
     *
     * @return test data source
     */
    @Bean
    //@Bean("test_dataSource")
    //@Primary
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.HSQL).build();
    }

    /**
     * Configures a session factory bean that in turn configures the hibernate session factory.
     * Enables auto scanning of annotations such that entities do not need to be registered in a
     * hibernate configuration file.
     *
     * @return session factory
     */
    //@Bean("test_entityMangerFactory")
    @Bean
    //@Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

        final LocalContainerEntityManagerFactoryBean entityManagerBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerBean.setDataSource(dataSource());
        entityManagerBean.setPackagesToScan("hirs.attestationca.persist.entity");
        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        entityManagerBean.setJpaVendorAdapter(vendorAdapter);
        entityManagerBean.setJpaProperties(hibernateProperties());

        return entityManagerBean;
    }

    /**
     * Generates properties using configuration file that will be used to configure the session
     * factory.
     *
     * @return properties for hibernate session factory
     */
    final Properties hibernateProperties() {
        final Properties hibernateProperties = new Properties();
//        hibernateProperties.setProperty("hibernate.hbm2ddl.auto",
//                "create");
//        hibernateProperties.setProperty("hibernate.dialect",
//                "org.hibernate.dialect.HSQLDialect");
//        hibernateProperties.setProperty("hibernate.current_session_context_class",
//                "thread");


        System.out.println("\nXXXXXXXXXXXXXXXXXXXXXXX hibernate.hbm2ddl.auto: " + environment.getProperty("hibernate.hbm2ddl.auto"));
        System.out.println("\nXXXXXXXXXXXXXXXXXXXXXXX hibernate.dialect: " + environment.getProperty("hibernate.dialect"));
        System.out.println("\nXXXXXXXXXXXXXXXXXXXXXXX hibernate.connection.username: " + environment.getProperty("hibernate.connection.username"));
        System.out.println("\nXXXXXXXXXXXXXXXXXXXXXXX hibernate.connection.password: " + environment.getProperty("hibernate.connection.password"));

        hibernateProperties.setProperty("hibernate.hbm2ddl.auto",
                environment.getProperty("hibernate.hbm2ddl.auto"));
        hibernateProperties.setProperty("hibernate.dialect",
                environment.getProperty("hibernate.dialect"));
        hibernateProperties.setProperty("hibernate.current_session_context_class",
                "thread");
//
//        hibernateProperties.setProperty("hibernate.connection.username",
//                null);
//        hibernateProperties.setProperty("hibernate.connection.password",
//                null);

        System.out.println("\nXXXXXXXXXXXXXXXXXXXXXXX hibernate.hbm2ddl.auto: " + environment.getProperty("hibernate.hbm2ddl.auto"));
        System.out.println("\nXXXXXXXXXXXXXXXXXXXXXXX hibernate.dialect: " + environment.getProperty("hibernate.dialect"));
        //System.out.println("\nXXXXXXXXXXXXXXXXXXXXXXX hibernate.connection.username: " + environment.getProperty("hibernate.connection.username"));
        //System.out.println("\nXXXXXXXXXXXXXXXXXXXXXXX hibernate.connection.password: " + environment.getProperty("hibernate.connection.password"));

        return hibernateProperties;
    }


    /**
     * Generates JPA transaction manager.
     *
     * @return transaction manager
     */
    @Bean
    public PlatformTransactionManager transactionManager() {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }
}
