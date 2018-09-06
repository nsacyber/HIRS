package hirs.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for <code>RegexFilePathMatcher</code>.
 */
public class RegexFilePathMatcherTest {

    /**
     * Tests that identical strings match.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test
    public final void fullString() throws Exception {
        String path1 = "/var/lib/my-lib.so";
        String path2 = "/var/lib/someone-elses-lib.so";
        String pattern = "/var/lib/my-lib.so";
        Assert.assertTrue(new RegexFilePathMatcher(pattern).isMatch(path1));
        Assert.assertFalse(new RegexFilePathMatcher(pattern).isMatch(path2));
    }

    /**
     * Tests that nested files in a directory match due to initial substring matching.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test
    public final void directorySubstring() throws Exception {
        String path1 = "/var/lib/foobar";
        String path2 = "/var/lib/foo/bar";
        String path3 = "/etc/lib/foobar";
        String pattern = "/var/lib/";
        Assert.assertTrue(new RegexFilePathMatcher(pattern).isMatch(path1));
        Assert.assertTrue(new RegexFilePathMatcher(pattern).isMatch(path2));
        Assert.assertFalse(new RegexFilePathMatcher(pattern).isMatch(path3));
    }

    /**
     * Tests that regex can be used to match filename endings.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test
    public final void filenameEnding() throws Exception {
        String path1 = "/var/lib/foo";
        String path2 = "/var/lib/barfoo";
        String path3 = "/var/lib/foobar";
        String pattern = ".*foo";
        Assert.assertTrue(new RegexFilePathMatcher(pattern).isMatch(path1));
        Assert.assertTrue(new RegexFilePathMatcher(pattern).isMatch(path2));
        Assert.assertFalse(new RegexFilePathMatcher(pattern).isMatch(path3));
    }

    /**
     * Tests that regex can be used to match file extensions.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test
    public final void fileExtension() throws Exception {
        String path1 = "/var/lib/bar.foo";
        String path2 = "barbar.foo";
        String path3 = "/var/lib/foo.bar";
        String pattern = ".*\\.foo";
        Assert.assertTrue(new RegexFilePathMatcher(pattern).isMatch(path1));
        Assert.assertTrue(new RegexFilePathMatcher(pattern).isMatch(path2));
        Assert.assertFalse(new RegexFilePathMatcher(pattern).isMatch(path3));

    }

    /**
     * Tests that file extensions and directory substrings can be used together.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test
    public final void extensionAndDirectory() throws Exception {
        String path1 = "/usr/lib/foo.txt";
        String path2 = "/usr/lib/bar/foobar.txt";
        String path3 = "/etc/lib/foo.txt";
        String path4 = "/usr/lib/foobar.csv";
        String pattern = "\\/usr\\/lib\\/.*\\.txt";
        Assert.assertTrue(new RegexFilePathMatcher(pattern).isMatch(path1));
        Assert.assertTrue(new RegexFilePathMatcher(pattern).isMatch(path2));
        Assert.assertFalse(new RegexFilePathMatcher(pattern).isMatch(path3));
        Assert.assertFalse(new RegexFilePathMatcher(pattern).isMatch(path4));
    }

    /**
     * Tests that wildcards can be used in directories and file extensions.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test
    public final void extensionAndDirectoryContains() throws Exception {
        String path1 = "/var/lib/path1/info.txt";
        String path2 = "/usr/lib/path2/info2.txt";
        String path3 = "/usr/library/path3/info.txt";
        String path4 = "/usr/lib/path4/info.csv";
        String pattern = ".*\\/lib\\/.*\\.txt";
        Assert.assertTrue(new RegexFilePathMatcher(pattern).isMatch(path1));
        Assert.assertTrue(new RegexFilePathMatcher(pattern).isMatch(path2));
        Assert.assertFalse(new RegexFilePathMatcher(pattern).isMatch(path3));
        Assert.assertFalse(new RegexFilePathMatcher(pattern).isMatch(path4));
    }

    /**
     * Tests that negative lookbehind can be used to exclude a file extension.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test
    public final void negativeLookbehind() throws Exception {
        String path1 = "/foo.txt";
        String path2 = "/usr/lib/bar/foobar.xml";
        String path3 = "/foo/bar/foobar.csv";
        String pattern = ".*(?<!csv)$";
        Assert.assertTrue(new RegexFilePathMatcher(pattern).isMatch(path1));
        Assert.assertTrue(new RegexFilePathMatcher(pattern).isMatch(path2));
        Assert.assertFalse(new RegexFilePathMatcher(pattern).isMatch(path3));
    }

    /**
     * Tests that multiple patterns are checked.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test
    public final void multiplePatterns() throws Exception {
        String path1 = "/foo.txt";
        String path2 = "/bar.txt";
        String path3 = "/foobar.txt";
        String[] patterns = {"/foo.txt", "/bar.txt"};
        Assert.assertTrue(new RegexFilePathMatcher(patterns).isMatch(path1));
        Assert.assertTrue(new RegexFilePathMatcher(patterns).isMatch(path2));
        Assert.assertFalse(new RegexFilePathMatcher(patterns).isMatch(path3));
    }

    /**
     * Tests that a <code>RegexFilePathMatcher</code> constructor call with malformed regex patterns
     * will throw an exception. In this case the regex opens more parentheses than it closes.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void malformedRegex() throws Exception {
        String pattern = "(((**..";
        new RegexFilePathMatcher(pattern);
    }

    /**
     * Tests that a <code>RegexFilePathMatcher</code> constructor call with a malformed regex
     * pattern and a well-formed regex pattern will throw an exception. In this example the regex is
     * malformed because it contains some illegal <code>String</code> escape sequences.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void malformedAndWellFormedRegex() throws Exception {
        String[] patterns = {"**\b**\t**", "/foo.txt"};
        new RegexFilePathMatcher(patterns);
    }

    /**
     * Tests that a null path will not match any pattern.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test
    public final void nullPath() throws Exception {
        String path1 = null;
        String pattern = ".*";
        Assert.assertFalse(new RegexFilePathMatcher(pattern).isMatch(path1));
    }

    /**
     * Tests that a null path will not match any pattern.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test
    public final void emptyPath() throws Exception {
        String path1 = "";
        String pattern = ".*";
        Assert.assertFalse(new RegexFilePathMatcher(pattern).isMatch(path1));
    }

    /**
     * Tests that a constructor with no patterns throws an exception.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void noPatterns() throws Exception {
        new RegexFilePathMatcher();
    }

    /**
     * Tests that a constructor with an empty patterns array throws an exception.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void emptyPatterns() throws Exception {
        new RegexFilePathMatcher(new String[] {});
    }

    /**
     * Tests that a constructor with null inputPatterns throws an exception.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void nullPatterns() throws Exception {
        new RegexFilePathMatcher((String[]) null);
    }

    /**
     * Tests that new patterns can be set after construction.
     *
     * @throws Exception if an error is encountered while executing the test.
     */
    @Test
    public final void setPatterns() throws Exception {
        String path1 = "/foo.txt";
        String path2 = "/bar.txt";
        String patterns1 = "/foo.txt";
        String[] patterns2 = {"/foo.txt", "/bar.txt"};

        RegexFilePathMatcher matcher = new RegexFilePathMatcher(patterns1);

        Assert.assertTrue(matcher.isMatch(path1));
        Assert.assertFalse(matcher.isMatch(path2));

        matcher.setPatterns(patterns2);
        Assert.assertTrue(matcher.isMatch(path1));
        Assert.assertTrue(matcher.isMatch(path2));
    }
}
