package controller;

import dao.RestaurantTableDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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

    // Các trạng thái được phép lưu vào cơ sở dữ liệu.
    private static final Set<String> TABLE_STATUSES = new HashSet<>(Arrays.asList(
            "AVAILABLE", "RESERVED", "OCCUPIED"));

    // Tải danh sách bàn và chỉ mở form sửa cho quản lý.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        User actor = requireTableViewer(request, response);
        if (actor == null) {
            return;
        }

        boolean canManage = isManager(actor);
        boolean canChangeStatus = canManage || hasRole(actor, "Server") || hasRole(actor, "Receptionist");
        RestaurantTableDAO dao = new RestaurantTableDAO();
        List<RestaurantTable> tables;
        boolean tableLoadFailed = false;
        try {
            tables = dao.getAllTables();
        } catch (SQLException e) {
            e.printStackTrace();
            tables = new ArrayList<>();
            tableLoadFailed = true;
        }
        Map<String, List<RestaurantTable>> tableGroups = new LinkedHashMap<>();
        for (RestaurantTable table : tables) {
            tableGroups.computeIfAbsent(table.getTableType(), type -> new ArrayList<>()).add(table);
        }
        request.setAttribute("tableGroups", tableGroups);
        request.setAttribute("tableCount", tables.size());
        request.setAttribute("tableLoadFailed", tableLoadFailed);
        request.setAttribute("canManage", canManage);
        request.setAttribute("canChangeStatus", canChangeStatus);

        String editId = request.getParameter("edit");
        if (canManage && editId != null && !tableLoadFailed) {
            RestaurantTable editTable = dao.getTableById(parseInt(editId, -1));
            if (editTable != null) {
                request.setAttribute("editTable", editTable);
            } else {
                setFlash(request.getSession(), "Không tìm thấy bàn cần chỉnh sửa.", "error");
            }
        }

        HttpSession session = request.getSession();
        request.setAttribute("tableMessage", session.getAttribute("tableMessage"));
        request.setAttribute("tableMessageType", session.getAttribute("tableMessageType"));
        session.removeAttribute("tableMessage");
        session.removeAttribute("tableMessageType");
        request.getRequestDispatcher("/manage-tables.jsp").forward(request, response);
    }

    // Kiểm tra quyền, thực hiện thao tác bàn và chuyển hướng kèm thông báo.
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

        // Nhân viên chỉ được đổi trạng thái; mọi thao tác quản trị cần quyền Manager.
        if (!"changeStatus".equals(action) && !isManager(actor)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if ("changeStatus".equals(action)) {
            int tableId = parseInt(request.getParameter("tableId"), -1);
            String status = request.getParameter("status");
            // Kiểm tra trạng thái bằng danh sách cho phép thay vì tin dữ liệu từ form.
            if (tableId > 0 && TABLE_STATUSES.contains(status)
                    && dao.updateTableStatus(tableId, status)) {
                message = "Đã cập nhật trạng thái bàn.";
                messageType = "success";
            } else {
                message = "Không thể cập nhật trạng thái bàn; trạng thái chuyển đổi không hợp lệ hoặc bàn không còn tồn tại.";
            }
        } else if ("create".equals(action) || "update".equals(action)) {
            RestaurantTable table = readFormTable(request);
            if ("create".equals(action)) {
                // Bàn mới luôn bắt đầu ở trạng thái còn trống.
                table.setStatus("AVAILABLE");
            }
            message = validateTable(table);
            if (message == null && "update".equals(action)) {
                table.setTableId(parseInt(request.getParameter("tableId"), -1));
                if (table.getTableId() < 1) {
                    message = "Bàn cần cập nhật không hợp lệ.";
                }
            }
            if (message == null) {
                boolean saved = "create".equals(action)
                        ? dao.createTable(table)
                        : dao.updateTable(table);
                if (saved) {
                    message = "create".equals(action) ? "Đã thêm bàn." : "Đã cập nhật bàn.";
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

        setFlash(request.getSession(), message, messageType);
        response.sendRedirect(request.getContextPath() + "/manage-tables");
    }

    // Xác thực người dùng và kiểm tra quyền xem sơ đồ bàn.
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

    // Kiểm tra người dùng có vai trò quản lý hay không.
    private boolean isManager(User user) {
        return hasRole(user, "Manager");
    }

    // So sánh tên vai trò không phân biệt chữ hoa, chữ thường.
    private boolean hasRole(User user, String roleName) {
        return user.getRoleName() != null && roleName.equalsIgnoreCase(user.getRoleName());
    }

    // Đọc dữ liệu form và chuyển kiểu cho đối tượng bàn.
    private RestaurantTable readFormTable(HttpServletRequest request) {
        RestaurantTable table = new RestaurantTable();
        table.setTableName(trim(request.getParameter("tableName")));
        table.setTableType(trim(request.getParameter("tableType")));
        table.setCapacity(parseInt(request.getParameter("capacity"), -1));
        table.setStatus(request.getParameter("status"));
        table.setDescription(trim(request.getParameter("description")));
        return table;
    }

    // Kiểm tra trường bắt buộc và trạng thái trước khi lưu.
    private String validateTable(RestaurantTable table) {
        if (table.getTableName() == null || table.getTableName().isEmpty() || table.getTableName().length() > 50) {
            return "Mã bàn là bắt buộc và tối đa 50 ký tự.";
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

    // Đọc số nguyên từ request; trả về giá trị dự phòng nếu không hợp lệ.
    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // Bỏ khoảng trắng đầu/cuối và giữ null nếu tham số không tồn tại.
    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    // Lưu thông báo một lần để hiển thị sau khi chuyển hướng.
    private void setFlash(HttpSession session, String message, String messageType) {
        session.setAttribute("tableMessage", message);
        session.setAttribute("tableMessageType", messageType);
    }
}
