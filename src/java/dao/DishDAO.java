package dao;

import dal.DBContext;
import model.Dish;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DishDAO extends DBContext {

    // Lấy danh sách món ăn: Search + Filter Category + Sort Price + Pagination
    public List<Dish> getDishes(String keyword, int categoryId, String sortBy, int pageIndex, int pageSize) {
        List<Dish> list = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT d.*, c.category_name FROM Dish d ")
           .append("JOIN Category c ON d.category_id = c.category_id ")
           .append("WHERE d.status = 1 ");

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND d.dish_name LIKE ? ");
        }
        if (categoryId > 0) {
            sql.append("AND d.category_id = ? ");
        }

        // Sắp xếp
        if ("asc".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY d.price ASC ");
        } else if ("desc".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY d.price DESC ");
        } else {
            sql.append("ORDER BY d.dish_id DESC ");
        }

        // Phân trang SQL Server
        sql.append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        try {
            PreparedStatement ps = connection.prepareStatement(sql.toString());
            int paramIndex = 1;
            if (keyword != null && !keyword.trim().isEmpty()) {
                ps.setNString(paramIndex++, "%" + keyword.trim() + "%");
            }
            if (categoryId > 0) {
                ps.setInt(paramIndex++, categoryId);
            }

            int offset = (pageIndex - 1) * pageSize;
            ps.setInt(paramIndex++, offset);
            ps.setInt(paramIndex++, pageSize);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Dish(
                    rs.getInt("dish_id"),
                    rs.getInt("category_id"),
                    rs.getNString("category_name"),
                    rs.getNString("dish_name"),
                    rs.getNString("description"),
                    rs.getBigDecimal("price"),
                    rs.getNString("unit"),
                    rs.getString("image"),
                    rs.getBoolean("is_featured"),
                    rs.getBoolean("status"),
                    rs.getTimestamp("created_at")
                ));
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return list;
    }

    // Đếm tổng số món thỏa mãn điều kiện lọc
    public int getTotalDishesCount(String keyword, int categoryId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Dish WHERE status = 1 ");
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND dish_name LIKE ? ");
        }
        if (categoryId > 0) {
            sql.append("AND category_id = ? ");
        }

        try {
            PreparedStatement ps = connection.prepareStatement(sql.toString());
            int paramIndex = 1;
            if (keyword != null && !keyword.trim().isEmpty()) {
                ps.setNString(paramIndex++, "%" + keyword.trim() + "%");
            }
            if (categoryId > 0) {
                ps.setInt(paramIndex++, categoryId);
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return 0;
    }
}