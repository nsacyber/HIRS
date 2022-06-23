using CommandLine;

namespace hirs {
    public class CLI {
        [Option("tcp", SetName="type", Default = false, HelpText = "Connect to the TPM by IP. Use the format ip:port. By default will connect to " + CommandTpm.DefaultSimulatorNamePort + ".")]
        public bool Tcp {
            get; set;
        }

        [Option("win", SetName = "type", Default = false, HelpText = "Connect to a Windows TPM device.")]
        public bool Win {
            get; set;
        }

        [Option("nix", SetName = "type", Default = false, HelpText = "Connect to a Linux TPM device.")]
        public bool Nix {
            get; set;
        }

        [Option("sim", Default = false, HelpText = "Notify the program of intent to connect to a TPM simulator.")]
        public bool Sim {
            get; set;
        }

        [Option("ip", Default = CommandTpm.DefaultSimulatorNamePort, HelpText = "IP of the TPM Device. Use the format ip:port.")]
        public string Ip {
            get; set;
        }

        [Option("replaceAK", Default = false, HelpText = "Clear any existing hirs AK and create a new one.")]
        public bool ReplaceAK {
            get; set;
        }

        public static string[] SplitArgs(string argString) {
            return argString.SplitArgs(true);
        }
    }
}
