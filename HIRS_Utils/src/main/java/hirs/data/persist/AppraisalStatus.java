package hirs.data.persist;

/**
 * Class to capture appraisal results and corresponding messages.
 */
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

    /**
     * Default constructor. Set appraisal status and description.
     * @param appStatus status of appraisal
     * @param message description of result
     */
    public AppraisalStatus(final Status appStatus, final String message) {
        this.appStatus = appStatus;
        this.message = message;
    }

    /**
     * Get appraisal status.
     * @return appraisal status
     */
    public Status getAppStatus() {
        return appStatus;
    }

    /**
     * Set appraisal status.
     * @param appStatus new status
     */
    public void setAppStatus(final Status appStatus) {
        this.appStatus = appStatus;
    }

    /**
     * Get appraisal description message.
     * @return appraisal description message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set appraisal description message.
     * @param message appraisal description message
     */
    public void setMessage(final String message) {
        this.message = message;
    }
}
