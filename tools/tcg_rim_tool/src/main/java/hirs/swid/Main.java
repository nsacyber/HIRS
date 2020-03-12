package hirs.swid;

import hirs.swid.utils.Commander;
import com.beust.jcommander.JCommander;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        Commander commander = new Commander();
        JCommander jc = JCommander.newBuilder().addObject(commander).build();
        jc.parse(args);
        SwidTagGateway gateway = new SwidTagGateway();

        if (commander.isHelp()) {
            jc.usage();
            System.out.println(commander.printHelpExamples());
        } else {
            if (!commander.getVerifyFile().isEmpty()) {
                System.out.println(commander.toString());
                String verifyFile = commander.getVerifyFile();
                String publicCertificate = commander.getPublicCertificate();
                if (!verifyFile.isEmpty() && !publicCertificate.isEmpty()) {
                    try {
                        gateway.validateSwidTag(verifyFile);
                    } catch (IOException e) {
                        System.out.println("Error validating RIM file: " + e.getMessage());
                    }
                } else {
                    System.out.println("Need both a RIM file to validate and a public certificate to validate with!");
                }
            } else {
                System.out.println(commander.toString());
                String createType = commander.getCreateType().toUpperCase();
                String attributesFile = commander.getAttributesFile();
                String certificateFile = commander.getPublicCertificate();
                String privateKeyFile = commander.getPrivateKeyFile();
                switch (createType) {
                    case "BASE":
                        if (!attributesFile.isEmpty()) {
                            gateway.setAttributesFile(attributesFile);
                        }
                        if (!certificateFile.isEmpty() && !privateKeyFile.isEmpty()) {
                            gateway.setDefaultCredentials(false);
                            gateway.setPemCertificateFile(certificateFile);
                            gateway.setPemPrivateKeyFile(privateKeyFile);
                        }
                        gateway.generateSwidTag(commander.getOutFile());
                        break;
                    case "EVENTLOG":
                        break;
                    case "PCR":
                        break;
                }
            }
        }
    }
}
