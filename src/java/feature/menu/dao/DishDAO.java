package feature.menu.dao;

import dal.DBContext;
import model.Dish;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import utils.SearchUtils;

public class DishDAO extends DBContext {

    public Dish getActiveDishById(int dishId) {
        String sql = "SELECT d.*, c.category_name FROM Dish d JOIN Category c ON d.category_id=c.category_id "
                + "WHERE d.dish_id=? AND d.status=1 AND c.status=1";
        if (connection == null) return null;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Dish(rs.getInt("dish_id"), rs.getInt("category_id"), rs.getNString("category_name"),
                        rs.getNString("dish_name"), rs.getNString("description"), rs.getBigDecimal("price"),
                        rs.getNString("unit"), rs.getString("image"), rs.getBoolean("is_featured"),
                        rs.getBoolean("status"), rs.getTimestamp("created_at"));
            }
        } catch (SQLException e) { e.printStackTrace(); return null; }
    }

    /*
     * Tìm kiếm không dấu: COLLATE Vietnamese_CI_AI của SQL Server KHÔNG coi "e"="ê", "a"="â/ă", "o"="ô/ơ",
     * "u"="ư", "d"="đ" (tiếng Việt xem đó là chữ cái riêng) ⇒ gõ "canh ga chien" không ra "Cánh gà chiên".
     * Vì vậy lấy các món đang bán (lọc danh mục + sắp xếp bằng SQL), rồi lọc từ khoá bằng Java với
     * SearchUtils (bỏ hết dấu, đ→d, khớp từng từ, không cần đúng thứ tự). Thực đơn nhà hàng chỉ vài trăm món
     * nên lọc trong bộ nhớ là đủ nhanh.
     */
    private List<Dish> findActiveDishes(String keyword, int categoryId, String sortBy) {
        List<Dish> list = new ArrayList<>();
        if (connection == null) return list;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT d.*, c.category_name FROM Dish d ")
           .append("JOIN Category c ON d.category_id = c.category_id ")
           .append("WHERE d.status = 1 ");
        if (categoryId > 0) {
            sql.append("AND d.category_id = ? ");
        }
        if ("asc".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY d.price ASC, d.dish_id DESC");
        } else if ("desc".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY d.price DESC, d.dish_id DESC");
        } else {
            sql.append("ORDER BY d.dish_id DESC");
        }

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            if (categoryId > 0) {
                ps.setInt(1, categoryId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getNString("dish_name");
                    if (!SearchUtils.matches(name, keyword)) {
                        continue;
                    }
                    list.add(new Dish(
                        rs.getInt("dish_id"),
                        rs.getInt("category_id"),
                        rs.getNString("category_name"),
                        name,
                        rs.getNString("description"),
                        rs.getBigDecimal("price"),
                        rs.getNString("unit"),
                        rs.getString("image"),
                        rs.getBoolean("is_featured"),
                        rs.getBoolean("status"),
                        rs.getTimestamp("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return list;
    }

    // Lấy danh sách món ăn: Search (không dấu) + Filter Category + Sort Price + Pagination
    public List<Dish> getDishes(String keyword, int categoryId, String sortBy, int pageIndex, int pageSize) {
        List<Dish> all = findActiveDishes(keyword, categoryId, sortBy);
        int from = Math.max(0, (pageIndex - 1) * pageSize);
        if (from >= all.size()) {
            return new ArrayList<>();
        }
        int to = Math.min(all.size(), from + pageSize);
        return new ArrayList<>(all.subList(from, to));
    }

    // Đếm tổng số món thỏa mãn điều kiện lọc (cùng quy tắc tìm không dấu)
    public int getTotalDishesCount(String keyword, int categoryId) {
        return findActiveDishes(keyword, categoryId, null).size();
    }

    // ===================== QUẢN TRỊ (Manager) =====================

    /** Kết quả xoá món. */
    public enum DeleteResult { DELETED, HIDDEN_HAS_ORDERS, NOT_FOUND }

    private Dish mapDish(ResultSet rs) throws SQLException {
        return new Dish(rs.getInt("dish_id"), rs.getInt("category_id"), rs.getNString("category_name"),
                rs.getNString("dish_name"), rs.getNString("description"), rs.getBigDecimal("price"),
                rs.getNString("unit"), rs.getString("image"), rs.getBoolean("is_featured"),
                rs.getBoolean("status"), rs.getTimestamp("created_at"));
    }

    /**
     * Mọi món (kể cả đang ẩn) cho trang quản lý. status: "active" | "hidden" | "" (tất cả).
     * Tìm kiếm không dấu giống trang Thực đơn.
     */
    public List<Dish> getAdminDishes(String keyword, int categoryId, String status) throws SQLException {
        List<Dish> list = new ArrayList<>();
        if (connection == null) throw new SQLException("Không kết nối được CSDL");
        StringBuilder sql = new StringBuilder("SELECT d.*, c.category_name FROM Dish d "
                + "JOIN Category c ON d.category_id = c.category_id WHERE 1 = 1 ");
        if (categoryId > 0) sql.append("AND d.category_id = ? ");
        if ("active".equals(status)) sql.append("AND d.status = 1 ");
        else if ("hidden".equals(status)) sql.append("AND d.status = 0 ");
        sql.append("ORDER BY d.dish_id DESC");
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            if (categoryId > 0) ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (SearchUtils.matches(rs.getNString("dish_name"), keyword)) list.add(mapDish(rs));
                }
            }
        }
        return list;
    }

    /** Lấy một món bất kể trạng thái (để sửa). */
    public Dish getDishById(int dishId) throws SQLException {
        if (connection == null) throw new SQLException("Không kết nối được CSDL");
        String sql = "SELECT d.*, c.category_name FROM Dish d JOIN Category c ON d.category_id = c.category_id "
                + "WHERE d.dish_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapDish(rs) : null;
            }
        }
    }

    /** Thêm món, trả về id mới (hoặc -1). */
    public int createDish(Dish d) throws SQLException {
        if (connection == null) throw new SQLException("Không kết nối được CSDL");
        String sql = "INSERT INTO Dish (category_id, dish_name, description, price, unit, image, is_featured, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            fillDish(ps, d);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public boolean updateDish(Dish d) throws SQLException {
        if (connection == null) throw new SQLException("Không kết nối được CSDL");
        String sql = "UPDATE Dish SET category_id = ?, dish_name = ?, description = ?, price = ?, unit = ?, "
                + "image = ?, is_featured = ?, status = ? WHERE dish_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            fillDish(ps, d);
            ps.setInt(9, d.getDishId());
            return ps.executeUpdate() > 0;
        }
    }

    private void fillDish(PreparedStatement ps, Dish d) throws SQLException {
        ps.setInt(1, d.getCategoryId());
        ps.setNString(2, d.getDishName());
        ps.setNString(3, d.getDescription());
        ps.setBigDecimal(4, d.getPrice());
        ps.setNString(5, d.getUnit());
        ps.setNString(6, d.getImage());
        ps.setBoolean(7, d.isIsFeatured());
        ps.setBoolean(8, d.isStatus());
    }

    public boolean setDishStatus(int dishId, boolean active) throws SQLException {
        if (connection == null) throw new SQLException("Không kết nối được CSDL");
        try (PreparedStatement ps = connection.prepareStatement("UPDATE Dish SET status = ? WHERE dish_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, dishId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Xoá món. Món đã nằm trong đơn hàng cũ (OrderItems tham chiếu dish_id) thì KHÔNG xoá cứng được
     * vì sẽ làm hỏng lịch sử đơn ⇒ chuyển sang ẩn (status = 0) và trả HIDDEN_HAS_ORDERS.
     */
    public DeleteResult deleteDish(int dishId) throws SQLException {
        if (connection == null) throw new SQLException("Không kết nối được CSDL");
        if (getDishById(dishId) == null) return DeleteResult.NOT_FOUND;
        boolean used;
        String check = "IF OBJECT_ID('dbo.OrderItems', 'U') IS NOT NULL "
                + "SELECT COUNT(*) FROM dbo.OrderItems WHERE dish_id = ? ELSE SELECT 0";
        try (PreparedStatement ps = connection.prepareStatement(check)) {
            ps.setInt(1, dishId);
            try (ResultSet rs = ps.executeQuery()) {
                used = rs.next() && rs.getInt(1) > 0;
            }
        }
        if (used) {
            setDishStatus(dishId, false);
            return DeleteResult.HIDDEN_HAS_ORDERS;
        }
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM Dish WHERE dish_id = ?")) {
            ps.setInt(1, dishId);
            return ps.executeUpdate() > 0 ? DeleteResult.DELETED : DeleteResult.NOT_FOUND;
        } catch (SQLException ex) {
            // Còn bảng khác tham chiếu tới món ⇒ ẩn thay vì xoá
            if (ex.getErrorCode() == 547) {
                setDishStatus(dishId, false);
                return DeleteResult.HIDDEN_HAS_ORDERS;
            }
            throw ex;
        }
    }
}
