<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Báo cáo doanh thu - Việt À la carte</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/manager-report.css">
    </head>
    <body class="manager-report-page">
        <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
        <main class="report-shell">
            <header class="report-heading">
                <p class="report-eyebrow">TỔNG QUAN KINH DOANH</p>
                <h1>Báo cáo nhà hàng</h1>
                <p>Doanh thu tính theo số tiền khách thực trả. Số lượng món tính từ các đơn đã thanh toán.</p>
            </header>

            <section class="revenue-grid" aria-label="Doanh thu">
                <article class="revenue-card revenue-today">
                    <span class="revenue-icon"><i class="fa-solid fa-calendar-day" aria-hidden="true"></i></span>
                    <span>Doanh thu hôm nay</span>
                    <strong><fmt:formatNumber value="${revenueSummary.today}" pattern="#,##0"/> đ</strong>
                    <small><c:out value="${reportDate}"/></small>
                </article>
                <article class="revenue-card">
                    <span class="revenue-icon"><i class="fa-solid fa-calendar-week" aria-hidden="true"></i></span>
                    <span>Doanh thu tháng này</span>
                    <strong><fmt:formatNumber value="${revenueSummary.month}" pattern="#,##0"/> đ</strong>
                    <small>Từ đầu tháng đến hôm nay</small>
                </article>
                <article class="revenue-card">
                    <span class="revenue-icon"><i class="fa-solid fa-chart-column" aria-hidden="true"></i></span>
                    <span>Doanh thu năm nay</span>
                    <strong><fmt:formatNumber value="${revenueSummary.year}" pattern="#,##0"/> đ</strong>
                    <small>Từ đầu năm đến hôm nay</small>
                </article>
            </section>

            <section class="dish-report" aria-labelledby="dish-report-title">
                <div class="section-heading">
                    <div>
                        <p class="report-eyebrow">THEO DANH MỤC</p>
                        <h2 id="dish-report-title">Món bán chạy &amp; ít được chọn</h2>
                    </div>
                    <span class="report-period">Toàn bộ đơn đã thanh toán</span>
                </div>

                <c:choose>
                    <c:when test="${empty categoryReports}">
                        <p class="report-empty">Chưa có danh mục món ăn để thống kê.</p>
                    </c:when>
                    <c:otherwise>
                        <div class="category-grid">
                            <c:forEach var="category" items="${categoryReports}">
                                <article class="category-card">
                                    <h3><c:out value="${category.categoryName}"/></h3>
                                    <div class="category-content">
                                        <section class="top-dishes">
                                            <h4><i class="fa-solid fa-arrow-trend-up" aria-hidden="true"></i> Top 5 bán chạy</h4>
                                            <c:choose>
                                                <c:when test="${empty category.topDishes}">
                                                    <p class="no-dishes">Danh mục chưa có món.</p>
                                                </c:when>
                                                <c:otherwise>
                                                    <ol>
                                                        <c:forEach var="dish" items="${category.topDishes}" varStatus="loop">
                                                            <li>
                                                                <span class="dish-rank"><c:out value="${loop.count}"/></span>
                                                                <span class="dish-name"><c:out value="${dish.dishName}"/></span>
                                                                <strong><fmt:formatNumber value="${dish.quantitySold}" pattern="#,##0"/> phần</strong>
                                                            </li>
                                                        </c:forEach>
                                                    </ol>
                                                </c:otherwise>
                                            </c:choose>
                                        </section>
                                        <section class="least-dish">
                                            <h4><i class="fa-solid fa-arrow-trend-down" aria-hidden="true"></i> Ít được chọn nhất</h4>
                                            <c:choose>
                                                <c:when test="${not empty category.leastChosenDish}">
                                                    <div class="least-dish-info">
                                                        <span><c:out value="${category.leastChosenDish.dishName}"/></span>
                                                        <strong><fmt:formatNumber value="${category.leastChosenDish.quantitySold}" pattern="#,##0"/> phần</strong>
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <p class="no-dishes">Danh mục chưa có món.</p>
                                                </c:otherwise>
                                            </c:choose>
                                        </section>
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
