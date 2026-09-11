using Hirs.Pb;

namespace hirs {
    public interface IHirsDeviceInfoCollector {
        DeviceInfo CollectDeviceInfo(string address);
    }
}
