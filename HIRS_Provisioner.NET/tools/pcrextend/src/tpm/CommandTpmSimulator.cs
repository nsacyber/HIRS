using Tpm2Lib;

namespace pcrextend {
    public class CommandTpmSimulator {
        /// <summary>
        /// If using a TCP connection, the default DNS name/IP address for the
        /// simulator.
        /// </summary>
        public const string DefaultSimulatorNamePort = "127.0.0.1:2321";

        private readonly Tpm2 tpm;

        private readonly Boolean simulator;

        /**
         * For TCP TpmDevices
         */
        public CommandTpmSimulator(string ip, int port) {
            simulator = true;
            Tpm2Device tpmDevice = new TcpTpmDevice(ip, port);
            Console.WriteLine("Connecting to TPM at " + ip + ":" + port);
            tpm = tpmSetupByType(tpmDevice);
        }

        ~CommandTpmSimulator() {
            if (tpm != null) {
                tpm.Dispose();
            }
        }

        public void pcrextend(int pcr, string hashAlgStr, List<byte[]> digestList) {
            TpmHandle pcrHandle = TpmHandle.Pcr(pcr);
            TpmAlgId hashAlg = Enum.Parse<TpmAlgId>(hashAlgStr, true);
            List<TpmHash> digests = new();
            foreach (byte[] digest in digestList) {
                digests.Add(new TpmHash(hashAlg, digest));
            }
            tpm.PcrExtend(pcrHandle, digests.ToArray());
        }

        private Tpm2 tpmSetupByType(Tpm2Device tpmDevice) {
            tpmDevice.Connect();

            Tpm2 tpm = new Tpm2(tpmDevice);
            if (tpmDevice is TcpTpmDevice) {
                //
                // If we are using the simulator, we have to do a few things the
                // firmware would usually do. These actions have to occur after
                // the connection has been established.
                // 
                if (simulator) {
                    uint rc = 0;
                    try {
                        rc = tpm.PcrRead(new PcrSelection[] { new PcrSelection(TpmAlgId.Sha1, new uint[] { 0 }) }, out _, out _);
                    } catch (TpmException e) {
                        if (e.RawResponse == TpmRc.Initialize) {
                            Console.WriteLine("TPM simulator not initialized. Running startup with clear.");
                            tpmDevice.PowerCycle();
                            tpm.Startup(Su.Clear);
                        } else {
                            Console.WriteLine("TPM simulator already initialized. Skipping TPM2_Startup.");
                        }
                    }
                }
            } 
            return tpm;
        }
    }
}
