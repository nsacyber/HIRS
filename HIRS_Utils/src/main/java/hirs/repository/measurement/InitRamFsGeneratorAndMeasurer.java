package hirs.repository.measurement;

import com.google.common.collect.Multimap;
import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.repository.RPMRepoPackage;
import hirs.utils.exec.ExecBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class is able to take in a kernel RPM and generate an initramfs image from it, whose
 * contents are then extracted and measured.
 *
 * Extends <code>PackageMeasurer</code> to conduct the measurement of initamfs content.  Requires
 * dracut to be installed.  This measurer specifically (and only) operates on kernel RPMs; given a
 * kernel RPM, this class will generate an initramfs image and extract and measure its contents in
 * temporary directories.
 */
public class InitRamFsGeneratorAndMeasurer extends PackageMeasurer {
    private static final String KERNEL_RPM_NAME = "kernel";
    private static final Logger LOGGER = LogManager.getLogger(InitRamFsGeneratorAndMeasurer.class);
    private static final int GENERATE_EXTRACT_TIMEOUT_MS = 2 * 60 * 1000;
    private static final String GENERATE_EXTRACT_SCRIPT =
            "/repository/generate_and_extract_initramfs.sh";

    /**
     * Construct a new InitRamFsGeneratorAndMeasurer.
     *
     * @param digestAlgorithm the algorithm with which to measure files.
     */
    public InitRamFsGeneratorAndMeasurer(final DigestAlgorithm digestAlgorithm) {
        super(digestAlgorithm);
    }

    @Override
    protected Multimap<Path, Digest> doMeasure(final Path packageFile) throws IOException {
        if (packageFile == null) {
            throw new IllegalArgumentException("packageFile cannot be null");
        }
        Path rpmRoot = null;

        try {
            Path fileName = packageFile.getFileName();
            if (fileName == null) {
                throw new IllegalArgumentException(
                        "Given path to package file doesn't have a filename"
                );
            }
            String rpmFileName = fileName.toString();

            // assign rpm root for deletion in finally block
            rpmRoot = RPMMeasurer.createRpmExtractionFakeRoot(packageFile);
            // extract RPM
            RPMMeasurer.extractRpm(packageFile, rpmRoot);

            // generate initramfs file from RPM, remove extra files from RPM,
            // and extract contents of initramfs file
            Path initRamFsContents = generateInitRamFsContents(
                    rpmRoot,
                    RPMRepoPackage.parseRPMCompleteVersion(rpmFileName)
            );

            // measure
            return measureDirectory(initRamFsContents);
        } catch (RuntimeException | IOException e) {
            throw e;
        } finally {
            // clean up
            try {
                if (rpmRoot != null) {
                    FileUtils.deleteDirectory(rpmRoot.toFile());
                }
            } catch (IOException | IllegalArgumentException e) {
                LOGGER.error("Error deleting extraction directory for RPM. " + rpmRoot, e);
            }
        }
    }

    private static Path generateInitRamFsContents(final Path extractedKernelRpm,
                                                  final String completeVersion)
            throws IOException {
        if (extractedKernelRpm == null) {
            throw new IllegalArgumentException("extractedKernelRpm cannot be null");
        }
        if (completeVersion == null) {
            throw new IllegalArgumentException("completeVersion cannot be null");
        }

        Path extractedKernelRpmFileName = extractedKernelRpm.getFileName();
        if (extractedKernelRpmFileName == null) {
            throw new IllegalArgumentException("extractedKernelRpm has no filename");
        }

        // create fake root
        Path rootDir = extractedKernelRpm.resolveSibling(String.format("%s.%s.%s",
                UUID.randomUUID(), extractedKernelRpmFileName.toString(), "fakeroot"));
        Files.createDirectory(rootDir);

        // create script if it doesn't exist
        Path parentOfRootDir = rootDir.getParent();
        if (parentOfRootDir == null) {
            throw new IllegalArgumentException("RPM extraction dir has no parent");
        }
        Path script = parentOfRootDir.resolve("generate_initramfs.sh");
        createInitRamFsGeneratorScript(script);

        Path targetFilePath = rootDir.resolve(String.format("initramfs-%s.img", completeVersion));
        Path kModDir = rootDir.resolve(String.format("./lib/modules/%s/", completeVersion));

        Map<String, String> env = new HashMap<>();
        env.put("TARGET_FILEPATH", targetFilePath.toAbsolutePath().toString());
        env.put("COMPLETE_VERSION", completeVersion);
        env.put("KMODDIR", kModDir.toAbsolutePath().toString());
        env.put("WORKING_DIR", rootDir.toAbsolutePath().toString());

        // extract
        new ExecBuilder("/bin/sh")
                .args("-c", script.toAbsolutePath().toString())
                .timeout(GENERATE_EXTRACT_TIMEOUT_MS)
                .workingDirectory(rootDir)
                .environment(env)
                .exec();

        return rootDir;
    }

    @Override
    protected boolean shouldMeasureFileWhenChainedMeasurer(final Path filePath) {
        Path fileName = filePath.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("Given path has no filename");
        }
        String filename = fileName.toString();
        return RPMRepoPackage.isRpmFilename(filename)
                && KERNEL_RPM_NAME.equals(RPMRepoPackage.parseName(filename));
    }

    @Override
    protected boolean shouldMeasureFile(final Path filePath) {
        return true;
    }

    private static synchronized void createInitRamFsGeneratorScript(final Path script)
            throws IOException {
        if (Files.exists(script)) {
            return;
        }

        try (InputStream resourceAsStream = InitRamFsGeneratorAndMeasurer.class
                .getResourceAsStream(GENERATE_EXTRACT_SCRIPT)) {
            Files.copy(
                    resourceAsStream,
                    script
            );
        }

        if (!script.toFile().setExecutable(true)) {
            throw new IOException("Unable to make script executable.");
        }
    }
}
