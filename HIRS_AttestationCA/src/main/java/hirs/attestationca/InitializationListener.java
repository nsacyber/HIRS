package hirs.attestationca;

import hirs.attestationca.configuration.PersistenceConfiguration;
import hirs.attestationca.persist.DBAppraiserManager;
import hirs.attestationca.persist.DBDeviceGroupManager;
import hirs.attestationca.persist.DBPolicyManager;
import hirs.utils.HIRSProfiles;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Simply holds a contextInitialized method which will be called when the web app starts.
 */
public class InitializationListener extends AbstractAnnotationConfigDispatcherServletInitializer
        implements ServletContextListener {
    @Override
    public void contextInitialized(final ServletContextEvent event) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().addActiveProfile(HIRSProfiles.SERVER);

        // register the database configuration and refresh the context
        context.register(AttestationCertificateAuthorityConfiguration.class);
        context.refresh();

        // obtain reference to hibernate session factory
        SessionFactory sessionFactory = context.getBean(LocalSessionFactoryBean.class).getObject();
        AcaDbInit.insertDefaultEntries(
                new DBAppraiserManager(sessionFactory),
                new DBDeviceGroupManager(sessionFactory),
                new DBPolicyManager(sessionFactory)
        );
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {

    }

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[] {
                AttestationCertificateAuthorityConfiguration.class
        };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[] {
                PersistenceConfiguration.class,
        };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] {
                "/"
        };
    }
}
