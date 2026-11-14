<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Đánh giá bữa ăn | Việt À la carte</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reviews.css?v=2"></head><body class="review-page">
<jsp:include page="/WEB-INF/views/fragments/header.jsp"/><main class="review-shell review-write-shell">
<a class="review-back" href="${pageContext.request.contextPath}/my-payments">← Quay lại lịch sử thanh toán</a>
<section class="review-write-card"><p class="review-kicker">VIỆT À LA CARTE · HÓA ĐƠN #<c:out value="${bill.historyId}"/></p><h1>Chia sẻ trải nghiệm</h1>
<p class="review-write-intro">Bữa ăn của bạn tại bàn <strong><c:out value="${bill.tableName}"/></strong> · <c:out value="${bill.paidAtText}"/></p>
<c:choose><c:when test="${alreadyReviewed}"><div class="review-empty">Hóa đơn này đã có đánh giá. Cảm ơn bạn đã chia sẻ!</div></c:when><c:otherwise>
<form method="post" action="${pageContext.request.contextPath}/review" class="review-write-form">
<input type="hidden" name="historyId" value="${bill.historyId}">
<fieldset class="review-rating"><legend>Bạn chấm bữa ăn này bao nhiêu sao?</legend><div class="review-rating-stars">
<label><input type="radio" name="rating" value="5" required><span aria-label="5 sao">★</span></label>
<label><input type="radio" name="rating" value="4"><span aria-label="4 sao">★</span></label>
<label><input type="radio" name="rating" value="3"><span aria-label="3 sao">★</span></label>
<label><input type="radio" name="rating" value="2"><span aria-label="2 sao">★</span></label>
<label><input type="radio" name="rating" value="1"><span aria-label="1 sao">★</span></label>
</div><div class="review-rating-hint">1 sao · Chưa hài lòng <span>Rất hài lòng · 5 sao</span></div></fieldset>
<label class="review-comment-label" for="reviewText">Lời nhắn của bạn</label><textarea id="reviewText" name="reviewText" maxlength="1000" required rows="6" placeholder="Món ăn, không gian, cách phục vụ..."></textarea>
<button type="submit" class="review-submit">Gửi đánh giá</button></form>
</c:otherwise></c:choose></section></main></body></html>
