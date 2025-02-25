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
    <jsp:attribute name="pageHeaderTitle">Endorsement Key Credentials</jsp:attribute>

    <jsp:body>
        <div class="aca-input-box-header">
            <form:form method="POST" action="${portal}/certificate-request/endorsement-key-credentials/upload" enctype="multipart/form-data">
                Import Endorsement Key Credentials
                <my:file-chooser id="ek-editor" label="Import Endorsement Key Credentials">
                    <input id="importFile" type="file" name="file" multiple="multiple" />
                </my:file-chooser>
                <a href="${portal}/certificate-request/endorsement-key-credentials/bulk">
                    <img src="${icons}/ic_file_download_black_24dp.png" title="Download All Endorsement Certificates">
                </a>
            </form:form>
        </div>
        <br/>
        <div class="aca-data-table">
            <table id="endorsementKeyTable" class="display" width="100%">
                <thead>
                    <tr>
                        <th>Device</th>
                        <th>Issuer</th>
                        <th>Type</th>
                        <th>Manufacturer</th>
                        <th>Model</th>
                        <th>Version</th>
                        <th>Valid (begin)</th>
                        <th>Valid (end)</th>
                        <th>Options</th>
                    </tr>
                </thead>
            </table>
        </div>
        <script>
            $(document).ready(function() {
                let url = pagePath +'/list';
                let columns = [
                        {
                            data: 'deviceName',
                            render: function (data, type, full, meta) {
                                // if there's a device, display its name, otherwise
                                return full.deviceName;
                            }
                        },
                        {data: 'issuer'},
                        {data: 'credentialType'},
                        {data: 'manufacturer'},
                        {data: 'model'},
                        {data: 'version'},
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
                                let html = '';
                                html += certificateDetailsLink('endorsement', full.id, true);
                                html += certificateDownloadLink(full.id, pagePath);
                                html += certificateDeleteLink(full.id, pagePath);

                                return html;
                            }
                        }
                    ];

               //Set data tables
                setDataTables("#endorsementKeyTable", url, columns);
            });
        </script>
    </jsp:body>
</my:page>