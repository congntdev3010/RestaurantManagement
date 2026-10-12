package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Dish {

    private int dishId;
    private int categoryId;
    private String categoryName; // Tên danh mục phục vụ hiển thị
    private String dishName;
    private String description;
    private BigDecimal price;
    private String unit;
    private String image;
    private boolean isFeatured;
    private boolean status;
    private Timestamp createdAt;

    public Dish() {
    }

    public Dish(int dishId, int categoryId, String categoryName, String dishName, String description,
            BigDecimal price, String unit, String image, boolean isFeatured, boolean status, Timestamp createdAt) {
        this.dishId = dishId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.dishName = dishName;
        this.description = description;
        this.price = price;
        this.unit = unit;
        this.image = image;
        this.isFeatured = isFeatured;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getDishId() {
        return dishId;
    }

    public void setDishId(int dishId) {
        this.dishId = dishId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDishName() {
        return dishName;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
