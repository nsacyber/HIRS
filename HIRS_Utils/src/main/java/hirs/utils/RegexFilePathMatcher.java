package hirs.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.RegexValidator;

/**
 *
 * An implementation of <code>FilePathMatcher</code> in which the match is determined using a
 * combination of initial substring comparison, full string comparison, and regular expression
 * evaluation. See {@link RegexFilePathMatcher#isMatch} for examples of matches.
 *
 */
public class RegexFilePathMatcher implements FilePathMatcher {

    private RegexValidator validator = null;
    private String[] inputPatterns;

    /**
     * Constructs a <code>RegexFilePathMatcher</code> using the given array of input patterns.
     *
     * @param inputPatterns
     *            non-empty <code>Array</code> of <code>String</code>s to match against
     * @throws IllegalArgumentException if the input array is empty or contains malformed regex
     */
    public RegexFilePathMatcher(final String... inputPatterns) throws IllegalArgumentException {
        if (ArrayUtils.isEmpty(inputPatterns)) {
            throw new IllegalArgumentException("a RegexFilePathMatcher needs at least one pattern");
        }

        this.inputPatterns = inputPatterns;
        this.validator = new RegexValidator(inputPatterns);
    }

    /**
     * Determine whether or not a given file path (represented as a <code>String</code>) matches
     * this <code>RegexFilePathMatcher</code>'s <code>inputPatterns</code>. The match is determined
     * using a combination of initial substring comparison, full string comparison, and regular
     * expression evaluation.
     * <p>
     * For example:
     * <p>&nbsp;
     * <ul>
     *   <li>full path input pattern <code>/var/lib/my-lib.so</code> matches the file
     *       "/var/lib/my-lib.so"</li>
     *   <li>directory input pattern <code>/var/lib/</code> recursively matches all files in
     *       "/var/lib/"</li>
     *   <li>regex input pattern <code>.*foo</code> matches all files ending with "foo"</li>
     *   <li>regex input pattern <code>.*\.foo</code> matches all files ending with ".foo"</li>
     *   <li>regex input pattern <code>\/usr\/lib\/.*.txt</code> matches all .txt files contained in
     *       /usr/lib path</li>
     *   <li>regex input pattern <code>.*\/lib\/.*.txt</code> matches all .txt files contained in a
     *       path that contains the word "/lib/", so that both "/var/lib/path1/info.txt" and
     *       "/usr/lib/path2/info2.txt" would match</li>
     * </ul>
     * <p>
     * For general regex info, see http://www.regexr.com
     *
     * @param path
     *          the <code>Path</code> to use for comparison
     * @return true if <code>path</code> matches <code>inputPatterns</code>, false if they don't
     *          match or if <code>path</code> or <code>inputPatterns</code> is null
     */
    public boolean isMatch(final String path) {
        if (validator == null || StringUtils.isEmpty(path)) {
            return false;
        }

        if (validator.isValid(path)) {
            return true;
        }

        // If the RegexValidator fails, also check if any of the <code>inputPatterns</code> are the
        // initial substring of the <code>path</code>, in which case we return true because there is
        // a full path match or a directory match.
        for (String inputPattern : inputPatterns) {
            if (path.startsWith(inputPattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set this <code>RegexFilePathMatcher</code>'s array of input patterns.
     *
     * @param patterns
     *            non-empty <code>Array</code> of <code>String</code>s to match against
     *
     * @throws IllegalArgumentException if the input array is empty or contains malformed regex
     *
     */
    public void setPatterns(final String... patterns) throws IllegalArgumentException {

        if (ArrayUtils.isEmpty(patterns)) {
            throw new IllegalArgumentException("a RegexFilePathMatcher needs at least one pattern");
        }

        this.inputPatterns = patterns;
        this.validator = new RegexValidator(patterns);
    }

}


