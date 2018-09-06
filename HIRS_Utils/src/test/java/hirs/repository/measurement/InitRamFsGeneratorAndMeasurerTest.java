package hirs.repository.measurement;

import com.google.common.collect.Multimap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hirs.data.persist.Digest;
import hirs.data.persist.DigestAlgorithm;

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
import java.nio.file.Paths;

/**
 * Tests the functionality of the {@link InitRamFsGeneratorAndMeasurer} class.
 */
public class InitRamFsGeneratorAndMeasurerTest {
    private static final String TEST_KERNEL_RPM_RESOURCE =
            "/repository/kernel-2.6.32-642.6.1.el6.x86_64.rpm";
    private static final String TEST_KERNEL_RPM_FILENAME = "kernel-2.6.32-642.6.1.el6.x86_64.rpm";

    // in testing, this file appeared in initramfs images generated under both CentOS 6 and 7
    private static final String TEST_FILENAME = "/usr/sbin/chroot";

    private Path rpmDir;
    private Path rpmFile;

    /**
     * Creates a temporary directory and copies a temporary RPM into that directory for testing.
     * @throws IOException if an error is encountered performing the above operations.
     */
    @BeforeClass
    public final void setup() throws IOException {
        rpmDir = Files.createTempDirectory(
                FileSystems.getDefault().getPath(FileUtils.getTempDirectoryPath()),
                "rpmDir"
        );
        rpmFile = rpmDir.resolve(TEST_KERNEL_RPM_FILENAME);
        FileUtils.copyURLToFile(
                getClass().getResource(TEST_KERNEL_RPM_RESOURCE),
                rpmFile.toFile()
        );
    }

    /**
     * Removes the temporary directory that setup() creates.
     * @throws IOException if an error is encountered while removing the directory.
     */
    @AfterClass
    public final void teardown() throws IOException {
        File rpmDirAsFile = rpmDir.toFile();
        if (rpmDirAsFile == null) {
            throw new IllegalArgumentException("RPM directory is null and not removed");
        }
        FileUtils.deleteDirectory(rpmDirAsFile);
    }

    /**
     * Tests that an InitRamFsGeneratorAndMeasurer can generate an initramfs file from a kernel RPM
     * and extract and measure its contents.
     *
     * @throws IOException if an error is encountered while creating and measuring the initramfs
     *                     file
     */
    @Test(groups = { "rhel-6" })
    @SuppressFBWarnings(
        value = "DMI_HARDCODED_ABSOLUTE_FILENAME", justification = "because i said so"
    )
    public final void testExtractAndMeasureKernel() throws IOException {
        InitRamFsGeneratorAndMeasurer genMes =
                new InitRamFsGeneratorAndMeasurer(DigestAlgorithm.SHA1);
        Multimap<Path, Digest> measurements = genMes.measure(rpmFile);
        Assert.assertNotNull(measurements);
        Assert.assertTrue(measurements.containsKey(Paths.get(TEST_FILENAME)));
    }
}
