<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tổng quan quản lý</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/manager-dashboard.css?v=1.0">
</head>
<body class="manager-dashboard-page">
    <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
    <main class="manager-dashboard">
        <header class="dashboard-heading">
            <div>
                <p class="eyebrow">TỔNG QUAN VẬN HÀNH</p>
                <h1>Dashboard quản lý</h1>
                <p class="dashboard-subtitle">Tình hình kinh doanh và phục vụ trong hôm nay.</p>
            </div>
            <div class="today-badge"><i class="fa-regular fa-calendar" aria-hidden="true"></i> Hôm nay</div>
        </header>

        <section class="summary-grid" aria-label="Chỉ số trong ngày">
            <article class="summary-card revenue-card">
                <div class="summary-card-top">
                    <span>Doanh thu hôm nay</span>
                    <span class="summary-icon"><i class="fa-solid fa-coins" aria-hidden="true"></i></span>
                </div>
                <strong class="summary-value"><fmt:formatNumber value="${dashboard.todayRevenue}" groupingUsed="true" maxFractionDigits="0"/> <small>đ</small></strong>
                <c:choose>
                    <c:when test="${dashboard.revenueIncrease}">
                        <p class="comparison is-up"><i class="fa-solid fa-arrow-trend-up" aria-hidden="true"></i>
                            Tăng <fmt:formatNumber value="${dashboard.revenueDifference}" groupingUsed="true" maxFractionDigits="0"/> đ
                            <span>so với hôm qua</span></p>
                    </c:when>
                    <c:when test="${dashboard.revenueDecrease}">
                        <p class="comparison is-down"><i class="fa-solid fa-arrow-trend-down" aria-hidden="true"></i>
                            Giảm <fmt:formatNumber value="${dashboard.revenueDifference}" groupingUsed="true" maxFractionDigits="0"/> đ
                            <span>so với hôm qua</span></p>
                    </c:when>
                    <c:otherwise>
                        <p class="comparison is-neutral"><i class="fa-solid fa-minus" aria-hidden="true"></i>
                            Bằng hôm qua</p>
                    </c:otherwise>
                </c:choose>
            </article>
            <article class="summary-card">
                <div class="summary-card-top">
                    <span>Đơn hàng trong ngày</span>
                    <span class="summary-icon icon-green"><i class="fa-solid fa-receipt" aria-hidden="true"></i></span>
                </div>
                <strong class="summary-value"><c:out value="${dashboard.todayOrderCount}"/></strong>
                <p class="summary-note">Không bao gồm đơn đã hủy</p>
            </article>
            <article class="summary-card">
                <div class="summary-card-top">
                    <span>Bàn đang phục vụ</span>
                    <span class="summary-icon icon-blue"><i class="fa-solid fa-chair" aria-hidden="true"></i></span>
                </div>
                <strong class="summary-value"><c:out value="${dashboard.occupiedTableCount}"/>
                    <small>/ <c:out value="${dashboard.totalTableCount}"/> bàn</small></strong>
                <p class="summary-note">Theo trạng thái bàn hiện tại</p>
            </article>
            <article class="summary-card">
                <div class="summary-card-top">
                    <span>Khách hàng hôm nay</span>
                    <span class="summary-icon icon-purple"><i class="fa-solid fa-user-group" aria-hidden="true"></i></span>
                </div>
                <strong class="summary-value"><c:out value="${dashboard.newCustomerCount + dashboard.returningCustomerCount}"/></strong>
                <p class="customer-split">
                    <span><b><c:out value="${dashboard.newCustomerCount}"/></b> khách mới</span>
                    <span><b><c:out value="${dashboard.returningCustomerCount}"/></b> quay lại</span>
                </p>
            </article>
        </section>

        <section class="insight-grid" aria-label="Hiệu quả vận hành và khách hàng thân thiết">
            <article class="report-card insight-card">
                <div class="insight-card-heading">
                    <span class="summary-icon icon-red"><i class="fa-solid fa-ban" aria-hidden="true"></i></span>
                    <div>
                        <p class="eyebrow">VẬN HÀNH</p>
                        <h2>Tỷ lệ hủy đơn</h2>
                    </div>
                </div>
                <strong class="insight-value"><fmt:formatNumber value="${dashboard.cancellationRate}" minFractionDigits="1" maxFractionDigits="1"/>%</strong>
                <p class="summary-note">
                    <c:out value="${dashboard.cancelledOrderCount}"/> đơn hủy /
                    <c:out value="${dashboard.allTodayOrderCount}"/> đơn tạo hôm nay
                </p>
                <p class="insight-footnote">Tính cả đơn đã hủy trong tổng số đơn.</p>
            </article>

            <article class="report-card insight-card">
                <div class="insight-card-heading">
                    <span class="summary-icon icon-blue"><i class="fa-solid fa-chair" aria-hidden="true"></i></span>
                    <div>
                        <p class="eyebrow">HIỆU SUẤT BÀN</p>
                        <h2>Lượt phục vụ hôm nay</h2>
                    </div>
                </div>
                <strong class="insight-value"><c:out value="${dashboard.todayServiceCount}"/></strong>
                <p class="summary-note">
                    <c:choose>
                        <c:when test="${not empty dashboard.averageServiceMinutes}">
                            Trung bình <fmt:formatNumber value="${dashboard.averageServiceMinutes}" minFractionDigits="0" maxFractionDigits="0"/> phút / lượt
                        </c:when>
                        <c:otherwise>Chưa có dữ liệu check-in để tính thời gian phục vụ.</c:otherwise>
                    </c:choose>
                </p>
                <p class="insight-footnote">Thời gian tính từ check-in đến thanh toán.</p>
                <c:if test="${not empty dashboard.busiestTables}">
                    <h3 class="mini-heading">Bàn có nhiều lượt nhất</h3>
                    <ol class="mini-ranking">
                        <c:forEach var="table" items="${dashboard.busiestTables}" varStatus="loop">
                            <li>
                                <span><b><c:out value="${loop.count}"/></b> <c:out value="${table.tableName}"/></span>
                                <strong><c:out value="${table.serviceCount}"/> lượt</strong>
                            </li>
                        </c:forEach>
                    </ol>
                </c:if>
            </article>

            <article class="report-card insight-card">
                <div class="insight-card-heading">
                    <span class="summary-icon icon-purple"><i class="fa-solid fa-star" aria-hidden="true"></i></span>
                    <div>
                        <p class="eyebrow">LOYALTY</p>
                        <h2>Khách hàng thân thiết</h2>
                    </div>
                </div>
                <strong class="insight-value"><c:out value="${dashboard.loyaltyCustomerCount}"/></strong>
                <p class="summary-note">Khách dùng tài khoản tích điểm hôm nay</p>
                <div class="loyalty-breakdown">
                    <div><span>Điểm đã cộng</span><strong>+<c:out value="${dashboard.loyaltyPointsEarnedToday}"/></strong></div>
                    <div><span>Điểm đã dùng</span><strong>-<c:out value="${dashboard.loyaltyPointsUsedToday}"/></strong></div>
                    <div><span>Tổng điểm còn hiệu lực</span><strong><c:out value="${dashboard.outstandingLoyaltyPoints}"/></strong></div>
                </div>
                <p class="insight-footnote">Điểm cộng/trừ tính từ hóa đơn đã thanh toán hôm nay.</p>
            </article>
        </section>

        <section class="report-section">
            <div class="section-heading">
                <div>
                    <p class="eyebrow">THEO DANH MỤC</p>
                    <h2>Top 5 món bán chạy</h2>
                </div>
                <span class="section-period">Số lượng bán hôm nay</span>
            </div>
            <div class="category-grid">
                <c:forEach var="category" items="${dashboard.categorySales}">
                    <article class="report-card">
                        <h3><c:out value="${category.categoryName}"/></h3>
                        <c:choose>
                            <c:when test="${not empty category.dishes}">
                                <ol class="dish-ranking">
                                    <c:forEach var="dish" items="${category.dishes}" varStatus="loop">
                                        <li>
                                            <span class="rank-number"><c:out value="${loop.count}"/></span>
                                            <span class="dish-name"><c:out value="${dish.dishName}"/></span>
                                            <strong><c:out value="${dish.quantity}"/> <small><c:out value="${dish.unit}"/></small></strong>
                                        </li>
                                    </c:forEach>
                                </ol>
                            </c:when>
                            <c:otherwise>
                                <p class="empty-note">Chưa có món nào được bán hôm nay.</p>
                            </c:otherwise>
                        </c:choose>
                    </article>
                </c:forEach>
                <c:if test="${empty dashboard.categorySales}">
                    <div class="report-card empty-card">
                        <p class="empty-note">Chưa có danh mục món ăn đang hoạt động.</p>
                    </div>
                </c:if>
            </div>
        </section>

        <section class="report-section least-section">
            <div class="section-heading">
                <div>
                    <p class="eyebrow">CẦN QUAN TÂM</p>
                    <h2>5 món bán ít nhất</h2>
                </div>
                <span class="section-period">Các món đang hoạt động · hôm nay</span>
            </div>
            <article class="report-card least-card">
                <c:choose>
                    <c:when test="${not empty dashboard.leastSoldDishes}">
                        <ol class="least-list">
                            <c:forEach var="dish" items="${dashboard.leastSoldDishes}" varStatus="loop">
                                <li>
                                    <span class="rank-number muted-rank"><c:out value="${loop.count}"/></span>
                                    <span class="least-name">
                                        <strong><c:out value="${dish.dishName}"/></strong>
                                        <small><c:out value="${dish.categoryName}"/></small>
                                    </span>
                                    <span class="least-quantity"><c:out value="${dish.quantity}"/>
                                        <small><c:out value="${dish.unit}"/></small></span>
                                </li>
                            </c:forEach>
                        </ol>
                    </c:when>
                    <c:otherwise>
                        <p class="empty-note">Chưa có món ăn đang hoạt động để thống kê.</p>
                    </c:otherwise>
                </c:choose>
            </article>
        </section>
    </main>
</body>
</html>
