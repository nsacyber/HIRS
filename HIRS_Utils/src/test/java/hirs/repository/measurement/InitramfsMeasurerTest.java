package hirs.repository.measurement;

import com.google.common.collect.Multimap;
import hirs.data.persist.Digest;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.apache.commons.io.FileUtils;

/**
 * Unit test for <code>InitramfsMeasurer</code>.
 */
public class InitramfsMeasurerTest {
    private static final String TEST_INITRAMFS_RESOURCE =
            "/repository/initramfs-2.6.32-431.el6.x86_64.img";

    private Path initramfsFileSource;
    private static final int INITRAMFS_FILE_COUNT = 1186;

    /**
     * Gets the test initramfs resource file reference.
     * @throws IOException if an error is encountered performing the above operations.
     * @throws URISyntaxException if there is a syntax error in the URI specification.
     */
    @BeforeClass
    public final void setup() throws IOException, URISyntaxException {
        URL resourceUrl = getClass().getResource(TEST_INITRAMFS_RESOURCE);

        Assert.assertNotNull(resourceUrl, "Could not find test resource: "
                + TEST_INITRAMFS_RESOURCE);
        initramfsFileSource = new File(resourceUrl.toURI()).toPath();
    }

    /**
     * Verifies no temporary directories exist.
     * @throws IOException if an error is encountered while removing the directory.
     */
    @AfterClass
    public final void teardown() throws IOException {
        String temporaryPath = FileUtils.getTempDirectoryPath();
        File temporaryPathFile = new File(temporaryPath);
        File[] matchedFiles = temporaryPathFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                return pathname.isDirectory() && pathname.getAbsolutePath()
                        .contains("initramfs");
            }
        });

        if (matchedFiles == null) {
             throw new IllegalArgumentException("Matched files is null");
        }

        // delete any directories that may still exist before assertion.
        for (File matchedFile : matchedFiles) {
            FileUtils.deleteDirectory(matchedFile);
        }

        Assert.assertEquals(matchedFiles.length, 0);
    }

    /**
     * Tests that the measurer throws an exception if providing a null argument for the
     * file source.
     * @throws IOException if an error is encountered while measuring the file.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void illegalArgumentExceptionOnMeasure()
            throws IOException {
        InitramfsMeasurer measurer = new InitramfsMeasurer();
        measurer.measure(null);
    }

    /**
     * Tests that the initramfs file can be processed and measured.
     * @throws IOException if an error is encountered while measuring the file.
     */
    @Test
    public final void testMeasureInitramfs() throws IOException {
        InitramfsMeasurer measurer = new InitramfsMeasurer();

        Multimap<Path, Digest> measurements = measurer.measure(initramfsFileSource);

        Assert.assertEquals(measurements.keySet().size(), INITRAMFS_FILE_COUNT,
                "incorrect number of files were measured");

        for (Path path : measurements.keySet()) {
            Assert.assertFalse(path.startsWith("/tmp/initramfs"),
                    "measurement path should not include extraction root");
            Assert.assertNotEquals(path.getFileName(), initramfsFileSource.getFileName(),
                    "the initramfs file itself should not be measured");
        }
    }
}
