package hirs.repository.spacewalk;

import hirs.repository.RepositoryException;
import hirs.repository.RPMRepository;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Repository representing a single channel of a Spacewalk instance. A Spacewalk instance is a
 * server running the Spacewalk content management software which contains one or more software
 * channels. The authentication information is optionally stored.
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"baseUrl", "channelLabel" }))
public class SpacewalkChannelRepository extends RPMRepository<SpacewalkPackage> {

    private static final Logger LOGGER = LogManager.getLogger(SpacewalkChannelRepository.class);

    @Column
    private String channelLabel;

    @Column
    private boolean isPersistingCredentials;

    @Column()
    private String userName;

    @Column()
    private String password;

    @Transient
    private Credentials credentials;

    @Transient
    private Path tempPackageDirectory;

    /**
     * Construct a SpacewalkChannelRepository.
     *
     * @param name
     *            the repository name
     * @param baseUrl
     *            the repository base URL
     * @param channelLabel
     *            the channel label
     */
    public SpacewalkChannelRepository(final String name, final URL baseUrl,
            final String channelLabel) {
        super(name, baseUrl);
        if (StringUtils.isEmpty(channelLabel)) {
            throw new NullPointerException("null channelLabel");
        }

        this.channelLabel = channelLabel;
    }

    /**
     * Construct a SpacewalkChannelRepository.
     *
     * @param name
     *            the repository name
     */
    public SpacewalkChannelRepository(final String name) {
        super(name);
    }

    /**
     * Protected default constructor for Hibernate.
     */
    protected SpacewalkChannelRepository() {
        super();
    }

    /**
     * Gets boolean indicating if this SpacewalkChannelRepository will persist the password
     * authentication.
     *
     * @return true if persisting authentication information, false otherwise.
     */
    public boolean isPersistingCredentials() {
        return isPersistingCredentials;
    }

    /**
     * Gets the password authentication for this repository.
     *
     * @return the password authentication
     */
    public Credentials getCredentials() {
        if (isPersistingCredentials && null == credentials) {
            Credentials auth = new Credentials(userName, password);
            setCredentials(auth, isPersistingCredentials);
        }
        return credentials;
    }

    /**
     * Sets the password authentication, and the flag indicating if the info should be persisted.
     * The entity fields for user name and password are assigned based on the state of the
     * isPersistingAuthentication flag.
     *
     * @param credentials
     *            the authentication info
     * @param isPersistingCredentials
     *            flag indicating if the authentication info should be persisted.
     */
    public void setCredentials(final Credentials credentials,
            final boolean isPersistingCredentials) {

        if (null == credentials) {
            throw new NullPointerException("must provide credentials");
        }
        if (StringUtils.isEmpty(credentials.getUserName())) {
            throw new IllegalArgumentException("must provide a user name");
        }
        if (StringUtils.isEmpty(credentials.getPassword())) {
            throw new IllegalArgumentException("must provide a password");
        }

        this.credentials = credentials;
        this.isPersistingCredentials = isPersistingCredentials;
        if (this.isPersistingCredentials) {
            userName = credentials.getUserName();
            password = credentials.getPassword();
        } else {
            userName = null;
            password = null;
        }
    }

    @Override
    protected Set<SpacewalkPackage> listRemotePackages() throws RepositoryException {
        Credentials auth = getCredentials();
        if (null == auth) {
            throw new RepositoryException("Spacewalk authentication has not been provided");
        }

        List<SpacewalkPackage> packageList = Arrays.asList();
                //SpacewalkService.getPackages(auth, getBaseUrl(), channelLabel, this);
        return new HashSet<>(packageList);
    }

    @Override
    protected void measurePackage(final SpacewalkPackage repoPackage, final int maxDownloadAttempts)
            throws RepositoryException {
        Path tempPath;
        try {
            tempPath = getTempExtractionDirectory();
        } catch (IOException e1) {
            String message = "Error creating temporary directory for Spacewalk package downloads";
            LOGGER.error(message, e1);
            throw new RepositoryException(message, e1);
        }

        int tries = 0;
        while (true) {
            try {
                LOGGER.debug("Attempting download of package: " + repoPackage.getRPMIdentifier());
                Path rpmPath = downloadRpmFromSpacewalk(repoPackage, maxDownloadAttempts,
                        tempPath);
                LOGGER.debug("Measuring package: " + repoPackage.getRPMIdentifier());
                measurePackageAtPath(repoPackage, rpmPath);
                return;
            } catch (RepositoryException e) {
                LOGGER.warn("Failed to download {} on attempt #{}",
                        repoPackage.getRPMIdentifier(), tries);
                tries++;
                if (tries >= maxDownloadAttempts) {
                    LOGGER.error("Exceeded max download attemps", e);
                    throw e;
                }
            }
        }
    }

    /**
     * Sets up the temporary directory to download Spacewalk packages to, and queues the directory
     * for deletion.
     * @throws IOException if creating the temporary directory fails
     */
    private synchronized Path getTempExtractionDirectory() throws IOException {
        if (null == tempPackageDirectory) {
            tempPackageDirectory = getTemporaryDirectoryForRPMExtraction();
        }
        return tempPackageDirectory;
    }

    /**
     * Downloads a package from Spacewalk to a file.
     *
     * @param spacewalkPackage
     *            the Spacewalk package to download
     * @param maxDownloadAttempts
     *            the maximum number of download attempts
     * @param tmpDirectory
     *            the directory to store the RPM in
     * @return the file path to the downloaded RPM
     * @throws RepositoryException
     *             a Repository exception occurs
     */
    private Path downloadRpmFromSpacewalk(final SpacewalkPackage spacewalkPackage,
            final int maxDownloadAttempts, final Path tmpDirectory) throws RepositoryException {
        Credentials auth = getCredentials();
        if (null == auth) {
            throw new RepositoryException("Spacewalk authentication has not been provided");
        }
        int tries = 0;
        while (true) {
            try {
                return downloadRpmFromSpacewalkSingleAttempt(auth, spacewalkPackage,
                        tmpDirectory);
            } catch (RepositoryException e) {
                LOGGER.warn("Failed to download {} on attempt #{}", spacewalkPackage, tries);
                tries++;
                if (tries >= maxDownloadAttempts) {
                    throw e;
                }
            }
        }
    }

    private Path downloadRpmFromSpacewalkSingleAttempt(final Credentials auth,
            final SpacewalkPackage spacewalkPackage, final Path tmpDirectory)
                    throws RepositoryException {
        String rpmFileName = spacewalkPackage.getRPMIdentifier();
        Path rpmFilePath = tmpDirectory.resolve(rpmFileName + ".rpm");
        File rpmFile = rpmFilePath.toFile();

        URL downloadUrl;
        try {
            downloadUrl = new URL("localhost");
        } catch (MalformedURLException e) {
            throw new RepositoryException("Error getting Package download URL", e);
        }
        try {
            FileUtils.copyURLToFile(downloadUrl, rpmFile);
            LOGGER.debug("downloaded to: " + rpmFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RepositoryException("Error copying package at spacewalk URL to file", e);
        }
        return rpmFilePath;
    }

    /**
     * Sets the channel label.
     * @param channelLabel The channel label
     */
    public void setChannelLabel(final String channelLabel) {
        this.channelLabel = channelLabel;
    }

    /**
     * Gets the channel label.
     * @return The channel label
     */
    public String getChannelLabel() {
        return channelLabel;
    }
}
