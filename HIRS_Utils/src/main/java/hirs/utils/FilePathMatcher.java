package hirs.utils;

/**
 * A utility class that determines whether or not a given file path (represented as a
 * <code>String</code>) matches any of a given array of user-provided input patterns.
 *
 */
public interface FilePathMatcher {

    /**
     * Determine whether or not a given file path (represented as a <code>String</code>) matches
     * this <code>FilePathMatcher</code>.
     *
     * @param path
     *          a <code>String</code> to attempt a match against
     * @return
     *          whether or not a successful match occurred
     */
    boolean isMatch(String path);

    /**
     * Set this <code>FilePathMatcher</code>'s array of input patterns.
     *
     * @param patterns
     *            non-empty <code>Array</code> of <code>String</code>s to match against
     */
    void setPatterns(String... patterns);
}
