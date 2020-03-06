package hirs.swid;

import hirs.swid.utils.Commander;
import java.io.IOException;

/*
 * Command-line application for generating and validating SWID tags.
 * Input arg: path to *.swidtag file
 * 
 * If an argument is given it will be validated against the schema at http://standards.iso.org/iso/19770/-2/2015/schema.xsd
 * If an argument is not given a SWID tag file will be generated.
 */
public class Main {

    public static void main(String[] args) {
        Commander commander = new Commander(args);
        SwidTagGateway gateway = new SwidTagGateway();

        if (commander.hasArguments()) {
            // we have arguments to work with
            if (commander.isAttributesGiven()) {
                gateway.setAttributesFile(commander.getAttributesFile());
            }
            if (commander.isKeystoreGiven()) {
                gateway.setKeystoreFile(commander.getKeystore());
            }
            if (commander.isShowCert()) {
                gateway.setShowCert(true);
            }

            if (commander.create()) {
                // parsing the arguments detected a create parameter (-c)
                gateway.generateSwidTag(commander.getCreateOutFile());
            }
            if (commander.validate()) {
                // parsing the arguments detected a validation parameter (-v)
                try {
                    gateway.validateSwidTag(commander.getValidateFile());
                } catch (IOException e) {
                    System.out.println("Unable to validate file: " + e.getMessage());
                }
            }
            if (commander.parse()) {
                try {
                    gateway.parsePayload(commander.getParseFile());
                } catch (IOException e) {
                    System.out.println("Unable to parse file: " + e.getMessage());
                }
            }
        }
    }
}
