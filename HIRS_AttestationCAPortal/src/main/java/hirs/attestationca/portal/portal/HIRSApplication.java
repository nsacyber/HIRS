package hirs.attestationca.portal.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

import java.util.Collections;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan({"hirs.attestationca.portal", "hirs.attestationca.portal.page.controllers", "hirs.attestationca.persist.entity", "hirs.attestationca.persist.entity.service"})
public class HIRSApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(HIRSApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(HIRSApplication.class);
        springApplication.setDefaultProperties(Collections.singletonMap("server.servlet.context-path", "/portal"));
        springApplication.run(args);
//        SpringApplication.run(HIRSApplication.class, args);
    }
}