<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý đánh giá - Việt À la carte</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reviews.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/fragments/header.jsp"/>
<main class="manage-reviews-page">
    <header class="manage-reviews-heading">
        <div class="manage-reviews-title-group">
            <p class="manage-reviews-breadcrumb"><span>Quản trị</span><i class="fa-solid fa-chevron-right" aria-hidden="true"></i><strong>Đánh giá</strong></p>
            <h1>Quản lý đánh giá</h1>
            <p class="manage-reviews-description">Kiểm duyệt phản hồi và quản lý chất lượng trải nghiệm khách hàng.</p>
        </div>
        <a class="manage-reviews-refresh" href="${pageContext.request.contextPath}/manage-reviews"><i class="fa-solid fa-rotate" aria-hidden="true"></i> Làm mới</a>
    </header>

    <c:if test="${not empty manageReviewMessage}"><p class="review-flash ${manageReviewMessageType}" role="status"><c:out value="${manageReviewMessage}"/></p></c:if>

    <section class="manage-reviews-stats" aria-label="Thống kê đánh giá">
        <article class="manage-reviews-stat"><span class="manage-reviews-stat-icon wine"><i class="fa-regular fa-comments" aria-hidden="true"></i></span><div><span>Tổng đánh giá</span><strong><c:out value="${totalReviewCount}"/></strong></div></article>
        <article class="manage-reviews-stat"><span class="manage-reviews-stat-icon amber"><i class="fa-regular fa-clock" aria-hidden="true"></i></span><div><span>Đang chờ duyệt</span><strong><c:out value="${pendingReviewCount}"/></strong></div></article>
        <article class="manage-reviews-stat"><span class="manage-reviews-stat-icon green"><i class="fa-solid fa-check" aria-hidden="true"></i></span><div><span>Đã duyệt</span><strong><c:out value="${approvedReviewCount}"/></strong></div></article>
        <article class="manage-reviews-stat"><span class="manage-reviews-stat-icon gold"><i class="fa-solid fa-star" aria-hidden="true"></i></span><div><span>Điểm trung bình</span><strong><fmt:formatNumber value="${averageReviewRating}" minFractionDigits="1" maxFractionDigits="1"/><small> / 5</small></strong></div></article>
    </section>

    <section class="manage-reviews-queue" aria-labelledby="manage-reviews-queue-title">
        <header class="manage-reviews-queue-heading">
            <div><p class="manage-reviews-kicker">KIỂM DUYỆT NỘI DUNG</p><h2 id="manage-reviews-queue-title">Chờ duyệt <span><c:out value="${pendingReviewCount}"/></span></h2></div>
            <span class="manage-reviews-queue-caption">Đánh giá mới nhất nằm ở cuối danh sách</span>
        </header>
        <c:choose>
            <c:when test="${empty pendingReviews}">
                <div class="manage-reviews-empty"><span><i class="fa-solid fa-check" aria-hidden="true"></i></span><h3>Không có đánh giá chờ duyệt</h3><p>Tất cả phản hồi mới đã được xử lý.</p></div>
            </c:when>
            <c:otherwise>
                <div class="manage-reviews-list">
                    <c:forEach var="review" items="${pendingReviews}">
                        <article class="manage-reviews-card">
                            <div class="review-customer-avatar" aria-hidden="true"><c:out value="${empty review.customerName ? 'K' : fn:substring(review.customerName, 0, 1)}"/></div>
                            <div class="review-card-content">
                                <div class="manage-reviews-card-head"><div><strong><c:out value="${review.customerName}"/></strong><span class="manage-reviews-status">Chờ duyệt</span></div><time><fmt:formatDate value="${review.createdAt}" pattern="dd/MM/yyyy 'lúc' HH:mm"/></time></div>
                                <div class="review-stars" aria-label="${review.rating} trên 5 sao"><c:forEach begin="1" end="${review.rating}">★</c:forEach><c:forEach begin="${review.rating + 1}" end="5">☆</c:forEach><span><c:out value="${review.rating}"/> / 5</span></div>
                                <c:choose><c:when test="${not empty review.reviewText}"><p class="review-text"><c:out value="${review.reviewText}"/></p></c:when><c:otherwise><p class="review-text review-no-text">Khách chỉ gửi số sao, không kèm nhận xét.</p></c:otherwise></c:choose>
                                <div class="review-moderation-actions">
                                    <form action="${pageContext.request.contextPath}/manage-reviews" method="post"><input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}"><input type="hidden" name="reviewId" value="${review.reviewId}"><input type="hidden" name="action" value="approve"><button type="submit"><i class="fa-solid fa-check" aria-hidden="true"></i> Duyệt đánh giá</button></form>
                                    <form action="${pageContext.request.contextPath}/manage-reviews" method="post"><input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}"><input type="hidden" name="reviewId" value="${review.reviewId}"><input type="hidden" name="action" value="reject"><button type="submit" class="danger"><i class="fa-regular fa-trash-can" aria-hidden="true"></i> Từ chối và xóa</button></form>
                                </div>
                            </div>
                        </article>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</main>
</body>
</html>
