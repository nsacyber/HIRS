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
    <jsp:attribute name="pageHeaderTitle">Issued Certificates</jsp:attribute>
    <jsp:body>
        <div class="aca-input-box-header">
            Issued Credentials
            <a href="${portal}/certificate-request/issued-certificates/bulk">
                <img src="${icons}/ic_file_download_black_24dp.png" title="Download All Issued Certificates">
            </a>
        </div>
        <br />
        <div class="aca-data-table">
            <table id="issuedTable" class="display" width="100%">
                <thead>
                    <tr>
                        <th rowspan="2">Hostname</th>
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
            $(document).ready(function() {
                var url = pagePath + '/list';
                var columns = [
                        {
                            data: 'device.name',
                            render: function (data, type, full, meta) {
                                // if there's a device, display its name, otherwise
                                // display nothing
                                if (full.device) {
                                    // TODO render a link to a device details page,
                                    // passing the device.id
                                    return full.device.name;
                                }
                                return '';
                            }
                        },
                        {data: 'issuer'},
                        {
                            data: 'beginValidity',
                            searchable:false,
                            render: function (data, type, full, meta) {
                                return formatDateTime(data);
                            }
                        },
                        {
                            data: 'endValidity',
                            searchable:false,
                            render: function (data, type, full, meta) {
                                return formatDateTime(data);
                            }
                        },
                        {
                            data: 'id',
                            orderable: false,
                            searchable:false,
                            render: function (data, type, full, meta) {
                                //Display endorsement credential
                                var html = '';
                                if (full.endorsementCredential !== undefined
                                        && full.endorsementCredential !== null){
                                    var id = full.endorsementCredential.id;
                                    html += certificateDetailsLink('endorsement', id, false) +'&nbsp;';
                                }
                                return html;
                            }
                        },
                        {
                            data: 'id',
                            orderable: false,
                            searchable:false,
                            render: function (data, type, full, meta) {
                                //Display platform credential
                                var html = '';
                                if (full.platformCredentials !== undefined
                                        && full.platformCredentials !== null) {
                                    var size = full.platformCredentials.length;

                                    for(var i = 0; i < size; i++) {
                                        var id = full.platformCredentials[i].id;
                                        html += certificateDetailsLink('platform', id, false) +'&nbsp;';
                                    }
                                }

                                return html;
                            }
                        },
                        {
                            data: 'id',
                            orderable: false,
                            searchable:false,
                            render: function(data, type, full, meta) {
                                // set up link to details page
                                var html = '';
                                html += certificateDetailsLink('issued', full.id, true);
                                html += certificateDownloadLink(full.id, pagePath);
                                html += certificateDeleteLink(full.id, pagePath);

                                return html;
                            }
                        }
                    ];

                //Set data tables
                setDataTables("#issuedTable", url, columns);
            });
        </script>
    </jsp:body>

</my:page>