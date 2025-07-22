<%@tag description="page messages" pageEncoding="UTF-8"%>

<%@taglib prefix="c" uri="jakarta.tags.core" %>

<c:if test="${not empty messages}">

    <div id="page-messages-container">
        <ul id="page-messages" class="noPaddingOrMargin">
            <c:forEach var="error" items="${messages.errorMessages}">
                <li id="page-errorMessage" class="page-message">
                    <span class="page-messageIcon">
                        <img src="${icons}/ic_priority_high_white_24dp.png"/>
                    </span>
                    ${error}
                </li>
            </c:forEach>

            <c:forEach var="success" items="${messages.successMessages}">
                <li id="page-successMessage" class="page-message">
                    <span class="page-messageIcon">
                        <img src="${icons}/ic_done_white_24dp.png"/>
                    </span>
                    ${success}
                </li>
            </c:forEach>

            <c:forEach var="info" items="${messages.infoMessages}">
                <li id="page-infoMessage" class="page-message">
                    <span class="page-messageIcon">
                        <img src="${icons}/ic_priority_high_white_24dp.png"/>
                    </span>
                    ${info}
                </li>
            </c:forEach>
        </ul>
    </div>
</c:if>