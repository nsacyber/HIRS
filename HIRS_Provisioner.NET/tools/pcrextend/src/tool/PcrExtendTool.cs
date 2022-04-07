namespace pcrextend {
    public class PcrExtendTool {
        private CLI? cli = null;

        public PcrExtendTool(CLI cli) {
            SetCLI(cli);
        }

        public void SetCLI(CLI cli) {
            this.cli = cli;
        }

        public CommandTpmSimulator? connectTpm() {
            CommandTpmSimulator? tpm = null;
            if (cli != null && cli.Ip != null) {
                string[] split = cli.Ip.Split(":");
                if (split.Length == 2) {
                    try {
                        tpm = new CommandTpmSimulator(split[0], Int32.Parse(split[1]));
                    } catch (Exception) {
                        Console.WriteLine("No tpm found at " + cli.Ip);
                    }
                } else {
                    Console.WriteLine("ip input should have the format servername:port. The given input was '" + cli.Ip + "'.");
                }
            }

            return tpm;
        }

        public bool Extend(CommandTpmSimulator tpm) {
            bool result = false;
            if (tpm != null && cli != null) {
                // parse hashAlg
                string hashAlg = cli.HashAlg;
                if (hashAlg.StartsWith("0x", StringComparison.OrdinalIgnoreCase) || hashAlg.EndsWith("h")) {
                    hashAlg = hashAlg.Replace("0x", "");
                    hashAlg = hashAlg.TrimEnd('h');
                    hashAlg = "" + Convert.ToInt64(hashAlg, 16);
                }

                string digestCSV = cli.Digests;
                digestCSV = digestCSV.ToUpper().Replace("0X", "").Replace("H", "");
                string[] digestArray = digestCSV.Split(",");
                List<byte[]> digestList = new();
                foreach (string digest in digestArray) {
                    digestList.Add(Convert.FromHexString(digest));
                }
                tpm.pcrextend(cli.Pcr, hashAlg, digestList);
                result = true;
            }
            return result;
        }
    }
}
