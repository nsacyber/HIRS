package hirs.attestationca;

import hirs.attestationca.service.AppraiserServiceImpl;
import hirs.attestationca.service.PolicyServiceImpl;
import hirs.utils.HIRSProfiles;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Simply holds a contextInitialized method which will be called when the web app starts.
 */
public class InitializationListener implements ServletContextListener {
    @Override
    public void contextInitialized(final ServletContextEvent event) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().addActiveProfile(HIRSProfiles.SERVER);

        // register the database configuration and refresh the context
        context.register(AttestationCertificateAuthorityConfiguration.class);
        context.refresh();

        // obtain reference to hibernate session factory
        EntityManager entityManager = context.getBean(EntityManagerFactory.class)
                .createEntityManager();
        AcaDbInit.insertDefaultEntries(new AppraiserServiceImpl(entityManager),
                new PolicyServiceImpl()
        );
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {

    }
}
