<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ attribute name="testName" type="java.lang.String" description="Test Name" %>
<%@ attribute name="devApplications" type="java.util.Set" description="dev Applications" %>
<%@ attribute name="qaApplications" type="java.util.Set" description="qa Applications" %>
<%@ attribute name="productionApplications" type="java.util.Set" description="production Applications" %>
<%@ attribute name="devDynamicClients" type="java.util.Set" description="dev Applications resolving the test dynamically" %>
<%@ attribute name="qaDynamicClients" type="java.util.Set" description="qa Applications resolving the test dynamically" %>
<%@ attribute name="productionDynamicClients" type="java.util.Set" description="production Applications resolving the test dynamiclly" %>
<ui:grid-row>
    <ui:grid-columns width="twelve">
        <h6>DEV</h6>
        <ul class="nice">
            <c:forEach items="${devApplications}" var="application">
                <li>
                    <span class="label">${fn:escapeXml(application)}</span>
                    <a class="ui-icon" href="/proctor/specification?branch=${proctor:urlencode("trunk")}&version=${application.version}&app=${proctor:urlencode(application.app)}">sp</a>
                    <c:if test="${devDynamicClients.contains(application)}">
                        <span class="label round">Dynamic</span>
                    </c:if>
                </li>
            </c:forEach>
        </ul>
    </ui:grid-columns>
</ui:grid-row>
<ui:grid-row>
    <ui:grid-columns width="twelve">
        <h6>QA</h6>
        <ul class="nice">
            <c:forEach items="${qaApplications}" var="application">
                <li>
                    <span class="label">${fn:escapeXml(application)}</span>
                    <a class="ui-icon" href="/proctor/specification?branch=${proctor:urlencode("qa")}&version=${application.version}&app=${proctor:urlencode(application.app)}">sp</a>
                    <c:if test="${qaDynamicClients.contains(application)}">
                        <span class="label round">Dynamic</span>
                    </c:if>
                </li>
            </c:forEach>
        </ul>
    </ui:grid-columns>
</ui:grid-row>
<ui:grid-row>
    <ui:grid-columns width="twelve">
        <h6>PRODUCTION</h6>
        <ul class="nice">
            <c:forEach items="${productionApplications}" var="application">
                <li>
                    <span class="label">${fn:escapeXml(application)}</span>
                    <a class="ui-icon" href="/proctor/specification?branch=${proctor:urlencode("production")}&version=${application.version}&app=${proctor:urlencode(application.app)}">sp</a>
                    <c:if test="${productionDynamicClients.contains(application)}">
                        <span class="label round">Dynamic</span>
                    </c:if>
                </li>
            </c:forEach>
        </ul>
    </ui:grid-columns>
</ui:grid-row>
