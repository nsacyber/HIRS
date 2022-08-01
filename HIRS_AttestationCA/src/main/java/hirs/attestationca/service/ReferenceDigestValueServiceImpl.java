package hirs.attestationca.service;

import hirs.attestationca.repository.ReferenceDigestValueRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A <code>ReferenceDigestValueServiceImpl</code> manages <code>Digest Value Event</code>s. A
 * <code>ReferenceDigestValueServiceImpl</code> is used to store and manage digest events. It has
 * support for the basic create, read, update, and delete methods.
 */
@Service
public class ReferenceDigestValueServiceImpl {

    private static final Logger LOGGER = LogManager.getLogger();
    @Autowired
    private ReferenceDigestValueRepository referenceDigestValueRepository;
}
