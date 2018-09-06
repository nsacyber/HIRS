package hirs.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Helper class that holds the ApplicationContext which can be used to get spring beans.
 * By implementing ApplicationContextAware, Spring will wire in the ApplicationContext.
 *
 * After constructing this class with the default constructor, a user can call
 * getApplicationContext().
 *
 */
@Component
public class SpringContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    /**
     * Gets the application context.
     * @return the ApplicationContext
     */
    public ApplicationContext getApplicationContext() {
        return context;
    }

    /**
     * Sets the ApplicationContext. Invoked by spring.
     * @param applicationContext the applicationContext
     * @throws BeansException a bean related exception occurs.
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        context = applicationContext;
    }
}
