package dao;

import dal.DBContext;
import model.Category;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO extends DBContext {

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM Category WHERE status = 1 ORDER BY category_id ASC";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Category(
                    rs.getInt("category_id"),
                    rs.getNString("category_name"),
                    rs.getNString("description"),
                    rs.getBoolean("status")
                ));
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return list;
    }
}