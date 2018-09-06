package hirs.provisioner.client;

/**
 * Defines the responsibilities and behavior of a service that provisions clients. That is, to
 * take ownership of the machine's TPM, create an identity credential, attest said credential with a
 * certificate authority, and activate that credential with the client's TPM.
 */
public interface ClientProvisioner {

    /**
     * Takes control of the machine's TPM. Then creates an identity credential using the TPM. That
     * credential is then attested by a certificate authority. If that passes, the provisioner
     * will activate the credential with the TPM and persist the credential on the client for
     * future TPM operations. This method may throw {@link ProvisioningException}s when encountering
     * errors dealing with the TPM and/certificate authority.
     *
     * @return true if provisioning was successful, false otherwise
     */
    boolean provision();
}
