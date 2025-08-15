package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import hirs.utils.signature.cose.Cbor.CborItems;

/**
 * Section 5.1.4.1.4.5 of the IETF CoRim specification.
 * <pre>
 * flags-map = {
 *      ? &amp;(is-configured: 0) => bool
 *      ? &amp;(is-configured: 1) => bool
 *      ? &amp;(is-recovery: 2) => bool
 *      ? &amp;(is-debug: 3) => bool
 *      ? &amp;(is-replay-protected: 4) => bool
 *      ? &amp;(is-integrity-protected: 5) => bool
 *      ? &amp;(is-runtime-meas: 6) => bool
 *      ? &amp;(is-immutable: 7) => bool
 *      ? &amp;(is-tcb: 8) => bool
 *      ? &amp;(is-confidentiality-protected: 9) => bool
 *      * $$flags-map-extension
 *    }
 * </pre>
 */
public class FlagsItems extends CborItems {
    public static final int IS_CONFIGURED_INT = 0;
    public static final int IS_SECURE_INT = 1;
    public static final int IS_RECOVERY_INT = 2;
    public static final int IS_DEBUG_INT = 3;
    public static final int IS_REPLAY_PROTECTED_INT = 4;
    public static final int IS_INTEGRITY_PROTECTED_INT = 5;
    public static final int IS_RUNTIME_MEAS_INT = 6;
    public static final int IS_IMMUTABLE_INT = 7;
    public static final int IS_TCB_INT = 8;
    public static final int IS_CONFIDENTIALITY_PROTECTED_INT = 9;

    public static final String IS_CONFIGURED_STR = "is-configured";
    public static final String IS_SECURE_STR = "is-configured";
    public static final String IS_RECOVERY_STR = "is-recovery";
    public static final String IS_DEBUG_STR = "is-debug";
    public static final String IS_REPLAY_PROTECTED_STR = "is-replay-protected";
    public static final String IS_INTEGRITY_PROTECTED_STR = "is-integrity-protected";
    public static final String IS_RUNTIME_MEAS_STR = "is-runtime-meas";
    public static final String IS_IMMUTABLE_STR = "is-immutable";
    public static final String IS_TCB_STR = "is-tcb";
    public static final String IS_CONFIDENTIALITY_PROTECTED_STR = "is-confidentiality-protected";

    private static final String[][] INDEX_NAMES = {
            {Integer.toString(IS_CONFIGURED_INT), IS_CONFIGURED_STR },
            {Integer.toString(IS_SECURE_INT), IS_SECURE_STR },
            {Integer.toString(IS_RECOVERY_INT), IS_RECOVERY_STR },
            {Integer.toString(IS_DEBUG_INT), IS_DEBUG_STR },
            {Integer.toString(IS_REPLAY_PROTECTED_INT), IS_REPLAY_PROTECTED_STR },
            {Integer.toString(IS_INTEGRITY_PROTECTED_INT), IS_INTEGRITY_PROTECTED_STR },
            {Integer.toString(IS_RUNTIME_MEAS_INT), IS_RUNTIME_MEAS_STR },
            {Integer.toString(IS_IMMUTABLE_INT), IS_IMMUTABLE_STR },
            {Integer.toString(IS_TCB_INT), IS_TCB_STR },
            {Integer.toString(IS_CONFIDENTIALITY_PROTECTED_INT), IS_CONFIDENTIALITY_PROTECTED_STR }
    };
}
