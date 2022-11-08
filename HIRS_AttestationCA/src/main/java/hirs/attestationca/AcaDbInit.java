package hirs.attestationca;

import hirs.appraiser.SupplyChainAppraiser;
import hirs.data.persist.policy.SupplyChainPolicy;
import hirs.persist.service.AppraiserService;
import hirs.persist.service.PolicyService;

import static hirs.attestationca.AbstractAttestationCertificateAuthority.LOG;

/**
 * Utility class that simply holds logic to seed the ACA's database with its
 * default entries.
 */
public final class AcaDbInit {
    // prevent construction
    private AcaDbInit() { }

    /**
     * Insert the ACA's default entries into the DB.  This class is invoked after successful
     * install of the HIRS_AttestationCA RPM.
     *
     * @param appraiserService the AppraiserService to use to persist appraisers
     * @param policyService the PolicyService to use to persist policies
     */
    public static synchronized void insertDefaultEntries(
            final AppraiserService appraiserService,
            final PolicyService policyService) {
        LOG.info("Ensuring default ACA database entries are present.");

        // If the SupplyChainAppraiser exists, do not attempt to re-save the supply chain appraiser
        // or SupplyChainPolicy
        SupplyChainAppraiser supplyChainAppraiser = (SupplyChainAppraiser)
                appraiserService.getAppraiser(SupplyChainAppraiser.NAME);
        if (supplyChainAppraiser != null) {
            LOG.info("Supply chain appraiser is present; not inserting any more entries.");
            LOG.info("ACA database initialization complete.");
            return;
        }

        // Create the SupplyChainAppraiser
        LOG.info("Saving supply chain appraiser...");
        supplyChainAppraiser = (SupplyChainAppraiser)
                appraiserService.saveAppraiser(new SupplyChainAppraiser());

        // Create the SupplyChainPolicy
        LOG.info("Saving default supply chain policy...");
        SupplyChainPolicy supplyChainPolicy = new SupplyChainPolicy(
                SupplyChainPolicy.DEFAULT_POLICY);
        policyService.savePolicy(supplyChainPolicy);
        policyService.setDefaultPolicy(supplyChainAppraiser, supplyChainPolicy);
        policyService.setPolicy(supplyChainAppraiser, supplyChainPolicy);

        LOG.info("ACA database initialization complete.");
    }
}
