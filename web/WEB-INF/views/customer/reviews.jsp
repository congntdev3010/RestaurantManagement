<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đánh giá - Việt À la carte</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reviews.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/fragments/header.jsp"/>
<main class="reviews-page">
    <h1>Đánh giá của khách hàng</h1>
    <section class="reviews-summary" aria-label="Điểm đánh giá được duyệt">
        <strong><fmt:formatNumber value="${averageRating}" minFractionDigits="1" maxFractionDigits="1"/> / 5</strong>
        <span><c:out value="${reviewCount}"/> đánh giá đã được Quản lý duyệt</span>
    </section>

    <c:if test="${not empty reviewMessage}">
        <p class="review-flash ${reviewMessageType}" role="status"><c:out value="${reviewMessage}"/></p>
    </c:if>

    <c:choose>
        <c:when test="${isCustomer}">
            <section class="customer-reviews">
                <h2>Gửi đánh giá</h2>
                <p>Khách hàng đã đăng nhập có thể gửi đánh giá. Đánh giá sẽ hiển thị sau khi Quản lý duyệt.</p>
                <form action="${pageContext.request.contextPath}/review" method="post">
                    <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                    <label>Số sao
                        <select name="rating" required>
                            <option value="">Chọn số sao</option>
                            <option value="5">5 - Rất hài lòng</option>
                            <option value="4">4 - Hài lòng</option>
                            <option value="3">3 - Bình thường</option>
                            <option value="2">2 - Chưa hài lòng</option>
                            <option value="1">1 - Không hài lòng</option>
                        </select>
                    </label>
                    <label>Nhận xét (không bắt buộc)
                        <textarea name="reviewText" maxlength="1000" rows="3"></textarea>
                    </label>
                    <button type="submit">Gửi đánh giá</button>
                </form>
            </section>
        </c:when>
        <c:otherwise>
            <p>Vui lòng <a href="${pageContext.request.contextPath}/login">đăng nhập bằng tài khoản khách hàng</a> để gửi đánh giá.</p>
        </c:otherwise>
    </c:choose>

    <section class="approved-reviews">
        <h2>Đánh giá đã được duyệt</h2>
        <c:choose>
            <c:when test="${empty approvedReviews}"><p>Chưa có đánh giá được công khai.</p></c:when>
            <c:otherwise>
                <c:forEach var="review" items="${approvedReviews}">
                    <article class="approved-review">
                        <div class="review-order-heading">
                            <strong><c:out value="${review.customerName}"/></strong>
                            <span><c:out value="${review.rating}"/> / 5</span>
                        </div>
                        <c:if test="${not empty review.reviewText}"><p><c:out value="${review.reviewText}"/></p></c:if>
                        <small><c:out value="${review.createdAt}"/></small>
                    </article>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </section>
</main>
</body>
</html>
