package model;

public class DishSales {
    private String dishName;
    private int quantitySold;

    public DishSales(String dishName, int quantitySold) {
        this.dishName = dishName;
        this.quantitySold = quantitySold;
    }

    public String getDishName() {
        return dishName;
    }

    public int getQuantitySold() {
        return quantitySold;
    }
}
