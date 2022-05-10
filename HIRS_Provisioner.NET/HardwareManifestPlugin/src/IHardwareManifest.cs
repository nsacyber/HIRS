namespace HardwareManifestPlugin {
    public interface IHardwareManifest {
        string Name {
            get;
        }
        string Description {
            get;
        }

        int Execute(string[] args);
        string asJsonString();
    }
}
