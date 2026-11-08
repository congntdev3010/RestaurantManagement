package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private int allTodayOrderCount;
    private int cancelledOrderCount;
    private int todayServiceCount;
    private BigDecimal averageServiceMinutes;
    private int loyaltyCustomerCount;
    private int loyaltyPointsEarnedToday;
    private int loyaltyPointsUsedToday;
    private int outstandingLoyaltyPoints;
    private String period = "WEEK";
    private String periodLabel = "Tuần này";
    private String previousPeriodLabel = "kỳ trước";
    private final List<CategorySales> categorySales = new ArrayList<>();
    private final List<CategorySales> leastSoldCategories = new ArrayList<>();
    private final List<TableTurnover> busiestTables = new ArrayList<>();

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

    public int getAllTodayOrderCount() {
        return allTodayOrderCount;
    }

    public void setAllTodayOrderCount(int allTodayOrderCount) {
        this.allTodayOrderCount = allTodayOrderCount;
    }

    public int getCancelledOrderCount() {
        return cancelledOrderCount;
    }

    public void setCancelledOrderCount(int cancelledOrderCount) {
        this.cancelledOrderCount = cancelledOrderCount;
    }

    public BigDecimal getCancellationRate() {
        if (allTodayOrderCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(cancelledOrderCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(allTodayOrderCount), 1, RoundingMode.HALF_UP);
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

    public int getTodayServiceCount() {
        return todayServiceCount;
    }

    public void setTodayServiceCount(int todayServiceCount) {
        this.todayServiceCount = todayServiceCount;
    }

    public BigDecimal getAverageServiceMinutes() {
        return averageServiceMinutes;
    }

    public void setAverageServiceMinutes(BigDecimal averageServiceMinutes) {
        this.averageServiceMinutes = averageServiceMinutes;
    }

    public int getLoyaltyCustomerCount() {
        return loyaltyCustomerCount;
    }

    public void setLoyaltyCustomerCount(int loyaltyCustomerCount) {
        this.loyaltyCustomerCount = loyaltyCustomerCount;
    }

    public int getLoyaltyPointsEarnedToday() {
        return loyaltyPointsEarnedToday;
    }

    public void setLoyaltyPointsEarnedToday(int loyaltyPointsEarnedToday) {
        this.loyaltyPointsEarnedToday = loyaltyPointsEarnedToday;
    }

    public int getLoyaltyPointsUsedToday() {
        return loyaltyPointsUsedToday;
    }

    public void setLoyaltyPointsUsedToday(int loyaltyPointsUsedToday) {
        this.loyaltyPointsUsedToday = loyaltyPointsUsedToday;
    }

    public int getOutstandingLoyaltyPoints() {
        return outstandingLoyaltyPoints;
    }

    public void setOutstandingLoyaltyPoints(int outstandingLoyaltyPoints) {
        this.outstandingLoyaltyPoints = outstandingLoyaltyPoints;
    }

    public List<CategorySales> getCategorySales() {
        return categorySales;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
    }

    public String getPreviousPeriodLabel() {
        return previousPeriodLabel;
    }

    public void setPreviousPeriodLabel(String previousPeriodLabel) {
        this.previousPeriodLabel = previousPeriodLabel;
    }

    public List<CategorySales> getLeastSoldCategories() {
        return leastSoldCategories;
    }

    public List<TableTurnover> getBusiestTables() {
        return busiestTables;
    }

    public static class TableTurnover {
        private final String tableName;
        private final int serviceCount;

        public TableTurnover(String tableName, int serviceCount) {
            this.tableName = tableName;
            this.serviceCount = serviceCount;
        }

        public String getTableName() {
            return tableName;
        }

        public int getServiceCount() {
            return serviceCount;
        }
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
