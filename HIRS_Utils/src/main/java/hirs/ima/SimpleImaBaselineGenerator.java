package hirs.ima;

import hirs.data.persist.IMAReport;
import hirs.data.persist.baseline.SimpleImaBaseline;
import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.baseline.ImaBaseline;
import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.IntegrityReport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class used to process comma separated value measurement files or HIRS
 * integrity reports to generate an IMA measurement baseline. Generating an IMA
 * measurement baseline from a HIRS integrity report is considered experimental
 * and provided as a tool to assist with creating, analyzing, and refining
 * measurement baselines.
 */
public class SimpleImaBaselineGenerator {

    private static final Logger LOGGER
            = LogManager.getLogger(ImaBaseline.class);

    /**
     * Method generates an IMA measurement baseline from a .csv file containing
     * IMA baseline records. An IMA record consists of properties associated
     * with an IMA measured file such as file hash, and path. An off-line IMA
     * measurement baseline process is used to create the .csv file. IMA
     * baseline records are expected to adhere to the following record
     * structure separated by commas:
     * <ul>
     * <li>file Path (includes file name)</li>
     * <li>SHA1 of file</li>
     * </ul>
     *
     * @param baselineName
     *            name applied to baseline
     * @param in
     *            is input stream containing IMA baseline records
     * @return IMAbaseline
     * @throws IOException
     *             if error encountered reading data from input stream
     * @throws ParseException
     *             if error encountered parsing data
     * @throws IMABaselineGeneratorException
     *             if error encountered when retrieving baseline records from
     *             input stream
     */
    public final SimpleImaBaseline generateBaselineFromCSVFile(
            final String baselineName, final InputStream in)
            throws IOException, ParseException, IMABaselineGeneratorException {
        if (baselineName == null) {
            LOGGER.error("null argument: baselineName");
            throw new NullPointerException("baselineName");
        }
        SimpleImaBaseline imaBaseline = new SimpleImaBaseline(baselineName);
        updateBaselineFromCSVFile(imaBaseline, in);
        LOGGER.debug("meaurement baseline initialized: {}", imaBaseline.getName());
        return imaBaseline;
    }

    /**
     * Produces IMA baseline from IMA report. The method extracts the list of
     * IMA records from the trusted report then it uses each IMA report record
     * to construct the baseline. Note: The HIRS portal selects an IMA report
     * to use as a seed to generate IMA baseline, the IMA report should be
     * generated with sufficient (complete list of files measurements) and
     * should be in trusted state to use as basis for baseline. However, IMA
     * baselines generated from measurement reports are expected to have high
     * rate of false positives because many alerts will be generated for files
     * that change every boot cycle that need to be filtered by system
     * operators that will build expertise regarding IMA measurements in their
     * specific deployment configuration, and who will utilize the portal to
     * adjust the baseline to identify all files that should have policy set
     * to ignore hash. there are many other cases that may cause false
     * positive which will require human in the loop also to refine the
     * produced baseline.
     *
     * @param baselineName
     *            name of the new baseline
     * @param report
     *            IMAReport instance of IMA integrity report object. The IMA
     *            Integrity report is extracted from a host integrity report.
     *
     * @return ImaBaseline
     * @throws IMABaselineGeneratorException
     *             if we find no IMA records in the trusted report that method
     *             should use to generate IMA baseline.
     * @throws NullPointerException
     *             if baselineName or report parameters are null.
     */
    public final SimpleImaBaseline generateBaselineFromIMAReport(
            final String baselineName, final IMAReport report)
            throws IMABaselineGeneratorException, NullPointerException {

        IMABaselineRecord imaBaselineRecord;

        if (baselineName == null) {
            LOGGER.error("null argument: baselineName");
            throw new NullPointerException("baselineName");
        }

        if (report == null) {
            LOGGER.error("null argument: report");
            throw new NullPointerException("report");
        }

        if (report.getRecords().size() < 1) {
            String msg;
            msg = "no IMA records in the report";
            LOGGER.error(msg);
            throw new IMABaselineGeneratorException(msg);
        }

        SimpleImaBaseline newBaseline = new SimpleImaBaseline(baselineName);
        for (IMAMeasurementRecord record : report.getRecords()) {
            imaBaselineRecord = new IMABaselineRecord(record.getPath(), record.getHash());
            newBaseline.addToBaseline(imaBaselineRecord);
        }

        return newBaseline;
    }

    /**
     * Produces IMA baseline from a HIRS full integrity report that complies
     * with HIRS new XML report format. The method the IMA report from the full
     * integrity report, then it extracts the the list of IMA records from the
     * trusted report then it uses each IMA report record to construct the IMA
     * baseline.
     * @param baselineName
     *            name of the new baseline
     * @param report
     *            IMAReport instance of IMA integrity report object. The IMA
     *            Integrity report is extracted from a host integrity report.
     *
     * @return ImaBaseline
     * @throws IMABaselineGeneratorException
     *             if no IMA report, or IMA records, in the trusted integrity
     *             report passed to the method to generate IMA baseline.
     * @throws NullPointerException
     *             if baselineName or report parameters are null.
     */
    public final SimpleImaBaseline generateBaselineFromIntegrityReport(
            final String baselineName, final IntegrityReport report)
            throws IMABaselineGeneratorException, NullPointerException {
        IMABaselineRecord imaBaselineRecord;

        if (baselineName == null) {
            LOGGER.error("null argument: baselineName");
            throw new NullPointerException("baselineName");
        }

        if (report == null) {
            LOGGER.error("null argument: report");
            throw new NullPointerException("report");
        }

        if (!report.contains(IMAReport.class)) {
            String msg = "no IMA report in the integrity report";
            LOGGER.error(msg);
            throw new IMABaselineGeneratorException(msg);
        }

        IMAReport imaReport = report.extractReport(IMAReport.class);
        if (imaReport.getRecords().size() < 1) {
            String msg = "no IMA records in the report";
            LOGGER.error(msg);
            throw new IMABaselineGeneratorException(msg);
        }
        SimpleImaBaseline newBaseline = new SimpleImaBaseline(baselineName);
        for (IMAMeasurementRecord imaRecord : imaReport.getRecords()) {
            imaBaselineRecord = new IMABaselineRecord(imaRecord.getPath(),
                    imaRecord.getHash());
            newBaseline.addToBaseline(imaBaselineRecord);
        }
        return newBaseline;
    }

    /**
     * Method parses a .csv file containing IMA baseline records and adds then to
     * a IMA baseline object.
     *
     * @param baseline The baseline to be updated with records from the stream
     * @param inStream
     *            containing file contents to be read. inStream is closed by
     *            this method.
     * @throws IOException
     *             if error encountered reading data from input stream
     * @throws ParseException
     *             if error encountered parsing data
     * @throws IMABaselineGeneratorException
     *             if error encountered when retrieving baseline records from
     *             input stream
     */
    public final void updateBaselineFromCSVFile(final SimpleImaBaseline baseline,
            final InputStream inStream) throws IOException, ParseException,
            IMABaselineGeneratorException {

        if (baseline == null) {
            LOGGER.error("null argument: baseline");
            throw new NullPointerException("baseline");
        }
        if (inStream == null) {
            LOGGER.error("null argument: in");
            throw new NullPointerException("in");
        }

        String regex = "\"?(.*?)\"?,([a-fA-F0-9]{40})";
        Pattern p = Pattern.compile(regex);

        BufferedReader reader = new BufferedReader(new InputStreamReader(
                inStream, "UTF8"));
        String dataRow;

        while ((dataRow = reader.readLine()) != null) {
            if (StringUtils.isBlank(dataRow)) {
                continue;
            }
            Matcher m = p.matcher(dataRow);
            if (!m.matches() || m.groupCount() != 2) {
                final String msg = "row does not match regex: " + dataRow;
                LOGGER.error(msg);
                throw new IMABaselineGeneratorException(msg);
            }

            String path = m.group(1);
            path = path.replace("\"\"", "\"");

            try {
                final byte[] hash = Hex.decodeHex(m.group(2).toCharArray());
                final Digest digest = new Digest(DigestAlgorithm.SHA1, hash);
                final IMABaselineRecord imaRecord = new IMABaselineRecord(path,
                        digest);
                baseline.addToBaseline(imaRecord);
                final String msg = String.format("added record %s", imaRecord);
                LOGGER.debug(msg);
            } catch (DecoderException e) {
                String msg = "File " + path + " has invalid hash sting.  "
                        + "Record not added to baseline";
                LOGGER.error(msg, e);
            }
        }
        reader.close();
    }
}
