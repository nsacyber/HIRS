package hirs.repository.measurement;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Measures the contents of an initramfs file. This class unpacks the file to a temporary
 * directory, performs measurements, and then cleans up the files. This class is used
 * in the golden baseline feature when the user specifies to create or update a baseline with
 * an initramfs file.
 *
 */
public class InitramfsMeasurer extends PackageMeasurer {
    private static final Logger LOGGER = LogManager.getLogger(InitramfsMeasurer.class);
    private static final Pattern INIT_RAM_FS_PATH = Pattern.compile("/boot/initramfs.+\\.img$");

    /**
     * Constructs a new InitramfsMeasurer.
     */
    public InitramfsMeasurer() {
        super(DigestAlgorithm.SHA1);
    }

    /**
     * Measures the initramfs file by unpacking it to a temporary location,
     * measuring the files, and then cleaning up the temporary directory.
     *
     * @param initramfsFile the initramfs file path.
     * @return the resulting Multimap of absolute file paths to their measurements (one or many)
     * @throws IOException if there is an error encountered while processing the package
     */
    @Override
    protected final Multimap<Path, Digest> doMeasure(final Path initramfsFile)
            throws IOException {
        // extract file to temporary directory

        if (initramfsFile == null) {
            String errorMessage = "initramfsFile is null";
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        File sourceFile = initramfsFile.toFile();
        if (!sourceFile.exists()) {
            String errorMessage = "initramfs file does not exist: " + initramfsFile.toString();
            LOGGER.error(errorMessage);
            throw new FileNotFoundException(errorMessage);
        }

        Path extractionRootPath = null;
        final Multimap<Path, Digest> measurements = HashMultimap.create();

        try {
            extractionRootPath = Files.createTempDirectory(
                    FileSystems.getDefault().getPath(FileUtils.getTempDirectoryPath()),
                    "initramfsExtractionRoot");

            File extractionRoot = new File(extractionRootPath.toString());
            File destinationFile = new File(extractionRootPath.toString(), sourceFile.getName());

            LOGGER.info("Copying {} to {}", sourceFile.getAbsolutePath(),
                    destinationFile.getAbsolutePath());
            FileUtils.copyFile(sourceFile, destinationFile);
            LOGGER.info("File copied. Beginning extracting of file to temp directory");

            // can't specify cpio destination directory, so run this process
            // in the extraction path directory. Also need to run this in a separate shell
            // instance so that the pipe redirection works properly.
            String extractionArgument = String.format("dd if=%s | gunzip | cpio -id",
                    destinationFile.getName());
            String[] processArguments = new String[] {"/bin/sh", "-c", extractionArgument};

            ProcessBuilder processBuilder = new ProcessBuilder(processArguments);
            processBuilder.directory(extractionRoot);

            Process startedProcess = processBuilder.start();

            startedProcess.waitFor();
            // delete the copied initramfs file now that it is extracted. This ensures
            // that the initramfs file itself is not measured.
            FileUtils.deleteQuietly(destinationFile);

            LOGGER.info("Starting initramfs file measurements");
            measurements.putAll(measureDirectory(extractionRoot.toPath()));
            LOGGER.info("Finished initramfs file measurements");
        } catch (InterruptedException e) {
            throw new IOException("initramfs measurement process was interrupted", e);
        } finally {
            if (null != extractionRootPath) {
                FileUtils.deleteDirectory(extractionRootPath.toFile());
            }
        }

        return measurements;
    }

    /**
     * This method will determine whether the give filepath matches the expected initramfs filename
     * and location.
     *
     * @param filePath the filePath to be evaluated
     * @return true if {@link InitramfsMeasurer} should measure this file
     */
    @Override
    public final boolean shouldMeasureFileWhenChainedMeasurer(final Path filePath) {
        return INIT_RAM_FS_PATH.matcher(filePath.toString()).matches();
    }

    @Override
    public final boolean shouldMeasureFile(final Path filePath) {
        return true;
    }
}
