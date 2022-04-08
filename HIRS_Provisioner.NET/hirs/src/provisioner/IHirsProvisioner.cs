using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;

namespace hirs {
    public interface IHirsProvisioner {
        void setSettings(Settings settings);
        void setCLI(CLI cli);
        IHirsAcaTpm connectTpm();
        void setClient(IHirsAcaClient clientWithAddress);
        void setDeviceInfoCollector(IHirsDeviceInfoCollector collector);
        Task<int> provision(IHirsAcaTpm tpm);
    }
}
