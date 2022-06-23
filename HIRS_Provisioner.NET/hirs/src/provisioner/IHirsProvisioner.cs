using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;

namespace hirs {
    public interface IHirsProvisioner {
        void SetSettings(Settings settings);
        void SetCLI(CLI cli);
        IHirsAcaTpm ConnectTpm();
        void SetClient(IHirsAcaClient clientWithAddress);
        void SetDeviceInfoCollector(IHirsDeviceInfoCollector collector);
        Task<int> Provision(IHirsAcaTpm tpm);
    }
}
