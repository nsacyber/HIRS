package hirs.client.collector;

import hirs.DeviceInfoReportRequest;
import hirs.ReportRequest;
import hirs.collector.CollectorException;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.FirmwareInfo;
import hirs.data.persist.HardwareInfo;
import hirs.data.persist.NetworkInfo;
import hirs.data.persist.OSInfo;
import hirs.data.persist.OSName;
import hirs.data.persist.Report;
import hirs.data.persist.TPMInfo;
import hirs.utils.exec.ExecBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * The DeviceInfoCollector is used to gather information about the device.
 */
@Service
public class DeviceInfoCollector extends AbstractCollector {

    private static final String LINUX = "Linux";
    private static final int TYPE_INDEX = 0;
    private static final int VERSION_INDEX = 1;
    private static final int VERSION_TOKEN_INDEX = 2;
    private static final int VERSION_TOKEN_LENGTH = 4;
    private static final int VENDOR_INDEX = 1;
    private static final int ID_INDEX = 2;
    private static final int TOKEN_INDEX = 3;
    private static final int NULL_TPM_VERSION = -1;
    private static final int TPM_MAJOR_INDEX = 0;
    private static final int TPM_MINOR_INDEX = 1;
    private static final int TPM_REV_MAJOR_INDEX = 2;
    private static final int TPM_REV_MINOR_INDEX = 3;
    private static final int TOKEN_LENGTH_CHIP = 2;
    private static final int TOKEN_LENGTH_TPM = 3;

    /**
     * A variable used to describe unavailable hardware, firmware, or OS info.
     */
    public static final String NOT_SPECIFIED = "Not Specified";

    private static final Logger LOGGER = LogManager.getLogger(DeviceInfoCollector.class);

    private static final String OPT_PACCOR_SCRIPTS_ALLCOMPONENTS_SH
            = "/opt/paccor/scripts/allcomponents.sh";

    private X509Certificate clientCredential;

    private final String credentialFilePath;
    private final boolean tpmCollectionEnabled;
    private final String hostname;

    /**
     * Constructor. If constructing via spring context, uses properties defined
     * in a file identified as a @PropertySource, e.g. HIRSClient.properties.
     *
     * @param credentialFilePath path to the client credential
     * @param tpmCollectionEnabled true if TPM collection is enabled
     * @param hostname the host name (device name)
     */
    @Autowired
    public DeviceInfoCollector(
            @Value("${hirs.client.identity.credential}") final String credentialFilePath,
            @Value("${hirs.client.collector.tpm}") final boolean tpmCollectionEnabled,
            @Value("${hirs.client.name}") final String hostname) {
        this.credentialFilePath = credentialFilePath;
        this.tpmCollectionEnabled = tpmCollectionEnabled;
        this.hostname = hostname;
    }

    /**
     * Default constructor for unit testing (mockito spy).
     */
    DeviceInfoCollector() {
        this.credentialFilePath = "";
        this.tpmCollectionEnabled = false;
        this.hostname = "";
        LOGGER.warn("default constructor called! Should only be done in test");
    }

    private static String callPaccor() throws CollectorException {
        String[] command = {OPT_PACCOR_SCRIPTS_ALLCOMPONENTS_SH};

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        String errorAsString;
        try {
            // issue the command
            Process process = processBuilder.start();

            // block and wait for the process to be complete
            int returnCode = process.waitFor();

            try (InputStream processInputStream = process.getInputStream();
                 InputStream processErrorStream = process.getErrorStream()) {

                if (returnCode == 0) {
                    return IOUtils.toString(processInputStream);
                }

                errorAsString = IOUtils.toString(processErrorStream);
            }
        } catch (IOException | InterruptedException e) {
            errorAsString = e.getMessage();
            LOGGER.error(e.toString());
        }

        String errorMessage = "Failed to collect component info: " + errorAsString;
        LOGGER.error(errorMessage);
        throw new CollectorException(errorMessage);
    }

    /**
     * Retrieves dmidecode for the device. Only supports Linux.
     *
     * @param osName name of OS (Linux, Windows, etc.)
     * @param command dmidecode string keyword command
     * @return String of the result
     * @throws CollectorException if there is a problem encountered while performing collection
     */
    public static String collectDmiDecodeValue(final OSName osName, final String command)
            throws CollectorException {
        ExecBuilder execBuilder = null;
        try {
            switch (osName) {
                case LINUX:
                    execBuilder = new ExecBuilder("dmidecode").args("-s", command);
                    break;
                default:
                    throw new CollectorException(String.format(
                            "Unsupported operating system detected: %s.", osName));
            }

            return execBuilder.exec().getStdOutResult().trim();
        } catch (IOException e) {
            String msg = String.format("Could not call dmidecode using command: %s", execBuilder);
            throw new CollectorException(msg, e);
        }
    }

    /**
     * Takes a DeviceInfoReportRequest and returns a DeviceInfoReport. The
     * DeviceInfo report contains the following: <ul> <li>OSInfo</li> <ul>
     * <li>Name</li> <li>Version</li>
     * <li>Arch</li> <li>Distribution (if device is Linux)</li> <li>Distribution
     * release (if device is Linux)</li> </ul> <li>TPMInfo (may be null if TPM
     * is not available)</li> <ul>
     * <li>Make</li> <li>Version Major</li> <li>Version Minor</li> <li>Version
     * Revision Major</li>
     * <li>Version Revision Minor</li> <li>Identity Certificate</li> </ul>
     * <li>NetworkInfo</li> <ul>
     * <li>Hostname (may be null if not known)</li> <li>IP address (may be null
     * if not known)</li>
     * <li>MAC address (may be null if not known)</li> </ul> </ul>
     *
     * @param reportRequest DeviceInfoReportRequest
     * @return DeviceInfoReport
     * @throws CollectorException if issue occurs while collecting info for
     * report
     */
    @Override
    Report doCollect(final ReportRequest reportRequest) throws CollectorException {
        // Get device info
        NetworkInfo networkInfo = collectNetworkInfo();
        OSInfo osInfo = getPlatformOSInfo();
        FirmwareInfo firmwareInfo = getFirmwareInfo(osInfo.getOSName());
        HardwareInfo hardwareInfo = getHardwareInfo(osInfo.getOSName());
        TPMInfo tpmInfo = null;
        if (tpmCollectionEnabled) {
            tpmInfo = collectTPMInfo();
        }

        DeviceInfoReport report = new DeviceInfoReport(
                networkInfo,
                osInfo,
                firmwareInfo,
                hardwareInfo,
                tpmInfo
        );

        report.setPaccorOutputString(callPaccor());

        return report;
    }

    @Override
    public final boolean isCollectionEnabled() {
        // device info collection is always enabled.
        return true;
    }

    @Override
    public Class<? extends ReportRequest> reportRequestTypeSupported() {
        return DeviceInfoReportRequest.class;
    }

    private NetworkInfo collectNetworkInfo() throws CollectorException {
        try {
            InetAddress ipAddress = getFirstNonLoopbackAddress(true, false);
            if (ipAddress == null) {
                throw new CollectorException("Device must have non-loopback IP address");
            }
            byte[] macAddress = getMacAddress(ipAddress);
            return new NetworkInfo(hostname, ipAddress, macAddress);
        } catch (SocketException e) {
            LOGGER.error("error occurred while collecting IP address");
            throw new CollectorException(e);
        }
    }

    private InetAddress getFirstNonLoopbackAddress(final boolean preferIpv4,
            final boolean preferIPv6) throws SocketException {
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface i = en.nextElement();
            if (i.isUp()) {
                for (Enumeration<InetAddress> en2 = i.getInetAddresses(); en2.hasMoreElements();) {
                    InetAddress addr = en2.nextElement();
                    if (!addr.isLoopbackAddress()) {
                        if (addr instanceof Inet4Address) {
                            LOGGER.debug("IP v4 address" + addr);
                            if (preferIPv6) {
                                continue;
                            }
                            return addr;
                        }
                        if (addr instanceof Inet6Address) {
                            LOGGER.debug("IP v6 address" + addr);
                            if (preferIpv4) {
                                continue;
                            }
                            return addr;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static byte[] getMacAddress(final InetAddress currentAddress) throws SocketException {
        NetworkInterface network = NetworkInterface.getByInetAddress(currentAddress);
        return network.getHardwareAddress();
    }

    private TPMInfo collectTPMInfo() throws CollectorException {
        String tpmMake = null;
        short tpmVersionMajor = NULL_TPM_VERSION;
        short tpmVersionMinor = NULL_TPM_VERSION;
        short tpmVersionRevMajor = NULL_TPM_VERSION;
        short tpmVersionRevMinor = NULL_TPM_VERSION;

        BufferedReader quoteReader = getTPMVersion();
        if (quoteReader == null) {
            LOGGER.info("received a null response from the TPM version reader, "
                    + "assuming the TPM is not available and returning null "
                    + "TPMInfo");
            return null;
        }

        try {
            String line;
            while ((line = quoteReader.readLine()) != null) {
                String[] tokens = line.trim().replaceAll(":", "").split("\\s+");
                if (tokens.length > TOKEN_LENGTH_CHIP && tokens[TYPE_INDEX].equals("Chip")
                        && tokens[VERSION_INDEX].equals("Version")) {
                    String[] versionTokens = tokens[VERSION_TOKEN_INDEX].split("\\.");
                    if (versionTokens.length == VERSION_TOKEN_LENGTH) {
                        tpmVersionMajor = Short.parseShort(versionTokens[TPM_MAJOR_INDEX]);
                        tpmVersionMinor = Short.parseShort(versionTokens[TPM_MINOR_INDEX]);
                        tpmVersionRevMajor = Short.parseShort(versionTokens[TPM_REV_MAJOR_INDEX]);
                        tpmVersionRevMinor = Short.parseShort(versionTokens[TPM_REV_MINOR_INDEX]);
                        LOGGER.debug("Found TPM version {}.{}.{}.{}",
                                tpmVersionMajor, tpmVersionMinor,
                                tpmVersionRevMajor, tpmVersionRevMinor);
                    } else {
                        LOGGER.error("version number is unexpected number "
                                + "of tokens");
                    }
                } else if (tokens.length > TOKEN_LENGTH_TPM
                        && tokens[TYPE_INDEX].equals("TPM")
                        && tokens[VENDOR_INDEX].equals("Vendor")
                        && tokens[ID_INDEX].equals("ID")) {
                    tpmMake = tokens[TOKEN_INDEX];
                }
            }
        } catch (IOException e) {
            LOGGER.error(
                    "IOException - unable to parse tpm_version, returning "
                    + "null TPMInfo %s", e);
            return null;
        } finally {
            try {
                quoteReader.close();
            } catch (IOException ioe) {
                LOGGER.debug(String.format(
                        "%s had trouble closing the quoteReader in collectTPMInfo()",
                        getClass().getSimpleName()));
            }
        }

        if (tpmMake == null || tpmVersionMajor == NULL_TPM_VERSION
                || tpmVersionMinor == NULL_TPM_VERSION
                || tpmVersionRevMajor == NULL_TPM_VERSION
                || tpmVersionRevMinor == NULL_TPM_VERSION
                || clientCredential == null) {
            LOGGER.error("tpm info was not parsed correctly");
            throw new CollectorException("tpm info incorrectly parsed");
        }
        return new TPMInfo(tpmMake, tpmVersionMajor, tpmVersionMinor,
                tpmVersionRevMajor, tpmVersionRevMinor, clientCredential);
    }

    /**
     * Generates the OSInfo for a {@link DeviceInfoReport}.
     */
    private OSInfo getPlatformOSInfo() throws CollectorException {
        String osName = getSystemProperty("os.name");
        String osVersion = getSystemProperty("os.version");
        String osArch = getSystemProperty("os.arch");
        String distribution = NOT_SPECIFIED;
        String distributionRelease = NOT_SPECIFIED;

        if (osName.equals(LINUX)) {
            final File debianRelease = new File("/etc/lsb-release");
            final File redhatRelease = new File("/etc/redhat-release");
            BufferedReader reader = null;
            String line;

            if (debianRelease.exists()) {
                try {
                    reader = new BufferedReader(new InputStreamReader(
                            new FileInputStream(debianRelease), "UTF-8"));
                    while ((line = reader.readLine()) != null) {
                        String[] ubuntuTokens = line.split("=");
                        if (ubuntuTokens.length == 2) {
                            if (ubuntuTokens[0].equals("DISTRIB_ID")) {
                                distribution = ubuntuTokens[1];
                            }
                            if (ubuntuTokens[0].equals("DISTRIB_RELEASE")) {
                                distributionRelease = ubuntuTokens[1];
                            }
                        }
                    }

                } catch (IOException e) {
                    String msg = String.format("an error occurred while reading"
                            + " and parsing %s", debianRelease.toString());
                    LOGGER.error(msg);
                    throw new CollectorException(msg);
                } finally {
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException e) {
                        LOGGER.error(String.format("an error occurred while "
                                + "closing %s", debianRelease.toString()));
                    }
                }

            } else if (redhatRelease.exists()) {
                try {
                    reader = new BufferedReader(new InputStreamReader(
                            new FileInputStream(redhatRelease), "UTF-8"));
                    while ((line = reader.readLine()) != null) {
                        String[] redhatTokens = line.split("release");
                        if (redhatTokens.length == 2) {
                            distribution = redhatTokens[0];
                            distributionRelease = redhatTokens[1];
                        }
                    }

                } catch (IOException e) {
                    String msg = String.format("an error occurred while reading"
                            + " or parsing %s", redhatRelease.toString());
                    LOGGER.error(msg);
                    throw new CollectorException(msg);
                } finally {
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException e) {
                        LOGGER.error(String.format("an error occurred while "
                                + "closing %s", redhatRelease.toString()));
                    }
                }

            } else {
                String msg = String.format("Could not find either %s or %s",
                        debianRelease.getPath(), redhatRelease.getPath());
                LOGGER.error(msg);
            }
        }

        return new OSInfo(osName, osVersion, osArch, distribution, distributionRelease);
    }

    private FirmwareInfo getFirmwareInfo(final String osName) throws CollectorException {
        String biosVendor = NOT_SPECIFIED;
        String biosVersion = NOT_SPECIFIED;
        String biosReleaseDate = NOT_SPECIFIED;

        switch (osName) {
            case LINUX:
                biosVendor = StringUtils.defaultIfEmpty(
                        collectDmiDecodeValue(
                                OSName.LINUX, "bios-vendor"
                        ),
                        NOT_SPECIFIED
                );
                biosVersion = StringUtils.defaultIfEmpty(
                        collectDmiDecodeValue(
                                OSName.LINUX, "bios-version"
                        ),
                        NOT_SPECIFIED
                );
                biosReleaseDate = StringUtils.defaultIfEmpty(
                        collectDmiDecodeValue(
                                OSName.LINUX, "bios-release-date"
                        ),
                        NOT_SPECIFIED
                );
                break;
            default:
                LOGGER.error("OS is not supported: {}", osName);
        }

        return new FirmwareInfo(biosVendor, biosVersion, biosReleaseDate);
    }

    private HardwareInfo getHardwareInfo(final String osName) throws CollectorException {
        String manufacturer = NOT_SPECIFIED;
        String productName = NOT_SPECIFIED;
        String version = NOT_SPECIFIED;
        String serialNumber = NOT_SPECIFIED;
        String chassisSerialNumber = NOT_SPECIFIED;
        String baseboardSerialNumber = NOT_SPECIFIED;

        switch (osName) {
            case LINUX:
                manufacturer = StringUtils.defaultIfEmpty(
                        collectDmiDecodeValue(
                                OSName.LINUX, "system-manufacturer"
                        ),
                        NOT_SPECIFIED
                );

                productName = StringUtils.defaultIfEmpty(
                        collectDmiDecodeValue(
                                OSName.LINUX, "system-product-name"
                        ),
                        NOT_SPECIFIED
                );

                version = StringUtils.defaultIfEmpty(
                        collectDmiDecodeValue(
                            OSName.LINUX, "system-version"
                        ),
                        NOT_SPECIFIED
                );

                serialNumber = StringUtils.defaultIfEmpty(
                        collectDmiDecodeValue(
                                OSName.LINUX, "system-serial-number"
                        ),
                        NOT_SPECIFIED
                );

                chassisSerialNumber = StringUtils.defaultIfEmpty(
                        collectDmiDecodeValue(
                                OSName.LINUX, "chassis-serial-number"
                        ),
                        NOT_SPECIFIED
                );

                baseboardSerialNumber = StringUtils.defaultIfEmpty(
                        collectDmiDecodeValue(
                                OSName.LINUX, "baseboard-serial-number"
                        ),
                        NOT_SPECIFIED
                );

                break;
            default:
                LOGGER.error("OS is not supported: {}", osName);
        }

        return new HardwareInfo(
                manufacturer,
                productName,
                version,
                serialNumber,
                chassisSerialNumber,
                baseboardSerialNumber
        );
    }

    /**
     * This method is used to create a buffered reader to read the output from
     * the tpm_version command.
     *
     * @return a BufferedReader if tpm_version available, null if not
     */
    private BufferedReader getTPMVersion() {
        BufferedReader quoteReader;
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("tpm_version");
        try {
            Process quoteProcess = processBuilder.start();
            quoteReader
                    = new BufferedReader(new InputStreamReader(
                                    quoteProcess.getInputStream(), "utf-8"));
        } catch (IOException e) {
            LOGGER.info("IOException occurred while attempting to read "
                    + "tpm_version command, assume the TPM is not present and "
                    + "returning null");
            return null;
        }
        return quoteReader;
    }

    /**
     * Initializes the {@link DeviceInfoCollector}. Reads in the client
     * properties and client credential files.
     *
     * @throws CollectorException if the client credential cannot be found
     */
    @PostConstruct
    protected void initialize() throws CollectorException {
        if (tpmCollectionEnabled) {
            if (Files.exists(Paths.get(credentialFilePath))) {
                try (InputStream inputStream = new FileInputStream(credentialFilePath)) {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    clientCredential = (X509Certificate) cf.generateCertificate(inputStream);
                } catch (IOException | CertificateException ex) {
                    LOGGER.error(String.format("error occurred while "
                            + "reading from certificate file: %s", credentialFilePath), ex);
                }
            } else {
                throw new CollectorException("Client Credential not found: " + credentialFilePath);
            }
        }
    }

    /**
     * Retrieves the system property given the key value.
     *
     * @param value property value to retrieve
     * @return value of the system property
     */
    String getSystemProperty(final String value) {
        return System.getProperty(value);
    }
}
