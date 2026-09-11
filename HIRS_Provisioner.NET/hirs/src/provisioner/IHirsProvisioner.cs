namespace hirs {
    public interface IHirsProvisioner {
        IHirsAcaTpm ConnectTpm();
        void SetClient(IHirsAcaClient clientWithAddress);
        void SetDeviceInfoCollector(IHirsDeviceInfoCollector collector);
        Task<int> Provision(IHirsAcaTpm tpm);
    }
}
