<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- CONTENT --%>
<my:page>

    <jsp:attribute name="script">
        <script type="text/javascript" src="${lib}/jquery.spring-friendly/jquery.spring-friendly.js"></script>
    </jsp:attribute>
    <jsp:attribute name="pageHeaderTitle">IDevID Certificates</jsp:attribute>

    <jsp:body>
        <div class="aca-input-box-header">
            <form:form method="POST" action="${portal}/certificate-request/idevid-certificates/upload" enctype="multipart/form-data">
                Import IDevID Certificates
                <my:file-chooser id="idevid-editor" label="Import IDevID Certificates">
                    <input id="importFile" type="file" name="file" multiple="multiple" />
                </my:file-chooser>
                <a href="${portal}/certificate-request/idevid-certificates/bulk">
                    <img src="${icons}/ic_file_download_black_24dp.png" title="Download All IDevID Certificates">
                </a>
            </form:form>
        </div>
        <br/>
        <div class="aca-data-table">
            <table id="idevidCertificateTable" class="display" width="100%">
                <thead>
                    <tr>
                        <th>Issuer</th>
                        <th>Subject</th>
                        <th>Valid (begin)</th>
                        <th>Valid (end)</th>
                        <th>Options</th>
                    </tr>
                </thead>
            </table>
        </div>
        <script>
            $(document).ready(function() {
                var url = pagePath +'/list';
                var columns = [
                        {data: 'issuer'},
                        {data: 'subject'},
                        {
                            data: 'beginValidity',
                            searchable:false,
                            render: function (data, type, full, meta) {
                                return formatCertificateDate(full.beginValidity);
                            }
                        },
                        {
                            data: 'endValidity',
                            searchable:false,
                            render: function (data, type, full, meta) {
                                return formatCertificateDate(full.endValidity);
                            }
                        },
                        {
                            data: 'id',
                            orderable: false,
                            searchable:false,
                            render: function(data, type, full, meta) {
                                // Set up a delete icon with link to handleDeleteRequest().
                                // sets up a hidden input field containing the ID which is
                                // used as a parameter to the REST POST call to delete
                                var html = '';
                                html += certificateDetailsLink('idevid', full.id, true);
                                html += certificateDownloadLink(full.id, pagePath);
                                html += certificateDeleteLink(full.id, pagePath);
                                return html;
                            }
                        }
                    ];

               //Set data tables
                setDataTables("#idevidCertificateTable", url, columns);
            });
        </script>
    </jsp:body>
</my:page>