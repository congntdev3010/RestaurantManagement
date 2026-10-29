<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="formIsUpdate" value="${not empty editingDish or formAction eq 'update'}"/>
<c:set var="formItem" value="${not empty formDish ? formDish : editingDish}"/>
<c:set var="openForm" value="${formIsUpdate or not empty formError}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý thực đơn - Việt À la carte</title>
    <link rel="stylesheet" href="${ctx}/assets/css/manage-news.css">
    <link rel="stylesheet" href="${ctx}/assets/css/manage-dishes.css?v=1.1">
</head>
<body>
<jsp:include page="/WEB-INF/views/fragments/header.jsp" />
<main class="manage-page">
    <header class="manage-heading">
        <div><p class="manage-eyebrow">QUẢN TRỊ NHÀ HÀNG</p><h1>Quản lý thực đơn</h1></div>
        <a class="button-primary" href="#dish-form" onclick="document.getElementById('dish-form').open = true">+ Thêm món mới</a>
    </header>

    <c:if test="${not empty manageMessage}">
        <p class="manage-message ${manageMessageType eq 'success' ? 'is-success' : 'is-error'}" role="status"><c:out value="${manageMessage}"/></p>
    </c:if>

    <!-- ===== FORM THÊM / SỬA ===== -->
    <details class="news-list-section dish-form-section" id="dish-form" ${openForm ? 'open' : ''}>
        <summary class="section-heading">
            <h2>${formIsUpdate ? 'Sửa món' : 'Thêm món mới'}<c:if test="${formIsUpdate}">: <c:out value="${formItem.dishName}"/></c:if></h2>
            <c:if test="${formIsUpdate}"><a href="${ctx}/manage-dishes">Thoát chỉnh sửa</a></c:if>
        </summary>
        <c:if test="${not empty formError}"><p class="manage-message is-error" role="alert"><c:out value="${formError}"/></p></c:if>

        <form class="news-form dish-form" action="${ctx}/manage-dishes" method="post" enctype="multipart/form-data">
            <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
            <input type="hidden" name="action" value="${formIsUpdate ? 'update' : 'create'}">
            <c:if test="${formIsUpdate}"><input type="hidden" name="dishId" value="${formItem.dishId}"></c:if>

            <div class="dish-form-grid">
                <div class="dish-form-fields">
                    <label>Tên món <span>*</span>
                        <input name="dishName" maxlength="150" required value="<c:out value='${formItem.dishName}'/>" placeholder="VD: Cánh gà chiên nước mắm">
                    </label>
                    <div class="field-row">
                        <label>Danh mục <span>*</span>
                            <select name="categoryId" required>
                                <option value="">-- Chọn danh mục --</option>
                                <c:forEach var="cat" items="${categories}">
                                    <option value="${cat.categoryId}" ${formItem.categoryId eq cat.categoryId ? 'selected' : ''}><c:out value="${cat.categoryName}"/></option>
                                </c:forEach>
                            </select>
                        </label>
                        <label>Giá (VNĐ) <span>*</span>
                            <input name="price" type="number" min="1000" step="1000" required
                                   value="${not empty formItem.price ? formItem.price.toPlainString() : ''}" placeholder="145000">
                        </label>
                        <label>Đơn vị tính
                            <input name="unit" maxlength="50" list="unit-options" value="<c:out value='${empty formItem.unit ? "Phần" : formItem.unit}'/>">
                            <datalist id="unit-options"><option value="Phần"><option value="Đĩa"><option value="Tô"><option value="Nồi"><option value="Ly"><option value="Chén"></datalist>
                        </label>
                    </div>
                    <label>Mô tả
                        <textarea name="description" rows="4" maxlength="2000" placeholder="Nguyên liệu, hương vị..."><c:out value="${formItem.description}"/></textarea>
                    </label>
                    <div class="field-row checks">
                        <label class="check"><input type="checkbox" name="isFeatured" value="1" ${formItem.isFeatured ? 'checked' : ''}> Món bán chạy (gắn nhãn nổi bật)</label>
                        <label class="check"><input type="checkbox" name="status" value="1" ${empty formItem or formItem.status ? 'checked' : ''}
                               onchange="document.getElementById('status-off').disabled = this.checked"> Hiển thị trên thực đơn</label>
                        <input type="hidden" id="status-off" name="status" value="0" ${empty formItem or formItem.status ? 'disabled' : ''}>
                    </div>
                </div>

                <div class="dish-form-image">
                    <span class="image-label">Ảnh món <c:if test="${not formIsUpdate}"><span>*</span></c:if></span>
                    <c:set var="img" value="${formItem.image}"/>
                    <c:set var="imgSrc" value="${empty img ? '' : (fn:startsWith(img, 'images/') ? ctx.concat('/assets/').concat(img) : ctx.concat('/').concat(img))}"/>
                    <label class="image-drop" for="imageFile">
                        <img id="image-preview" src="${imgSrc}" alt="" ${empty imgSrc ? 'hidden' : ''}>
                        <span id="image-placeholder" ${empty imgSrc ? '' : 'hidden'}>
                            <strong>Bấm để chọn ảnh</strong><small>JPG, PNG, WEBP, GIF · tối đa 5 MB</small>
                        </span>
                    </label>
                    <input id="imageFile" name="imageFile" type="file" accept="image/jpeg,image/png,image/webp,image/gif" ${formIsUpdate ? '' : 'required'}>
                    <c:if test="${formIsUpdate and not empty img}">
                        <label class="check small"><input type="checkbox" name="removeImage" value="1"> Xoá ảnh hiện tại</label>
                    </c:if>
                    <small id="image-error" class="image-error" hidden></small>
                </div>
            </div>

            <div class="form-actions">
                <button class="button-primary" type="submit">${formIsUpdate ? 'Lưu thay đổi' : 'Thêm món'}</button>
                <a class="button-secondary" href="${ctx}/manage-dishes">Huỷ</a>
            </div>
        </form>
    </details>

    <!-- ===== DANH SÁCH ===== -->
    <section class="news-list-section">
        <div class="section-heading"><h2>Danh sách món</h2><span><c:out value="${totalDishes}"/> món</span></div>
        <form class="news-filters" action="${ctx}/manage-dishes" method="get">
            <input type="search" name="keyword" value="<c:out value='${keyword}'/>" placeholder="Tìm tên món (có dấu hoặc không dấu)" aria-label="Tìm tên món">
            <select name="categoryId" aria-label="Lọc danh mục">
                <option value="0">Tất cả danh mục</option>
                <c:forEach var="cat" items="${categories}">
                    <option value="${cat.categoryId}" ${categoryFilter eq cat.categoryId ? 'selected' : ''}><c:out value="${cat.categoryName}"/></option>
                </c:forEach>
            </select>
            <select name="status" aria-label="Lọc trạng thái">
                <option value="">Tất cả trạng thái</option>
                <option value="active" ${statusFilter eq 'active' ? 'selected' : ''}>Đang bán</option>
                <option value="hidden" ${statusFilter eq 'hidden' ? 'selected' : ''}>Đang ẩn</option>
            </select>
            <button class="button-primary" type="submit">Lọc</button>
        </form>

        <div class="table-wrap"><table>
            <thead><tr><th>Ảnh</th><th>Tên món</th><th>Danh mục</th><th>Giá</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
            <tbody>
            <c:choose>
                <c:when test="${empty dishes}"><tr><td colspan="6" class="empty-row">Không có món nào phù hợp.</td></tr></c:when>
                <c:otherwise>
                    <c:forEach var="d" items="${dishes}">
                        <c:set var="thumb" value="${empty d.image ? '' : (fn:startsWith(d.image, 'images/') ? ctx.concat('/assets/').concat(d.image) : ctx.concat('/').concat(d.image))}"/>
                        <tr class="${d.status ? '' : 'is-hidden'}">
                            <td><c:choose><c:when test="${not empty thumb}"><img class="dish-thumb" src="${thumb}" alt="" loading="lazy"></c:when><c:otherwise><span class="dish-thumb empty">—</span></c:otherwise></c:choose></td>
                            <td class="title-cell"><strong><c:out value="${d.dishName}"/><c:if test="${d.isFeatured}"> <span class="featured-tag">Bán chạy</span></c:if></strong><small><c:out value="${d.description}"/></small></td>
                            <td><c:out value="${d.categoryName}"/></td>
                            <td class="price-cell"><fmt:formatNumber value="${d.price}" pattern="#,###"/> đ<small>/ <c:out value="${d.unit}"/></small></td>
                            <td><span class="status-pill ${d.status ? 'published' : 'draft'}">${d.status ? 'Đang bán' : 'Đang ẩn'}</span></td>
                            <td><div class="row-actions">
                                <a class="button-small" href="${ctx}/manage-dishes?edit=${d.dishId}#dish-form">Sửa</a>
                                <form method="post" action="${ctx}/manage-dishes">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                    <input type="hidden" name="action" value="toggleStatus">
                                    <input type="hidden" name="dishId" value="${d.dishId}">
                                    <input type="hidden" name="active" value="${d.status ? '0' : '1'}">
                                    <button class="button-small" type="submit">${d.status ? 'Ẩn' : 'Hiện'}</button>
                                </form>
                                <form method="post" action="${ctx}/manage-dishes" onsubmit="return confirm('Xoá món này khỏi thực đơn?');">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="dishId" value="${d.dishId}">
                                    <button class="button-small danger" type="submit">Xoá</button>
                                </form>
                            </div></td>
                        </tr>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
            </tbody>
        </table></div>

        <c:if test="${totalPages gt 1}">
            <nav class="pagination" aria-label="Phân trang">
                <c:forEach begin="1" end="${totalPages}" var="p">
                    <c:url var="pageUrl" value="/manage-dishes">
                        <c:param name="page" value="${p}"/>
                        <c:if test="${not empty keyword}"><c:param name="keyword" value="${keyword}"/></c:if>
                        <c:if test="${categoryFilter gt 0}"><c:param name="categoryId" value="${categoryFilter}"/></c:if>
                        <c:if test="${not empty statusFilter}"><c:param name="status" value="${statusFilter}"/></c:if>
                    </c:url>
                    <a class="${p eq currentPage ? 'active' : ''}" href="${pageUrl}"><c:out value="${p}"/></a>
                </c:forEach>
            </nav>
        </c:if>
    </section>
</main>

<script>
    // Xem trước ảnh + kiểm tra kích thước ngay trên trình duyệt
    (function () {
        var input = document.getElementById('imageFile');
        var preview = document.getElementById('image-preview');
        var placeholder = document.getElementById('image-placeholder');
        var error = document.getElementById('image-error');
        if (!input) return;
        input.addEventListener('change', function () {
            error.hidden = true;
            var file = input.files && input.files[0];
            if (!file) return;
            if (file.size > 5 * 1024 * 1024) {
                error.textContent = 'Ảnh lớn hơn 5 MB, vui lòng chọn ảnh khác.';
                error.hidden = false;
                input.value = '';
                return;
            }
            preview.src = URL.createObjectURL(file);
            preview.hidden = false;
            placeholder.hidden = true;
        });
    })();
</script>
</body></html>
