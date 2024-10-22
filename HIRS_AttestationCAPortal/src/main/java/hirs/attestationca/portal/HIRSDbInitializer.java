package hirs.attestationca.portal;

import hirs.attestationca.persist.PersistenceConfiguration;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

@Log4j2
@WebListener
public class HIRSDbInitializer extends AbstractAnnotationConfigDispatcherServletInitializer
        implements ServletContextListener {

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.getEnvironment().addActiveProfile("Server");


//        applicationContext.register(PersistenceConfiguration.class);
        try {
            applicationContext.refresh();

        } catch (NoSuchBeanDefinitionException nsbdEx) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Unable to locate MultipartResolver with name 'multipartResolver': no multipart request handling provided");
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[] {
                PersistenceJPAConfig.class, PageConfiguration.class, PersistenceConfiguration.class
        };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return null;
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] {
                "/"
        };
    }

}
