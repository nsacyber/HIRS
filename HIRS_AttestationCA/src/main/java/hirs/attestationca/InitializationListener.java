package hirs.attestationca;

import org.hibernate.SessionFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import hirs.persist.DBAppraiserManager;
import hirs.persist.DBDeviceGroupManager;
import hirs.persist.DBPolicyManager;
import hirs.persist.PersistenceConfiguration;
import hirs.utils.HIRSProfiles;

/**
 * Simply holds a contextInitialized method which will be called when the web app starts.
 */
public class InitializationListener implements ServletContextListener {
    @Override
    public void contextInitialized(final ServletContextEvent event) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().addActiveProfile(HIRSProfiles.SERVER);

        // register the database configuration and refresh the context
        context.register(PersistenceConfiguration.class);
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
}
