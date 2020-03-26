package hirs.ima;

import hirs.data.persist.Digest;
import hirs.data.persist.FirmwareInfo;
import hirs.data.persist.HardwareInfo;
import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.baseline.ImaAcceptableRecordBaseline;
import hirs.data.persist.baseline.ImaBlacklistBaseline;
import hirs.data.persist.ImaBlacklistRecord;
import hirs.data.persist.baseline.ImaIgnoreSetBaseline;
import hirs.data.persist.ImaIgnoreSetRecord;
import hirs.data.persist.OSInfo;
import hirs.data.persist.baseline.TPMBaseline;
import hirs.data.persist.TPMInfo;
import hirs.data.persist.TPMMeasurementRecord;
import hirs.tpm.TPMBaselineGenerator.TPMBaselineFields;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;

/**
 * Class used to generate CSV from baselines and ignore sets.  These utility functions are useful
 * for exporting the objects to files.
 */
public final class CSVGenerator {

    private static final Logger LOGGER = LogManager.getLogger(CSVGenerator.class);

    /**
     * Private constructor, should never be called.
     */
    private CSVGenerator() {
        // intentionally blank
    }

    /**
     * Returns all IMA records stored in IMABaselineRecords for IMABaseline.
     *
     * Adds logic to remove stray commas that were appearing in large baselines.
     *
     * @param imaBaseline baseline
     * @return CSV in a String
     */
    public static String imaRecordsToCsv(final ImaAcceptableRecordBaseline imaBaseline) {
        LOGGER.info("Retrieved and parsing all records");
        StringBuilder sb = new StringBuilder();
        for (IMABaselineRecord record : imaBaseline.getBaselineRecords()) {
            String stringHashValue;
            Digest theHash = record.getHash();
            if (theHash == null) {
                stringHashValue = "";
            } else {
                final byte[] digest = theHash.getDigest();
                final char[] hash = Hex.encodeHex(digest);
                stringHashValue = String.valueOf(hash);
            }

            sb.append("\"")
                    .append(record.getPath().replace("\"", "\"\""))
                    .append("\",")
                    .append(stringHashValue)
                    .append(System.lineSeparator());
        }
        return sb.toString();
    }

    /**
     * Returns all of the file paths of records that should be ignored.
     *
     * @param ignoreSetBaseline ignoreSetBaseline
     * @return CSV in a String
     */
    public static String ignoreSetToCsv(final ImaIgnoreSetBaseline ignoreSetBaseline) {
        LOGGER.info("Retrieved and parsing all records");
        StringBuilder sb = new StringBuilder();
        for (ImaIgnoreSetRecord record : ignoreSetBaseline.getImaIgnoreRecords()) {
            sb.append(record.getPath())
                    .append(",")
                    .append(StringUtils.defaultString(record.getDescription()))
                    .append(",")
                    .append(System.lineSeparator());
        }
        return sb.toString();
    }

    /**
     * Serializes an {@link ImaBlacklistBaseline} into a CSV file, whose rows represent
     * entries in the baseline.
     *
     * @param blacklistBaseline the baseline to serialize
     * @return the resulting CSV in a String
     */
    public static String blacklistToCsv(final ImaBlacklistBaseline blacklistBaseline) {
        StringBuilder sb = new StringBuilder();
        CSVPrinter csvPrinter;
        try {
            csvPrinter = CSVFormat.DEFAULT.withRecordSeparator(System.lineSeparator()).print(sb);
            for (ImaBlacklistRecord record : blacklistBaseline.getRecords()) {
                String digest = "";
                Digest hash = record.getHash();
                if (hash != null) {
                    digest = hash.toString();
                }
                csvPrinter.printRecord(
                        nullToEmpty(record.getPath()),
                        digest,
                        nullToEmpty(record.getDescription())
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not instantiate CSV printer");
        }
        return sb.toString();
    }

    /**
     * Returns the TPM records in the TPMBaseline.
     *
     * @param tpmBaseline tpmBaseline
     * @return CSV in a String
     */
    public static String tpmRecordsToCsv(final TPMBaseline tpmBaseline) {
        LOGGER.info("Retrieved and parsing all records");
        StringBuilder sb = new StringBuilder();
        // Add device info records to the map
        HashMap<TPMBaselineFields, String> map = new HashMap<TPMBaselineFields, String>();
        final FirmwareInfo firmwareInfo = tpmBaseline.getFirmwareInfo();
        map.put(TPMBaselineFields.biosvendor, firmwareInfo.getBiosVendor());
        map.put(TPMBaselineFields.biosversion, firmwareInfo.getBiosVersion());
        map.put(TPMBaselineFields.biosreleasedate, firmwareInfo.getBiosReleaseDate());
        final HardwareInfo hardwareInfo = tpmBaseline.getHardwareInfo();
        map.put(TPMBaselineFields.manufacturer, hardwareInfo.getManufacturer());
        map.put(TPMBaselineFields.productname, hardwareInfo.getProductName());
        map.put(TPMBaselineFields.version, hardwareInfo.getVersion());
        map.put(TPMBaselineFields.systemserialnumber, hardwareInfo.getSystemSerialNumber());
        map.put(TPMBaselineFields.chassisserialnumber, hardwareInfo.getChassisSerialNumber());
        map.put(TPMBaselineFields.baseboardserialnumber, hardwareInfo.getBaseboardSerialNumber());
        final OSInfo osInfo = tpmBaseline.getOSInfo();
        map.put(TPMBaselineFields.osname, osInfo.getOSName());
        map.put(TPMBaselineFields.osversion, osInfo.getOSVersion());
        map.put(TPMBaselineFields.osarch, osInfo.getOSArch());
        map.put(TPMBaselineFields.distribution, osInfo.getDistribution());
        map.put(TPMBaselineFields.distributionrelease, osInfo.getDistributionRelease());
        final TPMInfo tpmInfo = tpmBaseline.getTPMInfo();
        map.put(TPMBaselineFields.tpmmake, tpmInfo.getTPMMake());
        map.put(TPMBaselineFields.tpmversionmajor, "" + tpmInfo.getTPMVersionMajor());
        map.put(TPMBaselineFields.tpmversionminor, "" + tpmInfo.getTPMVersionMinor());
        map.put(TPMBaselineFields.tpmversionrevmajor, "" + tpmInfo.getTPMVersionRevMajor());
        map.put(TPMBaselineFields.tpmversionrevminor, "" + tpmInfo.getTPMVersionRevMinor());
        // Add device info records to the CSV file
        sb.append(TPMBaselineFields.toCSV(map));

        // Add measurement records to the CSV file
        for (TPMMeasurementRecord record : tpmBaseline.getPcrRecords()) {
            final byte[] digest = record.getHash().getDigest();
            final char[] hash = Hex.encodeHex(digest);
            sb.append(record.getPcrId())
                    .append(",")
                    .append(String.valueOf(hash))
                    .append(System.lineSeparator());
        }
        return sb.toString();
    }

    /**
     * Convers a null String to an empty String, for serialization purposes.
     *
     * @param str the input string
     * @return the same string, or an empty String if the original was null
     */
    public static String nullToEmpty(final String str) {
        if (str == null) {
            return "";
        }
        return str;
    }
}
