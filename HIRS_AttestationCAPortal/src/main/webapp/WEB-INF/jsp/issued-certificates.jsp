<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- CONTENT --%>
<my:page>
  <jsp:attribute name="script">
    <script
      type="text/javascript"
      src="${lib}/jquery.spring-friendly/jquery.spring-friendly.js"
    ></script>
  </jsp:attribute>
  <jsp:attribute name="pageHeaderTitle">Issued Certificates</jsp:attribute>
  <jsp:body>
    <div class="aca-input-box-header">
      Issued Credentials
      <a href="${portal}/certificate-request/issued-certificates/bulk-download">
         <img src="${icons}/ic_file_download_black_24dp.png" title="Download All Issued Certificates">
      </a>
    </div>
    <br />
    <div class="aca-data-table">
      <table id="issuedTable" class="display" width="100%">
        <thead>
          <tr>
            <th rowspan="2">Hostname</th>
            <th rowspan="2">Type</th>
            <th rowspan="2">Issuer</th>
            <th rowspan="2">Valid (begin)</th>
            <th rowspan="2">Valid (end)</th>
            <th colspan="2">Credentials</th>
            <th rowspan="2">Options</th>
          </tr>
          <tr>
            <th>Endorsement</th>
            <th>Platform</th>
          </tr>
        </thead>
      </table>
    </div>
    <script>
      $(document).ready(function () {
        let url = pagePath + "/list";
        let columns = [
          {
            name: "deviceName",
            data: "deviceName",
            searchable: true,
            orderable: true,
            render: function (data, type, full, meta) {
              // if there's a device, display its name, otherwise
              // display nothing
              return full.deviceName;
            },
          },
          {
            name: "ldevID",
            data: "ldevID",
            searchable: false,
            orderable: false,
            render: function (data, type, full, meta) {
              if (data === true) {
                return "LDevID";
              }
              return "AK";
            },
          },
          {
            name: "issuer",
            data: "issuer",
            searchable: true,
            orderable: true,
          },
          {
            name: "beginValidity",
            data: "beginValidity",
            searchable: false,
            orderable: true,
            render: function (data, type, full, meta) {
              return formatCertificateDate(data);
            },
          },
          {
            name: "endValidity",
            data: "endValidity",
            searchable: false,
            orderable: true,
            render: function (data, type, full, meta) {
              return formatCertificateDate(data);
            },
          },
          {
            data: "id",
            orderable: false,
            searchable: false,
            render: function (data, type, full, meta) {
              //Display endorsement credential
              let html = "";
              if (
                full.endorsementCredential !== undefined &&
                full.endorsementCredential !== null
              ) {
                let id = full.endorsementCredential.id;
                html +=
                  certificateDetailsLink("endorsement", id, false) + "&nbsp;";
              }
              return html;
            },
          },
          {
            data: "id",
            orderable: false,
            searchable: false,
            render: function (data, type, full, meta) {
              //Display platform credential
              let html = "";
              if (
                full.platformCredentials !== undefined &&
                full.platformCredentials !== null
              ) {
                let size = full.platformCredentials.length;

                for (let i = 0; i < size; i++) {
                  let id = full.platformCredentials[i].id;
                  html +=
                    certificateDetailsLink("platform", id, false) + "&nbsp;";
                }
              }

              return html;
            },
          },
          {
            data: "id",
            orderable: false,
            searchable: false,
            render: function (data, type, full, meta) {
              // set up link to details page
              let html = "";
              html += certificateDetailsLink("issued", full.id, true);
              html += certificateDownloadLink(full.id, pagePath);
              html += certificateDeleteLink(full.id, pagePath);

              return html;
            },
          },
        ];

        //Set data tables
        setDataTables("#issuedTable", url, columns);
      });
    </script>
  </jsp:body>
</my:page>
