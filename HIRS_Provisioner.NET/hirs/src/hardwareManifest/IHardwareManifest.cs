using Serilog;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Text;

namespace hirs {
    public interface IHardwareManifest {
        public static readonly string pluginsPath = //AppContext.BaseDirectory;
            Path.Combine(Path.GetDirectoryName(Environment.ProcessPath), "plugins");
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
