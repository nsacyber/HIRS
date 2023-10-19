<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- CONTENT --%>
<my:page>
    <jsp:attribute name="pageHeaderTitle">Help</jsp:attribute>

    <jsp:body>
        <h3 class="content-subhead" id="alerttype">Documentation</h3>

       <!--
        <ul>
            <c:forEach items="${docs}" var="doc">
                <li><a href="${baseURL}/docs/${doc.name}">${doc.name}</a></li>
            </c:forEach>
        </ul>
        -->

        <p>
           For more documentation on the project, you may visit the wiki section of our <a href="https://github.com/nsacyber/HIRS/wiki">code repository</a>.
        </p>
    </jsp:body>
</my:page>