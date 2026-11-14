<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý đánh giá</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reviews.css?v=2">
</head>
<body class="review-page review-admin">
    <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
    <main class="review-shell review-management">
        <header class="review-admin-head">
            <p class="review-kicker">QUẢN LÝ NHÀ HÀNG</p>
            <h1>Quản lý đánh giá</h1>
            <p>Theo dõi phản hồi của thực khách và kiểm duyệt nội dung công khai.</p>
            <c:if test="${not empty sessionScope.reviewManageMessage}">
                <div class="review-flash"><c:out value="${sessionScope.reviewManageMessage}" /></div>
                <c:remove var="reviewManageMessage" scope="session" />
            </c:if>
        </header>

        <section class="review-stats" aria-label="Thống kê đánh giá">
            <article class="review-stat"><span>Điểm trung bình</span><strong><fmt:formatNumber value="${averageRating}" minFractionDigits="1" maxFractionDigits="1" /></strong><small>Trên các đánh giá đã duyệt</small></article>
            <article class="review-stat"><span>Tổng đánh giá</span><strong><c:out value="${totalReviews}" /></strong><small>Tất cả trạng thái</small></article>
            <article class="review-stat review-stat-pending"><span>Cần chấp nhận</span><strong><c:out value="${pendingCount}" /></strong><small>Đang chờ manager duyệt</small></article>
        </section>

        <section class="review-list" aria-label="Tất cả đánh giá">
            <c:choose>
                <c:when test="${empty reviews}"><div class="review-empty">Chưa có đánh giá nào.</div></c:when>
                <c:otherwise>
                    <c:forEach var="review" items="${reviews}">
                        <article class="review-card">
                            <div class="review-card-top">
                                <div>
                                    <strong><c:out value="${review.customerName}" /></strong>
                                    <small>Hóa đơn #<c:out value="${review.historyId}" /> · <c:out value="${review.createdAtText}" /> · Bàn <c:out value="${review.tableName}" /></small>
                                </div>
                                <div class="review-card-rating">
                                    <span class="stars" aria-label="${review.rating} trên 5 sao"><c:forEach begin="1" end="${review.rating}" var="i">★</c:forEach><c:forEach begin="1" end="${5-review.rating}" var="i">☆</c:forEach></span>
                                    <span class="review-status review-status-${review.status}"><c:choose><c:when test="${review.status eq 'PENDING'}">Chờ duyệt</c:when><c:when test="${review.status eq 'APPROVED'}">Đã duyệt</c:when><c:otherwise>Đã từ chối</c:otherwise></c:choose></span>
                                </div>
                            </div>
                            <p><c:out value="${review.reviewText}" /></p>
                            <div class="review-actions">
                                <c:if test="${review.status eq 'PENDING'}">
                                    <form method="post" action="${pageContext.request.contextPath}/manage-reviews">
                                        <input type="hidden" name="reviewId" value="${review.reviewId}">
                                        <button class="review-approve" name="action" value="approve">Chấp nhận</button>
                                        <button class="review-reject" name="action" value="reject">Từ chối</button>
                                    </form>
                                </c:if>
                                <form method="post" action="${pageContext.request.contextPath}/manage-reviews" onsubmit="return confirm('Bạn có chắc muốn xóa đánh giá này?');">
                                    <input type="hidden" name="reviewId" value="${review.reviewId}">
                                    <button class="review-delete" name="action" value="delete">Xóa đánh giá</button>
                                </form>
                            </div>
                        </article>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </section>
    </main>
</body>
</html>
