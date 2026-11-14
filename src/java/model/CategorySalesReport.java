package model;

import java.util.ArrayList;
import java.util.List;

public class CategorySalesReport {
    private final String categoryName;
    private final List<DishSales> topDishes = new ArrayList<>();
    private DishSales leastChosenDish;

    public CategorySalesReport(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public List<DishSales> getTopDishes() {
        return topDishes;
    }

    public DishSales getLeastChosenDish() {
        return leastChosenDish;
    }

    public void setLeastChosenDish(DishSales leastChosenDish) {
        this.leastChosenDish = leastChosenDish;
    }
}
