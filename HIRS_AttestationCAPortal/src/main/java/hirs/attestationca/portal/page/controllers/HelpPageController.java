package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;

import static hirs.attestationca.portal.page.Page.HELP;

/**
 * Controller for the Help page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/help")
public class HelpPageController extends PageController<NoPageParams> {

    @Autowired
    private ApplicationContext applicationContext;

    private static final String PATH = "/docs";

    /**
     * Constructor providing the Page's display and routing specification.
     */
    public HelpPageController() {
        super(HELP);
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model The data model for the request. Can contain data from redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        ModelAndView mav = getBaseModelAndView();

        try {
            File[] documents = new File(
                    applicationContext.getResource(PATH).getFile().getPath()
            ).listFiles();
            mav.addObject("docs", documents);
        } catch (IOException ex) {
            log.error("Could not get files from resource.");
        }

        return mav;
    }

}
