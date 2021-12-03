package hirs.tpm;

import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.info.FirmwareInfo;
import hirs.data.persist.info.HardwareInfo;
import hirs.data.persist.IntegrityReport;
import hirs.data.persist.info.OSInfo;
import hirs.data.persist.Report;
import hirs.data.persist.baseline.TPMBaseline;
import hirs.data.persist.info.TPMInfo;
import hirs.data.persist.TPMMeasurementRecord;
import hirs.data.persist.TPMReport;
import hirs.data.persist.baseline.TpmBlackListBaseline;
import hirs.data.persist.baseline.TpmWhiteListBaseline;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class used to process comma separated value files containing TPM PCR
 * measurements or HIRS integrity reports to generate a TPM measurement
 * baseline. Generating a TPM measurement baseline from a HIRS integrity report
 * is considered experimental and provided as a tool to assist with creating,
 * analyzing, and refining measurement baselines.
 */
public class TPMBaselineGenerator {

    private static final Logger LOGGER
            = LogManager.getLogger(TPMBaselineGenerator.class);

    private static final String KERNEL_UPDATE_BASELINE_NAME = "Kernel Update %s %s";
    private static final String VALID_REGEX = "[0-9a-zA-Z./()_,\" -]+";


    /**
     * Enumerates the device info fields looked for within a TPMBaseline CSV file.
     */
    public enum TPMBaselineFields {
        /**
         * FirmwareInfo's BIOS Vendor.
         */
        BIOS_VENDOR,

        /**
         * FirmwareInfo's BIOS Version.
         */
        BIOS_VERSION,

        /**
         * FirmwareInfo's BIOS Release Date.
         */
        BIOS_RELEASE_DATE,

        /**
         * HardwareInfo's Manufacturer.
         */
        MANUFACTURER,

        /**
         * HardwareInfo's Product Name.
         */
        PRODUCT_NAME,

        /**
         * HardwareInfo's Version.
         */
        VERSION,

        /**
         * HardwareInfo's Serial number.
         */
        SYSTEM_SERIAL_NUMBER,

        /**
         * HardwareInfo's Chassis serial number.
         */
        CHASSIS_SERIAL_NUMBER,

        /**
         * HardwareInfo's baseboard serial number.
         */
        BASEBOARD_SERIAL_NUMBER,

        /**
         * OSInfo's OS Name.
         */
        OS_NAME,

        /**
         * OSInfo's OS Version.
         */
        OS_VERSION,

        /**
         * OSInfo's OS Arch.
         */
        OS_ARCH,

        /**
         * OSInfo's Distribution.
         */
        DISTRIBUTION,

        /**
         * OSInfo's Distribution Release.
         */
        DISTRIBUTION_RELEASE,

        /**
         * TPMInfo's TPM Make.
         */
        TPM_MAKE,

        /**
         * TPMInfo's TPM Version Major.
         */
        TPM_VERSION_MAJOR,

        /**
         * TPMInfo's TPM Version Minor.
         */
        TPM_VERSION_MINOR,

        /**
         * TPMInfo's TPM Version Rev Major.
         */
        TPM_VERSION_REV_MAJOR,

        /**
         * TPMInfo's TPM Version Rev Minor.
         */
        TPM_VERSION_REV_MINOR;

        /**
         * Generates a CSV String from a map of TPMBaselineFields to values.
         * @param map Map&lt;TPMBaselineFields, String&gt;
         * @return CSV-escaped String corresponding to the map of values
         */
        public static String toCSV(final Map<TPMBaselineFields, String> map) {
            if (map == null) {
                throw new IllegalArgumentException("TPMBaselineFields.toCSV:"
                    + " This method should not have been called with a null parameter.");
            }
            final StringBuilder builder = new StringBuilder();
            for (final Entry<TPMBaselineFields, String> field : map.entrySet()) {
                builder.append(field.getKey().name())
                        .append(",")
                        .append(StringEscapeUtils.escapeCsv(field.getValue()))
                        .append(System.lineSeparator());
            }
            return builder.toString();
        }

        /**
         * Creates a new FirmwareInfo object from the supplied data field changes.  Any
         * fields not supplied will retain their existing values from the given default
         * FirmwareInfo object.
         * @param map Map<TPMBaselineFields, String> correlating field keys to values.
         * @param defaultInfo the FirmwareInfo object to reference for default values.
         * @return new FirmwareInfo object with updated fields.
         */
        private static FirmwareInfo toFirmwareInfo(final Map<TPMBaselineFields, String> map,
                                                   final FirmwareInfo defaultInfo) {
            if (map == null || defaultInfo == null) {
                throw new IllegalArgumentException("TPMBaselineFields.toFirmwareInfo:"
                    + " This method should not have been called with a null parameter.");
            }

            final String biosVendor =
                StringUtils.defaultIfBlank(
                    map.get(TPMBaselineFields.BIOS_VENDOR), defaultInfo.getBiosVendor());

            final String biosVersion =
                    StringUtils.defaultIfBlank(
                        map.get(TPMBaselineFields.BIOS_VERSION), defaultInfo.getBiosVersion());

            final String biosReleaseDate =
                    StringUtils.defaultIfBlank(map.get(TPMBaselineFields.BIOS_RELEASE_DATE),
                        defaultInfo.getBiosReleaseDate());

            return new FirmwareInfo(biosVendor, biosVersion, biosReleaseDate);
        }

        /**
         * Creates a new HardwareInfo object from the supplied data field changes.  Any
         * fields not supplied will retain their existing values from the given default
         * HardwareInfo object.
         * @param map Map<TPMBaselineFields, String> correlating field keys to values.
         * @param defaultInfo the HardwareInfo object to reference for default values.
         * @return new HardwareInfo object with updated fields.
         */
        private static HardwareInfo toHardwareInfo(final Map<TPMBaselineFields, String> map,
                                                   final HardwareInfo defaultInfo) {
            if (map == null || defaultInfo == null) {
                throw new IllegalArgumentException("TPMBaselineFields.toHardwareInfo:"
                    + " This method should not have been called with a null parameter.");
            }

            final String manufacturer =
                StringUtils.defaultIfBlank(
                    map.get(TPMBaselineFields.MANUFACTURER), defaultInfo.getManufacturer());

            final String productName =
                    StringUtils.defaultIfBlank(
                        map.get(TPMBaselineFields.PRODUCT_NAME), defaultInfo.getProductName());

            final String version =
                    StringUtils.defaultIfBlank(
                        map.get(TPMBaselineFields.VERSION), defaultInfo.getVersion());

            final String serialNumber =
                    StringUtils.defaultIfBlank(map.get(TPMBaselineFields.SYSTEM_SERIAL_NUMBER),
                        defaultInfo.getSystemSerialNumber());

            final String chassisSerialNumber =
                    StringUtils.defaultIfBlank(map.get(TPMBaselineFields.CHASSIS_SERIAL_NUMBER),
                            defaultInfo.getChassisSerialNumber());

            final String baseboardSerialNumber =
                    StringUtils.defaultIfBlank(map.get(TPMBaselineFields.BASEBOARD_SERIAL_NUMBER),
                            defaultInfo.getBaseboardSerialNumber());

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
         * Creates a new OSInfo object from the supplied data field changes.  Any
         * fields not supplied will retain their existing values from the given default
         * OSInfo object.
         * @param map Map<TPMBaselineFields, String> correlating field keys to values.
         * @param defaultInfo the HardwareInfo object to reference for default values.
         * @return new OSInfo object with updated fields.
         */
        private static OSInfo toOSInfo(final Map<TPMBaselineFields, String> map,
                                       final OSInfo defaultInfo) {
            if (map == null || defaultInfo == null) {
                throw new IllegalArgumentException("TPMBaselineFields.toOSInfo:"
                    + " This method should not have been called with a null parameter.");
            }

            final String osName =
                StringUtils.defaultIfBlank(
                    map.get(TPMBaselineFields.OS_NAME), defaultInfo.getOSName());

            final String osVersion =
                    StringUtils.defaultIfBlank(
                        map.get(TPMBaselineFields.OS_VERSION), defaultInfo.getOSVersion());

            final String osArch =
                    StringUtils.defaultIfBlank(
                        map.get(TPMBaselineFields.OS_ARCH), defaultInfo.getOSArch());

            final String distribution =
                    StringUtils.defaultIfBlank(map.get(TPMBaselineFields.DISTRIBUTION),
                        defaultInfo.getDistribution());

            final String distributionRelease =
                    StringUtils.defaultIfBlank(map.get(TPMBaselineFields.DISTRIBUTION_RELEASE),
                        defaultInfo.getDistributionRelease());

            return new OSInfo(osName, osVersion, osArch, distribution, distributionRelease);
        }

        /**
         * Creates a new TPMInfo object from the supplied data field changes.  Any
         * fields not supplied will retain their existing values from the given default
         * TPMInfo object.
         * @param map Map<TPMBaselineFields, String> correlating field keys to values.
         * @param defaultInfo the HardwareInfo object to reference for default values.
         * @return new TPMInfo object with updated fields.
         */
        private static TPMInfo toTPMInfo(final Map<TPMBaselineFields, String> map,
                                         final TPMInfo defaultInfo) {
            if (map == null || defaultInfo == null) {
                throw new IllegalArgumentException("TPMBaselineFields.toTPMInfo:"
                    + " This method should not have been called with a null parameter.");
            }

            final String tpmMake =
                StringUtils.defaultIfBlank(
                    map.get(TPMBaselineFields.TPM_MAKE), defaultInfo.getTPMMake());

            final String tpmVersionMajor =
                    StringUtils.defaultIfBlank(map.get(TPMBaselineFields.TPM_VERSION_MAJOR),
                        "" + defaultInfo.getTPMVersionMajor());

            final String tpmVersionMinor =
                    StringUtils.defaultIfBlank(map.get(TPMBaselineFields.TPM_VERSION_MINOR),
                        "" + defaultInfo.getTPMVersionMinor());

            final String tpmVersionRevMajor =
                    StringUtils.defaultIfBlank(map.get(TPMBaselineFields.TPM_VERSION_REV_MAJOR),
                        "" + defaultInfo.getTPMVersionRevMajor());

            final String tpmVersionRevMinor =
                    StringUtils.defaultIfBlank(map.get(TPMBaselineFields.TPM_VERSION_REV_MINOR),
                        "" + defaultInfo.getTPMVersionMinor());

            return new TPMInfo(tpmMake, Short.valueOf(tpmVersionMajor),
                Short.valueOf(tpmVersionMinor), Short.valueOf(tpmVersionRevMajor),
                Short.valueOf(tpmVersionRevMinor));
        }
    }

    /**
     * Method generates a TPM measurement white list baseline from a .csv file containing
     * PCR measurement entries. An off-line PCR measurement baseline process is
     * used to generate the .csv file. TPM measurement records are expected to
     * adhere to the following record structure with a comma separating them:
     * <ul>
     * <li>PCR index</li>
     * <li>SHA-1 hash</li>
     * </ul>
     *
     * @param baselineName
     *            of baseline to generate
     * @param in
     *            is input stream containing PCR measurement entries
     * @return tpm baseline
     * @throws IOException
     *             if error encountered reading data from input stream
     * @throws ParseException
     *             if error encountered parsing data
     * @throws TPMBaselineGeneratorException
     *             if error encountered when retrieving measurement entries from
     *             input stream
     */
    public final TpmWhiteListBaseline generateWhiteListBaselineFromCSVFile(
            final String baselineName, final InputStream in)
            throws IOException, ParseException, TPMBaselineGeneratorException {
        checkBaselineCreationArgs(baselineName, in);

        TpmWhiteListBaseline tpmBaseline = parseWhiteListCsvFile(baselineName, in);
        LOGGER.debug("measurement baseline initialized: {}", tpmBaseline);
        return tpmBaseline;
    }

    /**
     * Method generates a TPM measurement black list baseline from a .csv file containing
     * PCR measurement entries. An off-line PCR measurement baseline process is
     * used to generate the .csv file. TPM measurement records are expected to
     * adhere to the following record structure with a comma separating them:
     * <ul>
     * <li>PCR index</li>
     * <li>SHA-1 hash</li>
     * </ul>
     *
     * @param baselineName
     *            of baseline to generate
     * @param in
     *            is input stream containing PCR measurement entries
     * @return tpm baseline
     * @throws IOException
     *             if error encountered reading data from input stream
     * @throws ParseException
     *             if error encountered parsing data
     * @throws TPMBaselineGeneratorException
     *             if error encountered when retrieving measurement entries from
     *             input stream
     */
    public TpmBlackListBaseline generateBlackListBaselineFromCSVFile(final String baselineName,
         final InputStream in) throws ParseException, TPMBaselineGeneratorException, IOException {
        checkBaselineCreationArgs(baselineName, in);

        TpmBlackListBaseline tpmBaseline = parseBlackListCsvFile(baselineName, in);
        LOGGER.debug("measurement baseline initialized: {}", tpmBaseline);
        return tpmBaseline;
    }

    private void checkBaselineCreationArgs(final String baselineName, final InputStream in) {
        if (baselineName == null) {
            LOGGER.error("null argument: baselineName");
            throw new NullPointerException("baselineName");
        }
        if (in == null) {
            LOGGER.error("null argument: in");
            throw new NullPointerException("in");
        }
    }

    /**
     * Generates a TPM baseline from a <code>IntegrityReport</code>. This
     * extracts the PCR composite object from the report and verifies that valid
     * number of TPM PCRs are provided. The method creates a new
     * <code>TPMBaseline</code> object and then uses each PCR value obtained
     * from the PCR composite object to add it to the <code>TPMBaseline</code>
     * object.
     *
     * @param baselineName
     *            name of baseline to be created
     * @param report
     *            integrity report that containing PCR values for baseline
     * @return baseline that contains the PCR values from the report
     * @throws NullPointerException
     *             if either baselineName or report parameters are null.
     */
    public final TpmWhiteListBaseline generateBaselineFromIntegrityReport(
            final String baselineName, final IntegrityReport report)
            throws NullPointerException {

        if (baselineName == null) {
            LOGGER.error("null argument: baselineName");
            throw new NullPointerException("baselineName");
        }
        if (report == null) {
            LOGGER.error("null argument: report");
            throw new NullPointerException("report");
        }

        final TpmWhiteListBaseline baseline = new TpmWhiteListBaseline(baselineName);

        LOGGER.debug("generating TPM baseline from report");

        if (report.contains(TPMReport.class)) {
            final TPMReport tpmReport = report.extractReport(TPMReport.class);
            final List<TPMMeasurementRecord> pcrs = tpmReport.getTPMMeasurementRecords();
            for (TPMMeasurementRecord p : pcrs) {
                int id = p.getPcrId();
                final Digest sha1 = p.getHash();
                baseline.addToBaseline(new TPMMeasurementRecord(id, sha1));
            }
        } else {
            LOGGER.debug(String.format(
                    "In generateBaselineFromIntegrityReport of %s, "
                            + "the IntegrityReport did not include a TPMReport",
                    getClass().getSimpleName()));
        }

        LOGGER.debug("retrieving Device Info data from report");

        if (report.contains(DeviceInfoReport.class)) {
            Report reportBuffer = report.extractReport(DeviceInfoReport.class);
            final DeviceInfoReport diReport = (DeviceInfoReport) reportBuffer;
            baseline.setFirmwareInfo(diReport.getFirmwareInfo());
            baseline.setHardwareInfo(diReport.getHardwareInfo());
            baseline.setOSInfo(diReport.getOSInfo());
            baseline.setTPMInfo(diReport.getTPMInfo());
        } else {
            LOGGER.debug(String.format(
                    "In generateBaselineFromIntegrityReport of %s, "
                            + "the IntegrityReport did not include a DeviceInfoReport",
                    getClass().getSimpleName()));
        }

        return baseline;
    }

    /**
     * Generates a TPM whitelist baseline from a <code>IntegrityReport</code>. This
     * extracts the PCR composite object from the report and verifies that valid
     * number of TPM PCRs are provided. The method creates a new
     * <code>TPMBaseline</code> object and then uses each PCR value obtained
     * from the PCR composite object to add it to the <code>TPMBaseline</code>
     * object.
     * @param baselineName name of baseline to be created
     * @param report integrity report that containing PCR values for baseline
     * @param kernelPcrMask the kernel PCR mask from a TPM Policy
     * @return baseline that contains the PCR values from the report and the device info
     * @throws NullPointerException if either baselineName or report parameters are null.
     */
    public final TpmWhiteListBaseline generateWhiteListBaselineOnKernelUpdate(
            final String baselineName, final IntegrityReport report, final int kernelPcrMask)
            throws NullPointerException {
        if (baselineName == null) {
            LOGGER.error("null argument: baselineName");
            throw new NullPointerException("baselineName");
        }

        LOGGER.debug("generating TPM baseline on kernel update from report");

        // Generate a temporary reference baseline from the given report.  The name can be blank.
        final TpmWhiteListBaseline referenceBaseline =
                generateBaselineFromIntegrityReport("", report);
        final Set<TPMMeasurementRecord> records = referenceBaseline.getPcrRecords();

        final TpmWhiteListBaseline newBaseline = new TpmWhiteListBaseline(baselineName);

        // Copy each of the kernel PCR values from the reference baseline to the new one
        for (final TPMMeasurementRecord record : records) {
            final int shifted = 1 << record.getPcrId();
            if ((shifted & kernelPcrMask) == shifted) {
                newBaseline.addToBaseline(record);
            }
        }

        LOGGER.debug("retrieving Device Info data from referenced baseline");

        // Copy the criteria from the device info report corroborated the kernel update.
        final OSInfo referenceOSInfo = referenceBaseline.getOSInfo();
        final HashMap<TPMBaselineFields, String> map = new HashMap<>();
        map.put(TPMBaselineFields.OS_NAME, referenceOSInfo.getOSName());
        map.put(TPMBaselineFields.OS_VERSION, referenceOSInfo.getOSVersion());
        final OSInfo osInfo = TPMBaselineFields.toOSInfo(map, new OSInfo());
        newBaseline.setOSInfo(osInfo);

        return newBaseline;
    }

    /**
     * Provides a standard name for baselines that are automatically created during a kernel
     * update event.
     * @param report IntegrityReport from which a kernel update was detected.
     * @return String of the name that can be used for a corresponding TPM baseline.
     * @throws NullPointerException If the parameter is null.
     * @throws TPMBaselineGeneratorException If the report doesn't contain a device info report.
     */
    public final String generateNameForKernelUpdateBaseline(final IntegrityReport report)
        throws NullPointerException, TPMBaselineGeneratorException {
        LOGGER.debug("retrieving Device Info data to create kernel update baseline name");
        if (report == null) {
            LOGGER.error("null argument: report");
            throw new NullPointerException("report");
        }

        if (report.contains(DeviceInfoReport.class)) {
            final DeviceInfoReport diReport = report.extractReport(DeviceInfoReport.class);

            final OSInfo osInfo = diReport.getOSInfo();
            return String.format(KERNEL_UPDATE_BASELINE_NAME,
                osInfo.getOSName(), osInfo.getOSVersion());
        }

        final String msg = "The integrity report did not contain a device info report."
                + " Investigate how the appraiser got this far.";
        throw new TPMBaselineGeneratorException(msg);
    }

    /**
     * Method parses a .csv file containing TPM measurement entries and
     * initializes and returns a new TPM baseline object.
     *
     * @param baselineName the name of the baseline.
     * @param inStream
     *            containing file contents to be read. inStream is closed by
     *            this method.
     * @return a TPM baseline initialized with measurement entries imported from
     *         .csv TPM baseline file
     * @exception NullPointerException
     *                if baselineName is a null value
     * @exception NullPointerException
     *                if inStream is a null value
     * @throws IOException
     *             if error encountered reading data from input stream
     * @throws ParseException
     *             if error encountered parsing data
     * @throws TPMBaselineGeneratorException
     *             if error encountered when retrieving measurement entries from
     *             input stream
     */
    private TpmWhiteListBaseline parseWhiteListCsvFile(final String baselineName,
          final InputStream inStream) throws IOException, ParseException,
            TPMBaselineGeneratorException {
        TpmWhiteListBaseline baseline = new TpmWhiteListBaseline(baselineName);

        parseBaseline(baselineName, inStream, baseline);
        return baseline;
    }

    private TpmBlackListBaseline parseBlackListCsvFile(final String baselineName,
                   final InputStream inStream) throws IOException, ParseException,
            TPMBaselineGeneratorException {
        TpmBlackListBaseline baseline = new TpmBlackListBaseline(baselineName);

        parseBaseline(baselineName, inStream, baseline);
        return baseline;
    }

    private void parseBaseline(final String baselineName, final InputStream inStream,
                               final TPMBaseline baseline)
            throws IOException, TPMBaselineGeneratorException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                inStream, "UTF8"));
        String dataRow = reader.readLine();
        HashMap<TPMBaselineFields, String> fieldMap = new HashMap<>();

        try {
            while (dataRow != null && dataRow.matches(VALID_REGEX)) {
                String[] dataArray = dataRow.split(",", 2); // looking for two values per row
                if (dataArray.length != 2) { // could be 1, if there were no commas
                    final String msg = String.format(
                            "invalid number of fields: %d", dataArray.length);
                    LOGGER.error(msg);
                    throw new TPMBaselineGeneratorException(msg);
                } else if (!dataArray[1].matches(VALID_REGEX)) {
                    final String msg = String.format("One record contained invalid data"
                            + "while parsing a CSV file for TPM Baseline '%s'.", baselineName);
                    LOGGER.error(msg);
                    throw new TPMBaselineGeneratorException(msg);
                }

                // Measurements will start with a number,
                // Device info records will start with the field name of the device info to set
                try {
                    TPMBaselineFields field =
                        TPMBaselineFields.valueOf(dataArray[0].toUpperCase());
                    fieldMap.put(field, StringEscapeUtils.unescapeCsv(dataArray[1]));
                } catch (IllegalArgumentException e) {
                    // Wasn't in the list of fields, treat it as a measurement record
                    int id = Integer.parseInt(dataArray[0]);
                    final byte[] sha1Bytes
                             = Hex.decodeHex(dataArray[1].toCharArray());
                    final Digest sha1 = new Digest(DigestAlgorithm.SHA1, sha1Bytes);
                    baseline.addToBaseline(new TPMMeasurementRecord(id, sha1));
                }

                dataRow = reader.readLine();
            }

            // Use the map to overwrite new device info data
            baseline.setFirmwareInfo(
                TPMBaselineFields.toFirmwareInfo(fieldMap, baseline.getFirmwareInfo()));
            baseline.setHardwareInfo(
                TPMBaselineFields.toHardwareInfo(fieldMap, baseline.getHardwareInfo()));
            baseline.setOSInfo(
                TPMBaselineFields.toOSInfo(fieldMap, baseline.getOSInfo()));
            baseline.setTPMInfo(
                TPMBaselineFields.toTPMInfo(fieldMap, baseline.getTPMInfo()));

            if (baseline.isEmpty()) {
                throw new TPMBaselineGeneratorException("TPM baseline is empty!");
            }
        //Checks that PCR values are actual
        } catch (NumberFormatException nfe) {
            String recordInfo = "";
            if (dataRow != null) {
                recordInfo = " record: \"" + dataRow + "\"";
            }
            String msg = "TPMBaselineGenerator.parseWhiteListCsvFile: Error when attempting to "
                    + "parse a number in CSV file" + recordInfo + ".";
            LOGGER.error(msg + "\n" + nfe.getMessage());
            throw new TPMBaselineGeneratorException(msg);
        } catch (IllegalArgumentException iae) {
            //Removes the Exception header to hide it from the user explanation
            LOGGER.error(iae.getMessage());
            String message = iae.getMessage().substring(
                    iae.getMessage().indexOf(":") + 1);
            String error = "Baseline import failed due to "
                    + message.replaceAll("(.*):(.*)", "$1");
            throw new TPMBaselineGeneratorException(error);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            throw new TPMBaselineGeneratorException("Error when attempting to "
                    + "parse CSV file.  Is file formatted correctly for a TPM"
                    + " Baseline?");
        } finally {
            reader.close();
        }
    }
}
