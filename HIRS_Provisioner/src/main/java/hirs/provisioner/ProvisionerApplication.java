package hirs.provisioner;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import hirs.provisioner.configuration.ProvisionerConfiguration;
import hirs.utils.LogConfigurationUtil;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Defines the application's entry point and the main configuration.
 */
public final class ProvisionerApplication {
    private static final String JAVA_PROP_TRUST_STORE = "javax.net.ssl.trustStore";
    private static final String JAVA_PROP_KEY_STORE = "javax.net.ssl.keyStore";
    private static final String JAVA_PROP_KEY_STORE_PASSWORD = "javax.net.ssl.keyStorePassword";
    private static final List<String> SYSTEM_PROPERTIES_TO_SET = Arrays.asList(
            JAVA_PROP_KEY_STORE, JAVA_PROP_TRUST_STORE, JAVA_PROP_KEY_STORE_PASSWORD
    );
    private static final Logger LOGGER = LogManager.getLogger(ProvisionerApplication.class);
    private static final int REQUIRED_ARG_COUNT = 1;
    private static final String PROP_FILE_PATH =
        "/etc/hirs/provisioner/provisioner.properties";

    /**
     * Hidden default constructor.
     */
    private ProvisionerApplication() {
    }

    /**
     * Application entry point. Uses Spring Boot to bootstrap the application.
     *
     * @param args not used
     */
    public static void main(final String[] args) {
        try {
            LogConfigurationUtil.applyConfiguration();
        } catch (IOException e) {
            LOGGER.error("Error configuring provisioner logger", e);
        }

        if (ArrayUtils.isEmpty(args) || args.length != REQUIRED_ARG_COUNT) {
            LOGGER.error("Provisioner requires exactly " + REQUIRED_ARG_COUNT
                    + " args (Host Name)");
            System.exit(-1);
        }

        CommandLineArguments.setHostName(args[0]);

        LOGGER.debug("Starting HIRS Provisioner command line application");

        try {
            setTrustStore();
        } catch (IOException ex) {
            LOGGER.error("Error provisioning client", ex);
            System.exit(-1);
        }

        // enable TLS 1.1 and 1.2
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.1");

        // initialize the context
        new AnnotationConfigApplicationContext(ProvisionerConfiguration.class);
    }

    /**
     * This method configures the JVM with the trustStore as read from
     * provisioner.properties.
     *
     * @throws IOException if provisioner.properties cannot be read
     */
    private static void setTrustStore() throws IOException {
        Properties runtimeJVMProperties = new Properties();

        try (InputStream propertiesFileInputStream = new FileInputStream(
                FileSystems.getDefault().getPath(PROP_FILE_PATH)
                        .toFile())) {
            runtimeJVMProperties.load(propertiesFileInputStream);
        }

        for (String property : SYSTEM_PROPERTIES_TO_SET) {
            String value = runtimeJVMProperties.getProperty(property);
            if (StringUtils.isBlank(value)) {
                throw new IllegalArgumentException(String.format(
                        "Cannot start HIRS Provisioner; please set a value for %s in %s",
                        property, PROP_FILE_PATH));
            }
            System.setProperty(property, value);
        }
    }
}
