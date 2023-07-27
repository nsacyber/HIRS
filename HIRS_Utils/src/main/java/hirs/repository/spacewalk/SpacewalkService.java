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

/**
 * Class responsible with all direct interfacing with a Spacewalk server via the Spacewalk API 2.4.
 * Uses XMLRPC to authenticate and pull repository information from Spacewalk.
 *
 * NOTE: multiple functions in this class were stubbed out to remove a dependency.
 * If you want to look at the original source code for this class, please see:
 * https://github.com/nsacyber/HIRS/blob/v2.1.2/
 *         HIRS_Utils/src/main/java/hirs/repository/spacewalk/SpacewalkService.java
 */
public final class SpacewalkService {

    private static final Logger LOGGER = LogManager.getLogger(SpacewalkService.class);

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

        List<SpacewalkChannel> channelList = new ArrayList<>();
        try {
            Object[] rawChannels = new Object[0];

            for (Object channelObj : rawChannels) {
                Map<String, Object> channelMap = (Map<String, Object>) channelObj;

                SpacewalkChannel channel = new SpacewalkChannel(channelMap, spacewalkBaseUrl);
                channelList.add(channel);
            }
            LOGGER.info("Found " + rawChannels.length + " channels. Closing session");
        } catch (IllegalArgumentException iae) {
            String message = "Error processing channel map returned from Spacewalk";
            LOGGER.error(message, iae);
            throw new SpacewalkException(message, iae);
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

        List<SpacewalkPackage> packageList = new ArrayList<>();

        Object[] rawPackages;
        try {
            rawPackages = new Object[0];
            for (Object packagObj : rawPackages) {
                Map<String, Object> packageMap = (Map<String, Object>) packagObj;

                packageList.add(SpacewalkPackage
                        .buildSpacewalkPackage(packageMap, sourceRepository));
            }
        } catch (IllegalArgumentException iae) {
            String message =
                    "Error processing channel map returned from Spacewalk for channel: "
                            + channelLabel;
            LOGGER.error(message, iae);
            throw new SpacewalkException(message, iae);
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

        LOGGER.debug("Getting download URL for package: " + spacewalkPackage.getRPMIdentifier());
        URL downloadUrl = null;
        Object rawUrl = null;
        try {
            rawUrl = new URL("localhost");
            downloadUrl = new URL(rawUrl.toString());
        } catch (MalformedURLException e) {
            String message = "Spacewalk returned an invalid download URL: " + rawUrl;
            LOGGER.error(message, e);
            throw new SpacewalkException(message, e);
        }

        return downloadUrl;
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
}
