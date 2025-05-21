<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

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
    <form:form method="POST" action="${portal}/hirs-log/setLogLevel">
        <label for="logLevelSelector">Select Logging Level:</label>
        <select name="logLevel" id="logLevelSelector">
            <option value="ERROR">ERROR</option>
            <option value="WARN">WARN</option>
            <option value="INFO">INFO</option>
            <option value="DEBUG">DEBUG</option>
            <option value="TRACE">TRACE</option>
        </select>
        <input type="submit" value = "Submit" />
    </form:form>
    <div>
      <h3 class="content-subhead" id="alerttype">Documentation</h3>
      <p>
        For more documentation on the project, you may visit the wiki section of
        our <a href="https://github.com/nsacyber/HIRS/wiki">code repository</a>.
      </p>
    </div>

  </jsp:body>
</my:page>
