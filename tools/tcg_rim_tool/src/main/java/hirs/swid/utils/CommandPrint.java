package hirs.swid.utils;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.Getter;

@Parameters
@Getter
public class CommandPrint {
    @Parameter(names = {"--in"},
            validateWith = FileArgumentValidator.class,
            description = "The path of the file to print")
    private String inFile = "";
    @Parameter(names = {"-h", "--help"}, help = true, description = "Print this help text.")
    private boolean help;
}
