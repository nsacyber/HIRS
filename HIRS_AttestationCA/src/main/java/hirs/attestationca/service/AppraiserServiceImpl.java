package hirs.attestationca.service;

import hirs.appraiser.Appraiser;
import hirs.attestationca.repository.AppraiserRepository;
import hirs.persist.AppraiserManagerException;
import hirs.persist.DBManagerException;
import hirs.persist.service.AppraiserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.UUID;

/**
 * A <code>AppraiserServiceImpl</code> manages <code>Appraiser</code>s. A
 * <code>AppraiserServiceImpl</code> is used to store and manage certificates. It has
 * support for the basic create, read, update, and delete methods.
 */
@Service
public class AppraiserServiceImpl extends DbServiceImpl<Appraiser>
        implements AppraiserService {
    private static final Logger LOGGER = LogManager.getLogger();
    @Autowired
    private AppraiserRepository appraiserRepository;

    /**
     * Default constructor.
     * @param em entity manager for jpa hibernate events
     */
    public AppraiserServiceImpl(final EntityManager em) {
    }

    @Override
    public Appraiser saveAppraiser(final Appraiser appraiser) throws AppraiserManagerException {
        LOGGER.debug("saving appraiser: {}", appraiser);

        return getRetryTemplate().execute(new RetryCallback<Appraiser,
                DBManagerException>() {
            @Override
            public Appraiser doWithRetry(final RetryContext context)
                    throws DBManagerException {
                return appraiserRepository.save(appraiser);
            }
        });
    }

    @Override
    public void updateAppraiser(final Appraiser appraiser) throws AppraiserManagerException {
        LOGGER.debug("updating appraiser: {}", appraiser);
        Appraiser dBAppraiser;

        if (appraiser.getId() == null) {
            LOGGER.debug("Appraiser not found: {}", appraiser);
            dBAppraiser = appraiser;
        } else {
            // will not return null, throws and exception
            dBAppraiser = appraiserRepository.getReferenceById(
                    UUID.fromString(appraiser.getId().toString()));

            // run through things that aren't equal and update

            if (!dBAppraiser.getName().equals(appraiser.getName())) {
                dBAppraiser.setName(appraiser.getName());
            }

        }

        saveAppraiser(dBAppraiser);
    }

    @Override
    public Appraiser getAppraiser(final String name) throws AppraiserManagerException {
        LOGGER.debug("retrieve appraiser: {}", name);

        return getRetryTemplate().execute(new RetryCallback<Appraiser,
                DBManagerException>() {
            @Override
            public Appraiser doWithRetry(final RetryContext context)
                    throws DBManagerException {
                return appraiserRepository.findByName(name);
            }
        });
    }

    @Override
    public final void deleteAppraiser(final Appraiser appraiser)
            throws AppraiserManagerException {
        LOGGER.debug("Deleting appraiser by name: {}", appraiser.getName());

        getRetryTemplate().execute(new RetryCallback<Void, DBManagerException>() {
            @Override
            public Void doWithRetry(final RetryContext context)
                    throws DBManagerException {
                appraiserRepository.delete(appraiser);
                appraiserRepository.flush();
                return null;
            }
        });
    }
}
