using Google.Protobuf;
using Hirs.Pb;
using System.Security.Cryptography;
using Tpm2Lib;

namespace hirs {
    /**
     * Translates key into templates and handles. 
     */
    public static class KeyTemplateCatalog {
        /**
         * TPMA_OBJECT Bit 8 firmwareLimited
         * Not in this version of ObjectAttr from TSS.MSR
         * SET (1): The object exists only within a firmware-limited hierarchy.
         * CLEAR (0): The object can exist outside a firmware-limited hierarchy
         */
        private const uint FirmwareLimitedBit = 1u << 8;

        /**
         * TCG EK Credential Profile defines indicies for each template. This enables translation.
         */
        private static readonly IReadOnlyDictionary<EkTemplate, uint> EkCertificateNvIndices =
            new Dictionary<EkTemplate, uint> {
                // Low Range (original v2.0 templates)
                [EkTemplate.EkL1] = 0x01c00002,
                [EkTemplate.EkL2] = 0x01c0000a,
                // Classical storage
                [EkTemplate.EkH1] = 0x01c00012,
                [EkTemplate.EkH2] = 0x01c00014,
                [EkTemplate.EkH3] = 0x01c00016,
                [EkTemplate.EkH4] = 0x01c00018,
                [EkTemplate.EkH5] = 0x01c0001a,
                [EkTemplate.EkH6] = 0x01c0001c,
                [EkTemplate.EkH7] = 0x01c0001e,
                [EkTemplate.EkH18] = 0x01c00040,
                [EkTemplate.EkH19] = 0x01c00042,
                [EkTemplate.EkH20] = 0x01c00044,
                [EkTemplate.EkH21] = 0x01c00046,
                // Classical signing
                [EkTemplate.EkH8] = 0x01c00032,
                [EkTemplate.EkH9] = 0x01c00034,
                [EkTemplate.EkH10] = 0x01c00036,
                [EkTemplate.EkH11] = 0x01c00038,
                [EkTemplate.EkH12] = 0x01c0003a,
                [EkTemplate.EkH13] = 0x01c0003c,
                [EkTemplate.EkH14] = 0x01c0003e,
                [EkTemplate.EkH15] = 0x01c00050,
                [EkTemplate.EkH16] = 0x01c00052,
                [EkTemplate.EkH17] = 0x01c00054,
                [EkTemplate.EkH22] = 0x01c00056,
                [EkTemplate.EkH23] = 0x01c00058,
                [EkTemplate.EkH24] = 0x01c0005a,
                [EkTemplate.EkH25] = 0x01c0005c,
                // Post-quantum storage (ML-KEM)
                [EkTemplate.EkH26] = 0x01c00060,
                [EkTemplate.EkH27] = 0x01c00062,
                [EkTemplate.EkH28] = 0x01c00064,
                [EkTemplate.EkH29] = 0x01c00066,
                [EkTemplate.EkH30] = 0x01c00068,
                [EkTemplate.EkH31] = 0x01c0006a,
                // Post-quantum signing (ML-DSA)
                [EkTemplate.EkH32] = 0x01c00070,
                [EkTemplate.EkH33] = 0x01c00072,
                [EkTemplate.EkH34] = 0x01c00074,
                [EkTemplate.EkH35] = 0x01c00076,
                [EkTemplate.EkH36] = 0x01c00078,
                [EkTemplate.EkH37] = 0x01c0007a,
            };

        /**
         * Returns the NV index holding the EK certificate for the template, or
         * null for EkTemplate.Unspecified / unknown templates.
         */
        public static uint? GetEkCertificateNvIndex(EkTemplate template) {
            return EkCertificateNvIndices.TryGetValue(template, out uint index) ? index : null;
        }

        /**
         * PolicyA, the authPolicy of the Low Range templates L-1 and L-2
         * (TPM2_PolicySecret(TPM_RH_ENDORSEMENT)). The High Range templates use
         * PolicyB instead, which is what separates L-1/L-2 from H-1/H-2.
         */
        private static readonly byte[] LowRangeAuthPolicy = [ 
            0x83, 0x71, 0x97, 0x67, 0x44, 0x84,
            0xB3, 0xF8, 0x1A, 0x90, 0xCC, 0x8D,
            0x46, 0xA5, 0xD7, 0x24, 0xFD, 0x52,
            0xD7, 0x6E, 0x06, 0x52, 0x0B, 0x64,
            0xF2, 0xA1, 0xDA, 0x1B, 0x33, 0x14,
            0x69, 0xAA
        ];

        /**
         * 
         * Low-range attributes: userWithAuth is clear and the authPolicy is PolicyA. Both are visible
         * in the created object's public area.
         * Test the TPM_PUBLIC for whether it is a key in the low range.
         */
        private static bool IsLowRange(TpmPublic pub) {
            return (pub.objectAttributes & ObjectAttr.UserWithAuth) == 0
                && (pub.objectAttributes & ObjectAttr.AdminWithPolicy) != 0
                && pub.authPolicy != null
                && pub.authPolicy.AsSpan().SequenceEqual(LowRangeAuthPolicy);
        }

        private static EkTemplate ClassifyLowRangeEk(TpmPublic pub, bool firmwareLimited, bool decrypt, bool sign) {
            // L-1/L-2 are restricted storage (decrypt) keys and never firmware-limited.
            if (firmwareLimited || !decrypt || sign) {
                return EkTemplate.Unspecified;
            }
            if (pub.parameters is RsaParms { keyBits: 2048 }) {
                return EkTemplate.EkL1;
            }
            if (pub.parameters is EccParms { curveID: EccCurve.TpmEccNistP256 }) {
                return EkTemplate.EkL2;
            }
            return EkTemplate.Unspecified;
        }

        public static EkTemplate ClassifyEk(TpmPublic pub) {
            if (pub == null) {
                return EkTemplate.Unspecified;
            }
            bool firmwareLimited = ((uint)pub.objectAttributes & FirmwareLimitedBit) != 0;
            bool decrypt = (pub.objectAttributes & ObjectAttr.Decrypt) != 0;
            bool sign = (pub.objectAttributes & ObjectAttr.Sign) != 0;
            // Storage EKs decrypt, signing EKs sign; anything else is not a TCG EK.
            if (decrypt == sign) {
                return EkTemplate.Unspecified;
            }
            if (IsLowRange(pub)) {
                return ClassifyLowRangeEk(pub, firmwareLimited, decrypt, sign);
            }
            bool signing = sign;

            if (pub.parameters is RsaParms rsa) {
                // The profile defines no firmware-limited RSA EKs.
                if (firmwareLimited) {
                    return EkTemplate.Unspecified;
                }
                if (!signing) {
                    return rsa.keyBits switch {
                        2048 => EkTemplate.EkH1,
                        3072 => EkTemplate.EkH6,
                        4096 => EkTemplate.EkH7,
                        _ => EkTemplate.Unspecified
                    };
                }
                bool pss = rsa.scheme is SchemeRsapss;
                if (!pss && rsa.scheme is not SchemeRsassa) {
                    return EkTemplate.Unspecified;
                }
                return (pss, rsa.keyBits) switch {
                    (false, 2048) => EkTemplate.EkH8,
                    (true, 2048) => EkTemplate.EkH9,
                    (false, 3072) => EkTemplate.EkH14,
                    (true, 3072) => EkTemplate.EkH15,
                    (false, 4096) => EkTemplate.EkH16,
                    (true, 4096) => EkTemplate.EkH17,
                    _ => EkTemplate.Unspecified
                };
            }
            if (pub.parameters is EccParms ecc) {
                return (signing, firmwareLimited, ecc.curveID) switch {
                    (false, false, EccCurve.TpmEccNistP256) => EkTemplate.EkH2,
                    (false, false, EccCurve.TpmEccNistP384) => EkTemplate.EkH3,
                    (false, false, EccCurve.TpmEccNistP521) => EkTemplate.EkH4,
                    (false, false, EccCurve.TpmEccSm2P256) => EkTemplate.EkH5,
                    (false, true, EccCurve.TpmEccNistP256) => EkTemplate.EkH18,
                    (false, true, EccCurve.TpmEccNistP384) => EkTemplate.EkH19,
                    (false, true, EccCurve.TpmEccNistP521) => EkTemplate.EkH20,
                    (false, true, EccCurve.TpmEccSm2P256) => EkTemplate.EkH21,
                    (true, false, EccCurve.TpmEccNistP256) => EkTemplate.EkH10,
                    (true, false, EccCurve.TpmEccNistP384) => EkTemplate.EkH11,
                    (true, false, EccCurve.TpmEccNistP521) => EkTemplate.EkH12,
                    (true, false, EccCurve.TpmEccSm2P256) => EkTemplate.EkH13,
                    (true, true, EccCurve.TpmEccNistP256) => EkTemplate.EkH22,
                    (true, true, EccCurve.TpmEccNistP384) => EkTemplate.EkH23,
                    (true, true, EccCurve.TpmEccNistP521) => EkTemplate.EkH24,
                    (true, true, EccCurve.TpmEccSm2P256) => EkTemplate.EkH25,
                    _ => EkTemplate.Unspecified
                };
            }
            
            return EkTemplate.Unspecified;
        }

        public static AsymmetricKeyTemplate ClassifyAsymmetric(TpmPublic pub) {
            if (pub == null) {
                return AsymmetricKeyTemplate.Unspecified;
            }
            bool restricted = (pub.objectAttributes & ObjectAttr.Restricted) != 0;

            if (pub.parameters is RsaParms rsa) {
                return (restricted, rsa.keyBits) switch {
                    (true, 2048) => AsymmetricKeyTemplate.Rsa2048Quote,
                    (true, 3072) => AsymmetricKeyTemplate.Rsa3072Quote,
                    (false, 2048) => AsymmetricKeyTemplate.Rsa2048Sign,
                    (false, 3072) => AsymmetricKeyTemplate.Rsa3072Sign,
                    _ => AsymmetricKeyTemplate.Unspecified,
                };
            }
            if (pub.parameters is EccParms ecc) {
                return (restricted, ecc.curveID) switch {
                    (true, EccCurve.TpmEccNistP256) => AsymmetricKeyTemplate.EccP256Quote,
                    (true, EccCurve.TpmEccNistP384) => AsymmetricKeyTemplate.EccP384Quote,
                    (true, EccCurve.TpmEccNistP521) => AsymmetricKeyTemplate.EccP521Quote,
                    (false, EccCurve.TpmEccNistP256) => AsymmetricKeyTemplate.EccP256Sign,
                    (false, EccCurve.TpmEccNistP384) => AsymmetricKeyTemplate.EccP384Sign,
                    (false, EccCurve.TpmEccNistP521) => AsymmetricKeyTemplate.EccP521Sign,
                    _ => AsymmetricKeyTemplate.Unspecified,
                };
            }
            return AsymmetricKeyTemplate.Unspecified;
        }

        /**
         * Helper to derive the key_id.
         */
        public static byte[] DeriveKeyId(KeyRole role, ProvisioningOrigin origin, byte[]? publicArea) {
            byte[] input = new byte[2 + (publicArea?.Length ?? 0)];
            input[0] = (byte)role;
            input[1] = (byte)origin;
            publicArea?.CopyTo(input, 2);
            return SHA256.HashData(input);
        }

        public static readonly IReadOnlyList<EkTemplate> SupportedEkTemplates = [EkTemplate.EkL1, EkTemplate.EkL2];

        public static uint? GetConventionalEkHandle(EkTemplate template) {
            return template switch {
                EkTemplate.EkL1 => CommandTpm.DefaultL1EkHandle,
                EkTemplate.EkL2 => CommandTpm.DefaultL2EkHandle,
                _ => null
            };
        }

        public static KeyCandidate BuildEkCandidate(byte[] publicArea, TpmPublic pub, uint? persistentHandle,
            byte[]? certificate = null) {
            KeyCandidate candidate = new();
            candidate.Role.Add(KeyRole.Endorsement);
            EkTemplate template = ClassifyEk(pub);
            candidate.KeyId = ByteString.CopyFrom(DeriveKeyId(KeyRole.Endorsement,
                ProvisioningOrigin.Manufacturer, publicArea));
            candidate.Tpm = new TpmKeyMaterial {
                PublicArea = ByteString.CopyFrom(publicArea),
                EkTemplate = template,
            };
            if (persistentHandle is uint handle) {
                candidate.Tpm.PersistentHandle = handle;
            }
            if (certificate is not null and not []) {
                candidate.Tpm.Certificate = ByteString.CopyFrom(certificate);
            }
            if (GetEkCertificateNvIndex(template) is uint nvIndex) {
                candidate.Tpm.CertificateNvIndex = nvIndex;
            }
            return candidate;
        }

        public static KeyCandidate BuildAsymmetricCandidate(byte[] publicArea, TpmPublic pub, uint persistentHandle,
            KeyRole role, ProvisioningOrigin origin) {
            KeyCandidate candidate = new();
            candidate.Role.Add(role);
            AsymmetricKeyTemplate template = ClassifyAsymmetric(pub);
            candidate.KeyId = ByteString.CopyFrom(DeriveKeyId(role, origin, publicArea));
            candidate.Tpm = new TpmKeyMaterial {
                PublicArea = ByteString.CopyFrom(publicArea),
                AsymmetricTemplate = template,
                ProvisioningOrigin = origin,
                PersistentHandle = persistentHandle,
            };
            return candidate;
        }
    }
}
