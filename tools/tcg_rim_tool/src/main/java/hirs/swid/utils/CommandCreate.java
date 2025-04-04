package hirs.swid.utils;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.Getter;

@Parameters(parametersValidators = CreateArgumentValidator.class)
@Getter
public class CommandCreate {
    @Parameter(names = {"-a", "--attributes"}, validateWith = FileArgumentValidator.class,
            description = "The configuration file holding attributes "
                    + "to populate the base RIM with.  An example file can be found in /opt/rimtool/data.")
    private String attributesFile = "";
    @Parameter(names = {"-l", "--rimel"}, validateWith = FileArgumentValidator.class,
            description = "The TCG eventlog file to use as a support RIM.")
    private String rimEventLog = "";
    @Parameter(names = {"-o", "--out"},
            description = "The file to write the RIM out to. "
                    + "The RIM will be written to stdout by default.")
    private String outFile = "";
}
