package hirs.attestationca.portal.page.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class LombokLoggingController {

    /**
     * REST endpoint that logs various issues regarding Lombok.
     *
     * @return a message that indicates there are logs regarding Lombok that should be looked at
     */
    @RequestMapping("/HIRS_AttestationCAPortal/portal/lombok")
    public String index() {
        log.trace("A TRACE Message");
        log.debug("A DEBUG Message");
        log.info("An INFO Message");
        log.warn("A WARN Message");
        log.error("An ERROR Message");

        return "Howdy! Check out the Logs to see the output...";
    }
}
