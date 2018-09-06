<%@tag description="page navigation components" pageEncoding="UTF-8"%>

<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<c:if test="${page.hasMenu}">
    <nav class="navbar navbar-default sidebar" role="navigation">
        <div class="container-fluid">
        <div class="navbar-header">
          <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#sidebar-navbar">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
        </div>
        <div class="collapse navbar-collapse" id="sidebar-navbar">
          <ul class="nav navbar-nav">
            <c:forEach var="p" items="${pages}">
                <c:if test="${p.inMenu}">
                    <li class="${p.menuLinkClass}${p.title == page.title ? " active" : ""}">
                        <a href="${portal}/${p.prefixPath}${p.viewName}">
                            ${p.title}
                            <span class="pull-right hidden-xs showopacity"><img src="${icons}/${p.icon}_white_24dp.png"/></span>
                        </a>
                    </li>
                </c:if>
            </c:forEach>
          </ul>
        </div>
      </div>
    </nav>
</c:if>