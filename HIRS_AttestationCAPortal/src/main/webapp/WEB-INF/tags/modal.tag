<%@tag description="modal dialog with form" pageEncoding="UTF-8"%>

<%@attribute name="id"%>
<%@attribute name="label"%>
<%@attribute name="customButtons" fragment="true" required="false"%>

<div id="${id}" class="modal fade" role="dialog" style="top:30%">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h1 id="modal-title">${label}</h1>
            </div>
            <div class="modal-body">
                <jsp:doBody/>
            </div>
            <div class="modal-footer">
                <div class="modal-custom-buttons">
                    <jsp:invoke fragment="customButtons"/>
                </div>
                <button class="btn btn-secondary" data-dismiss="modal">Cancel</button>
                <input class="btn btn-primary" type="submit" value="Save">
            </div>
        </div>
    </div>
</div>