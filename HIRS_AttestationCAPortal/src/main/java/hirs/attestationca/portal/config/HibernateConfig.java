package hirs.attestationca.portal.config;

import hirs.attestationca.configuration.AttestationCertificateAuthorityConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@PropertySources({
        @PropertySource(value = "classpath:defaults.properties"),
        @PropertySource(value = "classpath:persistence.properties"),

        // detects if file exists, if not, ignore errors
        @PropertySource(value = "file:/etc/hirs/aca/aca.properties",
                ignoreResourceNotFound = true)
})
@EnableTransactionManagement
@Import({ AttestationCertificateAuthorityConfiguration.class })
public class HibernateConfig {

    @Autowired
    private ApplicationContext context;
    @Autowired
    private Environment environment;

    @Bean
    public LocalSessionFactoryBean getSessionFactory() {
        LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
        factoryBean.setDataSource(dataSource());
        factoryBean.setHibernateProperties(hibernateProperties());
        factoryBean.setPackagesToScan("hirs");
        return factoryBean;
    }

    /**
     * Generates properties using configuration file that will be used to configure the session
     * factory.
     *
     * @return properties for hibernate session factory
     */
    @Bean
    public Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.hbm2ddl.auto",
                environment.getRequiredProperty("persistence.hibernate.ddl"));
        return properties;
    }

    /**
     * Configures the data source to be used by the hibernate session factory.
     *
     * @return configured data source
     */
    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(
                environment.getRequiredProperty("persistence.db.url"));
        dataSource.setUsername(
                environment.getRequiredProperty("persistence.db.username"));
        dataSource.setPassword(
                environment.getRequiredProperty("persistence.db.password"));
        dataSource.setDriverClassName(
                environment.getRequiredProperty("persistence.db.driverClass"));

        return dataSource;
    }

    @Bean
    public HibernateTransactionManager getTransactionManager() {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(getSessionFactory().getObject());
        return transactionManager;
    }
}
