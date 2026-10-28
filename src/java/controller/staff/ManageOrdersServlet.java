package controller.staff;

import controller.customer.CartServlet;
import dao.CategoryDAO;
import dao.DishDAO;
import dao.OrderDAO;
import dao.RestaurantTableDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Category;
import model.Dish;
import model.Order;
import model.RestaurantTable;
import model.User;

/**
 * Server (và Manager) tạo order hộ khách từ sơ đồ bàn, duyệt từng món của order chờ duyệt,
 * đánh dấu món đã phục vụ và đóng đơn. Bếp không dùng servlet này.
 */
@WebServlet(name = "ManageOrdersServlet", urlPatterns = {"/manage-orders"})
public class ManageOrdersServlet extends HttpServlet {

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        User actor = requireStaff(request, response); if (actor == null) return;
        HttpSession session = request.getSession(); CartServlet.ensureToken(session);
        request.setAttribute("orderMessage", session.getAttribute("staffOrderMessage")); session.removeAttribute("staffOrderMessage");
        int id = parseInt(request.getParameter("id"), -1);
        try (OrderDAO orders = new OrderDAO()) {
            if (id > 0) {
                Order order = orders.getOrder(id, null);
                if (order == null) { response.sendError(404); return; }
                if (isServer(actor) && "PAID".equalsIgnoreCase(order.getStatus())) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                request.setAttribute("order", order);
                request.setAttribute("paymentQrImageUrl", getServletContext().getInitParameter("paymentQrImageUrl"));
                request.setAttribute("listStatus", listStatus(request.getParameter("status")));
                request.getRequestDispatcher("/WEB-INF/views/staff/order-detail.jsp").forward(request, response);
            } else if ("new".equals(request.getParameter("action"))) {
                // Màn order hộ khách đã tách sang TableOrderServlet; giữ link cũ chạy được.
                response.sendRedirect(request.getContextPath() + "/table-order?tableId=" + request.getParameter("tableId")); return;
            } else {
                // Mặc định chỉ hiện đơn chờ duyệt; status=ALL để xem toàn bộ.
                String status = request.getParameter("status");
                if (status == null || status.isEmpty()) status = "PENDING";
                if (isServer(actor) && "PAID".equalsIgnoreCase(status)) {
                    request.setAttribute("currentStatus", status);
                    request.setAttribute("orders", java.util.Collections.emptyList());
                    request.getRequestDispatcher("/WEB-INF/views/staff/manage-orders.jsp").forward(request, response);
                    return;
                }
                request.setAttribute("currentStatus", status);
                List<Order> visibleOrders = orders.getOrders(null, "ALL".equals(status) ? null : status);
                if (isServer(actor)) {
                    visibleOrders.removeIf(order -> "PAID".equalsIgnoreCase(order.getStatus()));
                }
                request.setAttribute("orders", visibleOrders);
                request.getRequestDispatcher("/WEB-INF/views/staff/manage-orders.jsp").forward(request, response);
            }
        }
    }

    /** Mục của danh sách (PENDING/OPEN/CANCELLED/ALL) mà nhân viên đang xem; giá trị lạ thì bỏ. */
    private static String listStatus(String s) {
        return ("PENDING".equals(s) || "OPEN".equals(s) || "PAID".equals(s)
                || "CANCELLED".equals(s) || "ALL".equals(s)) ? s : "";
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8"); User actor = requireStaff(request,response); if (actor==null) return;
        HttpSession session=request.getSession(false);
        if (session==null || !CartServlet.validToken(session,request.getParameter("csrfToken"))) { response.sendError(403); return; }
        String action=request.getParameter("action"); int id=parseInt(request.getParameter("orderId"),-1);
        String message="Không thể thực hiện thao tác với đơn hàng."; String anchor="";
        String ls=listStatus(request.getParameter("listStatus"));
        try (OrderDAO dao=new OrderDAO()) {
            if ("approveItems".equals(action)) {
                int result=dao.approveItems(id,actor.getUserId(),readApprovals(request));
                if (result==OrderDAO.APPROVE_ORDER_OPENED) message="Đã duyệt món, các món được duyệt đã lên màn hình bếp.";
                else if (result==OrderDAO.APPROVE_ORDER_CANCELLED) message="Không món nào được duyệt nên đơn đã được hủy.";
                // Duyệt xong (kể cả khi hủy hết) quay về danh sách duyệt đơn.
                if (result!=OrderDAO.APPROVE_FAILED) anchor=null;
            } else if ("serve".equals(action)) {
                int itemId=parseInt(request.getParameter("itemId"),-1); anchor="#item-"+itemId;
                if (dao.markItemServed(id,itemId)) message="Đã đánh dấu món đã phục vụ.";
            } else if ("paid".equals(action)) {
                if (!"Receptionist".equalsIgnoreCase(actor.getRoleName())
                        && !"Manager".equalsIgnoreCase(actor.getRoleName())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN); return;
                }
                String method = trim(request.getParameter("paymentMethod"));
                String reference = trim(request.getParameter("paymentReference"));
                if ("QR".equals(method) && reference == null) {
                    message = "Vui lòng nhập mã giao dịch QR.";
                } else if (dao.markPaid(id, actor.getUserId(), method, reference)) {
                    message = "Đã thanh toán bằng " + ("QR".equals(method) ? "QR" : "tiền mặt") + " và giải phóng bàn.";
                } else {
                    message = "Chỉ được thanh toán khi tất cả món đã phục vụ hoặc hết món.";
                }
            } else if ("cancel".equals(action)) {
                if (dao.cancelStaffOrder(id,trim(request.getParameter("reason")),actor.getUserId())) message="Đã hủy đơn hàng.";
                else message="Cần nhập lý do để hủy đơn.";
            }
        }
        session.setAttribute("staffOrderMessage",message);
        if (id>0 && anchor!=null) response.sendRedirect(request.getContextPath()+"/manage-orders?id="+id+(ls.isEmpty()?"":"&status="+ls)+anchor);
        else response.sendRedirect(request.getContextPath()+"/manage-orders"+(ls.isEmpty()?"":"?status="+ls));
    }

    /** Món được tick (approve_<itemId>) kèm số lượng phục vụ được (qty_<itemId>); món không tick coi như hết. */
    private Map<Integer,Integer> readApprovals(HttpServletRequest request) {
        Map<Integer,Integer> approvals=new LinkedHashMap<>();
        for(String key:request.getParameterMap().keySet()) if(key.startsWith("approve_")) {
            int itemId=parseInt(key.substring(8),-1), qty=parseInt(request.getParameter("qty_"+itemId),0);
            if(itemId>0 && qty>0 && qty<=99) approvals.put(itemId,qty);
        }
        return approvals;
    }
    private User requireStaff(HttpServletRequest request,HttpServletResponse response)throws IOException {
        HttpSession s=request.getSession(false); Object v=s==null?null:s.getAttribute("user");
        if(!(v instanceof User)){response.sendRedirect(request.getContextPath()+"/login");return null;}
        User u=(User)v; String role=u.getRoleName();
        if(!"Server".equalsIgnoreCase(role)&&!"Manager".equalsIgnoreCase(role)
                &&!"Receptionist".equalsIgnoreCase(role)){response.sendError(403);return null;} return u;
    }
    private boolean isServer(User user) {
        return "Server".equalsIgnoreCase(user.getRoleName());
    }
    private int parseInt(String value,int fallback){try{return Integer.parseInt(value);}catch(Exception e){return fallback;}}
    private String trim(String value){if(value==null)return null;String s=value.trim();return s.isEmpty()?null:s;}

}
