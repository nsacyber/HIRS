using CommandLine;

namespace pcrextend {
    public class CLI {
        // These fields are controlled by the CommandLineParser library.
        //   CS8618 is not relevant at this time.
        //   Non-nullable field must contain a non-null value when exiting constructor.
#pragma warning disable CS8618 
        [Option('i', "ip", Default = CommandTpmSimulator.DefaultSimulatorNamePort, HelpText = "IP of the TPM Simulator. Use the format ip:port.")]
        public string Ip {

            get; set;
        }
        [Option('p', "pcr", HelpText = "PCR Index 0 thru 23")]
        public short Pcr {
            get; set;
        }
        [Option('a', "hashalg", HelpText = "sha1, sha256, sha384, or sha512. OR their ID Values in decimal (4, 11, 12, 13) or in hex (0x4, 0xB, 0xC, 0xD).")]
        public string HashAlg {
            get; set;
        }

        [Option('d', "digests", HelpText = "Comma separated digest values in hex. e.g. AB,34F53,9785")]
        public string Digests {
            get; set;
        }
#pragma warning restore CS8618
    }
}
