package integration.hirs.repository.spacewalk;

import hirs.persist.RepositoryManager;
import hirs.repository.RepositoryException;
import hirs.repository.RepositoryUpdateService;
import hirs.repository.spacewalk.Credentials;
import hirs.repository.spacewalk.SpacewalkChannel;
import hirs.repository.spacewalk.SpacewalkChannelRepository;
import hirs.repository.spacewalk.SpacewalkException;
import hirs.repository.spacewalk.SpacewalkPackage;
import hirs.repository.spacewalk.SpacewalkService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Console tester application that exercises the SpacewalkService test by connecting to a real
 * Spacewalk instance with username / password.
 */
public final class SpacewalkServiceConsoleTester {
    private static final Logger LOGGER = LogManager.getLogger(SpacewalkServiceConsoleTester.class);
    private static final int ARGUMENT_COUNT = 3;
    private static final int DEFAULT_DOWNLOAD_ATTEMPTS = 1;
    private static final long UPDATE_SERVICE_SLEEP_MS = 10000;

    private static RepositoryManager repositoryManager;

    // this class should never be instantiated
    private SpacewalkServiceConsoleTester() {
        // intentionally blank
    }

    /**
     * Main method of application.
     *
     * @param args
     *            command line args
     * @throws MalformedURLException
     *             if the URL is malformed
     * @throws SpacewalkException
     *             if there is an exception processing with Spacewalk.
     */
    public static void main(final String[] args) throws MalformedURLException, SpacewalkException {
        if (args.length != ARGUMENT_COUNT) {
            LOGGER.error("Requires exactly " + ARGUMENT_COUNT
                    + " arguments: username, password, spacewalkUrl.");
            LOGGER.error(
                    "e.g. SpacewalkServiceConsoleTester MyUserName MyPass https://spacewalk-host");
            return;
        }

        loadBeansFromSpringContext();

        Credentials auth = new Credentials(args[0], args[1]);

        URL url = new URL(args[2]);
        try {
            useSpacewalkRepository(auth, url);
        } catch (Exception ex) {
            LOGGER.error("error processing spacewalk", ex);
        }
        LOGGER.info("Exiting Spacewalk console tester");
    }

    private static void loadBeansFromSpringContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
//        context.register(null);
        context.refresh();

        // register a shutdown hook such that components are properly shutdown when JVM is closing
        context.registerShutdownHook();

        repositoryManager = context.getBean(RepositoryManager.class);
    }

    private static void useSpacewalkRepository(final Credentials auth, final URL url)
            throws SpacewalkException, IOException, RepositoryException, InterruptedException {
        List<SpacewalkChannel> channels = SpacewalkService.getChannels(auth, url);
        for (SpacewalkChannel channel : channels) {
            LOGGER.info("getting packages for channel: " + channel.getName());

            SpacewalkChannelRepository spaceRepository =
                    new SpacewalkChannelRepository("space-integ", url, channel.getLabel());
            spaceRepository.setCredentials(auth, false);
            repositoryManager.saveRepository(spaceRepository);
            Set<SpacewalkPackage> packages = spaceRepository.getPackages();

            LOGGER.info("Packages for channel: " + packages.size());

            RepositoryUpdateService updateService = RepositoryUpdateService.getInstance();
            LOGGER.info("Starting update job");
            updateService.startUpdateJob(spaceRepository,
                    DEFAULT_DOWNLOAD_ATTEMPTS, repositoryManager);
            while (updateService.getActiveJobs().size() > 0) {
                LOGGER.info("Waiting for update job completion...");
                Thread.sleep(UPDATE_SERVICE_SLEEP_MS);
            }
            LOGGER.info("Update job completed");
        }
    }
}
