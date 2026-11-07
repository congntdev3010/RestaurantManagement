<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Duyệt đánh giá | Việt À la carte</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reviews.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/fragments/header.jsp"/>
<main class="reviews-shell manage-reviews-shell">
    <header class="manage-reviews-heading">
        <div><p class="reviews-eyebrow">VẬN HÀNH NHÀ HÀNG</p><h1>Quản lý đánh giá</h1></div>
        <span><c:out value="${reviewSummary.pendingCount}"/> đang chờ duyệt</span>
    </header>
    <c:if test="${not empty reviewMessage}">
        <p class="review-message ${reviewMessageType eq 'success' ? 'is-success' : 'is-error'}" role="status"><c:out value="${reviewMessage}"/></p>
    </c:if>
    <section class="review-stats" aria-label="Thống kê đánh giá">
        <article class="review-stat-card"><span>Tổng đánh giá</span><strong><c:out value="${reviewSummary.totalCount}"/></strong><small>Tất cả trạng thái</small></article>
        <article class="review-stat-card"><span>Điểm trung bình</span><strong><fmt:formatNumber value="${reviewSummary.averageRating}" minFractionDigits="1" maxFractionDigits="1"/> <i>★</i></strong><small><c:out value="${reviewSummary.approvedCount}"/> đánh giá đã duyệt</small></article>
        <article class="review-stat-card"><span>Chờ duyệt</span><strong><c:out value="${reviewSummary.pendingCount}"/></strong><small>Cần quản lý xử lý</small></article>
        <article class="review-stat-card"><span>Đã từ chối</span><strong><c:out value="${reviewSummary.rejectedCount}"/></strong><small>Không hiển thị công khai</small></article>
    </section>
    <section class="review-rating-breakdown" aria-label="Phân bố số sao của đánh giá đã duyệt">
        <h2>Phân bố số sao <small>Đánh giá đã duyệt</small></h2>
        <c:forEach begin="1" end="5" var="star">
            <c:set var="starCount" value="${star eq 1 ? reviewSummary.oneStarCount : star eq 2 ? reviewSummary.twoStarCount : star eq 3 ? reviewSummary.threeStarCount : star eq 4 ? reviewSummary.fourStarCount : reviewSummary.fiveStarCount}"/>
            <div class="rating-breakdown-row">
                <span><c:out value="${star}"/> <b>★</b></span>
                <div class="rating-breakdown-track"><span style="width: ${reviewSummary.approvedCount eq 0 ? 0 : starCount * 100 / reviewSummary.approvedCount}%"></span></div>
                <strong><c:out value="${starCount}"/></strong>
            </div>
        </c:forEach>
    </section>
    <section class="pending-reviews">
        <div class="review-list-heading"><h2>Danh sách đánh giá</h2><span>Tối đa 300 đánh giá mới nhất</span></div>
        <c:choose>
            <c:when test="${empty reviews}">
                <div class="reviews-empty"><span aria-hidden="true">✦</span><h2>Chưa có đánh giá</h2><p>Đánh giá của khách sẽ xuất hiện tại đây.</p></div>
            </c:when>
            <c:otherwise>
                <c:forEach var="review" items="${reviews}">
                    <article class="pending-review-card">
                        <div class="pending-review-meta">
                            <span>Đánh giá #<c:out value="${review.reviewId}"/> · Hóa đơn #<c:out value="${review.historyId}"/></span>
                            <time><c:out value="${review.createdAtText}"/></time>
                        </div>
                        <div class="pending-review-customer"><h2><c:out value="${review.customerName}"/></h2><span>Bàn <c:out value="${empty review.tableName ? '—' : review.tableName}"/></span></div>
                        <div class="review-stars" aria-label="${review.rating} trên 5 sao">
                            <c:forEach begin="1" end="${review.rating}">★</c:forEach><c:forEach begin="${review.rating + 1}" end="5">☆</c:forEach>
                            <strong><c:out value="${review.rating}"/>/5</strong>
                        </div>
                        <blockquote><c:out value="${review.comment}"/></blockquote>
                        <div class="moderation-actions">
                            <span class="review-status ${review.status eq 'APPROVED' ? 'is-approved' : review.status eq 'REJECTED' ? 'is-rejected' : 'is-pending'}">
                                <c:out value="${review.status eq 'APPROVED' ? 'Đã duyệt' : review.status eq 'REJECTED' ? 'Đã từ chối' : 'Chờ duyệt'}"/>
                            </span>
                            <div class="moderation-buttons">
                                <c:if test="${review.status eq 'PENDING'}">
                                    <form method="post" action="${pageContext.request.contextPath}/manage-reviews">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.reviewCsrf}">
                                        <input type="hidden" name="action" value="moderate">
                                        <input type="hidden" name="reviewId" value="${review.reviewId}">
                                        <input type="hidden" name="targetStatus" value="APPROVED">
                                        <button class="approve-review" type="submit">Chấp nhận</button>
                                    </form>
                                    <form method="post" action="${pageContext.request.contextPath}/manage-reviews">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.reviewCsrf}">
                                        <input type="hidden" name="action" value="moderate">
                                        <input type="hidden" name="reviewId" value="${review.reviewId}">
                                        <input type="hidden" name="targetStatus" value="REJECTED">
                                        <button class="reject-review" type="submit">Từ chối</button>
                                    </form>
                                </c:if>
                                <form method="post" action="${pageContext.request.contextPath}/manage-reviews" onsubmit="return confirm('Xóa vĩnh viễn đánh giá này? Khách có thể gửi lại đánh giá cho hóa đơn.');">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.reviewCsrf}">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="reviewId" value="${review.reviewId}">
                                    <button class="delete-review" type="submit">Xóa</button>
                                </form>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </section>
</main>
</body>
</html>
