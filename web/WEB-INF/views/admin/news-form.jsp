<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${not empty editingNews ? 'Sửa tin tức' : 'Tạo tin mới'} - Việt À la carte</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/manage-news.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/fragments/header.jsp" />
<main class="manage-page">
    <c:set var="formIsUpdate" value="${not empty editingNews or formAction eq 'update'}"/>
    <c:set var="formItem" value="${not empty formNews ? formNews : editingNews}"/>
    <header class="manage-heading"><div><p class="manage-eyebrow">QUẢN TRỊ HỆ THỐNG</p><h1>${formIsUpdate ? 'Sửa tin tức' : 'Tạo tin mới'}</h1></div><a class="button-secondary" href="${pageContext.request.contextPath}/manage-news">← Quay lại danh sách</a></header>
    <c:if test="${not empty formError}"><p class="manage-message is-error" role="alert"><c:out value="${formError}"/></p></c:if>
    <section class="news-form-section">
        <form class="news-form" action="${pageContext.request.contextPath}/manage-news" method="post">
            <input type="hidden" name="action" value="${formIsUpdate ? 'update' : 'create'}">
            <c:if test="${formIsUpdate}"><input type="hidden" name="newsId" value="${not empty formItem ? formItem.newsId : editingNews.newsId}"></c:if>
            <label>Tiêu đề <span>*</span><input name="title" maxlength="200" required value="<c:out value='${formItem.title}'/>" placeholder="Tiêu đề tin"></label>
            <label>Tóm tắt <span>*</span><textarea name="summary" maxlength="500" rows="3" required placeholder="Tóm tắt ngắn"><c:out value="${formItem.summary}"/></textarea></label>
            <label>Nội dung <span>*</span><textarea name="content" rows="9" required placeholder="Nội dung tin tức"><c:out value="${formItem.content}"/></textarea></label>
            <label>Đường dẫn ảnh<input name="imageUrl" maxlength="1000" value="<c:out value='${formItem.imageUrl}'/>" placeholder="https://... hoặc /assets/images/..."><small>Chỉ URL HTTPS hoặc ảnh trong /assets/images/. Nội dung tin được hiển thị dạng văn bản.</small></label>
            <label>Trạng thái<select name="status" required><option value="DRAFT" ${empty formItem.status or formItem.status eq 'DRAFT' ? 'selected' : ''}>Bản nháp</option><option value="PUBLISHED" ${formItem.status eq 'PUBLISHED' ? 'selected' : ''}>Đã xuất bản</option></select></label>
            <div class="form-actions"><button class="button-primary" type="submit"><c:out value="${formIsUpdate ? 'Lưu thay đổi' : 'Tạo tin mới'}"/></button><a class="button-secondary" href="${pageContext.request.contextPath}/manage-news">Hủy</a></div>
        </form>
    </section>
</main>
</body></html>
