package hirs.attestationca.persist;

import jakarta.persistence.criteria.CriteriaQuery;

/**
 * Allows a user of the DBManager to modify the criteria object before processing.
 */
public interface CriteriaModifier<T> {
    /**
     * Allows a client to modify the criteria object by reference.
     * @param criteria The hibernate criteria builder object
     */
    void modify(CriteriaQuery<T> criteria);
}
