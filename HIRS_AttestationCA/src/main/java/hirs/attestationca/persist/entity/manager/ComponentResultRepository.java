package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComponentResultRepository extends JpaRepository<ComponentResult, UUID> {

    /**
     * Query based on the device serial number.
     *
     * @param boardSerialNumber variable holding the device serial number
     * @return a list of component result.
     */
    List<ComponentResult> findByBoardSerialNumber(String boardSerialNumber);

    /**
     * Query based on the device serial number.
     *
     * @param boardSerialNumber variable holding the device serial number
     * @param delta             flag indicating if the component is associated with a delta certificate
     * @return a list of component result.
     */
    List<ComponentResult> findByBoardSerialNumberAndDelta(String boardSerialNumber, boolean delta);

    /**
     * Query based on certificate serial number and device serial number.
     *
     * @param certificateSerialNumber certificate specific serial number
     * @param boardSerialNumber       variable holding the device serial number
     * @return a list of component result.
     */
    List<ComponentResult> findByCertificateSerialNumberAndBoardSerialNumber(
            String certificateSerialNumber, String boardSerialNumber);
}
