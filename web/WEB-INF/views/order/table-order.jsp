<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Tạo order tại bàn</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/orders.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/table-order.css">
    </head>
    <body>
        <jsp:include page="/WEB-INF/views/fragments/header.jsp"/>

        <main class="orders-page oc-page">
            <a class="button secondary oc-back" href="${pageContext.request.contextPath}/manage-tables">&larr; Quay lại sơ đồ bàn</a>
            <h1>Order - <c:out value="${table.tableName}"/></h1>
            <c:if test="${not empty orderMessage}">
                <p class="notice"><c:out value="${orderMessage}"/></p>
            </c:if>

            <c:choose>
                <c:when test="${not empty pendingOrder}">
                    <p>Bàn đang có order #<c:out value="${pendingOrder.orderId}"/> chờ duyệt. Món mới chọn chỉ được lưu tạm, chưa đưa vào danh sách duyệt.</p>
                </c:when>
                <c:otherwise>
                    <p>Order sẽ ở trạng thái chờ duyệt; bạn xác nhận với bếp rồi duyệt từng món ở bước sau.</p>
                </c:otherwise>
            </c:choose>

            <form id="orderForm" action="${pageContext.request.contextPath}/table-order" method="post" class="order-form">
                <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                <input type="hidden" name="tableId" value="${table.tableId}">

                <div class="form-grid">
                    <label>Tên khách (tùy chọn)<input name="customerName" maxlength="30"></label>
                    <label>Số điện thoại (tùy chọn)<input name="customerPhone" type="tel" inputmode="numeric" maxlength="10" pattern="0[0-9]{9}" title="Số điện thoại gồm 10 chữ số, bắt đầu bằng 0"></label>
                    <label>Ghi chú<input name="note" maxlength="100"></label>
                </div>

                <div class="oc-layout">
                    <div>
                        <h2>Chọn món</h2>
                        <div class="oc-filter"><input type="search" id="ocSearch" maxlength="50" placeholder="Tìm món ăn" autocomplete="off"></div>
                        <div class="oc-cats" id="ocCat">
                            <button type="button" class="active" data-cat="">Tất cả</button>
                            <c:forEach var="cat" items="${categories}">
                                <button type="button" data-cat="${cat.categoryId}"><c:out value="${cat.categoryName}"/></button>
                            </c:forEach>
                        </div>
                        <p class="oc-none" id="ocNone">Không tìm thấy món phù hợp.</p>
                        <div class="dish-choice">
                            <c:forEach var="dish" items="${dishes}">
                                <label class="dish-choice-row" data-cat="${dish.categoryId}" data-search="<c:out value='${dish.dishName}'/>">
                                    <span><c:out value="${dish.dishName}"/> — <fmt:formatNumber value="${dish.price}" pattern="#,###"/> đ / <c:out value="${dish.unit}"/></span>
                                    <input type="number" min="0" max="99" name="quantity_${dish.dishId}"
                                           value="${draftQuantities[dish.dishId]}" placeholder="0"
                                           data-name="<c:out value='${dish.dishName}'/>" data-price="${dish.price}"
                                           aria-label="Số lượng ${dish.dishName}">
                                </label>
                            </c:forEach>
                        </div>
                    </div>

                    <aside class="oc-panel" aria-label="Món đã chọn">
                        <c:set var="approvedTotal" value="0"/>
                        <c:forEach var="ai" items="${approvedItems}">
                            <c:set var="approvedTotal" value="${approvedTotal + ai.lineTotal}"/>
                        </c:forEach>
                        <c:set var="pendingTotal" value="0"/>
                        <c:if test="${not empty pendingOrder}">
                            <c:forEach var="pi" items="${pendingOrder.items}">
                                <c:set var="pendingTotal" value="${pendingTotal + pi.lineTotal}"/>
                            </c:forEach>
                        </c:if>

                        <div class="oc-grand">
                            <span>Tổng cả bàn<small>đã gọi + chờ duyệt + món mới chọn</small></span>
                            <strong id="ocGrand" data-base="${approvedTotal + pendingTotal}"><fmt:formatNumber value="${approvedTotal + pendingTotal}" pattern="#,###"/> đ</strong>
                        </div>

                        <c:if test="${not empty approvedItems}">
                            <h2>Đã gọi (đã duyệt)</h2>
                            <ul class="oc-lines oc-done">
                                <c:forEach var="it" items="${approvedItems}">
                                    <li>
                                        <div>
                                            <c:out value="${it.dishName}"/>
                                            <c:if test="${not empty it.itemNote}"><small class="oc-note-text">Note: <c:out value="${it.itemNote}"/></small></c:if>
                                            </div>
                                            <div class="oc-right">
                                                <strong>x<c:out value="${it.quantity}"/></strong>
                                            <span class="oc-status ${it.itemStatus eq 'SERVED' ? 'is-served' : 'is-cooking'}">${it.itemStatus eq 'SERVED' ? 'Đã phục vụ' : 'Đang nấu'}</span>
                                        </div>
                                    </li>
                                </c:forEach>
                            </ul>
                            <div class="oc-total oc-sub">Tổng đã gọi <strong><fmt:formatNumber value="${approvedTotal}" pattern="#,###"/> đ</strong></div>
                            <hr class="oc-divider">
                        </c:if>

                        <c:if test="${not empty pendingOrder.items}">
                            <h2>Đang chờ duyệt</h2>
                            <ul class="oc-lines oc-done">
                                <c:forEach var="it" items="${pendingOrder.items}">
                                    <li>
                                        <div>
                                            <c:out value="${it.dishName}"/>
                                            <c:if test="${not empty it.itemNote}"><small class="oc-note-text">Note: <c:out value="${it.itemNote}"/></small></c:if>
                                            </div>
                                            <div class="oc-right">
                                                <strong>x<c:out value="${it.requestedQuantity}"/></strong>
                                            <span class="oc-status is-pending">Chờ duyệt</span>
                                        </div>
                                    </li>
                                </c:forEach>
                            </ul>
                            <div class="oc-total oc-sub">Tổng chờ duyệt <strong><fmt:formatNumber value="${pendingTotal}" pattern="#,###"/> đ</strong></div>
                            <hr class="oc-divider">
                        </c:if>

                        <h2>Món mới chọn <span id="ocCount"></span></h2>
                        <ul class="oc-lines" id="ocLines"></ul>
                        <p class="oc-empty" id="ocEmpty">Chưa chọn món nào.</p>
                        <div class="oc-total">Tổng tiền <strong id="ocTotal">0 đ</strong></div>
                        <button type="submit" class="oc-send" id="ocSend" disabled>
                            <c:choose>
                                <c:when test="${not empty pendingOrder}">Lưu món tạm</c:when>
                                <c:otherwise>Gửi order chờ duyệt</c:otherwise>
                            </c:choose>
                        </button>
                    </aside>
                </div>
            </form>
        </main>
        <script>
            (function () {
                var form = document.getElementById('orderForm'), inputs = form.querySelectorAll('input[data-price]'),
                        lines = document.getElementById('ocLines'), empty = document.getElementById('ocEmpty'),
                        totalEl = document.getElementById('ocTotal'), grandEl = document.getElementById('ocGrand'),
                        base = Math.round(parseFloat(grandEl.dataset.base) || 0),
                        countEl = document.getElementById('ocCount'),
                        send = document.getElementById('ocSend');
                var notes = {}, open = {};
                function fmt(n) {
                    return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',') + ' đ';
                }
                function render() {
                    lines.innerHTML = '';
                    var total = 0, count = 0;
                    inputs.forEach(function (i) {
                        var q = parseInt(i.value, 10) || 0;
                        if (q < 1)
                            return;
                        if (q > 99) {
                            q = 99;
                            i.value = 99;
                        }
                        var sub = q * parseFloat(i.dataset.price);
                        total += sub;
                        count += q;
                        var id = i.name.substring(9), li = document.createElement('li'), name = document.createElement('div'), s = document.createElement('small'),
                                qty = document.createElement('strong'),
                                right = document.createElement('div'),
                                btn = document.createElement('button');
                        name.textContent = i.dataset.name;
                        s.textContent = fmt(sub);
                        name.appendChild(s);
                        qty.textContent = 'x' + q;
                        btn.type = 'button';
                        btn.className = 'oc-note-btn' + (notes[id] ? ' has' : '');
                        btn.textContent = 'Note';
                        right.className = 'oc-right';
                        right.appendChild(qty);
                        right.appendChild(btn);
                        li.appendChild(name);
                        li.appendChild(right);
                        var inp = document.createElement('input');
                        inp.type = 'text';
                        inp.name = 'note_' + id;
                        inp.maxLength = 100;
                        inp.className = 'oc-note';
                        inp.value = notes[id] || '';
                        inp.style.display = (open[id] || notes[id]) ? 'block' : 'none';
                        inp.addEventListener('input', function () {
                            notes[id] = inp.value;
                            btn.className = 'oc-note-btn' + (inp.value ? ' has' : '');
                        });
                        inp.addEventListener('keydown', function (e) {
                            if (e.key === 'Enter')
                                e.preventDefault();
                        });
                        btn.addEventListener('click', function () {
                            var show = inp.style.display === 'none';
                            inp.style.display = show ? 'block' : 'none';
                            open[id] = show;
                            if (show)
                                inp.focus();
                        });
                        li.appendChild(inp);
                        lines.appendChild(li);
                    });
                    empty.style.display = count ? 'none' : 'block';
                    totalEl.textContent = fmt(total);
                    grandEl.textContent = fmt(base + Math.round(total));
                    countEl.textContent = count ? '(' + count + ')' : '';
                    send.disabled = !count;
                }
                var rows = form.querySelectorAll('.dish-choice-row'), q = document.getElementById('ocSearch'), cat = {value: ''}, catBox = document.getElementById('ocCat'), none = document.getElementById('ocNone');
                function norm(str) {
                    if (!str)
                        return'';
                    str = ('' + str).toLowerCase();
                    str = str.replace(/à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ/g, "a");
                    str = str.replace(/è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ/g, "e");
                    str = str.replace(/ì|í|ị|ỉ|ĩ/g, "i");
                    str = str.replace(/ò|ó|ọ|ỏ|õ|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ/g, "o");
                    str = str.replace(/ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ/g, "u");
                    str = str.replace(/ỳ|ý|ỵ|ỷ|ỹ/g, "y");
                    str = str.replace(/đ/g, "d");
                    return str.normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();
                }
                function filter() {
                    var k = norm(q.value.trim()), c = cat.value, shown = 0;
                    rows.forEach(function (r) {
                        var ok = (!c || r.dataset.cat === c) && (!k || norm(r.dataset.search).indexOf(k) > -1);
                        r.style.display = ok ? '' : 'none';
                        if (ok)
                            shown++;
                    });
                    none.style.display = shown ? 'none' : 'block';
                }
                q.addEventListener('input', filter);
                catBox.addEventListener('click', function (e) {
                    var b = e.target.closest('button[data-cat]');
                    if (!b)
                        return;
                    catBox.querySelectorAll('button').forEach(function (x) {
                        x.classList.toggle('active', x === b);
                    });
                    cat.value = b.dataset.cat;
                    filter();
                });
                q.addEventListener('keydown', function (e) {
                    if (e.key === 'Enter')
                        e.preventDefault();
                });
                inputs.forEach(function (i) {
                    i.addEventListener('input', render);
                    // Lăn chuột không đổi số lượng: bỏ focus để trang cuộn bình thường (chỉ click mũi tên hoặc gõ số mới đổi).
                    i.addEventListener('wheel', function () {
                        i.blur();
                    }, {passive: true});
                });
                form.addEventListener('submit', function (e) {
                    if (!confirm('Gửi order này để chờ duyệt?'))
                        e.preventDefault();
                });
                render();
            })();
        </script>
    </body>
</html>
