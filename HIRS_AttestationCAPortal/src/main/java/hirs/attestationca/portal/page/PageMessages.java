package hirs.attestationca.portal.page;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates error, success, and informational messages to display on a page.
 */
@Getter
public class PageMessages {

    private final List<String> errorMessages = new ArrayList<>();
    private final List<String> successMessages = new ArrayList<>();
    private final List<String> infoMessages = new ArrayList<>();

    /**
     * Adds an error message.
     *
     * @param error the error message to add
     */
    public void addErrorMessage(final String error) {
        this.errorMessages.add(error);
    }

    /**
     * Adds multiple error messages.
     *
     * @param multipleErrors list of error messages
     */
    public void addErrorMessages(final List<String> multipleErrors) {
        this.errorMessages.addAll(multipleErrors);
    }

    /**
     * Adds a success message.
     *
     * @param success the success message to add
     */
    public void addSuccessMessage(final String success) {
        this.successMessages.add(success);
    }

    /**
     * Adds multiple success messages.
     *
     * @param multipleSuccessMessages list of success messages to add
     */
    public void addSuccessMessages(final List<String> multipleSuccessMessages) {
        this.successMessages.addAll(multipleSuccessMessages);
    }

    /**
     * Adds an informational message.
     *
     * @param info the informational message to add
     */
    public void addInfoMessage(final String info) {
        this.infoMessages.add(info);
    }

    /**
     * Adds multiple informational messages.
     *
     * @param multipleInfoMessages list of informational messages to add
     */
    public void addInfoMessages(final List<String> multipleInfoMessages) {
        this.errorMessages.addAll(multipleInfoMessages);
    }
}
