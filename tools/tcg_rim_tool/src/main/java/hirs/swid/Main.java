package hirs.swid;

import com.beust.jcommander.JCommander;
import hirs.swid.utils.Commander;
import hirs.swid.utils.CredentialArgumentValidator;
import hirs.swid.utils.TimestampArgumentValidator;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        Commander commander = new Commander();
        JCommander jc = JCommander.newBuilder().addObject(commander).build();
        jc.parse(args);
        SwidTagGateway gateway;
        SwidTagValidator validator;
        CredentialArgumentValidator caValidator;

        if (commander.isHelp()) {
            jc.usage();
            System.out.println(commander.printHelpExamples());
        } else {
            if (!commander.getVerifyFile().isEmpty()) {
                validator = new SwidTagValidator();
                System.out.println(commander.toString());
                String verifyFile = commander.getVerifyFile();
                String directory = commander.getDirectoryOverride();
                String certificateFile = commander.getPublicCertificate();
                String trustStore = commander.getTruststoreFile();
                boolean defaultKey = commander.isDefaultKey();
                validator.setRimEventLog(rimel);
                if (defaultKey) {
                    validator.validateSwidTag(verifyFile, "DEFAULT");
                } else {
                    caValidator = new CredentialArgumentValidator(trustStore,
                            certificateFile, "", "", "", true);
                    if (caValidator.isValid()) {
                        validator.setTrustStoreFile(trustStore);
                        validator.validateSwidTag(verifyFile, caValidator.getFormat());
                    } else {
                        System.out.println("Invalid combination of credentials given: "
                                + caValidator.getErrorMessage());
                        System.exit(1);
                    }
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
                String directory = commander.getDirectoryOverride();
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
                            caValidator = new CredentialArgumentValidator(truststoreFile,
                                    certificateFile, privateKeyFile, "", "", false);
                            if (caValidator.isValid()) {
                                gateway.setTruststoreFile(truststoreFile);
                                gateway.setPemCertificateFile(certificateFile);
                                gateway.setPemPrivateKeyFile(privateKeyFile);
                            } else {
                                System.out.println("Invalid combination of credentials given: "
                                        + caValidator.getErrorMessage());
                                System.exit(1);
                            }
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
                        gateway.setDirectoryOverride(directory);
                        gateway.generateSwidTag(commander.getOutFile());
                        break;
                    default:
                        System.out.println("No create type given, nothing to do");
                }
            }
        }
    }
}
