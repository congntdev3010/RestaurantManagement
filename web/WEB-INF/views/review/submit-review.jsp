<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Gửi đánh giá | Việt À la carte</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reviews.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/fragments/header.jsp"/>
<main class="reviews-shell review-form-shell">
    <a class="review-back" href="${pageContext.request.contextPath}/my-payments">&larr; Lịch sử thanh toán</a>
    <section class="review-form-card">
        <p class="reviews-eyebrow">CẢM ƠN BẠN ĐÃ GHÉ THĂM</p>
        <h1>Chia sẻ trải nghiệm</h1>
        <p class="form-intro">Mỗi lời góp ý giúp chúng tôi chăm chút hơn cho những lần gặp tiếp theo.</p>
        <div class="review-bill">
            <span>Hóa đơn #<c:out value="${bill.historyId}"/></span>
            <strong><c:out value="${bill.tableName}"/> · <c:out value="${bill.paidAtText}"/></strong>
            <span>Đã thanh toán <b><fmt:formatNumber value="${bill.payableAmount}" pattern="#,##0"/> đ</b></span>
        </div>
        <c:if test="${not empty reviewMessage}">
            <p class="review-message ${reviewMessageType eq 'success' ? 'is-success' : 'is-error'}" role="status"><c:out value="${reviewMessage}"/></p>
        </c:if>
        <c:choose>
            <c:when test="${alreadySubmitted}">
                <div class="review-done">Cảm ơn bạn, đánh giá của hóa đơn này đã được gửi.</div>
            </c:when>
            <c:otherwise>
                <form class="review-form" method="post" action="${pageContext.request.contextPath}/submit-review">
                    <input type="hidden" name="csrfToken" value="${sessionScope.reviewCsrf}">
                    <input type="hidden" name="historyId" value="${bill.historyId}">
                    <fieldset class="rating-field">
                        <legend>Mức độ hài lòng của bạn</legend>
                        <div class="rating-options">
                            <c:forEach begin="1" end="5" var="score">
                                <label>
                                    <input type="radio" name="rating" value="${score}" required>
                                    <span aria-hidden="true">★</span>
                                    <small><c:out value="${score}"/></small>
                                </label>
                            </c:forEach>
                        </div>
                    </fieldset>
                    <label class="comment-label" for="review-comment">Điều bạn muốn chia sẻ</label>
                    <textarea id="review-comment" name="comment" rows="5" maxlength="1000" required placeholder="Món ăn, không gian hay sự phục vụ đã để lại ấn tượng gì với bạn?"></textarea>
                    <div class="form-footer"><span>Tối đa 1.000 ký tự</span><button type="submit">Gửi đánh giá <span aria-hidden="true">→</span></button></div>
                </form>
            </c:otherwise>
        </c:choose>
    </section>
</main>
</body>
</html>
