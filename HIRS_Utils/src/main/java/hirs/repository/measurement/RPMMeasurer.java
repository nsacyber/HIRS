package hirs.repository.measurement;

import com.google.common.collect.Multimap;

import hirs.data.persist.Digest;
import hirs.data.persist.DigestAlgorithm;
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
import java.util.regex.Pattern;

/**
 * Extends <code>PackageMeasurer</code> to conduct the measurement of RPM content.  Requires
 * rpm2cpio and cpio to be installed.  This 'chains' {@link InitRamFsGeneratorAndMeasurer};
 * if a kernel RPM is measured, an InitRamFsGeneratorAndMeasurer will be used to generate and
 * measure an initramfs image for that kernel; these measurements be added to the measurements from
 * the RPM itself.
 */
public class RPMMeasurer extends PackageMeasurer {
    private static final Logger LOGGER = LogManager.getLogger(RPMMeasurer.class);
    private static final Pattern RPM_FILENAME = Pattern.compile(".+\\.rpm$");
    private static final int RPM_EXTRACT_TIMEOUT = 2 * 60 * 1000;
    private static final String EXTRACT_SCRIPT = "/repository/extract_rpm.sh";

    /**
     * Construct a new RPMMeasurer.
     *
     * @param digestAlgorithm the algorithm with which to measure files.
     */
    public RPMMeasurer(final DigestAlgorithm digestAlgorithm) {
        super(digestAlgorithm, new InitRamFsGeneratorAndMeasurer(digestAlgorithm));
    }

    /**
     * This method measures an RPM at the given file path.  It creates a temporary directory in the
     * given RPM file's directory, extracts the RPM's files into the directory, measures them, then
     * removes the temporary directory.
     *
     * @param rpmFile the path to the package to be measured
     * @return the resulting Multimap of absolute file paths to their measurements (one or many)
     * @throws IOException if there is an error encountered while processing the RPM
     */
    @Override
    protected final Multimap<Path, Digest> doMeasure(final Path rpmFile) throws IOException {
        Path rpmRoot = null;

        try {
            // assign rpm root for deletion in finally block
            rpmRoot = RPMMeasurer.createRpmExtractionFakeRoot(rpmFile);
            // extract RPM
            RPMMeasurer.extractRpm(rpmFile, rpmRoot);

            // measure
            return measureDirectory(rpmRoot);
        } catch (RuntimeException | IOException e) {
            throw e;
        } finally {
            // clean up
            try {
                if (rpmRoot != null) {
                    FileUtils.deleteDirectory(rpmRoot.toFile());
                }
            } catch (IOException e) {
                LOGGER.error("Error deleting extraction directory for RPM. " + rpmRoot, e);
            }
        }
    }

    /**
     * Creates a new directory for the provided RPM file for later extraction.
     * @param rpmFile the RPM file used as a basis for the name of the extraction directory
     * @return the path to the newly-created directory
     * @throws IOException if an exception occurs creating the directory
     * @see RPMMeasurer#extractRpm(Path, Path)
     */
    static Path createRpmExtractionFakeRoot(final Path rpmFile) throws IOException {
        if  (rpmFile == null) {
            throw new IllegalArgumentException("rpmFile can't be null");
        }
        Path rpmFileName = rpmFile.getFileName();
        if (rpmFileName == null) {
            throw new IllegalArgumentException("rpmFile has no filename");
        }
        Path rpmExtractionFakeRoot = rpmFile.resolveSibling(String.format("%s.%s.%s",
                UUID.randomUUID(), rpmFileName.toString(), "fakeroot"));
        Files.createDirectory(rpmExtractionFakeRoot);
        return rpmExtractionFakeRoot;
    }

    /**
     * Extracts the contents of the RPM at the given file path to the provided rpm extraction root
     * directory.
     *
     * @param rpmFile the path of the RPM to extract
     * @param rpmExtractionFakeRoot the already-created path that will serve as the fake root
     *                              for RPM extraction
     * @throws IOException if the extraction fails
     * @see RPMMeasurer#createRpmExtractionFakeRoot(Path)
     */
    static void extractRpm(final Path rpmFile, final Path rpmExtractionFakeRoot)
            throws IOException {
        if (null == rpmExtractionFakeRoot)  {
            throw new IllegalArgumentException("rpmExtractionFakeRoot cannot be null");
        }

        // create extraction directory if necessary
        if (!Files.exists(rpmExtractionFakeRoot)) {
            Files.createDirectory(rpmExtractionFakeRoot);
        }

        Path parentDir = rpmFile.getParent();
        if (parentDir == null)  {
            throw new IllegalArgumentException("RPM file path has no parent dir");
        }

        // create script if it doesn't exist
        Path script = parentDir.resolve("unpack.sh");
        createRPMUnpackerScript(script);

        Map<String, String> env = new HashMap<>();
        env.put("RPM_FILEPATH", rpmFile.toAbsolutePath().toString());

        // extract
        new ExecBuilder("/bin/sh")
                .args("-c", script.toAbsolutePath().toString())
                .timeout(RPM_EXTRACT_TIMEOUT)
                .workingDirectory(rpmExtractionFakeRoot)
                .environment(env)
                .exec();
    }

    /**
     * This method will determine whether the given filepath matches the expected RPM extension.
     *
     * @param filePath the filePath to be evaluated
     * @return true if {@link RPMMeasurer} should measure this file
     */
    @Override
    protected final boolean shouldMeasureFileWhenChainedMeasurer(final Path filePath) {
        Path fileName = filePath.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("Given path has no filename");
        }
        return RPM_FILENAME.matcher(fileName.toString()).matches();
    }

    @Override
    protected final boolean shouldMeasureFile(final Path filePath) {
        return true;
    }

    private static synchronized void createRPMUnpackerScript(final Path script) throws IOException {
        if (Files.exists(script)) {
            return;
        }

        try (InputStream resourceAsStream =
                     InitRamFsGeneratorAndMeasurer.class.getResourceAsStream(EXTRACT_SCRIPT)) {
            Files.copy(resourceAsStream, script);
        }

        if (!script.toFile().setExecutable(true)) {
            throw new IOException("Unable to make script executable.");
        }
    }
}
