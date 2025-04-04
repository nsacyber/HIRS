package hirs.swid.utils;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.Getter;

@Parameters(parametersValidators = VerifyArgumentValidator.class)
@Getter
public class CommandVerify {
    @Parameter(names = {"--in"}, validateWith = FileArgumentValidator.class,
            description = "")
    private String inFile = "";
    @Parameter(names = {"-l", "--rimel"}, validateWith = FileArgumentValidator.class,
            description = "The TCG eventlog file to use as a support RIM.")
    private String rimEventLog = "";
    @Parameter(names = {"-t", "--truststore"}, validateWith = FileArgumentValidator.class,
            description = "The truststore to sign the base RIM created "
                    + "or to validate the signed base RIM.")
    private String truststore = "";
}
