package hirs.repository.measurement;

import com.google.common.collect.Multimap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Unit test for <code>RPMMeasurer</code>.
 */
public class RPMMeasurerTest {
    private static final String TEST_RPM_RESOURCE =
            "/repository/vim-common-7.2.411-1.8.el6.x86_64.rpm";
    private static final String TEST_RPM_FILEPATH = "/usr/share/vim/vim72/syntax/d.vim";
    private static final String TEST_RPM_SHA1_DIGEST = "c068d837b672cb3f80ac2f20e91a4a845189cb21";
    private static final int TEST_RPM_FILE_COUNT = 1432;

    private Path rpmDir;
    private Path rpmFile;

    /**
     * Creates a temporary directory and copies a temporary RPM into that directory for testing.
     * @throws IOException if an error is encountered performing the above operations.
     */
    @BeforeClass
    public final void setup() throws IOException {
        // copy out RPM
        rpmDir = Files.createTempDirectory(
                FileSystems.getDefault().getPath(FileUtils.getTempDirectoryPath()),
                "rpmDir"
        );

        rpmFile = Files.createTempFile(rpmDir, "vim", ".rpm");
        FileUtils.copyURLToFile(getClass().getResource(TEST_RPM_RESOURCE), rpmFile.toFile());
    }

    /**
     * Tests that an <code>RPMMeasurer</code> can measure an RPM.
     * @throws IOException if an error is encountered while measuring the RPM.
     */
    @Test(groups = { "rhel-6" })
    @SuppressFBWarnings(
            value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
            justification = "Hardcoded path is a known file inside a resource RPM."
    )
    public final void testMeasureRPM() throws IOException {
        RPMMeasurer measurer = new RPMMeasurer(DigestAlgorithm.SHA1);
        Multimap<Path, Digest> measurements = measurer.measure(rpmFile);
        Assert.assertEquals(measurements.keySet().size(), TEST_RPM_FILE_COUNT);
        for (Digest digest : measurements.get(new File(TEST_RPM_FILEPATH).toPath())) {
            Assert.assertEquals(digest.getDigestString(), TEST_RPM_SHA1_DIGEST);
        }
    }

    /**
     * Removes the temporary directory that setup() creates.
     * @throws IOException if an error is encountered while removing the directory.
     */
    @AfterClass
    public final void teardown() throws IOException {
        FileUtils.deleteDirectory(rpmDir.toFile());
    }
}
