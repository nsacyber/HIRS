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
    @Query(value = "SELECT * FROM ReferenceDigestValue WHERE model = ?1", nativeQuery = true)
    List<ReferenceDigestValue> listByModel(String model);
    @Query(value = "SELECT * FROM ReferenceDigestValue WHERE manufacturer = ?1", nativeQuery = true)
    List<ReferenceDigestValue> listByManufacturer(String manufacturer);
    @Query(value = "SELECT * FROM ReferenceDigestValue WHERE baseRimId = '?1' OR supportRimId = '?1'", nativeQuery = true)
    List<ReferenceDigestValue> getValuesByRimId(UUID associatedRimId);
    @Query(value = "SELECT * FROM ReferenceDigestValue WHERE supportRimId = '?1'", nativeQuery = true)
    List<ReferenceDigestValue> getValuesBySupportRimId(UUID supportRimId);
}
