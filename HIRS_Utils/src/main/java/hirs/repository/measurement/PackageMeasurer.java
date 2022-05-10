package hirs.repository.measurement;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This abstract class implements the basic functionality for measuring a software package.  It
 * should be extended by classes which measure a specific type of software package.
 * PackageMeasurers are capable of 'chaining' measurements to other PackageMeasurers - for instance,
 * if an initramfs file is found inside of an RPM, RPMMeasurer will chain measurement
 * of the initramfs file to {@link InitramfsMeasurer}.
 * <p>
 * This class is threadsafe.
 */
public abstract class PackageMeasurer {
    private static final Logger LOGGER = LogManager.getLogger(PackageMeasurer.class);
    private static final Path ROOT = FileSystems.getDefault().getPath("/");

    private final DigestAlgorithm digestAlgorithm;
    private final List<PackageMeasurer> chainedPackageMeasurers;

    /**
     * Construct a PackageMeasurer with the desired configuration.
     *
     * @param digestAlgorithm the algorithm with which to measure files.
     * @param chainedPackageMeasurers an optional parameter to specify additional PackageMeasurers
     *                                that should be used during measurement (see #measureDirectory)
     */
    public PackageMeasurer(final DigestAlgorithm digestAlgorithm, final PackageMeasurer...
                           chainedPackageMeasurers) {
        this.digestAlgorithm = digestAlgorithm;
        this.chainedPackageMeasurers = Collections.synchronizedList(
                Arrays.asList(chainedPackageMeasurers));
    }

    /**
     * Get the digest algorithm to use for measurement.
     *
     * @return the digest algorithm
     */
    public final DigestAlgorithm getDigestAlgorithm() {
        return digestAlgorithm;
    }

    /**
     * Measures a specific software package.  This method will first measure the contents of the
     * package according to the implementing class, and will then query all chained measurers to
     * see if they should also measure the given software package.  If so, they will be called
     * to measure the package and the resulting measurements will be added to the returned
     * Multimap.
     *
     * @param packageFile the path to the package to be measured
     * @return the resulting Multimap of absolute file paths to their measurements (one or many)
     * @throws IOException if there is an error encountered while processing the package
     */
    public Multimap<Path, Digest> measure(final Path packageFile) throws IOException {
        Multimap<Path, Digest> measurements = doMeasure(packageFile);
        for (PackageMeasurer measurer : chainedPackageMeasurers) {
            if (measurer.shouldMeasureFileWhenChainedMeasurer(packageFile)) {
                try {
                    measurements.putAll(measurer.measure(packageFile.toAbsolutePath()));
                } catch (IOException e) {
                    LOGGER.error("Failed to measure file: " + packageFile.toString(),
                            e.getMessage());
                }
            }
        }
        return measurements;
    }

    /**
     * Measures a specific software package.  The file type of the given packageFile is
     * specific to the implementing class.  This method returns a Multimap from Path to a collection
     * of Digests, as a certain Path could have multiple measurements (from extracted files, etc.)
     *
     * @param packageFile the path to the package to be measured
     * @return the resulting Multimap of absolute file paths to their measurements (one or many)
     * @throws IOException if there is an error encountered while processing the package
     */
    protected abstract Multimap<Path, Digest> doMeasure(Path packageFile) throws IOException;

    /**
     * This method reports whether this measurer expects to be able to measure the given filepath
     * when used as a chained measurer.
     *
     * @param filePath the filePath to be evaluated
     * @return true if this measurer can (and should) measure the given file, false otherwise
     */
    protected abstract boolean shouldMeasureFileWhenChainedMeasurer(Path filePath);

    /**
     * This method reports whether this measurer expects to be able to measure the file at the
     * given filepath.
     *
     * @param filePath the filePath to be evaluated
     * @return true if this measurer can (and should) measure the given file, false otherwise
     */
    protected abstract boolean shouldMeasureFile(Path filePath);

    /**
     * Measures a directory, descending into child directories and files.  Excludes symbolic links.
     * For every file that is measured, all chained PackageMeasurers (as specified in this class'
     * constructor) will be queried to check if they are capable of unpacking and/or measuring the
     * file's contents.  If so, the PackageMeasurer will measure the file and the resulting
     * measurements will be included in the returned measurements.
     *
     * @param directory the directory contain the subdirectories and files to measure.
     * @return the resulting Multimap of absolute file paths to their measurements (one or many)
     * @throws IOException if there is an error encountered while measuring the files.
     */
     protected final Multimap<Path, Digest> measureDirectory(final Path directory)
            throws IOException {
        final Multimap<Path, Digest> measurements = HashMultimap.create();

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file,
                                             final BasicFileAttributes attrs) {
                // skip symbolic links
                if (Files.isSymbolicLink(file)) {
                    return FileVisitResult.CONTINUE;
                }

                // add measurement for file
                Path relativePath = ROOT.resolve(directory.relativize(file));
                if (shouldMeasureFile(file)) {

                    LOGGER.debug("Measuring: " + relativePath.toString());
                    try {
                        measurements.put(relativePath, measureFile(file));
                    } catch (IOException e) {
                        LOGGER.error("Failed to measure file: " + file.toString(), e.getMessage());
                    }
                }

                // call all applicable chained measurers
                for (PackageMeasurer measurer : chainedPackageMeasurers) {
                    if (measurer.shouldMeasureFileWhenChainedMeasurer(relativePath)) {
                        try {
                            measurements.putAll(measurer.measure(file.toAbsolutePath()));
                        } catch (IOException e) {
                            LOGGER.error("Failed to measure file: " + file.toString(),
                                    e.getMessage());
                        }
                    }
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return measurements;
    }

    /**
     * Measures the given file according to the set digest algorithm.
     *
     * @param fileLocation the absolute path of the file to measure.
     * @return the resulting Digest
     * @throws IOException if there is an error encountered while reading the file.
     */
     public final Digest measureFile(final Path fileLocation) throws IOException {
        byte[] value;

        try (FileInputStream fis = new FileInputStream(fileLocation.toFile())) {
            switch (digestAlgorithm) {
                case MD2:
                    value = DigestUtils.md2(fis);
                    break;
                case MD5:
                    value = DigestUtils.md5(fis);
                    break;
                case SHA1:
                    value = DigestUtils.sha1(fis);
                    break;
                case SHA256:
                    value = DigestUtils.sha256(fis);
                    break;
                case SHA384:
                    value = DigestUtils.sha384(fis);
                    break;
                case SHA512:
                    value = DigestUtils.sha512(fis);
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "Measurement type not supported: " + digestAlgorithm
                                    .toString()
                    );
            }
        }

        return new Digest(digestAlgorithm, value);
    }
}
