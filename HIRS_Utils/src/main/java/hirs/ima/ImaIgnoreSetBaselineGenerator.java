package hirs.ima;

import hirs.data.persist.baseline.ImaIgnoreSetBaseline;
import hirs.data.persist.ImaIgnoreSetRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
public class ImaIgnoreSetBaselineGenerator {

    private static final Logger LOGGER
            = LogManager.getLogger(ImaIgnoreSetBaseline.class);
    private static final int PATH_INDEX = 0;
    private static final int DESCRIPTION_INDEX = 1;

    /**
     * Method generates an IMA measurement baseline from a .csv file containing
     * IMA baseline records. An IMA record consists of properties associated
     * with an IMA measured file such as file hash, and path. An off-line IMA
     * measurement baseline process is used to create the .csv file. IMA
     * baseline records are expected to adhere to the following record
     * structure separated by commas:
     * <ul>
     * <li>file Path (includes file name)</li>
     * <li>Description of why path was chosen to be ignored</li>
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
     * @throws ImaIgnoreSetBaselineGeneratorException
     *             if error encountered when retrieving baseline records from
     *             input stream
     */
    public final ImaIgnoreSetBaseline generateBaselineFromCSVFile(
            final String baselineName, final InputStream in)
            throws IOException, ParseException, ImaIgnoreSetBaselineGeneratorException {
        if (baselineName == null) {
            LOGGER.error("null argument: baselineName");
            throw new NullPointerException("baselineName");
        }
        ImaIgnoreSetBaseline ignoreBaseline = new ImaIgnoreSetBaseline(baselineName);
        updateBaselineFromCSVFile(ignoreBaseline, in);
        LOGGER.debug("meaurement baseline initialized: {}", ignoreBaseline.getName());
        return ignoreBaseline;
    }

    /**
     * Method parses a .csv file containing IMA ignore baseline records and adds then to
     * a IMA ignore baseline object.
     *
     * @param baseline The baseline to be updated with records from the stream
     * @param inStream
     *            containing file contents to be read. inStream is closed by
     *            this method.
     * @throws IOException
     *             if error encountered reading data from input stream
     * @throws ParseException
     *             if error encountered parsing data
     * @throws ImaIgnoreSetBaselineGeneratorException
     *             if error encountered when retrieving baseline records from
     *             input stream
     */
    public final void updateBaselineFromCSVFile(final ImaIgnoreSetBaseline baseline,
            final InputStream inStream) throws IOException, ParseException,
            ImaIgnoreSetBaselineGeneratorException {

        if (baseline == null) {
            LOGGER.error("null argument: baseline");
            throw new NullPointerException("baseline");
        }
        if (inStream == null) {
            LOGGER.error("null argument: in");
            throw new NullPointerException("in");
        }

        // (tmmcgil) I realize this is not the most robust way to parse, but it works
        // better than what was here before and it doesn't require a lot of changes.
        String regex = "\"?(.*?)\"?,(.*)";
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
                throw new ImaIgnoreSetBaselineGeneratorException(msg);
            }

            String path = m.group(1);
            path = path.replace("\"\"", "\"");

            String description = m.group(2);
            if (StringUtils.defaultString(description).equalsIgnoreCase("")) {
                description = null;
            }
            final ImaIgnoreSetRecord imaRecord = new ImaIgnoreSetRecord(path,
                    description);
            baseline.addToBaseline(imaRecord);
            final String msg = String.format("added record %s", imaRecord);
            LOGGER.debug(msg);
        }
        reader.close();
    }
}
