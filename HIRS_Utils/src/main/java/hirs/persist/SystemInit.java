package hirs.persist;

import hirs.appraiser.Appraiser;
import hirs.appraiser.AppraiserPlugin;
import hirs.appraiser.AppraiserPluginManager;
import hirs.appraiser.DeviceInfoAppraiser;
import hirs.appraiser.HIRSAppraiser;
import hirs.appraiser.IMAAppraiser;
import hirs.appraiser.TPMAppraiser;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.HIRSPolicy;
import hirs.data.persist.Policy;
import hirs.utils.HIRSProfiles;
import hirs.utils.SpringContextProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

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

    private static final String IMA_POLICY_NAME = "Test IMA Policy";
    private static final String TPM_POLICY_NAME = "Test TPM Policy";

    /**
     * Default constructor that does nothing.
     */
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
        LOGGER.info("Seeding database with initial entries...");
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
        LOGGER.info("Beans scanned: " + registeredBeanCount);

        // register the database configuration and refresh the context
        context.register(PersistenceConfiguration.class);
        context.refresh();

        // obtain reference to hibernate session factory
        SessionFactory sessionFactory = context.getBean(LocalSessionFactoryBean.class).getObject();

        // initialize the managers for this initialization process
        final DeviceGroupManager deviceGroupManager = new DBDeviceGroupManager(sessionFactory);
        final AppraiserManager appraiserManager = new DBAppraiserManager(sessionFactory);
        final PolicyManager policyManager = new DBPolicyManager(sessionFactory);

        // save the default group
        LOGGER.info("Checking for default device group...");
        if (deviceGroupManager.getDeviceGroup(DeviceGroup.DEFAULT_GROUP) == null) {
            LOGGER.info("Default device group not found; creating...");
            deviceGroupManager.saveDeviceGroup(
                    new DeviceGroup(DeviceGroup.DEFAULT_GROUP, "This is the default group")
            );
            LOGGER.info("Default device group saved.");
        } else {
            LOGGER.info("Default device group found.");
        }

        // initiate all the appraisers
        LOGGER.info("Checking for HIRS appraiser...");
        HIRSAppraiser hirsApp = (HIRSAppraiser) appraiserManager.getAppraiser(HIRSAppraiser.NAME);
        if (hirsApp == null) {
            LOGGER.info("HIRS appraiser not found; creating...");
             hirsApp = (HIRSAppraiser) appraiserManager.saveAppraiser(new HIRSAppraiser());
        } else {
            LOGGER.info("HIRS appraiser found.");
        }

        LOGGER.info("Checking for IMA appraiser...");
        IMAAppraiser imaApp = (IMAAppraiser) appraiserManager.getAppraiser(IMAAppraiser.NAME);
        if (imaApp == null) {
            LOGGER.info("IMA appraiser not found; creating...");
//            imaApp = (IMAAppraiser) appraiserManager.saveAppraiser(new IMAAppraiser());
        } else {
            LOGGER.info("IMA appraiser found.");
        }

        LOGGER.info("Checking for TPM appraiser...");
        TPMAppraiser tpmApp = (TPMAppraiser) appraiserManager.getAppraiser(TPMAppraiser.NAME);
        if (tpmApp == null) {
            LOGGER.info("TPM appraiser not found; creating...");
//            tpmApp = (TPMAppraiser) appraiserManager.saveAppraiser(new TPMAppraiser());
        } else {
            LOGGER.info("TPM appraiser found.");
        }

        LOGGER.info("Checking for DeviceInfo appraiser...");
        DeviceInfoAppraiser deviceInfoAppraiser = (DeviceInfoAppraiser)
                appraiserManager.getAppraiser(DeviceInfoAppraiser.NAME);
        if (deviceInfoAppraiser == null) {
            LOGGER.info("DeviceInfo appraiser not found; creating...");
            appraiserManager.saveAppraiser(new DeviceInfoAppraiser());
        } else {
            LOGGER.info("DeviceInfo appraiser found.");
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
            Appraiser storedAppraiser = appraiserManager.getAppraiser(appraiserPlugin.getName());
            if (storedAppraiser == null) {
                LOGGER.info("Saving plugin appraiser {}...", appraiserPlugin);
                storedAppraiser = appraiserManager.saveAppraiser(appraiserPlugin);
            } else {
                LOGGER.info("Found plugin appraiser {}.", appraiserPlugin);
            }

            Policy policy = appraiserPlugin.getDefaultPolicy();
            if (policy != null) {
                LOGGER.info("Saving plugin appraiser's default policy: {}", policy);
                policy = policyManager.savePolicy(policy);
                policyManager.setDefaultPolicy(storedAppraiser, policy);
            }
        }

        // create HIRS policy
        LOGGER.info("Checking for HIRS policy...");
        HIRSPolicy hirsPolicy = (HIRSPolicy) policyManager.getPolicy(
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
            policyManager.savePolicy(hirsPolicy);
            policyManager.setDefaultPolicy(hirsApp, hirsPolicy);
        } else {
            LOGGER.info("HIRS policy found.");
        }

        LOGGER.info("Complete.");
    }
}
