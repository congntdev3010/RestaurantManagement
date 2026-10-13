package controller;

import dao.UserDAO;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "ManageUsersServlet", urlPatterns = {"/manage-users"})
public class ManageUsersServlet extends HttpServlet {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");

    // Hiển thị danh sách, bộ lọc và form thêm/sửa tài khoản.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        User actor = requireManagerOrAdmin(request, response);
        if (actor == null) {
            return;
        }

        boolean isAdmin = isAdmin(actor);
        UserDAO dao = new UserDAO();
        Map<Integer, String> roles = dao.getManageableRoles(isAdmin);
        int roleFilterId = parseInt(request.getParameter("roleId"), 0);
        if (!roles.containsKey(roleFilterId)) {
            roleFilterId = 0;
        }
        String nameQuery = trim(request.getParameter("name"));
        if (nameQuery == null) {
            nameQuery = "";
        }
        request.setAttribute("users", dao.getManageableUsers(isAdmin, roleFilterId, nameQuery));
        request.setAttribute("roles", roles);
        request.setAttribute("roleFilterId", roleFilterId);
        request.setAttribute("nameQuery", nameQuery);

        // Khi chọn sửa, tải tài khoản để đưa dữ liệu lên form.
        String editId = request.getParameter("edit");
        if (editId != null) {
            User editUser = dao.getManageableUserById(parseInt(editId, -1), isAdmin);
            if (editUser != null) {
                request.setAttribute("editUser", editUser);
            } else {
                setFlash(request.getSession(), "Không tìm thấy tài khoản trong phạm vi được quản lý.", "error");
            }
        }

        HttpSession session = request.getSession();
        request.setAttribute("manageMessage", session.getAttribute("manageMessage"));
        request.setAttribute("manageMessageType", session.getAttribute("manageMessageType"));
        session.removeAttribute("manageMessage");
        session.removeAttribute("manageMessageType");
        request.getRequestDispatcher("/manage-users.jsp").forward(request, response);
    }

    // Thực hiện thao tác được gửi từ form quản lý tài khoản.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User actor = requireManagerOrAdmin(request, response);
        if (actor == null) {
            return;
        }

        boolean isAdmin = isAdmin(actor);
        UserDAO dao = new UserDAO();
        String action = request.getParameter("action");
        int roleFilterId = parseInt(request.getParameter("filterRoleId"), 0);
        String nameQuery = trim(request.getParameter("filterName"));
        String message;
        String messageType = "error";

        if ("create".equals(action)) {
            // Tạo mới cần mật khẩu; dữ liệu được kiểm tra trước khi gọi DAO.
            User user = readFormUser(request);
            message = validateUser(user, true);
            if (message == null) {
                if (dao.createManagedUser(user, isAdmin)) {
                    message = "Đã tạo tài khoản.";
                    messageType = "success";
                } else {
                    message = "Không thể tạo tài khoản. Email hoặc tên đăng nhập có thể đã tồn tại.";
                }
            }
        } else if ("update".equals(action)) {
            // Khi sửa, mật khẩu để trống nghĩa là giữ nguyên mật khẩu cũ.
            User user = readFormUser(request);
            user.setUserId(parseInt(request.getParameter("userId"), -1));
            message = validateUser(user, false);
            String password = user.getPassword();
            if (message == null && password != null && !password.isEmpty()
                    && (password.length() < 8 || password.length() > 255)) {
                message = "Mật khẩu mới phải có từ 8 đến 255 ký tự.";
            }
            if (message == null) {
                if (dao.updateManagedUser(user, isAdmin)) {
                    message = "Đã cập nhật tài khoản.";
                    messageType = "success";
                } else {
                    message = "Không thể cập nhật tài khoản. Hãy kiểm tra role và email/tên đăng nhập.";
                }
            }
        } else if ("delete".equals(action)) {
            // DAO ngăn xóa tài khoản đang đăng nhập và tài khoản ngoài phạm vi quyền.
            int userId = parseInt(request.getParameter("userId"), -1);
            if (dao.deleteManagedUser(userId, actor.getUserId(), isAdmin)) {
                message = "Đã xóa tài khoản.";
                messageType = "success";
            } else {
                message = "Không thể xóa tài khoản này; có thể đây là tài khoản đang đăng nhập hoặc không thuộc phạm vi quản lý.";
            }
        } else if ("toggle".equals(action)) {
            // Đổi trạng thái hoạt động: đang mở thì khóa, đang khóa thì mở lại.
            int userId = parseInt(request.getParameter("userId"), -1);
            User target = dao.getManageableUserById(userId, isAdmin);
            if (target != null && dao.setManagedUserActive(
                    userId, actor.getUserId(), !target.isIsActive(), isAdmin)) {
                message = target.isIsActive() ? "Đã khóa tài khoản." : "Đã kích hoạt tài khoản.";
                messageType = "success";
            } else {
                message = "Không thể thay đổi trạng thái tài khoản này.";
            }
        } else {
            message = "Thao tác không hợp lệ.";
        }

        // Dùng redirect để tải lại danh sách và chỉ hiển thị thông báo một lần.
        setFlash(request.getSession(), message, messageType);
        String redirect = request.getContextPath() + "/manage-users";
        if (roleFilterId > 0 || (nameQuery != null && !nameQuery.isEmpty())) {
            redirect += "?roleId=" + roleFilterId;
            if (nameQuery != null && !nameQuery.isEmpty()) {
                redirect += "&name=" + java.net.URLEncoder.encode(nameQuery, java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        response.sendRedirect(redirect);
    }

    // Kiểm tra đăng nhập và quyền trước khi cho phép quản lý tài khoản.
    private User requireManagerOrAdmin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Chỉ Admin và Manager được vào chức năng quản lý tài khoản.
        HttpSession session = request.getSession(false);
        Object sessionUser = session == null ? null : session.getAttribute("user");
        if (!(sessionUser instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        User actor = (User) sessionUser;
        if (!isAdmin(actor) && !isManager(actor)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return actor;
    }

    // Trả về true nếu tài khoản hiện tại có vai trò Admin.
    private boolean isAdmin(User user) {
        return user.getRoleName() != null && "Admin".equalsIgnoreCase(user.getRoleName());
    }

    // Trả về true nếu tài khoản hiện tại có vai trò Manager.
    private boolean isManager(User user) {
        return user.getRoleName() != null && "Manager".equalsIgnoreCase(user.getRoleName());
    }

    // Đọc các trường trong form và chuyển thành đối tượng User.
    private User readFormUser(HttpServletRequest request) {
        // Gom dữ liệu form thành User để kiểm tra và chuyển xuống DAO.
        User user = new User();
        user.setUsername(trim(request.getParameter("username")));
        user.setEmail(trim(request.getParameter("email")));
        user.setFullName(trim(request.getParameter("fullName")));
        user.setPhone(trim(request.getParameter("phone")));
        user.setPassword(request.getParameter("password"));
        user.setRoleId(parseInt(request.getParameter("roleId"), -1));
        user.setIsActive(true);
        return user;
    }

    // Kiểm tra dữ liệu bắt buộc trước khi thêm hoặc sửa tài khoản.
    private String validateUser(User user, boolean requirePassword) {
        // Kiểm tra các trường bắt buộc trước khi thêm hoặc cập nhật tài khoản.
        if (user.getUsername() == null || user.getUsername().isEmpty() || user.getUsername().length() > 50) {
            return "Tên đăng nhập là bắt buộc và tối đa 50 ký tự.";
        }
        if (user.getEmail() == null || user.getEmail().length() > 100
                || !EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            return "Email không hợp lệ.";
        }
        if (user.getFullName() == null || user.getFullName().isEmpty() || user.getFullName().length() > 100) {
            return "Họ và tên là bắt buộc và tối đa 100 ký tự.";
        }
        if (user.getPhone() == null || !PHONE_PATTERN.matcher(user.getPhone()).matches()) {
            return "Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số.";
        }
        if (user.getRoleId() < 1) {
            return "Vui lòng chọn vai trò hợp lệ.";
        }
        if (requirePassword && (user.getPassword() == null
                || user.getPassword().length() < 8 || user.getPassword().length() > 255)) {
            return "Mật khẩu phải có từ 8 đến 255 ký tự.";
        }
        return null;
    }

    // Chuyển chuỗi sang số; trả về giá trị mặc định nếu dữ liệu không hợp lệ.
    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // Xóa khoảng trắng đầu và cuối, đồng thời xử lý giá trị null.
    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    // Lưu thông báo vào session để hiển thị sau khi chuyển hướng.
    private void setFlash(HttpSession session, String message, String messageType) {
        session.setAttribute("manageMessage", message);
        session.setAttribute("manageMessageType", messageType);
    }
}
