<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tin tức - Việt À la carte</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/news.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/fragments/header.jsp" />
<main class="news-page">
    <header class="news-heading"><p class="news-eyebrow">VIỆT À LA CARTE</p><h1>Tin tức</h1><p>Câu chuyện và cập nhật mới nhất từ nhà hàng.</p></header>
    <form class="news-search" action="${pageContext.request.contextPath}/news" method="get">
        <label class="visually-hidden" for="news-keyword">Tìm tin tức</label>
        <input id="news-keyword" type="search" name="keyword" value="<c:out value='${keyword}'/>" placeholder="Tìm theo tiêu đề">
        <button type="submit"><i class="fa-solid fa-magnifying-glass" aria-hidden="true"></i> Tìm kiếm</button>
    </form>
    <c:choose>
        <c:when test="${empty newsList}"><p class="news-empty">Chưa có tin tức phù hợp.</p></c:when>
        <c:otherwise>
            <section class="news-grid" aria-label="Danh sách tin tức">
                <c:forEach var="item" items="${newsList}">
                    <article class="news-card">
                        <a class="news-card-image" href="${pageContext.request.contextPath}/news?id=${item.newsId}">
                            <c:choose><c:when test="${not empty item.imageUrl}"><img src="<c:out value='${item.imageUrl}'/>" alt=""></c:when><c:otherwise><span class="news-image-placeholder"><i class="fa-regular fa-newspaper" aria-hidden="true"></i></span></c:otherwise></c:choose>
                        </a>
                        <div class="news-card-body">
                            <time datetime="${item.publishedAt}"><c:out value="${item.publishedAt}"/></time>
                            <h2><a href="${pageContext.request.contextPath}/news?id=${item.newsId}"><c:out value="${item.title}"/></a></h2>
                            <p><c:out value="${item.summary}"/></p>
                            <a class="news-read-more" href="${pageContext.request.contextPath}/news?id=${item.newsId}">Đọc tiếp <i class="fa-solid fa-arrow-right" aria-hidden="true"></i></a>
                        </div>
                    </article>
                </c:forEach>
            </section>
            <c:if test="${totalPages gt 1}"><nav class="news-pagination" aria-label="Phân trang">
                <c:forEach begin="1" end="${totalPages}" var="p"><c:url var="pageUrl" value="/news"><c:param name="page" value="${p}"/><c:if test="${not empty keyword}"><c:param name="keyword" value="${keyword}"/></c:if></c:url>
                    <a href="${pageUrl}" class="${p eq currentPage ? 'active' : ''}" aria-current="${p eq currentPage ? 'page' : 'false'}"><c:out value="${p}"/></a>
                </c:forEach>
            </nav></c:if>
        </c:otherwise>
    </c:choose>
</main>
</body></html>
