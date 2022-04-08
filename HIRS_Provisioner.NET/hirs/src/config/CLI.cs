using CommandLine;
using System;
using System.Collections.Generic;
using System.Text;

namespace hirs {
    public class CLI {
        [Option(SetName="type", Default = false, HelpText = "Connect to the TPM by IP. Use the format ip:port. By default will connect to " + CommandTpm.DefaultSimulatorNamePort + ".")]
        public bool tcp {
            get; set;
        }

        [Option(SetName = "type", Default = false, HelpText = "Connect to a Windows TPM device.")]
        public bool win {
            get; set;
        }

        [Option(SetName = "type", Default = false, HelpText = "Connect to a Linux TPM device.")]
        public bool nix {
            get; set;
        }

        [Option(Default = false, HelpText = "Notify the program of intent to connect to a TPM simulator.")]
        public bool sim {
            get; set;
        }

        [Option(Default = CommandTpm.DefaultSimulatorNamePort, HelpText = "IP of the TPM Device. Use the format ip:port.")]
        public string ip {
            get; set;
        }

        [Option(Default = false, HelpText = "Clear any existing hirs AK and create a new one.")]
        public bool replaceAK {
            get; set;
        }
    }
}
