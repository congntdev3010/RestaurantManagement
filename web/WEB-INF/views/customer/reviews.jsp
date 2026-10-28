<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
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
<main class="reviews-page customer-reviews-page">
    <header class="reviews-hero">
        <div class="reviews-hero-copy">
            <p class="reviews-eyebrow">LỜI CHIA SẺ TỪ THỰC KHÁCH</p>
            <h1>Trải nghiệm của bạn là niềm cảm hứng của chúng tôi</h1>
            <p class="reviews-intro">Mỗi lời nhận xét giúp chúng tôi chăm chút hơn cho từng bữa ăn tại Việt À la carte.</p>
        </div>
        <section class="reviews-summary" aria-label="Điểm đánh giá được duyệt">
            <span class="summary-label">ĐIỂM ĐÁNH GIÁ TRUNG BÌNH</span>
            <strong><fmt:formatNumber value="${averageRating}" minFractionDigits="1" maxFractionDigits="1"/></strong>
            <span class="summary-stars" aria-label="${averageRating} trên 5 sao"><c:forEach begin="1" end="5" varStatus="star"><c:choose><c:when test="${averageRating ge star.count}">★</c:when><c:otherwise>☆</c:otherwise></c:choose></c:forEach></span>
            <span class="summary-count"><c:out value="${reviewCount}"/> đánh giá đã được duyệt</span>
        </section>
    </header>

    <c:if test="${not empty reviewMessage}"><p class="review-flash ${reviewMessageType}" role="status"><c:out value="${reviewMessage}"/></p></c:if>

    <div class="reviews-content-grid">
        <aside class="review-write-column">
            <c:choose>
                <c:when test="${isCustomer}">
                    <section class="customer-reviews review-panel">
                        <p class="panel-kicker">GÓP Ý CÙNG CHÚNG TÔI</p>
                        <h2>Chia sẻ trải nghiệm</h2>
                        <p class="panel-description">Đánh giá của bạn sẽ được quản lý xem xét trước khi hiển thị công khai.</p>
                        <form id="review-submit-form" action="${pageContext.request.contextPath}/review" method="post">
                            <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                            <fieldset class="rating-picker">
                                <legend>Bạn hài lòng đến mức nào?</legend>
                                <div class="rating-stars" role="radiogroup" aria-label="Chọn số sao">
                                    <input id="rating-5" type="radio" name="rating" value="5" required><label for="rating-5" title="5 sao" aria-label="5 sao">★</label>
                                    <input id="rating-4" type="radio" name="rating" value="4" required><label for="rating-4" title="4 sao" aria-label="4 sao">★</label>
                                    <input id="rating-3" type="radio" name="rating" value="3" required><label for="rating-3" title="3 sao" aria-label="3 sao">★</label>
                                    <input id="rating-2" type="radio" name="rating" value="2" required><label for="rating-2" title="2 sao" aria-label="2 sao">★</label>
                                    <input id="rating-1" type="radio" name="rating" value="1" required><label for="rating-1" title="1 sao" aria-label="1 sao">★</label>
                                </div>
                                <output class="rating-description" id="rating-description" aria-live="polite">Chọn số sao</output>
                            </fieldset>
                            <label for="review-text">Nhận xét <span>(không bắt buộc)</span></label>
                            <textarea id="review-text" name="reviewText" maxlength="1000" rows="5" placeholder="Điều gì khiến trải nghiệm của bạn đáng nhớ?"></textarea>
                            <button class="review-submit" type="submit">Gửi đánh giá <span aria-hidden="true">→</span></button>
                        </form>
                    </section>
                </c:when>
                <c:otherwise>
                    <section class="review-login-card">
                        <span class="login-icon" aria-hidden="true">✦</span>
                        <h2>Bạn đã dùng bữa tại nhà hàng?</h2>
                        <p>Đăng nhập bằng tài khoản khách hàng để gửi đánh giá và chia sẻ trải nghiệm của bạn.</p>
                        <a href="${pageContext.request.contextPath}/login">Đăng nhập để đánh giá <span aria-hidden="true">→</span></a>
                    </section>
                </c:otherwise>
            </c:choose>
        </aside>

        <section class="approved-reviews" aria-labelledby="approved-reviews-title">
            <div class="reviews-section-heading">
                <div><p class="panel-kicker">KHÁCH HÀNG NÓI GÌ</p><h2 id="approved-reviews-title">Đánh giá đã được duyệt</h2></div>
                <span class="reviews-total"><c:out value="${reviewCount}"/> đánh giá</span>
            </div>
            <c:choose>
                <c:when test="${empty approvedReviews}">
                    <div class="reviews-empty"><span aria-hidden="true">☆</span><p>Chưa có đánh giá được công khai.</p><small>Hãy là người đầu tiên chia sẻ cảm nhận của bạn.</small></div>
                </c:when>
                <c:otherwise>
                    <div class="approved-reviews-list">
                        <c:forEach var="review" items="${approvedReviews}">
                            <article class="approved-review">
                                <div class="review-customer-avatar" aria-hidden="true"><c:out value="${empty review.customerName ? 'K' : fn:substring(review.customerName, 0, 1)}"/></div>
                                <div class="review-card-content">
                                    <div class="review-order-heading"><strong><c:out value="${review.customerName}"/></strong><time><fmt:formatDate value="${review.createdAt}" pattern="dd/MM/yyyy"/></time></div>
                                    <div class="review-stars" aria-label="${review.rating} trên 5 sao"><c:forEach begin="1" end="${review.rating}">★</c:forEach><c:forEach begin="${review.rating + 1}" end="5">☆</c:forEach></div>
                                    <c:if test="${not empty review.reviewText}"><p class="review-text"><c:out value="${review.reviewText}"/></p></c:if>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</main>
<script>
(() => {
    const form = document.getElementById('review-submit-form');
    if (!form) return;
    const description = document.getElementById('rating-description');
    const labels = {1: 'Không hài lòng', 2: 'Chưa hài lòng', 3: 'Bình thường', 4: 'Hài lòng', 5: 'Rất hài lòng'};
    form.querySelectorAll('input[name="rating"]').forEach((input) => {
        input.addEventListener('change', () => { description.textContent = labels[input.value]; });
    });
    form.addEventListener('submit', () => {
        const button = form.querySelector('.review-submit');
        if (!form.reportValidity()) return;
        button.disabled = true;
        button.setAttribute('aria-busy', 'true');
        button.innerHTML = 'Đang gửi…';
    });
})();
</script>
</body>
</html>
