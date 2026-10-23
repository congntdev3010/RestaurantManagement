<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gọi món - Việt À la carte</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/order-menu.css?v=1.1">
</head>
<body class="qr-order">
    <c:set var="ctx" value="${pageContext.request.contextPath}"/>
    <header class="qr-top">
        <small>Việt À la carte</small>
        <h1>Gọi món</h1>
        <c:if test="${not empty orderTableName}"><span class="qr-table">Bàn: <strong><c:out value="${orderTableName}"/></strong></span></c:if>
    </header>

    <c:if test="${empty orderTableName}">
        <p class="qr-warn">Bạn chưa chọn bàn. Hãy quét mã QR đặt trên bàn để gọi món.</p>
    </c:if>
    <c:if test="${not empty cartMessage}"><p class="qr-flash" role="status"><c:out value="${cartMessage}"/></p></c:if>

    <div class="qr-search">
        <input type="search" id="qrSearchInput" placeholder="Tìm món ăn (gõ để tìm ngay...)" value="<c:out value='${keyword}'/>" autocomplete="off" aria-label="Tìm món ăn">
    </div>

    <nav class="qr-cats" id="qrCats" aria-label="Danh mục món ăn">
        <a href="javascript:void(0)" data-cat="" class="${categoryId == 0 ? 'active' : ''}">Tất cả</a>
        <c:forEach var="cat" items="${categories}">
            <a href="javascript:void(0)" data-cat="${cat.categoryId}" class="${categoryId == cat.categoryId ? 'active' : ''}"><c:out value="${cat.categoryName}"/></a>
        </c:forEach>
    </nav>

    <div class="qr-layout">
    <main class="qr-list" id="qrDishList">
        <p class="qr-empty" id="qrEmptySearch" style="display:none;">Không tìm thấy món phù hợp.</p>
        <c:choose>
            <c:when test="${empty dishes}"><p class="qr-empty">Không tìm thấy món phù hợp.</p></c:when>
            <c:otherwise>
                <c:forEach var="dish" items="${dishes}">
                    <c:set var="qty" value="${empty cartQuantities[dish.dishId] ? 0 : cartQuantities[dish.dishId]}"/>
                    <article class="qr-dish" data-cat="${dish.categoryId}" data-name="<c:out value='${dish.dishName}'/>" data-desc="<c:out value='${dish.description}'/>">
                        <div class="qr-dish-info">
                            <h2><c:out value="${dish.dishName}"/><c:if test="${dish.isFeatured}"><span class="qr-hot">Bán chạy</span></c:if></h2>
                            <p><c:out value="${dish.description}"/></p>
                            <span class="qr-price"><fmt:formatNumber value="${dish.price}" pattern="#,###"/> đ <small>/ <c:out value="${dish.unit}"/></small></span>
                        </div>
                        <div class="qr-dish-media">
                            <c:if test="${not empty dish.image}">
                                <img loading="lazy" alt="<c:out value='${dish.dishName}'/>"
                                     src="${ctx}/${fn:startsWith(dish.image, 'images/') ? 'assets/' : ''}${dish.image}">
                            </c:if>
                            <div class="qr-step">
                                <c:if test="${qty > 0}">
                                    <form action="${ctx}/cart" method="post">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                        <input type="hidden" name="action" value="update">
                                        <input type="hidden" name="returnTo" value="${orderReturnTo}">
                                        <input type="hidden" name="quantity_${dish.dishId}" value="${qty - 1}">
                                        <button type="submit" aria-label="Bớt một ${dish.dishName}">−</button>
                                    </form>
                                    <span><c:out value="${qty}"/></span>
                                </c:if>
                                <form action="${ctx}/cart" method="post">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                    <input type="hidden" name="action" value="add">
                                    <input type="hidden" name="returnTo" value="${orderReturnTo}">
                                    <input type="hidden" name="dishId" value="${dish.dishId}">
                                    <input type="hidden" name="quantity" value="1">
                                    <button type="submit" aria-label="Thêm một ${dish.dishName}">+</button>
                                </form>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </main>

    <aside class="qr-panel" id="cart-panel" aria-label="Các món đã chọn">
        <h2>Món đã chọn <span>(<c:out value="${cartCount}"/>)</span></h2>
        <c:choose>
            <c:when test="${empty cartDishes}"><p class="qr-empty">Chưa chọn món nào.</p></c:when>
            <c:otherwise>
                <ul class="qr-lines">
                    <c:forEach var="d" items="${cartDishes}">
                        <c:set var="q" value="${cartQuantities[d.dishId]}"/>
                        <li>
                            <div class="qr-line-name"><c:out value="${d.dishName}"/><small><fmt:formatNumber value="${d.price * q}" pattern="#,###"/> đ</small></div>
                            <div class="qr-line-right">
                                <div class="qr-step qr-step-inline qr-step-sm">
                                    <form action="${ctx}/cart" method="post">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                        <input type="hidden" name="action" value="update">
                                        <input type="hidden" name="returnTo" value="${orderReturnTo}">
                                        <input type="hidden" name="quantity_${d.dishId}" value="${q - 1}">
                                        <button type="submit" aria-label="Bớt một ${d.dishName}">−</button>
                                    </form>
                                    <span><c:out value="${q}"/></span>
                                    <form action="${ctx}/cart" method="post">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                        <input type="hidden" name="action" value="add">
                                        <input type="hidden" name="returnTo" value="${orderReturnTo}">
                                        <input type="hidden" name="dishId" value="${d.dishId}">
                                        <input type="hidden" name="quantity" value="1">
                                        <button type="submit" aria-label="Thêm một ${d.dishName}">+</button>
                                    </form>
                                </div>
                                <span class="qr-unit-price"><small>Đơn giá: </small><fmt:formatNumber value="${d.price}" pattern="#,###"/> đ</span>
                            </div>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
        <div class="qr-total">Tổng tiền <strong><fmt:formatNumber value="${cartTotal}" pattern="#,###"/> đ</strong></div>
        <c:choose>
            <c:when test="${cartCount > 0 and not empty orderTableName}"><a class="qr-send" href="${ctx}/cart?review=1">Chi tiết order</a></c:when>
            <c:otherwise><span class="qr-send disabled"><c:out value="${empty orderTableName ? 'Quét QR bàn để gửi order' : 'Hãy chọn món'}"/></span></c:otherwise>
        </c:choose>
    </aside>
    </div>

    <%-- Thanh giỏ hàng cố định dưới đáy (chỉ hiện trên điện thoại/máy tính bảng). --%>
    <div class="qr-cartbar">
        <div class="qr-sum"><c:out value="${cartCount}"/> món<strong><fmt:formatNumber value="${cartTotal}" pattern="#,###"/> đ</strong></div>
        <a href="#cart-panel">Xem món đã chọn</a>
    </div>
    <div hidden>
    </div>

    <script>
    (function () {
        var KEY = 'qrOrderScroll';
        // (thanh giỏ hàng dưới đáy nằm trong thẻ .qr-cartbar phía trên script này)
        var KEY_SEARCH = 'qrSearchKey';
        var KEY_CAT = 'qrSearchCat';

        if ('scrollRestoration' in history) history.scrollRestoration = 'manual';
        try {
            var saved = sessionStorage.getItem(KEY);
            if (saved !== null) {
                sessionStorage.removeItem(KEY);
                var y = parseInt(saved, 10) || 0;
                window.scrollTo(0, y);
                window.addEventListener('load', function () { window.scrollTo(0, y); });
            }
        } catch (e) {}

        // Tìm kiếm realtime và lọc danh mục
        var searchInput = document.getElementById('qrSearchInput');
        var dishes = document.querySelectorAll('.qr-dish');
        var emptySearch = document.getElementById('qrEmptySearch');
        var catNav = document.getElementById('qrCats');
        var currentCat = '${categoryId > 0 ? categoryId : ""}';

        function norm(str) {
            if (!str) return '';
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

        function applyFilter() {
            var query = norm(searchInput ? searchInput.value : '');
            var matchedCount = 0;
            dishes.forEach(function (dish) {
                var dishCat = dish.getAttribute('data-cat') || '';
                var dishName = norm(dish.getAttribute('data-name'));
                var dishDesc = norm(dish.getAttribute('data-desc'));

                var catMatch = !currentCat || dishCat === currentCat;
                var queryMatch = !query || dishName.indexOf(query) > -1 || dishDesc.indexOf(query) > -1;

                if (catMatch && queryMatch) {
                    dish.style.display = '';
                    matchedCount++;
                } else {
                    dish.style.display = 'none';
                }
            });

            if (emptySearch) {
                emptySearch.style.display = (matchedCount === 0 && dishes.length > 0) ? 'block' : 'none';
            }
        }

        if (searchInput) {
            ['input', 'keyup', 'change', 'search'].forEach(function (evt) {
                searchInput.addEventListener(evt, function () {
                    try { sessionStorage.setItem(KEY_SEARCH, searchInput.value); } catch(e) {}
                    applyFilter();
                });
            });
            searchInput.addEventListener('keydown', function (e) {
                if (e.key === 'Enter') e.preventDefault();
            });
        }

        if (catNav) {
            catNav.addEventListener('click', function (e) {
                var btn = e.target.closest('a[data-cat]');
                if (!btn) return;
                e.preventDefault();
                catNav.querySelectorAll('a[data-cat]').forEach(function (a) {
                    a.classList.toggle('active', a === btn);
                });
                currentCat = btn.getAttribute('data-cat') || '';
                try { sessionStorage.setItem(KEY_CAT, currentCat); } catch(e) {}
                applyFilter();
            });
        }

        // Khôi phục bộ lọc từ khóa và danh mục sau khi tải lại trang do bấm +/-
        try {
            var savedCat = sessionStorage.getItem(KEY_CAT);
            if (savedCat !== null && catNav) {
                var targetBtn = catNav.querySelector('a[data-cat="' + savedCat + '"]');
                if (targetBtn) {
                    catNav.querySelectorAll('a[data-cat]').forEach(function (a) {
                        a.classList.toggle('active', a === targetBtn);
                    });
                    currentCat = savedCat;
                }
            }
            var savedSearch = sessionStorage.getItem(KEY_SEARCH);
            if (savedSearch && searchInput) {
                searchInput.value = savedSearch;
            }
        } catch (e) {}

        applyFilter();

        document.querySelectorAll('form[action$="/cart"]').forEach(function (f) {
            f.addEventListener('submit', function () {
                try {
                    sessionStorage.setItem(KEY, String(window.pageYOffset || document.documentElement.scrollTop || 0));
                    if (searchInput) sessionStorage.setItem(KEY_SEARCH, searchInput.value || '');
                    sessionStorage.setItem(KEY_CAT, currentCat || '');
                } catch (e) {}
            });
        });
    })();
    </script>
</body>
</html>
