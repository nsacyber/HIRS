package hirs.repository;

import hirs.utils.exec.ExecBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class implements the <code>Repository</code> interface for a Yum repository.  It requires
 * access to locally-installed copies of yum, yum-utils, rpm2cpio, cpio, rpmdev-vercmp from
 * rpmdevtools, as well as network access to a functional Yum repository.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YumRepository extends RPMRepository<RPMRepoPackage> {

    private static final HashMap<Integer, Path> CONFIG_FILES = new HashMap<>();
    private static final HashMap<Integer, Path> RPM_DIRECTORIES = new HashMap<>();

    private static final Pattern YUM_LIST_PACKAGE_LINE
            = Pattern.compile("(?<name>\\S+)\\.(?<architecture>\\S+)\\s+"
            + "(?<repetitionCounter>\\d+:)?(?<version>\\S+)-(?<release>\\S+)\\s+(?<repo>\\S+)");
    private static final String YUM_PACKAGE_HEADER = "Available Packages\n";
    private static final String YUM_ERROR_STRING = "PYCURL ERROR";
    private static final Logger LOGGER = LogManager.getLogger(YumRepository.class);

    private static final int DEFAULT_MAX_DOWNLOAD_ATTEMPTS = 3;

    @Column
    private URL mirrorListUrl;


    @Column
    private boolean gpgCheck;

    @Column
    private String gpgKey;

    @Transient
    private Path configFile = null;

    @Transient
    private Path rpmDirectory = null;

    /**
     * Construct an interface to a Yum repository.  These parameters precisely relate to their
     * equivalents in a standard yum repo configuration.  At least one of mirrorListUrl or baseUrl
     * must be non-null; if both are non-null, the mirrorList is used.
     *
     * @param name the repository's name
     * @param mirrorListUrl the URL of the repository's mirror list, if it exists.
     * @param baseUrl the base URL to the repository.
     * @param gpgCheck whether to enable Yum GPG checking.
     * @param gpgKey where the repository's GPG key resides on the local system
     */
    public YumRepository(final String name,
                         final URL mirrorListUrl,
                         final URL baseUrl,
                         final boolean gpgCheck,
                         final String gpgKey) {
        this(name);

        if (mirrorListUrl == null && baseUrl == null) {
            throw new IllegalArgumentException("Either mirrorListUrl or baseUrl must be non-null");
        }

        setBaseUrl(baseUrl);
        this.mirrorListUrl = mirrorListUrl;
        this.gpgCheck = gpgCheck;
        this.gpgKey = gpgKey;
    }

    /**
     * Construct an interface to a Yum repository.
     *
     * @param name the repository's name
     */
    public YumRepository(final String name) {
        super(name);
    }

    /**
     * Protected default constructor for Hibernate.
     */
    protected YumRepository() {
        super();
    }

    /**
     * Gets the repository's mirror list URL.
     *
     * @return the repository's mirror list URL.
     */
    public final URL getMirrorListUrl() {
        return mirrorListUrl;
    }


    /**
     * Gets the repo's GPG check setting.
     *
     * @return the repo's GPG check setting.
     */
    public final boolean isGpgCheck() {
        return gpgCheck;
    }

    /**
     * Gets the repo's GPG key setting.
     *
     * @return the repo's GPG key setting.
     */
    public final String getGpgKey() {
        return gpgKey;
    }

    @Override
    public final Set<RPMRepoPackage> listRemotePackages() throws RepositoryException {

        String list = null;
        try {
            checkTmpDirSetup();
            // clear yum cache
            yumExpireCache();

            // list all known packages
            list = yumListAll();
        } catch (IOException e) {
            String message = "IO Exception getting yum info";
            LOGGER.error(message, e);
            throw new RepositoryException(message, e);
        }

        int headerPos = list.indexOf(YUM_PACKAGE_HEADER);
        int errorPos = list.indexOf(YUM_ERROR_STRING);
        if (headerPos == -1 || errorPos != -1) {
            throw new RepositoryException(String.format(
                    "Failed to retrieve the list of packages at mirrorList:%s & baseUrl:%s",
                    mirrorListUrl, getBaseUrl()));
        }

        // trim output until the list of available packages starts
        list = list.substring(list.indexOf(YUM_PACKAGE_HEADER) + YUM_PACKAGE_HEADER.length());
        list = list.trim();
        Set<RPMRepoPackage> packages = new HashSet<>();
        Matcher matcher = YUM_LIST_PACKAGE_LINE.matcher(list);
        while (matcher.find()) {
            packages.add(new RPMRepoPackage(
                    matcher.group("name"),
                    matcher.group("version"),
                    matcher.group("release"),
                    matcher.group("architecture"),
                    this
            ));
        }

        return packages;
    }

    /**
     * Downloads the specified package and uses RPMMeasurer to measure its contents, which
     * are then stored in the given RPMRepoPackage object.  Will attempt to download a package up
     * to <code>maxDownloadAttempts</code> times before throwing an I/O exception.  This method
     * will also hash the software package file itself and store the result in the RPMRepoPackage.
     *
     * @param repoPackage the packages to measure
     * @param maxDownloadAttempts the RPM download attempt limit
     *
     * @throws RepositoryException
     *          if a problem is encountered while downloading and measuring the package
     */
    @Override
    public final void measurePackage(final RPMRepoPackage repoPackage,
                                     final int maxDownloadAttempts) throws RepositoryException {
        try {
            checkTmpDirSetup();
            Path rpmPath = yumDownload(repoPackage, maxDownloadAttempts);
            measurePackageAtPath(repoPackage, rpmPath);
        } catch (IOException e) {
            String message = "error mesauring RPM package";
            LOGGER.error(message, e);
            throw new RepositoryException(message, e);
        }
    }

    /**
     * Downloads and measures all the specified packages with the default number of download
     * attempts.
     *
     * @param repoPackage the packages to measure
     *
     * @throws RepositoryException
     *          if a problem is encountered while downloading and measuring the package
     */
    public final void measurePackage(final RPMRepoPackage repoPackage) throws RepositoryException {
        this.measurePackage(repoPackage, DEFAULT_MAX_DOWNLOAD_ATTEMPTS);
    }

    // augments platform Yum configuration to use only this repository
    private String generateConfig() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(FileUtils.readFileToString(
                        FileSystems.getDefault().getPath("/etc", "yum.conf").toFile())
        );
        sb.append(String.format("%nreposdir=%n%n"));

        sb.append(String.format("[%s]%n", getId()));
        sb.append(String.format("name=%s%n", getId()));

        if (mirrorListUrl != null) {
            sb.append(String.format("mirrorlist=%s%n", mirrorListUrl));
        }

        URL baseUrl = getBaseUrl();
        if (baseUrl != null) {
            if (mirrorListUrl != null) {
                // if the mirror list URL is used, also print the baseUrl as a comment
                sb.append(String.format("#baseurl=%s%n", baseUrl));
            } else {
                sb.append(String.format("baseurl=%s%n", baseUrl));
            }
        }

        if (gpgCheck) {
            sb.append(String.format("gpgcheck=%d%n", 1));
        } else {
            sb.append(String.format("gpgcheck=%d%n", 0));
        }

        if (gpgKey != null) {
            sb.append(String.format("gpgkey=%s%n", gpgKey));
        }

        sb.append(String.format("enabled=1%n"));

        return sb.toString();
    }

    private void yumExpireCache() throws IOException {
        new ExecBuilder("yum")
                .args("--config=" + configFile.toAbsolutePath().toString(),
                        "clean",
                        "expire-cache")
                .exec();
    }

    private String yumListAll() throws IOException {
        try {
            return new ExecBuilder("yum")
                    .args("--config=" + configFile.toAbsolutePath().toString(),
                            "--showduplicates",
                            "list",
                            "all")
                    .exec().getStdOutResult();
        } catch (IOException ioException) {
            String errorMessage;
            if (ioException.getMessage().contains("repomd.xml")) {
                errorMessage = "failed to retrieve repository metadata (repomd.xml)";
            } else {
                errorMessage = "failed to list packages";
            }
            throw new IOException(errorMessage + " from repository: " + this.getName() + ".");
        }
    }

    private Path yumDownload(final RPMRepoPackage pkg, final int maxDownloadAttempts)
            throws IOException {
        String yumRPMName = pkg.getRPMIdentifier();
        Path rpmPath = rpmDirectory.resolve(yumRPMName + ".rpm");

        int tries = 0;
        boolean downloaded = false;

        while (!downloaded) {
            try {
                new ExecBuilder("yumdownloader")
                        .args("--config=" + configFile.toAbsolutePath().toString(),
                                "--destdir=" + rpmDirectory.toAbsolutePath().toString(),
                                yumRPMName)
                        .exec();

                if (!Files.exists(rpmPath)) {
                    throw new IOException("Failed to download file " + yumRPMName);
                }

                downloaded = true;
            } catch (IOException e) {
                LOGGER.warn("Failed to download {} on attempt #{}", yumRPMName, tries);
                tries++;
                if (tries >= maxDownloadAttempts) {
                    throw e;
                }
            }
        }

        return rpmPath;
    }

    private void checkTmpDirSetup() throws IOException {
        if (getBaseUrl() == null && mirrorListUrl == null) {
            throw new IllegalStateException("No URL specified!");
        }
        if (configFile != null) {
            return;
        }

        setupTmpDirs(this);
    }

    private static synchronized void setupTmpDirs(final YumRepository repo) throws IOException {
        int configHash = repo.generateConfig().hashCode();

        if (CONFIG_FILES.containsKey(configHash)) {
            repo.configFile = CONFIG_FILES.get(configHash);
            repo.rpmDirectory = RPM_DIRECTORIES.get(configHash);
            return;
        }

        try {
            // create temporary directory to store data in
            final Path tmpDirectory = getTemporaryDirectoryForRPMExtraction();

            // create config file and store its contents
            Path configFile = Files.createTempFile(tmpDirectory, "tmp-yum", ".conf");
            FileUtils.writeStringToFile(configFile.toFile(), repo.generateConfig());
            LOGGER.debug("wrote the yum config to file: {}", configFile.toString());
            repo.configFile = configFile;
            CONFIG_FILES.put(configHash, configFile);

            // create directory to store RPMs in
            Path rpmDirectory = Files.createTempDirectory(tmpDirectory, "rpms");
            repo.rpmDirectory = rpmDirectory;
            RPM_DIRECTORIES.put(configHash, rpmDirectory);

        } catch (IOException e) {
            throw new IOException("Failed to set up temporary repository directory", e);
        }
    }
}
