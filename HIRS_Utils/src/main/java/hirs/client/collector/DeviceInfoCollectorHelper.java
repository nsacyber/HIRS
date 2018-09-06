package hirs.client.collector;

import hirs.collector.CollectorException;
import hirs.data.persist.BIOSComponentInfo;
import hirs.data.persist.BaseboardComponentInfo;
import hirs.data.persist.ChassisComponentInfo;
import hirs.data.persist.ComponentInfo;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.HardDriveComponentInfo;
import hirs.data.persist.MemoryComponentInfo;
import hirs.data.persist.NICComponentInfo;
import hirs.data.persist.OSName;
import hirs.data.persist.ProcessorComponentInfo;
import hirs.utils.exec.AsynchronousExecResult;
import hirs.utils.exec.ExecBuilder;
import hirs.utils.exec.ExecPipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a utility class which supports collecting additional
 * information for the DeviceInfoCollector.
 */
@SuppressWarnings("checkstyle:linelength")
public final class DeviceInfoCollectorHelper {
    private static final Logger LOGGER = LogManager.getLogger(DeviceInfoCollectorHelper.class);

    // commands
    private static final String DMIDECODE = "dmidecode";
    private static final String LSHW = "lshw";
    private static final String GREP = "grep";
    private static final String SED = "sed";
    private static final String WC = "wc";
    private static final String AWK = "awk";

    // DMI component enumerations
    private static final int DMI_BIOS = 0;
    private static final int DMI_BASEBOARD = 2;
    private static final int DMI_CHASSIS = 3;
    private static final int DMI_PROCESSOR = 4;
    private static final int DMI_MEMORY = 17;

    // lshw hardware categories
    private static final String LSHW_NETWORK = "network";
    private static final String LSHW_DISK = "disk";

    private static final String AWK_COUNT_COMPONENTS =
            "BEGIN { num=0 } { if ($0 ~ /^  \\*-.+/) { num++ } } END { print num }";

    private static final String AWK_GET_LSHW_HEADER =
            "BEGIN { device_idx=0 } { if ($0 ~ /^  \\*-.+/) { device_idx++ } if (device_idx == dev && $0 ~ /^  \\*-.+/) { print } }";

    private static final String AWK_GET_LSHW_FIELD =
            "BEGIN { device_idx=0 } { if ($0 ~ /^  \\*-.+/) { device_idx++ } if (device_idx == dev && $0 ~ field) { print } }";

    /**
     * Default constructor to prevent construction.
     */
    private DeviceInfoCollectorHelper() {
    }

    /**
     * Collect all possible ComponentInfo information from the
     * local machine and store it in the given DeviceInfoReport.
     *
     * @param report the DeviceInfoReport that will hold the ComponentInfo
     * @throws CollectorException if an error is encountered during collection
     */
    public static void collectAndStoreComponentInfo(final DeviceInfoReport report)
            throws CollectorException {
        try {
            report.setChassisInfo(getChassisInfo());
            report.setBaseboardInfo(getBaseboardInfo());
            report.setProcessorInfo(getProcessorInfo());
            report.setBiosInfo(getBiosInfo());
            report.setNicInfo(getNicInfo());
            report.setHardDriveInfo(getHardDriveInfo());
            report.setMemoryInfo(getMemoryInfo());
        } catch (IOException e) {
            throw new CollectorException("Failed to collect component info", e);
        }
    }

    private static List<ChassisComponentInfo> getChassisInfo() throws IOException {
        LOGGER.debug("Collecting ChassisComponentInfo...");
        List<ChassisComponentInfo> components = new ArrayList<>();
        int compCount = getDmiCompCount(DMI_CHASSIS, "Manufacturer");
        for (int i = 1; i <= compCount; i++) {
            String manufacturer = getDmiCompAttr(DMI_CHASSIS, "Manufacturer", getSedDmiLshwVal(i));
            String model = getDmiCompAttr(DMI_CHASSIS, "Type", getSedDmiLshwVal(i));
            String serial = getDmiCompAttr(DMI_CHASSIS, "Serial Number", getSedDmiLshwVal(i));
            String revision = getDmiCompAttr(DMI_CHASSIS, "Version", getSedDmiLshwVal(i));

            if (ComponentInfo.isComplete(manufacturer, model, serial, revision)) {
                ChassisComponentInfo component =
                        new ChassisComponentInfo(manufacturer, model, serial, revision);
                LOGGER.debug(String.format("Collected: %s", component.toString()));
                components.add(component);
            }
        }
        return components;
    }

    private static List<BaseboardComponentInfo> getBaseboardInfo() throws IOException {
        LOGGER.debug("Collecting BaseboardComponentInfo...");
        List<BaseboardComponentInfo> components = new ArrayList<>();
        int compCount = getDmiCompCount(DMI_BASEBOARD, "Manufacturer");
        for (int i = 1; i <= compCount; i++) {
            String manufacturer =
                    getDmiCompAttr(DMI_BASEBOARD, "Manufacturer", getSedDmiLshwVal(i));
            String model = getDmiCompAttr(DMI_BASEBOARD, "Product Name", getSedDmiLshwVal(i));
            String serial = getDmiCompAttr(DMI_BASEBOARD, "Serial Number", getSedDmiLshwVal(i));
            String revision = getDmiCompAttr(DMI_BASEBOARD, "Version", getSedDmiLshwVal(i));

            if (ComponentInfo.isComplete(manufacturer, model, serial, revision)) {
                BaseboardComponentInfo component =
                        new BaseboardComponentInfo(manufacturer, model, serial, revision);
                LOGGER.debug(String.format("Collected: %s", component.toString()));
                components.add(component);
            }
        }
        return components;
    }

    private static List<ProcessorComponentInfo> getProcessorInfo() throws IOException {
        LOGGER.debug("Collecting ProcessorComponentInfo...");
        List<ProcessorComponentInfo> components = new ArrayList<>();
        int compCount = getDmiCompCount(DMI_PROCESSOR, "Manufacturer");
        for (int i = 1; i <= compCount; i++) {
            String manufacturer =
                    getDmiCompAttr(DMI_PROCESSOR, "Manufacturer", getSedDmiLshwVal(i));
            String model = getDmiCompAttr(DMI_PROCESSOR, "Family", getSedDmiLshwVal(i));
            String serial = getDmiCompAttr(DMI_PROCESSOR, "Serial Number", getSedDmiLshwVal(i));
            String revision = getDmiCompAttr(DMI_PROCESSOR, "Version", getSedDmiLshwVal(i));

            if (ComponentInfo.isComplete(manufacturer, model, serial, revision)) {
                ProcessorComponentInfo component =
                        new ProcessorComponentInfo(manufacturer, model, serial, revision);
                LOGGER.debug(String.format("Collected: %s", component.toString()));
                components.add(component);
            }
        }
        return components;
    }

    private static List<BIOSComponentInfo> getBiosInfo() throws IOException {
        LOGGER.debug("Collecting BIOSComponentInfo...");
        List<BIOSComponentInfo> components = new ArrayList<>();
        int compCount = getDmiCompCount(DMI_BIOS, "Vendor");
        for (int i = 1; i <= compCount; i++) {
            String manufacturer = getDmiCompAttr(DMI_BIOS, "Vendor", getSedDmiLshwVal(i));
            String model = "BIOS";
            String revision =  getDmiCompAttr(DMI_BIOS, "Version", getSedDmiLshwVal(i));

            if (ComponentInfo.isComplete(manufacturer, model, null, revision)) {
                BIOSComponentInfo component = new BIOSComponentInfo(manufacturer, model, revision);
                LOGGER.debug(String.format("Collected: %s", component.toString()));
                components.add(component);
            }
        }
        return components;
    }

    private static List<NICComponentInfo> getNicInfo() throws IOException {
        LOGGER.debug("Collecting NICComponentInfo...");
        List<NICComponentInfo> components = new ArrayList<>();
        int compCount = getLshwCompCount(LSHW_NETWORK);
        for (int i = 1; i <= compCount; i++) {
            String manufacturer = getLshwCompAttr(LSHW_NETWORK, i, "vendor");
            String model = getLshwCompAttr(LSHW_NETWORK, i, "product");
            String serial =
                    getLshwCompAttr(LSHW_NETWORK, i, "serial");
            String revision = getLshwCompAttr(LSHW_NETWORK, i, "version");

            if (ComponentInfo.isComplete(manufacturer, model, serial, revision)) {
                NICComponentInfo component =
                        new NICComponentInfo(manufacturer, model, serial, revision);
                LOGGER.debug(String.format("Collected: %s", component.toString()));
                components.add(component);
            }
        }
        return components;
    }

    private static List<HardDriveComponentInfo> getHardDriveInfo() throws IOException {
        LOGGER.debug("Collecting HardDriveComponentInfo...");
        List<HardDriveComponentInfo> components = new ArrayList<>();
        int compCount = getLshwCompCount(LSHW_DISK);
        for (int i = 1; i <= compCount; i++) {
            // known values: disk, cdrom
            String diskDeviceType = getLshwHeader(LSHW_DISK, i);
            if (!diskDeviceType.startsWith("*-disk")) {
                LOGGER.debug(String.format("%s did not match *-disk prefix; skipping.", diskDeviceType));
                continue;
            }

            String manufacturer = getLshwCompAttr(LSHW_DISK, i, "vendor");
            String model = getLshwCompAttr(LSHW_DISK, i, "product");
            String serial =
                    getLshwCompAttr(LSHW_DISK, i, "serial");
            String revision = getLshwCompAttr(LSHW_DISK, i, "version");

            if (ComponentInfo.isComplete(manufacturer, model, serial, revision)) {
                HardDriveComponentInfo component =
                        new HardDriveComponentInfo(manufacturer, model, serial, revision);
                LOGGER.debug(String.format("Collected: %s", component.toString()));
                components.add(component);
            }
        }
        return components;
    }

    private static List<MemoryComponentInfo> getMemoryInfo() throws IOException {
        LOGGER.debug("Collecting MemoryComponentInfo...");
        List<MemoryComponentInfo> components = new ArrayList<>();
        int compCount = getDmiCompCount(DMI_MEMORY, "Manufacturer");
        for (int i = 1; i <= compCount; i++) {
            String manufacturer = getDmiCompAttr(DMI_MEMORY, "Manufacturer", getSedDmiLshwVal(i));
            String model = getDmiCompAttr(DMI_MEMORY, "Part Number", getSedDmiLshwVal(i));
            String serial = getDmiCompAttr(DMI_MEMORY, "Serial Number", getSedDmiLshwVal(i));
            String revision = null;

            if (ComponentInfo.isComplete(manufacturer, model, serial, revision)) {
                MemoryComponentInfo component =
                        new MemoryComponentInfo(manufacturer, model, serial, revision);
                LOGGER.debug(String.format("Collected: %s", component.toString()));
                components.add(component);
            }
        }
        return components;
    }

    private static int getDmiCompCount(final int dmidecodeComponentType, final String termToCount)
            throws IOException {
        return Integer.parseInt(waitForAndGetOutput(ExecPipe.pipeOf(false, new String[][]{
                {DMIDECODE, "-t", Integer.toString(dmidecodeComponentType)},
                {GREP, termToCount + ":"},
                {WC, "-l"}
        }).exec()));
    }

    private static String getDmiCompAttr(
            final int dmidecodeComponentType,
            final String attr,
            final String[] sedCleanFormat
    ) throws IOException {
        return waitForAndGetOutput(ExecPipe.pipeOf(false, new String[][]{
                {DMIDECODE, "-t", Integer.toString(dmidecodeComponentType)},
                {GREP, attr + ":"},
                sedCleanFormat
        }).exec());
    }

    private static String getLshwHeader(
            final String lshwComponentName,
            final int lshwComponentIndex
    ) throws IOException {
        return waitForAndGetOutput(ExecPipe.pipeOf(false, new String[][]{
                {LSHW, "-class", lshwComponentName},
                {AWK, "-vdev=" + lshwComponentIndex, AWK_GET_LSHW_HEADER}
        }).exec());
    }

    private static int getLshwCompCount(final String componentType)
            throws IOException {
        return Integer.parseInt(waitForAndGetOutput(ExecPipe.pipeOf(false, new String[][]{
                {LSHW, "-class", componentType},
                {AWK, AWK_COUNT_COMPONENTS}
        }).exec()));
    }

    private static String getLshwCompAttr(
            final String lshwComponentName,
            final int lshwComponentIndex,
            final String attr
    ) throws IOException {
        String attrToSearch = "       " + attr + ":";
        String lshwFieldVal = waitForAndGetOutput(ExecPipe.pipeOf(false, new String[][]{
                {LSHW, "-class", lshwComponentName},
                {AWK, "-vdev=" + lshwComponentIndex, "-vfield=" + attrToSearch, AWK_GET_LSHW_FIELD}
        }).exec());

        if (!lshwFieldVal.isEmpty()) {
            // remove the field and the following ': ' to just leave the value
            return lshwFieldVal.substring(lshwFieldVal.indexOf(":") + 2);
        } else {
            return DeviceInfoCollector.NOT_SPECIFIED;
        }
    }

    private static String[] getSedDmiLshwVal(final int componentNumber) {
        return new String[]{
                SED, "-e", "s/[^:]*:[ ]*//", "-n", "-e", String.format("%dp", componentNumber)
        };
    }

    private static String waitForAndGetOutput(final AsynchronousExecResult execResult)
            throws IOException {
        return waitFor(execResult).getStdOutResult().trim();
    }

    private static AsynchronousExecResult waitFor(final AsynchronousExecResult execResult)
            throws IOException {
        try {
            execResult.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        execResult.throwExceptionIfFailed();
        return execResult;
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
}
