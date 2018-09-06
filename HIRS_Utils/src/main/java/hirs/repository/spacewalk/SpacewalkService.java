package hirs.repository.spacewalk;

import hirs.repository.Repository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

/**
 * Class responsible with all direct interfacing with a Spacewalk server via the Spacewalk API 2.4.
 * Uses XMLRPC to authenticate and pull repository information from Spacewalk.
 *
 */
public final class SpacewalkService {

    private static final Logger LOGGER = LogManager.getLogger(SpacewalkService.class);

    private static final int CONNECTION_TIMEOUT_MS = 1000;
    private static final String RPC_API_PATH = "/rpc/api";

    /**
     * Private constructor to prevent instantiation.
     */
    private SpacewalkService() {

    }

    /**
     * Gets the set of channels for the specified Spacewalk instance.
     *
     * @param credentials
     *            the authentication information for Spacewalk.
     * @param spacewalkBaseUrl
     *            the base URL for Spacewalk.
     * @return the list of Spacewalk channels
     * @throws SpacewalkException
     *             if an exception occurs interfacing with Spacewalk.
     */
    @SuppressWarnings("unchecked")
    public static List<SpacewalkChannel> getChannels(final Credentials credentials,
            final URL spacewalkBaseUrl) throws SpacewalkException {

        validateParameters(credentials, spacewalkBaseUrl);
        URL queryUrl = getQueryUrl(spacewalkBaseUrl);
        Object sessionKey = getSessionKey(credentials, queryUrl);
        XmlRpcClient client = createXmlRpcClient(queryUrl);

        List<SpacewalkChannel> channelList = new ArrayList<>();
        try {
            Object[] rawChannels =
                    (Object[]) client.execute("channel.listAllChannels", new Object[] {sessionKey});

            for (Object channelObj : rawChannels) {
                Map<String, Object> channelMap = (Map<String, Object>) channelObj;

                SpacewalkChannel channel = new SpacewalkChannel(channelMap, spacewalkBaseUrl);
                channelList.add(channel);
            }
            LOGGER.info("Found " + rawChannels.length + " channels. Closing session");
            closeSession(sessionKey, queryUrl);
        } catch (XmlRpcException e) {
            String message = "Error getting list of spacewalk channels: " + e.getMessage();
            LOGGER.error(message, e);
            throw new SpacewalkException(message, e);
        } catch (IllegalArgumentException iae) {
            String message = "Error processing channel map returned from Spacewalk";
            LOGGER.error(message, iae);
            throw new SpacewalkException(message, iae);
        } finally {
            closeSession(sessionKey, queryUrl);
        }
        return channelList;
    }

    /**
     * Gets the Spacewalk packages for the specified channel.
     *
     * @param credentials
     *            the authentication information for Spacewalk.
     * @param spacewalkBaseUrl
     *            the base URL for Spacewalk.
     * @param channelLabel
     *            the Spacewalk channel label
     * @param sourceRepository
     *            the repository object that the packages belong to
     * @return the list of Spacewalk packages
     * @throws SpacewalkException
     *             if an exception occurs interfacing with Spacewalk.
     */
    @SuppressWarnings("unchecked")
    public static List<SpacewalkPackage> getPackages(final Credentials credentials,
            final URL spacewalkBaseUrl, final String channelLabel,
            final Repository sourceRepository)
            throws SpacewalkException {
        if (null == sourceRepository) {
            throw new SpacewalkException("must provide sourceRepository");
        }
        if (StringUtils.isEmpty(channelLabel)) {
            throw new SpacewalkException("must provide channelLabel");
        }

        validateParameters(credentials, spacewalkBaseUrl);
        URL queryUrl = getQueryUrl(spacewalkBaseUrl);
        Object sessionKey = getSessionKey(credentials, queryUrl);
        XmlRpcClient client = createXmlRpcClient(queryUrl);

        List<SpacewalkPackage> packageList = new ArrayList<>();

        Object[] rawPackages;
        try {
            rawPackages =
                    (Object[]) client.execute("channel.software.listAllPackages", new Object[] {
                            sessionKey, channelLabel});
            for (Object packagObj : rawPackages) {
                Map<String, Object> packageMap = (Map<String, Object>) packagObj;

                packageList.add(SpacewalkPackage
                        .buildSpacewalkPackage(packageMap, sourceRepository));
            }
        } catch (XmlRpcException e) {
            String message =
                    "Error getting list of spacewalk packages for channel : " + channelLabel + " - "
                            + e.getMessage();
            LOGGER.error(message, e);
            throw new SpacewalkException(message, e);
        } catch (IllegalArgumentException iae) {
            String message =
                    "Error processing channel map returned from Spacewalk for channel: "
                            + channelLabel;
            LOGGER.error(message, iae);
            throw new SpacewalkException(message, iae);
        } finally {
            closeSession(sessionKey, queryUrl);
        }

        return packageList;
    }

    /**
     * Gets a URL for downloading the specified Spacewalk package.
     *
     * @param credentials
     *            the authentication information for Spacewalk.
     * @param spacewalkBaseUrl
     *            the base URL for Spacewalk.
     * @param spacewalkPackage
     *            the package to download
     * @return a URL pointing to the package
     * @throws SpacewalkException
     *             if an exception occurs interfacing with Spacewalk.
     */
    public static URL getPackageDownloadUrl(final Credentials credentials,
            final URL spacewalkBaseUrl, final SpacewalkPackage spacewalkPackage)
            throws SpacewalkException {
        validateParameters(credentials, spacewalkBaseUrl);
        URL queryUrl = getQueryUrl(spacewalkBaseUrl);
        Object sessionKey = getSessionKey(credentials, queryUrl);
        XmlRpcClient client = createXmlRpcClient(queryUrl);

        LOGGER.debug("Getting download URL for package: " + spacewalkPackage.getRPMIdentifier());
        URL downloadUrl = null;
        Object rawUrl = null;
        try {
            rawUrl =
                    client.execute("packages.getPackageUrl", new Object[] {sessionKey,
                            spacewalkPackage.getSpacewalkPackageId()});
            downloadUrl = new URL(rawUrl.toString());
        } catch (XmlRpcException e) {
            String message = "Error getting spacewalk package download URL: " + e.getMessage();
            LOGGER.error(message, e);
            throw new SpacewalkException(message, e);
        } catch (MalformedURLException e) {
            String message = "Spacewalk returned an invalid download URL: " + rawUrl;
            LOGGER.error(message, e);
            throw new SpacewalkException(message, e);
        } finally {
            closeSession(sessionKey, queryUrl);
        }

        return downloadUrl;
    }

    /**
     * Opens a Spacewalk session and returns the associated session key.
     *
     * @param authentication
     *            the authentication parameters
     * @param queryUrl
     *            the Spacewalk URL
     * @return the session key
     * @throws SpacewalkException
     *             if an exception occurs interfacing with Spacewalk.
     */
    private static Object getSessionKey(final Credentials credentials,
            final URL queryUrl) throws SpacewalkException {
        Object sessionKey;
        try {
            sessionKey = openSession(credentials, queryUrl);
        } catch (XmlRpcException e) {
            String message =
                    String.format("Error opening Spacewalk session for %s - %s", queryUrl,
                            e.getMessage());
            LOGGER.error(message, e);

            final String authErrorMsg = "Either the password or username is incorrect";
            if (message.contains(authErrorMsg)) {
                throw new SpacewalkAuthenticationException(authErrorMsg, e);
            } else {
                throw new SpacewalkException(message, e);
            }
        }
        return sessionKey;
    }

    /**
     * Opens a Spacewalk session to test the connection.
     *
     * @param credentials
     *            the authentication parameters
     * @param spacewalkBaseUrl
     *            the Spacewalk URL
     * @throws SpacewalkException
     *             if an exception occurs interfacing with Spacewalk.
     */
    public static void testConnection(final Credentials credentials,
            final URL spacewalkBaseUrl) throws SpacewalkException {
        validateParameters(credentials, spacewalkBaseUrl);
        URL queryUrl = getQueryUrl(spacewalkBaseUrl);
        getSessionKey(credentials, queryUrl);
    }

    /**
     * Gets a url that always has the rpc/api path off of the main url, even if the provided URL
     * already contains it. e.g. if http://my-spacewalk.com is passed in, the returned value will be
     * http://my-spacewalk.com/rpc/api.
     *
     * @param spacewalkBaseUrl
     *            the original URL to use as a basis of the API.
     * @return the host of originalURL with /rpc/api
     * @throws MalformedURLException
     *             if the URL is malformed
     */
    private static URL getQueryUrl(final URL spacewalkBaseUrl) throws SpacewalkException {
        if (!"https".equals(spacewalkBaseUrl.getProtocol())) {
            throw new SpacewalkException("URL must use https protocol");
        }

        URL queryUrl;
        try {
            queryUrl = new URL(spacewalkBaseUrl, RPC_API_PATH);
        } catch (MalformedURLException e1) {
            String message =
                    String.format("Error genearting Spacewalk URL from %s - %s", spacewalkBaseUrl,
                            e1.getMessage());
            throw new SpacewalkException(message, e1);
        }
        return queryUrl;
    }

    private static void validateParameters(final Credentials credentials,
            final URL spacewalkBaseUrl) throws SpacewalkException {
        if (null == credentials) {
            throw new SpacewalkAuthenticationException(
                    "must provide credentials to query Spacewalk");
        }

        if (null == spacewalkBaseUrl) {
            throw new SpacewalkException("must provide Spacewalk URL");
        }
    }

    /**
     * Opens a new session with the spacewalk repository and logs in.
     *
     * @param authentication
     *            the authentication information required for login
     * @param spacewalkUrl
     *            the spacewalk URL
     * @return the key/token for the open session
     * @throws XmlRpcException
     *             if an exception occurs connecting to the service, or the login is rejected.
     */
    private static Object openSession(final Credentials credentials,
            final URL spacewalkUrl) throws XmlRpcException {

        LOGGER.debug("logging in to spacewalk: " + spacewalkUrl.toString());
        XmlRpcClient client = createXmlRpcClient(spacewalkUrl);
        Object sessionKey =
                client.execute(
                        "auth.login",
                        new Object[] {credentials.getUserName(),
                                credentials.getPassword()});
        return sessionKey;
    }

    /**
     * Closes the Spacewalk session by logging out via the API.
     *
     * @param sessionKey
     *            the session key to be closed.
     * @param spacewalkUrl
     *            the spacewalk URL
     */
    private static void closeSession(final Object sessionKey, final URL spacewalkUrl) {

        if (null == sessionKey || null == spacewalkUrl) {
            LOGGER.warn("session key or URL was null. Skipping log out request");
        } else {
            LOGGER.debug("logging out of spacewalk: " + spacewalkUrl.toString());
            XmlRpcClient client = createXmlRpcClient(spacewalkUrl);
            try {
                client.execute("auth.logout", new Object[] {sessionKey});
            } catch (XmlRpcException e) {
                // log logout exceptions, but do not bubble them up.
                LOGGER.error("Error logging out of Spacewalk");
            }
        }
    }

    /**
     * Creates a new XML RPC client for interfacing with Spacewalk.
     *
     * @param spacewalkUrl
     *            the RPC API URL
     * @return the XML RPC client
     */
    private static XmlRpcClient createXmlRpcClient(final URL spacewalkUrl) {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(spacewalkUrl);
        config.setEnabledForExtensions(true);
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);

        // use this factory so that connections will timeout. Otherwise, we'll block forever
        // becuase the default factory ignores timeout settings.
        XmlRpcCommonsTransportFactory factory = new XmlRpcCommonsTransportFactory(client);
        client.setTransportFactory(factory);

        LOGGER.debug("connection timeout ms: " + config.getConnectionTimeout());
        return client;
    }
}
