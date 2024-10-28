package hirs.attestationca.portal;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@EnableAutoConfiguration
@Log4j2
public class HIRSApplication {
    //extends SpringBootServletInitializer {
//      private static final Logger LOGGER = LogManager.getLogger(HIRSApplication.class);
//    @Override
//    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
//        return application.sources(HIRSApplication.class);
//    }

//    @Override
//    public void onStartup(ServletContext servletContext) throws ServletException {
//        ServletRegistration.Dynamic appServlet = servletContext.addServlet("mvc", new DispatcherServlet(
//                new GenericWebApplicationContext()));

//        appServlet.setLoadOnStartup(1);
//    }

    /**
     * @param args
     */
    public static void main(final String[] args) {
//        SpringApplication springApplication = new SpringApplication(HIRSApplication.class);
//        springApplication.setDefaultProperties(Collections.singletonMap("server.servlet.context-path",
//        "/portal"));
//        springApplication.run(args);
        SpringApplication.run(HIRSApplication.class, args);
    }
}
