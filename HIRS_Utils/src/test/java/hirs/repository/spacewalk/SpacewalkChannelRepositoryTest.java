package hirs.repository.spacewalk;

import java.net.MalformedURLException;
import java.net.URL;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Unit tests for <code>SpacewalkChannelRepository</code>.
 *
 */
public class SpacewalkChannelRepositoryTest {

    private static final String NAME = "space-test-rep";
    private static final String CHANNEL_LABEL = "my-chan";
    private static final String TEST_USER_NAME = "ut-user";
    private static final String TEST_PASSWORD = "ut-pass";
    private URL baseUrl;

    /**
     * Initializes fields for the test.
     *
     * @throws MalformedURLException
     *             if the URL is malformed
     */
    @BeforeClass
    public final void setup() throws MalformedURLException {
        baseUrl = new URL("http://unit.test.biz");
    }

    /**
     * Verifies exception thrown with a null baseURL.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void exceptionWithNullBaseUrl() {
        new SpacewalkChannelRepository(NAME, null, CHANNEL_LABEL);
    }

    /**
     * Verifies exception thrown with a null channel label.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void exceptionWithNullChannelLabel() {
        new SpacewalkChannelRepository(NAME, baseUrl, null);
    }

    /**
     * Verifies exception is thrown with null authentication.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void exceptionWithNullAuthentication() {
        SpacewalkChannelRepository repo =
                new SpacewalkChannelRepository(NAME, baseUrl, CHANNEL_LABEL);
        repo.setCredentials(null, true);
    }

    /**
     * Verifies exception is thrown with a null user name.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void exceptionWithNullUserName() {
        SpacewalkChannelRepository repo =
                new SpacewalkChannelRepository(NAME, baseUrl, CHANNEL_LABEL);
        Credentials auth = new Credentials(null, TEST_PASSWORD);
        repo.setCredentials(auth, true);
    }

    /**
     * Verifies exception is thrown with an empty password. Note, a PasswordAuthentication can't be
     * constructed with a null char array for the password, but empty password can be tested here.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void exceptionWithEmptyPassword() {
        SpacewalkChannelRepository repo =
                new SpacewalkChannelRepository(NAME, baseUrl, CHANNEL_LABEL);
        Credentials auth = new Credentials(TEST_USER_NAME, "");
        repo.setCredentials(auth, true);
    }

    /**
     * Verifies the Credentials can be set.
     */
    @Test
    public void setCredentials() {
        SpacewalkChannelRepository repo =
                new SpacewalkChannelRepository(NAME, baseUrl, CHANNEL_LABEL);
        Credentials auth =
                new Credentials(TEST_USER_NAME, TEST_PASSWORD);
        repo.setCredentials(auth, true);

        Credentials retrievedAuth = repo.getCredentials();
        Assert.assertEquals(retrievedAuth, auth);
    }
}
