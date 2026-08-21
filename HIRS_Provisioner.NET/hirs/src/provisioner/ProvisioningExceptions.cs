using System;

namespace hirs {
    public sealed class AcaConnectionException : Exception {
        public AcaConnectionException(string message, Exception innerException)
            : base(message, innerException) { }
    }

    public sealed class AcaClientException : Exception {
        public AcaClientException(string message, Exception innerException = null)
            : base(message, innerException) { }
    }

    public sealed class ProvisioningFailureException : Exception {
        public ClientExitCodes ExitCode { get; }

        public ProvisioningFailureException(ClientExitCodes exitCode, string message, Exception innerException = null)
            : base(message, innerException) {
            ExitCode = exitCode;
        }
    }
}
