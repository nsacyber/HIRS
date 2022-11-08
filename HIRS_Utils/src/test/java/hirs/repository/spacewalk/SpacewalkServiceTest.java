package hirs.repository.spacewalk;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Unit test for <code>SpacewalkService</code>.
 */
public class SpacewalkServiceTest {

    private static final String SPACEWALK_BASE_URL_STRING = "https://earth.moon.spacewalk.unittest";
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
     * Verifies exception when URL is non-https.
     * @throws SpacewalkException if an exception occurs during the test.
     * @throws MalformedURLException if the URL is malformed
     */
    @Test(
            expectedExceptions = SpacewalkException.class,
            expectedExceptionsMessageRegExp = ".*https.*")
    public void exceptionWithNonHttpsUrl() throws SpacewalkException, MalformedURLException {
        SpacewalkService.getChannels(authentication, new URL("http://some-box"));
    }

    /**
     * Verifies exception when querying for channels from an unreachable URL and that the correct
     * URL pointing to the RPC API is used.
     *
     * @throws SpacewalkException if a Spacewalk error occurs during the test.
     */
    @Test(
          expectedExceptions = SpacewalkException.class,
          expectedExceptionsMessageRegExp = ".*Spacewalk session for " + SPACEWALK_BASE_URL_STRING
                  + "/rpc/api.*")
    public void exceptionWithUnreachableSpacewalkChannels() throws SpacewalkException {
        SpacewalkService.getChannels(authentication, spacewalkBaseUrl);
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
