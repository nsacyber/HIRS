package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReferenceDigestValueRepository extends JpaRepository<ReferenceDigestValue, UUID> {

    List<ReferenceDigestValue> findByModel(String model);
    List<ReferenceDigestValue> findByManufacturer(String manufacturer);
    List<ReferenceDigestValue> findValuesByBaseRimId(UUID associatedRimId);
    List<ReferenceDigestValue> findBySupportRimId(UUID supportRimId);
    List<ReferenceDigestValue> findBySupportRimHash(String supportRimHash);
    List<ReferenceDigestValue> findByManufacturerAndModel(String manufacturer, String model);
}
