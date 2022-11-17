package hirs.attestationca.portal.validation;

import hirs.attestationca.entity.Device;
import hirs.attestationca.entity.SupplyChainValidationSummary;
import hirs.attestationca.entity.certificate.EndorsementCredential;
import hirs.attestationca.entity.certificate.PlatformCredential;
import hirs.data.persist.policy.SupplyChainPolicy;

import java.util.Set;


/**
 * Interface defining a component that will perform supply chain validations, which yields a
 * {@link SupplyChainValidationSummary}.
 */
public interface SupplyChainValidationService {
    /**
     * The "main" method of supply chain validation. Takes the credentials from an identity
     * request and validates the supply chain in accordance to the current supply chain
     * policy.
     *
     * @param ec The endorsement credential from the identity request.
     * @param pc The set of platform credentials from the identity request.
     * @param device The device to be validated.
     * @return True if validation is successful, false otherwise.
     */
    SupplyChainValidationSummary validateSupplyChain(EndorsementCredential ec,
                                                     Set<PlatformCredential> pc,
                                                     Device device);

    /**
     * A supplemental method that handles validating just the quote post main validation.
     *
     * @param device the associated device.
     * @return True if validation is successful, false otherwise.
     */
    SupplyChainValidationSummary validateQuote(Device device);

    /**
     * Allows other service access to the policy information.
     * @return supply chain policy
     */
    SupplyChainPolicy getPolicy();
}
