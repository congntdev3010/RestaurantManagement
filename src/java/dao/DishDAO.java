package dao;

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
}
