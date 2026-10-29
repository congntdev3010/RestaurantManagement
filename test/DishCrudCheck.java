import feature.menu.dao.DishDAO;
import model.Dish;
import java.math.BigDecimal;

/** Chạy thử CRUD món trên CSDL thật (tạo một món tạm rồi xoá). */
public class DishCrudCheck {
    public static void main(String[] args) throws Exception {
        System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        try (DishDAO dao = new DishDAO()) {
            Dish d = new Dish();
            d.setCategoryId(1);
            d.setDishName("Món Thử Nghiệm Xoá Ngay");
            d.setDescription("Tạo bởi DishCrudCheck");
            d.setPrice(new BigDecimal("99000"));
            d.setUnit("Đĩa");
            d.setImage("uploads/dishes/00000000-0000-0000-0000-000000000000.jpg");
            d.setIsFeatured(true);
            d.setStatus(false);
            int id = dao.createDish(d);
            System.out.println("create id=" + id);

            Dish got = dao.getDishById(id);
            System.out.println("read: " + got.getDishName() + " | " + got.getPrice() + " | " + got.getUnit()
                    + " | featured=" + got.isIsFeatured() + " | status=" + got.isStatus());
            System.out.println("an voi khach (menu): " + dao.getTotalDishesCount("mon thu nghiem", 0)
                    + " | thay o trang quan ly: " + dao.getAdminDishes("mon thu nghiem", 0, "").size());

            got.setDishName("Món Thử Nghiệm Đã Sửa");
            got.setPrice(new BigDecimal("120000"));
            got.setStatus(true);
            System.out.println("update=" + dao.updateDish(got));
            Dish after = dao.getDishById(id);
            System.out.println("after: " + after.getDishName() + " | " + after.getPrice() + " | status=" + after.isStatus()
                    + " | menu thay: " + dao.getTotalDishesCount("thu nghiem da sua", 0));

            System.out.println("toggle hide=" + dao.setDishStatus(id, false) + " -> status=" + dao.getDishById(id).isStatus());
            System.out.println("delete=" + dao.deleteDish(id) + " | con ton tai: " + (dao.getDishById(id) != null));
            System.out.println("delete lan 2=" + dao.deleteDish(id));
        }
    }
}
