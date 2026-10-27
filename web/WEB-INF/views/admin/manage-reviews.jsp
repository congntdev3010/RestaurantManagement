<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Duyệt đánh giá</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reviews.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/fragments/header.jsp"/>
<main class="reviews-page">
    <h1>Duyệt đánh giá</h1>
    <c:if test="${not empty manageReviewMessage}">
        <p class="review-flash ${manageReviewMessageType}" role="status"><c:out value="${manageReviewMessage}"/></p>
    </c:if>
    <c:choose>
        <c:when test="${empty pendingReviews}"><p>Không có đánh giá nào đang chờ duyệt.</p></c:when>
        <c:otherwise>
            <c:forEach var="review" items="${pendingReviews}">
                <article class="approved-review">
                    <div class="review-order-heading">
                        <strong><c:out value="${review.customerName}"/></strong>
                        <span><c:out value="${review.rating}"/> / 5</span>
                    </div>
                    <c:if test="${not empty review.reviewText}"><p><c:out value="${review.reviewText}"/></p></c:if>
                    <small><c:out value="${review.createdAt}"/></small>
                    <div class="review-moderation-actions">
                        <form action="${pageContext.request.contextPath}/manage-reviews" method="post">
                            <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                            <input type="hidden" name="reviewId" value="${review.reviewId}">
                            <input type="hidden" name="action" value="approve">
                            <button type="submit">Duyệt</button>
                        </form>
                        <form action="${pageContext.request.contextPath}/manage-reviews" method="post">
                            <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                            <input type="hidden" name="reviewId" value="${review.reviewId}">
                            <input type="hidden" name="action" value="reject">
                            <button type="submit" class="danger">Từ chối và xóa</button>
                        </form>
                    </div>
                </article>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
