package hirs.attestationca.persist.enums;

import lombok.Getter;
import lombok.Setter;

/**
 * Class to capture appraisal results and corresponding messages.
 */
@Getter
@Setter
public class AppraisalStatus {
    /**
     * Enum used to represent appraisal status.
     */
    public enum Status {

        /**
         * Represents a passing appraisal.
         */
        PASS,

        /**
         * Represents a failed appraisal.
         */
        FAIL,

        /**
         * Represents an appraisal generation error.
         */
        ERROR,
        /**
         * Represents an unknown appraisal result.
         */
        UNKNOWN
    }

    private Status appStatus;
    private String message;
    private String additionalInfo;

    /**
     * Default constructor. Set appraisal status and description.
     * @param appStatus status of appraisal
     * @param message description of result
     */
    public AppraisalStatus(final Status appStatus, final String message) {
        this(appStatus, message, "");
    }

    /**
     * Default constructor. Set appraisal status and description.
     * @param appStatus status of appraisal
     * @param message description of result
     * @param additionalInfo any additional information needed to
     *                       be passed on
     */
    public AppraisalStatus(final Status appStatus, final String message,
                           final String additionalInfo) {
        this.appStatus = appStatus;
        this.message = message;
        this.additionalInfo = additionalInfo;
    }
}
