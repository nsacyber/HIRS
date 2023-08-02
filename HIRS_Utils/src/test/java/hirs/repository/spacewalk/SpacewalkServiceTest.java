package hirs.repository.spacewalk;

import java.net.MalformedURLException;
import java.net.URL;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import hirs.repository.TestRepository;

/**
 * Unit test for <code>SpacewalkService</code>.
 */
public class SpacewalkServiceTest {

    private static final String SPACEWALK_BASE_URL_STRING = "https://earth.moon.spacewalk.unittest";
    private static final TestRepository REPOSITORY = new TestRepository("test");
    private static final String CHANNEL_LABEL = "my-chan";
    private URL spacewalkBaseUrl = null;
    private Credentials authentication = null;

    /**
     * Sets up test resources.
     *
     * @throws MalformedURLException if the URL is malformed
     */
    @BeforeClass
    public final void setup() throws MalformedURLException {
        spacewalkBaseUrl = new URL(SPACEWALK_BASE_URL_STRING);
        authentication = new Credentials("spaceman", "spacepass");
    }

    /**
     * Verifies exception when no authentication information is provided when querying channels.
     *
     * @throws SpacewalkException if a Spacewalk error occurs during the test.
     */
    @Test(
          expectedExceptions = SpacewalkException.class,
          expectedExceptionsMessageRegExp = ".*credentials.*")
    public void exceptionWithNullAuthenticationChannels() throws SpacewalkException {
        SpacewalkService.getChannels(null, spacewalkBaseUrl);
    }

    /**
     * Verifies exception when no URL is provided when querying channels.
     *
     * @throws SpacewalkException if a Spacewalk error occurs during the test.
     */
    @Test(
          expectedExceptions = SpacewalkException.class,
          expectedExceptionsMessageRegExp = ".*Spacewalk URL.*")
    public void exceptionWithNullUrlChannels() throws SpacewalkException {
        SpacewalkService.getChannels(authentication, null);
    }

    /**
     * Verifies exception when no authentication information is provided when querying channels.
     *
     * @throws SpacewalkException if a Spacewalk error occurs during the test.
     */
    @Test(
          expectedExceptions = SpacewalkException.class,
          expectedExceptionsMessageRegExp = ".*credentials.*")
    public void exceptionWithNullAuthenticationPackages() throws SpacewalkException {
        SpacewalkService.getPackages(null, spacewalkBaseUrl, CHANNEL_LABEL, REPOSITORY);
    }

    /**
     * Verifies exception when no URL is provided when querying channels.
     *
     * @throws SpacewalkException if a Spacewalk error occurs during the test.
     */
    @Test(
          expectedExceptions = SpacewalkException.class,
          expectedExceptionsMessageRegExp = ".*Spacewalk URL.*")
    public void exceptionWithNullUrlPackages() throws SpacewalkException {
        SpacewalkService.getPackages(authentication, null, CHANNEL_LABEL, REPOSITORY);
    }

    /**
     * Verifies exception when no URL is provided when querying channels.
     *
     * @throws SpacewalkException if a Spacewalk error occurs during the test.
     */
    @Test(
          expectedExceptions = SpacewalkException.class,
          expectedExceptionsMessageRegExp = ".*channelLabel.*")
    public void exceptionWithNullChannelLabelPackages() throws SpacewalkException {
        SpacewalkService.getPackages(authentication, spacewalkBaseUrl, null, REPOSITORY);
    }

    /**
     * Verifies exception when no URL is provided when querying channels.
     *
     * @throws SpacewalkException if a Spacewalk error occurs during the test.
     */
    @Test(
          expectedExceptions = SpacewalkException.class,
          expectedExceptionsMessageRegExp = ".*sourceRepository.*")
    public void exceptionWithNullRepositoryPackages() throws SpacewalkException {
        SpacewalkService.getPackages(authentication, spacewalkBaseUrl, CHANNEL_LABEL, null);
    }
}
