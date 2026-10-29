package controller.admin;

import dao.UserDAO;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    /** Hiển thị danh sách tài khoản và nạp dữ liệu cho các bộ lọc/form. */
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
        int archivedRoleFilterId = parseInt(request.getParameter("archivedRoleId"), 0);
        if (!roles.containsKey(archivedRoleFilterId)) {
            archivedRoleFilterId = 0;
        }
        String nameQuery = limit(trim(request.getParameter("name")), 30);
        if (nameQuery == null) {
            nameQuery = "";
        }
        String archivedNameQuery = limit(trim(request.getParameter("archivedName")), 30);
        if (archivedNameQuery == null) {
            archivedNameQuery = "";
        }
        request.setAttribute("users", dao.getManageableUsers(isAdmin, roleFilterId, nameQuery));
        request.setAttribute("archivedUsers", dao.getArchivedUsers(isAdmin, archivedRoleFilterId, archivedNameQuery));
        request.setAttribute("roles", roles);
        request.setAttribute("roleFilterId", roleFilterId);
        request.setAttribute("nameQuery", nameQuery);
        request.setAttribute("archivedRoleFilterId", archivedRoleFilterId);
        request.setAttribute("archivedNameQuery", archivedNameQuery);

        // Chỉ tải mục tiêu sửa nếu tài khoản vẫn nằm trong phạm vi vai trò được quản lý.
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
        User createUserForm = (User) session.getAttribute("createUserForm");
        if (createUserForm != null) {
            request.setAttribute("createUserForm", createUserForm);
            session.removeAttribute("createUserForm");
        }
        request.setAttribute("manageMessage", session.getAttribute("manageMessage"));
        request.setAttribute("manageMessageType", session.getAttribute("manageMessageType"));
        session.removeAttribute("manageMessage");
        session.removeAttribute("manageMessageType");
        request.getRequestDispatcher("/WEB-INF/views/admin/manage-users.jsp").forward(request, response);
    }

    /** Xử lý thao tác tạo, sửa, xóa mềm, khôi phục hoặc khóa/mở tài khoản. */
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
        int archivedRoleFilterId = parseInt(request.getParameter("filterArchivedRoleId"), 0);
        String archivedNameQuery = trim(request.getParameter("filterArchivedName"));
        String message;
        String messageType = "error";

        // Mọi thao tác kết thúc bằng redirect để tránh gửi lại POST khi tải lại trang.
        if ("create".equals(action)) {
            User user = readFormUser(request);
            user.setIsActive(true);
            message = validateUser(user, true);
            if (message == null) {
                if (dao.createManagedUser(user, isAdmin)) {
                    message = "Đã tạo tài khoản.";
                    messageType = "success";
                    request.getSession().removeAttribute("createUserForm");
                } else {
                    message = "Không thể tạo tài khoản. Email hoặc tên đăng nhập có thể đã tồn tại.";
                }
            }
            if (!"success".equals(messageType)) {
                user.setPassword(null);
                request.getSession().setAttribute("createUserForm", user);
            }
        } else if ("update".equals(action)) {
            // Khi sửa, mật khẩu để trống nghĩa là giữ nguyên mật khẩu cũ.
            User user = readFormUser(request);
            user.setUserId(parseInt(request.getParameter("userId"), -1));
            message = validateUser(user, false);
            if (message == null) {
                if (dao.updateManagedUser(user, isAdmin)) {
                    message = "Đã cập nhật tài khoản.";
                    messageType = "success";
                } else {
                    message = "Không thể cập nhật tài khoản. Hãy kiểm tra role, email/tên đăng nhập; role Admin được cố định.";
                }
            }
        } else if ("delete".equals(action)) {
            // DAO ngăn xóa tài khoản đang đăng nhập và tài khoản ngoài phạm vi quyền.
            int userId = parseInt(request.getParameter("userId"), -1);
            if (dao.deleteManagedUser(userId, actor.getUserId(), isAdmin)) {
                message = "Đã xóa tài khoản; dữ liệu lịch sử được giữ lại.";
                messageType = "success";
            } else {
                message = "Không thể xóa tài khoản này; có thể đây là tài khoản đang đăng nhập hoặc không thuộc phạm vi quản lý.";
            }
        } else if ("restore".equals(action)) {
            int userId = parseInt(request.getParameter("userId"), -1);
            if (dao.restoreManagedUser(userId, actor.getUserId(), isAdmin)) {
                message = "Đã khôi phục tài khoản ở trạng thái khóa. Dùng Mở khóa để cho phép đăng nhập.";
                messageType = "success";
            } else {
                message = "Không thể khôi phục tài khoản này hoặc tài khoản không thuộc phạm vi quản lý.";
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
                redirect += "&name=" + URLEncoder.encode(nameQuery, StandardCharsets.UTF_8);
            }
        }
        if (archivedRoleFilterId > 0 || (archivedNameQuery != null && !archivedNameQuery.isEmpty())) {
            redirect += redirect.contains("?") ? "&" : "?";
            redirect += "archivedRoleId=" + archivedRoleFilterId;
            if (archivedNameQuery != null && !archivedNameQuery.isEmpty()) {
                redirect += "&archivedName=" + URLEncoder.encode(archivedNameQuery, StandardCharsets.UTF_8);
            }
        }
        response.sendRedirect(redirect);
    }

    /** Xác thực phiên và giới hạn endpoint này cho Admin hoặc Manager. */
    private User requireManagerOrAdmin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
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

    /** Trả về true nếu người dùng hiện tại có vai trò Admin. */
    private boolean isAdmin(User user) {
        return user.getRoleName() != null && "Admin".equalsIgnoreCase(user.getRoleName());
    }

    /** Trả về true nếu người dùng hiện tại có vai trò Manager. */
    private boolean isManager(User user) {
        return user.getRoleName() != null && "Manager".equalsIgnoreCase(user.getRoleName());
    }

    /** Đọc và chuẩn hóa các trường form thành đối tượng User. */
    private User readFormUser(HttpServletRequest request) {
        User user = new User();
        user.setUsername(trim(request.getParameter("username")));
        user.setEmail(trim(request.getParameter("email")));
        user.setFullName(trim(request.getParameter("fullName")));
        user.setPhone(trim(request.getParameter("phone")));
        user.setPassword(request.getParameter("password"));
        user.setRoleId(parseInt(request.getParameter("roleId"), -1));
        return user;
    }

    /** Kiểm tra dữ liệu tài khoản trước khi gọi DAO; mật khẩu có thể tùy chọn khi cập nhật. */
    private String validateUser(User user, boolean passwordRequired) {
        if (user.getUsername() == null || user.getUsername().isEmpty() || user.getUsername().length() > 30) {
            return "Tên đăng nhập là bắt buộc và tối đa 30 ký tự.";
        }
        if (user.getEmail() == null || user.getEmail().isEmpty()
                || !EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            return "Email không đúng định dạng.";
        }
        if (user.getEmail().length() > 50) {
            return "Email không được dài quá 50 ký tự.";
        }
        if (user.getFullName() == null || user.getFullName().isEmpty() || user.getFullName().length() > 30) {
            return "Họ và tên là bắt buộc và tối đa 30 ký tự.";
        }
        if (user.getPhone() == null || !PHONE_PATTERN.matcher(user.getPhone()).matches()) {
            return "Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số.";
        }
        if (user.getRoleId() < 1) {
            return "Vui lòng chọn vai trò hợp lệ.";
        }
        String password = user.getPassword();
        if ((passwordRequired || (password != null && !password.isEmpty()))
                && (password == null || password.length() < 8 || password.length() > 30)) {
            return "Mật khẩu phải có từ 8 đến 30 ký tự.";
        }
        return null;
    }

    /** Chuyển tham số sang số nguyên hoặc trả về giá trị mặc định nếu không hợp lệ. */
    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Xóa khoảng trắng ở hai đầu chuỗi và giữ nguyên null. */
    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String limit(String value, int maxLength) {
        return value != null && value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    /** Lưu thông báo một lần trong session cho lần GET kế tiếp (Post/Redirect/Get). */
    private void setFlash(HttpSession session, String message, String messageType) {
        session.setAttribute("manageMessage", message);
        session.setAttribute("manageMessageType", messageType);
    }
}
