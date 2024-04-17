<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- CONTENT --%>
<my:page>
    <jsp:attribute name="pageHeaderTitle">Error - 404</jsp:attribute>

    <jsp:body>
        <!--<div> Exception Message: <c:out value="${exception}"/></div>
        <div> from URL -> <span th:text="${url}"</span></div>-->
    </jsp:body>
</my:page>