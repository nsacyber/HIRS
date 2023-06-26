<%@tag description="standard page template" pageEncoding="UTF-8"%>
<!DOCTYPE html>

<%-- JSP TAGS --%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- ATTRIBUTES --%>

<c:set var="req" value="${pageContext.request}" scope="session" />
<c:set var="baseURL" value="${req.scheme}://${req.serverName}:${req.serverPort}${req.contextPath}" scope="session" />
<c:set var="images" value="${baseURL}/images" scope="session" />
<c:set var="icons" value="${images}/icons" scope="session" />
<c:set var="common" value="${baseURL}/common" scope="session" />
<c:set var="lib" value="${baseURL}/lib" scope="session" />
<c:set var="portal" value="${baseURL}" scope="session" />
<c:set var="pagePath" value="${portal}/${page.prefixPath}${page.viewName}" scope="session" />
<c:set var="certificateRequest" value="${portal}/certificate-request" scope="session" />

<c:set var="mainClass" value="main-without-navigation"/>
<c:if test="${page.hasMenu}">
    <c:set var="mainClass" value="main"/>
</c:if>

<%@attribute name="script" fragment="true"%>
<%@attribute name="style" fragment="true"%>
<%@attribute name="pageHeaderTitle" required="true"%>

<html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <%-- TITLE/ICONS --%>
        <title>${page.title}</title>
        <link rel="icon" href="${baseURL}/hirs.ico" type="image/x-icon">
        <link rel="shortcut icon" href="${baseURL}/hirs.ico" type="image/x-icon">

        <%-- STYLE --%>
        <link type="text/css" rel="stylesheet" href="${common}/common.css"/>
        <link type="text/css" rel="stylesheet" href="${common}/sidebar.css"/>

        <link type="text/css" rel="stylesheet" href="${lib}/bootstrap-3.3.7/css/bootstrap.min.css" />
        <link type="text/css" rel="stylesheet" href="${lib}/jquery.dataTables-1.10.13/media/css/jquery.dataTables.min.css" />

        <%-- page-specific style --%>
        <jsp:invoke fragment="style"/>

        <%-- SCRIPTS --%>
        <script type="text/javascript" src="${lib}/jquery/jquery-3.6.2.min.js"></script>
        <script type="text/javascript" src="${lib}/jquery.dataTables-1.10.13/media/js/jquery.dataTables.min.js"></script>
        <script type="text/javascript" src="${lib}/bootstrap-3.3.7/js/bootstrap.min.js"></script>
        <script type="text/javascript" src="${lib}/moment.min.js"></script>
        <script type="text/javascript" src="${common}/date.js"></script>
        <script type="text/javascript" src="${common}/common.js"></script>

        <script>
            //Set global variables
            var portal = '${portal}';
            var pagePath = '${pagePath}';
            var icons = '${icons}';
        </script>

        <%-- page-specific script --%>
        <jsp:invoke fragment="script"/>

        <%-- set banner variables --%>
        <my:banner/>
    </head>
    <body>
        <%-- HEADER --%>
        <c:out value="${topBanner}" escapeXml="false"/>
        <nav id="header" class="navbar navbar-fixed-top">
            <%-- logo --%>
            <a class="navbar-brand" href="${portal}/index">
                <img src="${images}/hirs92.png"/>
            </a>
            <%-- Header title --%>
            <div class="navbar-text page-title">
                Attestation Certificate Authority
                <c:if test="${not empty page.subtitle}">
                    <div class="subtitle">${page.subtitle}</div>
                </c:if>
            </div>
        </nav>

        <%-- CONTENT --%>
        <div id="content" class="container-fluid">
            <%-- side navigation --%>
            <my:page-nav/>
            <div class="${mainClass}" role="main">
                <%-- messages --%>
                <my:page-messages/>

                <div class="page-header">
                    <h1>${pageHeaderTitle}</h1>
                </div>
                <div class="content">
                    <%-- page-specific content --%>
                    <jsp:doBody/>
                </div>
                <div class="extra-spacer"></div>
                <c:out value="${bottomBannerInfo}" escapeXml="false"/>
                <div class="spacer"></div>
            </div>
        </div>
        <c:out value="${bottomBanner}" escapeXml="false"/>
    </body>
</html>
