package hirs.persist;

import hirs.data.persist.certificate.ComponentResult;

import java.util.Set;
import java.util.UUID;

/**
 * This class facilitates the persistence of {@link hirs.data.persist.certificate.ComponentResult}s
 * including storage, retrieval, and deletion.
 */
public interface ComponentResultManager extends OrderedListQuerier<ComponentResult> {
    /**
     * Persists a new Component Identifier Result.
     *
     * @param componentResult the ComponentResult
     * @return the persisted ComponentResult
     */
    ComponentResult saveResult(ComponentResult componentResult);

    /**
     * Persists a new Component Identifier Result.
     *
     * @param componentResult the ComponentResult
     * @return the persisted ComponentResult
     */
    ComponentResult getResult(ComponentResult componentResult);

    /**
     * Persists a new Component Identifier Result.
     *
     * @param componentId the component id
     * @return the persisted ComponentResult
     */
    ComponentResult getResultById(UUID componentId);

    /**
     * Returns a list of all <code>ComponentResult</code>s that are ordered by a column
     * and direction (ASC, DESC) that is provided by the user.  This method
     * helps support the server-side processing in the JQuery DataTables.
     *
     * @return FilteredRecordsList object with fields for DataTables
     */
    Set<ComponentResult> getComponentResultList();

    /**
     * Returns a list of all <code>ComponentResult</code>s that are
     * associated with the certificate
     *
     * @return FilteredRecordsList object with fields for DataTables
     */
    Set<ComponentResult> getComponentResultsByCertificate(UUID certificateId);

    /**
     * Delete the given value.
     *
     * @param componentResult the component result delete
     * @return true if the deletion succeeded, false otherwise.
     */
    boolean deleteResult(ComponentResult componentResult);
}
