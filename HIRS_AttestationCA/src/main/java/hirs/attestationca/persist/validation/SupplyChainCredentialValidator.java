package hirs.attestationca.persist.validation;

import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.security.KeyStore;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Log4j2
@NoArgsConstructor
public class SupplyChainCredentialValidator implements CredentialValidator {

    /**
     * AppraisalStatus message for a valid endorsement credential appraisal.
     */
    public static final String ENDORSEMENT_VALID = "Endorsement credential validated";

    /**
     * AppraisalStatus message for a valid platform credential appraisal.
     */
    public static final String PLATFORM_VALID = "Platform credential validated";

    /**
     * AppraisalStatus message for a valid platform credential attributes appraisal.
     */
    public static final String PLATFORM_ATTRIBUTES_VALID =
            "Platform credential attributes validated";

    /**
     * AppraisalStatus message for a valid firmware appraisal.
     */
    public static final String FIRMWARE_VALID = "Firmware validated";

    private static List<ComponentResult> componentResultList = new LinkedList<>();

    @Override
    public AppraisalStatus validatePlatformCredential(final PlatformCredential pc,
                                                      final KeyStore trustStore,
                                                      final boolean acceptExpired) {
        return null;
    }

    @Override
    public AppraisalStatus validatePlatformCredentialAttributes(final PlatformCredential pc,
                                                                final DeviceInfoReport deviceInfoReport,
                                                                final EndorsementCredential ec) {
        return null;
    }

    @Override
    public AppraisalStatus validateDeltaPlatformCredentialAttributes(final PlatformCredential delta,
                                                                     final DeviceInfoReport deviceInfoReport,
                                                                     final PlatformCredential base,
                                                                     final Map<PlatformCredential, SupplyChainValidation> deltaMapping) {
        return null;
    }

    @Override
    public AppraisalStatus validateEndorsementCredential(final EndorsementCredential ec,
                                                         final KeyStore trustStore,
                                                         final boolean acceptExpired) {
        return null;
    }
}
