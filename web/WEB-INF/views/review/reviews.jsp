<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html><html lang="vi"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Đánh giá | Việt À la carte</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reviews.css?v=1">
</head><body class="review-page"><jsp:include page="/WEB-INF/views/fragments/header.jsp" />
<main class="review-shell">
    <header class="review-hero"><p class="review-kicker">VIỆT À LA CARTE · LỜI THƯƠNG GỬI LẠI</p><h1>Thực khách nói gì?</h1>
        <p>Những chia sẻ chân thành sau mỗi bữa ăn tại nhà hàng.</p>
        <div class="review-score"><strong><fmt:formatNumber value="${averageRating}" minFractionDigits="1" maxFractionDigits="1"/></strong><span class="stars" aria-label="Điểm đánh giá trung bình"><c:forEach begin="1" end="5" var="i">★</c:forEach></span><small><c:out value="${reviewCount}"/> đánh giá đã xác nhận</small></div>
    </header>
    <section class="review-list" aria-label="Đánh giá đã duyệt">
        <c:choose><c:when test="${empty reviews}"><div class="review-empty">Nhà hàng đang chờ những lời nhận xét đầu tiên từ bạn.</div></c:when>
            <c:otherwise><c:forEach var="review" items="${reviews}"><article class="review-card">
                <div class="review-card-top"><div><strong><c:out value="${review.customerName}"/></strong><small><c:out value="${review.createdAtText}"/> · Bàn <c:out value="${review.tableName}"/></small></div><span class="stars" aria-label="${review.rating} trên 5 sao"><c:forEach begin="1" end="${review.rating}" var="i">★</c:forEach><c:forEach begin="1" end="${5-review.rating}" var="i">☆</c:forEach></span></div>
                <p><c:out value="${review.reviewText}"/></p>
            </article></c:forEach></c:otherwise></c:choose>
    </section>
</main></body></html>
