package hirs.persist;

import org.hibernate.Criteria;

/**
 * Allows a user of the DBManager to modify the criteria object before processing.
 */
public interface CriteriaModifier {
    /**
     * Allows a client to modify the criteria object by reference.
     * @param criteria The hibernate criteria builder object
     */
    void modify(Criteria criteria);
}
