package hirs.attestationca.portal.datatables;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

import java.util.Map;

/**
 * Serializes the DataTableResponse from the view as JSON and writes it to the HTTP response.
 */
public class DataTableView extends AbstractUrlBasedView {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String MODEL_FIELD;

    static {
        final String name = DataTableResponse.class.getSimpleName();
        MODEL_FIELD = name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    /**
     * Serializes the DataTableResponse from the view as JSON and writes it to the HTTP response.
     *
     * @param model    combined output Map (never {@code null}), with dynamic values taking precedence
     *                 over static attributes
     * @param request  current HTTP request
     * @param response current HTTP response
     * @throws Exception if rendering failed
     */
    @Override
    protected void renderMergedOutputModel(
            final Map<String, Object> model,
            final HttpServletRequest request,
            final HttpServletResponse response) throws Exception {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        DataTableResponse dataTable = (DataTableResponse) model.get(MODEL_FIELD);
        ServletOutputStream out = response.getOutputStream();
        String json = GSON.toJson(dataTable);
        out.print(json);
    }
}
