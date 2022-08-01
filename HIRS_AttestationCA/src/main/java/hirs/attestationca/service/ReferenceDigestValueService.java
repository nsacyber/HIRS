package hirs.attestationca.service;

import hirs.data.persist.ReferenceDigestValue;

import java.util.UUID;

/**
 * A <code>ReferenceDigestValue</code> manages <code>ReferenceDigestValue</code>s. A
 * <code>ReferenceDigestValue</code> is used to store and manage digest events. It has
 * support for the basic create, read, update, and delete methods.
 */
public interface ReferenceDigestValueService {

    /**
     * Saves the <code>ReferenceDigestValue</code> in the database. This creates a new
     * database session and saves the device.
     *
     * @param digestValue Certificate to save
     * @return reference to saved reference digest value
     */
    ReferenceDigestValue saveDigestValue(ReferenceDigestValue digestValue);

    /**
     * Updates a <code>ReferenceDigestValue</code>. This updates the database entries to
     * reflect the new values that should be set.
     *
     * @param digestValue Certificate object to save
     * @param uuid UUID for the database object
     * @return a ReferenceDigestValue object
     */
    ReferenceDigestValue updateDigestValue(ReferenceDigestValue digestValue, UUID uuid);
}
