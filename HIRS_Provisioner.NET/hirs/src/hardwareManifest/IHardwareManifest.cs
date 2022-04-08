using Serilog;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Text;

namespace hirs {
    public interface IHardwareManifest {
        public static readonly string pluginsPath =
#if DEBUG
            AppContext.BaseDirectory;
#else
            Path.Combine(Path.GetDirectoryName(Process.GetCurrentProcess().MainModule.FileName), "plugins");
#endif
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
