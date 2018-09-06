package hirs.alert.resolve;

import hirs.alert.AlertResolutionAction;
import hirs.alert.AlertResolutionRequest;
import hirs.data.persist.Alert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Creates the appropriate AlertResolver bean for the specified AlertResolutionRequest.
 */
@Component
public class AlertResolverFactory {

    @Autowired
    private ApplicationContext ac;

    /**
     * Returns an anonymous AlertResolver that can be used to record and return error messages in
     * cases where a specific AlertResolver cannot or has not been created.
     *
     * @param error the initial error message to record
     * @return an anonymous AlertResolver
     */
    public final AlertResolver getAnonymous(final String error) {
        AlertResolver resolver = new AlertResolver() {
            @Override
            public boolean resolve(final Alert alert) {
                throw new UnsupportedOperationException(
                        "This class is used only to report errors.");
            }
        };
        return resolver.addError(error);
    }

    /**
     * Creates the appropriate AlertResolver bean for the specified AlertResolutionRequest.
     *
     * @param request the user-specified AlertResolutionRequest
     * @return the appropriate AlertResolver bean
     */
    public final AlertResolver get(final AlertResolutionRequest request) {
        final AlertResolutionAction action = request.getAction();
        if (action == null) {
            return getAnonymous("No action was provided.");
        } else {
            Class<? extends AlertResolver> resolverClass = action.getAlertResolver();
            if (resolverClass == null) {
                return getAnonymous("Action " + action + " does not resolve alerts.");
            } else {
                final AlertResolver resolver = ac.getBean(resolverClass);
                return resolver.init(request);
            }
        }

    }

}
