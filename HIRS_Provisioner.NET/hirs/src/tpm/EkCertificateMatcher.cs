using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using Serilog;
using Tpm2Lib;

namespace hirs {
    /**
     * Checks that an EK certificate contains an EK public
     */
    public static class EkCertificateMatcher {
        public static bool Matches(byte[]? certificate, TpmPublic? ekPublic) {
            if (certificate is null or [] || ekPublic == null) {
                return false;
            }
            try {
                using X509Certificate2 cert = X509CertificateLoader.LoadCertificate(certificate);
                return ekPublic.unique switch {
                    Tpm2bPublicKeyRsa rsaUnique when ekPublic.parameters is RsaParms rsaParms =>
                        MatchesRsa(cert, rsaUnique.buffer, rsaParms.exponent),
                    EccPoint point => MatchesEcc(cert, point),
                    _ => false
                };
            } catch (CryptographicException e) {
                Log.Debug(e, "EK certificate could not be parsed.");
                return false;
            }
        }

        private static bool MatchesRsa(X509Certificate2 cert, byte[] modulus, uint tpmExponent) {
            using RSA? certKey = cert.GetRSAPublicKey();
            if (certKey == null) {
                return false;
            }
            RSAParameters p = certKey.ExportParameters(false);
            // A TPM exponent of 0 means the default, 2^16 + 1.
            uint exponent = tpmExponent == 0 ? 65537u : tpmExponent;
            return TrimLeadingZeros(p.Modulus!).AsSpan().SequenceEqual(TrimLeadingZeros(modulus))
                && TrimLeadingZeros(p.Exponent!).AsSpan().SequenceEqual(TrimLeadingZeros(ToBigEndian(exponent)));
        }

        private static bool MatchesEcc(X509Certificate2 cert, EccPoint point) {
            // For an EC key EncodedKeyValue is the uncompressed point: 0x04 || X || Y.
            byte[] encoded = cert.PublicKey.EncodedKeyValue.RawData;
            if (encoded.Length < 3 || encoded[0] != 0x04 || (encoded.Length - 1) % 2 != 0) {
                return false;
            }
            int size = (encoded.Length - 1) / 2;
            return encoded.AsSpan(1, size).SequenceEqual(LeftPad(point.x, size))
                && encoded.AsSpan(1 + size, size).SequenceEqual(LeftPad(point.y, size));
        }

        private static byte[] ToBigEndian(uint value) {
            return [(byte)(value >> 24), (byte)(value >> 16), (byte)(value >> 8), (byte)value];
        }

        private static byte[] TrimLeadingZeros(byte[] value) {
            int i = 0;
            while (i < value.Length - 1 && value[i] == 0) {
                i++;
            }
            return value[i..];
        }

        private static byte[] LeftPad(byte[] value, int size) {
            byte[] trimmed = TrimLeadingZeros(value);
            if (trimmed.Length > size) {
                return trimmed;
            }
            byte[] padded = new byte[size];
            trimmed.CopyTo(padded, size - trimmed.Length);
            return padded;
        }
    }
}
