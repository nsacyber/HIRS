using CommandLine;
using Serilog;
using System;
using System.Collections.Generic;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Security.Principal;
using System.Threading.Tasks;
using Tpm2Lib;

namespace hirs {
    class Program {
        public static readonly string VERSION = typeof(Program).Assembly
            .GetCustomAttribute<AssemblyInformationalVersionAttribute>()?
            .InformationalVersion;

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

                if (cliParseResult.Tag == ParserResultType.NotParsed
                    && !cliParseResult.Errors.IsHelp()
                    && !cliParseResult.Errors.IsVersion()) {
                    // Parsing failed. Exit.
                    Log.Warning("Could not parse command line arguments. Set --tcp --sim, --tcp <ip>:<port>, --nix, or --win. See documentation for further assistance.");
                } else {
                    Provisioner p = new(settings, cli);
                    IHirsAcaTpm tpm = p.ConnectTpm();
                    p.UseClassicDeviceInfoCollector();
                    result = (ClientExitCodes)await p.Provision(tpm);
                    Log.Information("----> Provisioning " + (result == 0 ? "successful" : "failed") + ".");
                }
            } catch (AcaConnectionException e) {
                result = ClientExitCodes.ACA_UNREACHABLE;
                Log.Error(e, "Network connection to the ACA failed. Verify the ACA address, network connection, and server availability.");
            } catch (AcaClientException e) {
                result = ClientExitCodes.EXTERNAL_APP_ERROR;
                Log.Error(e, "ACA client failure. The ACA response could not be used to continue provisioning.");
            } catch (ProvisioningFailureException e) {
                result = e.ExitCode;
                Log.Error(e, e.Message);
            } catch (TpmException e) {
                result = ClientExitCodes.TPM_ERROR;
                Log.Error(e, "TPM failure. Check that the TPM is available and that the provisioner has the required permissions.");
            } catch (Exception e) {
                result = ClientExitCodes.FAIL;
                Log.Error(e, "Provisioning Failed. See details on the ACA.");
            }
            Log.CloseAndFlush();

            return (int)result;
        }

        private static void HandleParseError(IEnumerable<Error> errs) {
            IEnumerable<Error> enumerable = errs.ToList();
            if (enumerable.IsHelp() || enumerable.IsVersion()) 
            {
                return;
            }
            
            //handle errors
            foreach (Error err in enumerable) {
                Log.Error($"There was a CLI error: {err.Tag}");
            }
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
