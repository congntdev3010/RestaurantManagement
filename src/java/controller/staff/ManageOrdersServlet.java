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
    private static final String STAFF_ORDER_DRAFTS = "staffOrderDrafts";

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
                // Bắt đầu order từ sơ đồ bàn: phải chọn bàn trước.
                Integer tableId = parsePositive(request.getParameter("tableId"));
                RestaurantTable table = null;
                try (RestaurantTableDAO tableDAO = new RestaurantTableDAO()) {
                    table = tableId == null ? null : tableDAO.getTableById(tableId);
                } catch (SQLException e) { throw new ServletException(e); }
                if (table == null || "UNAVAILABLE".equals(table.getStatus())) {
                    session.setAttribute("tableMessage", "Hãy chọn một bàn trên sơ đồ để bắt đầu order.");
                    session.setAttribute("tableMessageType", "error");
                    response.sendRedirect(request.getContextPath() + "/manage-tables"); return;
                }
                loadCreateForm(request);
                request.setAttribute("table", table);
                Order pendingOrder = orders.getPendingOrderForTable(table.getTableId());
                request.setAttribute("pendingOrder", pendingOrder);
                request.setAttribute("draftQuantities", getDrafts(session).get(table.getTableId()));
                // Order lần sau của bàn: hiện kèm các món đã được duyệt ở những lần trước.
                request.setAttribute("approvedItems", orders.getApprovedItemsForTable(table.getTableId()));
                request.getRequestDispatcher("/WEB-INF/views/staff/order-create.jsp").forward(request, response);
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

    private void loadCreateForm(HttpServletRequest request) throws ServletException, IOException {
        List<Category> categories; List<Dish> dishes;
        try (DishDAO dishDAO = new DishDAO(); CategoryDAO categoryDAO = new CategoryDAO()) {
            categories = categoryDAO.getAllCategories();
            dishes = dishDAO.getDishes(null, 0, null, 1, 1000);
        }
        request.setAttribute("categories", categories); request.setAttribute("dishes", dishes);
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
            if ("create".equals(action)) {
                Integer tableId=parsePositive(request.getParameter("tableId"));
                Map<Integer,Integer> quantities=readQuantities(request);
                if (tableId!=null && dao.hasPendingOrder(tableId)) {
                    if (tableId != null) {
                        getDrafts(session).put(tableId, quantities);
                        session.setAttribute("staffOrderMessage","Đã lưu tạm món mới. Order đang chờ duyệt chưa bị thay đổi.");
                        response.sendRedirect(request.getContextPath()+"/manage-orders?action=new&tableId="+tableId); return;
                    }
                }
                if (tableId!=null && !quantities.isEmpty()) {
                    // Order của server cũng phải qua bước chờ duyệt, giống order của khách.
                    int created=dao.createOrder(null,actor.getUserId(),tableId,null,"PENDING",
                            trim(request.getParameter("customerName")),trim(request.getParameter("customerPhone")),
                            null,trim(request.getParameter("note")),quantities,readItemNotes(request,quantities));
                    if (created>0) {
                        getDrafts(session).remove(tableId);
                        // Gửi xong quay về sơ đồ bàn.
                        session.setAttribute("tableMessage","Đã gửi order #"+created+", đang chờ duyệt.");
                        session.setAttribute("tableMessageType","success");
                        response.sendRedirect(request.getContextPath()+"/manage-tables"); return;
                    }
                    message="Không thể tạo order. Hãy kiểm tra bàn và các món đã chọn.";
                } else message="Hãy chọn bàn và ít nhất một món.";
                session.setAttribute("staffOrderMessage",message);
                response.sendRedirect(request.getContextPath()+"/manage-orders?action=new"+(tableId==null?"":"&tableId="+tableId)); return;
            } else if ("approveItems".equals(action)) {
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

    /** Ghi chú từng món (note_<dishId>), chỉ lấy cho món có số lượng, tối đa 255 ký tự. */
    private Map<Integer,String> readItemNotes(HttpServletRequest request, Map<Integer,Integer> quantities) {
        Map<Integer,String> notes=new LinkedHashMap<>();
        for(Integer dishId:quantities.keySet()) {
            String n=trim(request.getParameter("note_"+dishId));
            if(n!=null) notes.put(dishId,n.length()>255?n.substring(0,255):n);
        }
        return notes;
    }
    private Map<Integer,Integer> readQuantities(HttpServletRequest request) {
        Map<Integer,Integer> quantities=new LinkedHashMap<>();
        for(String key:request.getParameterMap().keySet()) if(key.startsWith("quantity_")) {
            int dishId=parseInt(key.substring(9),-1), qty=parseInt(request.getParameter(key),-1);
            if(dishId>0 && qty>0 && qty<=99) quantities.put(dishId,qty);
        }
        return quantities;
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
    private Integer parsePositive(String value){int i=parseInt(value,-1);return i>0?i:null;}
    private String trim(String value){if(value==null)return null;String s=value.trim();return s.isEmpty()?null:s;}

    @SuppressWarnings("unchecked")
    private Map<Integer, Map<Integer, Integer>> getDrafts(HttpSession session) {
        Object value = session.getAttribute(STAFF_ORDER_DRAFTS);
        if (value instanceof Map<?, ?>) return (Map<Integer, Map<Integer, Integer>>) value;
        Map<Integer, Map<Integer, Integer>> drafts = new HashMap<>();
        session.setAttribute(STAFF_ORDER_DRAFTS, drafts);
        return drafts;
    }
}
