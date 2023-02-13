package hirs.swid;

import hirs.swid.utils.Commander;
import com.beust.jcommander.JCommander;
import hirs.swid.utils.TimestampArgumentValidator;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        Commander commander = new Commander();
        JCommander jc = JCommander.newBuilder().addObject(commander).build();
        jc.parse(args);
        SwidTagGateway gateway;
        SwidTagValidator validator;

        if (commander.isHelp()) {
            jc.usage();
            System.out.println(commander.printHelpExamples());
        } else {
            if (!commander.getVerifyFile().isEmpty()) {
                validator = new SwidTagValidator();
                System.out.println(commander.toString());
                String verifyFile = commander.getVerifyFile();
                String rimel = commander.getRimEventLog();
                String certificateFile = commander.getPublicCertificate();
                String trustStore = commander.getTruststoreFile();
                if (!verifyFile.isEmpty()) {
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
                    validator.validateSwidTag(verifyFile);
                } else {
                    System.out.println("Need a RIM file to validate!");
                    System.exit(1);
                }
            } else {
                gateway = new SwidTagGateway();
                System.out.println(commander.toString());
                String createType = commander.getCreateType().toUpperCase();
                String attributesFile = commander.getAttributesFile();
                String truststoreFile = commander.getTruststoreFile();
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
                        if (defaultKey) {
                            gateway.setDefaultCredentials(true);
                            gateway.setTruststoreFile(SwidTagConstants.DEFAULT_KEYSTORE_FILE);
                        } else {
                            gateway.setDefaultCredentials(false);
                            gateway.setTruststoreFile(truststoreFile);
                            gateway.setPemCertificateFile(certificateFile);
                            gateway.setPemPrivateKeyFile(privateKeyFile);
/*
                            if () {
                                System.out.println("Signing credentials must be provided " +
                                        "if not using defaults");
                                System.exit(1);
                            }
*/
                            if (embeddedCert) {
                                gateway.setEmbeddedCert(true);
                            }
                        }
                        gateway.setRimEventLog(rimEventLog);
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
                }
            }
        }
    }
}
