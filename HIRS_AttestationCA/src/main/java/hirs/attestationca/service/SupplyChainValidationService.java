package hirs.attestationca.service;

import java.util.Set;
import hirs.data.persist.Device;
import hirs.data.persist.SupplyChainValidationSummary;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.PlatformCredential;


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
     * @param summary the associated device summary
     * @return True if validation is successful, false otherwise.
     */
    SupplyChainValidationSummary validateQuote(Device device,
                                               SupplyChainValidationSummary summary);
}
