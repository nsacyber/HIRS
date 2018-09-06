package hirs.persist;

import hirs.data.persist.Baseline;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.IMAReport;
import hirs.data.persist.IntegrityReport;
import hirs.data.persist.TPMReport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import hirs.ima.SimpleImaBaselineGenerator;
import hirs.ima.IMABaselineGeneratorException;
import hirs.tpm.TPMBaselineGenerator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * This class is a command-line interface (CLI) for importing baselines into a
 * database. This takes a file and stores it in a <code>BaselineManager</code>.
 */
public final class ImportCLI {
    private static final Logger LOGGER = LogManager.getLogger(ImportCLI.class);
    private static final String HELP = "help";
    private static final String TPM = "tpm";
    private static final String IMA = "ima";
    private static final String CSV = "csv";
    private static final String XML = "xml";

    private static BaselineManager baselineManager;

    /**
     * Default constructor that is private to prevent this class from being
     * initialized.
     */
    private ImportCLI() {
        /* do nothing */
    }

    /**
     * Imports a new baseline.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        LOGGER.debug("import cli started");
        loadBeansFromSpringContext();

        final int fileIndex = 0;
        final int nameIndex = 1;
        final Options options = getOptions();
        final CommandLineParser parser = new DefaultParser();
        InputStream istream = null;
        try {
            final CommandLine line = parser.parse(options, args);
            if (line.hasOption("help")) {
                printHelp(options);
                return;
            }

            final String[] extraArgs = line.getArgs();
            if (extraArgs.length != 2) {
                LOGGER.error("file and baseline name not given");
                printHelp(options);
                return;
            }
            LOGGER.debug("importing file: {}", extraArgs[fileIndex]);
            istream = new FileInputStream(new File(extraArgs[fileIndex]));
            final String name = extraArgs[nameIndex];

            Baseline baseline = null;
            if (line.hasOption("tpm")) {
                LOGGER.debug("specified TPM import");
                final TPMBaselineGenerator generator = new TPMBaselineGenerator();
                if (line.hasOption("csv")) {
                    LOGGER.debug("importing csv file");
                    baseline = generator.generateWhiteListBaselineFromCSVFile(name, istream);
                } else {
                    LOGGER.debug("importing TPM baseline from intgerity report xml file");
                    IntegrityReport report;
                    report = getIntegrityReportFromXmlFile(istream);
                    if (report != null) {
                        baseline = generator.generateBaselineFromIntegrityReport(name, report);
                    }
                }
            }
            if (line.hasOption("ima")) {
                LOGGER.debug("specified IMA import");
                final SimpleImaBaselineGenerator generator = new SimpleImaBaselineGenerator();
                if (line.hasOption("csv")) {
                    LOGGER.debug("importing csv file");
                    baseline = generator.generateBaselineFromCSVFile(name, istream);
                } else {
                    LOGGER.debug("importing IMA baseline from intgerity report xml file");
                    IntegrityReport report;
                    report = getIntegrityReportFromXmlFile(istream);
                    try {
                        if (report != null) {
                            generator.generateBaselineFromIntegrityReport(name, report);
                        } else {
                            LOGGER.error("could not create integrity report from xml file");
                            return;
                        }
                    } catch (IMABaselineGeneratorException | NullPointerException e) {
                        return;
                    }
                }
            }
            saveBaseline(baseline);

        } catch (ParseException e) {
            if (options.hasOption(HELP)) {
                printHelp(options);
            } else {
                LOGGER.error("error parsing options");
                LOGGER.error(e.getMessage());
                printHelp(options);
            }
        } catch (Exception e) {
            LOGGER.error("unable to generate baseline", e);
        } finally {
            try {
                if (istream != null) {
                    istream.close();
                }
            } catch (IOException e) {
                LOGGER.error("unexpected error closing istream", e);
            }
            LOGGER.debug("shutdown session factory");
        }
    }

    private static void loadBeansFromSpringContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(PersistenceConfiguration.class);
        context.refresh();

        // register a shutdown hook such that components are properly shutdown when JVM is closing
        context.registerShutdownHook();

        baselineManager = context.getBean(BaselineManager.class);
    }

    private static void printHelp(final Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("import-cli {--tpm | --ima} {--csv | --xml} file name", options);
    }

    private static Options getOptions() {
        final Option help = new Option("h", HELP, false, "print this help menu");

        final OptionGroup typeGroup = new OptionGroup();
        typeGroup.setRequired(true);
        final Option tpm = new Option("t", TPM, false, "add TPM baseline");
        final Option ima = new Option("i", IMA, false, "add IMA baseline");
        typeGroup.addOption(tpm);
        typeGroup.addOption(ima);

        final OptionGroup formatGroup = new OptionGroup();
        formatGroup.setRequired(true);
        final Option csv = new Option("c", CSV, false, "CSV file format");
        final Option xml = new Option("x", XML, false, "XML report file format");
        formatGroup.addOption(csv);
        formatGroup.addOption(xml);

        final Options options = new Options();
        options.addOption(help);
        options.addOptionGroup(typeGroup);
        options.addOptionGroup(formatGroup);
        return options;
    }

    private static void saveBaseline(final Baseline baseline)
            throws BaselineManagerException {
        if (baseline == null) {
            LOGGER.info("baseline is null");
            return;
        }
        LOGGER.debug("saving baseline");
        baselineManager.saveBaseline(baseline);
    }

    private static IntegrityReport getIntegrityReportFromXmlFile(final InputStream istream) {
        try {
            JAXBContext context = JAXBContext.newInstance(IntegrityReport.class,
                            DeviceInfoReport.class, TPMReport.class,
                            IMAReport.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (IntegrityReport) unmarshaller.unmarshal(istream);
        } catch (Exception e) {
            LOGGER.error("error occurred while unmarshalling Integrity report", e);
            return null;
        }
    }
}
