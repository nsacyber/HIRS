using HardwareManifestPlugin;
using Serilog;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;

namespace hirs {
    public class HardwareManifestPluginManager {
        public static readonly string pluginsPath = Path.Combine(Path.GetDirectoryName(Environment.ProcessPath), "plugins");
        public HardwareManifestPluginManager() {
        }
        
        public List<IHardwareManifest> LoadPlugins(List<string> names) {
            string[] pluginDlls = Directory.GetFiles(pluginsPath, "*.dll");
            List<IHardwareManifest> manifests = new List<IHardwareManifest>();
            foreach(string dllPath in pluginDlls) {
                Assembly pluginAssembly = LoadHardwareManifest(dllPath);
                IHardwareManifest manifest = GatherManifests(pluginAssembly, names);
                if (manifest != null) {
                    manifests.Add(manifest);
                }
            }
            if (names.Count > 0) {
                Log.Warning("There was no Hardware Manifest plugin with the name " + (names.Count > 1 ? "s" : "") + string.Join(",", names) + ".");
            }
            return manifests;
        }

        private Assembly LoadHardwareManifest(string relativePath) {
            string fullPath = Path.GetFullPath(relativePath).Replace('\\', Path.DirectorySeparatorChar);

            Log.Warning($"Loading manifest: {fullPath}");
            PluginLoadContext loadContext = new PluginLoadContext(fullPath);
            return loadContext.LoadFromAssemblyName(new AssemblyName(Path.GetFileNameWithoutExtension(fullPath)));
        }

        private IHardwareManifest GatherManifests(Assembly assembly, List<string> names) {
            int count = 0;

            foreach (Type type in assembly.GetTypes()) {
                if (typeof(IHardwareManifest).IsAssignableFrom(type)) {
                    IHardwareManifest result = Activator.CreateInstance(type) as IHardwareManifest;
                    if (result != null && names.Remove(result.Name)) {
                        count++;
                        return result;
                    }
                }
            }

            if (count == 0) {
                string availableTypes = string.Join(",", assembly.GetTypes().Select(t => t.FullName));
                Log.Warning(
                    $"Can't find any type which implements IHardwareManifest in {assembly}.\n" +
                    $"Available types: {availableTypes}");
            }
            return null;
        }
    }
}
