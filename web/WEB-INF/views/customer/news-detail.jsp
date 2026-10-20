<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${news.title}"/> - Tin tức</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/news.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/fragments/header.jsp" />
<main class="news-page news-detail-page">
    <a class="news-back" href="${pageContext.request.contextPath}/news"><i class="fa-solid fa-arrow-left" aria-hidden="true"></i> Tất cả tin tức</a>
    <article class="news-detail">
        <p class="news-eyebrow">TIN TỨC</p>
        <h1><c:out value="${news.title}"/></h1>
        <p class="news-detail-meta"><time datetime="${news.publishedAt}"><c:out value="${news.publishedAt}"/></time><span>•</span><c:out value="${news.createdByName}"/></p>
        <c:choose><c:when test="${not empty news.imageUrl}"><img class="news-detail-image" src="<c:out value='${news.imageUrl}'/>" alt=""></c:when><c:otherwise><div class="news-detail-placeholder" aria-label="Tin tức không có ảnh"><i class="fa-regular fa-newspaper" aria-hidden="true"></i><span>Không có ảnh</span></div></c:otherwise></c:choose>
        <p class="news-detail-summary"><c:out value="${news.summary}"/></p>
        <div class="news-content"><c:out value="${news.content}"/></div>
    </article>
</main>
</body></html>
