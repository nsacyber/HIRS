<%@tag description="editor icon that opens modal dialog with form" pageEncoding="UTF-8"%>

<%@attribute name="id"%>
<%@attribute name="label"%>
<%@attribute name="customButtons" fragment="true" required="false"%>

<%@ taglib prefix="my" tagdir="/WEB-INF/tags" %>

<a href="#${id}" data-toggle="modal" title="${label}">
    <img src="${icons}/ic_mode_edit_blue_18dp.png"/>
</a>
<my:modal id="${id}" label="${label}" customButtons="${customButtons}">
    <jsp:doBody/>
</my:modal>