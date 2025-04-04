package hirs.swid.utils;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Parameters
@Getter
public class CommandSign {
    @Parameter(names = {"--in"}, validateWith = FileArgumentValidator.class,
            description = "")
    private String inFile = "";
    @Parameter(names = {"-d", "--default-key"},
            description = "Use the JKS keystore installed in /opt/rimtool/data.")
    private boolean defaultKey = false;
    @Parameter(names = {"-t", "--truststore"}, validateWith = FileArgumentValidator.class,
            description = "The truststore to sign the base RIM created "
                    + "or to validate the signed base RIM.")
    private String truststore = "";
    @Parameter(names = {"-p", "--publicCertificate"},
            validateWith = FileArgumentValidator.class,
            description = "The public key certificate to embed in the base RIM created by "
                    + "this tool.")
    private String publicCertificate = "";
    @Parameter(names = {"-k", "--privateKeyFile"},
            validateWith = FileArgumentValidator.class,
            description = "The private key used to sign the base RIM created by this tool.")
    private String privateKey = "";
    @Parameter(names = {"-e", "--embed-cert"},
            description = "Embed the provided certificate in the signed swidtag.")
    private boolean embedded = false;
    @Parameter(names = {"--timestamp"}, variableArity = true,
            description = "Add a timestamp to the signature. " +
                    "Currently only RFC3339 and RFC3852 are supported:\n" +
                    "\tRFC3339 [yyyy-MM-ddThh:mm:ssZ]\n\tRFC3852 <counterSignature.bin>")
    private List<String> timestampArguments = new ArrayList<String>(2);
}
