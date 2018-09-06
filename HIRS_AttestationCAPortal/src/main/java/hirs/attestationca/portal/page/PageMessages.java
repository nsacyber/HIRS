package hirs.attestationca.portal.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates error, success, and informational messages to display on a page.
 */
public class PageMessages {

    private final List<String> error = new ArrayList<>();
    private final List<String> success = new ArrayList<>();
    private final List<String> info = new ArrayList<>();

    /**
     * Returns the list of error messages.
     *
     * @return the list of error messages.
     */
    public List<String> getError() {
        return Collections.unmodifiableList(error);
    }

    /**
     * Adds an error message.
     *
     * @param error the error message to add
     */
    public void addError(final String error) {
        this.error.add(error);
    }

    /**
     * Returns the list of success messages.
     *
     * @return the list of success messages.
     */
    public List<String> getSuccess() {
        return Collections.unmodifiableList(success);
    }

    /**
     * Adds a success message.
     *
     * @param success the success message to add
     */
    public void addSuccess(final String success) {
        this.success.add(success);
    }

    /**
     * Returns the list of informational messages.
     *
     * @return the list of informational messages.
     */
    public List<String> getInfo() {
        return Collections.unmodifiableList(info);
    }

    /**
     * Adds an informational message.
     *
     * @param info the informational message to add
     */
    public void addInfo(final String info) {
        this.info.add(info);
    }

}
