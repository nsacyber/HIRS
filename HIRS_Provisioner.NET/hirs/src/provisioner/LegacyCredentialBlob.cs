namespace hirs {
    /**
     * Attempts to provide compatibility to legacy data format.
     * In this format, credentialBlob was concatenated with the encryptedSecret and conveyed as a single sequence.
     * It was only compatible with RSA.
     */
    public static class LegacyCredentialBlob {
        private const int SecretSizeOffset = 134;
        private const int SecretOffset = 136;

        public static bool TrySplit(byte[] blob, out byte[] integrityHmac, out byte[] encIdentity, out byte[] encryptedSecret,
            out string error) {
            integrityHmac = encIdentity = encryptedSecret = [];
            if (blob.Length < SecretOffset) {
                error = "The ACA returned a credential blob that is too short to process.";
                return false;
            }

            int credentialSize = blob[0] | (blob[1] << 8);
            int integrityHmacSize = (blob[2] << 8) | blob[3];
            if (credentialSize < integrityHmacSize + 2 || credentialSize + 2 > blob.Length) {
                error = "The ACA returned an invalid credential blob length.";
                return false;
            }

            int encryptedSecretSize = blob[SecretSizeOffset] | (blob[SecretSizeOffset + 1] << 8);
            if (SecretOffset + encryptedSecretSize > blob.Length) {
                error = "The ACA returned a credential blob with an invalid encrypted-secret length.";
                return false;
            }

            integrityHmac = blob[4..(4 + integrityHmacSize)];
            encIdentity = blob[(4 + integrityHmacSize)..(2 + credentialSize)];
            encryptedSecret = blob[SecretOffset..(SecretOffset + encryptedSecretSize)];
            error = "";
            return true;
        }
    }
}
