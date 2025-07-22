package hirs.attestationca.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SuppressWarnings("checkstyle:hideutilityclassconstructor")
@SpringBootApplication
public class HIRSApplication {
    /**
     * This is the starting point of the HIRS application.
     *
     * @param args main method arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(HIRSApplication.class, args);
    }
}
