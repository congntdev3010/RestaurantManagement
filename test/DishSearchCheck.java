import feature.menu.dao.DishDAO;
import model.Dish;
import java.util.List;

/** Chạy thử tìm kiếm trên CSDL thật (cần SQL Server đang chạy). */
public class DishSearchCheck {
    public static void main(String[] args) throws Exception {
        System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        DishDAO dao = new DishDAO();
        for (String kw : new String[]{"canh ga chien", "ga", "pho", "khong-co-mon-nay"}) {
            List<Dish> list = dao.getDishes(kw, 0, null, 1, 8);
            System.out.println("[" + kw + "] tong=" + dao.getTotalDishesCount(kw, 0) + " trang1=" + list.size());
            for (Dish d : list) System.out.println("   - " + d.getDishName());
        }
        System.out.println("Khong tu khoa, tong=" + dao.getTotalDishesCount(null, 0)
                + ", trang 2 = " + dao.getDishes(null, 0, "asc", 2, 8).size() + " mon");
    }
}
