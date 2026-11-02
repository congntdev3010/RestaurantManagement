<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Chi tiết order</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/orders.css?v=1.2">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/order-review.css">
    </head>
    <body>
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
                            <col class="rv-c-name">
                            <col class="rv-c-qty">
                            <col class="rv-c-price">
                            <col class="rv-c-note">
                            <col class="rv-c-sum">
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
                                    <td class="rv-name"><c:out value="${dish.dishName}"/></td>
                                    <td class="rv-qty" data-label="Số lượng"><c:out value="${cartQuantities[dish.dishId]}"/></td>
                                    <td class="rv-price" data-label="Đơn giá"><fmt:formatNumber value="${dish.price}" pattern="#,###"/> đ</td>
                                    <td class="rv-notecell"><input type="text" class="rv-note" name="note_${dish.dishId}" maxlength="100" placeholder="Ghi chú (ít cay, không hành...)"></td>
                                    <td class="rv-sum" data-label="Tạm tính"><fmt:formatNumber value="${dish.price * cartQuantities[dish.dishId]}" pattern="#,###"/> đ</td>
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
        <script>
            (function () {
                var storageKey = 'qrOrderItemNotes';
                var notes = {};
                try {
                    notes = JSON.parse(sessionStorage.getItem(storageKey) || '{}');
                } catch (e) {
                    notes = {};
                }
                document.querySelectorAll('.rv-note').forEach(function (input) {
                    var dishId = input.name.substring(5);
                    if (notes[dishId])
                        input.value = notes[dishId];
                    input.addEventListener('input', function () {
                        notes[dishId] = input.value;
                        try {
                            sessionStorage.setItem(storageKey, JSON.stringify(notes));
                        } catch (e) {
                        }
                    });
                });
                document.querySelector('.checkout-form').addEventListener('submit', function () {
                    try {
                        sessionStorage.removeItem(storageKey);
                    } catch (e) {
                    }
                });
            })();
        </script>
    </body>
</html>
