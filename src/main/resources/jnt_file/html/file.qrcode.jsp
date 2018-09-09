<%@ page import="org.jahia.utils.FileUtils" %>
<%@ page import="org.jahia.services.content.JCRNodeWrapper" %>
<%@ page import="net.glxn.qrgen.javase.QRCode" %>
<%@ page import="net.glxn.qrgen.core.image.ImageType" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="propertyDefinition" type="org.jahia.services.content.nodetypes.ExtendedPropertyDefinition"--%>
<%--@elvariable id="type" type="org.jahia.services.content.nodetypes.ExtendedNodeType"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<template:addResources type="css" resources="files.css"/>

<jcr:nodeProperty node="${currentNode}" name="jcr:created" var="created"/>
<jcr:nodeProperty node="${currentNode}" name="jcr:title" var="title"/>
<fmt:formatDate value="${created.time}" dateStyle="full" var="displayDate"/>
<c:choose>
    <c:when test="${fn:startsWith(currentNode.fileContent.contentType,'image/')}">
        <img src="${currentNode.url}"
             alt="${fn:escapeXml(currentNode.name)}"
             width="250px"
             height="250px"
        />
    </c:when>
    <c:otherwise>
        <span class="icon <%=FileUtils.getFileIcon( ((JCRNodeWrapper)pageContext.findAttribute("currentNode")).getName()) %>"></span>
        <a href="${currentNode.url}"
           title="${fn:escapeXml(not empty title.string ? title.string : currentNode.name)}">
                ${fn:escapeXml(not empty refTitle ? refTitle : not empty title.string ? title.string : currentNode.name)}
        </a>
    </c:otherwise>
</c:choose>

<img src="${url.server}${currentNode.url}?t=qrcode"
     alt="${fn:escapeXml(currentNode.name)}"/>

<!--
<img src="${url.server}${url.base}${node.path}.qrcode.html?t=qrcode"
     alt="${fn:escapeXml(currentNode.name)}"/>
-->