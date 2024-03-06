package hirs.swid.utils;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import hirs.swid.SwidTagConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Commander is a class that handles the command line arguments for the SWID
 * Tags gateway by implementing the JCommander package.
 */
@Parameters(parametersValidators = {CreateArgumentValidator.class, VerifyArgumentValidator.class})
public class Commander {

    @Parameter(description = "This parameter catches all unrecognized arguments.")
    private List<String> unknownOptions = new ArrayList<>();
    @Parameter(names = {"-h", "--help"}, help = true, description = "Print this help text.")
    private boolean help;
    @Parameter(names = {"-c", "--create"}, order = 0,
            description = "The type of RIM to create. A base RIM will be created by default.")
    private String createType = "";
    @Parameter(names = {"-v", "--verify"}, validateWith = FileArgumentValidator.class,
            description = "Specify a RIM file to verify.")
    private String verifyFile = "";
    @Parameter(names = {"-V", "--version"}, description = "Output the current version.")
    private boolean version = false;
    @Parameter(names = {"-a", "--attributes"}, validateWith = FileArgumentValidator.class,
            description = "The configuration file holding attributes "
            + "to populate the base RIM with.  An example file can be found in /opt/rimtool/data.")
    private String attributesFile = "";
    @Parameter(names = {"-o", "--out"}, order = 2,
            description = "The file to write the RIM out to. "
            + "The RIM will be written to stdout by default.")
    private String outFile = "";
    @Parameter(names = {"--verbose"}, description = "Control output verbosity.")
    private boolean verbose = false;
    @Parameter(names = {"-t", "--truststore"}, validateWith = FileArgumentValidator.class,
            description = "The truststore to sign the base RIM created "
            + "or to validate the signed base RIM.")
    private String truststoreFile = "";
    @Parameter(names = {"-k", "--privateKeyFile"},
            validateWith = FileArgumentValidator.class,
            description = "The private key used to sign the base RIM created by this tool.")
    private String privateKeyFile = "";
    @Parameter(names = {"-p", "--publicCertificate"},
            validateWith = FileArgumentValidator.class,
            description = "The public key certificate to embed in the base RIM created by "
            + "this tool.")
    private String publicCertificate = "";
    @Parameter(names = {"-e", "--embed-cert"}, order = 7,
            description = "Embed the provided certificate in the signed swidtag.")
    private boolean embedded = false;
    @Parameter(names = {"-d", "--default-key"}, order = 8,
            description = "Use the JKS keystore installed in /opt/rimtool/data.")
    private boolean defaultKey = false;
    @Parameter(names = {"-l", "--rimel"}, validateWith = FileArgumentValidator.class,
            description = "The TCG eventlog file to use as a support RIM.")
    private String rimEventLog = "";
    @Parameter(names = {"--timestamp"}, order = 10, variableArity = true,
            description = "Add a timestamp to the signature. " +
                    "Currently only RFC3339 and RFC3852 are supported:\n" +
                    "\tRFC3339 [yyyy-MM-ddThh:mm:ssZ]\n\tRFC3852 <counterSignature.bin>")
    private List<String> timestampArguments = new ArrayList<String>(2);

    public List<String> getUnknownOptions() {
        return unknownOptions;
    }

    public boolean isHelp() {
        return help;
    }

    public String getCreateType() {
        return createType;
    }

    public String getVerifyFile() {
        return verifyFile;
    }

    public boolean isVersion() {
        return version;
    }
    public boolean isVerbose() { return verbose; }
    public String getAttributesFile() {
        return attributesFile;
    }

    public String getOutFile() {
        return outFile;
    }

    public String getTruststoreFile() { return truststoreFile; }

    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    public String getPublicCertificate() {
        return publicCertificate;
    }

    public boolean isEmbedded() { return embedded; }

    public boolean isDefaultKey() { return defaultKey; }

    public String getRimEventLog() { return rimEventLog; }

    public List<String> getTimestampArguments() {
        return timestampArguments;
    }

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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Creating: " + this.getCreateType() + System.lineSeparator());
        sb.append("Using attributes file: " + this.getAttributesFile() + System.lineSeparator());
        sb.append("Write to: " + this.getOutFile() + System.lineSeparator());
        sb.append("Verify file: " + this.getVerifyFile() + System.lineSeparator());
        if (!this.getTruststoreFile().isEmpty()) {
            sb.append("Truststore file: " + this.getTruststoreFile() + System.lineSeparator());
        } else if (!this.getPrivateKeyFile().isEmpty() &&
                    !this.getPublicCertificate().isEmpty()) {
            sb.append("Private key file: " + this.getPrivateKeyFile() + System.lineSeparator());
            sb.append("Public certificate: " + this.getPublicCertificate()
                    + System.lineSeparator());
            sb.append("Embedded certificate: " + this.isEmbedded() + System.lineSeparator());
        } else if (this.isDefaultKey()){
            sb.append("Truststore file: default (" + SwidTagConstants.DEFAULT_KEYSTORE_FILE + ")"
                    + System.lineSeparator());
        } else {
            sb.append("Signing credential: (none given)" + System.lineSeparator());
        }
        sb.append("Event log support RIM: " + this.getRimEventLog() + System.lineSeparator());
        List<String> timestampArguments = this.getTimestampArguments();
        if (timestampArguments.size() > 0) {
            sb.append("Timestamp format: " + timestampArguments.get(0));
            if (timestampArguments.size() == 2) {
                sb.append(", " + timestampArguments.get(1));
            }
        } else {
            sb.append("No timestamp included");
        }
        return sb.toString();
    }
}
