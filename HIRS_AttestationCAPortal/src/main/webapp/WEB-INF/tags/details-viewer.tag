<%@tag description="Generic details viewer icon that opens modal-dialogue" pageEncoding="UTF-8"%>

<%@attribute name="id"%>
<%@attribute name="label"%>
<%@attribute name="customButton" fragment="true" required="false"%>

<%@ taglib prefix="my" tagdir="/WEB-INF/tags" %>

<a href="#${id}" data-toggle="modal" title="${label}">
    <img src="${icons}/ic_assignment_black_24dp.png"/>
</a>
<my:modal-dialogue id="${id}" label="${label}" customButton="${customButton}">
    <jsp:doBody/>
</my:modal-dialogue>