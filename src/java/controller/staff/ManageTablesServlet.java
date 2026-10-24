package controller.staff;

import dao.RestaurantTableDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.RestaurantTable;
import model.User;

@WebServlet(name = "ManageTablesServlet", urlPatterns = {"/manage-tables"})
public class ManageTablesServlet extends HttpServlet {

    /** Trạng thái được chấp nhận từ form và lưu vào cơ sở dữ liệu. */
    private static final Set<String> TABLE_STATUSES = Set.of("AVAILABLE", "RESERVED", "OCCUPIED");

    /**
     * Xác thực quyền xem, tải sơ đồ bàn và chuẩn bị dữ liệu cho view.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        User actor = requireTableViewer(request, response);
        if (actor == null) {
            return;
        }

        boolean canManage = isManager(actor);
        // Server không được đổi trạng thái bàn; bàn tự chuyển đang phục vụ khi server order hộ khách.
        boolean canChangeStatus = canManage || hasRole(actor, "Receptionist");
        RestaurantTableDAO dao = new RestaurantTableDAO();
        List<RestaurantTable> tables;
        boolean tableLoadFailed = false;

        // Giữ view hoạt động khi đọc danh sách lỗi, đồng thời hiển thị lỗi rõ ràng.
        try {
            tables = dao.getAllTables();
        } catch (SQLException e) {
            e.printStackTrace();
            tables = new ArrayList<>();
            tableLoadFailed = true;
        }

        // Gom bàn theo khu vực để view có thể hiển thị từng nhóm.
        Map<String, List<RestaurantTable>> tableGroups = new LinkedHashMap<>();
        for (RestaurantTable table : tables) {
            tableGroups.computeIfAbsent(table.getTableType(), type -> new ArrayList<>()).add(table);
        }
        request.setAttribute("tableGroups", tableGroups);
        request.setAttribute("tableCount", tables.size());
        request.setAttribute("tableLoadFailed", tableLoadFailed);
        request.setAttribute("canManage", canManage);
        request.setAttribute("canChangeStatus", canChangeStatus);
        request.setAttribute("canOrder", canManage || hasRole(actor, "Server"));

        // Chỉ Manager được tải dữ liệu cho form sửa; lỗi truy vấn không được báo nhầm là không tồn tại.
        String editId = request.getParameter("edit");
        if (canManage && editId != null && !tableLoadFailed) {
            try {
                RestaurantTable editTable = dao.getTableById(parseInt(editId, -1));
                if (editTable != null) {
                    request.setAttribute("editTable", editTable);
                } else {
                    setFlash(request.getSession(), "Không tìm thấy bàn cần chỉnh sửa.", "error");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                setFlash(request.getSession(), "Không thể tải bàn cần chỉnh sửa do lỗi dữ liệu.", "error");
            }
        }

        // Chuyển thông báo flash từ session sang request để chỉ hiển thị một lần.
        HttpSession session = request.getSession();
        request.setAttribute("tableMessage", session.getAttribute("tableMessage"));
        request.setAttribute("tableMessageType", session.getAttribute("tableMessageType"));
        session.removeAttribute("tableMessage");
        session.removeAttribute("tableMessageType");
        request.getRequestDispatcher("/WEB-INF/views/staff/manage-tables.jsp").forward(request, response);
    }

    /**
     * Kiểm tra quyền theo hành động, xác thực dữ liệu và gọi DAO để thay đổi bàn.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User actor = requireTableViewer(request, response);
        if (actor == null) {
            return;
        }

        RestaurantTableDAO dao = new RestaurantTableDAO();
        String action = request.getParameter("action");
        String message;
        String messageType = "error";

        // Nhân viên chỉ được đổi trạng thái; các thao tác quản lý khác cần quyền Manager.
        if ("changeStatus".equals(action) && hasRole(actor, "Server")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!"changeStatus".equals(action) && !isManager(actor)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if ("changeStatus".equals(action)) {
            int tableId = parseInt(request.getParameter("tableId"), -1);
            String status = request.getParameter("status");

            // Kiểm tra allowlist tại servlet; DAO còn kiểm tra chuyển trạng thái trong câu SQL.
            if (tableId > 0 && TABLE_STATUSES.contains(status)
                    && dao.updateTableStatus(tableId, status)) {
                message = "Đã cập nhật trạng thái bàn.";
                messageType = "success";
            } else {
                message = "Không thể cập nhật trạng thái bàn; trạng thái chuyển đổi không hợp lệ hoặc bàn không còn tồn tại.";
            }
        } else if ("create".equals(action) || "update".equals(action)) {
            RestaurantTable table = readFormTable(request);
            boolean creating = "create".equals(action);

            // Không nhận trạng thái tạo mới từ client; bàn mới luôn bắt đầu ở trạng thái trống.
            if (creating) {
                table.setStatus("AVAILABLE");
            }

            message = validateTable(table);
            if (message == null && !creating) {
                table.setTableId(parseInt(request.getParameter("tableId"), -1));
                if (table.getTableId() < 1) {
                    message = "Bàn cần cập nhật không hợp lệ.";
                }
            }

            if (message == null) {
                boolean saved = creating ? dao.createTable(table) : dao.updateTable(table);
                if (saved) {
                    message = creating ? "Đã thêm bàn." : "Đã cập nhật bàn.";
                    messageType = "success";
                } else {
                    message = "Không thể lưu bàn. Mã bàn có thể bị trùng hoặc trạng thái chuyển đổi không hợp lệ.";
                }
            }
        } else if ("delete".equals(action)) {
            int tableId = parseInt(request.getParameter("tableId"), -1);
            if (tableId > 0 && dao.deleteTable(tableId)) {
                message = "Đã xóa bàn.";
                messageType = "success";
            } else {
                message = "Không thể xóa bàn; chỉ xóa được bàn đang trống.";
            }
        } else {
            message = "Thao tác không hợp lệ.";
        }

        // Dùng POST/Redirect/GET để tránh gửi lại thao tác khi tải lại trang.
        setFlash(request.getSession(), message, messageType);
        response.sendRedirect(request.getContextPath() + "/manage-tables");
    }

    /**
     * Yêu cầu đăng nhập và giới hạn quyền xem sơ đồ cho Manager, Server, Receptionist.
     */
    private User requireTableViewer(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        Object sessionUser = session == null ? null : session.getAttribute("user");
        if (!(sessionUser instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        User actor = (User) sessionUser;
        if (!isManager(actor) && !hasRole(actor, "Server") && !hasRole(actor, "Receptionist")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return actor;
    }

    /** Trả về true nếu người dùng có vai trò Manager. */
    private boolean isManager(User user) {
        return hasRole(user, "Manager");
    }

    /** So sánh tên vai trò không phân biệt chữ hoa/thường. */
    private boolean hasRole(User user, String roleName) {
        return user.getRoleName() != null && roleName.equalsIgnoreCase(user.getRoleName());
    }

    /** Đọc và chuẩn hóa các trường bàn được gửi từ form. */
    private RestaurantTable readFormTable(HttpServletRequest request) {
        RestaurantTable table = new RestaurantTable();
        table.setTableName(trim(request.getParameter("tableName")));
        table.setTableType(trim(request.getParameter("tableType")));
        table.setCapacity(parseInt(request.getParameter("capacity"), -1));
        table.setStatus(request.getParameter("status"));
        table.setDescription(trim(request.getParameter("description")));
        return table;
    }

    /**
     * Kiểm tra giới hạn độ dài, sức chứa và trạng thái trước khi ghi dữ liệu.
     * Trả về thông báo lỗi đầu tiên hoặc null nếu dữ liệu hợp lệ.
     */
    private String validateTable(RestaurantTable table) {
        if (table.getTableName() == null || table.getTableName().isEmpty() || table.getTableName().length() > 50) {
            return "Tên bàn là bắt buộc và tối đa 50 ký tự.";
        }
        if (table.getTableType() == null || table.getTableType().isEmpty() || table.getTableType().length() > 50) {
            return "Loại bàn là bắt buộc và tối đa 50 ký tự.";
        }
        if (table.getCapacity() < 1 || table.getCapacity() > 30) {
            return "Sức chứa phải từ 1 đến 30 khách.";
        }
        if (table.getDescription() != null && table.getDescription().length() > 255) {
            return "Ghi chú tối đa 255 ký tự.";
        }
        if (!TABLE_STATUSES.contains(table.getStatus())) {
            return "Trạng thái bàn không hợp lệ.";
        }
        return null;
    }

    /** Chuyển chuỗi sang số nguyên; dùng giá trị dự phòng khi đầu vào sai định dạng. */
    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Xóa khoảng trắng hai đầu; giữ null nếu tham số không được gửi. */
    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** Lưu thông báo trong session để hiển thị một lần sau redirect. */
    private void setFlash(HttpSession session, String message, String messageType) {
        session.setAttribute("tableMessage", message);
        session.setAttribute("tableMessageType", messageType);
    }
}
