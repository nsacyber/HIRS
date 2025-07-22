<%-- Set variables for the banner if it have one. --%>
<%@tag description="page navigation components" pageEncoding="UTF-8"%>

<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- Default values --%>
<c:set var="topBanner" />
<c:set var="bottomBanner" />
<c:set var="bottomBannerInfo" />

<c:if test="${banner.hasBanner}">

    <link type="text/css" rel="stylesheet" href="${common}/banner.css"/>
    <script>
        $(document).ready(function(){
            let headerHeight = $('#header').height();
            let topBannerHeight = $(".topBanner").height();

            //Add margins
            $('body').css({'padding-top': headerHeight + topBannerHeight });
            $('#header').css({'margin-top': topBannerHeight });

            // wait 50ms before setting the new height
            window.setTimeout( setHeights, 50);

            function setHeights(){
                //Set bannerColor
                let bannerColor = "${banner.bannerColor}";
                $('.bannerColor').css({'background-color': bannerColor});

                //get basic heights
                let mainHeight = $('.main, .main-without-navigation').height();
                let contentHeight = $('.content').height();
                let spacerHeight = $('.spacer').height() + $('.extra-spacer').height();
                let bottomBannerHeight = $(".bottomBanner").height() + $(".bottomBannerInfo").height();
                let pageHeaderHeight = $('.page-header').height() +
                        parseInt($('.page-header').css('margin-top').replace('px', '')) +
                        parseInt($('.page-header').css('margin-bottom').replace('px', '')) +
                        parseInt($('.page-header').css('padding-top').replace('px', '')) +
                        parseInt($('.page-header').css('padding-bottom').replace('px', '')) +
                        parseInt($('.page-header').css('border-bottom-width').replace('px', '')) + 1;

                //Current total
                let totalHeight = pageHeaderHeight + contentHeight + spacerHeight + bottomBannerHeight;

                //Check if there is still space
                if(mainHeight > totalHeight) {
                    $('.content').height(mainHeight - totalHeight + contentHeight);
                }
            }

        });
    </script>

    <c:set var="bannerTemplate">
        <c:if test="${not empty banner.bannerDynamic}">
            <div class="classBanner dynamic">${banner.bannerDynamic}</div>
        </c:if>
        <div class="classBanner bannerColor">
            ${banner.bannerString}
        </div>
    </c:set>

    <c:set var="topBanner" scope="session">
        <div class="topBanner">${bannerTemplate}</div>
    </c:set>
    <c:set var="bottomBanner" scope="session">
        <div class="bottomBanner">${bannerTemplate}</div>
    </c:set>
    <c:set var="bottomBannerInfo" scope="session">
        <div class="bottomBannerInfo">
            <div class="row">
                <div class="col-md-3 col-sm-4 col-xs-5 alignLeft">
                    <c:forEach var="data" items="${banner.leftContent}">
                         <span>${data}</span>
                    </c:forEach>
                </div>
                <div class="col-md-3 col-sm-4 col-xs-5 col-md-offset-6 col-sm-offset-4 col-xs-offset-2 alignRight">
                    <c:forEach var="data" items="${banner.rightContent}">
                         <span>${data}</span>
                    </c:forEach>
                </div>
            </div>
        </div>
    </c:set>

</c:if>