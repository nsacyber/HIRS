package hirs.attestationca.portal.page;

import hirs.attestationca.portal.enums.Page;
import hirs.attestationca.portal.utils.BannerConfiguration;
import lombok.AllArgsConstructor;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract class to provide common functionality for page Controllers.
 *
 * @param <P> PageParams class used by the subclass.
 */
@AllArgsConstructor
public abstract class PageController<P extends PageParams> {

    private static final Logger LOGGER = LogManager.getLogger(PageController.class);

    /**
     * Model attribute name used by initPage for the initial data passed to the page.
     */
    public static final String INITIAL_DATA = "initialData";

    /**
     * Reserved attribute used by page.tag to identify a page's general
     * information.
     */
    public static final String PAGE_ATTRIBUTE = "page";

    /**
     * Reserved attribute used by page.tag to identify the page collection used
     * for navigation.
     */
    public static final String PAGES_ATTRIBUTE = "pages";

    /**
     * Reserved attribute used by page.tag to identify the banner information.
     */
    public static final String BANNER_ATTRIBUTE = "banner";

    /**
     * Reserved attribute used by page.tag to identify the messages the page
     * should display.
     */
    public static final String MESSAGES_ATTRIBUTE = "messages";

    private final Page page;

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model The data model for the request. Can contain data from
     * redirect.
     * @return the path for the view and data model for the page.
     */
    @RequestMapping
    public abstract ModelAndView initPage(@ModelAttribute P params, Model model);

    /**
     * Creates a generic ModelAndView containing this page's configuration and
     * the list of other pages for navigational purposes.
     *
     * @return A generic ModelAndView containing basic information for the page.
     */
    protected final ModelAndView getBaseModelAndView() {
        return getBaseModelAndView(page);
    }

    /**
     * Creates a generic ModelAndView containing the specify page
     * configuration and the list of other pages for navigational
     * purposes.
     *
     * @param newPage new page to get the model and view
     * @return A generic ModelAndView containing basic information for the page.
     */
    protected final ModelAndView getBaseModelAndView(final Page newPage) {
        ModelMap modelMap = new ExtendedModelMap();

        // add page information
        modelMap.addAttribute(PAGE_ATTRIBUTE, newPage);

        // add other pages for navigation
        modelMap.addAttribute(PAGES_ATTRIBUTE, Page.values());

        // add banner information
        try {
            BannerConfiguration banner = new BannerConfiguration();
            modelMap.addAttribute(BANNER_ATTRIBUTE, banner);
        } catch (IOException ex) {
            modelMap.addAttribute(BANNER_ATTRIBUTE, null);
        }

        return new ModelAndView(newPage.getViewName(), modelMap);
    }

    /**
     * Redirects back to this controller's page with the specified data.
     *
     * @param params The url parameters to pass to the page.
     * @param model The model data to pass to the page.
     * @param attr The request's RedirectAttributes to hold the model data.
     * @return RedirectView back to the page with the specified parameters.
     * @throws java.net.URISyntaxException if malformed URI
     */
    protected final RedirectView redirectToSelf(
            final P params,
            final Map<String, ?> model,
            final RedirectAttributes attr) throws URISyntaxException {

        return redirectTo(page, params, model, attr);
    }

    /**
     * Redirects controller's page with the specified data.
     *
     * @param newPage new page to get the model and view
     * @param params The url parameters to pass to the page.
     * @param model The model data to pass to the page.
     * @param attr The request's RedirectAttributes to hold the model data.
     * @return RedirectView back to the page with the specified parameters.
     * @throws java.net.URISyntaxException if malformed URI
     */
    protected final RedirectView redirectTo(
            final Page newPage,
            final P params,
            final Map<String, ?> model,
            final RedirectAttributes attr) throws URISyntaxException {

        String defaultUri = "../" + newPage.getViewName();
        // create uri with specified parameters
        URIBuilder uri = new URIBuilder("../" + newPage.getViewName());
        LOGGER.error(uri.toString());

        if (params != null) {
            for (Map.Entry<String, ?> e : params.asMap().entrySet()) {
                Object v = Optional.ofNullable(e.getValue()).orElse("");
                uri.addParameter(e.getKey(), v.toString());
            }
        }

        // create view
        RedirectView redirect = new RedirectView(defaultUri);

        // do not put model attributes in the url
        redirect.setExposeModelAttributes(false);

        // add model data to forward to redirected page
        if (model != null) {
            for (Map.Entry<String, ?> e : model.entrySet()) {
                attr.addFlashAttribute(e.getKey(), e.getValue());
            }
        }

        return redirect;
    }
}
