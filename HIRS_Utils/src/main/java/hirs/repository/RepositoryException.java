package hirs.repository;

/**
 * An exception representing an error processing a Repository.
 *
 */
public class RepositoryException extends Exception {

    private static final long serialVersionUID = 755395737092446396L;

    /**
     * Create a new RepositoryException.
     * @param message the exception message
     */
    public RepositoryException(final String message) {
        super(message);
    }

    /**
     * Create a new RepositoryException.
     * @param exception the exception
     */
    public RepositoryException(final Exception exception) {
        super(exception);
    }

    /**
     * Create a new RepositoryException.
     * @param message the exception message
     * @param exception the exception
     */
    public RepositoryException(final String message, final Exception exception) {
        super(message, exception);
    }

}
