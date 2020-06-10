package hirs.data.persist.enums;

import javax.xml.bind.annotation.XmlType;

/**
 * The 'type' of the Alert, which is the category of problem identified by the
 * 'source'.
 */
@XmlType(name = "AlertType")
public enum AlertType {

    /**
     * The alert type has not been specified.
     */
    UNSPECIFIED,
    /**
     * The <code>Report</code> does not contain the necessary elements or it
     * contains certain unnecessary elements.
     */
    MALFORMED_REPORT,
    /**
     * The <code>Report</code> does not contain the correct
     * <code>TPMMeasurementRecord</code>s or the PCR values are not correct.
     */
    WHITE_LIST_PCR_MISMATCH,
    /**
     * The <code>Report</code> contains a <code>TPMMeasurementRecord</code>
     * matching a TPM BlackList.
     */
    BLACK_LIST_PCR_MATCH,
    /**
     * The <code>TPMReport</code> does not contain a valid nonce.
     */
    INVALID_NONCE,
    /**
     * The <code>TPMReport</code> does not contain a valid TPM Quote (PCR
     * Digest).
     */
    INVALID_TPM_QUOTE,
    /**
     * The <code>TPMReport</code> does not contain a valid signature.
     */
    INVALID_SIGNATURE,
    /**
     * The <code>TPMReport</code> does not contain a valid certificate.
     */
    INVALID_CERTIFICATE,
    /**
     * The <code>IMAReport</code> contains a whitelist hash mismatch.
     */
    WHITELIST_MISMATCH,
    /**
     * The <code>IMAReport</code> contains a required set hash mismatch.
     */
    REQUIRED_SET_MISMATCH,
    /**
     * The <code>Report</code> is missing a required record.
     */
    MISSING_RECORD,
    /**
     * The <code>IMAReport</code> contains an unknown filepath.
     */
    UNKNOWN_FILE,
    /**
     * The client's <code>ReportRequest</code> query messages missing.
     */
    REPORT_REQUESTS_MISSING,
    /**
     * Client periodic <code>IntegrityReport</code> missing.
     */
    PERIODIC_REPORT_MISSING,
    /**
     * On-demand <code>IntegrityReport</code> missing.
     */
    ON_DEMAND_REPORT_MISSING,
    /**
     * The client sent a report that indicates IMA was not enabled correctly.
     */
    IMA_MISCONFIGURED,
    /**
     * PCR mismatches and device info changes indicated a kernel update.
     */
    KERNEL_UPDATE_DETECTED,
    /**
     * The <code>Report</code> does not contain the correct
     * <code>TPMMeasurementRecord</code>s associated with IMA measurements.
     */
    IMA_PCR_MISMATCH,
    /**
     * Indicates an IMA measurement had a path which matched an entry in a
     * blacklist baseline.
     */
    IMA_BLACKLIST_PATH_MATCH,
    /**
     * Indicates an IMA measurement had a hash which matched an entry in a
     * blacklist baseline.
     */
    IMA_BLACKLIST_HASH_MATCH,
    /**
     * Indicates an IMA measurement had both a path and hash which matched an
     * entry in a blacklist baseline.
     */
    IMA_BLACKLIST_PATH_AND_HASH_MATCH,
    /**
     * Indicates an IMA measurement had a path that matched an entry in a
     * blacklist baseline, and also had a hash that matched another entry in the
     * same (or another) baseline.
     */
    IMA_BLACKLIST_MIXED_MATCH
}
