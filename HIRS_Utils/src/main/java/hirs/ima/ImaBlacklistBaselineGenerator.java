package hirs.ima;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import hirs.data.persist.Digest;
import hirs.data.persist.baseline.ImaBlacklistBaseline;
import hirs.data.persist.ImaBlacklistRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class provides methods for generating {@link ImaBlacklistBaseline}s as serialized
 * by {@link CSVGenerator}.
 */
public final class ImaBlacklistBaselineGenerator {
    private static final int NUM_CSV_FIELDS = 3;

    /**
     * This is a utility class that should not be constructed.
     */
    private ImaBlacklistBaselineGenerator() {

    }

    /**
     * Creates a new {@link ImaBlacklistBaseline} instance from the provided CSV input.
     *
     * @param baselineName the name that the resultant baseline should have
     * @param inputStream input stream containing the CSV data
     * @return the resulting blacklist baseline
     * @throws IOException if there is a problem deserializing the baseline records
     */
    public static ImaBlacklistBaseline generateBaselineFromCSV(
            final String baselineName,
            final InputStream inputStream
    ) throws IOException {
        Preconditions.checkArgument(
                !StringUtils.isBlank(baselineName),
                "Cannot generate a baseline with a blank name"
        );

        ImaBlacklistBaseline blacklistBaseline = new ImaBlacklistBaseline(baselineName);
        updateBaselineFromCSVFile(blacklistBaseline, inputStream);
        return blacklistBaseline;
    }

    /**
     * Adds blacklist baseline records from the provided CSV input to the given
     * {@link ImaBlacklistBaseline} instance.
     *
     * @param baseline the baseline whose blacklist records should be updated
     * @param inputStream input stream containing the CSV data
     * @throws IOException if there is a problem deserializing the baseline records
     */
    public static void updateBaselineFromCSVFile(
            final ImaBlacklistBaseline baseline,
            final InputStream inputStream
    ) throws IOException {
        Preconditions.checkNotNull(baseline, "Cannot update null baseline");
        Preconditions.checkNotNull(inputStream, "Cannot update from null input");

        BufferedReader buffReader = new BufferedReader(new InputStreamReader(
                inputStream, Charsets.UTF_8
        ));
        CSVParser parser = CSVFormat.DEFAULT.parse(buffReader);

        try {
            for (CSVRecord record : parser.getRecords()) {
                if (record.size() != NUM_CSV_FIELDS) {
                    throw new IOException(String.format(
                            "Expected %d fields for record %s", NUM_CSV_FIELDS, record.toString()
                    ));
                }

                String path = StringUtils.defaultIfBlank(record.get(0), null);
                String description = StringUtils.defaultIfBlank(record.get(2), null);
                Digest digest = null;

                if (!StringUtils.isBlank(record.get(1))) {
                    digest = Digest.fromString(record.get(1));
                }

                baseline.addToBaseline(new ImaBlacklistRecord(path, digest, description));
            }
        } catch (IOException e) {
            throw e;
        } finally {
            parser.close();
            buffReader.close();
        }
    }
}
