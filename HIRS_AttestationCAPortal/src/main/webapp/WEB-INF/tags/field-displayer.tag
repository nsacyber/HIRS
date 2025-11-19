<%@ tag description="Render table row" pageEncoding="UTF-8" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="value" required="true" %>

<tr>
    <th>${label}</th>
    <td>${empty value ? '[Not Present]' : value}</td>
</tr>