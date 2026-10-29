<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thiết lập quy đổi điểm tích lũy</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/payment.css?v=1.1">
    <style>
        .ls-wrap { max-width: 860px; margin: 0 auto; }
        .ls-head { margin-bottom: 22px; }
        .ls-head .eyebrow { margin: 0 0 6px; }
        .ls-head h1 { margin: 0 0 6px; font-size: 1.9rem; color: #3f1a1f; }
        .ls-head p { margin: 0; color: #7a6a64; }

        .ls-alert { margin: 0 0 18px; padding: 12px 16px; border-radius: 12px; font-size: .94rem; }
        .ls-alert.success { background: #e6f4ea; color: #1e6b38; border: 1px solid #c5e5cf; }
        .ls-alert.error { background: #fdeaea; color: #9b2227; border: 1px solid #f3c6c8; }

        .ls-current { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px; margin-bottom: 22px; }
        .ls-pill { background: linear-gradient(135deg, #4a1620, #7b1f28); color: #fff; border-radius: 16px; padding: 16px 20px; }
        .ls-pill span { display: block; font-size: .75rem; letter-spacing: .07em; text-transform: uppercase; opacity: .8; margin-bottom: 6px; }
        .ls-pill strong { font-size: 1.35rem; }

        .ls-form { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 18px; }
        .ls-card { background: #fff; border: 1px solid #ecdfd5; border-radius: 18px; padding: 22px 24px; box-shadow: 0 6px 18px rgba(74, 22, 32, .05); }
        .ls-card h2 { margin: 0 0 4px; font-size: 1.2rem; color: #4a1620; }
        .ls-card .desc { margin: 0 0 18px; font-size: .88rem; color: #8a7a73; }
        .ls-field { margin-bottom: 16px; }
        .ls-field:last-child { margin-bottom: 0; }
        .ls-field label { display: block; font-size: .85rem; font-weight: 600; color: #4d3b35; margin-bottom: 6px; }
        .ls-input { display: flex; align-items: center; border: 1px solid #e3d3c8; border-radius: 12px; background: #fffaf6; overflow: hidden; }
        .ls-input:focus-within { border-color: #c98a3d; box-shadow: 0 0 0 3px rgba(201, 138, 61, .18); }
        .ls-input input { flex: 1; min-width: 0; border: 0; background: transparent; padding: 12px 14px; font: inherit; font-size: 1.05rem; outline: none; }
        .ls-input .unit { padding: 0 16px; color: #8a7a73; font-size: .9rem; background: #f6ece4; align-self: stretch; display: flex; align-items: center; }

        .ls-actions { grid-column: 1 / -1; display: flex; align-items: center; justify-content: space-between; gap: 14px; flex-wrap: wrap; }
        .ls-actions .meta { color: #8a7a73; font-size: .85rem; }
        .ls-save { border: 0; border-radius: 12px; background: #7b1f28; color: #fff; padding: 13px 34px; font: inherit; font-weight: 700; cursor: pointer; }
        .ls-save:hover { background: #63171f; }
    </style>
</head>
<body class="payment-page">
    <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
    <main class="payment-shell">
        <div class="ls-wrap">
            <header class="ls-head">
                <p class="eyebrow">QUẢN LÝ</p>
                <h1>Quy đổi điểm tích lũy</h1>
                <p>Quy tắc mới chỉ áp dụng cho các hóa đơn thanh toán sau khi lưu.</p>
            </header>

            <c:if test="${not empty loyaltyMessage}">
                <div class="ls-alert ${loyaltyMessageType eq 'success' ? 'success' : 'error'}"><c:out value="${loyaltyMessage}"/></div>
            </c:if>

            <div class="ls-current">
                <div class="ls-pill">
                    <span>Đang áp dụng · Tích điểm</span>
                    <strong><fmt:formatNumber value="${settings.earnSpendAmount}" pattern="#,##0"/> đ = <c:out value="${settings.earnPoints}"/> điểm</strong>
                </div>
                <div class="ls-pill">
                    <span>Đang áp dụng · Dùng điểm</span>
                    <strong>1 điểm = giảm <fmt:formatNumber value="${settings.redeemValuePerPoint}" pattern="#,##0"/> đ</strong>
                </div>
            </div>

            <form class="ls-form" action="${pageContext.request.contextPath}/loyalty-settings" method="post">
                <section class="ls-card">
                    <h2>Tích điểm</h2>
                    <p class="desc">Khách thanh toán bao nhiêu thì được cộng điểm.</p>
                    <div class="ls-field">
                        <label for="earnSpendAmount">Mỗi số tiền thanh toán</label>
                        <div class="ls-input">
                            <input id="earnSpendAmount" name="earnSpendAmount" type="number" min="1" max="1000000000" step="1" required
                                   value="<c:out value='${settings.earnSpendAmount}'/>">
                            <span class="unit">đồng</span>
                        </div>
                    </div>
                    <div class="ls-field">
                        <label for="earnPoints">Sẽ được cộng</label>
                        <div class="ls-input">
                            <input id="earnPoints" name="earnPoints" type="number" min="1" max="100000" step="1" required
                                   value="<c:out value='${settings.earnPoints}'/>">
                            <span class="unit">điểm</span>
                        </div>
                    </div>
                </section>

                <section class="ls-card">
                    <h2>Dùng điểm</h2>
                    <p class="desc">Giá trị giảm giá của mỗi điểm khi khách dùng để trừ vào hóa đơn.</p>
                    <div class="ls-field">
                        <label for="redeemValuePerPoint">Mỗi 1 điểm giảm</label>
                        <div class="ls-input">
                            <input id="redeemValuePerPoint" name="redeemValuePerPoint" type="number" min="1" max="1000000000" step="1" required
                                   value="<c:out value='${settings.redeemValuePerPoint}'/>">
                            <span class="unit">đồng</span>
                        </div>
                    </div>
                </section>

                <div class="ls-actions">
                    <span class="meta"><c:if test="${not empty lastUpdate}">Cập nhật lần cuối: <c:out value="${lastUpdate}"/></c:if></span>
                    <button class="ls-save" type="submit">Lưu quy tắc</button>
                </div>
            </form>
        </div>
    </main>
</body>
</html>
