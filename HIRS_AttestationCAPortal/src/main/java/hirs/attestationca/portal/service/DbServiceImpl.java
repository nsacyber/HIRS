package hirs.attestationca.portal.service;

public class DbServiceImpl {
    /**
     * The default maximum number of retries to attempt a database transaction.
     */
    public static final int DEFAULT_MAX_RETRY_ATTEMPTS = 10;
    /*
     * The default number of milliseconds to wait before retrying a database transaction.
     */
    private static final long DEFAULT_RETRY_WAIT_TIME_MS = 3000;

    // structure for retrying methods in the database
//    private RetryTemplate retryTemplate;
}
