package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReferenceDigestValueRepository extends JpaRepository<ReferenceDigestValue, UUID> {

    @Query(value = "SELECT * FROM ReferenceDigestValue", nativeQuery = true)
    List<ReferenceDigestValue> listAll();
    List<ReferenceDigestValue> findByModel(String model);
    List<ReferenceDigestValue> findByManufacturer(String manufacturer);
    @Query(value = "SELECT * FROM ReferenceDigestValue WHERE baseRimId = '?1' OR supportRimId = '?1'", nativeQuery = true)
    List<ReferenceDigestValue> getValuesByRimId(UUID associatedRimId);
    @Query(value = "SELECT * FROM ReferenceDigestValue WHERE supportRimId = '?1'", nativeQuery = true)
    List<ReferenceDigestValue> findBySupportRimId(UUID supportRimId);
    List<ReferenceDigestValue> findBySupportRimHash(String supportRimHash);
}
