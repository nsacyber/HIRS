package hirs.attestationca.portal.page.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;


@Controller("error")
public class ErrorController {

    /**
     * Handles exceptions based on the provided request and exception.
     *
     * @param request http servlet request.
     * @param ex      exception.
     * @return model and view
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(final HttpServletRequest request, final Exception ex) {
        ModelAndView modelAndView = new ModelAndView();

        modelAndView.addObject("exception", ex.getLocalizedMessage());
        modelAndView.addObject("url", request.getRequestURL());

        modelAndView.setViewName("error");

        return modelAndView;
    }
}
