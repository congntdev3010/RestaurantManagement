<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đánh giá | Việt À la carte</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reviews.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/fragments/header.jsp"/>
<main class="reviews-shell">
    <header class="reviews-hero">
        <p class="reviews-eyebrow">GÓC CẢM NHẬN</p>
        <h1>Lời khách gửi</h1>
        <p>Những chia sẻ chân thành từ trải nghiệm tại Việt À la carte.</p>
        <c:if test="${reviewCount gt 0}">
            <div class="reviews-summary" aria-label="Điểm đánh giá trung bình">
                <span class="summary-stars" aria-hidden="true">★★★★★</span>
                <strong><fmt:formatNumber value="${averageRating}" minFractionDigits="1" maxFractionDigits="1"/></strong>
                <span>từ <c:out value="${reviewCount}"/> đánh giá gần nhất</span>
            </div>
        </c:if>
    </header>

    <section class="reviews-list" aria-label="Đánh giá của khách hàng">
        <c:choose>
            <c:when test="${empty reviews}">
                <div class="reviews-empty">
                    <span aria-hidden="true">✦</span>
                    <h2>Mở đầu một câu chuyện đẹp</h2>
                    <p>Những cảm nhận đầu tiên sẽ xuất hiện tại đây.</p>
                </div>
            </c:when>
            <c:otherwise>
                <c:forEach var="review" items="${reviews}">
                    <article class="review-card">
                        <div class="review-card-top">
                            <span class="review-stars" role="img" aria-label="${review.rating} trên 5 sao">
                                <c:forEach begin="1" end="${review.rating}">★</c:forEach><c:forEach begin="${review.rating + 1}" end="5">☆</c:forEach>
                            </span>
                            <time><c:out value="${review.createdAtText}"/></time>
                        </div>
                        <blockquote><c:out value="${review.comment}"/></blockquote>
                        <div class="review-author">
                            <span class="author-mark" aria-hidden="true">V</span>
                            <span><strong>Khách hàng</strong><small>Trải nghiệm tại Việt À la carte</small></span>
                        </div>
                    </article>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </section>
</main>
</body>
</html>
