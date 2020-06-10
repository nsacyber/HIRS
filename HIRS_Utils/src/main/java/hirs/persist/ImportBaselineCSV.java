package hirs.persist;

import hirs.data.persist.baseline.Baseline;
import hirs.ima.IMABaselineGeneratorException;
import hirs.ima.ImaIgnoreSetBaselineGenerator;
import hirs.ima.ImaIgnoreSetBaselineGeneratorException;
import hirs.ima.ImaBlacklistBaselineGenerator;
import hirs.ima.SimpleImaBaselineGenerator;
import hirs.tpm.TPMBaselineGenerator;
import hirs.tpm.TPMBaselineGeneratorException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

/**
 * This class imports a baseline from a csv file uploaded to the HIRS portal.
 */
public final class ImportBaselineCSV {

    /**
     * private entry so the class isn't invoked.
     */
    private ImportBaselineCSV() {
    }

    /**
     * Sets up the logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(ImportBaselineCSV.class);

    /**
     * Imports a new baseline.
     *
     * @param name        User defined name of baseline
     * @param inputStream Input stream to the CSV file
     * @param type        Represents the type of Baseline being created (IMA, TPM)
     * @return create baseline
     */
    public static Baseline createBaseline(final String name, final InputStream inputStream,
            final String type) {
        LOGGER.debug("import csv started");

        try (InputStream is = new BufferedInputStream(inputStream)) {
            Baseline baseline = null;

            // IMA baselines only
            LOGGER.debug("specified IMA import");
            if (type.equalsIgnoreCase("IMA")) {
                final SimpleImaBaselineGenerator imaGenerator = new SimpleImaBaselineGenerator();
                baseline = imaGenerator.generateBaselineFromCSVFile(name, is);
            } else if (type.equalsIgnoreCase("Ignore")) {
                final ImaIgnoreSetBaselineGenerator ignoreGenerator =
                        new ImaIgnoreSetBaselineGenerator();
                baseline = ignoreGenerator.generateBaselineFromCSVFile(name, is);
            } else if (type.equalsIgnoreCase("IMABlack")) {
                baseline = ImaBlacklistBaselineGenerator.generateBaselineFromCSV(name, is);
            } else if (type.equalsIgnoreCase("TPMWhite")) {
                final TPMBaselineGenerator tpmGenerator = new TPMBaselineGenerator();
                baseline = tpmGenerator.generateWhiteListBaselineFromCSVFile(name, is);
            } else if (type.equalsIgnoreCase("TPMBlack")) {
                final TPMBaselineGenerator tpmGenerator = new TPMBaselineGenerator();
                baseline = tpmGenerator.generateBlackListBaselineFromCSVFile(name, is);
            } else {
                String error = type + " baseline type not supported";
                LOGGER.error(error);
                throw new BaselineManagerException(error);
            }

            /* CSV files only */
            LOGGER.debug("importing csv file");
            return baseline;
        } catch (IMABaselineGeneratorException e) {
            throw new RuntimeException("Type mismatch, verify import baseline type", e);
        } catch (IOException | ParseException | ImaIgnoreSetBaselineGeneratorException
                | TPMBaselineGeneratorException | BaselineManagerException e) {
            LOGGER.error("unable to generate baseline", e);
            throw new RuntimeException(e);
        }
    }
}
