package hirs.attestationca;

import hirs.appraiser.SupplyChainAppraiser;
import hirs.data.persist.policy.SupplyChainPolicy;
import hirs.persist.AppraiserManager;
import hirs.persist.PolicyManager;

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
     * @param appraiserManager the AppraiserManager to use to persist appraisers
     * @param policyManager the PolicyManager to use to persist policies
     */
    public static synchronized void insertDefaultEntries(
            final AppraiserManager appraiserManager,
            final PolicyManager policyManager) {
        LOG.info("Ensuring default ACA database entries are present.");

        // If the SupplyChainAppraiser exists, do not attempt to re-save the supply chain appraiser
        // or SupplyChainPolicy
        SupplyChainAppraiser supplyChainAppraiser = (SupplyChainAppraiser)
                appraiserManager.getAppraiser(SupplyChainAppraiser.NAME);
        if (supplyChainAppraiser != null) {
            LOG.info("Supply chain appraiser is present; not inserting any more entries.");
            LOG.info("ACA database initialization complete.");
            return;
        }

        // Create the SupplyChainAppraiser
        LOG.info("Saving supply chain appraiser...");
        supplyChainAppraiser = (SupplyChainAppraiser)
                appraiserManager.saveAppraiser(new SupplyChainAppraiser());

        // Create the SupplyChainPolicy
        LOG.info("Saving default supply chain policy...");
        SupplyChainPolicy supplyChainPolicy = new SupplyChainPolicy(
                SupplyChainPolicy.DEFAULT_POLICY);
        policyManager.savePolicy(supplyChainPolicy);
        policyManager.setDefaultPolicy(supplyChainAppraiser, supplyChainPolicy);
        policyManager.setPolicy(supplyChainAppraiser, supplyChainPolicy);

        LOG.info("ACA database initialization complete.");
    }
}
