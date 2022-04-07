using CommandLine;

namespace pcrextend {
    class Program {
        static void Main(string[] args) {
            bool result = false;
            try {
                CLI cli = new();
                ParserResult<CLI> cliParseResult =
                    CommandLine.Parser.Default.ParseArguments<CLI>(args)
                        .WithParsed(parsed => cli = parsed)
                        .WithNotParsed(HandleParseError);

                if (cliParseResult.Tag == ParserResultType.Parsed) {
                    // filter pci index
                    int pcr = cli.Pcr;
                    if (pcr < 0 || pcr > 23) {
                        Console.WriteLine("Unknown PCR index: " + pcr + ". Should be from 0 to 23.");
                        return;
                    }
                    PcrExtendTool p = new(cli);
                    CommandTpmSimulator? tpm = p.connectTpm();
                    if (tpm != null) {
                        result = p.Extend(tpm);
                    }
                    Console.WriteLine("PCR Extend " + (result ? "" : "un") + "successful.");
                }
            } catch (Exception e) {
                Console.WriteLine("Application stopped.");
                Console.WriteLine(e.ToString());
                Console.WriteLine(e.StackTrace);
            }
        }

        private static void HandleParseError(IEnumerable<Error> errs) {
            if (!errs.IsHelp() && !errs.IsVersion()) {
                //handle errors
                Console.WriteLine("There was a CLI error: " + errs.ToString());
            }
        }
    }
}
