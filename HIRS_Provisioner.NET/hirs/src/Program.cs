using CommandLine;
using Serilog;
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Security.Principal;
using System.Threading.Tasks;

namespace hirs {
    class Program {
        public static readonly string VERSION = "17";

        static async Task<int> Main(string[] args) {
            ClientExitCodes result = 0;
            try {
                Settings settings = Settings.LoadSettingsFromDefaultFile();
                settings.SetUpLog();
                Log.Information("Starting hirs version " + VERSION);
                if (!IsRunningAsAdmin()) {
                    result = ClientExitCodes.NOT_PRIVILEGED;
                    Log.Warning("The HIRS provisioner is not running as administrator.");
                }
                settings.CompleteSetUp();
                CLI cli = new();
                Log.Debug("Parsing CLI args.");
                ParserResult<CLI> cliParseResult =
                    CommandLine.Parser.Default.ParseArguments<CLI>(args)
                        .WithParsed(parsed => cli = parsed)
                        .WithNotParsed(HandleParseError);

                if (cliParseResult.Tag == ParserResultType.NotParsed) {
                    // Help text requested, or parsing failed. Exit.
                    Log.Warning("Could not parse command line arguments. Set --tcp --sim, --tcp <ip>:<port>, --nix, or --win. See documentation for further assistance.");
                } else {
                    Provisioner p = new(settings, cli);
                    IHirsAcaTpm tpm = p.ConnectTpm();
                    p.UseClassicDeviceInfoCollector();
                    result = (ClientExitCodes)await p.Provision(tpm);
                    Log.Information("----> Provisioning " + (result == 0 ? "successful" : "failed") + ".");
                }
            } catch (Exception e) {
                result = ClientExitCodes.FAIL;
                Log.Fatal(e, "Application stopped.");
            }
            Log.CloseAndFlush();

            return (int)result;
        }

        private static void HandleParseError(IEnumerable<Error> errs) {
            //handle errors
            Log.Error("There was a CLI error: " + errs.ToString());
        }

        private static bool IsRunningAsAdmin() {
            bool isAdmin = false;
            try {
                if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows)) {
                    WindowsIdentity user = WindowsIdentity.GetCurrent();
                    WindowsPrincipal principal = new(user);
                    isAdmin = principal.IsInRole(WindowsBuiltInRole.Administrator);
                } else {
                    isAdmin = Mono.Unix.Native.Syscall.geteuid() == 0;
                }
            } catch { }
            return isAdmin;
        }
    }
}
