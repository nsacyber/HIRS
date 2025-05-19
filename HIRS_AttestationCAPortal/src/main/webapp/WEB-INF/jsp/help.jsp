<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- CONTENT --%>

<my:page>
  <jsp:attribute name="pageHeaderTitle">Help</jsp:attribute>

  <jsp:body>
    <div class="download-header">
      <h3>Download HIRS Attestation Log File</h3>
      <a href="${portal}/hirs-log/download">
        <img src="${icons}/ic_file_download_black_24dp.png" title="Download HIRS Log">
      </a>
    </div>
    <!-- todo-->
    <div class="filter-section">
        <label for="log-level">Log Level: </label>
        <select id="log-level">
          <option value="ALL">All</option>
          <option value="INFO">INFO</option>
          <option value="DEBUG">DEBUG</option>
          <option value="ERROR">ERROR</option>
          <option value="WARN">WARN</option>
        </select>
    </div>
    <div>
      <h3 class="content-subhead" id="alerttype">Documentation</h3>
      <p>
        For more documentation on the project, you may visit the wiki section of
        our <a href="https://github.com/nsacyber/HIRS/wiki">code repository</a>.
      </p>
    </div>

  </jsp:body>
</my:page>
