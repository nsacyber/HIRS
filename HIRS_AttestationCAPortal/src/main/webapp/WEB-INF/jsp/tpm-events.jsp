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
    <jsp:attribute name="pageHeaderTitle">TPM Events</jsp:attribute>

    <jsp:body>
        <br/>
        <div class="aca-data-table">
            <table id="tpmEventTable" class="display" width="100%">
                <thead>
                    <tr>
                        <th>Manufacturer</th>
                        <th>Model</th>
                        <th>Type</th>
                        <th>Index</th>
                        <th>Digest</th>
                        <th>Details</th>
                        <th>Base RIM</th>
                        <th>Support RIM</th>
                    </tr>
                </thead>
            </table>
        </div>

        <script>
            $(document).ready(function() {
                var url = pagePath +'/list';
                var columns = [
                        {data: 'manufacturer'},
                        {data: 'model'},
                        {data: 'supportRim'},
                        {
                            data: 'id',
                            orderable: false,
                            searchable: false,
                            render: function(data, type, full, meta) {
                                // Set up a delete icon with link to handleDeleteRequest().
                                // sets up a hidden input field containing the ID which is
                                // used as a parameter to the REST POST call to delete
                                var html = '';
                                return html;
                            }
                        }
                    ];

                //Set data tables
                setDataTables("#tpmEventTable", url, columns);
            });
        </script>
    </jsp:body>
</my:page>
