using Serilog;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Security.Principal;
using System.Text;

namespace hirs {
    class TbsWrapper {
        public class NativeMethods {
            [DllImport("tbs.dll", CharSet = CharSet.Unicode)]
            internal static extern TBS_RESULT
            Tbsi_Context_Create(
                ref TBS_CONTEXT_PARAMS ContextParams,
                ref UIntPtr Context);

            [DllImport("tbs.dll", CharSet = CharSet.Unicode)]
            internal static extern TBS_RESULT
            Tbsip_Context_Close(
                UIntPtr Context);

            [DllImport("tbs.dll", CharSet = CharSet.Unicode)]
            internal static extern TBS_RESULT
                Tbsi_Get_OwnerAuth(
                UIntPtr Context,
                [System.Runtime.InteropServices.MarshalAs(UnmanagedType.U4), In]
                TBS_OWNERAUTH_TYPE OwnerAuthType,
                [System.Runtime.InteropServices.MarshalAs(UnmanagedType.LPArray, SizeParamIndex = 3), In, Out]
                byte[] OutBuffer,
                ref uint OutBufferSize);

            [DllImport("tbs.dll", CharSet = CharSet.Unicode)]
            internal static extern TBS_RESULT
                Tbsi_Get_TCG_Log(
                UIntPtr Context,
                [System.Runtime.InteropServices.MarshalAs(UnmanagedType.LPArray, SizeParamIndex = 2), In, Out]
                byte[] pOutputBuf,
                ref uint pOutputBufLen);
        }

        public enum TBS_RESULT : uint {
            TBS_SUCCESS = 0,
            TBS_E_BLOCKED = 0x80280400,
            TBS_E_INTERNAL_ERROR = 0x80284001,
            TBS_E_BAD_PARAMETER = 0x80284002,
            TBS_E_INSUFFICIENT_BUFFER = 0x80284005,
            TBS_E_COMMAND_CANCELED = 0x8028400D,
            TBS_E_OWNERAUTH_NOT_FOUND = 0x80284015
        }

        public enum TBS_OWNERAUTH_TYPE : uint {
            TBS_OWNERAUTH_TYPE_FULL = 1,
            TBS_OWNERAUTH_TYPE_ADMIN = 2,
            TBS_OWNERAUTH_TYPE_USER = 3,
            TBS_OWNERAUTH_TYPE_ENDORSEMENT = 4,
            TBS_OWNERAUTH_TYPE_ENDORSEMENT_20 = 12,
            TBS_OWNERAUTH_TYPE_STORAGE_20 = 13
        }

        [StructLayout(LayoutKind.Sequential)]
        public struct TBS_CONTEXT_PARAMS {
            public TBS_CONTEXT_VERSION Version;
            public TBS_CONTEXT_CREATE_FLAGS Flags;
        }

        public enum TBS_CONTEXT_VERSION : uint {
            ONE = 1,
            TWO = 2
        }

        public enum TBS_CONTEXT_CREATE_FLAGS : uint {
            RequestRaw = 0x00000001,
            IncludeTpm12 = 0x00000002,
            IncludeTpm20 = 0x00000004,
        }

        public static bool GetOwnerAuthFromOS(out byte[] ownerAuth) {
            ownerAuth = new byte[0];
            WindowsIdentity identity = WindowsIdentity.GetCurrent();
            WindowsPrincipal principal = new WindowsPrincipal(identity);
            if (!principal.IsInRole(WindowsBuiltInRole.Administrator)) {
                Log.Error("GetOwnerAuthFromOS: run the client with Administrator privileges");
                return false;
            }

            // open context
            TbsWrapper.TBS_CONTEXT_PARAMS contextParams;
            UIntPtr tbsContext = UIntPtr.Zero;
            contextParams.Version = TbsWrapper.TBS_CONTEXT_VERSION.TWO;
            contextParams.Flags = TbsWrapper.TBS_CONTEXT_CREATE_FLAGS.IncludeTpm20;
            TbsWrapper.TBS_RESULT result = TbsWrapper.NativeMethods.Tbsi_Context_Create(ref contextParams, ref tbsContext);

            if (result != TbsWrapper.TBS_RESULT.TBS_SUCCESS) {
                return false;
            }
            if (tbsContext == UIntPtr.Zero) {
                return false;
            }

            // get owner auth size
            uint ownerAuthSize = 0;
            TbsWrapper.TBS_OWNERAUTH_TYPE ownerType = TbsWrapper.TBS_OWNERAUTH_TYPE.TBS_OWNERAUTH_TYPE_STORAGE_20;
            result = TbsWrapper.NativeMethods.Tbsi_Get_OwnerAuth(tbsContext, ownerType, ownerAuth, ref ownerAuthSize);
            if (result != TbsWrapper.TBS_RESULT.TBS_SUCCESS &&
                result != TbsWrapper.TBS_RESULT.TBS_E_INSUFFICIENT_BUFFER) {
                ownerType = TbsWrapper.TBS_OWNERAUTH_TYPE.TBS_OWNERAUTH_TYPE_FULL;
                result = TbsWrapper.NativeMethods.Tbsi_Get_OwnerAuth(tbsContext, ownerType, ownerAuth, ref ownerAuthSize);
                if (result != TbsWrapper.TBS_RESULT.TBS_SUCCESS &&
                    result != TbsWrapper.TBS_RESULT.TBS_E_INSUFFICIENT_BUFFER) {
                    Log.Debug("Failed to get ownerAuthSize.");
                    return false;
                }
            }
            // get owner auth itself
            ownerAuth = new byte[ownerAuthSize];
            result = TbsWrapper.NativeMethods.Tbsi_Get_OwnerAuth(tbsContext, ownerType, ownerAuth, ref ownerAuthSize);
            if (result != TbsWrapper.TBS_RESULT.TBS_SUCCESS) {
                Log.Debug("Failed to get ownerAuth.");
                return false;
            }

            TbsWrapper.NativeMethods.Tbsip_Context_Close(tbsContext);

            return true;
        }

        public static bool GetEventLog(out byte[] eventLog) {
            eventLog = new byte[0];
            WindowsIdentity identity = WindowsIdentity.GetCurrent();
            WindowsPrincipal principal = new WindowsPrincipal(identity);
            if (!principal.IsInRole(WindowsBuiltInRole.Administrator)) {
                Log.Debug("GetEventLog: run the client with Administrator privileges");
                return false;
            }

            // open context
            TbsWrapper.TBS_CONTEXT_PARAMS contextParams;
            UIntPtr tbsContext = UIntPtr.Zero;
            contextParams.Version = TbsWrapper.TBS_CONTEXT_VERSION.TWO;
            contextParams.Flags = TbsWrapper.TBS_CONTEXT_CREATE_FLAGS.IncludeTpm12 | TbsWrapper.TBS_CONTEXT_CREATE_FLAGS.IncludeTpm20;
            TbsWrapper.TBS_RESULT result = TbsWrapper.NativeMethods.Tbsi_Context_Create(ref contextParams, ref tbsContext);

            if (result != TbsWrapper.TBS_RESULT.TBS_SUCCESS) {
                return false;
            }
            if (tbsContext == UIntPtr.Zero) {
                return false;
            }

            // Two calls needed
            // First gets the log size
            uint eventLogSize = 0;
            Log.Debug("Attempting to get the event log size from Tbsi.");
            result = TbsWrapper.NativeMethods.Tbsi_Get_TCG_Log(tbsContext, eventLog, ref eventLogSize);
            if (result != TbsWrapper.TBS_RESULT.TBS_SUCCESS &&
                result != TbsWrapper.TBS_RESULT.TBS_E_INSUFFICIENT_BUFFER) {
                Log.Debug("Failed to get eventLogSize.");
                return false;
            }
            // Second gets the log
            Log.Debug("Attempting to get the event log from Tbsi.");
            eventLog = new byte[eventLogSize];
            result = TbsWrapper.NativeMethods.Tbsi_Get_TCG_Log(tbsContext, eventLog, ref eventLogSize);
            if (result != TbsWrapper.TBS_RESULT.TBS_SUCCESS) {
                Log.Debug("Failed to get eventLog.");
                return false;
            }

            TbsWrapper.NativeMethods.Tbsip_Context_Close(tbsContext);

            return true;
        }
    }
}
