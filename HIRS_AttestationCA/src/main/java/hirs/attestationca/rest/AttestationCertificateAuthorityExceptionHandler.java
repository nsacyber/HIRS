package hirs.attestationca.rest;

import hirs.attestationca.AcaRestError;
import hirs.attestationca.exceptions.CertificateProcessingException;
import hirs.attestationca.exceptions.IdentityProcessingException;
import hirs.attestationca.exceptions.UnexpectedServerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Handle processing of exceptions for ACA REST API.
 */
@ControllerAdvice
public class AttestationCertificateAuthorityExceptionHandler
        extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LogManager.getLogger(
            AttestationCertificateAuthorityExceptionHandler.class);

    /**
     * Method to handle errors of the type {@link CertificateProcessingException},
     * {@link IdentityProcessingException}, and {@link IllegalArgumentException}
     * that are thrown when performing a RESTful operation.
     *
     * @param ex exception that was thrown
     * @param request the web request that started the RESTful operation
     * @return the response entity that will form the message returned to the client
     */
    @ExceptionHandler({ CertificateProcessingException.class, IdentityProcessingException.class,
            IllegalArgumentException.class })
    public final ResponseEntity<Object> handleExpectedExceptions(final Exception ex,
                                                              final WebRequest request) {
        LOGGER.error(String.format("The ACA has encountered an expected exception: %s",
                ex.getMessage()), ex);
        return handleGeneralException(ex, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Method to handle errors of the type {@link IllegalStateException} and
     * {@link UnexpectedServerException} that are thrown when performing a RESTful operation.
     *
     * @param ex exception that was thrown
     * @param request the web request that started the RESTful operation
     * @return the response entity that will form the message returned to the client
     */
    @ExceptionHandler({ IllegalStateException.class, UnexpectedServerException.class })
    public final ResponseEntity<Object> handleUnexpectedExceptions(final Exception ex,
                                                                final WebRequest request) {
        LOGGER.error(String.format("The ACA has encountered an unexpected exception: %s",
                ex.getMessage()), ex);
        return handleGeneralException(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<Object> handleGeneralException(final Exception ex,
                                                          final HttpStatus responseStatus,
                                                          final WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return handleExceptionInternal(ex, new AcaRestError(ex.getMessage()),
                headers, responseStatus, request);
    }

}
