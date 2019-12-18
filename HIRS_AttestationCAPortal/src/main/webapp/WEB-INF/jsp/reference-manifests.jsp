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
    <jsp:attribute name="pageHeaderTitle">Reference Integrity Manifests</jsp:attribute>

    <jsp:body>
        <!-- text and icon resource variables -->
        <div class="aca-input-box-header">
            <form:form method="POST"  action="${portal}/reference-manifests/upload" enctype="multipart/form-data">
                Import RIMs
                <my:file-chooser id="referenceManifestsEditor" label="Import RIMs">
                    <input id="importFile" type="file" name="file" multiple="multiple" />
                </my:file-chooser>
            </form:form>
        </div>
        <br/>
        <div class="aca-data-table">
            <table id="referenceManifestTable" class="display" width="100%">
                <thead>
                    <tr>
                        <th>Tag ID</th>
                        <th>Type</th>
                        <th>Manufacturer</th>
                        <th>Model</th>
                        <th>Version</th>
                        <th>Options</th>
                    </tr>
                </thead>
            </table>
        </div>

        <script>
            $(document).ready(function() {
                var url = pagePath +'/list';
                var columns = [
                        {data: 'tagId'},
                        {data: 'rimType'},
                        {data: 'manufacturer'},
                        {data: 'model'},
                        {data: 'firmwareVersion'},
                        {
                            data: 'id',
                            orderable: false,
                            searchable:false,
                            render: function(data, type, full, meta) {
                                // Set up a delete icon with link to handleDeleteRequest().
                                // sets up a hidden input field containing the ID which is
                                // used as a parameter to the REST POST call to delete
                                var html = '';
                                html += certificateDetailsLink('referencemanifest', full.id, true);
                                html += certificateDownloadLink(full.id, pagePath);
                                html += certificateDeleteLink(full.id, pagePath);

                                return html;
                            }
                        }
                    ];

                //Set data tables
                setDataTables("#referenceManifestTable", url, columns);
            });
        </script>
    </jsp:body>
</my:page>
