<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- CONTENT --%>
<my:page>

    <jsp:attribute name="script">
        <script type="text/javascript" src="${lib}/jquery.spring-friendly/jquery.spring-friendly.js"></script>
    </jsp:attribute>
    <jsp:attribute name="pageHeaderTitle">Platform Certificates</jsp:attribute>

    <jsp:body>
        <!-- text and icon resource variables -->
        <c:set var="endorsementIcon" value="${icons}/ic_vpn_key_black_24dp.png"/>
        <div class="aca-input-box-header">
            <form:form method="POST"  action="${portal}/certificate-request/platform-credentials/upload" enctype="multipart/form-data">
                Platform Credentials
                <my:file-chooser id="platformCredentialEditor" label="Import Platform Credentials">
                    <input id="importFile" type="file" name="file" multiple="multiple" />
                </my:file-chooser>
                <a href="${portal}/certificate-request/platform-credentials/bulk">
                    <img src="${icons}/ic_file_download_black_24dp.png" title="Download Certificates">
                </a>
            </form:form>
        </div>
        <br/>
        <div class="aca-data-table">
            <table id="platformTable" class="display" width="100%">
                <thead>
                    <tr>
                        <th>Device</th>
                        <th>Issuer</th>
                        <th>Type</th>
                        <th>Manufacturer</th>
                        <th>Model</th>
                        <th>Version</th>
                        <th>Board SN</th>
                        <th>Valid (begin)</th>
                        <th>Valid (end)</th>
                        <th>Endorsement</th>
                        <th>Options</th>
                    </tr>
                </thead>
            </table>
        </div>

        <script>
            $(document).ready(function() {
                var url = pagePath +'/list';
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
                            data: 'credentialType',
                            render: function (data, type, full, meta) {
                                if (full.platformType !== '') {
                                    return full.platformType;
                                } else {
                                    return full.credentialType;
                                }
                            }
                        },
                        {data: 'manufacturer'},
                        {data: 'model'},
                        {data: 'version'},
                        {data: 'platformSerial'},
                        {
                            data: 'beginValidity',
                            searchable:false,
                            render: function (data, type, full, meta) {
                                return formatDateTime(full.beginValidity);
                            }
                        },
                        {
                            data: 'endValidity',
                            searchable:false,
                            render: function (data, type, full, meta) {
                                return formatDateTime(full.endValidity);
                            }
                        },
                        {
                            data: 'id',
                            orderable: false,
                            searchable:false,
                            render: function (data, type, full, meta) {
                                //Display endorsement credential
                                if(full.endorsementCredential === null) return '';
                                var html = '';

                                var id = full.endorsementCredential.id;
                                html = certificateDetailsLink('endorsement', id, false) +'&nbsp;';

                                return html;
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
                                html += certificateDetailsLink('platform', full.id, true);
                                html += certificateDownloadLink(full.id, pagePath);
                                html += certificateDeleteLink(full.id, pagePath);

                                return html;
                            }
                        }
                    ];

                //Set data tables
                setDataTables("#platformTable", url, columns);
            });
        </script>
    </jsp:body>
</my:page>
