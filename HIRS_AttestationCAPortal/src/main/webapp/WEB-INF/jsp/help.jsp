<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

<%-- CONTENT --%>

<my:page>
  <jsp:attribute name="pageHeaderTitle">Help</jsp:attribute>

  <jsp:attribute name="script">
    <script
      type="text/javascript"
      src="${lib}/jquery.spring-friendly/jquery.spring-friendly.js"
    ></script>
  </jsp:attribute>

  <jsp:body>
    <div class="download-header">
      <h3>Download HIRS Attestation Log File</h3>
      <a href="${portal}/help/hirs-log/download">
        <img src="${icons}/ic_file_download_black_24dp.png" title="Download HIRS Log">
      </a>
    </div>
    <div class="aca-data-table">
      <h2>Logger Management</h2>
      <table id="loggersTable" class="display" style="width: 100%">
        <thead>
          <tr>
            <th>Logger Name</th>
            <th>Configured Level</th>
            <th>Reset</th>
          </tr>
        </thead>
      </table>
    </div>

    <!-- <form:form method="POST" action="${portal}/help/hirs-log/setLogLevel">
      <label for="logLevelSelector">Select Logging Level:</label>
      <select name="logLevel" id="logLevelSelector">
        <option value="ERROR">ERROR</option>
        <option value="WARN">WARN</option>
        <option value="INFO">INFO</option>
        <option value="DEBUG">DEBUG</option>
        <option value="TRACE">TRACE</option>
      </select>
      <input type="submit" value="Submit" />
    </form:form> -->
    <div>
      <h3 class="content-subhead" id="alerttype">Documentation</h3>
      <p>
        For more documentation on the project, you may visit the wiki section of
        our <a href="https://github.com/nsacyber/HIRS/wiki">code repository</a>.
      </p>
    </div>
  </jsp:body>
</my:page>

<script>
  $(document).ready(function () {
    const levels = ["FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL"];

    let url = pagePath + "/hirs-log/loggers-list";
    let columns = [
      {
        name: "loggerName",
        data: "loggerName",
        orderable: true,
        searchable: true,
      },
      {
        name: "logLevel",
        data: "logLevel",
        orderable: true,
        searchable: true,
      },
      {
        name: "reset",
        data: "reset",
        orderable: false,
        searchable: false,
      },
    ];

    //Set data tables
    let dataTable = setDataTables("#loggersTable", url, columns);
  });
</script>
