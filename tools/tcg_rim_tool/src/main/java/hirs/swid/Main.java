package hirs.swid;

import hirs.swid.utils.Commander;
import hirs.swid.utils.TimestampArgumentValidator;
import hirs.utils.rim.ReferenceManifestValidator;
import com.beust.jcommander.JCommander;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        Commander commander = new Commander();
        JCommander jc = JCommander.newBuilder().addObject(commander).build();
        jc.parse(args);
        SwidTagGateway gateway;
        ReferenceManifestValidator validator;

        if (commander.isHelp()) {
            jc.usage();
            System.out.println(commander.printHelpExamples());
        } else {
            if (!commander.getVerifyFile().isEmpty()) {
                validator = new ReferenceManifestValidator();
                System.out.println(commander.toString());
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
                    System.out.println("Need a RIM file to validate!");
                    System.exit(1);
                }
            } else {
                gateway = new SwidTagGateway();
                System.out.println(commander.toString());
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
                            System.out.println("A private key (-k) and public certificate (-p) " +
                                    "are required, or the default key (-d) must be indicated.");
                            System.exit(1);
                        }
                        if (rimEventLog.isEmpty()) {
                            System.out.println("Error: a support RIM is required!");
                            System.exit(1);
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
