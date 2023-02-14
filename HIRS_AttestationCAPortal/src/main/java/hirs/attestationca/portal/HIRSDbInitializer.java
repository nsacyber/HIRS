package hirs.attestationca.portal;

import hirs.attestationca.portal.service.SettingsServiceImpl;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@WebListener
public class HIRSDbInitializer implements ServletContextListener {

    private static final Logger LOGGER = LogManager.getLogger(HIRSDbInitializer.class);

    @Autowired
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    @Autowired
    static SettingsServiceImpl settingsService = new SettingsServiceImpl();
//
//    public void contextInitialized(final ServletContextEvent servletContextEvent) {
////        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
//        context.getEnvironment().addActiveProfile("server");
//        context.register(PersistenceJPAConfig.class);
//        context.refresh();
//
//        // obtain reference to hibernate session factory
//        EntityManager entityManager = context.getBean(EntityManagerFactory.class)
//                .createEntityManager();
//        /**
//         * This fails if there is an entry already.
//         */
////        entityManager.getTransaction().begin();
////        entityManager.persist(context.getBean("default-settings"));
////        entityManager.getTransaction().commit();
//
//        insertDefaultEntries();
//    }
//
//    /**
//     * Insert the ACA's default entries into the DB.  This class is invoked after successful
//     * install of the HIRS_AttestationCA RPM.
//     *
//     */
//    public static synchronized void insertDefaultEntries() {
//        LOGGER.error("Ensuring default ACA database entries are present.");
//
//        // If the SupplyChainAppraiser exists, do not attempt to re-save the supply chain appraiser
//        // or SupplyChainSettings
//
//        // Create the SupplyChainAppraiser
//        LOGGER.error("Saving supply chain appraiser...");
//
//
//        // Create the SupplyChainSettings
//        LOGGER.error("Saving default supply chain policy...");
////        SupplyChainSettings supplyChainPolicy = new SupplyChainSettings(
////                SupplyChainSettings.DEFAULT_POLICY);
//        settingsService.saveSettings(new SupplyChainSettings("Default", "Settings are configured for no validation flags set."));
//
//        LOGGER.error("ACA database initialization complete.");
//    }
}
