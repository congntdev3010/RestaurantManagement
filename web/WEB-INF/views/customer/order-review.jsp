<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Chi tiết order</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/orders.css?v=1.1">
    <style>
        .orders-page {
            max-width: 860px;
            margin: 28px auto;
            padding: 0 16px 40px;
        }
        .review-top-meta {
            display: flex;
            align-items: center;
            gap: 14px;
            flex-wrap: wrap;
            background: #fff;
            padding: 10px 16px;
            border-radius: 8px;
            border: 1px solid #e8dfd8;
            margin: 12px 0 16px;
            font-size: 0.95rem;
            color: #421217;
        }
        .review-top-meta .sep {
            color: #d1c7be;
        }
        .rv-table {
            table-layout: fixed;
            width: 100%;
        }
        .rv-table th, .rv-table td {
            padding: 10px 8px;
            vertical-align: middle;
        }
        .rv-note {
            width: 100%;
            box-sizing: border-box;
            padding: 8px 10px;
            border: 1px solid #ddd;
            border-radius: 8px;
            font-size: .95rem;
        }
        .rv-table td.rv-qty, .rv-table th.rv-qty {
            text-align: center;
        }
        .rv-table td.rv-price, .rv-table th.rv-price,
        .rv-table td.rv-sum, .rv-table th.rv-sum {
            text-align: right;
            white-space: nowrap;
        }
        .table-responsive {
            width: 100%;
            overflow-x: auto;
            -webkit-overflow-scrolling: touch;
        }
        .review-total-bar {
            display: flex;
            justify-content: flex-end;
            align-items: baseline;
            gap: 10px;
            padding: 10px 4px 0;
            font-size: 1.05rem;
            color: #421217;
            margin: 0;
        }
        .review-total-bar .total-amount {
            font-size: 1.35rem;
            font-weight: 700;
            color: #7a1f1f;
        }
        .review-confirm-box {
            background: #fff;
            border: 1px solid #e8dfd8;
            border-radius: 10px;
            padding: 14px 16px;
            margin-top: 10px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.03);
        }
        .review-notice {
            margin: 0 0 10px;
            font-size: 0.85rem;
            color: #6a6259;
            line-height: 1.45;
        }
        .review-action-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            flex-wrap: wrap;
            padding-top: 10px;
            border-top: 1px dashed #e8dfd8;
        }
        .review-question {
            font-weight: 700;
            color: #421217;
            font-size: 0.95rem;
            margin: 0;
        }
        .review-buttons {
            display: flex;
            align-items: center;
            gap: 10px;
            flex-wrap: wrap;
        }
        .review-buttons button,
        .review-buttons .button {
            margin: 0 !important;
            padding: 9px 16px;
            font-size: 0.92rem;
            white-space: nowrap;
        }
        @media (max-width: 600px) {
            .review-action-row {
                flex-direction: column;
                align-items: stretch;
                gap: 10px;
            }
            .review-buttons {
                display: flex;
                flex-direction: column-reverse;
                gap: 8px;
            }
            .review-buttons button,
            .review-buttons .button {
                width: 100%;
                text-align: center;
            }
        }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragments/header.jsp"/>
<main class="orders-page">
    <h1>Chi tiết order</h1>

    <div class="review-top-meta">
        <span>Bàn: <strong><c:out value="${orderTableName}"/></strong></span>
        <span class="sep">•</span>
        <span>Khách: <strong><c:out value="${sessionScope.orderGuestName}"/></strong> · <c:out value="${sessionScope.orderGuestPhone}"/></span>
    </div>

    <form action="${pageContext.request.contextPath}/my-orders" method="post" class="checkout-form">
        <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
        <input type="hidden" name="action" value="checkout">

        <div class="table-responsive">
            <table class="rv-table">
                <colgroup>
                    <col style="width:28%">
                    <col style="width:10%">
                    <col style="width:18%">
                    <col style="width:26%">
                    <col style="width:18%">
                </colgroup>
                <thead>
                    <tr>
                        <th>Món</th>
                        <th class="rv-qty">Số lượng</th>
                        <th class="rv-price">Đơn giá</th>
                        <th>Ghi chú</th>
                        <th class="rv-sum">Tạm tính</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="dish" items="${cartDishes}">
                    <tr>
                        <td><c:out value="${dish.dishName}"/></td>
                        <td class="rv-qty"><c:out value="${cartQuantities[dish.dishId]}"/></td>
                        <td class="rv-price"><fmt:formatNumber value="${dish.price}" pattern="#,###"/> đ</td>
                        <td><input type="text" class="rv-note" name="note_${dish.dishId}" maxlength="255"></td>
                        <td class="rv-sum"><fmt:formatNumber value="${dish.price * cartQuantities[dish.dishId]}" pattern="#,###"/> đ</td>
                    </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>

        <div class="review-total-bar">
            <span>Tạm tính:</span>
            <span class="total-amount"><fmt:formatNumber value="${cartTotal}" pattern="#,###"/> đ</span>
        </div>

        <div class="review-confirm-box">
            <p class="review-notice">
                Order sau khi gửi sẽ chờ nhân viên duyệt. Món nào hết sẽ bị bỏ hoặc giảm số lượng. Muốn gọi thêm món, bạn tạo một order mới.
            </p>
            <div class="review-action-row">
                <p class="review-question">Bạn đã chắc chắn gửi order này chưa?</p>
                <div class="review-buttons">
                    <a class="button secondary" href="${pageContext.request.contextPath}/order">Quay lại chỉnh sửa</a>
                    <button type="submit">Chắc chắn, gửi order</button>
                </div>
            </div>
        </div>
    </form>
</main>
</body>
</html>
