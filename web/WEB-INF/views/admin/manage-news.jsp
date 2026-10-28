<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý tin tức - Việt À la carte</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/manage-news.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/fragments/header.jsp" />
<main class="manage-page">
    <header class="manage-heading"><div><p class="manage-eyebrow">QUẢN TRỊ HỆ THỐNG</p><h1>Quản lý tin tức</h1></div><div class="manage-heading-actions"><p><c:out value="${fn:length(newsList)}"/> tin trên trang</p><a class="button-primary" href="${pageContext.request.contextPath}/manage-news/new">+ Tạo tin mới</a></div></header>
    <c:if test="${not empty manageMessage}"><p class="manage-message ${manageMessageType eq 'success' ? 'is-success' : 'is-error'}" role="status"><c:out value="${manageMessage}"/></p></c:if>

    <section class="news-list-section">
        <div class="section-heading"><h2>Danh sách tin</h2><span><c:out value="${fn:length(newsList)}"/> kết quả</span></div>
        <form class="news-filters" action="${pageContext.request.contextPath}/manage-news" method="get">
            <input type="search" name="keyword" value="<c:out value='${keyword}'/>" placeholder="Tìm theo tiêu đề" aria-label="Tìm theo tiêu đề">
            <select name="status" aria-label="Lọc trạng thái"><option value="">Tất cả trạng thái</option><option value="DRAFT" ${statusFilter eq 'DRAFT' ? 'selected' : ''}>Bản nháp</option><option value="PUBLISHED" ${statusFilter eq 'PUBLISHED' ? 'selected' : ''}>Đã xuất bản</option></select>
            <button class="button-primary" type="submit">Lọc</button>
        </form>
        <div class="table-wrap"><table><thead><tr><th>ID</th><th>Tiêu đề</th><th>Trạng thái</th><th>Người tạo</th><th>Cập nhật</th><th>Thao tác</th></tr></thead>
            <tbody><c:choose><c:when test="${empty newsList}"><tr><td colspan="6" class="empty-row">Chưa có tin tức.</td></tr></c:when><c:otherwise><c:forEach var="item" items="${newsList}"><tr>
                <td><c:out value="${item.newsId}"/></td><td class="title-cell"><strong><c:out value="${item.title}"/></strong><small><c:out value="${item.summary}"/></small></td>
                <td><span class="status-pill ${item.status eq 'PUBLISHED' ? 'published' : 'draft'}"><c:out value="${item.status eq 'PUBLISHED' ? 'Đã xuất bản' : 'Bản nháp'}"/></span></td>
                <td><c:out value="${item.createdByName}"/></td><td><c:out value="${item.updatedAt}"/></td>
                <td><div class="row-actions"><a class="button-small" href="${pageContext.request.contextPath}/manage-news/new?edit=${item.newsId}">Sửa</a>
                    <form method="post" action="${pageContext.request.contextPath}/manage-news"><input type="hidden" name="action" value="toggleStatus"><input type="hidden" name="newsId" value="${item.newsId}"><input type="hidden" name="targetStatus" value="${item.status eq 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'}"><button class="button-small" type="submit"><c:out value="${item.status eq 'PUBLISHED' ? 'Ẩn' : 'Xuất bản'}"/></button></form>
                    <form method="post" action="${pageContext.request.contextPath}/manage-news" onsubmit="return confirm('Xóa tin này?');"><input type="hidden" name="action" value="delete"><input type="hidden" name="newsId" value="${item.newsId}"><button class="button-small danger" type="submit">Xóa</button></form>
                </div></td>
            </tr></c:forEach></c:otherwise></c:choose></tbody></table></div>
        <c:if test="${totalPages gt 1}"><nav class="pagination" aria-label="Phân trang"><c:forEach begin="1" end="${totalPages}" var="p"><c:url var="pageUrl" value="/manage-news"><c:param name="page" value="${p}"/><c:if test="${not empty keyword}"><c:param name="keyword" value="${keyword}"/></c:if><c:if test="${not empty statusFilter}"><c:param name="status" value="${statusFilter}"/></c:if></c:url><a class="${p eq currentPage ? 'active' : ''}" href="${pageUrl}"><c:out value="${p}"/></a></c:forEach></nav></c:if>
    </section>
</main>
</body></html>
