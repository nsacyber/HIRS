package hirs.attestationca;

import hirs.appraiser.Appraiser;
import hirs.appraiser.AppraiserPlugin;
import hirs.appraiser.AppraiserPluginManager;
import hirs.appraiser.DeviceInfoAppraiser;
import hirs.appraiser.IMAAppraiser;
import hirs.appraiser.TPMAppraiser;
import hirs.attestationca.configuration.PersistenceConfiguration;
import hirs.attestationca.service.AppraiserServiceImpl;
import hirs.attestationca.service.PolicyServiceImpl;
import hirs.data.persist.policy.HIRSPolicy;
import hirs.data.persist.policy.Policy;
import hirs.data.persist.policy.TPMPolicy;
import hirs.utils.HIRSProfiles;
import hirs.utils.SpringContextProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class initializes the system for appraisals. This stores the requisite items in the database
 * to ensure that an appraisal can happen. For example, the system requires that a set of
 * <code>Appraiser</code>s be defined in the database. This class will initialize the set of
 * <code>Appraiser</code>s.
 */
public final class SystemInit {

    private static final Logger LOGGER = LogManager.getLogger(SystemInit.class);
    private static final int ALL_MASK = 0xFFFFFF;
    private static final int NONE_MASK = 0x000000;

    private static final String TPM_POLICY_NAME = "Test TPM Policy";

    private SystemInit() {
        /* do nothing */
    }

    /**
     * Initializes the system by creating a new <code>IMAAppraiser</code> and storing it in the
     * database.
     * <p>
     * This method is currently available for command line use, but is not used within the project.
     *
     * @param args not used
     */
    @SuppressWarnings("checkstyle:methodlength")
    public static void main(final String[] args) {
        LOGGER.error("Seeding database with initial entries...");
        // construct application context
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().addActiveProfile(HIRSProfiles.SERVER);

        // create class path scanner for discovering appraiser plugins
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, false);
        scanner.addIncludeFilter(new AssignableTypeFilter(AppraiserPlugin.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(SpringContextProvider.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AppraiserPluginManager.class));

        // scan for appraiser plugins
        int registeredBeanCount = scanner.scan("hirs");
        System.out.println("Beans scanned " + registeredBeanCount);
        LOGGER.error("Beans scanned: " + registeredBeanCount);

        // register the database configuration and refresh the context
        context.register(PersistenceConfiguration.class);
        context.refresh();

        // obtain reference to hibernate Entity Manager
        EntityManagerFactory entityManagerFactory
                = context.getBean(LocalContainerEntityManagerFactoryBean.class).getObject();

        // initialize the managers for this initialization process
        AppraiserServiceImpl appraiserServiceImpl = new AppraiserServiceImpl(
                entityManagerFactory.createEntityManager());
        PolicyServiceImpl policyServiceImpl = new PolicyServiceImpl();


        LOGGER.info("Checking for DeviceInfo appraiser...");
        DeviceInfoAppraiser deviceInfoAppraiser = (DeviceInfoAppraiser)
                appraiserServiceImpl.getAppraiser(DeviceInfoAppraiser.NAME);
        if (deviceInfoAppraiser == null) {
            LOGGER.info("DeviceInfo appraiser not found; creating...");
            appraiserServiceImpl.saveAppraiser(new DeviceInfoAppraiser());
        } else {
            LOGGER.info("DeviceInfo appraiser found.");
        }

        LOGGER.error("Checking for TPM appraiser...");
        TPMAppraiser tpmApp = (TPMAppraiser) appraiserServiceImpl.getAppraiser(TPMAppraiser.NAME);
        if (tpmApp == null) {
            LOGGER.info("TPM appraiser not found; creating...");
            tpmApp = (TPMAppraiser) appraiserServiceImpl.saveAppraiser(new TPMAppraiser());
        } else {
            LOGGER.info("TPM appraiser found.");
        }

        // build up required appraisers set
        Set<Class<? extends Appraiser>> requiredAppraisers = new HashSet<>();
        requiredAppraisers.add(DeviceInfoAppraiser.class);
        requiredAppraisers.add(TPMAppraiser.class);
        requiredAppraisers.add(IMAAppraiser.class);

        // obtain plugins from the context
        Collection<AppraiserPlugin> appraiserPlugins =
                context.getBeansOfType(AppraiserPlugin.class).values();

        LOGGER.info("Total Appraiser Plugins: " + appraiserPlugins.size());
        System.out.println("Total Appraiser Plugins: " + appraiserPlugins.size());

        // merge the appraiser plugins with the hirs policy appraisers
        for (AppraiserPlugin appraiserPlugin : appraiserPlugins) {
            // add in appraiser plugin to required appraisers list
            requiredAppraisers.add(appraiserPlugin.getClass());

            LOGGER.info("Checking for plugin appraiser {}...", appraiserPlugin);
            Appraiser storedAppraiser = appraiserServiceImpl
                    .getAppraiser(appraiserPlugin.getName());
            if (storedAppraiser == null) {
                LOGGER.info("Saving plugin appraiser {}...", appraiserPlugin);
                storedAppraiser = appraiserServiceImpl.saveAppraiser(appraiserPlugin);
            } else {
                LOGGER.info("Found plugin appraiser {}.", appraiserPlugin);
            }

            Policy policy = appraiserPlugin.getDefaultPolicy();
            if (policy != null) {
                LOGGER.info("Saving plugin appraiser's default policy: {}", policy);
                policy = policyServiceImpl.savePolicy(policy);
                policyServiceImpl.setDefaultPolicy(storedAppraiser, policy);
            }
        }

        // create HIRS policy
        LOGGER.info("Checking for HIRS policy...");
        HIRSPolicy hirsPolicy = (HIRSPolicy) policyServiceImpl.getPolicyByName(
                HIRSPolicy.DEFAULT_HIRS_POLICY_NAME
        );
        if (hirsPolicy == null) {
            LOGGER.info(
                    "HIRS policy not found; saving with required appraisers: {}",
                    requiredAppraisers
            );
            hirsPolicy = new HIRSPolicy(HIRSPolicy.DEFAULT_HIRS_POLICY_NAME);
            hirsPolicy.setRequiredAppraisers(requiredAppraisers);

            // initialize the default policy
            policyServiceImpl.savePolicy(hirsPolicy);
        } else {
            LOGGER.info("HIRS policy found.");
        }

        // initiate the default tpm policy
        LOGGER.info("Checking for TPM policy...");
        TPMPolicy tpmPolicy = (TPMPolicy) policyServiceImpl.getPolicyByName(TPM_POLICY_NAME);
        if (tpmPolicy == null) {
            LOGGER.info("TPM policy not found, creating...");
            tpmPolicy = new TPMPolicy(TPM_POLICY_NAME);
            tpmPolicy.setAppraiseFullReport(true);
            tpmPolicy.setAppraisePcrMask(NONE_MASK);
            tpmPolicy.setDefaultPcrAppraisalValues();
            tpmPolicy.setReportPcrMask(ALL_MASK);
            tpmPolicy = (TPMPolicy) policyServiceImpl.savePolicy(tpmPolicy);
            policyServiceImpl.setDefaultPolicy(tpmApp, tpmPolicy);
        } else {
            LOGGER.info("TPM policy found.");
        }

        LOGGER.info("Complete.");
    }
}
