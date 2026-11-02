package controller.menu;

import controller.order.CartServlet;
import dao.CategoryDAO;
import dao.DishDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import model.Category;
import model.Dish;
import model.User;
import utils.DishImageStore;

/** Quản lý thực đơn (chỉ Manager): thêm / sửa / xoá / ẩn-hiện món, có tải ảnh món. */
@WebServlet(name = "ManageDishesServlet", urlPatterns = {"/manage-dishes"})
@MultipartConfig(fileSizeThreshold = 512 * 1024, maxFileSize = 6 * 1024 * 1024, maxRequestSize = 8 * 1024 * 1024)
public class ManageDishesServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (requireManager(request, response) == null) return;
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession();
        CartServlet.ensureToken(session);

        String keyword = trim(request.getParameter("keyword"));
        int categoryId = positiveInt(request.getParameter("categoryId"), 0);
        String status = request.getParameter("status");
        if (!"active".equals(status) && !"hidden".equals(status)) status = "";
        int page = positiveInt(request.getParameter("page"), 1);

        try (DishDAO dao = new DishDAO(); CategoryDAO categoryDAO = new CategoryDAO()) {
            List<Category> categories = categoryDAO.getAllCategories();
            List<Dish> all = dao.getAdminDishes(keyword, categoryId, status);
            int pages = Math.max(1, (int) Math.ceil(all.size() / (double) PAGE_SIZE));
            page = Math.min(page, pages);
            int from = (page - 1) * PAGE_SIZE;
            request.setAttribute("dishes", all.subList(from, Math.min(all.size(), from + PAGE_SIZE)));
            request.setAttribute("totalDishes", all.size());
            request.setAttribute("categories", categories);
            request.setAttribute("keyword", keyword == null ? "" : keyword);
            request.setAttribute("categoryFilter", categoryId);
            request.setAttribute("statusFilter", status);
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", pages);

            String editValue = request.getParameter("edit");
            if (editValue != null) {
                int id = positiveInt(editValue, -1);
                Dish editing = id > 0 ? dao.getDishById(id) : null;
                if (editing == null) flash(session, "Không tìm thấy món cần sửa.", "error");
                else request.setAttribute("editingDish", editing);
            }
            // Dữ liệu form bị lỗi lần trước (để người dùng không phải gõ lại)
            Dish formDish = (Dish) session.getAttribute("dishForm");
            if (formDish != null) {
                request.setAttribute("formDish", formDish);
                request.setAttribute("formAction", session.getAttribute("dishFormAction"));
                request.setAttribute("formError", session.getAttribute("dishFormError"));
                session.removeAttribute("dishForm");
                session.removeAttribute("dishFormAction");
                session.removeAttribute("dishFormError");
            }
            request.setAttribute("manageMessage", session.getAttribute("dishMessage"));
            request.setAttribute("manageMessageType", session.getAttribute("dishMessageType"));
            session.removeAttribute("dishMessage");
            session.removeAttribute("dishMessageType");
            request.getRequestDispatcher("/WEB-INF/views/menu/manage-dishes.jsp").forward(request, response);
        } catch (SQLException ex) {
            getServletContext().log("Could not load dish administration", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải trang quản lý thực đơn.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (requireManager(request, response) == null) return;
        HttpSession session = request.getSession();
        String back = request.getContextPath() + "/manage-dishes";

        Part imagePart;
        try {
            imagePart = request.getContentType() != null && request.getContentType().startsWith("multipart/")
                    ? request.getPart("imageFile") : null;
        } catch (IllegalStateException tooLarge) {
            flash(session, "Ảnh quá lớn (tối đa 5 MB).", "error");
            response.sendRedirect(back);
            return;
        }
        if (!CartServlet.validToken(session, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên làm việc đã hết hạn, vui lòng tải lại trang.");
            return;
        }

        String action = request.getParameter("action");
        String message = "Thao tác không hợp lệ.";
        String type = "error";
        try (DishDAO dao = new DishDAO()) {
            if ("delete".equals(action)) {
                int id = positiveInt(request.getParameter("dishId"), -1);
                Dish old = id > 0 ? dao.getDishById(id) : null;
                DishDAO.DeleteResult result = id > 0 ? dao.deleteDish(id) : DishDAO.DeleteResult.NOT_FOUND;
                if (result == DishDAO.DeleteResult.DELETED) {
                    if (old != null) DishImageStore.deleteIfUploaded(old.getImage());
                    message = "Đã xoá món khỏi thực đơn."; type = "success";
                } else if (result == DishDAO.DeleteResult.HIDDEN_HAS_ORDERS) {
                    message = "Món này đã có trong đơn hàng cũ nên không xoá hẳn được — đã chuyển sang ẨN (khách không còn thấy).";
                    type = "success";
                } else {
                    message = "Không tìm thấy món cần xoá.";
                }
            } else if ("toggleStatus".equals(action)) {
                int id = positiveInt(request.getParameter("dishId"), -1);
                boolean active = "1".equals(request.getParameter("active"));
                if (id > 0 && dao.setDishStatus(id, active)) {
                    message = active ? "Món đã hiển thị trên thực đơn." : "Đã ẩn món khỏi thực đơn.";
                    type = "success";
                } else message = "Không tìm thấy món.";
            } else if ("create".equals(action) || "update".equals(action)) {
                boolean isUpdate = "update".equals(action);
                Dish dish = readForm(request);
                Dish old = null;
                String error = validate(dish, request.getParameter("price"));
                if (error == null && isUpdate) {
                    dish.setDishId(positiveInt(request.getParameter("dishId"), -1));
                    old = dish.getDishId() > 0 ? dao.getDishById(dish.getDishId()) : null;
                    if (old == null) error = "Không tìm thấy món cần cập nhật.";
                    else dish.setImage(old.getImage()); // giữ ảnh cũ nếu không chọn ảnh mới
                }
                if (error == null && isUpdate && "1".equals(request.getParameter("removeImage"))) {
                    dish.setImage(null);
                }
                if (error == null && DishImageStore.hasFile(imagePart)) {
                    try {
                        dish.setImage(DishImageStore.save(imagePart));
                    } catch (IllegalArgumentException badImage) {
                        error = badImage.getMessage();
                    }
                }
                if (error == null && !isUpdate && dish.getImage() == null) {
                    error = "Vui lòng chọn ảnh cho món ăn.";
                }
                if (error == null) {
                    if (isUpdate) {
                        dao.updateDish(dish);
                        if (old.getImage() != null && !old.getImage().equals(dish.getImage())) {
                            DishImageStore.deleteIfUploaded(old.getImage());
                        }
                        message = "Đã cập nhật món \"" + dish.getDishName() + "\".";
                    } else {
                        dao.createDish(dish);
                        message = "Đã thêm món \"" + dish.getDishName() + "\" vào thực đơn.";
                    }
                    type = "success";
                } else {
                    // Ảnh mới vừa lưu nhưng form lỗi ⇒ dọn đi, tránh rác
                    if (old == null || dish.getImage() == null || !dish.getImage().equals(old.getImage())) {
                        if (DishImageStore.hasFile(imagePart)) DishImageStore.deleteIfUploaded(dish.getImage());
                    }
                    dish.setImage(old != null ? old.getImage() : null);
                    session.setAttribute("dishForm", dish);
                    session.setAttribute("dishFormAction", action);
                    session.setAttribute("dishFormError", error);
                    response.sendRedirect(back + (isUpdate && dish.getDishId() > 0 ? "?edit=" + dish.getDishId() : "") + "#dish-form");
                    return;
                }
            }
        } catch (SQLException ex) {
            getServletContext().log("Could not save dish administration change", ex);
            message = "Có lỗi khi lưu dữ liệu. Vui lòng thử lại.";
        }
        flash(session, message, type);
        response.sendRedirect(back);
    }

    private User requireManager(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) { response.sendRedirect(request.getContextPath() + "/login"); return null; }
        if (!"Manager".equalsIgnoreCase(user.getRoleName())) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return null; }
        return user;
    }

    private Dish readForm(HttpServletRequest request) {
        Dish d = new Dish();
        d.setDishName(trim(request.getParameter("dishName")));
        d.setDescription(trim(request.getParameter("description")));
        d.setUnit(trim(request.getParameter("unit")));
        d.setCategoryId(positiveInt(request.getParameter("categoryId"), 0));
        d.setIsFeatured("1".equals(request.getParameter("isFeatured")));
        d.setStatus(!"0".equals(request.getParameter("status")));
        try {
            String raw = request.getParameter("price");
            d.setPrice(raw == null ? null : new BigDecimal(raw.replace(".", "").replace(",", "").trim()));
        } catch (NumberFormatException ex) {
            d.setPrice(null);
        }
        return d;
    }

    private String validate(Dish d, String rawPrice) {
        if (d.getDishName() == null || d.getDishName().isEmpty()) return "Tên món là bắt buộc.";
        if (d.getDishName().length() > 150) return "Tên món không được quá 150 ký tự.";
        if (d.getCategoryId() <= 0) return "Vui lòng chọn danh mục.";
        if (d.getPrice() == null) return "Giá không hợp lệ (chỉ nhập số, VD: 145000).";
        if (d.getPrice().signum() <= 0) return "Giá phải lớn hơn 0.";
        if (d.getPrice().compareTo(new BigDecimal("999999999")) > 0) return "Giá quá lớn.";
        if (d.getUnit() == null || d.getUnit().isEmpty()) d.setUnit("Phần");
        if (d.getUnit().length() > 50) return "Đơn vị tính không được quá 50 ký tự.";
        if (d.getDescription() != null && d.getDescription().length() > 2000) return "Mô tả không được quá 2000 ký tự.";
        return null;
    }

    private int positiveInt(String value, int fallback) {
        try { int parsed = Integer.parseInt(value); return parsed > 0 ? parsed : fallback; }
        catch (NumberFormatException ex) { return fallback; }
    }

    private String trim(String value) { return value == null ? null : value.trim(); }

    private void flash(HttpSession session, String message, String type) {
        session.setAttribute("dishMessage", message);
        session.setAttribute("dishMessageType", type);
    }
}
