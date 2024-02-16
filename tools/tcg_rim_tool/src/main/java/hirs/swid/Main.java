package hirs.swid;

import hirs.swid.utils.Commander;
import hirs.swid.utils.TimestampArgumentValidator;
import hirs.utils.rim.ReferenceManifestValidator;
import com.beust.jcommander.JCommander;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Log4j2
public class Main {

    public static void main(String[] args) {
        Commander commander = new Commander();
        JCommander jc = JCommander.newBuilder().addObject(commander).build();
        try {
            jc.parse(args);
        } catch (Exception e) {
            exitWithErrorCode(e.getMessage());
        }
        SwidTagGateway gateway;
        ReferenceManifestValidator validator;
        List<String> unknownOpts = commander.getUnknownOptions();

        if (!unknownOpts.isEmpty()) {
            StringBuilder sb = new StringBuilder("Unknown options encountered: ");
            for (String opt : unknownOpts) {
                sb.append(opt + ", ");
            }
            exitWithErrorCode(sb.substring(0,sb.lastIndexOf(",")));
        } else if (commander.isHelp()) {
            jc.usage();
            System.out.println(commander.printHelpExamples());
        } else if (commander.isVersion()) {
            try {
                byte[] content = Files.readAllBytes(Paths.get(SwidTagConstants.VERSION_FILE));
                String version = new String(content);
                System.out.println("TCG rimtool version: " + version);
            } catch (IOException e) {
                parseVersionFromJar();
            }
        } else {
            if (!commander.getVerifyFile().isEmpty()) {
                validator = new ReferenceManifestValidator();
                if (commander.isVerbose()) {
                    System.out.println(commander.toString());
                }
                String verifyFile = commander.getVerifyFile();
                String rimel = commander.getRimEventLog();
                String certificateFile = commander.getPublicCertificate();
                String trustStore = commander.getTruststoreFile();
                if (!verifyFile.isEmpty()) {
                    validator.setRim(verifyFile);
                    if (!rimel.isEmpty()) {
                        validator.setRimEventLog(rimel);
                    }
                    if (!trustStore.isEmpty()) {
                        validator.setTrustStoreFile(trustStore);
                    }
                    if (!certificateFile.isEmpty()) {
                        System.out.println("A single cert cannot be used for verification. " +
                                "The signing cert will be searched for in the trust store.");
                    }
                    validator.validateSwidtagFile(verifyFile);
                } else {
                    exitWithErrorCode("A RIM file was not found for validation.");
                }
            } else {
                gateway = new SwidTagGateway();
                if (commander.isVerbose()) {
                    System.out.println(commander.toString());
                }
                String createType = commander.getCreateType().toUpperCase();
                String attributesFile = commander.getAttributesFile();
                String jksTruststoreFile = commander.getTruststoreFile();
                String certificateFile = commander.getPublicCertificate();
                String privateKeyFile = commander.getPrivateKeyFile();
                boolean embeddedCert = commander.isEmbedded();
                boolean defaultKey = commander.isDefaultKey();
                String rimEventLog = commander.getRimEventLog();
                switch (createType) {
                    case "BASE":
                        if (!attributesFile.isEmpty()) {
                            gateway.setAttributesFile(attributesFile);
                        }
                        if (!jksTruststoreFile.isEmpty()) {
                            gateway.setDefaultCredentials(true);
                            gateway.setJksTruststoreFile(jksTruststoreFile);
                        } else if (!certificateFile.isEmpty() && !privateKeyFile.isEmpty()) {
                            gateway.setDefaultCredentials(false);
                            gateway.setPemCertificateFile(certificateFile);
                            gateway.setPemPrivateKeyFile(privateKeyFile);
                            if (embeddedCert) {
                                gateway.setEmbeddedCert(true);
                            }
                        } else if (defaultKey){
                            gateway.setDefaultCredentials(true);
                            gateway.setJksTruststoreFile(SwidTagConstants.DEFAULT_KEYSTORE_FILE);
                        } else {
                            exitWithErrorCode("A private key (-k) and public certificate (-p) " +
                                    "are required, or the default key (-d) must be indicated.");
                        }
                        if (rimEventLog.isEmpty()) {
                            exitWithErrorCode("A support RIM is required.");
                        } else {
                            gateway.setRimEventLog(rimEventLog);
                        }
                        List<String> timestampArguments = commander.getTimestampArguments();
                        if (timestampArguments.size() > 0) {
                            if (new TimestampArgumentValidator(timestampArguments).isValid()) {
                                gateway.setTimestampFormat(timestampArguments.get(0));
                                if (timestampArguments.size() > 1) {
                                    gateway.setTimestampArgument(timestampArguments.get(1));
                                }
                            } else {
                                exitWithErrorCode("The provided timestamp argument(s) " +
                                        "is/are not valid.");
                            }
                        }
                        gateway.generateSwidTag(commander.getOutFile());
                        break;
                    default:
                        exitWithErrorCode("Create type not recognized.");
                }
            }
        }
    }

    /**
     * Use cases that exit with an error code are redirected here.
     */
    private static void exitWithErrorCode(String errorMessage) {
        log.error(errorMessage);
        System.exit(1);
    }

    /**
     * This method parses the version number from the jar filename in the absence of
     * the VERSION file expected with an rpm installation.
     */
    private static void parseVersionFromJar() {
        System.out.println("Installation file VERSION not found.");
        String filename = new File(Main.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath()).getName();
        Pattern pattern = Pattern.compile("(?<=tcg_rim_tool-)[0-9]\\.[0-9]\\.[0-9]");
        Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            System.out.println("TCG rimtool version: " + matcher.group());
        }
    }
}
