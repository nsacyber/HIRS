package hirs.swid;

import hirs.swid.utils.Commander;
import hirs.swid.utils.CredentialArgumentValidator;
import hirs.swid.utils.TimestampArgumentValidator;
import hirs.utils.rim.ReferenceManifestValidator;
import com.beust.jcommander.JCommander;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        Commander commander = new Commander();
        JCommander jc = JCommander.newBuilder().addObject(commander).build();
        jc.parse(args);
        SwidTagGateway gateway;
        ReferenceManifestValidator validator;
        CredentialArgumentValidator credValidator;

        if (commander.isHelp()) {
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
                System.out.println(commander.toString());
                String verifyFile = commander.getVerifyFile();
                String rimel = commander.getRimEventLog();
                String trustStore = commander.getTruststoreFile();
                if (!verifyFile.isEmpty()) {
                    validator.setRim(verifyFile);
                    if (!rimel.isEmpty()) {
                        validator.setRimEventLog(rimel);
                    } else {
                        System.out.println("A support RIM is required for validation.");
                        System.exit(1);
                    }
                    credValidator = new CredentialArgumentValidator(trustStore,
                            "","", true);
                    if (credValidator.isValid()) {
                        validator.setTrustStoreFile(trustStore);
                    } else {
                        System.out.println(credValidator.getErrorMessage());
                        System.exit(1);
                    }
                    if (validator.validateSwidtagFile(verifyFile)) {
                        System.out.println("Successfully verified " + verifyFile);
                    } else {
                        System.out.println("Failed to verify " + verifyFile);
                        System.exit(1);
                    }
                } else {
                    System.out.println("Need a RIM file to validate!");
                    System.exit(1);
                }
            } else {
                gateway = new SwidTagGateway();
                System.out.println(commander.toString());
                String createType = commander.getCreateType().toUpperCase();
                String attributesFile = commander.getAttributesFile();
                String certificateFile = commander.getPublicCertificate();
                String privateKeyFile = commander.getPrivateKeyFile();
                boolean embeddedCert = commander.isEmbedded();
                boolean defaultKey = commander.isDefaultKey();
                String rimEventLog = commander.getRimEventLog();
                switch (createType) {
                    case "BASE":
                        if (!attributesFile.isEmpty()) {
                            gateway.setAttributesFile(attributesFile);
                        } else {
                            System.out.println("An attribute file is required.");
                            System.exit(1);
                        }
                        if (!rimEventLog.isEmpty()) {
                            gateway.setRimEventLog(rimEventLog);
                        } else {
                            System.out.println("A support RIM is required.");
                            System.exit(1);
                        }
                        credValidator = new CredentialArgumentValidator("" ,
                                certificateFile, privateKeyFile, false);
                        if (defaultKey){
                            gateway.setDefaultCredentials(true);
                            gateway.setJksTruststoreFile(SwidTagConstants.DEFAULT_KEYSTORE_FILE);
                        } else if (credValidator.isValid()) {
                            gateway.setDefaultCredentials(false);
                            gateway.setPemCertificateFile(certificateFile);
                            gateway.setPemPrivateKeyFile(privateKeyFile);
                            if (embeddedCert) {
                                gateway.setEmbeddedCert(true);
                            }
                        } else {
                            System.out.println(credValidator.getErrorMessage());
                            System.exit(1);
                        }
                        List<String> timestampArguments = commander.getTimestampArguments();
                        if (timestampArguments.size() > 0) {
                            if (new TimestampArgumentValidator(timestampArguments).isValid()) {
                                gateway.setTimestampFormat(timestampArguments.get(0));
                                if (timestampArguments.size() > 1) {
                                    gateway.setTimestampArgument(timestampArguments.get(1));
                                }
                            } else {
                                System.exit(1);
                            }
                        }
                        gateway.generateSwidTag(commander.getOutFile());
                        break;
                    default:
                        System.out.println("No create type given, nothing to do");
                        System.exit(1);

                }
            }
        }
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
