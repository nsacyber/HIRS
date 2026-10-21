package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.dto.PageMessages;
import hirs.attestationca.persist.entity.userdefined.DownloadFile;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentEvidenceResult;
import hirs.attestationca.persist.service.ComponentEvidencePageService;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.params.ComponentEvidencePageParams;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

/**
 * Controller for the Component Evidence page: what the ACA verified when a platform component proved possession
 * of its key, how it links to the platform certificate and its issuer, and the files for download.
 */
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/component-evidence")
@Log4j2
public class ComponentEvidencePageController extends PageController<ComponentEvidencePageParams> {
    private final ComponentEvidencePageService componentEvidencePageService;

    /**
     * Constructor.
     *
     * @param componentEvidencePageService component evidence page service
     */
    @Autowired
    public ComponentEvidencePageController(final ComponentEvidencePageService componentEvidencePageService) {
        super(Page.COMPONENT_EVIDENCE);
        this.componentEvidencePageService = componentEvidencePageService;
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    public ModelAndView initPage(final ComponentEvidencePageParams params, final Model model) {
        ModelAndView mav = getBaseModelAndView();
        PageMessages messages = new PageMessages();
        Optional<ComponentEvidenceResult> result = componentEvidencePageService.findResult(params.getId());
        if (result.isEmpty()) {
            messages.addErrorMessage("Unable to find component evidence with ID: " + params.getId());
        }
        mav.addObject(MESSAGES_ATTRIBUTE, messages);
        mav.addObject(INITIAL_DATA, result.map(componentEvidencePageService::describe).orElse(new HashMap<>()));
        return mav;
    }

    /**
     * Downloads the evidence, a certificate, the nonce, or the platform certificate's trust chain.
     *
     * @param id       component evidence result id
     * @param file     evidence, deviceid, attestation, nonce, platform or chain
     * @param response the response
     * @throws IOException when writing to the response
     */
    @GetMapping("/download")
    public void download(@RequestParam final String id, @RequestParam final String file,
                         final HttpServletResponse response) throws IOException {
        Optional<DownloadFile> download = componentEvidencePageService.findResult(id)
                .flatMap(result -> componentEvidencePageService.download(result, file));
        if (download.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + download.get().getFileName());
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.getOutputStream().write(download.get().getFileBytes());
    }
}
