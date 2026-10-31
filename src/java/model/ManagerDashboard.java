package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ManagerDashboard {
    private BigDecimal todayRevenue = BigDecimal.ZERO;
    private BigDecimal yesterdayRevenue = BigDecimal.ZERO;
    private int todayOrderCount;
    private int occupiedTableCount;
    private int totalTableCount;
    private int newCustomerCount;
    private int returningCustomerCount;
    private final List<CategorySales> categorySales = new ArrayList<>();
    private final List<DishSales> leastSoldDishes = new ArrayList<>();

    public BigDecimal getTodayRevenue() {
        return todayRevenue;
    }

    public void setTodayRevenue(BigDecimal todayRevenue) {
        this.todayRevenue = todayRevenue;
    }

    public BigDecimal getYesterdayRevenue() {
        return yesterdayRevenue;
    }

    public void setYesterdayRevenue(BigDecimal yesterdayRevenue) {
        this.yesterdayRevenue = yesterdayRevenue;
    }

    public BigDecimal getRevenueDifference() {
        return todayRevenue.subtract(yesterdayRevenue).abs();
    }

    public boolean isRevenueIncrease() {
        return todayRevenue.compareTo(yesterdayRevenue) > 0;
    }

    public boolean isRevenueDecrease() {
        return todayRevenue.compareTo(yesterdayRevenue) < 0;
    }

    public int getTodayOrderCount() {
        return todayOrderCount;
    }

    public void setTodayOrderCount(int todayOrderCount) {
        this.todayOrderCount = todayOrderCount;
    }

    public int getOccupiedTableCount() {
        return occupiedTableCount;
    }

    public void setOccupiedTableCount(int occupiedTableCount) {
        this.occupiedTableCount = occupiedTableCount;
    }

    public int getTotalTableCount() {
        return totalTableCount;
    }

    public void setTotalTableCount(int totalTableCount) {
        this.totalTableCount = totalTableCount;
    }

    public int getNewCustomerCount() {
        return newCustomerCount;
    }

    public void setNewCustomerCount(int newCustomerCount) {
        this.newCustomerCount = newCustomerCount;
    }

    public int getReturningCustomerCount() {
        return returningCustomerCount;
    }

    public void setReturningCustomerCount(int returningCustomerCount) {
        this.returningCustomerCount = returningCustomerCount;
    }

    public List<CategorySales> getCategorySales() {
        return categorySales;
    }

    public List<DishSales> getLeastSoldDishes() {
        return leastSoldDishes;
    }

    public static class CategorySales {
        private final String categoryName;
        private final List<DishSales> dishes = new ArrayList<>();

        public CategorySales(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public List<DishSales> getDishes() {
            return dishes;
        }
    }

    public static class DishSales {
        private final String dishName;
        private final String categoryName;
        private final String unit;
        private final int quantity;

        public DishSales(String dishName, String categoryName, String unit, int quantity) {
            this.dishName = dishName;
            this.categoryName = categoryName;
            this.unit = unit;
            this.quantity = quantity;
        }

        public String getDishName() {
            return dishName;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public String getUnit() {
            return unit;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}
