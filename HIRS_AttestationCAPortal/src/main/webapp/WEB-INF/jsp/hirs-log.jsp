<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- CONTENT --%>

<my:page>
  <jsp:attribute name="pageHeaderTitle">
    HIRS Attestation Log Viewer</jsp:attribute
  >
  <jsp:body>
    <div class="aca-input-box-header">
     HIRS Attestation Log File
      <a href="${portal}/hirs-log/download">
        <img src="${icons}/ic_file_download_black_24dp.png" title="Download HIRS Log">
      </a>
    </div>
    <div class="filter-section">
      <label for="log-level">Log Level: </label>
      <select id="log-level">
        <option value="ALL">All</option>
        <option value="INFO">INFO</option>
        <option value="DEBUG">DEBUG</option>
        <option value="ERROR">ERROR</option>
        <option value="WARN">WARN</option>
      </select>
      <!-- <input type="text" id="log-search" placeholder="Search logs..."> TODO -->
    </div>
    <div id="log-container" class="log-container">
      <!-- Logs will be dynamically appended here TODO-->
    </div>
  </jsp:body>
</my:page>
