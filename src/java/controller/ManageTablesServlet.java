package controller;

import dao.RestaurantTableDAO;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
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

    private static final Set<String> TABLE_STATUSES = new HashSet<>(Arrays.asList(
            "AVAILABLE", "RESERVED", "OCCUPIED", "UNAVAILABLE"));

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
        request.setAttribute("tables", dao.getAllTables());
        request.setAttribute("canManage", canManage);
        request.setAttribute("canChangeStatus", canChangeStatus);

        String editId = request.getParameter("edit");
        if (canManage && editId != null) {
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

        if (!"changeStatus".equals(action) && !isManager(actor)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if ("changeStatus".equals(action)) {
            int tableId = parseInt(request.getParameter("tableId"), -1);
            String status = request.getParameter("status");
            if (tableId > 0 && TABLE_STATUSES.contains(status)
                    && dao.updateTableStatus(tableId, status)) {
                message = "Đã cập nhật trạng thái bàn.";
                messageType = "success";
            } else {
                message = "Không thể cập nhật trạng thái bàn.";
            }
        } else if ("create".equals(action) || "update".equals(action)) {
            RestaurantTable table = readFormTable(request);
            if ("create".equals(action)) {
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
                    message = "Không thể lưu bàn. Mã bàn có thể đã được sử dụng.";
                }
            }
        } else if ("delete".equals(action)) {
            int tableId = parseInt(request.getParameter("tableId"), -1);
            if (tableId > 0 && dao.deleteTable(tableId)) {
                message = "Đã xóa bàn.";
                messageType = "success";
            } else {
                message = "Không thể xóa bàn này.";
            }
        } else {
            message = "Thao tác không hợp lệ.";
        }

        setFlash(request.getSession(), message, messageType);
        response.sendRedirect(request.getContextPath() + "/manage-tables");
    }

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

    private boolean isManager(User user) {
        return hasRole(user, "Manager");
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoleName() != null && roleName.equalsIgnoreCase(user.getRoleName());
    }

    private RestaurantTable readFormTable(HttpServletRequest request) {
        RestaurantTable table = new RestaurantTable();
        table.setTableName(trim(request.getParameter("tableName")));
        table.setTableType(trim(request.getParameter("tableType")));
        table.setCapacity(parseInt(request.getParameter("capacity"), -1));
        table.setStatus(request.getParameter("status"));
        table.setDescription(trim(request.getParameter("description")));
        return table;
    }

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

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void setFlash(HttpSession session, String message, String messageType) {
        session.setAttribute("tableMessage", message);
        session.setAttribute("tableMessageType", messageType);
    }
}
