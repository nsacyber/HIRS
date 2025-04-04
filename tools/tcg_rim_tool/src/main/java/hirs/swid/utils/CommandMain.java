package hirs.swid.utils;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import hirs.swid.SwidTagConstants;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Commander is a class that handles the command line arguments for the SWID
 * Tags gateway by implementing the JCommander package.
 */
@Parameters
@Getter
public class CommandMain {

    @Parameter(description = "This parameter catches all unrecognized arguments.")
    private List<String> unknownOptions = new ArrayList<>();
    @Parameter(names = {"-h", "--help"}, help = true, description = "Print this help text.")
    private boolean help;
    @Parameter(names = {"--version"}, description = "Output the current version.")
    private boolean version = false;
    @Parameter(names = {"--verbose"}, description = "Control output verbosity.")
    private boolean verbose = false;

    public String printHelpExamples() {
        StringBuilder sb = new StringBuilder();
        sb.append("Create a base RIM: use the values in attributes.json; ");
        sb.append("add support_rim.bin to the payload; ");
        sb.append("sign it using privateKey.pem and cert.pem; embed cert.pem in the signature; ");
        sb.append("add a RFC3852 timestamp; and write the data to base_rim.swidtag:\n\n");
        sb.append("\t\t-c base -a attributes.json -l support_rim.bin "
                + "-k privateKey.pem -p cert.pem -e --timestamp RFC3852 counterSignature.bin "
                + "-o base_rim.swidtag\n\n\n");
        sb.append("Validate base_rim.swidtag: "
                + "the payload <File> is validated with support_rim.bin; "
                + "and the signature is validated with ca.crt:\n\n");
        sb.append("\t\t-v base_rim.swidtag -l support_rim.bin -t ca.crt\n\n\n");

        return sb.toString();
    }
}
