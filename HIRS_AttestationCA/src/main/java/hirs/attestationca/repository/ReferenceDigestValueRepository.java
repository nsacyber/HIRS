package hirs.attestationca.repository;

import hirs.data.persist.ReferenceDigestValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Setting up for new creation for CRUD operations.
 */
@Repository
public interface ReferenceDigestValueRepository extends JpaRepository<ReferenceDigestValue, UUID> {

    /**
     * Persists a new Reference Digest value.
     *
     * @param uuid associated with the base rim .
     * @return the persisted list of ReferenceDigestValue
     */
    List<ReferenceDigestValue> findValuesByBaseRimId(UUID uuid);

    /**
     * Persists a new Reference Digest value.
     *
     * @param uuid associated with the support rim.
     * @return the persisted list of ReferenceDigestValue
     */
    List<ReferenceDigestValue> findValuesBySupportRimId(UUID uuid);

    /**
     * List<String> results = session.createCriteria(User.class).add(Projections.projectionList()
     * .add(Projections.property("id")).add()....).list();
     *
     * List<Object[]> result = session.createCriteria(User.class).setProjection(Projections
     * .projectionList().add(Projections.groupProperty("lastName"))
     * .add(Projections.rowCount())).list();
     */
}
