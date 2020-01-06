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

        if (commander.hasArguments()) {
            // we have arguments to work with
            if (commander.create()) {
                // parsing the arguments detected a create parameter (-c)
                (new SwidTagGateway()).generateSwidTag(commander.getCreateInFile(),
                        commander.getCreateOutFile(), commander.getHashAlg());
            } else if (commander.validate()) {
                // parsing the arguments detected a validation parameter (-v)
                try {
                    (new SwidTagGateway()).validateSwidTag(commander.getValidateFile());
                } catch (IOException e) {
                    System.out.println("Unable to validate file: " + e.getMessage());
                }
            } else if (commander.parse()) {
                try {
                    (new SwidTagGateway()).parsePayload(commander.getParseFile());
                } catch (IOException e) {
                    System.out.println("Unable to parse file: " + e.getMessage());
                }
            }
        } else {
                // development stage in which no valid arguments were given
            // therefore generate a mock tag file
            (new SwidTagGateway()).generateSwidTag();
        }
    }
}
