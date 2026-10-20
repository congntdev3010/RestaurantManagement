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

@WebServlet(name = "ManageOrdersServlet", urlPatterns = {"/manage-orders"})
public class ManageOrdersServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = requireStaff(request, response); if (actor == null) return;
        HttpSession session = request.getSession(); CartServlet.ensureToken(session);
        request.setAttribute("orderMessage", session.getAttribute("staffOrderMessage")); session.removeAttribute("staffOrderMessage");
        int id = parseInt(request.getParameter("id"), -1);
        try (OrderDAO orders = new OrderDAO()) {
            if (id > 0) {
                Order order = orders.getOrder(id, null);
                if (order == null) { response.sendError(404); return; }
                request.setAttribute("order", order);
                if ("OPEN".equals(order.getStatus())) {
                    try (DishDAO dishDAO = new DishDAO()) {
                        request.setAttribute("availableDishes", dishDAO.getDishes(null, 0, null, 1, 1000));
                    }
                }
                request.getRequestDispatcher("/WEB-INF/views/staff/order-detail.jsp").forward(request, response);
            } else {
                request.setAttribute("orders", orders.getOrders(null, request.getParameter("status")));
                if ("new".equals(request.getParameter("action"))) {
                    loadCreateForm(request);
                    request.getRequestDispatcher("/WEB-INF/views/staff/order-create.jsp").forward(request, response);
                } else request.getRequestDispatcher("/WEB-INF/views/staff/manage-orders.jsp").forward(request, response);
            }
        }
    }

    private void loadCreateForm(HttpServletRequest request) throws ServletException, IOException {
        List<Category> categories; List<Dish> dishes;
        try (DishDAO dishDAO = new DishDAO(); CategoryDAO categoryDAO = new CategoryDAO()) {
            categories = categoryDAO.getAllCategories();
            dishes = dishDAO.getDishes(null, 0, null, 1, 1000);
        }
        try (RestaurantTableDAO tableDAO = new RestaurantTableDAO()) { request.setAttribute("tables", tableDAO.getAllTables()); }
        catch (SQLException e) { throw new ServletException(e); }
        request.setAttribute("categories", categories); request.setAttribute("dishes", dishes);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8"); User actor = requireStaff(request,response); if (actor==null) return;
        HttpSession session=request.getSession(false);
        if (session==null || !CartServlet.validToken(session,request.getParameter("csrfToken"))) { response.sendError(403); return; }
        String action=request.getParameter("action"); int id=parseInt(request.getParameter("orderId"),-1); boolean ok=false;
        try (OrderDAO dao=new OrderDAO()) {
            if ("create".equals(action)) {
                Integer tableId=parsePositive(request.getParameter("tableId")), reservationId=parsePositive(request.getParameter("reservationId"));
                String name=trim(request.getParameter("customerName")), phone=trim(request.getParameter("customerPhone"));
                if (tableId!=null && name!=null && phone!=null) {
                    int created=dao.createOrder(null,actor.getUserId(),tableId,reservationId,"OPEN",name,phone,
                            trim(request.getParameter("customerEmail")),trim(request.getParameter("note")),readQuantities(request));
                    if (created>0) { session.setAttribute("staffOrderMessage","Đã tạo đơn hàng."); response.sendRedirect(request.getContextPath()+"/manage-orders?id="+created); return; }
                }
            } else if ("approve".equals(action)) ok=dao.approve(id,actor.getUserId());
            else if ("start".equals(action)) ok=dao.startServing(id);
            else if ("paid".equals(action)) ok=dao.markPaid(id);
            else if ("cancel".equals(action)) ok=dao.cancelStaffOrder(id,trim(request.getParameter("reason")),actor.getUserId());
            else if ("chefStart".equals(action) && "Server".equalsIgnoreCase(actor.getRoleName()))
                ok=dao.startChefPreparation(id);
            else if ("chefComplete".equals(action) && "Server".equalsIgnoreCase(actor.getRoleName()))
                ok=dao.completeChefPreparation(id);
            else if ("updateItems".equals(action)) {
                Map<Integer,Integer> quantities=readQuantities(request); addNewDish(request,quantities);
                ok=dao.updateStaffOpenOrder(id,quantities);
            }
        }
        session.setAttribute("staffOrderMessage",ok?"Đã cập nhật đơn hàng.":"Không thể thực hiện thao tác với đơn hàng.");
        if (id>0) response.sendRedirect(request.getContextPath()+"/manage-orders?id="+id);
        else response.sendRedirect(request.getContextPath()+"/manage-orders?action=new");
    }

    private Map<Integer,Integer> readQuantities(HttpServletRequest request) {
        Map<Integer,Integer> quantities=new LinkedHashMap<>();
        for(String key:request.getParameterMap().keySet()) if(key.startsWith("quantity_")) {
            int dishId=parseInt(key.substring(9),-1), qty=parseInt(request.getParameter(key),-1);
            if(dishId>0 && qty>0 && qty<=99) quantities.put(dishId,qty);
        }
        return quantities;
    }
    private void addNewDish(HttpServletRequest request,Map<Integer,Integer> quantities) {
        int dishId=parseInt(request.getParameter("newDishId"),-1), qty=parseInt(request.getParameter("newQuantity"),0);
        if(dishId>0&&qty>0&&qty<=99) quantities.merge(dishId,qty,(a,b)->Math.min(99,a+b));
    }
    private User requireStaff(HttpServletRequest request,HttpServletResponse response)throws IOException {
        HttpSession s=request.getSession(false); Object v=s==null?null:s.getAttribute("user");
        if(!(v instanceof User)){response.sendRedirect(request.getContextPath()+"/login");return null;}
        User u=(User)v; String role=u.getRoleName();
        if(!"Server".equalsIgnoreCase(role)&&!"Manager".equalsIgnoreCase(role)){response.sendError(403);return null;} return u;
    }
    private int parseInt(String value,int fallback){try{return Integer.parseInt(value);}catch(Exception e){return fallback;}}
    private Integer parsePositive(String value){int i=parseInt(value,-1);return i>0?i:null;}
    private String trim(String value){if(value==null)return null;String s=value.trim();return s.isEmpty()?null:s;}
}
