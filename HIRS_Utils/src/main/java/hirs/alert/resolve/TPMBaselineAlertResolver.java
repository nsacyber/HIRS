package hirs.alert.resolve;

import hirs.data.persist.Alert;
import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.baseline.TPMBaseline;
import hirs.data.persist.TPMMeasurementRecord;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract class providing common functionality for TPM Baseline alert resolvers.
 */
public abstract class TPMBaselineAlertResolver extends BaselineAlertResolver {

    private static final Logger LOGGER = LogManager.getLogger(TPMBaselineAlertResolver.class);

    /**
     * The TPM baseline to be modified by this resolver.
     */
    @SuppressWarnings("checkstyle:visibilitymodifier")
    protected TPMBaseline tpmBaseline = null;

    /**
     * Casts BaselineAlertResolver's baseline to a TPMBaseline.
     *
     * @return true to indicate success
     */
    @Override
    protected boolean beforeLoop() {
        if (super.beforeLoop()) {
            tpmBaseline = (TPMBaseline) baseline;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates the baseline.
     *
     * @return true if successful
     */
    @Override
    protected boolean afterLoop() {
        if (super.afterLoop()) {
            baselineManager.updateBaseline(baseline);
            return true;
        } else {
            return false;
        }
    }

    // Regex patterns used to parse expected/received Strings in Alerts into records
    private static final Pattern TPM_PCR_PATTERN = Pattern.compile(".*#([0-9]{1,2})");
    private static final Pattern TPM_DIGEST_PATTERN = Pattern.compile("(.*:|or).(([a-f]|[0-9])*)");

    /**
     * Parses the record of an Alert into a TPMMeasurementRecord.
     *
     * @param alert - Alert to be resolved
     * @param record - record chosen to be parsed
     * @return TPMMeasurementRecord based on the TPM record and alert information
     */
    protected Set<TPMMeasurementRecord> parseTpmRecords(final Alert alert, final String record) {

        // parse the PCR values out of the Alert details String
        Integer pcr = null;
        final Matcher pcrMatcher = TPM_PCR_PATTERN.matcher(alert.getDetails());
        if (pcrMatcher.find()) {
            pcr = Integer.parseInt(pcrMatcher.group(1));
        }

        // creates digest from String
        Set<Digest> digests = new HashSet<>();
        final Matcher digestMatcher = TPM_DIGEST_PATTERN.matcher(record);
        boolean digestError = false;
        while (digestMatcher.find()) {
            final String match = digestMatcher.group(2);
            if (StringUtils.isBlank(match)) {
                addError("Invalid syntax or digest value in record '" + record + "' for alert ["
                        + alert.getId() + "].");
                digestError = true;
            } else {
                try {
                    final byte[] digestBytes = Hex.decodeHex(match.toCharArray());
                    final Digest digest = new Digest(DigestAlgorithm.SHA1, digestBytes);
                    digests.add(digest);
                } catch (DecoderException ex) {
                    final String msg = "Error decoding TPM record digest '" + match + "' in alert ["
                            + alert.getId() + "]: " + getMessage(ex);
                    addError(msg);
                    digestError = true;
                }
            }
        }

        // check for errors
        if (pcr == null || digests.isEmpty()) {
            if (pcr == null) {
                addError("Could not parse PCR from details '" + alert.getDetails() + "' in alert ["
                        + alert.getId() + "].");
            }
            if (digests.isEmpty() && !digestError) {
                addError("Could not parse digest from record '" + record + "' in alert ["
                        + alert.getId() + "].");
            }
            return null;
        }

        // create records
        final Set<TPMMeasurementRecord> measurementRecords = new HashSet<>();
        for (Digest digest : digests) {
            measurementRecords.add(new TPMMeasurementRecord(pcr, digest));
        }
        return measurementRecords;

    }

}
