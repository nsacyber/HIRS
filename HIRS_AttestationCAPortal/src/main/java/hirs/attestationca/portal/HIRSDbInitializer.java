package hirs.attestationca.portal;

import hirs.attestationca.persist.service.SettingsServiceImpl;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@WebListener
public class HIRSDbInitializer implements ServletContextListener {

    @Autowired
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    @Autowired
    static SettingsServiceImpl settingsService = new SettingsServiceImpl();
}
